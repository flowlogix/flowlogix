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

import com.flowlogix.web.services.internal.AssetPathProcessor;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.services.RequestConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.BaseURLSource;
import org.apache.tapestry5.services.Context;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestFilter;
import org.apache.tapestry5.services.RequestHandler;
import org.apache.tapestry5.services.Response;

/**
 * Add Shiro SecurityInterceptorFilter filter, 
 * disable directory listing
 * 
 * @author lprimak
 */
public class SecurityModule 
{
    public SecurityModule(@Symbol(SymbolConstants.ASSET_PATH_PREFIX) String assetPathPrefix)
    {
        this.assetPathPrefix = assetPathPrefix;
        pathProcessor = new AssetPathProcessor(assetPathPrefix);
    }
    
    
    public static void contributeFactoryDefaults(MappedConfiguration<String, String> configuration)
    {
        configuration.add(Symbols.DISABLE_PORTNUM_REMOVAL, Boolean.FALSE.toString());
    }
    
    
    /**
     * See <a href="https://issues.apache.org/jira/browse/TAP5-1779" target="_blank">TAP5-1779</a>
     * @param configuration
     * @param applicationVersion
     * @param ctxt
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
                    File realFile = ctxt.getRealFile(pathProcessor.removeAssetPathPart(request.getPath()));
                    if(request.getPath().endsWith("/") || (realFile != null && realFile.isDirectory()))
                    {
                        return false;
                    }
                }
                return handler.service(request, response);
            }
        }, "before:AssetDispatcher");
    }      

    
    /**
     * Fix for https://issues.apache.org/jira/browse/TAP5-1973
     * Remove appending the port number for URLs
     * @param source
     * @param disablePortnumRemoval
     * @return 
     */
    @Match("BaseURLSource")
    public BaseURLSource decorateDisablePortNumAppend(final BaseURLSource source,
        @Symbol(Symbols.DISABLE_PORTNUM_REMOVAL) final Boolean disablePortnumRemoval)
    {
        return new BaseURLSource() {
            @Override
            public String getBaseURL(boolean secure)
            {
                String rv = source.getBaseURL(secure);
                if(disablePortnumRemoval)
                {
                    return rv;
                }
                else
                {
                    return removePortNumber.matcher(rv).replaceFirst("");
                }
            }
        };
    }

    
    public static class Symbols
    {
        public static final String REMEMBER_ME_DURATION = "flowlogix.security.remembermeduration";        
        public static final String INVALID_AUTH_DELAY = "flowlogix.security.invalid-auth-delay";
        public static final String SESSION_EXPIRED_MESSAGE = "flowlogix.security.session-expired-message";
        public static final String DISABLE_PORTNUM_REMOVAL = "flowlogix.security.disable-portnum-removal";
    }
    
    
    public static final String SECURITY_PATH_PREFIX = "flowlogix/security";
   
    private final AssetPathProcessor pathProcessor;
    private final String assetPathPrefix;
    private final Pattern removePortNumber = Pattern.compile(":(80|443)$");
}
