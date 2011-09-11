/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.components;

import org.apache.shiro.SecurityUtils;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.SymbolSource;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 *
 * @author lprimak
 */
@Import(library = "SessionMonitor.js")
public class SessionMonitor 
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
        SecurityUtils.getSubject().getSession(false).touch();
        return null;
    }

    JSONObject onEnd() 
    {
        return new JSONObject();
    }
    
    @SetupRender
    public void init()
    {
        isSecure = Boolean.valueOf(symbolProvider.valueForSymbol(SymbolConstants.SECURE_ENABLED));
        setSession();
        if(endedHandler != null)
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

        jsSupport.addScript(String.format("%s = new SessionMonitor('%s', '%s', '%s', %s, true, %s, %s);", 
                componentResources.getId(), request.getContextPath(), baseURI, defaultURIparameters, 
                keepAlive, idleCheck, _endedHandler));
    }
    
    
    private void setSession()
    {
        hasSession = rg.getHTTPServletRequest().getSession(false) != null;
    }
    
    
    private boolean isValidSession() 
    {
        return Boolean.TRUE.equals(hasSession);
    }


    @Parameter("15") private int idleCheck;
    @Parameter(defaultPrefix = BindingConstants.LITERAL) private String endedHandler;
    @Parameter("false") private boolean keepAlive;
    
    @Inject private ComponentResources componentResources;
    @Environmental private JavaScriptSupport jsSupport;    
    @Inject private Request request;
    private @Inject RequestGlobals rg;  
    private @Inject SymbolSource symbolProvider;
    
    private @Persist Boolean isSecure;
    private @Persist Boolean hasSession;
    private String _endedHandler;

    private static final String eventName = "checkidle";
    private static final String KEEPALIVE_NAME = "keepAlive";
}
