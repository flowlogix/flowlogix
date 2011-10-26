/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.pages.security;

import com.flowlogix.web.base.LoginBase;
import org.apache.tapestry5.annotations.SessionAttribute;
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
        if (exception != null)
        {
            return exception.getMessage() + " Try login.";
        } else
        {
            if(loginSessionExpiredMessage != null && (!loginSessionExpiredMessage.isEmpty()))
            {
                String rv = loginSessionExpiredMessage;
                loginSessionExpiredMessage = null;
                return rv;
            }
            return "";
        }
    }


    private @SessionAttribute String loginSessionExpiredMessage;
    private Throwable exception;
}