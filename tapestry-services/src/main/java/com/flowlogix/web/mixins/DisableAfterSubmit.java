package com.flowlogix.web.mixins;

import com.flowlogix.web.services.internal.DisableAfterSubmitWorker;
import org.apache.tapestry5.ClientElement;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectContainer;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Environment;
import org.apache.tapestry5.services.FormSupport;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLDisableAfterSubmit" 
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Import(library="DisableAfterSubmit.js")
public class DisableAfterSubmit
{
    @AfterRender
    private void addDisabler()
    {
        if(DisableAfterSubmitWorker.isSubmitButton(cr))
        {
            enableSubmitProcessing(clientElement, fs, js);
        }
    }
    
    
    private void setupRender()
    {
        if(!DisableAfterSubmitWorker.isSubmitButton(cr))
        {
            environment.push(DisableAfterSubmit.class, this);
        }
    }
    
    
    private void cleanupRender()
    {
        if(!DisableAfterSubmitWorker.isSubmitButton(cr))
        {        
            environment.pop(DisableAfterSubmit.class);
        }
    }
    
    
    static public void enableSubmitProcessing(ClientElement clientElement, FormSupport fs, JavaScriptSupport js)
    {
        JSONObject spec = new JSONObject();
        spec.put("elementId", clientElement.getClientId());
        spec.put("formId", fs.getClientId());
        js.addInitializerCall("disableAfterSubmit", spec);
    }
    
    
    private @Inject ComponentResources cr;
    private @Environmental JavaScriptSupport js;
    private @InjectContainer ClientElement clientElement;
    private @Environmental(false) FormSupport fs;
    private @Inject Environment environment;
}
