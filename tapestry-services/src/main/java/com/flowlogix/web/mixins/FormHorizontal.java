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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.annotations.MixinAfter;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.dom.Attribute;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.dom.Visitor;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;

/**
 * Workaround for This Issue:
 * https://issues.apache.org/jira/browse/TAP5-2182
 * @author lprimak
 */
@MixinAfter
@Slf4j
public class FormHorizontal
{
        private @Parameter(defaultPrefix = BindingConstants.LITERAL, allowNull = false, 
            value = "false", name = "form-horizontal-disable") Boolean disabled;
    private @Parameter(defaultPrefix = BindingConstants.LITERAL, allowNull = false) String labelClass;
    private @Parameter(defaultPrefix = BindingConstants.LITERAL, allowNull = false) String inputClass;
    

    public void setupRender()
    {
        if(labelClass == null)
        {
            labelClass = labelClassDefault;
        }
        if(inputClass == null)
        {
            inputClass = inputClassDefault;
        }
        
        Matcher matcher = offsetPattern.matcher(labelClass);
        if(matcher.find())
        {
            computedOffset = Integer.valueOf(matcher.group(1));
        }
    }
    
    
    public void afterRender(MarkupWriter mw)
    {
        if(globalDisabled == false && disabled == false)
        {
            mw.getElement().visit(new HV());
        }
    }

    
    private class HV implements Visitor
    {
        @Override
        public void visit(Element element)
        {
            log.debug("EltName: " + element.getName()
                    + ", attr: " + Collections2.transform(element.getAttributes(), new Function<Attribute, String>()
            {
                @Override
                public String apply(Attribute input)
                {
                    return input.getName() + "=" + input.getValue();
                }
            }));
            
            if("true".equals(element.getAttribute(FORM_HORIZONTAL_PROCESSED)))
            {
                return;
            }
            
            if(element.getName().equals("label"))
            {
                String classAttr = element.getAttribute("class");
                if(classAttr != null && classAttr.contains("control-label"))
                {
                    element.attribute("class", labelClass);
                    labelElement = element;
                    labelProcessed = true;
                }
            }            
            else if(labelProcessed && !"hidden".equals(element.getAttribute("type")))
            {
                labelProcessed = false;
                if(element.getContainer().getName().equals("form"))
                {
                    Element formGroup = element.wrap("div", "class", "form-group", FORM_HORIZONTAL_PROCESSED, "true");
                    labelElement.moveToTop(formGroup);
                }
                element.wrap("div", "class", inputClass, FORM_HORIZONTAL_PROCESSED, "true");
            }
            else if(element.getName().equals("form"))
            {
                element.attributes("class", "form-horizontal t-beaneditor", FORM_HORIZONTAL_PROCESSED, "true");
            }
            else if(element.getName().equals("div"))
            {
                String classAttr = element.getAttribute("class");
                if("checkbox".equals(classAttr) || "btn-toolbar".equals(classAttr))
                {
                    element.attributes("class", inputClass + " col-md-offset-" + computedOffset, FORM_HORIZONTAL_PROCESSED, "true");
                    element.wrap("div", "class", "form-group", FORM_HORIZONTAL_PROCESSED, "true");
                    if("checkbox".equals(classAttr))
                    {
                        element.wrap("div", "class", "checkbox", FORM_HORIZONTAL_PROCESSED, "true");
                    }
                }
            }
        }
        
        private Element labelElement;
        private boolean labelProcessed = false;
    }
    
    
    private int computedOffset = 2;
    private static Pattern offsetPattern = Pattern.compile(".*-([0-9]*)");
    public static final String FORM_HORIZONTAL_PROCESSED="data-form-horizontal-processed";
    
    public static class Symbols
    {
        public static final String FORM_HORIZONTAL_DISABLED = "flowlogix.form-horizontal-disabled";
        public static final String LABEL_CLASS_DEFAULT = "flowlogix.label-class-default";
        public static final String INPUT_CLASS_DEFAULT = "flowlogix.input-class-default";
    }
    
    
    private @Inject @Symbol(Symbols.FORM_HORIZONTAL_DISABLED) boolean globalDisabled;
    private @Inject @Symbol(Symbols.LABEL_CLASS_DEFAULT) String labelClassDefault;
    private @Inject @Symbol(Symbols.INPUT_CLASS_DEFAULT) String inputClassDefault;
}
