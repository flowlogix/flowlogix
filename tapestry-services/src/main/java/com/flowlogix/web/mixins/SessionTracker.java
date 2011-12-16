/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import com.flowlogix.session.internal.SessionTrackerBase;
import com.flowlogix.session.internal.SessionTrackerHolder;
import javax.servlet.http.HttpSession;
import org.apache.tapestry5.annotations.CleanupRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.RequestGlobals;

/**
 *
 * @author lprimak
 */
public class SessionTracker implements SessionTrackerBase
{
    @Override
    public boolean isValidSession() 
    {
        return Boolean.TRUE.equals(hasSession);
    }    
    
        
    @CleanupRender
    private void setSession()
    {
        if (rg.getRequest().isXHR() == false)
        {
            HttpSession session = rg.getHTTPServletRequest().getSession(false);
            hasSession = session != null;
            if(hasSession == true)
            {
                holder.setPageSession(rg.getActivePageName(), session, this);
            }
        }
    }

    
    private @Inject RequestGlobals rg;  
    private @Persist Boolean hasSession;
    private SessionTrackerHolder holder = SessionTrackerHolder.get();
}
