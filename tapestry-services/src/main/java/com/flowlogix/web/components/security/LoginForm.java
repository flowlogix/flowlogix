/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.components.security;

import com.flowlogix.web.base.LoginFormBase;
import lombok.Setter;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.util.StringUtils;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;

/**
 * Default Login form component
 *
 * @author lprimak
 */
public class LoginForm extends LoginFormBase
{
    public Object onActionFromTynamoLoginForm()
    {
        try
        {
            Object rv = login(login, password, tynamoRememberMe, null);
            return rv;
        }
        catch (UnknownAccountException e)
        {
            loginMessage = "Account not exists";
            return null;
        } catch (IncorrectCredentialsException e)
        {
            loginMessage = "Wrong password";
            return null;
        } catch (LockedAccountException e)
        {
            loginMessage = "Account locked";
            return null;
        } catch (AuthenticationException e)
        {
            loginMessage = "Authentication Error";
            return null;
        }
    }

    public String getLoginMessage()
    {
        if (StringUtils.hasText(loginMessage))
        {
            return loginMessage;
        } else
        {
            return " ";
        }
    }


    private @Property String login;
    private @Property String password;
    private @Property boolean tynamoRememberMe;
    private @Persist(PersistenceConstants.FLASH)
    @Setter String loginMessage;
}
