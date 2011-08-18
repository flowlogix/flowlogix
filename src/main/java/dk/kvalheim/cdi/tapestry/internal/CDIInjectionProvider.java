/**
 * @(#)CDIInjectionProvider.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 */
package dk.kvalheim.cdi.tapestry.internal;

import org.apache.tapestry5.ioc.ObjectLocator;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.services.ClassTransformation;
import org.apache.tapestry5.services.InjectionProvider;
import org.apache.tapestry5.services.TransformField;

import dk.kvalheim.cdi.CDIFactory;

/**
 * 
 * @author Magnus
 */
public class CDIInjectionProvider implements InjectionProvider {

	private CDIFactory cdiFactory;
	public CDIInjectionProvider(CDIFactory cdiFactory) {
		this.cdiFactory = cdiFactory;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tapestry5.services.InjectionProvider#provideInjection(java.lang.String, java.lang.Class, org.apache.tapestry5.ioc.ObjectLocator, org.apache.tapestry5.services.ClassTransformation, org.apache.tapestry5.model.MutableComponentModel)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean provideInjection(String fieldName, Class fieldType,
			ObjectLocator locator, ClassTransformation transformation,
			MutableComponentModel componentModel) {
		
		/**
		 * Problem: in many cases a tapestry service will qualify as a cdi bean.
		 * In order to prevent cdi for managing a service that should be provided by tapestry we check if locator has the service.
		 */
		try {
			if(locator.getService(fieldType)!=null)
				return false;
		} catch (RuntimeException e) {
			// TODO: handle exception
		}
		
		TransformField field = transformation.getField(fieldName);		
        final Object injectionValue = cdiFactory.get(fieldType);
        
        if(injectionValue!=null) {
        	field.inject(injectionValue);
        	return true;
        }
		return false;
	}
}
