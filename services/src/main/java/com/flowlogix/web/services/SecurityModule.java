/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services;

import com.flowlogix.web.services.internal.PageServiceOverride;
import com.flowlogix.web.services.internal.SecurityInterceptorFilter;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import org.apache.shiro.io.DefaultSerializer;
import org.apache.shiro.io.SerializationException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.tapestry5.MetaDataConstants;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.services.RequestConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.ServiceOverride;
import org.apache.tapestry5.services.ComponentRequestFilter;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestFilter;
import org.apache.tapestry5.services.RequestHandler;
import org.apache.tapestry5.services.Response;
import org.tynamo.security.services.PageService;
import org.tynamo.security.services.TapestryRealmSecurityManager;

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
    }


    @Contribute(ServiceOverride.class)
    public static void overrideLoginScreen(MappedConfiguration<Class<?>, Object> configuration)
    {
        configuration.addInstance(PageService.class, PageServiceOverride.class);
    }
    
       
    public void contributeMetaDataLocator(MappedConfiguration<String, String> configuration)
    {
        configuration.add(MetaDataConstants.SECURE_PAGE, Boolean.toString(isSecure));
    }
    
    
    @Contribute(RequestHandler.class)
    public void disableAssetDirListing(OrderedConfiguration<RequestFilter> configuration,
                    @Symbol(SymbolConstants.APPLICATION_VERSION) final String applicationVersion)
    {
        configuration.add("DisableDirListing", new RequestFilter() {

            @Override
            public boolean service(Request request, Response response, RequestHandler handler) throws IOException
            {
                final String assetFolder = RequestConstants.ASSET_PATH_PREFIX + applicationVersion + "/" + 
                        RequestConstants.CONTEXT_FOLDER;
                System.err.println("Folder: " + assetFolder + ", path: " + request.getPath());
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
    public static ComponentRequestFilter decorateComponentRequestFilter(ComponentRequestFilter filter)
    {
        return new SecurityInterceptorFilter(filter);
    }

    
    @Match("WebSecurityManager")
    public static WebSecurityManager decorateWebSecurityManager(WebSecurityManager _manager, 
        @Symbol(Symbols.REMEMBER_ME_DURATION) Integer daysToRemember)
    {
        if (_manager instanceof TapestryRealmSecurityManager)
        {
            TapestryRealmSecurityManager manager = (TapestryRealmSecurityManager)_manager;
            CookieRememberMeManager mgr = (CookieRememberMeManager)manager.getRememberMeManager();
            mgr.getCookie().setMaxAge(daysToRemember * 24 * 60 * 60);
            
            mgr.setSerializer(new Serialize<PrincipalCollection>());
        }
        return null;
    }
    
    
    private static class Serialize<T> extends DefaultSerializer<T> 
    {
        @Override
        public T deserialize(byte[] serialized) throws SerializationException
        {
            if (serialized == null)
            {
                String msg = "argument cannot be null.";
                throw new IllegalArgumentException(msg);
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
            BufferedInputStream bis = new BufferedInputStream(bais);
            try
            {
                ObjectInputStream ois = new ObjectInputStream(bis)
                {
                    @Override
                    public Class resolveClass(ObjectStreamClass desc) throws ClassNotFoundException
                    {
                        return Thread.currentThread().getContextClassLoader().loadClass(desc.getName());
                    }
                };
                @SuppressWarnings({"unchecked"})
                T deserialized = (T) ois.readObject();
                ois.close();
                return deserialized;
            } catch (Exception e)
            {
                String msg = "Unable to deserialze argument byte array.";
                throw new SerializationException(msg, e);
            }
        }
    }    
    
    
    public static class Symbols
    {
        public static final String LOGIN_URL = "flowlogix.security.loginurl";
        public static final String SUCCESS_URL = "flowlogix.security.successurl";
        public static final String UNAUTHORIZED_URL = "flowlogix.security.unauthorizedurl";        
        public static final String REMEMBER_ME_DURATION = "flowlogix.security.remembermeduration";        
        public static final String INVALID_AUTH_DELAY = "flowlogix.security.invalid-auth-delay";
    }
    
    
    private static final String SECURITY_PATH_PREFIX = "flowlogix/security";
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;
}
