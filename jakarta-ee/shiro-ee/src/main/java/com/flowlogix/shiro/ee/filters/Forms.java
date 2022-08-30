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

import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.FORM_IS_RESUBMITTED;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.SESSION_EXPIRED_PARAMETER;
import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import static org.omnifaces.exceptionhandler.ViewExpiredExceptionHandler.wasViewExpired;
import org.omnifaces.util.Faces;

/**
 * Methods to redirect to saved requests upon logout
 * functionality includes saving a previous form state and resubmitting
 * if the form times out
 *
 * @author lprimak
 */
@Slf4j
public class Forms {
    /**
     * Jakarta Faces variant
     * redirect to saved request, possibly resubmitting an existing form
     * the saved request is via a cookie
     *
     * @param useFallbackPath
     * @param fallbackPath
     */
    public static void redirectToSaved(Callable<Boolean> useFallbackPath, String fallbackPath) {
        FormResubmitSupport.redirectToSaved(Faces.getRequest(), Faces.getResponse(), useFallbackPath, fallbackPath, true);
    }

    /**
     * Jakarta Faces variant
     * redirects to current view after a form submit, or a logout, for example
     */
    public static void redirectToView() {
        FormResubmitSupport.redirectToView(Faces.getRequest(), Faces.getResponse());
    }

    /**
     * Faces variant
     * @param useFallback
     * @param fallbackPath
     */
    public static void logout(Callable<Boolean> useFallback, String fallbackPath) {
        logout(Faces.getRequest(), Faces.getResponse(), useFallback, fallbackPath);
    }

    /**
     * makes sure that there is no double-logout
     *
     * @param request
     * @param response
     * @param useFallback
     * @param fallbackPath
     */
    public static void logout(HttpServletRequest request, HttpServletResponse response,
            Callable<Boolean> useFallback, String fallbackPath) {
        if (!Boolean.TRUE.toString().equals(request.getHeader(FORM_IS_RESUBMITTED))) {
            SecurityUtils.getSubject().logout();
            FormResubmitSupport.redirectToView(request, response, useFallback, fallbackPath);
        }
    }

    public static boolean isLoggedIn() {
        var subject = SecurityUtils.getSubject();
        return subject.isAuthenticated() || subject.isRemembered();
    }

    public static boolean isSessionExpired() {
        return wasViewExpired() || Boolean.parseBoolean(Faces.getRequestParameter(SESSION_EXPIRED_PARAMETER));
    }
}
