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
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.doRedirectToSaved;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.util.WebUtils;
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
     * redirect to saved request, possibly resubmitting an existing form
     * the saved request is via a cookie
     *
     * @param useFallbackPath
     * @param fallbackPath
     */
    public static void redirectToSaved(Callable<Boolean> useFallbackPath, String fallbackPath) {
        redirectToSaved(useFallbackPath, fallbackPath, true);
    }

    /**
     * Redirects the user to saved request after login, if available
     * Resumbits the form that caused the logout upon successfull login.Form resumnission supports JSF and Ajax forms
     * @param useFallbackPath predicate whether to use fall back path
     * @param fallbackPath
     * @param resubmit if true, attempt to resubmit the form that was unsubmitted prior to logout
     */
    @SneakyThrows({IOException.class, URISyntaxException.class, InterruptedException.class})
    public static void redirectToSaved(Callable<Boolean> useFallbackPath, String fallbackPath, boolean resubmit) {
        String savedRequest = Faces.getRequestCookie(WebUtils.SAVED_REQUEST_KEY);
        if (savedRequest != null) {
            doRedirectToSaved(savedRequest, resubmit);
        } else {
            redirectToView(useFallbackPath, fallbackPath);
        }
    }

    /**
     * redirects to current view after a form submit, or a logout, for example
     */
    public static void redirectToView() {
        redirectToView(() -> false, null);
    }

    /**
     * redirects to current view after a form submit,
     * or the fallback path if predicate succeeds
     *
     * @param useFallbackPath
     * @param fallbackPath
     */
    @SneakyThrows
    public static void redirectToView(Callable<Boolean> useFallbackPath, String fallbackPath) {
        if (useFallbackPath.call()) {
            Faces.redirect(fallbackPath);
        } else {
            Faces.redirect(Faces.getRequestURLWithQueryString());
        }
    }

    /**
     * makes sure that there is no double-logout
     *
     * @param useFallback
     * @param fallbackPath
     */
    public static void logout(Callable<Boolean> useFallback, String fallbackPath) {
        if (!Boolean.TRUE.toString().equals(Faces.getRequestHeader(FORM_IS_RESUBMITTED))) {
            SecurityUtils.getSubject().logout();
            redirectToView(useFallback, Faces.getRequestContextPath());
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
