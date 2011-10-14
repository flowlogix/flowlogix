/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import com.flowlogix.util.Streams;
import java.io.IOException;
import java.io.InputStream;
import lombok.Cleanup;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.annotations.BindParameter;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 *
 * @author lprimak
 */
public class ColorHighlightOverride
{
    public ColorHighlightOverride() throws IOException
    {
        @Cleanup InputStream strm = scriptAsset.getResource().openStream();
        script = Streams.readString(strm);
    }
    
    
    @SetupRender
    void init() throws IOException
    {
        js.addScript(script, highlightColor);
    }
    
    
    @Environmental private JavaScriptSupport js;
    private @BindParameter String highlightColor;
    @Inject @Path("EffectOverrides.js")
    private Asset scriptAsset;
    private final String script;
}
