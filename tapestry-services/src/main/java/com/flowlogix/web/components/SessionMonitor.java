package com.flowlogix.web.components;

import com.flowlogix.web.mixins.SessionTracker;
import javax.validation.constraints.NotNull;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLSessionMonitor"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
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

        
    @SetupRender
    public void init()
    {
        if(!endedHandler.isEmpty())
        {
            _endedHandler = endedHandler;
        }
        
        Link link = componentResources.createEventLink(eventName);
        String baseURI = link.toAbsoluteURI(request.isSecure());
        int index = baseURI.indexOf(":" + eventName);
        String defaultURIparameters = baseURI.substring(index + eventName.length() + 1);
        defaultURIparameters += "".equals(defaultURIparameters) ? "?" : "&";
        defaultURIparameters += KEEPALIVE_NAME + "=";
        baseURI = baseURI.substring(0, index + 1);

        JSONObject spec = new JSONObject();
        spec.put("contextPath", contextPath);
        spec.put("baseURI", baseURI);
        spec.put("defaultURIparameters", defaultURIparameters);
        spec.put("keepAlive", keepAlive);
        spec.put("endOnClose", true);
        spec.put("idleCheckSeconds", idleCheck);
        spec.put("endedHandler", _endedHandler);
        
        jsSupport.require("flowlogix/SessionMonitor").with(spec);
    }

        
    private @Parameter("15") int idleCheck;
    private @Parameter(defaultPrefix = BindingConstants.LITERAL, value = "end") @NotNull String endedHandler;
    private @Parameter("false") boolean keepAlive;
    
    private @Inject ComponentResources componentResources;
    private @Environmental JavaScriptSupport jsSupport;    
    private @Inject Request request;
    private @Inject @Symbol(SymbolConstants.CONTEXT_PATH) String contextPath;
    
    private String _endedHandler;

    private static final String eventName = "checkidle";
    private static final String KEEPALIVE_NAME = "keepAlive";
}
