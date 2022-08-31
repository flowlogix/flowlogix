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

import com.flowlogix.shiro.ee.filters.AuthenticationFilterDelegate.MethodsFromFilter;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.redirectToSaved;
import com.flowlogix.shiro.ee.filters.Forms.FallbackPredicate;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;

/**
 * Implements JSF Ajax redirection via OmniFaces
 * Implements form resubmit and auto remember-me functionality
 *
 * @author lprimak
 */
public class FormAuthenticationFilter extends org.apache.shiro.web.filter.authc.FormAuthenticationFilter {
    private final @Delegate AuthenticationFilterDelegate delegate;
    private static final FallbackPredicate NO_PREDICATE = (path, request) -> false;
    private @Getter @Setter FallbackPredicate fallbackType = NO_PREDICATE;

    private class Methods implements MethodsFromFilter {
        @Override
        public Subject getSubject(ServletRequest request, ServletResponse response) {
            return FormAuthenticationFilter.super.getSubject(request, response);
        }

        @Override
        public boolean isLoginRequest(ServletRequest request, ServletResponse response) {
            return FormAuthenticationFilter.super.isLoginRequest(request, response);
        }

        @Override
        public String getLoginUrl() {
            return FormAuthenticationFilter.super.getLoginUrl();
        }
    };

    public FormAuthenticationFilter() {
        delegate = new AuthenticationFilterDelegate(new Methods());
    }

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) throws Exception {
        if (request instanceof HttpServletRequest) {
            redirectToSaved(WebUtils.toHttp(request), WebUtils.toHttp(response), fallbackType::useFallback,
                    request.getServletContext().getContextPath());
        }
        return false;
    }
}
