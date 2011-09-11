/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.components;

import javax.servlet.http.HttpSession;
import org.apache.shiro.SecurityUtils;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Session;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 *
 * @author lprimak
 */
@Import(library = "SessionMonitor.js")
public class SessionMonitor 
{
    private static final String eventName = "checkidle";
    private static final String KEEPALIVE_NAME = "keepAlive";
    
    @Inject private ComponentResources componentResources;
    @Environmental private JavaScriptSupport jsSupport;
    
    @Inject private Request request;

    @Parameter("15") private int idleCheck;
    @Parameter(defaultPrefix = BindingConstants.LITERAL) private String endedHandler;
    @Parameter("false") private boolean keepAlive;
    
    private String _endedHandler;
    private @Persist Integer currentSssionId;
    private @Inject RequestGlobals rg;
    private @Persist Boolean hasSession;
    

    JSONObject onCheckidle() 
    {
        if(keepAlive && request.isRequestedSessionIdValid())
        {
            onRefresh();
        }
        JSONObject object = new JSONObject();
        if(request.isRequestedSessionIdValid() && verifyEqualId())
        {
            object.put("nextCheck", idleCheck);
        }
        if(SecurityUtils.getSubject().isRemembered())
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
        if(endedHandler != null)
        {
            _endedHandler = "'" + endedHandler + "'";
        }
    }

    
    @AfterRender
    public void afterRender() 
    {
        Link link = componentResources.createEventLink(eventName);
        String baseURI = link.toAbsoluteURI();
        int index = baseURI.indexOf(":" + eventName);
        String defaultURIparameters = baseURI.substring(index + eventName.length() + 1);
        defaultURIparameters += "".equals(defaultURIparameters) ? "?" : "&";
        defaultURIparameters += KEEPALIVE_NAME + "=";
        baseURI = baseURI.substring(0, index + 1);

        jsSupport.addScript(String.format("%s = new SessionMonitor('%s', '%s', '%s', %s, true, %s, %s);", 
                componentResources.getId(), request.getContextPath(), baseURI, defaultURIparameters, 
                keepAlive, idleCheck, _endedHandler));
    }

    
    private boolean verifyEqualId() 
    {
        HttpSession _id = rg.getHTTPServletRequest().getSession(false);
        Integer id = _id != null? _id.hashCode() : null;
        boolean rv = id != null;
        if(currentSssionId != null)
        {
            rv = (currentSssionId.equals(id));
        }
        currentSssionId = id;
        
        return rv;
    }
}
