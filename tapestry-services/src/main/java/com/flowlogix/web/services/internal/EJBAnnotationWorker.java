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
package com.flowlogix.web.services.internal;

import com.flowlogix.ejb.JNDIObjectLocator;
import com.flowlogix.web.services.annotations.Stateful;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.naming.NamingException;
import lombok.SneakyThrows;
import org.apache.tapestry5.internal.services.ComponentClassCache;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.FieldConduit;
import org.apache.tapestry5.plastic.InstanceContext;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticField;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Session;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

/**
 * Inject an EJB into tapestry sources
 * 
 * @author Magnus
 * Enhancements by Lenny Primak
 */
public class EJBAnnotationWorker implements ComponentClassTransformWorker2
{
    @Override
    @SneakyThrows({NamingException.class})
    public void transform(PlasticClass plasticClass,
            TransformationSupport support, MutableComponentModel model)
    {
        for (PlasticField field : plasticClass.getFieldsWithAnnotation(EJB.class))
        {
            final EJB annotation = field.getAnnotation(EJB.class);
            final Stateful stateful = field.getAnnotation(Stateful.class);
            final String fieldType = field.getTypeName();
            final String fieldName = field.getName();
            final String lookupname = getLookupName(annotation, fieldType);

            Object injectionValue = lookupBean(field, fieldType, fieldName, lookupname, stateful);

            if (injectionValue != null)
            {
                field.claim(annotation);
            }
        }
    }

    
    private String getLookupName(EJB annotation, String fieldType)
    {
        String lookupname = null;
        //try lookup
        if (!isBlankOrNull(annotation.lookup()))
        {
            lookupname = annotation.lookup();
        } //try name
        else
        {
            if (!isBlankOrNull(annotation.name()))
            {
                lookupname = annotation.name();
            } else
            {
                if (!isBlankOrNull(annotation.beanName()))
                {
                    lookupname = annotation.beanName();
                }
            }
        }

        //use type
        if (lookupname == null)
        {
            lookupname = JNDIObjectLocator.guessByType(fieldType);
        }

        lookupname = JNDIObjectLocator.prependPortableName(lookupname);
        return lookupname;
    }
    
    
    private Object lookupBean(final PlasticField field, final String typeName, final String fieldName,
            final String lookupname, final Stateful stateful) throws NamingException
    {
        if(stateful != null)
        {
            field.setConduit(new EJBFieldConduit(lookupname, stateful, stateful.isSessionAttribute()? fieldName : typeName, fieldName));              
            return true;
        }
        else
        {
            Object rv = locator.getJNDIObject(lookupname, false);
            if(rv != null)
            {
                field.inject(rv);
            }
            return rv;
        }
    }
    

    private boolean isBlankOrNull(String s)
    {
        return s == null || s.trim().equals("");
    }
    
    
    private class EJBFieldConduit implements FieldConduit<Object>
    {
        public EJBFieldConduit(String lookupname, Stateful stateful, String attributeName, String fieldName)
        {
            this.lookupname = lookupname;
            this.stateful = stateful;
            this.attributeName = "ejb:" + attributeName;
            this.fieldName = fieldName;
        }

        
        @Override
        public Object get(Object instance, InstanceContext context)
        {
            final Session session = rg.getRequest().getSession(true);
        
            Wrapper rv = (Wrapper)session.getAttribute(attributeName);
            if(rv == null)
            {
                rv = new Wrapper().lookupBean();
                session.setAttribute(attributeName, rv);
            }
            else if(rv.value == null)
            {
                rv.lookupBean();
            }
        
            return rv.value;
        }

        
        @Override
        @SneakyThrows(IllegalAccessException.class)
        public void set(Object instance, InstanceContext context, Object newValue)
        {
            throw new IllegalAccessException(String.format("Field %s is Read Only", fieldName));
        }
        
        
        protected final String lookupname;
        protected final Stateful stateful;
        protected final String attributeName;
        protected final String fieldName;
        
        
        private class Wrapper implements Serializable
        {
            public Wrapper() { }
            
            
            @SneakyThrows(NamingException.class)
            public Wrapper lookupBean()
            {
                value = locator.getJNDIObject(lookupname, stateful != null);
                return this;
            }
            public transient Object value;
        }
    }
    
    
    private final JNDIObjectLocator locator = new JNDIObjectLocator();
    private @Inject ApplicationStateManager asm;
    private @Inject ComponentClassCache classCache;
    private @Inject RequestGlobals rg;
}
