/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import com.flowlogix.web.services.internal.AjaxAnnotationWorker;
import com.flowlogix.web.services.internal.AssetMinimizerImpl;
import com.flowlogix.web.services.internal.GwtCachingFilter;
import com.flowlogix.web.services.internal.Html5DocTypeFilter;
import com.flowlogix.web.services.internal.ResourceChangeTrackerOverride;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.services.assets.ResourceChangeTracker;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Primary;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.FactoryDefaults;
import org.apache.tapestry5.ioc.services.ServiceOverride;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.apache.tapestry5.services.ComponentClassResolver;
import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.LibraryMapping;
import org.apache.tapestry5.services.MarkupRenderer;
import org.apache.tapestry5.services.MarkupRendererFilter;
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
    
    
    public void contributeHttpServletRequestHandler(OrderedConfiguration<HttpServletRequestFilter> config)
    {
        // add GWT html caching and gzip compression
        config.addInstance("GwtHtmlCompressor", GwtCachingFilter.class, "after:*");
    }

    
    
    @Contribute(MarkupRenderer.class)
    public void forceHTML5DocType(OrderedConfiguration<MarkupRendererFilter> configuration) 
    {
        // +++ remove this when Tapestry fixes HTML5 doctype in the browser output,
        // along with the filter class itself
        configuration.addInstance("Html5DocType", Html5DocTypeFilter.class, "after:MarkupRenderer");
    }
    
    
    @Contribute(ServiceOverride.class)
    public void fixNullTimestamps(MappedConfiguration<Class<?>, Object> configuration,
                                  @Symbol(SymbolConstants.PRODUCTION_MODE)
                                  boolean productionMode)
    {
        // +++ remove this when Tapestry correctly sends expire headers (not null) in production mode
        if(productionMode)
        {
            configuration.addInstance(ResourceChangeTracker.class, ResourceChangeTrackerOverride.class);
        }
    }
    
    
    public static class Symbols
    {
        public static final String MINIMIZE_ASSETS = "flowlogix.minimize-assets";
    }
}
