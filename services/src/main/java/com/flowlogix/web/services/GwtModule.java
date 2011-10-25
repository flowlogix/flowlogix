/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import com.flowlogix.web.services.internal.GwtCachingFilter;
import java.util.logging.Logger;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.services.FactoryDefaults;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.apache.tapestry5.services.HttpServletRequestFilter;

/**
 *
 * @author lprimak
 */
public class GwtModule 
{    
    public void contributeHttpServletRequestHandler(OrderedConfiguration<HttpServletRequestFilter> config)
    {
        // add GWT html caching and gzip compression
        config.addInstance("GwtHtmlCompressor", GwtCachingFilter.class, "after:*");
    }
    

    @Contribute(SymbolProvider.class)
    @FactoryDefaults
    public void configureFilter(MappedConfiguration<String, String> config)
    {
        // syntax: ".ext1,.ext2;.ext4
        // commas, semicolns. are the separators
        config.add(GwtCachingFilter.Symbols.NEVER_CACHE, ".nocache.js");
        config.add(GwtCachingFilter.Symbols.NEVER_EXPIRE, ".cache.html");
    }
    

    @Match("AssetPathConverter")
    @SuppressWarnings("unchecked")
    public void adviseGwtJsPathMethod(MethodAdviceReceiver receiver)
            throws SecurityException, NoSuchMethodException
    {
        MethodAdvice advice = new MethodAdvice()
        {
            @Override
            public void advise(MethodInvocation invocation)
            {
                invocation.proceed();
                if(invocation.getMethod().getReturnType().equals(String.class))
                {
                    String result = invocation.getReturnValue().toString();
                    
                    if (result.matches(".*\\.nocache\\.js"))
                    {                      
                        log.fine(String.format("Converting GWT Path: %s", result));
                        // remove assets/<version>/ctx from GWT path - interferes with servlets
                        invocation.setReturnValue(PathProcessor.removeAssetPathPart(result));
                    }
                }
            }
        };
        receiver.adviseMethod(receiver.getInterface().getMethod("convertAssetPath", String.class), advice);
    }
    
    
    public static class PathProcessor
    {
        public static String removeAssetPathPart(String path)
        {
            return path.replaceFirst("\\/assets\\/.*\\/ctx", "");
        }
    }
    
    
    private static final Logger log = Logger.getLogger(GwtModule.class.getName());
}
