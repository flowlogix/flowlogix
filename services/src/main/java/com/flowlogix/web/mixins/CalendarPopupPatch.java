/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import com.flowlogix.util.Streams;
import com.flowlogix.web.services.AssetMinimizer;
import java.io.IOException;
import java.io.InputStream;
import lombok.Cleanup;
import lombok.SneakyThrows;
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
        js.addScript(calPopupScript);
        js.addScript(dateFieldParserScript);
    }

    
    @SneakyThrows(IOException.class)
    public CalendarPopupPatch()
    {
        @Cleanup InputStream strm = dateFieldParserscriptAsset.getResource().openStream();
        dateFieldParserScript = Streams.readString(strm);
    }
    
    
    private @Environmental JavaScriptSupport js;
    private @Inject @Path("CalendarPopupPatch.js") Asset calPopupScriptAsset;
    private @Inject @Path("DateFieldParserPatch.js") Asset dateFieldParserscriptAsset;
    private @Inject AssetMinimizer minimizer;
    private final String calPopupScript = minimizer.minimize(calPopupScriptAsset);
    // +++ private final String dateFieldParserScript = minimizer.minimize(dateFieldParserscriptAsset);
    private final String dateFieldParserScript;
}
