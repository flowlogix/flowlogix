/**
 * @(#)CDIFactory.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 */
package dk.kvalheim.cdi;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.slf4j.Logger;

/**
 * 
 * @author Magnus
 */
public class CDIFactory {

	private BeanManager beanManager;
	private Logger log;
	public CDIFactory(Logger log, BeanManager bm) {
		this.beanManager = bm;
		this.log = log;
	}
	
	public <T> T get(Class<T> clazz) {
		Set<Bean<?>> beans =  beanManager.getBeans(clazz);
		if(beans!=null && beans.size()>0) {
			Bean<T> bean = (Bean<T>) beans.iterator().next();		
			CreationalContext<T> ctx = beanManager.createCreationalContext(bean);			
			T o = clazz.cast(beanManager.getReference(bean, clazz, ctx)); 
			log.info("Found and returning: "+clazz.getCanonicalName());
			return o;	
		}
		return null;
	}
}
