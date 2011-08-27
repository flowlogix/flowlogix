/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.services.ComponentClassResolver;
import org.apache.tapestry5.services.LibraryMapping;

/**
 *
 * @author lprimak
 */
@SubModule({ EjbModule.class, GwtModule.class })
public class ServicesModule 
{
    @Contribute(ComponentClassResolver.class)
    public static void addLibrary(Configuration<LibraryMapping> configuration)
    {
        configuration.add(new LibraryMapping("flowlogix", "com.flowlogix.web"));
    }
}
