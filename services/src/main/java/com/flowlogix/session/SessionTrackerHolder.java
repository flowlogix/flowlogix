/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.session;

import com.flowlogix.web.mixins.SessionTracker;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;

/**
 *
 * @author lprimak
 */
public class SessionTrackerHolder
{
    private SessionTrackerHolder()
    {
        // singleton
    }
    
    
    public static SessionTrackerHolder get()
    {
        return holder;
    }
    
    
    public boolean isValidSession(String pageName, HttpSession session)
    {
        if(session == null)
        {
            return false;
        }
        else if(trackers.containsKey(pageName) && trackers.get(pageName).containsKey(session.hashCode()))
        {
            return trackers.get(pageName).get(session.hashCode()).isValidSession();
        }
        else
        {
            return false;
        }
    }
    
    
    public void setPageSession(String pageName, HttpSession session, SessionTracker tracker)
    {
        Map<Integer, SessionTrackerBase> map = trackers.get(pageName);
        if(map == null)
        {
            map = Collections.synchronizedMap(new HashMap<Integer, SessionTrackerBase>());
            trackers.put(pageName, map);        
        }
        map.put(session.hashCode(), tracker);
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
    

    private Map<String, Map<Integer, SessionTrackerBase>> trackers = Collections.synchronizedMap(new HashMap<String, Map<Integer, SessionTrackerBase>>());
    private static final SessionTrackerHolder holder = new SessionTrackerHolder();
}
