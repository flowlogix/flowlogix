package com.flowlogix.web.mixins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.validation.constraints.NotNull;

import lombok.Cleanup;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

public class ColorHighlight
{
    public ColorHighlight() throws IOException
    {
        @Cleanup InputStream strm = scriptAsset.getResource().openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(strm));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }

        script = sb.toString();
    }
    
    
    @SetupRender
    void init() throws IOException
    {
        js.addScript(script, color);
    }
    
    
    @Environmental private JavaScriptSupport js;
    @Parameter(required=true, defaultPrefix=BindingConstants.LITERAL) @NotNull 
    private String color;
    @Inject @Path("ColorHighlight.js")
    private Asset scriptAsset;
    private final String script;
}
