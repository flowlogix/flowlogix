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

import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.resubmitSavedForm;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.web.util.WebUtils;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;

/**
 * Methods to redirect to saved requests upon logout
 * functionality includes saving a previous form state and resubmitting
 * if the form times out
 *
 * @author lprimak
 */
@Slf4j
public class Forms {
    // encoded view state
    static final Pattern VIEW_STATE_PATTERN = Pattern.compile("javax.faces.ViewState=([%\\d\\w:-]+)");
    static final String FACES_VIEW_STATE = "javax.faces.ViewState";
    static final String FACES_VIEW_STATE_EQUALS = FACES_VIEW_STATE + "=";
    static final String SHIRO_FORM_DATA = "SHIRO_FORM_DATA";


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

    static void saveRequest(ServletRequest request, ServletResponse response, boolean useReferer)
    {
        String path = useReferer? getReferer(WebUtils.toHttp(request))
                : Servlets.getRequestURLWithQueryString(WebUtils.toHttp(request));
        if (path != null) {
            Servlets.addResponseCookie(WebUtils.toHttp(request), WebUtils.toHttp(response),
                    WebUtils.SAVED_REQUEST_KEY, path, null,
                    WebUtils.toHttp(request).getContextPath(),
                    // cookie age = session timeout
                    Servlets.getContext().getSessionTimeout() * 60);
        }
    }

    static void saveRequestReferer(boolean rv, ServletRequest request, ServletResponse response) {
        if(rv && HttpMethod.GET.equalsIgnoreCase(WebUtils.toHttp(request).getMethod())) {
            if(Servlets.getRequestCookie(WebUtils.toHttp(request), WebUtils.SAVED_REQUEST_KEY) == null) {
                // only save refer when there is no saved request cookie already,
                // and only as a last resort
                Forms.saveRequest(request, response, true);
            }
        }
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

    /**
     * redirects to current view after a form submit,
     * or the fallback path if predicate succeeds
     *
     * @param useFallbackPath
     * @param fallbackPath
     */
    @SneakyThrows
    static void redirectToView(Callable<Boolean> useFallbackPath, String fallbackPath) {
        if (useFallbackPath.call()) {
            Faces.redirect(fallbackPath);
        } else {
            Faces.redirect(Faces.getRequestURLWithQueryString());
        }
    }

    private static void doRedirectToSaved(@NonNull String savedRequest, boolean resubmit) throws IOException, URISyntaxException, InterruptedException {
        deleteCookie(WebUtils.SAVED_REQUEST_KEY);

        String savedFormData = Faces.getRequestCookie(SHIRO_FORM_DATA);
        if (savedFormData != null && resubmit) {
            Optional.ofNullable(resubmitSavedForm(savedFormData, savedRequest))
                    .ifPresent(Faces::redirect);
        } else {
            Faces.redirect(savedRequest);
        }
    }

    static void deleteCookie(@NonNull String cokieName) {
        var cookieToDelete = new Cookie(cokieName, "tbd");
        cookieToDelete.setPath(Servlets.getContext().getContextPath());
        cookieToDelete.setMaxAge(0);
        Faces.getResponse().addCookie(cookieToDelete);
    }
}
