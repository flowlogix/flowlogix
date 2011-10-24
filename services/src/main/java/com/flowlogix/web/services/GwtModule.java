/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import com.flowlogix.web.services.internal.GwtCachingFilter;
import java.util.logging.Logger;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Match;
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
