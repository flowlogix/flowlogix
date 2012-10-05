/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.session.internal;

import java.io.IOException;
import java.io.PrintWriter;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Session;

/**
 * Internal support class for {@link com.flowlogix.web.services.annotations.AJAX} annotation
 * 
 * @author lprimak
 */
public class SessionTrackerUtil
{
    public static boolean isValidSession(String pageName, Session session)
    {
        if(session == null)
        {
            return false;
        }
        return Boolean.TRUE.equals(session.getAttribute(SESSION_INIT_PREFIX + pageName));
    }
    
    
    public static void setPageSession(String pageName, Session session)
    {
        session.setAttribute(SESSION_INIT_PREFIX + pageName, Boolean.TRUE);
    }
    
    
    public static void redirectToSelf(RequestGlobals rg, PageRenderLinkSource linkSource) throws IOException
    {
        if(rg.getRequest().isXHR() == false)
        {
            return;
        }
        try (PrintWriter writer = rg.getResponse().getPrintWriter("application/json"))
        {
            Link link = linkSource.createPageRenderLink(rg.getRequest().getPath().replaceFirst("\\..*", "").substring(1));
            writer.write("{\n\t\"redirectURL\" : \""
                    + link.toAbsoluteURI(rg.getRequest().isSecure()) + "\"\n}");
        }
    }
    
    
    private static final String SESSION_INIT_PREFIX = "session$$init$$";
}
