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
package com.flowlogix.examples.shiro;

import static com.flowlogix.examples.shiro.MyFilter.SHIRO_FORM_DATA;
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
import javax.enterprise.inject.Model;
import static javax.faces.application.StateManager.STATE_SAVING_METHOD_CLIENT;
import static javax.faces.application.StateManager.STATE_SAVING_METHOD_PARAM_NAME;
import javax.servlet.http.Cookie;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import static org.apache.shiro.web.servlet.ShiroHttpSession.DEFAULT_SESSION_ID_NAME;
import org.apache.shiro.web.util.WebUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;
import org.omnifaces.util.Servlets;

/**
 *
 * @author lprimak
 */
@Model
@Getter @Setter
@Slf4j
public class LoginBean {
    @NotBlank
    private String uname;
    @NotBlank
    private String pwd;
    private boolean rememberMe;

    // encoded view state
    static final Pattern VIEW_STATE_PATTERN = Pattern.compile("javax.faces.ViewState=([%\\d\\w:-]+)");
    static final String FACES_VIEW_STATE = "javax.faces.ViewState";
    static final String FACES_VIEW_STATE_EQUALS = FACES_VIEW_STATE + "=";

    public void login() {
        try {
            SecurityUtils.getSubject().login(new UsernamePasswordToken(uname, pwd, rememberMe));
            redirectToSaved();
        } catch (AuthenticationException e) {
            Messages.addFlashGlobalError("Incorrect Login");
            Faces.redirect(Faces.getRequestURLWithQueryString());
        }
    }

    public void logout() {
        SecurityUtils.getSubject().logout();
        redirectToView();
    }

    @SneakyThrows({IOException.class, URISyntaxException.class, InterruptedException.class})
    void redirectToSaved() {
        String savedRequest = Faces.getRequestCookie(WebUtils.SAVED_REQUEST_KEY);
        if (savedRequest != null) {
            var savedRequestCookieToDelete = new Cookie(WebUtils.SAVED_REQUEST_KEY, "tbd");
            savedRequestCookieToDelete.setPath(Servlets.getContext().getContextPath());
            savedRequestCookieToDelete.setMaxAge(0);
            Faces.getResponse().addCookie(savedRequestCookieToDelete);

            String savedFormData = Faces.getRequestCookie(SHIRO_FORM_DATA);
            if (savedFormData != null) {
                log.debug("saved form data: {}", savedFormData);
                var formDataCookieToDelete = new Cookie(SHIRO_FORM_DATA, "tbd");
                formDataCookieToDelete.setPath(Servlets.getContext().getContextPath());
                formDataCookieToDelete.setMaxAge(0);
                Faces.getResponse().addCookie(formDataCookieToDelete);

                CookieManager cookieManager = new CookieManager();
                HttpCookie cookie = new HttpCookie(Servlets.getContext().getSessionCookieConfig().getName() != null
                        ? Servlets.getContext().getSessionCookieConfig().getName() : DEFAULT_SESSION_ID_NAME,
                        SecurityUtils.getSubject().getSession().getId().toString());
                cookie.setPath(Servlets.getContext().getContextPath());
                cookieManager.getCookieStore().add(new URI(savedRequest), cookie);
                HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager)
                        .build();
                if (!STATE_SAVING_METHOD_CLIENT.equals(Servlets.getContext().getInitParameter(STATE_SAVING_METHOD_PARAM_NAME))) {
                    HttpRequest getRequest = HttpRequest.newBuilder()
                            .uri(URI.create(savedRequest))
                            .GET().build();
                    HttpResponse<String> htmlResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
                    if (htmlResponse.statusCode() != 200) {
                        Faces.redirect(savedRequest);
                        return;
                    }
                    Elements elts = Jsoup.parse(htmlResponse.body()).select(String.format("input[name=%s]", FACES_VIEW_STATE));
                    if (!elts.isEmpty()) {
                        String viewState = elts.first().attr("value");

                        var matcher = VIEW_STATE_PATTERN.matcher(savedFormData);
                        if (matcher.find()) {
                            savedFormData = matcher.replaceFirst(FACES_VIEW_STATE_EQUALS
                                    + URLEncoder.encode(viewState, StandardCharsets.UTF_8));
                            log.debug("Encoded w/Replaced ViewState: {}", savedFormData);
                        }
                    }
                }
                HttpRequest postRequest = HttpRequest.newBuilder()
                        .uri(URI.create(savedRequest))
                        .POST(HttpRequest.BodyPublishers.ofString(savedFormData))
                        .headers("Content-Type", "application/x-www-form-urlencoded")
                        .build();
                HttpResponse<String> response = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
                log.debug("requeust: {}, response: {}", postRequest, response);
                switch (response.statusCode()) {
                    case 302:
                        Faces.redirect(response.headers().firstValue("Location").orElseThrow());
                        break;
                    case 200:
                        Faces.getResponse().getWriter().append(response.body());
                        Faces.responseComplete();
                        break;
                    default:
                        Faces.redirect(savedRequest);
                        break;
                }
            } else {
                Faces.redirect(savedRequest);
            }
        } else {
            redirectToView();
        }
    }

    void redirectToView() {
        if (Faces.getViewId().contains("shiro/auth/")) {
            Faces.redirect(Faces.getRequestContextPath());
        } else {
            Faces.redirect(Faces.getRequestURLWithQueryString());
        }
    }
}
