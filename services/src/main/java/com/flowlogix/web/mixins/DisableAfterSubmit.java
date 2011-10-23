package com.flowlogix.web.mixins;

import org.apache.tapestry5.ClientElement;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectContainer;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.FormSupport;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

@Import(library="DisableAfterSubmit.js")
public class DisableAfterSubmit
{
    @AfterRender
    void addDisabler()
    {
        JSONObject spec = new JSONObject();
        spec.put("elementId", submitButton.getClientId());
        spec.put("formId", fs.getClientId());
        js.addInitializerCall("disableAfterSubmit", spec);
    }
    
    
    @Environmental private JavaScriptSupport js;
    @InjectContainer private ClientElement submitButton;
    @Environmental private FormSupport fs;
}
