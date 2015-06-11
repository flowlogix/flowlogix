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
package com.flowlogix.security.cdi;

import java.io.Serializable;
import java.util.Collection;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
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
    /**
     * intercept session destroy session listeners and destroy the beans
     * 
     * @param sessionListeners 
     */
    public void addDestroyHandlers(Collection<SessionListener> sessionListeners)
    {
        sessionListeners.add(new SessionListenerAdapter()
        {
            @Override
            public void onStop(Session session)
            {
                context.onDestroy(session);
            }

            @Override
            public void onExpiration(Session session)
            {
                onStop(session);
            }
        });
    }

    
    private void addScope(@Observes final BeforeBeanDiscovery event)
    {
        event.addScope(ShiroSessionScoped.class, true, true);
    }

    
    private void registerContext(@Observes final AfterBeanDiscovery event)
    {
        event.addContext(new ShiroSessionScopeContext());
    }
    
    
    private final ShiroSessionScopeContext context = new ShiroSessionScopeContext();
    private static final long serialVersionUID = 1L;
}
