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

import com.flowlogix.shiro.ee.cdi.ShiroSecurityExtension.ShiroSecureAnnotated;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.faces.view.ViewScoped;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.session.SessionListenerAdapter;

/**
 * Entry point for Shiro Session scope CDI extension
 *
 * @author lprimak
 */
public class ShiroSessionScopeExtension implements Extension, Serializable
{
    private static final List<ShiroScopeContext> contexts = Stream.of(
            new ShiroScopeContext(ShiroSessionScoped.class, SessionScoped.class),
            new ShiroScopeContext(ShiroViewScoped.class, ViewScoped.class))
            .collect(Collectors.toList());

    @SessionScoped
    @SuppressWarnings("serial")
    private static class SessionScopedAnnotated implements Serializable { }
    @ViewScoped
    @SuppressWarnings("serial")
    private static class ViewScopedAnnotated implements Serializable { }

    @ShiroSessionScoped
    @SuppressWarnings("serial")
    private static class ShiroSessionScopedAnnotated implements Serializable { }
    @ShiroViewScoped
    @SuppressWarnings("serial")
    private static class ShiroViewScopedAnnotated implements Serializable { }


    /**
     * intercept session destroy session listeners and destroy the beans
     *
     * @param sessionListeners
     */

    /**
     * intercept session destroy session listeners and destroy the beans
     * @param sessionListeners
     * @param sm
     */
    public void addDestroyHandlers(Collection<SessionListener> sessionListeners, SecurityManager sm)
    {
        sessionListeners.add(new SessionListenerAdapter()
        {
            @Override
            public void onStop(Session session)
            {
                contexts.forEach(ctx -> ctx.onDestroy(session));
            }

            @Override
            public void onExpiration(Session session)
            {
                onStop(session);
            }
        });
    }

    <T> void addSessionScoped(@Observes @WithAnnotations(SessionScoped.class) ProcessAnnotatedType<T> pat) {
        pat.setAnnotatedType(new AnnotatedTypeWrapper<>(pat.getAnnotatedType(), true,
                Set.of(ShiroSessionScopedAnnotated.class.getDeclaredAnnotations()[0],
                        ShiroSecureAnnotated.class.getDeclaredAnnotations()[0]),
                Set.of(SessionScopedAnnotated.class.getDeclaredAnnotations()[0])));
    }

    <T> void addViewScoped(@Observes @WithAnnotations(ViewScoped.class) ProcessAnnotatedType<T> pat) {
        pat.setAnnotatedType(new AnnotatedTypeWrapper<>(pat.getAnnotatedType(), true,
                Set.of(ShiroViewScopedAnnotated.class.getDeclaredAnnotations()[0],
                        ShiroSecureAnnotated.class.getDeclaredAnnotations()[0]),
                Set.of(ViewScopedAnnotated.class.getDeclaredAnnotations()[0])));
    }

    void addScope(@Observes final BeforeBeanDiscovery event)
    {
        contexts.forEach(ctx -> event.addScope(ctx.getScope(), true, true));
    }


    void registerContext(@Observes final AfterBeanDiscovery event)
    {
        contexts.forEach(event::addContext);
    }


    private static final long serialVersionUID = 1L;
}
