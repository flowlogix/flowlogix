package com.flowlogix.web.services.internal;

import com.flowlogix.web.services.GwtModule.PathProcessor;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Service;
import org.apache.tapestry5.internal.InternalConstants;
import org.apache.tapestry5.internal.services.RequestImpl;
import org.apache.tapestry5.internal.services.ResourceStreamer;
import org.apache.tapestry5.internal.services.ResponseImpl;
import org.apache.tapestry5.internal.services.TapestrySessionFactory;
import org.apache.tapestry5.internal.services.assets.ContextAssetRequestHandler;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.AssetFactory;
import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.HttpServletRequestHandler;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Response;

/**
 * Cache and gzip compress GWT assets outside of Tapestry control
 * TODO check operation
 * 
 * @author lprimak
 */
@Slf4j
public class GwtCachingFilter implements HttpServletRequestFilter
{
    public GwtCachingFilter(ResourceStreamer streamer, @Service("ContextAssetFactory") AssetFactory contextAssetFactory,
            TapestrySessionFactory sessionFactory, RequestGlobals rg,
            @Symbol(Symbols.NEVER_EXPIRE) String rawNeverExpires,
            @Symbol(Symbols.NEVER_CACHE) String rawNeverCache,
            @Symbol(SymbolConstants.ASSET_PATH_PREFIX) String assetPathPrefix)
    {
        this.carh = new ContextAssetRequestHandler(streamer, contextAssetFactory.getRootResource());
        this.sessionFactory = sessionFactory;
        this.rg = rg;
        this.pathProcessor = new PathProcessor(assetPathPrefix);
        configure(rawNeverExpires, rawNeverCache);
    }

    
    private void configure(String rawNeverExpires, String rawNeverCache)
    {
        if(rawNeverExpires != null && (!rawNeverExpires.isEmpty()))
        {
            neverExpireExtensions.addAll(Arrays.asList(extSplitPattern.split(rawNeverExpires)));
        }

        if(rawNeverCache != null && (!rawNeverCache.isEmpty()))
        {
            neverCachedExtensions.addAll(Arrays.asList(extSplitPattern.split(rawNeverCache)));
        }        
    }

    
    @Override
    public boolean service(HttpServletRequest request, HttpServletResponse response, HttpServletRequestHandler chainHandler) throws IOException
    {
        String path = request.getServletPath();
        boolean neverExpire = checkConfig(path, response);
        
        if (neverExpire == false)
        {
            return chainHandler.service(request, response);
        }
        
        log.debug("GwtCachingFilter: Processing %s", path);

        Request rq = new RequestImpl(request, applicationCharset, sessionFactory);
        Response rsp = new ResponseImpl(request, response);
        rg.storeRequestResponse(rq, rsp);

        rsp.setDateHeader("Expires", new Date().getTime() + InternalConstants.TEN_YEARS);

        try
        {
            return carh.handleAssetRequest(rq, rsp, pathProcessor.removeAssetPathPart(path));
        }
        catch(Exception e)
        {
            return chainHandler.service(request, response);
        }
    }
    
    
    private boolean checkConfig(String path, HttpServletResponse response)
    {
        boolean neverExpire = false;
        for (String ext : neverExpireExtensions)
        {
            if (path.endsWith(ext))
            {
                neverExpire = true;
                break;
            }
        }
        if (neverExpire == false)
        {
            for (String ext : neverCachedExtensions)
            {
                if (path.endsWith(ext))
                {
                    response.setHeader("Cache-Control", "no-cache"); //HTTP 1.1
                    response.setHeader("Pragma", "no-cache");        //HTTP 1.0
                    response.setDateHeader("Expires", 0);
                    break;
                }
            }
        }
        return neverExpire;
    }
    
    
    public static class Symbols
    {
        public static final String NEVER_EXPIRE = "flowlogix.gwt-never-expire";
        public static final String NEVER_CACHE = "flowlogix.gwt-never-cache";
    }
    
    
    private final ContextAssetRequestHandler carh;
    private final TapestrySessionFactory sessionFactory;
    private @Inject @Symbol(SymbolConstants.CHARSET) String applicationCharset;
    private final PathProcessor pathProcessor;
    private final RequestGlobals rg;
    private final List<String> neverExpireExtensions = new LinkedList<>();
    private final List<String> neverCachedExtensions = new LinkedList<>();
    private static final Pattern extSplitPattern = Pattern.compile("[;, \t\n\f\r]+");
}
