package com.flowlogix.web.mixins;

import com.flowlogix.web.services.internal.MixinAdderWorker;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.corelib.components.Submit;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Environment;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLDisableAfterSubmit" 
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
public class DisableAfterSubmit
{
    private void setupRender()
    {
        if(!MixinAdderWorker.isCorrectType(Submit.class, cr))
        {
            environment.push(DisableAfterSubmit.class, this);
        }
        js.require("flowlogix/DisableAfterSubmit");
    }
    
    
    private void cleanupRender()
    {
        if(!MixinAdderWorker.isCorrectType(Submit.class, cr))
        {        
            environment.pop(DisableAfterSubmit.class);
        }
    }

    
    private @Environmental JavaScriptSupport js;
    private @Inject ComponentResources cr;
    private @Inject Environment environment;
}
