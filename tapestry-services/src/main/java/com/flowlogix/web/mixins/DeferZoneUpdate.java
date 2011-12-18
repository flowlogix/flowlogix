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
 * <a href="http://code.google.com/p/flowlogix/wiki/TLPeriodicUpdater"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Import(library="DeferUpdate.js")
public class DeferZoneUpdate
{
    @AfterRender
    void addStatusReset()
    {
        js.addInitializerCall("deferUpdate", container.getClientId());
    }
    
    
    private @InjectContainer ClientElement container;
    private @Environmental JavaScriptSupport js;
}
