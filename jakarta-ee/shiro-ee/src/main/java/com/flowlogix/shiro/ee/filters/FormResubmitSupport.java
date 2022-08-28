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

import static com.flowlogix.shiro.ee.cdi.ShiroScopeContext.isWebContainerSessions;
import com.flowlogix.shiro.ee.filters.ShiroFilter.WrappedSecurityManager;
import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static java.util.function.Predicate.not;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static javax.faces.application.StateManager.STATE_SAVING_METHOD_CLIENT;
import static javax.faces.application.StateManager.STATE_SAVING_METHOD_PARAM_NAME;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import javax.ws.rs.core.Response;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.SessionsSecurityManager;
import static org.apache.shiro.web.servlet.ShiroHttpSession.DEFAULT_SESSION_ID_NAME;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;

/**
 * supporting methods for {@link Forms}
 *
 * @author lprimak
 */
@Slf4j
public class FormResubmitSupport {
    // encoded view state
    private static final String FACES_VIEW_STATE = "javax.faces.ViewState";
    private static final String FACES_VIEW_STATE_EQUALS = FACES_VIEW_STATE + "=";
    private static final Pattern VIEW_STATE_PATTERN
            = Pattern.compile(String.format("(.*)(%s[-]?[\\d]+:[-]?[\\d]+)(.*)", FACES_VIEW_STATE_EQUALS));
    private static final String PARTIAL_VIEW = "javax.faces.partial";
    private static final Pattern PARTIAL_REQUEST_PATTERN
            = Pattern.compile(String.format("[\\&]?%s.\\w+=[\\w\\s:%%\\d]*", PARTIAL_VIEW));
    private static final String FORM_DATA_CACHE = "com.flowlogix.form-data-cache";
    static final String SHIRO_FORM_DATA_KEY = "com.flowlogix.form-data-key";
    static final String SESSION_EXPIRED_PARAMETER = "com.flowlogix.sessionExpired";
    static final String FORM_IS_RESUBMITTED = "com.flowlogix.form-is-resubmitted";


    static void savePostDataForResubmit(ServletRequest request, ServletResponse response, String loginUrl) {
        if (isPostRequest(request) && unwrapSecurityManager(SecurityUtils.getSecurityManager())
                instanceof DefaultSecurityManager) {
            String postData = getPostData(request);
            var cacheKey = UUID.randomUUID();
            var dsm = (DefaultSecurityManager) unwrapSecurityManager(SecurityUtils.getSecurityManager());
            dsm.getCacheManager().getCache(FORM_DATA_CACHE).put(cacheKey, postData);
            addCookie(WebUtils.toHttp(response), SHIRO_FORM_DATA_KEY,
                    cacheKey.toString(), Servlets.getContext().getSessionTimeout() * 60);
        }
        boolean isGetRequest = HttpMethod.GET.equalsIgnoreCase(WebUtils.toHttp(request).getMethod());
        Servlets.facesRedirect(WebUtils.toHttp(request), WebUtils.toHttp(response),
                Servlets.getRequestBaseURL(WebUtils.toHttp(request))
                + loginUrl.replaceFirst("^/", "") + (isGetRequest? "" : "?%s=true"),
                SESSION_EXPIRED_PARAMETER);
    }

    static boolean isPostRequest(ServletRequest request) {
        return HttpMethod.POST.equalsIgnoreCase(WebUtils.toHttp(request).getMethod());
    }

    @SneakyThrows(IOException.class)
    static String getPostData(ServletRequest request) {
        return request.getReader().lines().collect(Collectors.joining());
    }

    static String getSavedFormDataFromKey(@NonNull String savedFormDataKey) {
        String savedFormData = null;
        if (unwrapSecurityManager(SecurityUtils.getSecurityManager()) instanceof DefaultSecurityManager) {
            var dsm = (DefaultSecurityManager) unwrapSecurityManager(SecurityUtils.getSecurityManager());
            var cache = dsm.getCacheManager().getCache(FORM_DATA_CACHE);
            var cacheKey = UUID.fromString(savedFormDataKey);
            savedFormData = (String)cache.get(cacheKey);
            cache.remove(cacheKey);
        }
        return savedFormData;
    }

    static void saveRequest(ServletRequest request, ServletResponse response, boolean useReferer) {
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
                saveRequest(request, response, true);
            }
        }
    }

    static String getReferer(HttpServletRequest request) {
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

    static void doRedirectToSaved(@NonNull String savedRequest, boolean resubmit) throws IOException, URISyntaxException, InterruptedException {
        deleteCookie(Faces.getResponse(), WebUtils.SAVED_REQUEST_KEY);
        Cookie formDataCookie = (Cookie)Faces.getExternalContext().getRequestCookieMap().get(SHIRO_FORM_DATA_KEY);
        String savedFormDataKey = formDataCookie == null ? null : formDataCookie.getValue();
        boolean doRedirectAtEnd = true;
        if (savedFormDataKey != null && resubmit) {
            String formData = getSavedFormDataFromKey(savedFormDataKey);
            if (formData != null) {
                Optional.ofNullable(resubmitSavedForm(formData, savedRequest,
                        Faces.getResponse(), Faces.getServletContext(), false))
                        .ifPresent(Faces::redirect);
                doRedirectAtEnd = false;
            } else {
                deleteCookie(Faces.getResponse(), SHIRO_FORM_DATA_KEY);
            }
        }
        if (doRedirectAtEnd) {
            Faces.redirect(savedRequest);
        }
    }

    static void addCookie(@NonNull HttpServletResponse response,
            @NonNull String cokieName, @NonNull String cookieValue, int maxAge) {
        var cookie = new Cookie(cokieName, cookieValue);
        cookie.setPath(Servlets.getContext().getContextPath());
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    static void deleteCookie(@NonNull HttpServletResponse response,
            @NonNull String cokieName) {
        var cookieToDelete = new Cookie(cokieName, "tbd");
        cookieToDelete.setPath(Servlets.getContext().getContextPath());
        cookieToDelete.setMaxAge(0);
        response.addCookie(cookieToDelete);
    }

    static String resubmitSavedForm(@NonNull String savedFormData, @NonNull String savedRequest,
            HttpServletResponse originalResponse, ServletContext servletContext, boolean rememberedAjaxResubmit)
            throws InterruptedException, URISyntaxException, IOException {
        log.debug("saved form data: {}", savedFormData);
        HttpClient client = buildHttpClient(savedRequest, servletContext);
        String decodedFormData = parseFormData(savedFormData, savedRequest, client, servletContext);
        HttpRequest postRequest = HttpRequest.newBuilder().uri(URI.create(savedRequest))
                .POST(HttpRequest.BodyPublishers.ofString(decodedFormData))
                .headers(CONTENT_TYPE, APPLICATION_FORM_URLENCODED,
                        FORM_IS_RESUBMITTED, Boolean.TRUE.toString())
                .build();
        HttpResponse<String> response = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        log.debug("Resubmit request: {}, response: {}", postRequest, response);
        if (rememberedAjaxResubmit) {
            HttpRequest redirectRequest = HttpRequest.newBuilder().uri(URI.create(savedRequest))
                    .POST(HttpRequest.BodyPublishers.ofString(savedFormData))
                    .headers(CONTENT_TYPE, APPLICATION_FORM_URLENCODED)
                    .build();
            var redirectResponse = client.send(redirectRequest, HttpResponse.BodyHandlers.ofString());
            log.debug("Redirect request: {}, response: {}", redirectRequest, redirectResponse);
            return processResubmitResponse(redirectResponse, originalResponse, response.headers(), savedRequest, servletContext);
        } else {
            deleteCookie(originalResponse, SHIRO_FORM_DATA_KEY);
            return processResubmitResponse(response, originalResponse, response.headers(), savedRequest, servletContext);
        }
    }

    private static String parseFormData(String savedFormData, String savedRequest,
            HttpClient client, ServletContext servletContext) throws IOException, InterruptedException {
        if (!isJSFClientStateSavingMethod(servletContext)) {
            String decodedFormData = URLDecoder.decode(savedFormData, StandardCharsets.UTF_8);
            if (isJSFStatefulForm(decodedFormData)) {
                savedFormData = getJSFNewViewState(savedRequest, client, decodedFormData);
            }
        }
        return noJSFAjaxRequests(savedFormData);
    }

    @SuppressWarnings("fallthrough")
    private static String processResubmitResponse(HttpResponse<String> response, HttpServletResponse originalResponse,
            HttpHeaders headers, String savedRequest, ServletContext servletContext) throws IOException {
        switch (Response.Status.fromStatusCode(response.statusCode())) {
            case FOUND:
                // can't use Faces.redirect() here
                originalResponse.setStatus(response.statusCode());
                originalResponse.setHeader(LOCATION, response.headers().firstValue(LOCATION).orElseThrow());
            case OK:
                // do not duplicate the session cookie
                transformCookieHeader(headers.allValues(SET_COOKIE))
                        .entrySet().stream().filter(not(entry -> entry.getKey().equals(getSessionCookieName(servletContext))))
                        .forEach(entry -> addCookie(originalResponse, entry.getKey(), entry.getValue(), -1));
                originalResponse.getWriter().append(response.body());
                if (Faces.hasContext()) {
                    Faces.responseComplete();
                }
                return null;
            default:
                return savedRequest;
        }
    }

    static Map<String, String> transformCookieHeader(@NonNull List<String> cookies) {
        return cookies.stream().map(s -> s.split("[=;]"))
                .collect(Collectors.toMap(k -> k[0], v -> (v.length > 1) ? v[1] : ""));
    }

    private static HttpClient buildHttpClient(String savedRequest, ServletContext servletContext) throws URISyntaxException {
        CookieManager cookieManager = new CookieManager();
        HttpCookie cookie = new HttpCookie(getSessionCookieName(servletContext),
                SecurityUtils.getSubject().getSession().getId().toString());
        cookie.setPath(Servlets.getContext().getContextPath());
        cookieManager.getCookieStore().add(new URI(savedRequest), cookie);
        return HttpClient.newBuilder().cookieHandler(cookieManager).build();
    }

    private static String getSessionCookieName(ServletContext context) {
        if (!isWebContainerSessions(SecurityUtils.getSecurityManager()) && getNativeSessionManager() != null) {
            return getNativeSessionManager().getSessionIdCookie().getName();
        } else {
            return context.getSessionCookieConfig().getName() != null
                    ? context.getSessionCookieConfig().getName() : DEFAULT_SESSION_ID_NAME;
        }
    }

    private static DefaultWebSessionManager getNativeSessionManager() {
        DefaultWebSessionManager rv = null;
        SecurityManager securityManager = unwrapSecurityManager(SecurityUtils.getSecurityManager());
        if (securityManager instanceof SessionsSecurityManager) {
            var ssm = (SessionsSecurityManager) securityManager;
            var sm = ssm.getSessionManager();
            if (sm instanceof DefaultWebSessionManager) {
                rv = (DefaultWebSessionManager) sm;
            }
        }
        return rv;
    }

    private static org.apache.shiro.mgt.SecurityManager unwrapSecurityManager(SecurityManager securityManager) {
        if (securityManager instanceof WrappedSecurityManager) {
            WrappedSecurityManager wsm = (WrappedSecurityManager)securityManager;
            return wsm.wrapped;
        } else {
            return securityManager;
        }
    }

    private static String getJSFNewViewState(String savedRequest, HttpClient client, String savedFormData)
            throws IOException, InterruptedException {
        var getRequest = HttpRequest.newBuilder().uri(URI.create(savedRequest)).GET().build();
        HttpResponse<String> htmlResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        if (htmlResponse.statusCode() == Response.Status.OK.getStatusCode()) {
            savedFormData = extractJSFNewViewState(htmlResponse.body(), savedFormData);
        }
        return savedFormData;
    }

    static String extractJSFNewViewState(@NonNull String responseBody, @NonNull String savedFormData) {
        Elements elts = Jsoup.parse(responseBody).select(String.format("input[name=%s]", FACES_VIEW_STATE));
        if (!elts.isEmpty()) {
            String viewState = elts.first().attr("value");

            var matcher = VIEW_STATE_PATTERN.matcher(savedFormData);
            if (matcher.matches()) {
                savedFormData = matcher.replaceFirst(String.format("$1%s%s$3",
                        FACES_VIEW_STATE_EQUALS, viewState));
                log.debug("Encoded w/Replaced ViewState: {}", savedFormData);
            }
        }
        return savedFormData;
    }

    static String noJSFAjaxRequests(String savedFormData) {
        return PARTIAL_REQUEST_PATTERN.matcher(savedFormData).replaceAll("");
    }

    static boolean isJSFStatefulForm(@NonNull String savedFormData) {
        var matcher = VIEW_STATE_PATTERN.matcher(savedFormData);
        return matcher.find() && matcher.groupCount() >= 2
                && !matcher.group(2).equalsIgnoreCase("stateless");
    }

    static boolean isJSFClientStateSavingMethod(ServletContext servletContext) {
        return STATE_SAVING_METHOD_CLIENT.equals(
                servletContext.getInitParameter(STATE_SAVING_METHOD_PARAM_NAME));
    }
}
