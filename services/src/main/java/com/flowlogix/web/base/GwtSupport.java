/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.base;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import lombok.Getter;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.AssetSource;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 *
 * @author lprimak
 */
@Import(library="gwtSupport.js")
public abstract class GwtSupport
{    
    /**
     * Override this method to add parameters to the GWT component
     */
    protected List<String> getGWTParameters()
    {
        ArrayList<String> params = new ArrayList<String>();
        return params;
    }
    
    
    protected abstract Class<?> getEntryPoint();
    protected abstract String getModuleName();
    
    
    @SetupRender
    public void addScript()
    {
        gwtModule = getModuleName();
        gwtModulePath = String.format("context:%s/%s.nocache.js", gwtModule, gwtModule);

        jsSupport.addScript("GWTComponentController.add('%s','%s')", getEntryPoint().getName(), 
                addParameters(resources.getCompleteId(), getGWTParameters()));
        jsSupport.importJavaScriptLibrary("flowlogix/js/GwtSupportVariable:action?value=" + gwtModule + "/sc/");
        jsSupport.importJavaScriptLibrary(assetSource.getExpandedAsset(gwtModulePath));
    }    

    
    private String addParameters(String id, List<String> additinalParameters)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        for(String parm : additinalParameters)
        {
            sb.append('|').append(parm);
        }
        return sb.toString();
    }
    
    
    private @Environmental JavaScriptSupport jsSupport;
    private String gwtModule;
    private String gwtModulePath;
    private @Inject AssetSource assetSource;
    private @Getter @Inject ComponentResources resources;
    private static final Logger log = Logger.getLogger(GwtSupport.class.getName());
}
