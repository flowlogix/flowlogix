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
package com.flowlogix.shiro.ee.listeners;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
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
    public static final String SHIRO_EE_DISABLED_PROP = "com.flowlogix.shiro.ee.disabled";
    static final boolean SHIRO_EE_DISABLED = Boolean.getBoolean(SHIRO_EE_DISABLED_PROP);

    public static boolean isShiroEEDisabled() {
        return SHIRO_EE_DISABLED;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (!SHIRO_EE_DISABLED) {
            initEnvironment(sce.getServletContext());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (!SHIRO_EE_DISABLED) {
            destroyEnvironment(sce.getServletContext());
        }
    }

    @Override
    protected Class<? extends WebEnvironment> getDefaultWebEnvironmentClass() {
        return IniEnvironment.class;
    }
}
