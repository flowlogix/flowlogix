/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.components.security;

import com.flowlogix.web.services.SecurityModule;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.SneakyThrows;
import org.apache.shiro.ShiroException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.util.UnknownValueException;
import org.apache.tapestry5.services.BaseURLSource;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
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
    @SneakyThrows({MalformedURLException.class, InterruptedException.class})
    public Object login(String tynamoLogin, String tynamoPassword, boolean tynamoRememberMe, String host) throws ShiroException
    {
        Subject currentUser = securityService.getSubject();

        if (currentUser == null)
        {
            throw new IllegalStateException("Subject can`t be null");
        }

        UsernamePasswordToken token = new UsernamePasswordToken(tynamoLogin, tynamoPassword);
        token.setRememberMe(tynamoRememberMe);
        token.setHost(host);

        try
        {
            currentUser.login(token);
        } catch(AuthenticationException ae)
        {
            if(authDelayInterval > 0)
            {
                Thread.sleep(authDelayInterval * 1000);
            }
            throw ae;
        }

        SavedRequest savedRequest = WebUtils.getAndClearSavedRequest(requestGlobals.getHTTPServletRequest());

        Object successLink;
        try
        {
            successLink = linkSource.createPageRenderLink(pageService.getSuccessPage());
        }
        catch(UnknownValueException e)
        {
            // try an external page
            
            successLink = new URL(String.format("%s%s/%s", 
                    urlSource.getBaseURL(isSecure), request.getContextPath(),
                    pageService.getSuccessPage()));
        }

        if (savedRequest != null && savedRequest.getMethod().equalsIgnoreCase("GET"))
        {
            try
            {
                response.sendRedirect(savedRequest.getRequestUrl());
                return null;
            } catch (IOException e)
            {
                logger.warn("Can't redirect to saved request.");
                return successLink;
            }
        } else
        {
            return successLink;
        }
    }
    

    @Inject private Response response;
    @Inject private Request request;
    @Inject private RequestGlobals requestGlobals;
    @Inject private SecurityService securityService;
    @Inject private PageService pageService;
    private @Inject PageRenderLinkSource linkSource;
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;  
    private @Inject @Symbol(SecurityModule.Symbols.INVALID_AUTH_DELAY) int authDelayInterval;
    private @Inject BaseURLSource urlSource;



    private static final Logger logger = LoggerFactory.getLogger(LoginForm.class);
}
