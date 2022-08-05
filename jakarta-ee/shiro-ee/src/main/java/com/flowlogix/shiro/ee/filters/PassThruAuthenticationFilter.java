/*
 * Copyright 2014 lprimak.
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
package com.flowlogix.shiro.ee.filters;

import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;
import org.omnifaces.util.Servlets;

/**
 * Implements JSF Ajax redirection via OmniFaces
 *
 * @author lprimak
 */
@Slf4j
public class PassThruAuthenticationFilter extends org.apache.shiro.web.filter.authc.PassThruAuthenticationFilter
{
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
    {
        Subject subject = getSubject(request, response);
        return subject.isAuthenticated() || (useRemembered && subject.isRemembered());
    }


    @Override
    protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException
    {
        boolean isGetRequest = HttpMethod.GET.equalsIgnoreCase(WebUtils.toHttp(request).getMethod());
        Servlets.facesRedirect(WebUtils.toHttp(request), WebUtils.toHttp(response),
                Servlets.getRequestBaseURL(WebUtils.toHttp(request))
                + getLoginUrl().replaceFirst("^/", "") + (isGetRequest? "" : "?%s=true"),
                "sessionExpired");
    }


    @Override
    protected boolean isLoginRequest(ServletRequest request, ServletResponse response)
    {
        boolean rv = super.isLoginRequest(request, response);
        if(rv && HttpMethod.GET.equalsIgnoreCase(WebUtils.toHttp(request).getMethod()))
        {
            if(Servlets.getRequestCookie(WebUtils.toHttp(request), WebUtils.SAVED_REQUEST_KEY) == null)
            {
                // only save refer when there is no saved request cookie already,
                // and only as a last resort
                saveRequest(request, response, true);
            }
        }
        return rv;
    }


    @Override
    protected void saveRequestAndRedirectToLogin(ServletRequest request, ServletResponse response) throws IOException
    {
        saveRequest(request, response, false);
        redirectToLogin(request, response);
    }


    private void saveRequest(ServletRequest request, ServletResponse response, boolean useReferer)
    {
        String path = useReferer? getReferer(WebUtils.toHttp(request))
                : Servlets.getRequestURLWithQueryString(WebUtils.toHttp(request));
        if(path == null)
        {
            return;
        }

        Servlets.addResponseCookie(WebUtils.toHttp(request), WebUtils.toHttp(response),
                WebUtils.SAVED_REQUEST_KEY, path, null,
                WebUtils.toHttp(request).getContextPath(), -1);
    }


    static String getReferer(HttpServletRequest request)
    {
        String referer = request.getHeader("referer");
        if (referer != null)
        {
            // do not switch to https if custom port is specified
            if(!referer.matches("^http:\\/\\/[A-z|.|[0-9]]+:[0-9]+(\\/.*|$)"))
            {
                referer = referer.replaceFirst("^http:", "https:");
            }
        }

        return referer;
    }


    @Override
    protected void saveRequest(ServletRequest request)
    {
        throw new UnsupportedOperationException("bad op");
    }


    private @Getter @Setter boolean useRemembered = false;
}
