/*
 * Copyright 2015 lprimak.
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

import com.flowlogix.shiro.ee.cdi.ShiroScopeContext;
import com.flowlogix.shiro.ee.cdi.ShiroSessionScopeExtension;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.DONT_ADD_ANY_MORE_COOKIES;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.FORM_IS_RESUBMITTED;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.getNativeSessionManager;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.getPostData;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.isJSFClientStateSavingMethod;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.isPostRequest;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.resubmitSavedForm;
import static com.flowlogix.shiro.ee.listeners.EnvironmentLoaderListener.isShiroEEDisabled;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import static org.apache.shiro.web.filter.authz.SslFilter.HTTPS_SCHEME;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.session.mgt.WebSessionKey;
import org.apache.shiro.web.subject.WebSubjectContext;
import org.apache.shiro.web.util.WebUtils;
import org.omnifaces.util.Lazy;
import org.omnifaces.util.Servlets;

/**
 * Stops JEE server from interpreting Shiro principal as direct EJB principal,
 * this has sideffects of trying to log in to remote EJBs with the credentials from Shiro,
 * which isn't what this meant to do, as it's meant to just transfer Shiro credentials
 * to remote EJB call site.
 *
 * Thus, force null EJB principal for the web session,
 * as the real principal comes from the EjbSecurityFilter's doAs() call
 *
 * Also handles X-Forwarded-Proto support
 *
 * @author lprimak
 */
@Slf4j
@WebFilter(filterName = "ShiroFilter", urlPatterns = "/*",
        dispatcherTypes = { DispatcherType.ERROR, DispatcherType.FORWARD,
            DispatcherType.INCLUDE, DispatcherType.REQUEST,
            DispatcherType.ASYNC }, asyncSupported = true)
public class ShiroFilter extends org.apache.shiro.web.servlet.ShiroFilter {
    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final Pattern HTTP_TO_HTTPS = Pattern.compile("^\\s*http(.*)");
    private @Inject ShiroSessionScopeExtension ssse;

    private static class WrappedRequest extends ShiroHttpServletRequest {
        private final Lazy<Boolean> httpsNeeded = new Lazy<>(this::isHttpButNeedHttps);
        private final Lazy<StringBuffer> requestURL = new Lazy<>(this::rewriteHttpToHttps);

        public WrappedRequest(HttpServletRequest wrapped, ServletContext servletContext, boolean httpSessions) {
            super(wrapped, servletContext, httpSessions);
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getScheme() {
            if (httpsNeeded.get()) {
                return HTTPS_SCHEME;
            } else {
                return super.getScheme();
            }
        }

        @Override
        public StringBuffer getRequestURL() {
            if (httpsNeeded.get()) {
                return requestURL.get();
            } else {
                return super.getRequestURL();
            }
        }

        @Override
        public boolean isSecure() {
            return super.isSecure() || httpsNeeded.get();
        }

        private boolean isHttpButNeedHttps() {
            return !HTTPS_SCHEME.equalsIgnoreCase(super.getScheme())
                    && HTTPS_SCHEME.equalsIgnoreCase(WebUtils.toHttp(getRequest())
                            .getHeader(X_FORWARDED_PROTO));
        }

        private StringBuffer rewriteHttpToHttps() {
            return new StringBuffer(HTTP_TO_HTTPS.matcher(super.getRequestURL())
                    .replaceFirst(HTTPS_SCHEME + "$1"));
        }
    }

    private static class WrappedResponse extends HttpServletResponseWrapper {
        private final HttpServletRequest request;

        public WrappedResponse(HttpServletResponse response, HttpServletRequest request) {
            super(response);
            this.request = request;
        }

        @Override
        public void addCookie(Cookie cookie) {
            if (request.getAttribute(DONT_ADD_ANY_MORE_COOKIES) != Boolean.TRUE) {
                super.addCookie(cookie);
            }
        }
    }

    @RequiredArgsConstructor
    static class WrappedSecurityManager implements WebSecurityManager {
        final @Delegate WebSecurityManager wrapped;

        @Override
        public Subject createSubject(SubjectContext context) {
            if (context instanceof WebSubjectContext && wrapped instanceof DefaultSecurityManager) {
                WebSubjectContext webContext = (WebSubjectContext) context;
                DefaultWebSecurityManager wsm = (DefaultWebSecurityManager) wrapped;
                Session session = null;
                try {
                    session = wsm.getSession(new WebSessionKey(webContext.getSessionId(), webContext.getServletRequest(),
                            webContext.getServletResponse()));
                } catch (SessionException e) {
                    log.debug("Create Session Failed", e);
                }
                var newSubject = wrapped.createSubject(context);
                if (newSubject.isRemembered() && session == null
                        && !isJSFClientStateSavingMethod(webContext.getServletRequest().getServletContext())) {
                    log.debug("Remembered Subject with new session {}", newSubject.getPrincipal());
                    webContext.getServletRequest().setAttribute(FORM_IS_RESUBMITTED, Boolean.TRUE);
                }
                return newSubject;
            } else {
                return wrapped.createSubject(context);
            }
        }
    }

    @Override
    protected ServletRequest wrapServletRequest(HttpServletRequest orig) {
        return new WrappedRequest(orig, getServletContext(), isHttpSessions());
    }

    @Override
    public void init() throws Exception {
        if (isShiroEEDisabled()) {
            return;
        }
        super.init();
        WebSecurityManager wsm = super.getSecurityManager();
        if(!ShiroScopeContext.isWebContainerSessions(wsm)) {
            var dsm = getNativeSessionManager(wsm);
            ssse.addSessionListeners(dsm.getSessionListeners(), wsm);
        }
    }

    @Override
    public void setSecurityManager(WebSecurityManager sm) {
        super.setSecurityManager(new WrappedSecurityManager(sm));
    }

    @Override
    @SneakyThrows
    protected void executeChain(ServletRequest request, ServletResponse origResponse,
            FilterChain origChain) throws IOException, ServletException {
        var response = new WrappedResponse(WebUtils.toHttp(origResponse), WebUtils.toHttp(request));
        if (isShiroEEDisabled()) {
            origChain.doFilter(request, origResponse);
        } else if (Boolean.TRUE.equals(request.getAttribute(FORM_IS_RESUBMITTED)) && isPostRequest(request)) {
            request.removeAttribute(FORM_IS_RESUBMITTED);
            String postData = getPostData(request);
            log.debug("Resubmitting Post Data: {}", postData);
            var httpRequest = WebUtils.toHttp(request);
            boolean rememberedAjaxResubmit = "partial/ajax".equals(httpRequest.getHeader("Faces-Request"));
            Optional.ofNullable(resubmitSavedForm(postData,
                    Servlets.getRequestURLWithQueryString(httpRequest),
                    WebUtils.toHttp(request), WebUtils.toHttp(response),
                    request.getServletContext(), rememberedAjaxResubmit))
                    .ifPresent(url -> sendRedirect(response, url));
        } else {
            super.executeChain(request, response, origChain);
        }
    }

    @SneakyThrows(IOException.class)
    private static void sendRedirect(ServletResponse response, String url) {
        WebUtils.toHttp(response).sendRedirect(url);
    }
}
