package com.flowlogix.ui;

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
            faces.addMapping(ResourceHandler.RESOURCE_IDENTIFIER + "/*");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
