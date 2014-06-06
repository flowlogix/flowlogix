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

import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.MixinAfter;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.dom.Visitor;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;

/**
 * Works around this issue: https://issues.apache.org/jira/browse/TAP5-2197
 *
 * @author lprimak
 */
@MixinAfter
public class OverrideCSS
{
    public void afterRender(MarkupWriter mw)
    {
        final Element head = mw.getDocument().find("html/head");
        head.visit(new Visitor()
        {
            @Override
            public void visit(Element element)
            {
                if (element.getName().equals("link"))
                {
                    if (element.getAttribute("href").contains(contextPath))
                    {
                        element.moveToBottom(head);
                    }
                }
            }
        });
    }
    
    
    private @Inject @Symbol(SymbolConstants.CONTEXT_PATH) String contextPath;
}
