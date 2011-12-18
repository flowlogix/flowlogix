/**
 * @(#)CDIModule.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 */
package dk.kvalheim.cdi.tapestry.services;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.tapestry5.internal.services.ComponentClassCache;
import org.apache.tapestry5.ioc.ObjectProvider;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Local;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.InjectionProvider2;
import org.slf4j.Logger;

import dk.kvalheim.cdi.CDIFactory;
import dk.kvalheim.cdi.tapestry.internal.CDIAnnotationWorker;
import dk.kvalheim.cdi.tapestry.internal.CDIInjectionProvider;

/**
 * 
 * @author Magnus
 */
public class CDIModule {
	
	public static void bind(ServiceBinder binder) {
    	binder.bind(ObjectProvider.class, CDIObjectProvider.class).withId("CDIObjectProvider");        
    }	
	public static BeanManager buildBeanManager(Logger log) {		
		try {
			BeanManager beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
			return beanManager;			
		} catch (NamingException e) {
			log.error("Could not lookup jndi resource: java:comp/BeanManager", e); 
		}
		return null;
	}	
	public static CDIFactory buildCDIFactory(Logger log, @Local BeanManager beanManager) {		
		return new CDIFactory(log, beanManager);
	}	
	public static void contributeInjectionProvider(
			OrderedConfiguration<InjectionProvider2> configuration,
			@Local CDIFactory cdiFactory,
			ComponentClassCache cache) {
		configuration.add("CDI", new CDIInjectionProvider(cdiFactory, cache), "after:*,before:Service");
	}
	
	@Contribute(ComponentClassTransformWorker2.class)
    public static void provideClassTransformWorkers(
    		OrderedConfiguration<ComponentClassTransformWorker2> configuration) {
        configuration.addInstance("CDI", CDIAnnotationWorker.class, "before:Property");
    }
}
