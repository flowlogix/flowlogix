/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * Trigger event after a Zone update
 * <a href="http://code.google.com/p/flowlogix/wiki/TLUpdateEvent"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Import(library = "UpdateEvent.js")
public class UpdateEvent extends SessionTracker
{
    @AfterRender
    private void addUpdater()
    {
        if(updateEvent != null)
        {
            createEvent(updateEvent);
        }
        createEvent(CHECK_SESSION_EVENT);
    }
    
    
    @OnEvent(value = CHECK_SESSION_EVENT)
    private JSONObject checkSession()
    {
        JSONObject rv = new JSONObject();
        if(isValidSession() == false)
        {
            rv.put("reloadPage", true);
            showSessionExpiredMessage = true;
        }
        return rv;
    }
    

    private void createEvent(String event)
    {
        Link link = null;
        if(context == null)
        {
            link = cr.createEventLink(event);
        }
        else
        {
            link = cr.createEventLink(event, context);
        }
        String uri = link.toAbsoluteURI(request.isSecure());

        JSONObject spec = new JSONObject();
        spec.put("elementId", zone.getClientId());
        spec.put("uri", uri);
        js.addInitializerCall("updateEvent", spec);
    }

    
    private @Parameter(allowNull = false, defaultPrefix = BindingConstants.LITERAL) String updateEvent;
    /**
     * The context for the triggered event.
     */
    private @Parameter String context;

    private @InjectContainer Zone zone;
    private @Environmental JavaScriptSupport js;
    private @Inject ComponentResources cr;
    private @SessionAttribute Boolean showSessionExpiredMessage;
    private @Inject Request request;
    public static final String CHECK_SESSION_EVENT = "checkSessionEvent";
}
