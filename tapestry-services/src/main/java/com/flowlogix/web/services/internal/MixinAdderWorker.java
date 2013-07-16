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

import lombok.RequiredArgsConstructor;
import org.apache.shiro.util.ClassUtils;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

/**
 * Adds specified mixin class to every component of type X
 * @author lprimak
 */
public @RequiredArgsConstructor class MixinAdderWorker implements ComponentClassTransformWorker2
{
    @Override
    public void transform(PlasticClass plasticClass, TransformationSupport support, MutableComponentModel model)
    {
        boolean hasMixin = model.getMixinClassNames().contains(mixinType.getName());
        if(hasMixin == false && isCorrectType(componentType, plasticClass.getClassName()))
        {
            model.addMixinClassName(mixinType.getName());
        }
    }
        
            
    /**
     * Determines if the mixin's container is a submit element
     */

    public static boolean isCorrectType(Class<?> componentType, ComponentResources cr)
    {
        return isCorrectType(componentType, cr.getContainerResources().getComponentModel().
                getComponentClassName());
    }
                
    /**
     * Determines if the mixin's container is a submit element
     */
    public static boolean isCorrectType(Class<?> componentType, String className)
    {
        Class<?> myClass = ClassUtils.forName(className);
        Class<?> componentClass = ClassUtils.forName(componentType.getName());
        return componentClass.isAssignableFrom(myClass);
    }
    
    
    private final Class<?> componentType;
    private final Class<?> mixinType;
}
