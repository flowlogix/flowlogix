/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import com.flowlogix.util.GwtSupportLoaded;
import com.flowlogix.web.services.internal.GwtCachingFilter;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.services.RequestConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.OperationException;
import org.apache.tapestry5.ioc.services.FactoryDefaults;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.apache.tapestry5.ioc.services.ThreadLocale;
import org.apache.tapestry5.services.AssetSource;
import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestFilter;
import org.apache.tapestry5.services.RequestHandler;
import org.apache.tapestry5.services.Response;

/**
 * Provides forever caching of GWT assets,
 * fixes paths for GWT-RPC<br>
 * See {@link com.flowlogix.web.base.GwtSupport}
 *
 * @author lprimak
 */
@Slf4j
public class GwtModule 
{  
    public void contributeHttpServletRequestHandler(OrderedConfiguration<HttpServletRequestFilter> config)
    {
        // add GWT html caching and gzip compression
        config.addInstance("GwtCachingFilter", GwtCachingFilter.class, "after:*");
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
    
    
    @Contribute(RequestHandler.class)
    public void fixGWTChecksumHandler(OrderedConfiguration<RequestFilter> configuration, 
        final AssetSource assetSource, final ThreadLocale threadLocale,
        final @Symbol(SymbolConstants.CONTEXT_PATH) String contextPath,
        final @Symbol(SymbolConstants.ASSET_PATH_PREFIX) String assetPathPrefix)
    {
        final PathProcessor pathProcessor = new PathProcessor(assetPathPrefix);
        
        configuration.add("fixGWTChecksums", new RequestFilter() 
        {
            @Override
            public boolean service(final Request request, Response response, RequestHandler handler) throws IOException
            {
                final String path = request.getPath();
                boolean handled = Sets.filter(GwtSupportLoaded.getGwtModuleNames(), new Predicate<String>() 
                {
                    @Override
                    public boolean apply(String input)
                    {
                        return path.startsWith("/asset") && path.contains(input) && request.getParameter("redirected") == null;
                    }
                }).size() > 0;
                if(handled)
                {
                    Asset asset = assetSource.getContextAsset(pathProcessor.removeAssetPathPart(path), 
                            threadLocale.getLocale());
                    String redirectionUrl;
                    try
                    {
                         redirectionUrl = asset.toClientURL() + "/?redirected";
                    }
                    catch(OperationException e)
                    {
                        redirectionUrl = contextPath + pathProcessor.removeAssetPathPart(path);
                    }
					response.sendRedirect(redirectionUrl);
                    return true;
                }
                else return handler.service(request, response);
            }
        });
    }
    
    
    public static class PathProcessor
    { 
        public PathProcessor(String assetPathPrefix)
        {
            filter = String.format("%s.*\\/%s\\/\\w+\\/", assetPathPrefix, RequestConstants.CONTEXT_FOLDER);
        }
        
        
        public String removeAssetPathPart(String path)
        {
            return path.replaceFirst(filter, "");
        }
        
        
        private final String filter;
    }
}
