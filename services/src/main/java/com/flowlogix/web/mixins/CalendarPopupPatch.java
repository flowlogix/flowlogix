/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import com.flowlogix.web.services.AssetMinimizer;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * Overrides Calendar to have "October 2011" and such on the popup label
 * 
 * @author lprimak
 */
public class CalendarPopupPatch
{
    @SetupRender
    void init()
    {
        js.addScript(script);
    }
    
    
    private @Environmental JavaScriptSupport js;
    private @Inject @Path("CalendarPopupPatch.js") Asset scriptAsset;
    private @Inject AssetMinimizer minimizer;
    private final String script = minimizer.minimize(scriptAsset);
}
