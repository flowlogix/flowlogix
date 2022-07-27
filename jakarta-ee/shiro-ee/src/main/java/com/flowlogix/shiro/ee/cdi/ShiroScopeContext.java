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
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.web.mgt.WebSecurityManager;

/**
 * If web environment, delegate to SessionScoped,
 * otherwise use Shiro sessions to store session beans
 *
 * @author lprimak
 */
public class ShiroScopeContext implements Context, Serializable
{
    public ShiroScopeContext(Class<? extends Annotation> scopeType, Class<? extends Annotation> webScopeType)
    {
        this.scopeType = scopeType;
        this.webScopeType = webScopeType;
        BEAN_PREFIX = String.format("FL_S%sSC_", scopeType.getSimpleName());
        bpPattern = Pattern.compile(String.format("^%s.*", BEAN_PREFIX));
    }


    @Override
    public Class<? extends Annotation> getScope()
    {
        return scopeType;
    }


    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext)
    {
        if(isWebContainerSessions(SecurityUtils.getSecurityManager()))
        {
            Context ctx = CDI.current().getBeanManager().getContext(webScopeType);
            return ctx.get(contextual, creationalContext);
        }
        else
        {
            Session session = SecurityUtils.getSubject().getSession();
            Bean<T> bean = (Bean<T>)contextual;
            synchronized(contextual)
            {
                @SuppressWarnings("unchecked")
                ScopeInst<T> scopeInst = (ScopeInst<T>)
                        session.getAttribute(BEAN_PREFIX + bean.getBeanClass().getName());
                T rv;
                if(scopeInst == null)
                {
                    rv = bean.create(creationalContext);
                    session.setAttribute(BEAN_PREFIX + bean.getBeanClass().getName(),
                            new ScopeInst<>(bean, rv, creationalContext));
                }
                else
                {
                    rv = scopeInst.instance;
                }
                return rv;
            }
        }
    }


    @Override
    public <T> T get(Contextual<T> contextual)
    {
        if(isWebContainerSessions(SecurityUtils.getSecurityManager()))
        {
            Context ctx = CDI.current().getBeanManager().getContext(webScopeType);
            return ctx.get(contextual);
        }
        else
        {
            Session session = SecurityUtils.getSubject().getSession(false);
            T rv = null;
            if(session != null)
            {
                Bean<T> bean = (Bean<T>)contextual;
                @SuppressWarnings("unchecked")
                ScopeInst<T> scopeInst = (ScopeInst<T>)
                        session.getAttribute(BEAN_PREFIX + bean.getBeanClass().getName());
                if(scopeInst != null)
                {
                    rv = scopeInst.instance;
                }
            }
            return rv;
        }
    }


    @Override
    public boolean isActive()
    {
        return true;
    }


    public <T> void onDestroy(Session session)
    {
        List<String> attrNames = session.getAttributeKeys().stream()
                .filter(String.class::isInstance)
                .map(String::valueOf)
                .filter(Objects::nonNull).filter(bpPattern.asPredicate()).collect(Collectors.toList());

        for (String attrName : attrNames)
        {
            @SuppressWarnings("unchecked")
            ScopeInst<T> scopeInst = (ScopeInst<T>) session.getAttribute(attrName);
            if (scopeInst != null)
            {
                scopeInst.bean.destroy(scopeInst.instance, scopeInst.context);
            }
        }
    }


    public static boolean isWebContainerSessions(SecurityManager sm)
    {
        if(sm instanceof WebSecurityManager)
        {
            WebSecurityManager wsm = (WebSecurityManager) sm;
            return wsm.isHttpSessionMode();
        }
        return false;
    }


    @RequiredArgsConstructor
    static class ScopeInst<T> implements Serializable
    {
        private final Bean<T> bean;
        private final T instance;
        private final CreationalContext<T> context;
        private static final long serialVersionUID = 1L;
    }


    private final Class<? extends Annotation> scopeType;
    private final Class<? extends Annotation> webScopeType;
    private final String BEAN_PREFIX;
    private final Pattern bpPattern;
    private static final long serialVersionUID = 1L;
}
