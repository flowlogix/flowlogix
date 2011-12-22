/**
 * @(#)CDIAnnotationWorker.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 */
package dk.kvalheim.cdi.tapestry.internal;

import org.apache.tapestry5.internal.services.ComponentClassCache;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticField;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

import dk.kvalheim.cdi.CDIFactory;
import dk.kvalheim.cdi.tapestry.annotation.CDI;

/**
 * Worker for the CDI annotation
 * @author Magnus Kvalheim
 * 
 */
public class CDIAnnotationWorker implements ComponentClassTransformWorker2 {

	private CDIFactory cdiFactory;
	private final ComponentClassCache cache;

	public CDIAnnotationWorker(CDIFactory cdiFactory, ComponentClassCache cache) {
		this.cdiFactory = cdiFactory;
		this.cache = cache;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.tapestry5.services.transform.ComponentClassTransformWorker2
	 * #transform(org.apache.tapestry5.plastic.PlasticClass,
	 * org.apache.tapestry5.services.transform.TransformationSupport,
	 * org.apache.tapestry5.model.MutableComponentModel)
	 */
	@Override
	public void transform(PlasticClass plasticClass,
			TransformationSupport support, MutableComponentModel model) {
		for (PlasticField field : plasticClass.getFieldsWithAnnotation(CDI.class)) {
			final CDI annotation = field.getAnnotation(CDI.class);
			Class type = cache.forName(field.getTypeName());
			final Object injectionValue = cdiFactory.get(type);
			if (injectionValue != null) {
				field.inject(injectionValue);
				field.claim(annotation);
			}
		}
	}
}
