/*
 * Copyright 2015 lprimak.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flowlogix.shiro.ee.cdi;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.CDI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.session.Session;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.omnifaces.cdi.viewscope.ViewScopeManager;
import org.omnifaces.util.Beans;

/**
 * If web environment, delegate to SessionScoped,
 * otherwise use Shiro sessions to store session beans
 *
 * @author lprimak
 */
@Slf4j
public class ShiroScopeContext implements Context, Serializable {
    private final Class<? extends Annotation> scopeType;
    private final Class<? extends Annotation> webScopeType;
    private final String BEAN_PREFIX;
    private final Pattern bpPattern;
    private final boolean isViewScoped;
    private static final long serialVersionUID = 1L;

    public ShiroScopeContext(Class<? extends Annotation> scopeType, Class<? extends Annotation> webScopeType) {
        this.scopeType = scopeType;
        this.webScopeType = webScopeType;
        BEAN_PREFIX = String.format("FL_S%sSC_", scopeType.getSimpleName());
        bpPattern = Pattern.compile(String.format("^%s.*", BEAN_PREFIX));
        isViewScoped = webScopeType.getName().endsWith("ViewScoped");
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scopeType;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        if (isWebContainerSessions()) {
            Context ctx = CDI.current().getBeanManager().getContext(webScopeType);
            return ctx.get(contextual, creationalContext);
        } else if (isViewScoped) {
            return Beans.getReference(ViewScopeManager.class).createBean(contextual, creationalContext);
        } else {
            Session session = SecurityUtils.getSubject().getSession();
            Bean<T> bean = (Bean<T>)contextual;
            synchronized (contextual) {
                var scopeInst = getScopedInst(session, bean);
                T rv;
                if (scopeInst == null) {
                    rv = bean.create(creationalContext);
                    setScopedInst(session, bean, new ScopeInst<>(bean, rv, creationalContext));
                } else {
                    rv = scopeInst.instance;
                }
                return rv;
            }
        }
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        if (isWebContainerSessions()) {
            Context ctx = CDI.current().getBeanManager().getContext(webScopeType);
            return ctx.get(contextual);
        } else if (isViewScoped) {
            return Beans.getReference(ViewScopeManager.class).getBean(contextual);
        } else {
            Session session = SecurityUtils.getSubject().getSession(false);
            T rv = null;
            if (session != null) {
                var scopeInst = getScopedInst(session, (Bean<T>)contextual);
                if (scopeInst != null)
                {
                    rv = scopeInst.instance;
                }
            }
            return rv;
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    <T> void onDestroy(Session session) {
        List<String> attrNames = session.getAttributeKeys().stream()
                .filter(String.class::isInstance)
                .map(String::valueOf)
                .filter(Objects::nonNull).filter(bpPattern.asPredicate()).collect(Collectors.toList());

        for (String attrName : attrNames) {
            @SuppressWarnings("unchecked")
            ScopeInst<T> scopeInst = (ScopeInst<T>) session.getAttribute(attrName);
            if (scopeInst != null) {
                scopeInst.bean.destroy(scopeInst.instance, scopeInst.context);
            }
        }
    }

    public static boolean isWebContainerSessions(SecurityManager sm) {
        if (sm instanceof WebSecurityManager) {
            WebSecurityManager wsm = (WebSecurityManager) sm;
            return wsm.isHttpSessionMode();
        }
        return false;
    }

    static boolean isWebContainerSessions() {
        try {
            return isWebContainerSessions(SecurityUtils.getSecurityManager());
        } catch (UnavailableSecurityManagerException unavailable) {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ScopeInst<T> getScopedInst(Session session, Bean<T> bean) {
        String attribute = BEAN_PREFIX + bean.getBeanClass().getName();
        return (ScopeInst<T>) session.getAttribute(attribute);
    }

    private <T> void setScopedInst(Session session, Bean<T> bean, ScopeInst<T> scoped) {
        String attribute = BEAN_PREFIX + bean.getBeanClass().getName();
        session.setAttribute(attribute, scoped);
    }

    @RequiredArgsConstructor
    static class ScopeInst<T> implements Serializable {
        private final Bean<T> bean;
        private final T instance;
        private final CreationalContext<T> context;
        private static final long serialVersionUID = 1L;
    }
}
