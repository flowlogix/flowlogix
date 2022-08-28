/*
 * Copyright 2014 lprimak.
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

import org.omnifaces.util.JNDIObjectLocator;
import java.io.IOException;
import javax.faces.application.ProjectStage;
import static javax.faces.application.ProjectStage.PROJECT_STAGE_JNDI_NAME;
import static javax.faces.application.ProjectStage.PROJECT_STAGE_PARAM_NAME;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * Enables Shiro' SslFilter depending
 * depending on whether in production mode or not
 * @author lprimak
 */
public class SslFilter extends org.apache.shiro.web.filter.authz.SslFilter {
    private final JNDIObjectLocator locator = JNDIObjectLocator.builder().build();
    @Getter @Setter
    private boolean enablePortFilter = true;

    @Override
    protected boolean isEnabled(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        boolean enabled = true;
        String stageParameter = request.getServletContext().getInitParameter(PROJECT_STAGE_PARAM_NAME);
        if (stageParameter == null || ProjectStage.Development.name().equalsIgnoreCase(stageParameter)) {
            enabled = false;
        }
        String stageOverride = locator.getObject(PROJECT_STAGE_JNDI_NAME);
        if (ProjectStage.Development.name().equalsIgnoreCase(stageOverride)) {
            enabled = false;
        }
        return enabled && super.isEnabled(request, response);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        if (enablePortFilter == false) {
            mappedValue = new String[] { Integer.toString(request.getServerPort()) };
        }
        return super.isAccessAllowed(request, response, mappedValue);
    }
}
