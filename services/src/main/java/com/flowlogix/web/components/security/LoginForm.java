/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.components.security;

import java.io.IOException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.StringUtils;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tynamo.security.services.PageService;
import org.tynamo.security.services.SecurityService;

/**
 *
 * @author lprimak
 */
/**
 * Login form component
 *
 */
public class LoginForm
{
    public Object onActionFromTynamoLoginForm()
    {
        Subject currentUser = securityService.getSubject();

        if (currentUser == null)
        {
            throw new IllegalStateException("Subject can`t be null");
        }

        UsernamePasswordToken token = new UsernamePasswordToken(tynamoLogin, tynamoPassword);
        token.setRememberMe(tynamoRememberMe);


        try
        {
            currentUser.login(token);
        } catch (UnknownAccountException e)
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


        SavedRequest savedRequest = WebUtils.getAndClearSavedRequest(requestGlobals.getHTTPServletRequest());

        if (savedRequest != null && savedRequest.getMethod().equalsIgnoreCase("GET"))
        {
            try
            {
                response.sendRedirect(savedRequest.getRequestUrl());
                return null;
            } catch (IOException e)
            {
                logger.warn("Can't redirect to saved request.");
                return pageService.getSuccessPage();
            }
        } else
        {
            return pageService.getSuccessPage();
        }

    }

    public void setLoginMessage(String loginMessage)
    {
        this.loginMessage = loginMessage;
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


    @Property
    private String tynamoLogin;
    @Property
    private String tynamoPassword;
    @Property
    private boolean tynamoRememberMe;
    @Persist(PersistenceConstants.FLASH)
    private String loginMessage;
    @Inject
    private Response response;
    @Inject
    private RequestGlobals requestGlobals;
    @Inject
    private SecurityService securityService;
    @Inject
    private PageService pageService;
    private static final Logger logger = LoggerFactory.getLogger(LoginForm.class);
}

