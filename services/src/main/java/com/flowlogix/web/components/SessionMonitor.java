/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.components;

import com.flowlogix.web.mixins.SessionTracker;
import com.flowlogix.web.services.SecurityModule.Symbols;
import javax.validation.constraints.NotNull;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.SessionAttribute;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 *
 * @author lprimak
 */
@Import(library = "SessionMonitor.js")
public class SessionMonitor extends SessionTracker
{
    JSONObject onCheckidle() 
    {
        if(keepAlive && isValidSession())
        {
            onRefresh();
        }
        JSONObject object = new JSONObject();
        if(isValidSession())
        {
            object.put("nextCheck", idleCheck);
        }
        else if(SecurityUtils.getSubject().isRemembered())
        {
            object.put("reloadPageOnly", true);
        }
        return object;
    }
    

    JSONObject onRefresh() 
    {
        Session session = SecurityUtils.getSubject().getSession(false);
        if(session != null)
        {
            session.touch();
        }
        return null;
    }

    
    JSONObject onEnd() 
    {
        if(sessionExpired)
        {
            loginSessionExpiredMessage = loginExpiredMessage;
        }
        return new JSONObject();
    }
    
    
    @SetupRender
    public void init()
    {
        if(!endedHandler.isEmpty())
        {
            _endedHandler = "'" + endedHandler + "'";
        }
    }

    
    @AfterRender
    public void afterRender() 
    {
        Link link = componentResources.createEventLink(eventName);
        String baseURI = link.toAbsoluteURI(isSecure);
        int index = baseURI.indexOf(":" + eventName);
        String defaultURIparameters = baseURI.substring(index + eventName.length() + 1);
        defaultURIparameters += "".equals(defaultURIparameters) ? "?" : "&";
        defaultURIparameters += KEEPALIVE_NAME + "=";
        baseURI = baseURI.substring(0, index + 1);

        
        JSONObject spec = new JSONObject();
        spec.put("contextPath", request.getContextPath());
        spec.put("baseURI", baseURI);
        spec.put("defaultURIparameters", defaultURIparameters);
        spec.put("keepAlive", keepAlive);
        spec.put("endOnClose", true);
        spec.put("idleCheckSeconds", idleCheck);
        spec.put("endedHandler", _endedHandler);
        jsSupport.addInitializerCall("sessionMonitor", spec);
    }
    
    
    @Parameter("15") private int idleCheck;
    @Parameter(defaultPrefix = BindingConstants.LITERAL, value = "end") @NotNull private String endedHandler;
    @Parameter("false") private boolean keepAlive;
    @Parameter("false") private boolean sessionExpired;
    private @SessionAttribute String loginSessionExpiredMessage;
    private @Inject @Symbol(Symbols.SESSION_EXPIRED_MESSAGE) String loginExpiredMessage;
    
    @Inject private ComponentResources componentResources;
    @Environmental private JavaScriptSupport jsSupport;    
    @Inject private Request request;
    
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;
    private String _endedHandler;

    private static final String eventName = "checkidle";
    private static final String KEEPALIVE_NAME = "keepAlive";
}
