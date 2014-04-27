/**
 * @(#)CDIObjectProvider.java
 *
 * Copyright 2009 by Movellas ApS All rights reserved.
 */
package com.flowlogix.web.services.internal;

import com.flowlogix.cdi.CDIFactory;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Singleton;
import org.apache.tapestry5.ioc.AnnotationProvider;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.ioc.ObjectProvider;
import org.apache.tapestry5.ioc.annotations.Local;
import org.slf4j.Logger;

/**
 *
 * @author Magnus
 */
public class CDIObjectProvider implements ObjectProvider
{

    private Logger log;
    private CDIFactory cdiFactory;
    private Set<Class<? extends Annotation>> allowedScopes;

    public CDIObjectProvider(
            Logger log,
            @Local CDIFactory cdiFactory)
    {
        this.allowedScopes = new HashSet<>();
        this.log = log;
        this.cdiFactory = cdiFactory;
        
        allowedScopes.add(ApplicationScoped.class);
        allowedScopes.add(Singleton.class);
        allowedScopes.add(RequestScoped.class);
    }

    @Override
    public <T> T provide(Class<T> objectType,
            AnnotationProvider annotationProvider, ObjectLocator locator)
    {

        /**
         * Problem: in many cases a tapestry service will qualify as a cdi bean. In order to prevent cdi for managing a
         * service that should be provided by tapestry we check if locator has the service.
         *
         * The MasterObjectProvider will attempt to delegate to locator after all providers has been asked.
         */
//		try {
//			if(locator.getService(objectType)!=null)
//				return null;
//		} catch (RuntimeException e) {
//		}
        return cdiFactory.get(objectType);

//		Set<Bean<?>> beans =  beanManager.getBeans(objectType);
//		if(beans!=null && beans.size()>0) {
//			Bean<T> bean = (Bean<T>) beans.iterator().next();		
//			if(hasValidScope(bean)) {
//				CreationalContext<T> ctx = beanManager.createCreationalContext(bean);
//				T o = (T) beanManager.getReference(bean, objectType, ctx); 
//				log.info("Found and returning: "+objectType.getCanonicalName());
//				return o;	
//			}
//		}
//		return null;
    }

    protected <T> boolean hasValidScope(Bean<T> bean)
    {
        return bean != null && allowedScopes.contains(bean.getScope());
    }
}
