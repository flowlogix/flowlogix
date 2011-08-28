package com.flowlogix.web.services;

import com.flowlogix.web.services.internal.EJBAnnotationWorker;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Primary;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;

/**
 * Integrate EJB3 Beans into the Web site
 * 
 * @author lprimak
 */
public class EjbModule
{
    @Contribute(ComponentClassTransformWorker2.class)
    @Primary
    public static void provideClassTransformWorkers(OrderedConfiguration<ComponentClassTransformWorker2> configuration)
    {
        configuration.addInstance("EJB", EJBAnnotationWorker.class, "before:Property");
    }
}
