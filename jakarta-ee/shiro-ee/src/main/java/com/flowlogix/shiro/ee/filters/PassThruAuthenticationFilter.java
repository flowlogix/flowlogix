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
import static com.flowlogix.shiro.ee.filters.FormSupport.saveRequestReferer;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.Subject;

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
    protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException
    {
        savePostDataForResubmit(request, response, getLoginUrl());
    }


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
        FormSupport.saveRequest(request, response, false);
        redirectToLogin(request, response);
    }


    @Override
    protected void saveRequest(ServletRequest request)
    {
        throw new UnsupportedOperationException("bad op");
    }
}
