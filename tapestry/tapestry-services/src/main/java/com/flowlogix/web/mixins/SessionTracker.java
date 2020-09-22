/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import com.flowlogix.session.internal.SessionTrackerUtil;
import org.apache.tapestry5.annotations.CleanupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Session;

/**
 * Internal mixin to support {@link com.flowlogix.web.services.annotations.AJAX} annotation
 *
 * @author lprimak
 */
public class SessionTracker
{
    public boolean isValidSession()
    {
        return SessionTrackerUtil.isValidSession(rg.getActivePageName(), rg.getRequest().getSession(false));
    }
    
    
    @CleanupRender
    private void setSession()
    {
        if (rg.getRequest().isXHR() == false)
        {
            Session session = rg.getRequest().getSession(false);
            boolean hasSession = session != null;
            if(hasSession == true)
            {
                SessionTrackerUtil.setPageSession(rg.getActivePageName(), session);
            }
        }
    }

    
    private @Inject RequestGlobals rg;  
}
