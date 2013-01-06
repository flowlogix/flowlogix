/**
 * @(#)CDIModule.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 */
package com.flowlogix.web.services;

import com.flowlogix.cdi.CDIFactory;
import com.flowlogix.web.services.internal.CDIAnnotationWorker;
import com.flowlogix.web.services.internal.CDIInjectionProvider;
import com.flowlogix.web.services.internal.CDIObjectProvider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

/**
 * 
 * @author Magnus
 */
public class CDIModule {
	
	public static void bind(ServiceBinder binder) {
    	binder.bind(ObjectProvider.class, CDIObjectProvider.class).withId("CDIObjectProvider");        
    }	
	public static BeanManager buildBeanManager(Logger log) {	
		log.debug("buildBeanManager");
		try {
			BeanManager beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
			return beanManager;			
		} catch (NamingException e) {
			log.info("CDI Not Available"); 
		}
		return (BeanManager)Proxy.newProxyInstance(BeanManager.class.getClassLoader(), 
                        new Class<?>[] { BeanManager.class }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                    {
                        throw new UnsupportedOperationException("CDI Not Supported");
                    }
            });
	}	
	public static CDIFactory buildCDIFactory(Logger log, @Local BeanManager beanManager) {		
		return new CDIFactory(log, beanManager);
	}	
	
	@Contribute(InjectionProvider2.class)
	public static void provideInjectionProvider(
			OrderedConfiguration<InjectionProvider2> configuration,
			@Local CDIFactory cdiFactory,
			ComponentClassCache cache) {
//		configuration.add("CDI", new CDIInjectionProvider(cdiFactory, cache), "after:*,before:Service");
		configuration.add("CDI", new CDIInjectionProvider(cdiFactory, cache), "after:InjectionProvider");
	}
	
	public static void contributeMasterObjectProvider(
			@Local ObjectProvider cdiProvider,
			OrderedConfiguration<ObjectProvider> configuration) {	
//		configuration.add("cdiProvider", cdiProvider, "after:Service,after:AnnotationBasedContributions,after:Alias,after:Autobuild");		
		configuration.add("cdiProvider", cdiProvider, "after:*");	
	} 
	
	@Contribute(ComponentClassTransformWorker2.class)
    public static void provideClassTransformWorkers(
    		OrderedConfiguration<ComponentClassTransformWorker2> configuration) {
        configuration.addInstance("CDI", CDIAnnotationWorker.class, "before:Property");
    }
}
