/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.ui.browsersync;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.omnifaces.util.Faces;

@Slf4j
@WebListener
@Generated
public class Configurator implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String disableCacheStr = sce.getServletContext().getInitParameter("com.flowlogix.faces.DISABLE_CACHE");
        boolean disableCache = disableCacheStr == null || Boolean.parseBoolean(disableCacheStr);
        if (Faces.isDevelopment() && disableCache) {
            sce.getServletContext().setInitParameter("jakarta.faces.FACELETS_REFRESH_PERIOD", "0");
        }
    }
}
