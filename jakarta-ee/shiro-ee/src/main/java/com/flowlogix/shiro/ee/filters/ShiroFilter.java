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
import java.security.Principal;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import static org.apache.shiro.web.filter.authz.SslFilter.HTTPS_SCHEME;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.util.WebUtils;
import org.omnifaces.util.Lazy;

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
public class ShiroFilter extends org.apache.shiro.web.servlet.ShiroFilter {
    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final Pattern HTTP_TO_HTTPS = Pattern.compile("^\\s*http(.*)");
    private @Inject ShiroSessionScopeExtension ssse;

    private class WrappedRequest extends ShiroHttpServletRequest {
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


    @Override
    protected ServletRequest wrapServletRequest(HttpServletRequest orig) {
        return new WrappedRequest(orig, getServletContext(), isHttpSessions());
    }

    @Override
    public void init() throws Exception {
        super.init();
        if(!ShiroScopeContext.isWebContainerSessions(super.getSecurityManager())) {
            DefaultSecurityManager dsm = (DefaultSecurityManager)super.getSecurityManager();
            DefaultSessionManager sm = (DefaultSessionManager)dsm.getSessionManager();
            ssse.addDestroyHandlers(sm.getSessionListeners(), dsm);
        }
    }
}
