package com.flowlogix.web.mixins;

import org.apache.tapestry5.ClientElement;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectContainer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * Provides Timed Zone Updates
 * 
 * @author lprimak
 */
@Import(library="DeferUpdate.js")
public class DeferZoneUpdate
{
    @AfterRender
    void addStatusReset()
    {
        js.addScript("new DeferUpdate('%s');", container.getClientId());
    }
    
    
    @InjectContainer private ClientElement container;
    private @Environmental JavaScriptSupport js;
}
