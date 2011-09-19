/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import com.flowlogix.session.SessionTrackerBase;
import com.flowlogix.session.SessionTrackerSSO;
import javax.servlet.http.HttpSession;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.annotations.SetupRender;
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
    
        
    @SetupRender
    private void setSession()
    {
        if (rg.getRequest().isXHR() == false)
        {
            HttpSession session = rg.getHTTPServletRequest().getSession(false);
            hasSession = session != null;
            if(ssoExists == false)
            {
                sso = new SessionTrackerSSO();
            }
            if(hasSession == true)
            {
                sso.setPageSession(rg.getActivePageName(), this);
            }
        }
    }

    
    private @Inject RequestGlobals rg;  
    private @Persist Boolean hasSession;
    private @SessionState(create = false) SessionTrackerSSO sso;
    private boolean ssoExists;
}
