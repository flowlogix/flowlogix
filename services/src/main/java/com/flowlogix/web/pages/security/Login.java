/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.pages.security;

import com.flowlogix.web.base.LoginBase;
import com.flowlogix.web.services.SecurityModule.Symbols;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.ExceptionReporter;

/**
 *
 * @author lprimak
 */
public class Login extends LoginBase implements ExceptionReporter
{
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
        if (exception != null && !checkExpiredMessage())
        {
            return exception.getMessage() + " Try login.";
        } else
        {
            if(checkExpiredMessage())
            {
                return loginSessionExpiredMessage;
            }
            return "";
        }
    }
    
    
    public boolean checkExpiredMessage()
    {
        return loginSessionExpiredMessage != null && (!loginSessionExpiredMessage.isEmpty());
    }
    
    
    JSONObject onSessionExpired() 
    {
        loginSessionExpiredMessage = loginExpiredMessage;
        return new JSONObject();
    }
    

    private @Persist(PersistenceConstants.FLASH) String loginSessionExpiredMessage;
    private Throwable exception;
    private @Inject @Symbol(Symbols.SESSION_EXPIRED_MESSAGE) String loginExpiredMessage;
}
