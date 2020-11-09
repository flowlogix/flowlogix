/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.mixins;

import com.flowlogix.web.base.UserEnvironment;
import com.flowlogix.web.services.AssetMinimizer;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Path;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * Overrides Calendar to have "October 2011" and such on the popup label,<br>
 * Fixes the JIRA issue <a href="https://issues.apache.org/jira/browse/TAP5-805">TAP5-805</a><br>
 * Fixes the JIRA issue <a href="https://issues.apache.org/jira/browse/TAP5-1697">TAP5-1697</a>
 *
 * <a href="http://code.google.com/p/flowlogix/wiki/TLCalendarPopupPatch"
 *    target="_blank">See Documentation</a>
 *
 * @author lprimak
 */
public class CalendarPopupPatch extends UserEnvironment
{
    @Override
    public void setupRender()
    {
        super.setupRender();

        js.addScript(calPopupScript);
        js.addScript(dateFieldParserScript);
    }


    public void afterRender()
    {
        if(isWindows())
        {
            js.importStylesheet(datefieldWinPatch);
        }
    }


    private @Environmental JavaScriptSupport js;
    private @Inject @Path("CalendarPopupPatch.js") Asset calPopupScriptAsset;
    private @Inject @Path("DateFieldParserPatch.js") Asset dateFieldParserscriptAsset;
    private @Inject @Path("DateFieldWindowsPatch.css") Asset datefieldWinPatch;
    private @Inject AssetMinimizer minimizer;
    private final String calPopupScript = minimizer.minimize(calPopupScriptAsset);
    private final String dateFieldParserScript = minimizer.minimize(dateFieldParserscriptAsset);
}
