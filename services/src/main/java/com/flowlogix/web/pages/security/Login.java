/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.pages.security;

import com.flowlogix.web.base.LoginBase;

/**
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
            return getException().getMessage() + " Please Log In";
        }
    }
}
