/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import com.flowlogix.web.services.annotations.AJAX;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectContainer;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.runtime.Component;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * Trigger event after a Zone update
 * 
 * @author lprimak
 */
@Import(library = "UpdateEvent.js")
public class UpdateEvent extends SessionTracker
{
    @AfterRender
    private void addUpdater()
    {
        ComponentResources cr = zone.getComponentResources();
        if(updateEvent != null)
        {
            createEvent(cr, updateEvent);
        }
        else
        {
            createEvent(cr, CHECK_SESSION_EVENT);
        }
    }
    
    
    @OnEvent(value = CHECK_SESSION_EVENT)
    @AJAX
    private void checkSession()
    {
        // placeholder for auto-reload page
    }
    

    private void createEvent(ComponentResources cr, String event)
    {
        Link link = cr.createEventLink(event);
        String uri = link.toAbsoluteURI(isSecure);

        js.addScript("new UpdateEvent('%s', '%s');",
                cr.getId(), uri);
    }

    
    private @Parameter(allowNull = false, defaultPrefix = BindingConstants.LITERAL) String updateEvent;
    private @InjectContainer Component zone;
    private @Environmental JavaScriptSupport js;
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;
    public static final String CHECK_SESSION_EVENT = "checkSessionEvent";
}
