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
package com.flowlogix.ui.livereload;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.omnifaces.util.Faces;

@WebListener
public class Configurator implements ServletContextListener {
    static final String DISABLE_CACHE_PARAM = "com.flowlogix.faces.DISABLE_CACHE";
    static final String FACELETS_REFRESH_PERIOD_PARAM = "jakarta.faces.FACELETS_REFRESH_PERIOD";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String disableCacheStr = sce.getServletContext().getInitParameter(DISABLE_CACHE_PARAM);
        boolean disableCache = disableCacheStr == null || Boolean.parseBoolean(disableCacheStr);
        boolean isFacesDevelopment = Faces.hasContext() && Faces.isDevelopment();
        if (isFacesDevelopment && disableCache) {
            sce.getServletContext().setInitParameter(FACELETS_REFRESH_PERIOD_PARAM, "0");
        }
    }
}
