/**
 * Enables a zone to be periodically refreshed with the response from the given event.
 */
package com.flowlogix.web.mixins;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectContainer;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 *
 * @author lprimak
 */
@Import(library="PeriodicUpdater.js")
public class PeriodicUpdater
{
    /**
     * The name of the event to call to update the zone.
     */
    @Parameter(required = true, defaultPrefix = BindingConstants.LITERAL)
    private String event;
    
    /**
     * The context for the triggered event.
     */
    @Parameter
    private String context;
 
    /**
     * How long, in seconds, to wait between the end of one request and the beginning of the next.
     */
    @Parameter(defaultPrefix = BindingConstants.LITERAL, value="2")
    private int period;
 
    @InjectContainer
    private Zone zone;
 
    @Inject
    private ComponentResources resources;
 
    @Environmental
    private JavaScriptSupport jsSupport;
 

    @AfterRender
    void afterRender() {
 
        final String id = zone.getClientId();
 
        Link link;
        if(context == null)
        {
            link = resources.createEventLink(event);
        }
        else
        {
            link = resources.createEventLink(event, context);
        }
 
        final JSONObject spec = new JSONObject();
 
        spec.put("period", period);
        spec.put("elementId", id);
        spec.put("uri", link.toAbsoluteURI());

        jsSupport.addInitializerCall("periodicUpdater", spec);
    }
}
