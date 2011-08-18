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

import org.apache.tapestry5.ioc.ObjectProvider;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Local;
import org.apache.tapestry5.services.ComponentClassTransformWorker;
import org.apache.tapestry5.services.InjectionProvider;
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
//	public static void contributeMasterObjectProvider(
//			@Local ObjectProvider cdiProvider,
//			OrderedConfiguration<ObjectProvider> configuration) {	
////		configuration.add("cdiProvider", cdiProvider, "after:Service,after:AnnotationBasedContributions,after:Alias,after:Autobuild");		
//		configuration.add("cdiProvider", cdiProvider, "after:*");	
//	} 
	public static void contributeInjectionProvider(
			OrderedConfiguration<InjectionProvider> configuration,
			@Local CDIFactory cdiFactory) {
		configuration.add("CDI", new CDIInjectionProvider(cdiFactory), "after:*,before:Service");
	}
	
	@Contribute(ComponentClassTransformWorker.class)
    public static void provideClassTransformWorkers(OrderedConfiguration<ComponentClassTransformWorker> configuration) {
        configuration.addInstance("CDI", CDIAnnotationWorker.class, "before:Property");
    }
}
