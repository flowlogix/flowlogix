/*
 * Copyright (C) 2011-2026 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.shiro.ee.listeners;

import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import static javax.servlet.SessionTrackingMode.COOKIE;
import javax.servlet.annotation.WebListener;
import org.apache.shiro.web.env.EnvironmentLoader;
import org.apache.shiro.web.env.WebEnvironment;

/**
 * Automatic, adds ability to disable via system property
 * Adds ability to have two shiro.ini configuration files that are merged
 * @author lprimak
 */
@WebListener
public class EnvironmentLoaderListener extends EnvironmentLoader implements ServletContextListener {
    private static final String SHIRO_EE_DISABLED_PARAM = "com.flowlogix.shiro.ee.disabled";
    private static final String FORM_RESUBMIT_DISABLED_PARAM = "com.flowlogix.shiro.form-resubmit.disabled";
    private static final String SHIRO_EE_SERVLET_NO_PRINCIPAL_PARAM = "com.flowlogix.shiro.servlet-no-principal";

    public static boolean isShiroEEDisabled(ServletContext ctx) {
        return Boolean.TRUE.equals(ctx.getAttribute(SHIRO_EE_DISABLED_PARAM));
    }

    public static boolean isFormResumbitDisabled(ServletContext ctx) {
        return Boolean.TRUE.equals(ctx.getAttribute(FORM_RESUBMIT_DISABLED_PARAM));
    }

    public static boolean isServletNoPrincipal(ServletContext ctx) {
        return Boolean.TRUE.equals(ctx.getAttribute(SHIRO_EE_SERVLET_NO_PRINCIPAL_PARAM));
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (Boolean.parseBoolean(sce.getServletContext().getInitParameter(SHIRO_EE_DISABLED_PARAM))) {
            sce.getServletContext().setAttribute(SHIRO_EE_DISABLED_PARAM, Boolean.TRUE);
        }
        if (Boolean.parseBoolean(sce.getServletContext().getInitParameter(SHIRO_EE_SERVLET_NO_PRINCIPAL_PARAM))) {
            sce.getServletContext().setAttribute(SHIRO_EE_SERVLET_NO_PRINCIPAL_PARAM, Boolean.TRUE);
        }
        if (!isShiroEEDisabled(sce.getServletContext())) {
            sce.getServletContext().setSessionTrackingModes(Set.of(COOKIE));
            initEnvironment(sce.getServletContext());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (!isShiroEEDisabled(sce.getServletContext())) {
            destroyEnvironment(sce.getServletContext());
        }
    }

    @Override
    protected Class<? extends WebEnvironment> getDefaultWebEnvironmentClass(ServletContext ctx) {
        return IniEnvironment.class;
    }
}
