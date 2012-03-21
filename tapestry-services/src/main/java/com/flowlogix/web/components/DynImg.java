/*
 * Copyright 2011 lprimak.
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
package com.flowlogix.web.components;

import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.SupportsInformalParameters;
import org.apache.tapestry5.ioc.annotations.Inject;

/**
 * Dynamic image, that is compatible and viewable for Web designers
 * with tools such as DreamWeaver,
 * Behaves just like an ordinary img tag<br>
 * 
 * <a href="http://code.google.com/p/flowlogix/wiki/TLDynImg"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@SupportsInformalParameters
public class DynImg
{
    boolean beginRender(MarkupWriter writer)
    {
        if (src != null)
        {
            writer.element("img", "src", src);
            if(renderInformals)
            {
                resources.renderInformalParameters(writer);
            }
            writer.end();
        }
        return false;
    }

    
    private @Parameter(required = true, allowNull = true) String src;
    private @Parameter(required = false, value = "true") boolean renderInformals;
    private @Inject ComponentResources resources;
}
