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
package com.flowlogix.web.services.internal;

import com.flowlogix.web.mixins.AutoDisableAfterSubmit;
import org.apache.shiro.util.ClassUtils;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.corelib.components.Submit;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

/**
 * Adds DisableAfterSubmit to every submit element
 * @author lprimak
 */
public class DisableAfterSubmitWorker implements ComponentClassTransformWorker2
{
    @Override
    public void transform(PlasticClass plasticClass, TransformationSupport support, MutableComponentModel model)
    {
        boolean hasMixin = model.getMixinClassNames().contains(AutoDisableAfterSubmit.class.getName());
        if(hasMixin == false && isSubmitButton(plasticClass.getClassName()))
        {
            model.addMixinClassName(AutoDisableAfterSubmit.class.getName());
        }
    }
        
            
    /**
     * Determines if the mixin's container is a submit element
     */

    public static boolean isSubmitButton(ComponentResources cr)
    {
        return isSubmitButton(cr.getContainerResources().getComponentModel().
                getComponentClassName());
    }
                
    /**
     * Determines if the mixin's container is a submit element
     */
    public static boolean isSubmitButton(String className)
    {
        Class<?> myClass = ClassUtils.forName(className);
        Class<?> submitClass = ClassUtils.forName(Submit.class.getName());
        return submitClass.isAssignableFrom(myClass);
    }
}
