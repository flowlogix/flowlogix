/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.base;

import java.io.IOException;
import java.io.PrintWriter;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.BeginRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;

/**
 *
 * @author lprimak
 */
public class LoginBase
{        
    /**
     * when a user is redirected to the Logon page through a AJAX
     * request after their session has expired,
     * return an json response that redirects them to the logon page
     * @return
     * @throws IOException
     */
    @BeginRender
    private Object checkForAjax() throws IOException
    {
        if (request.isXHR())
        {
            PrintWriter writer = response.getPrintWriter("application/json");
            // redirect to the same page
            Link link = linkSource.createPageRenderLink(request.getPath().replaceFirst("\\..*", "").substring(1));
            writer.write("{\n\t\"redirectURL\" : \""
                    + link.toAbsoluteURI(Boolean.valueOf(symbolProvider.valueForSymbol(SymbolConstants.SECURE_ENABLED))) + "\"\n}");
            writer.close();
        }
        return null;
    }
   
    private @Inject Request request;
    private @Inject Response response;
    private @Inject PageRenderLinkSource linkSource;
    private @Inject SymbolSource symbolProvider;  
}
