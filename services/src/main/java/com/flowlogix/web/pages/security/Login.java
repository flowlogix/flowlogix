/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.pages.security;

import java.io.IOException;
import java.io.PrintWriter;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.BeginRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.apache.tapestry5.services.ExceptionReporter;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;

/**
 *
 * @author lprimak
 */
public class Login implements ExceptionReporter
{
    @SetupRender
    public void init()
    {
        isSecure = Boolean.valueOf(symbolProvider.valueForSymbol(SymbolConstants.SECURE_ENABLED));
    }
        
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
                    + link.toAbsoluteURI(isSecure) + "\"\n}");
            writer.close();
            System.err.println("Link: " + writer);
        }
        return null;
    }
   
   
    @Override
    public void reportException(Throwable exception)
    {
        this.exception = exception;
    }

    
    public Throwable getException()
    {
        return exception;
    }

    
    public String getMessage()
    {
        if (exception != null)
        {
            return exception.getMessage() + " Try login.";
        } else
        {
            return "";
        }
    }

    
    private Throwable exception;
    private @Inject Request request;
    private @Inject Response response;
    private @Inject PageRenderLinkSource linkSource;
    private @Inject SymbolSource symbolProvider;  
    private @Persist Boolean isSecure;
}