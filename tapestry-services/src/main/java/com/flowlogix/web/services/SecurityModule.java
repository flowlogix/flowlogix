/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import com.flowlogix.web.services.internal.PageServiceOverride;
import com.flowlogix.web.services.internal.SecurityInterceptorFilter;
import java.io.IOException;
import org.apache.tapestry5.MetaDataConstants;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.services.RequestConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.*;
import org.apache.tapestry5.ioc.services.ServiceOverride;
import org.apache.tapestry5.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tynamo.security.services.PageService;

/**
 * patch Tynamo security to load classes from the
 * our package, otherwise the library doesn't have access to our
 * principal classes
 * 
 * @author lprimak
 */
public class SecurityModule 
{
    public static void contributeFactoryDefaults(MappedConfiguration<String, String> configuration)
    {
        configuration.add(Symbols.LOGIN_URL, "/" + SECURITY_PATH_PREFIX + "/login");
        configuration.add(Symbols.SUCCESS_URL, "/index");
        configuration.add(Symbols.UNAUTHORIZED_URL, "");
        configuration.add(Symbols.REMEMBER_ME_DURATION, Integer.toString(2 * 7)); // 2 weeks
        configuration.add(Symbols.INVALID_AUTH_DELAY, Integer.toString(3));
        configuration.add(Symbols.SESSION_EXPIRED_MESSAGE, "Your Session Has Expired");
    }
    
    
    public static void bind(ServiceBinder binder)
    {
        binder.bind(PageService.class, PageServiceOverride.class).withId("FLPageServiceOverride");
    }


    @Contribute(ServiceOverride.class)
    public static void overrideLoginScreen(MappedConfiguration<Class<?>, Object> configuration, @Local PageService override)
    {
        configuration.add(PageService.class, override);
    }
    
       
    public void contributeMetaDataLocator(MappedConfiguration<String, String> configuration)
    {
        configuration.add(String.format("%s:%s", SECURITY_PATH_PREFIX, MetaDataConstants.SECURE_PAGE), Boolean.toString(isSecure));
    }
    
    
    /**
     * See <a href="https://issues.apache.org/jira/browse/TAP5-1779" target="_blank">TAP5-1779</a>
     */
    @Contribute(RequestHandler.class)
    public void disableAssetDirListing(OrderedConfiguration<RequestFilter> configuration,
                    @Symbol(SymbolConstants.APPLICATION_VERSION) final String applicationVersion,
                    @Symbol(SymbolConstants.ASSET_PATH_PREFIX) final String assetPathPrefix)
    {
        configuration.add("DisableDirListing", new RequestFilter() {
            @Override
            public boolean service(Request request, Response response, RequestHandler handler) throws IOException
            {
                final String assetFolder = assetPathPrefix+ applicationVersion + "/" + 
                        RequestConstants.CONTEXT_FOLDER;
                if(request.getPath().startsWith(assetFolder) && request.getPath().endsWith("/"))
                {
                    return false;
                }
                else
                {
                    return handler.service(request, response);
                }
            }
        }, "before:AssetDispatcher");
    }      

    
    @Match("ComponentRequestFilter")
    public static ComponentRequestFilter decorateEJBSecurityInterceptor(ComponentRequestFilter filter)
    {
        return new SecurityInterceptorFilter(filter);
    }


//    @Match("RememberMeManager")
//    public RememberMeManager decorateRememberMeDefaults(@Autobuild CookieRememberMeManager mgr, 
//        @Symbol(Symbols.REMEMBER_ME_DURATION) Integer daysToRemember)
//    {
//            if(productionMode)
//            {
//                mgr.getCookie().setMaxAge(daysToRemember * 24 * 60 * 60);
//            }
//            else
//            {
//                mgr.getCookie().setMaxAge(-1);
//            }            
//        
//        return mgr;
//    }
    
    
    /**
     * Detects expired session and sets an attribute to indicate that fact
     */
//    public void contributeExceptionHandler(MappedConfiguration<Class<?>, ExceptionHandlerAssistant> configuration,
//            final SecurityService securityService, final RequestGlobals rg, final PageService pageService,
//            final RequestPageCache pageCache, final PageResponseRenderer renderer, final Cookies cookies)
//    {
//        ExceptionHandlerAssistant assistant = new ExceptionHandlerAssistantImpl(securityService, pageService, rg, pageCache, renderer, cookies);
//        configuration.override(ShiroException.class, assistant);
//    }
        
    
    public static class Symbols
    {
        public static final String LOGIN_URL = "flowlogix.security.loginurl";
        public static final String SUCCESS_URL = "flowlogix.security.successurl";
        public static final String UNAUTHORIZED_URL = "flowlogix.security.unauthorizedurl";        
        public static final String REMEMBER_ME_DURATION = "flowlogix.security.remembermeduration";        
        public static final String INVALID_AUTH_DELAY = "flowlogix.security.invalid-auth-delay";
        public static final String SESSION_EXPIRED_MESSAGE = "flowlogix.security.session-expired-message";
    }
    
    
    public static final String SECURITY_PATH_PREFIX = "flowlogix/security";
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;
    private @Inject @Symbol(SymbolConstants.PRODUCTION_MODE) boolean productionMode;
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityModule.class);
}
