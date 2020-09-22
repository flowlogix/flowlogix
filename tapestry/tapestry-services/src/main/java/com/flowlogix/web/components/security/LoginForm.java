/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.components.security;

import com.flowlogix.web.base.LoginFormBase;
import lombok.Getter;
import lombok.Setter;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.util.StringUtils;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;

/**
 * Default Login form component
 *
 * @author lprimak
 */
public class LoginForm extends LoginFormBase
{
    public Object onActionFromFlowlogixLoginForm()
    {
        try
        {
            return login(login, password, flowlogixRememberMe, null);
        }
        catch (UnknownAccountException e)
        {
            loginErrorMessage = "Account not exists";
            return null;
        } catch (IncorrectCredentialsException e)
        {
            loginErrorMessage = "Wrong password";
            return null;
        } catch (LockedAccountException e)
        {
            loginErrorMessage = "Account locked";
            return null;
        } catch (AuthenticationException e)
        {
            loginErrorMessage = "Authentication Error";
            return null;
        }
    }

    
    public String getLoginErrorMessage()
    {
        if (StringUtils.hasText(loginErrorMessage))
        {
            return loginErrorMessage;
        } else
        {
            return " ";
        }
    }
    
    
    private @Parameter("false") @Getter boolean rememberMeDisabled;

    private @Property String login;
    private @Property String password;
    private @Property boolean flowlogixRememberMe;
    private @Persist(PersistenceConstants.FLASH) @Setter String loginErrorMessage;
}
