/*
 * Copyright 2011 lprimak.
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

import java.net.MalformedURLException;
import java.net.URL;
import lombok.SneakyThrows;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.util.UnknownValueException;
import org.apache.tapestry5.services.BaseURLSource;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;

/**
 * Creates links to pages that are external to tapestry,
 * if a tapestry page was not found
 * 
 * @author lprimak
 */
public class ExternalPageLink
{
    @SneakyThrows(MalformedURLException.class)
    public String createLink(String pageName)
    {
        try
        {
            // try Tapestry page
            return linkSource.createPageRenderLink(pageName).toURI();
        }
        catch(UnknownValueException e)
        {
            // try external page
            return new URL(String.format("%s%s/%s",
                    urlSource.getBaseURL(isSecure), request.getContextPath(),
                    pageName)).toExternalForm();
        }
    }
    
    
    private @Inject Request request;
    private @Inject PageRenderLinkSource linkSource;
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;  
    private @Inject BaseURLSource urlSource;
}
