package com.flowlogix.web.pages.security;

import com.flowlogix.web.base.LoginBase;

/**
 * Default Login page
 * 
 * @author lprimak
 */
public class Login extends LoginBase
{
    @Override
    protected String getLoginMessage()
    {
        if(getException() == null)
        {
            return "";
        }
        else
        {
            return getException().getMessage() + " --- Please Log In: ";
        }
    }
}
