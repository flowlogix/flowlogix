/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.base;

import com.flowlogix.session.SessionTrackerHolder;
import com.flowlogix.web.services.SecurityModule.Symbols;
import java.io.IOException;
import lombok.Getter;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.BeginRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.SessionAttribute;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.ExceptionReporter;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.RequestGlobals;

/**
 *
 * @author lprimak
 */
public abstract class LoginBase implements ExceptionReporter
{        
    public String getMessage()
    {
        if(checkExpiredMessage())
        {
            return loginSessionExpiredMessage;
        }
        else
        {
            return getLoginMessage();
        }
    }
    

    /**
     * Return the actual message to display on the login page,
     * this could be an exception but is probably more friendly message
     * 
     * @return String, Message to display on the login page
     */
    protected abstract String getLoginMessage();
    
    
    private boolean checkExpiredMessage()
    {
        if(rg.getRequest().isXHR())
        {
            return false;
        }
        if(showSessionExpiredMessage == null)
        {
            showSessionExpiredMessage = false;
        }
        if(showSessionExpiredMessage)
        {
            loginSessionExpiredMessage = loginExpiredMessage;
            showSessionExpiredMessage = false;
        }
        return loginSessionExpiredMessage != null && (!loginSessionExpiredMessage.isEmpty());
    }
    
    
    @Override
    public void reportException(Throwable exception)
    {
        this.exception = exception;
    }
    
    
    /**
     * when a user is redirected to the Logon page through a AJAX
     * request after their session has expired,
     * return a JSON response that redirects them to the logon page
     * @return
     * @throws IOException
     */
    @BeginRender
    private Object checkForAjax() throws IOException
    {
        if(rg.getRequest().isXHR())
        {
            showSessionExpiredMessage = true;
        }
        SessionTrackerHolder.redirectToSelf(rg, linkSource, isSecure);
        return null;
    }
   
    private @Inject RequestGlobals rg;
    private @Inject PageRenderLinkSource linkSource;
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;
    private @SessionAttribute Boolean showSessionExpiredMessage;
    private @Persist(PersistenceConstants.FLASH) String loginSessionExpiredMessage;
    private @Inject @Symbol(Symbols.SESSION_EXPIRED_MESSAGE) String loginExpiredMessage;
    private @Getter Throwable exception;
}
