/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.jpa;

import java.util.logging.Logger;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.services.ClassNameLocator;
import org.apache.tapestry5.jpa.PersistenceUnitConfigurer;
import org.apache.tapestry5.jpa.TapestryPersistenceUnitInfo;

/**
 *
 * @author lprimak
 */
public class PersistenceRegistry 
{
    public void register(String persistenceUnit, final Class<?> entitySample,
            final ClassNameLocator locator, final MappedConfiguration<String, PersistenceUnitConfigurer> configuration)
    {
        final PersistenceUnitConfigurer configurer = new PersistenceUnitConfigurer()
        {
            @Override
            public void configure(final TapestryPersistenceUnitInfo unitInfo)
            {
                // Tap-JPA module makes you include entities one-by-one
                // so excludeUnlistedFiles has to be overridden and set to true
                unitInfo.excludeUnlistedClasses(true);
                String packageName = entitySample.getPackage().getName();
                log.fine("Registering JPA Package: " + packageName);
                for (String className : locator.locateClassNames(packageName))
                {
                    log.finer("Registering JPA Class: " + className);
                    unitInfo.addManagedClassName(className);
                }
            }
        };
        configuration.add(persistenceUnit, configurer);
    }  
        
    
    private static final Logger log = Logger.getLogger(PersistenceRegistry.class.getName());
}
