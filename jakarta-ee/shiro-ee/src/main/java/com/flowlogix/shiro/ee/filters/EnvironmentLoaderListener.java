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

import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

/**
 *
 * @author lprimak
 */
@WebListener
public class EnvironmentLoaderListener extends org.apache.shiro.web.env.EnvironmentLoaderListener {
    public static final String SHIRO_EE_DISABLED_PROP = "com.flowlogix.shiro.ee.disabled";
    static final boolean SHIRO_EE_DISABLED = Boolean.getBoolean(SHIRO_EE_DISABLED_PROP);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (!SHIRO_EE_DISABLED) {
            super.contextInitialized(sce);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (!SHIRO_EE_DISABLED) {
            super.contextDestroyed(sce);
        }
    }
}
