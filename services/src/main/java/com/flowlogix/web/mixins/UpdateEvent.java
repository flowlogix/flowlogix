/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectContainer;
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
public class UpdateEvent 
{
    @AfterRender
    void addUpdater()
    {
        ComponentResources cr = zone.getComponentResources();
        Link link = cr.createEventLink(updateEvent);
        String uri = link.toAbsoluteURI(isSecure);

        js.addScript("new UpdateEvent('%s', '%s', '%s');",
                cr.getId(), updateEvent, uri);
    }

    
    private @Parameter(required = true, allowNull = false,
            defaultPrefix = BindingConstants.LITERAL) String updateEvent;
    private @InjectContainer Component zone;
    private @Environmental JavaScriptSupport js;
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;
}
