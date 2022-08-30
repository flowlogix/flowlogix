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

import com.flowlogix.shiro.ee.filters.FormAuthenticationFilter.FallbackPredicate;
import static com.flowlogix.shiro.ee.filters.FormAuthenticationFilter.NO_PREDICATE;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.omnifaces.util.Faces;

/**
 * JSF Ajax support
 *
 * @author lprimak
 */
public class LogoutFilter extends org.apache.shiro.web.filter.authc.LogoutFilter {
    public @Getter Class<? extends FormAuthenticationFilter.FallbackPredicate> predicateType = NO_PREDICATE.getClass();
    private @Getter @Setter FallbackPredicate fallbackType = NO_PREDICATE;

    @Override
    protected void issueRedirect(ServletRequest request, ServletResponse response, String redirectUrl) throws Exception {
        Forms.logout(fallbackType::useFallback, Faces.getRequestContextPath());
    }
}
