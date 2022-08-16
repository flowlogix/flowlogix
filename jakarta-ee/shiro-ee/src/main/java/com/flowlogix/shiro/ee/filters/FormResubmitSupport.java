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

import static com.flowlogix.shiro.ee.filters.Forms.deleteCookie;
import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static javax.faces.application.StateManager.STATE_SAVING_METHOD_CLIENT;
import static javax.faces.application.StateManager.STATE_SAVING_METHOD_PARAM_NAME;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.HttpMethod;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import javax.ws.rs.core.Response;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import static org.apache.shiro.web.servlet.ShiroHttpSession.DEFAULT_SESSION_ID_NAME;
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
    private static final String FACES_VIEW_STATE_EQUALS = FACES_VIEW_STATE
            + URLEncoder.encode("=", StandardCharsets.UTF_8);
    private static final Pattern VIEW_STATE_PATTERN
            = Pattern.compile("(.*)" + "(" + FACES_VIEW_STATE_EQUALS + "[-]?[\\d]+"
                    + URLEncoder.encode(":", StandardCharsets.UTF_8)
                    + "[-]?[\\d]+)(.*)");
    static final String SHIRO_FORM_DATA = "SHIRO_FORM_DATA";


    @SneakyThrows(IOException.class)
    static void savePostDataForResubmit(ServletRequest request, ServletResponse response, String loginUrl) {
        if (HttpMethod.POST.equalsIgnoreCase(WebUtils.toHttp(request).getMethod())) {
            String postData = request.getReader().lines().collect(Collectors.joining());
            Servlets.addResponseCookie(WebUtils.toHttp(request), WebUtils.toHttp(response),
                    SHIRO_FORM_DATA, postData, null,
                    WebUtils.toHttp(request).getContextPath(),
                    // cookie age = session timeout
                    Servlets.getContext().getSessionTimeout() * 60);
        }
        boolean isGetRequest = HttpMethod.GET.equalsIgnoreCase(WebUtils.toHttp(request).getMethod());
        Servlets.facesRedirect(WebUtils.toHttp(request), WebUtils.toHttp(response),
                Servlets.getRequestBaseURL(WebUtils.toHttp(request))
                + loginUrl.replaceFirst("^/", "") + (isGetRequest? "" : "?%s=true"),
                "sessionExpired");
    }

    static String resubmitSavedForm(@NonNull String savedFormData, @NonNull String savedRequest)
            throws InterruptedException, URISyntaxException, IOException {
        log.debug("saved form data: {}", savedFormData);
        deleteCookie(SHIRO_FORM_DATA);
        HttpClient client = buildHttpClient(savedRequest);
        if (isJSFNewViewStateNeeded(savedFormData)) {
            savedFormData = getJSFNewViewState(savedRequest, client, savedFormData);
        }
        HttpRequest postRequest = HttpRequest.newBuilder().uri(URI.create(savedRequest))
                .POST(HttpRequest.BodyPublishers.ofString(savedFormData))
                .headers(CONTENT_TYPE, APPLICATION_FORM_URLENCODED)
                .build();
        HttpResponse<String> response = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        log.debug("requeust: {}, response: {}", postRequest, response);
        return processResubmitResponse(response, savedRequest);
    }

    private static String processResubmitResponse(HttpResponse<String> response, String savedRequest) throws IOException {
        switch (Response.Status.fromStatusCode(response.statusCode())) {
            case FOUND:
                return response.headers().firstValue(LOCATION).orElseThrow();
            case OK:
                Faces.getResponse().getWriter().append(response.body());
                Faces.responseComplete();
                return null;
            default:
                return savedRequest;
        }
    }

    private static HttpClient buildHttpClient(String savedRequest) throws URISyntaxException {
        CookieManager cookieManager = new CookieManager();
        HttpCookie cookie = new HttpCookie(Servlets.getContext().getSessionCookieConfig().getName() != null
                ? Servlets.getContext().getSessionCookieConfig().getName() : DEFAULT_SESSION_ID_NAME,
                SecurityUtils.getSubject().getSession().getId().toString());
        cookie.setPath(Servlets.getContext().getContextPath());
        cookieManager.getCookieStore().add(new URI(savedRequest), cookie);
        return HttpClient.newBuilder().cookieHandler(cookieManager).build();
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
                savedFormData = matcher.replaceFirst("$1" + FACES_VIEW_STATE_EQUALS
                        + URLEncoder.encode(viewState, StandardCharsets.UTF_8) + "$3");
                log.debug("Encoded w/Replaced ViewState: {}", savedFormData);
            }
        }
        return savedFormData;
    }

    static boolean isJSFStatefulFormForm(@NonNull String savedFormData) {
        var matcher = VIEW_STATE_PATTERN.matcher(savedFormData);
        return matcher.find() && matcher.groupCount() >= 2
                && !matcher.group(2).equalsIgnoreCase("stateless");
    }

    private static boolean isJSFNewViewStateNeeded(@NonNull String savedFormData) {
        return !STATE_SAVING_METHOD_CLIENT.equals(Servlets.getContext()
                .getInitParameter(STATE_SAVING_METHOD_PARAM_NAME))
                && isJSFStatefulFormForm(savedFormData);
    }
}
