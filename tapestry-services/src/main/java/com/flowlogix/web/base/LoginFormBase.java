package com.flowlogix.web.base;

import com.flowlogix.web.components.security.LoginForm;
import com.flowlogix.web.services.ExternalPageLink;
import com.flowlogix.web.services.SecurityModule;
import java.io.IOException;
import java.net.URL;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.ShiroException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.tynamo.security.SecuritySymbols;
import org.tynamo.security.internal.services.LoginContextService;
import org.tynamo.security.services.SecurityService;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLLoginBase"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Slf4j
public class LoginFormBase
{
    @SneakyThrows({InterruptedException.class, IOException.class})
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

        final String successLink = externalLink.createLink(contextService.getSuccessPage(), true);

        if (redirectToSavedUrl)
        {
            contextService.redirectToSavedRequest(successLink);
            return null;
        }
        
        return new URL(successLink);
    }
    
    
    @SetupRender
    public void resetJavaScriptDisabled()
    {
        javaScriptDisabled = true;
    }
    
    
    @AfterRender
    public void detectJavaScript()
    {
        Link link = componentResources.createEventLink(ENABLE_JS_EVENT);
        String eventURI = link.toAbsoluteURI(requestGlobals.getRequest().isSecure());
        jsSupport.require("flowlogix/DetectJS").with(eventURI);
    }
    
    
    @OnEvent(value = ENABLE_JS_EVENT)
    public void enableJavaScriptAvail()
    {
        javaScriptDisabled = false;
    }
    

    private @Inject Response response;
    private @Inject RequestGlobals requestGlobals;
    private @Inject SecurityService securityService;
    private @Inject LoginContextService contextService;
    private @Inject @Symbol(SecurityModule.Symbols.INVALID_AUTH_DELAY) int authDelayInterval;
    private @Inject @Symbol(SecuritySymbols.REDIRECT_TO_SAVED_URL) boolean redirectToSavedUrl;
    private @Environmental JavaScriptSupport jsSupport;
    private @Inject ComponentResources componentResources;
    private @SessionAttribute Boolean javaScriptDisabled;
    private @Inject ExternalPageLink externalLink;

    public static final String ENABLE_JS_EVENT = "enableJSOnLogin";
}
