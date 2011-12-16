/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import com.flowlogix.web.services.AssetMinimizer;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.annotations.BindParameter;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLColorHighlight"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
public class ColorHighlightOverride
{
    @SetupRender
    void init()
    {
        js.addScript(script, highlightColor);
    }
    
    
    private @Environmental JavaScriptSupport js;
    private @BindParameter String highlightColor;
    private @Inject @Path("EffectOverrides.js") Asset scriptAsset;
    private @Inject AssetMinimizer minimizer;
    private final String script = minimizer.minimize(scriptAsset);
}
