/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import com.flowlogix.web.services.internal.ExceptionHandlerAssistantImpl;
import com.flowlogix.web.services.internal.SecurityInterceptorFilter;
import java.io.IOException;
import org.apache.shiro.ShiroException;
import org.apache.shiro.mgt.RememberMeManager;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.tapestry5.MetaDataConstants;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.services.RequestConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.*;
import org.apache.tapestry5.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tynamo.exceptionpage.ExceptionHandlerAssistant;

/**
 * patch Tynamo security to load classes from the
 * our package, otherwise the library doesn't have access to our
 * principal classes
 * 
 * @author lprimak
 */
public class SecurityModule 
{
    public SecurityModule(@Symbol(SymbolConstants.ASSET_PATH_PREFIX) String assetPathPrefix)
    {
        this.assetPathPrefix = assetPathPrefix;
        pathProcessor = new GwtModule.PathProcessor(assetPathPrefix);
    }
    
    
    public static void contributeFactoryDefaults(MappedConfiguration<String, String> configuration)
    {
        configuration.add(Symbols.REMEMBER_ME_DURATION, Integer.toString(2 * 7)); // 2 weeks
        configuration.add(Symbols.INVALID_AUTH_DELAY, Integer.toString(3));
        configuration.add(Symbols.SESSION_EXPIRED_MESSAGE, "Your Session Has Expired");
    }
    
    
    public static void bind(ServiceBinder binder)
    {
        binder.bind(ExceptionHandlerAssistant.class, ExceptionHandlerAssistantImpl.class).withId("FLSecurityExceptionHandler");
    }


    public void contributeMetaDataLocator(MappedConfiguration<String, String> configuration)
    {
        configuration.add(String.format("%s:%s", SECURITY_PATH_PREFIX, MetaDataConstants.SECURE_PAGE), Boolean.toString(isSecure));
        configuration.add(String.format("%s:%s", "security", MetaDataConstants.SECURE_PAGE), Boolean.toString(isSecure));
    }
    
    
    /**
     * See <a href="https://issues.apache.org/jira/browse/TAP5-1779" target="_blank">TAP5-1779</a>
     */
    @Contribute(RequestHandler.class)
    public void disableAssetDirListing(OrderedConfiguration<RequestFilter> configuration,
                    @Symbol(SymbolConstants.APPLICATION_VERSION) final String applicationVersion,
                    final Context ctxt)
    {
        configuration.add("DisableDirListing", new RequestFilter() {
            @Override
            public boolean service(Request request, Response response, RequestHandler handler) throws IOException
            {
                final String assetFolder = assetPathPrefix + applicationVersion + "/"
                        + RequestConstants.CONTEXT_FOLDER;
                if (request.getPath().startsWith(assetFolder))
                {
                    if(request.getPath().endsWith("/") || 
                            ctxt.getRealFile(pathProcessor.removeAssetPathPart(
                            request.getPath())).isDirectory())
                    {
                        return false;
                    }
                }
                return handler.service(request, response);
            }
        }, "before:AssetDispatcher");
    }      

    
    @Match("ComponentRequestFilter")
    public static ComponentRequestFilter decorateEJBSecurityInterceptor(ComponentRequestFilter filter)
    {
        return new SecurityInterceptorFilter(filter);
    }


    @Match("RememberMeManager")
    public RememberMeManager decorateRememberMeDefaults(RememberMeManager _mgr, 
        @Symbol(Symbols.REMEMBER_ME_DURATION) Integer daysToRemember)
    {
        CookieRememberMeManager mgr = (CookieRememberMeManager)_mgr;
        if (productionMode)
        {
            mgr.getCookie().setMaxAge(daysToRemember * 24 * 60 * 60);
        } 
        else
        {
            mgr.getCookie().setMaxAge(-1);
        }
        
        return null;
    }
    
    
    /**
     * Detects expired session and sets an attribute to indicate that fact
     */
    public void contributeExceptionHandler(MappedConfiguration<Class<?>, ExceptionHandlerAssistant> configuration,
        @Local ExceptionHandlerAssistant assistant)
    {
        configuration.override(ShiroException.class, assistant);
    }
        
    
    public static class Symbols
    {
        public static final String REMEMBER_ME_DURATION = "flowlogix.security.remembermeduration";        
        public static final String INVALID_AUTH_DELAY = "flowlogix.security.invalid-auth-delay";
        public static final String SESSION_EXPIRED_MESSAGE = "flowlogix.security.session-expired-message";
    }
    
    
    public static final String SECURITY_PATH_PREFIX = "flowlogix/security";
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;
    private @Inject @Symbol(SymbolConstants.PRODUCTION_MODE) boolean productionMode;
   
    private final GwtModule.PathProcessor pathProcessor;
    private final String assetPathPrefix;
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityModule.class);
}
