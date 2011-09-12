/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.components.security;

import java.io.IOException;
import org.apache.shiro.ShiroException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
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
public class LoginFormBase
{
    public String login(String tynamoLogin, String tynamoPassword, boolean tynamoRememberMe, String host) throws ShiroException
    {
        Subject currentUser = securityService.getSubject();

        if (currentUser == null)
        {
            throw new IllegalStateException("Subject can`t be null");
        }

        UsernamePasswordToken token = new UsernamePasswordToken(tynamoLogin, tynamoPassword);
        token.setRememberMe(tynamoRememberMe);
        token.setHost(host);

        currentUser.login(token);

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


    @Inject private Response response;
    @Inject private RequestGlobals requestGlobals;
    @Inject private SecurityService securityService;
    @Inject private PageService pageService;
    private static final Logger logger = LoggerFactory.getLogger(LoginForm.class);
}
