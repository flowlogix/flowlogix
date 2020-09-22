/*
 * Copyright 2014 lprimak.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flowlogix.web.services;

import static com.flowlogix.web.services.SecurityModule.SECURITY_PATH_PREFIX;
import com.flowlogix.web.services.internal.ExceptionHandlerAssistantImpl;
import org.apache.shiro.ShiroException;
import org.apache.shiro.mgt.RememberMeManager;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.tapestry5.MetaDataConstants;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Local;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.tynamo.exceptionpage.ExceptionHandlerAssistant;

/**
 * patch Tynamo security to load classes from the
 * our package, otherwise the library doesn't have access to our
 * principal classes
 * 
 * @author lprimak
 */
public class TynamoSecurityModule
{
    public static void contributeFactoryDefaults(MappedConfiguration<String, String> configuration)
    {
        configuration.add(SecurityModule.Symbols.REMEMBER_ME_DURATION, Integer.toString(2 * 7)); // 2 weeks
        configuration.add(SecurityModule.Symbols.INVALID_AUTH_DELAY, Integer.toString(3));
        configuration.add(SecurityModule.Symbols.SESSION_EXPIRED_MESSAGE, "Your Session Has Expired");
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
    
    
    @Match("RememberMeManager")
    public RememberMeManager decorateRememberMeDefaults(RememberMeManager _mgr, 
        @Symbol(SecurityModule.Symbols.REMEMBER_ME_DURATION) Integer daysToRemember)
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
     * @param configuration
     * @param assistant
     */
    public void contributeExceptionHandler(MappedConfiguration<Class<?>, ExceptionHandlerAssistant> configuration,
        @Local ExceptionHandlerAssistant assistant)
    {
        configuration.override(ShiroException.class, assistant);
    }
    
    
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;
    private @Inject @Symbol(SymbolConstants.PRODUCTION_MODE) boolean productionMode;
}
