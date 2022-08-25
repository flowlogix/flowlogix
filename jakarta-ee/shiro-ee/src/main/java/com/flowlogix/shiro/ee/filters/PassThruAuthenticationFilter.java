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

import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.savePostDataForResubmit;
import static com.flowlogix.shiro.ee.filters.Forms.saveRequestReferer;
import java.io.IOException;
import javax.faces.application.ViewExpiredException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.Subject;
import static org.omnifaces.exceptionhandler.ViewExpiredExceptionHandler.FLASH_ATTRIBUTE_VIEW_EXPIRED;

/**
 * Implements JSF Ajax redirection via OmniFaces
 *
 * @author lprimak
 */
@Slf4j
public class PassThruAuthenticationFilter extends org.apache.shiro.web.filter.authc.PassThruAuthenticationFilter
{
    private @Getter @Setter boolean useRemembered = false;

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
    {
        Subject subject = getSubject(request, response);
        return subject.isAuthenticated() || (useRemembered && subject.isRemembered());
    }

    @Override
    protected void postHandle(ServletRequest request, ServletResponse response) throws Exception {
        if (Boolean.TRUE.equals(request.getAttribute(FLASH_ATTRIBUTE_VIEW_EXPIRED))) {
            Subject subject = getSubject(request, response);
            log.error("resubmit the form, session = {}", subject.getSession(false));
        }
    }

    @Override
    protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException
    {
        savePostDataForResubmit(request, response, getLoginUrl());
    }

    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            super.doFilterInternal(request, response, chain);
        } catch (ViewExpiredException e) {
            Subject subject = getSubject(request, response);
            log.error("resubmit the form, session = {}", subject.getSession(false));
        }
    }

    /**
     * in case the login link is clicked directly,
     * redirect to referer
     *
     * @param request
     * @param response
     * @return
     */
    @Override
    protected boolean isLoginRequest(ServletRequest request, ServletResponse response)
    {
        boolean rv = super.isLoginRequest(request, response);
        saveRequestReferer(rv, request, response);
        return rv;
    }


    @Override
    protected void saveRequestAndRedirectToLogin(ServletRequest request, ServletResponse response) throws IOException
    {
        Forms.saveRequest(request, response, false);
        redirectToLogin(request, response);
    }


    @Override
    protected void saveRequest(ServletRequest request)
    {
        throw new UnsupportedOperationException("bad op");
    }
}
