package com.flowlogix.web.base;

import com.flowlogix.web.services.SecurityModule.Symbols;
import lombok.Getter;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.SessionAttribute;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.ExceptionReporter;
import org.apache.tapestry5.services.RequestGlobals;

/** 
 * <a href="http://code.google.com/p/flowlogix/wiki/TLLoginBase"
 *    target="_blank">See Documentation</a>
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
    
    
    /**
     * Override if you want to disable remember me checkbox
     */
    public boolean isRememberMeDisabled()
    {
        return false;
    }
    
    
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
    
    
    private @Inject RequestGlobals rg;
    private @SessionAttribute Boolean showSessionExpiredMessage;
    private @Persist(PersistenceConstants.FLASH) String loginSessionExpiredMessage;
    private @Inject @Symbol(Symbols.SESSION_EXPIRED_MESSAGE) String loginExpiredMessage;
    private @Getter Throwable exception;
}
