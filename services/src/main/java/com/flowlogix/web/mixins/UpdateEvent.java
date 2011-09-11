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
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.SymbolSource;
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
    @SetupRender
    public void init()
    {
        isSecure = Boolean.valueOf(symbolProvider.valueForSymbol(SymbolConstants.SECURE_ENABLED));
        if(isInitialized == null)
        {
            isInitialized = false;
        }
    }

    @AfterRender
    void addUpdater()
    {
        if(isInitialized == false)
        {
            ComponentResources cr = zone.getComponentResources();
            Link link = cr.createEventLink(updateEvent);
            String uri = link.toAbsoluteURI(isSecure);

            js.addScript("new UpdateEvent('%s', '%s', '%s');",
                    cr.getId(), updateEvent, uri);
            isInitialized = true;
        }
    }

    
    private @Parameter(required = true, allowNull = false,
            defaultPrefix = BindingConstants.LITERAL) String updateEvent;
    private @InjectContainer Component zone;
    private @Environmental JavaScriptSupport js;
    private @Inject SymbolSource symbolProvider;    
    private @Persist Boolean isSecure;
    private @Persist Boolean isInitialized;
}
