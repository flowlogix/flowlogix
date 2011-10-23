/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import com.flowlogix.web.services.internal.AjaxAnnotationWorker;
import com.flowlogix.web.services.internal.AssetMinimizerImpl;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Primary;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.ioc.services.FactoryDefaults;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.apache.tapestry5.services.ComponentClassResolver;
import org.apache.tapestry5.services.LibraryMapping;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;

/**
 *
 * @author lprimak
 */
@SubModule({ EjbModule.class, GwtModule.class, SecurityModule.class })
public class ServicesModule 
{    
    @Contribute(SymbolProvider.class)
    @FactoryDefaults
    public void setFactoryDefaults(MappedConfiguration<String, String> configuration)
    {
        configuration.add(Symbols.MINIMIZE_ASSETS, "true");
    }


    @Contribute(ComponentClassResolver.class)
    public static void addLibrary(Configuration<LibraryMapping> configuration)
    {
        configuration.add(new LibraryMapping("flowlogix", "com.flowlogix.web"));
    }
    
        
    @Contribute(ComponentClassTransformWorker2.class)
    @Primary
    public static void provideClassTransformWorkers(OrderedConfiguration<ComponentClassTransformWorker2> configuration)
    {
        configuration.addInstance("AJAX", AjaxAnnotationWorker.class, "before:Property");
    }

    
    public static void bind(ServiceBinder binder)
    {
        binder.bind(AssetMinimizer.class, AssetMinimizerImpl.class);
    }
    
    
    public static class Symbols
    {
        public static final String MINIMIZE_ASSETS = "flowlogix.minimize-assets";
    }
}
