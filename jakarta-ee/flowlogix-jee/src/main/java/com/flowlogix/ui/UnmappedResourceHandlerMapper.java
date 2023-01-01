/*
 * Copyright (C) 2011-2023 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.ui;

import static com.flowlogix.util.JakartaTransformerUtils.jakartify;
import javax.faces.application.ResourceHandler;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * aids in implementation of OmniFaces {@link UnmappedResourceHandler}
 *
 * @author lprimak
 */
@WebListener
public class UnmappedResourceHandlerMapper implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        var faces = sce.getServletContext().getServletRegistration("FacesServlet");
        if (Boolean.parseBoolean(sce.getServletContext()
                .getInitParameter("com.flowlogix.add-unmapped-resources")) && faces != null) {
            faces.addMapping(jakartify(ResourceHandler.RESOURCE_IDENTIFIER) + "/*");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
