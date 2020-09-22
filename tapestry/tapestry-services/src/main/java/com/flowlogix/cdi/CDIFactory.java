/**
 * @(#)CDIFactory.java
 *
 * Copyright 2009 by Movellas ApS All rights reserved.
 */
package com.flowlogix.cdi;

import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

/**
 *
 * @author Magnus
 */
@RequiredArgsConstructor
public class CDIFactory
{
    public <T> T get(Class<T> clazz)
    {
        Set<Bean<?>> beans;
        try
        {
            beans = beanManager.getBeans(clazz);
        } catch (UnsupportedOperationException e)
        {
            return null;
        }
        if (beans != null && beans.size() > 0)
        {
            @SuppressWarnings("unchecked")
            Bean<T> bean = (Bean<T>) beans.iterator().next();
            CreationalContext<T> ctx = beanManager.createCreationalContext(bean);
            try
            {
                T o = clazz.cast(beanManager.getReference(bean, clazz, ctx));
                log.debug("Found and returning: " + clazz.getCanonicalName());
                return o;
            } catch (Exception e)
            {
                log.trace("CDI Retrieval Error", e);
            }
        }
        return null;
    }

    
    private final Logger log;
    private final BeanManager beanManager;
}
