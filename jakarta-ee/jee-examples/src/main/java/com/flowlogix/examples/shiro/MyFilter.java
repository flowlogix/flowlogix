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

import com.flowlogix.shiro.ee.filters.PassThruAuthenticationFilter;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.web.util.WebUtils;
import org.omnifaces.util.Servlets;

/**
 *
 * @author lprimak
 */
@Slf4j
public class MyFilter extends PassThruAuthenticationFilter {
    static final String SHIRO_FORM_DATA = "SHIRO_FORM_DATA";

    @Override
    protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
        if (HttpMethod.POST.equalsIgnoreCase(WebUtils.toHttp(request).getMethod())) {
            String postData = request.getReader().lines().collect(Collectors.joining());
            Servlets.addResponseCookie(WebUtils.toHttp(request), WebUtils.toHttp(response),
                    SHIRO_FORM_DATA, postData, null,
                    WebUtils.toHttp(request).getContextPath(), -1);

        }

        super.redirectToLogin(request, response);
    }
}
