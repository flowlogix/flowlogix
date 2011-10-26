/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import com.flowlogix.web.services.internal.GwtCachingFilter;
import java.util.logging.Logger;
import org.apache.tapestry5.internal.services.RequestConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.services.FactoryDefaults;
import org.apache.tapestry5.ioc.services.SymbolProvider;
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
        config.add(GwtCachingFilter.Symbols.NEVER_CACHE, "");
        config.add(GwtCachingFilter.Symbols.NEVER_EXPIRE, ".cache.html");
    }

    
    public static class PathProcessor
    {
        public static String removeAssetPathPart(String path)
        {
            return path.replaceFirst(filter, "");
        }
        
        
        private static final String filter = String.format("%s.*\\/%s", 
                RequestConstants.ASSET_PATH_PREFIX, RequestConstants.CONTEXT_FOLDER);
    }
    
    
    private static final Logger log = Logger.getLogger(GwtModule.class.getName());
}
