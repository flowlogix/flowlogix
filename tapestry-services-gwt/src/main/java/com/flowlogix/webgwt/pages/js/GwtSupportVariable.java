/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.webgwt.pages.js;

import lombok.Getter;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.ActivationRequestParameter;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * Support page to instantiate JavaScript variables needed for SmartGWT before other JS inclusions<br>
 * See {@link com.flowlogix.web.base.GwtSupport}
 * 
 * @author lprimak
 */
public class GwtSupportVariable
{
    public @OnEvent StreamResponse getVariable()
    {
        return new TextStreamResponse("text/javascript", value);
    }
            
    
    private @Getter @ActivationRequestParameter String value;
}
