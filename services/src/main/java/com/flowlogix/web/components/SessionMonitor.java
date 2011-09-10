/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.components;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Request;
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
    @Environmental private JavaScriptSupport renderSupport;
    
    @Inject private Request request;

    @Parameter("15") private int idleCheck;
    @Parameter("30") private int warnBefore;
    @Parameter(defaultPrefix = BindingConstants.LITERAL) private String warnBeforeHandler;
    @Parameter(defaultPrefix = BindingConstants.LITERAL) private String endedHandler;
    @Parameter("false") private boolean keepAlive;

    JSONObject onCheckidle() 
    {
        Session session = request.getSession(false);
        // FIXME check if keepalive is set
        JSONObject object = new JSONObject();
        object.put("nextCheck", 15);
        return object;
    }

    JSONObject onRefresh() 
    {
        return null;
    }

    JSONObject onEnd() 
    {
        return new JSONObject();
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

        // System.out.println("Active conversation is " + conversationManager.getActiveConversation());
        renderSupport.addScript(String.format("%s = new SessionMonitor('%s', '%s', %s, true, %s, %s, '%s', '%s');", componentResources.getId(), baseURI,
                defaultURIparameters, keepAlive, idleCheck, warnBefore, warnBeforeHandler, endedHandler));
    }
}
