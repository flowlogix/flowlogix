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
package com.flowlogix.ui;

import static com.flowlogix.util.JakartaTransformerUtils.jakartify;
import java.util.Optional;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.webapp.FacesServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;
import org.omnifaces.resourcehandler.UnmappedResourceHandler;

/**
 * aids in implementation of OmniFaces {@link UnmappedResourceHandler}
 *
 * @author lprimak
 */
@WebListener
public class UnmappedResourceHandlerMapper implements ServletContextListener {
    static final String COM_FLOWLOGIX_ADD_UNMAPPED_RESOURCES = "com.flowlogix.add-unmapped-resources";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (Boolean.parseBoolean(sce.getServletContext()
                .getInitParameter(COM_FLOWLOGIX_ADD_UNMAPPED_RESOURCES))) {
            getFacesServlet(sce.getServletContext()).ifPresent(faces -> faces
                    .addMapping(jakartify(ResourceHandler.RESOURCE_IDENTIFIER) + "/*"));
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // nothing to do
    }

    private Optional<? extends ServletRegistration> getFacesServlet(ServletContext ctx) {
        return ctx.getServletRegistrations().values().stream()
                .filter(it -> FacesServlet.class.getName().equals(it.getClassName()))
                .findFirst();
    }
}
