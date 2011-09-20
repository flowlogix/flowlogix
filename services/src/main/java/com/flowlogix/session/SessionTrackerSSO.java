/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.session;

import com.flowlogix.web.mixins.SessionTracker;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;

/**
 *
 * @author lprimak
 */
@SuppressWarnings(value = "serial")
public class SessionTrackerSSO implements Serializable 
{
    public boolean isValidSession(String pageName)
    {
        if(trackers.containsKey(pageName))
        {
            return trackers.get(pageName).isValidSession();
        }
        else
        {
            return false;
        }
    }
    
    
    public void setPageSession(String pageName, SessionTracker tracker)
    {
        trackers.put(pageName, tracker);
    }
    
    
    public static void redirectToSelf(RequestGlobals rg, PageRenderLinkSource linkSource,
            boolean isSecure) throws IOException
    {
        if(rg.getRequest().isXHR() == false)
        {
            return;
        }
        PrintWriter writer = rg.getResponse().getPrintWriter("application/json");
        // redirect to the same page
        Link link = linkSource.createPageRenderLink(rg.getRequest().getPath().replaceFirst("\\..*", "").substring(1));
        writer.write("{\n\t\"redirectURL\" : \""
                + link.toAbsoluteURI(isSecure) + "\"\n}");
        writer.close();
    }
    

    private transient Map<String, SessionTrackerBase> trackers = Collections.synchronizedMap(new HashMap<String, SessionTrackerBase>());
}
