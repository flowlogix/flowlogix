/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.session.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpSession;
import lombok.Data;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;

/**
 * Internal support class for {@link com.flowlogix.web.services.annotations.AJAX} annotation
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
            return trackers.get(pageName).get(session.hashCode()).getTracker().isValidSession();
        }
        else
        {
            return false;
        }
    }
    
    
    public void setPageSession(String pageName, HttpSession session, SessionTrackerBase tracker)
    {
        Map<Integer, Value> map = trackers.get(pageName);
        if(map == null)
        {
            map = Collections.synchronizedMap(new HashMap<Integer, Value>());
            trackers.put(pageName, map);        
        }
        map.put(session.hashCode(), new Value(tracker, session));
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
    
    
    public void purge(HttpSession session)
    {
        for(Map<Integer, Value> map : trackers.values())
        {
            map.remove(session.hashCode());
        }
    }
    
    
    private static @Data class Value
    {
        public Value(SessionTrackerBase tracker, HttpSession session)
        {
            this.tracker = tracker;
            this.session = session;
        }
        
        
        private final SessionTrackerBase tracker;
        private final HttpSession session;
    }
    

    private Map<String, Map<Integer, Value>> trackers = Collections.synchronizedMap(new HashMap<String, Map<Integer, Value>>());
    private static final SessionTrackerHolder holder = new SessionTrackerHolder();
}
