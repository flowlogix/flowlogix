package com.flowlogix.web.base;

import com.flowlogix.util.GwtSupportLoaded;
import com.flowlogix.web.mixins.GwtSupportMixin;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Mixin;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.ThreadLocale;
import org.apache.tapestry5.services.AssetSource;
import org.apache.tapestry5.services.Environment;

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
    
    
    protected List<String> getPostInitScripts()
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
        return new File(getGwtModuleAsset().toClientURL()).getParent();
    }
    
    
    protected Asset getGwtModuleAsset()
    {
        final String gwtModule = getModuleName();
        final String gwtModuleJSPath = String.format("context:%s/%s.nocache.js", gwtModule, gwtModule);
        return assetSource.getContextAsset(gwtModuleJSPath, threadLocale.getLocale());        
    }
    
    
    @SetupRender
    public void init()
    {
        // add to redirect list so relative asset paths work
        GwtSupportLoaded.getModuleNames().add(getModuleName());
    }
    
    
    @AfterRender
    public void addScript(MarkupWriter writer)
    {        
        Element head = writer.getDocument().find("html/head");
        
        GwtSupportLoaded supportScriptLoaded = environment.peek(GwtSupportLoaded.class);
        if(supportScriptLoaded == null)
        {
            // only one copy of module initializations per page
            supportScriptLoaded = new GwtSupportLoaded();
            environment.push(GwtSupportLoaded.class, supportScriptLoaded);
            List<String> initList = getJavaScriptInitialization();
            Element scriptElement = initList.isEmpty() ? null : head.element("script");
            for (String var : getJavaScriptInitialization())
            {
                scriptElement.raw(var);
            }
        
            head.element("script", "src", mixin.getGwtSupportAsset().toClientURL());
        }
        head.element("script").raw(String.format("GWTComponentController.add('%s', '%s');", 
                getEntryPoint().getName(), addParameters(resources.getCompleteId(), getGWTParameters())));

        if(supportScriptLoaded.getModulesLoaded().contains(getModuleName()) == false)
        {
            // only one copy of module script per page
            supportScriptLoaded.getModulesLoaded().add(getModuleName());
            head.element("script", "src", getGwtModuleAsset().toClientURL());
        }
        
        for(String var : getPostInitScripts())
        {
            head.element("script", "src", var);
        }
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
    
    
    private @Inject AssetSource assetSource;
    private @Inject ThreadLocale threadLocale;
    private @Getter @Inject ComponentResources resources;
    private @Mixin GwtSupportMixin mixin;
    private @Inject Environment environment;
}
