/*
 * Copyright 2015 lprimak2.
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

import java.util.Optional;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.servlet.ShiroHttpSession;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;
import org.jboss.weld.servlet.api.ServletListener;

/**
 *
 * Weld lifecycle listener and forwarder Makes possible for Weld to use Shiro native sessions
 *
 * @author lprimak
 */
public class WeldShiroNativeSession implements ServletRequestListener, ServletContextListener,
        SessionListener
{
    interface Action<T>
    {
        public void run(T param);
    }

    @Override
    public void onStart(Session session)
    {
        getListener(servletContext).sessionCreated(new HttpSessionEvent(toHttp(session)));
    }

    @Override
    public void onStop(Session session)
    {
        getListener(servletContext).sessionDestroyed(new HttpSessionEvent(toHttp(session)));
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre)
    {
        withSubject(sre.getServletRequest(), evt -> getListener(sre.getServletContext())
                .requestDestroyed(evt));
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre)
    {
        withSubject(sre.getServletRequest(), evt -> getListener(sre.getServletContext())
                .requestInitialized(evt));
    }

    @Override
    public void onExpiration(Session session)
    {
        onStop(session);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        DefaultWebSessionManager dwsm = (DefaultWebSessionManager) getSecurityManager(sce.getServletContext()).getSessionManager();
        dwsm.getSessionListeners().stream().filter(it -> it instanceof WeldShiroNativeSession)
                .map(it -> (WeldShiroNativeSession) it)
                .forEach(it -> it.servletContext = sce.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        // intensionally left blank
    }

    private ServletListener getListener(ServletContext ctx)
    {
        return (ServletListener) ctx.getAttribute(
                Optional.ofNullable(ctx.getInitParameter(DEFAULT_WELD_LISTENER_PARAM))
                        .orElse(DEFAULT_WELD_LISTENER_CLASS));
    }

    private HttpSession toHttp(Session session)
    {
        return new ShiroHttpSession(session, null, servletContext);
    }

    private DefaultWebSecurityManager getSecurityManager(ServletContext context)
    {
        return (DefaultWebSecurityManager) WebUtils
                .getRequiredWebEnvironment(context).getSecurityManager();
    }

    /**
     * run an action with a subject context need to get a native Shiro session from a web session, since we are outside
     * of Shiro filter's purvue here
     *
     * @param request
     * @param action
     */
    private void withSubject(ServletRequest request, Action<ServletRequestEvent> action)
    {
        ThreadContext.bind(getSecurityManager(request.getServletContext()));
        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        ServletRequestEvent evt = new ServletRequestEvent(request.getServletContext(),
                new ShiroHttpServletRequest(httpRequest, request.getServletContext(), false));
        Subject.Builder subjectBuilder = new Subject.Builder();
        String sessionId = blankSessionManager.getSessionIdCookie().readValue(httpRequest, null);
        if (sessionId != null)
        {
            subjectBuilder.sessionId(sessionId);
        }
        ThreadContext.bind(subjectBuilder.buildSubject());
        action.run(evt);
        ThreadContext.unbindSubject();
        ThreadContext.unbindSecurityManager();
    }

    
    private final DefaultWebSessionManager blankSessionManager = new DefaultWebSessionManager();
    private ServletContext servletContext;
    public final String DEFAULT_WELD_LISTENER_PARAM = "org.jboss.weld.servlet.WeldInitialListener";
    private final String DEFAULT_WELD_LISTENER_CLASS = "org.jboss.weld.servlet.WeldInitialListener";
}
