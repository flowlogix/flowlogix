package com.flowlogix.web.mixins;

import org.apache.tapestry5.Asset;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.apache.tapestry5.services.javascript.StylesheetLink;
import org.apache.tapestry5.services.javascript.StylesheetOptions;

/**
 * Puts a Loading Spinner on all Zone updates on the page
 * 
 * @author lprimak
 */
@Import(library = "spinner-zone-overlay.js", stylesheet = "spinner-zone-overlay.css")
public class ZoneLoadingSpinner
{
    public void afterRender()
    {
        jsSupport.importStylesheet(new StylesheetLink(ieCSS, new StylesheetOptions().withCondition("IE")));
    }
    
    
    private @Inject JavaScriptSupport jsSupport;
    private @Inject @Path("spinner-zone-overlay-ie.css") Asset ieCSS;
}
