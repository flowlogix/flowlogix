/*
 * Copyright 2012 lprimak.
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
package com.flowlogix.web.services.internal;

import com.flowlogix.session.internal.SessionTrackerHolder;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.util.StringUtils;
import org.apache.shiro.web.util.WebUtils;
import org.apache.tapestry5.internal.services.PageResponseRenderer;
import org.apache.tapestry5.internal.services.RequestPageCache;
import org.apache.tapestry5.internal.structure.Page;
import org.apache.tapestry5.services.Cookies;
import org.apache.tapestry5.services.RequestGlobals;
import org.tynamo.exceptionpage.ExceptionHandlerAssistant;
import org.tynamo.security.services.PageService;
import org.tynamo.security.services.SecurityService;

/**
 * See http://jira.codehaus.org/browse/TYNAMO-121
 * This is a workaround for this bug
 * 
 * @author lprimak
 */
public class ExceptionHandlerAssistantImpl implements ExceptionHandlerAssistant 
{
    public ExceptionHandlerAssistantImpl(SecurityService securityService, PageService pageService, RequestGlobals rg, 
            RequestPageCache pageCache, PageResponseRenderer renderer, Cookies cookies)
    {
        this.securityService = securityService;
        this.pageService = pageService;
        this.rg = rg;
        this.pageCache = pageCache;
        this.renderer = renderer;
        this.cookies = cookies;
    }

    
    @Override
    public String handleRequestException(Throwable exception, List<Object> exceptionContext) throws IOException
    {
        if (securityService.isAuthenticated())
        {
            String unauthorizedPage = pageService.getUnauthorizedPage();
            rg.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (!StringUtils.hasText(unauthorizedPage))
            {
                return null;
            }
            Page page = pageCache.get(unauthorizedPage);
            renderer.renderPageResponse(page);
            return null;
        }
        
        /**
         * Begin Flow Logix Addition
         */
        // do not invoke on Ajax bad sessions
        if (rg.getRequest().isXHR() && SessionTrackerHolder.get().isValidSession(rg.getActivePageName(), rg.getHTTPServletRequest().getSession(false)) == false)
        {
            rg.getRequest().getSession(true).setAttribute("showSessionExpiredMessage", Boolean.TRUE);
        }
        /**
         * End Flow Logix Addition
         */
        
        String contextPath = rg.getHTTPServletRequest().getContextPath();
        if ("".equals(contextPath))
        {
            contextPath = "/";
        }
        cookies.writeCookieValue(WebUtils.SAVED_REQUEST_KEY, WebUtils.getPathWithinApplication(rg.getHTTPServletRequest()), contextPath);
        return pageService.getLoginPage();
    }
    
    
    private final SecurityService securityService;
    private final PageService pageService;
    private final RequestGlobals rg;
    private final RequestPageCache pageCache;
    private final PageResponseRenderer renderer;
    private final Cookies cookies;
}
