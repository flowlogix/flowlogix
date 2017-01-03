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
package com.flowlogix.security;

import com.flowlogix.security.cdi.ShiroScopeContext;
import com.flowlogix.security.cdi.ShiroSessionScopeExtension;
import java.security.Principal;
import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;

/**
 * Stops JEE server from interpreting Shiro principal as direct EJB principal,
 * this has sideffects of trying to log in to remote EJBs with the credentials from Shiro,
 * which isn't what this meant to do, as it's meant to just transfer Shiro credentials
 * to remote EJB call site.
 * 
 * Thus, force null EJB principal for the web session,
 * as the real principal comes from the EjbSecurityFilter's doAs() call
 * 
 * @author lprimak
 */
public class ShiroFilter extends org.apache.shiro.web.servlet.ShiroFilter
{
    @Override
    protected ServletRequest wrapServletRequest(HttpServletRequest orig)
    {
        return new ShiroHttpServletRequest(orig, getServletContext(), isHttpSessions()) 
        {
            @Override
            public Principal getUserPrincipal()
            {
                return null;
            }
        };
    }

    @Override
    public void init() throws Exception
    {
        super.init();
        if(!ShiroScopeContext.isWebContainerSessions(super.getSecurityManager())) {
            DefaultSecurityManager dsm = (DefaultSecurityManager)super.getSecurityManager();
            DefaultSessionManager sm = (DefaultSessionManager)dsm.getSessionManager();
            ssse.addDestroyHandlers(sm.getSessionListeners(), dsm);
        }
    }


    private @Inject ShiroSessionScopeExtension ssse;
}
