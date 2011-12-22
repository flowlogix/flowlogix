/**
 * @(#)CDIInjectionProvider.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 */
package dk.kvalheim.cdi.tapestry.internal;

import org.apache.tapestry5.internal.services.ComponentClassCache;
import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.PlasticField;
import org.apache.tapestry5.services.transform.InjectionProvider2;

import dk.kvalheim.cdi.CDIFactory;

/**
 * 
 * @author Magnus
 */
public class CDIInjectionProvider implements InjectionProvider2 {

	private CDIFactory cdiFactory;
	private final ComponentClassCache cache;
	
	public CDIInjectionProvider(CDIFactory cdiFactory, ComponentClassCache cache) {
		this.cdiFactory = cdiFactory;
		this.cache = cache;
	}

	/* (non-Javadoc)
	 * @see org.apache.tapestry5.services.transform.InjectionProvider2#provideInjection(org.apache.tapestry5.plastic.PlasticField, org.apache.tapestry5.ioc.ObjectLocator, org.apache.tapestry5.model.MutableComponentModel)
	 */
	@Override
	public boolean provideInjection(PlasticField field, ObjectLocator locator,
			MutableComponentModel componentModel) {
		Class type = cache.forName(field.getTypeName());
		/**
		 * Problem: in many cases a tapestry service will qualify as a cdi bean.
		 * In order to prevent cdi for managing a service that should be provided by tapestry we check if locator has the service.
		 */
		try {
			if(locator.getService(type)!=null)
				return false;
		} catch (RuntimeException e) {
			// TODO: handle exception
		}
			
        final Object injectionValue = cdiFactory.get(type);
        
        if(injectionValue!=null) {
        	field.inject(injectionValue);
        	return true;
        }
		return false;
	}
}
