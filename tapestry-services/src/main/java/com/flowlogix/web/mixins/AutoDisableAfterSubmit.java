/*
 * Copyright 2013 lprimak.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flowlogix.web.mixins;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.dom.Node;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Environment;

/**
 * Internal mixin for DisableAfterSubmit
 * @author lprimak
 */
public class AutoDisableAfterSubmit
{
    private @Parameter(name = "disableAfterSubmitEnabled", defaultPrefix = BindingConstants.LITERAL) Boolean enabled;

    
    @AfterRender
    private void addDisabler(MarkupWriter writer)
    {
        if (environment.peek(DisableAfterSubmit.class) == null)
        {
            if(enabled == null)
            {
                return;
            }
        }
        else if(enabled == null)
        {
            enabled = true;
        }
        
        if (enabled == false)
        {
            for (Node child : writer.getElement().getChildren())
            {
                if (child instanceof Element)
                {
                    Element elt = (Element)child;
                    elt.attributes("data-disable-after-submit-excluded", "true");
                }
            }
        }
    }
    
    
    private @Inject Environment environment;
}
