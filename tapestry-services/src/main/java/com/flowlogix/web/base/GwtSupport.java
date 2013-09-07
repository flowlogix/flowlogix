package com.flowlogix.web.base;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.ThreadLocale;
import org.apache.tapestry5.services.AssetSource;
import org.apache.tapestry5.services.URLEncoder;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLGwtSupport"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Slf4j
public abstract class GwtSupport
{    
    protected abstract Class<?> getEntryPoint();
    protected abstract String getModuleName();
    
    
    /**
     * Override to add JavaScript initialization code that module depends on
     */
    protected List<String> getJavaScriptInitialization()
    {
        return Lists.newLinkedList();
    }
    
    
    /**
     * Override to add parameters to the GWT component
     */
    protected List<String> getGWTParameters()
    {
        return Lists.newLinkedList();
    }
    
    
    protected String getGwtModulePath()
    {
        final String gwtModule = getModuleName();
        final String gwtModuleJSPath = String.format("context:%s/%s.nocache.js", gwtModule, gwtModule);
        final Asset asset = assetSource.getContextAsset(gwtModuleJSPath, threadLocale.getLocale());
        return new File(asset.toClientURL()).getParent();
    }
    
    
    @AfterRender
    public void addScript(MarkupWriter writer)
    {        
        Element head = writer.getDocument().find("html/head");
        
        for (String var : getJavaScriptInitialization())
        {
            head.element("script").raw(var);
        }

        final String gwtModule = getModuleName();
        jsSupport.require("flowlogix/GwtModuleInit").with(getEntryPoint().getName(), 
                addParameters(resources.getCompleteId(), getGWTParameters()));
        
        final String gwtModuleJSPath = String.format("context:%s/%s.nocache.js", gwtModule, gwtModule);
        head.element("script", "src", assetSource.getExpandedAsset(gwtModuleJSPath).toClientURL());
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
    private @Inject ThreadLocale threadLocale;
    private @Getter @Inject ComponentResources resources;
    private @Inject @Symbol(SymbolConstants.CONTEXT_PATH) String contextPath;
    private @Inject URLEncoder urlEncoder;
}
