/**
 * @(#)CDIAnnotationWorker.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 */
package dk.kvalheim.cdi.tapestry.internal;

import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.services.ClassTransformation;
import org.apache.tapestry5.services.ComponentClassTransformWorker;
import org.apache.tapestry5.services.TransformField;

import dk.kvalheim.cdi.CDIFactory;
import dk.kvalheim.cdi.tapestry.annotation.CDI;

/**
 * 
 * @author Magnus
 */
public class CDIAnnotationWorker implements ComponentClassTransformWorker {

	private CDIFactory cdiFactory;
	public CDIAnnotationWorker(CDIFactory cdiFactory) {
		this.cdiFactory = cdiFactory;
	}
	public void transform(ClassTransformation transformation,
			MutableComponentModel model) {
		for(final TransformField field : transformation.matchFieldsWithAnnotation(CDI.class)) {
			final CDI annotation = field.getAnnotation(CDI.class);
			String fieldType = field.getType();
            Class type = transformation.toClass(fieldType);
            final Object injectionValue = cdiFactory.get(type);
            
            if(injectionValue!=null) {
            	field.inject(injectionValue);
            	field.claim(annotation);
            }
		}		
	}
}
