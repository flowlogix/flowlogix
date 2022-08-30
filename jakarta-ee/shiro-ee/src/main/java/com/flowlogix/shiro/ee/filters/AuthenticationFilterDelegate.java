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

import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.savePostDataForResubmit;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.saveRequestReferer;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.shiro.subject.Subject;

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
    }

    private final MethodsFromFilter methods;
    private @Getter @Setter boolean useRemembered = false;

    /**
     * added remembered functionality
     *
     * @param request
     * @param response
     * @param mappedValue
     * @return access allowed
     */
    boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
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
    void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
        if (request instanceof HttpServletRequest) {
            savePostDataForResubmit(request, response, methods.getLoginUrl());
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
    boolean isLoginRequest(ServletRequest request, ServletResponse response) {
        boolean rv = methods.isLoginRequest(request, response);
        if (request instanceof HttpServletRequest) {
            saveRequestReferer(rv, request, response);
        }
        return rv;
    }

    /**
     * combine the two because response is unavailable in saveRequest()
     * @param request
     * @param response
     * @throws IOException
     */
    void saveRequestAndRedirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
        if (request instanceof HttpServletRequest) {
            FormResubmitSupport.saveRequest(request, response, false);
        }
        redirectToLogin(request, response);
    }

    void saveRequest(ServletRequest request) {
        throw new UnsupportedOperationException("bad op");
    }
}
