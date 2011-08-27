/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.pages.js;

import lombok.Getter;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.ActivationRequestParameter;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 *
 * @author lprimak
 */
public class GwtSupportVariable
{
    public @OnEvent StreamResponse getVariable()
    {
        return new TextStreamResponse("text/javascript", String.format("var isomorphicDir = \"%s\";", value));
    }
            
    
    @Getter @ActivationRequestParameter(value = "value") private String value;
}
