/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.webgwt.base;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.internal.services.RequestConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.AssetSource;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.URLEncoder;
import org.apache.tapestry5.services.assets.AssetPathConstructor;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLGwtSupport"
 *    target="_blank">See Documentation</a>
 *
 * @author lprimak
 */
@Import(library="gwtSupport.js")
public abstract class GwtSupport
{
    protected abstract Class<?> getEntryPoint();
    protected abstract String getModuleName();


    /**
     * Override to add JavaScript initialization code that module depends on
     * @return
     */
    protected List<String> getJavaScriptInitialization()
    {
        return Lists.newLinkedList();
    }


    /**
     * Override to add parameters to the GWT component
     * @return
     */
    protected List<String> getGWTParameters()
    {
        return Lists.newLinkedList();
    }


    @SneakyThrows(IOException.class)
    protected String getGwtModulePath()
    {
        // +++ TODO not sure how to set the last argument since incompatible change
        return contextRoot.constructAssetPath(RequestConstants.CONTEXT_FOLDER, getModuleName(), null);
    }


    @SetupRender
    public void addScript()
    {
        jsSupport.addScript("GWTComponentController.add('%s','%s')", getEntryPoint().getName(),
                addParameters(resources.getCompleteId(), getGWTParameters()));

        final String gwtModule = getModuleName();
        final String supportVariablePath = "flowlogixgwt/js/GwtSupportVariable";
        for (String var : getJavaScriptInitialization())
        {
            jsSupport.importJavaScriptLibrary(String.format("%s/%s:action?value=%s",
                    requestGlobals.getRequest().getContextPath(), supportVariablePath,
                    urlEncoder.encode(var)));
        }
        final String gwtModuleJSPath = String.format("context:%s/%s.nocache.js", gwtModule, gwtModule);
        jsSupport.importJavaScriptLibrary(assetSource.getExpandedAsset(gwtModuleJSPath));
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
    private @Inject AssetSource assetSource;
    private @Getter @Inject ComponentResources resources;
    private @Inject RequestGlobals requestGlobals;
    private @Inject AssetPathConstructor contextRoot;
    private @Inject URLEncoder urlEncoder;
    private static final Logger log = Logger.getLogger(GwtSupport.class.getName());
}
