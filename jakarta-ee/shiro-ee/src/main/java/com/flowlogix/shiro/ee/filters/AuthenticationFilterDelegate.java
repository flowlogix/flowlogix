/*
 * Copyright 2022 lprimak.
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

import static com.flowlogix.shiro.ee.filters.FormAuthenticationFilter.LOGIN_PREDICATE_ATTR_NAME;
import static com.flowlogix.shiro.ee.filters.FormAuthenticationFilter.NO_PREDICATE;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.savePostDataForResubmit;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.saveRequestReferer;
import com.flowlogix.shiro.ee.filters.Forms.FallbackPredicate;
import static com.flowlogix.shiro.ee.filters.LogoutFilter.LOGOUT_PREDICATE_ATTR_NAME;
import static com.flowlogix.shiro.ee.filters.LogoutFilter.YES_PREDICATE;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;

/**
 * common functionality for both Form and PassThru authentication filters
 *
 * @author lprimak
 */
@RequiredArgsConstructor
class AuthenticationFilterDelegate {
    interface MethodsFromFilter {
        Subject getSubject(ServletRequest request, ServletResponse response);
        boolean isLoginRequest(ServletRequest request, ServletResponse response);
        String getLoginUrl();
        boolean preHandle(ServletRequest request, ServletResponse response) throws Exception;
    }

    private final MethodsFromFilter methods;
    private @Getter @Setter boolean useRemembered = false;
    private @Getter @Setter FallbackPredicate loginFallbackType = NO_PREDICATE;
    private @Getter @Setter FallbackPredicate logoutFallbackType = YES_PREDICATE;

    public boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        request.setAttribute(LOGIN_PREDICATE_ATTR_NAME, loginFallbackType);
        request.setAttribute(LOGOUT_PREDICATE_ATTR_NAME, logoutFallbackType);
        return methods.preHandle(request, response);
    }

    /**
     * added remembered functionality
     *
     * @param request
     * @param response
     * @param mappedValue
     * @return access allowed
     */
    public boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        Subject subject = methods.getSubject(request, response);
        boolean isAuthenticated = subject.isAuthenticated() && subject.getPrincipal() != null;
        return isAuthenticated || (useRemembered && subject.isRemembered());
    }

    /**
     * added form save for resubmit functionality
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
        if (request instanceof HttpServletRequest) {
            savePostDataForResubmit(WebUtils.toHttp(request), WebUtils.toHttp(response),
                    methods.getLoginUrl());
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
    public boolean isLoginRequest(ServletRequest request, ServletResponse response) {
        boolean rv = methods.isLoginRequest(request, response);
        if (request instanceof HttpServletRequest) {
            saveRequestReferer(rv, WebUtils.toHttp(request), WebUtils.toHttp(response));
        }
        return rv;
    }

    /**
     * combine the two because response is unavailable in saveRequest()
     * @param request
     * @param response
     * @throws IOException
     */
    public void saveRequestAndRedirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
        if (request instanceof HttpServletRequest) {
            FormResubmitSupport.saveRequest(WebUtils.toHttp(request), WebUtils.toHttp(response), false);
        }
        redirectToLogin(request, response);
    }

    public void saveRequest(ServletRequest request) {
        throw new UnsupportedOperationException("bad op");
    }
}
