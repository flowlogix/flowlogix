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

import com.flowlogix.ejb.JNDIConfigurer;
import com.flowlogix.ejb.JNDIObjectLocator;
import com.flowlogix.web.services.annotations.Stateful;
import java.util.Hashtable;
import java.util.Map;
import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.FieldConduit;
import org.apache.tapestry5.plastic.InstanceContext;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticField;
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
@Slf4j
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
            final String mappedName = annotation.mappedName();
            
            final JNDIObjectLocator locator = isBlankOrNull(mappedName)? new JNDIObjectLocator() : getConfiguredLocator(mappedName);
            final String lookupname = getLookupName(annotation, fieldType, locator);
            
            Object injectionValue = lookupBean(field, fieldType, fieldName, lookupname, mappedName, stateful, locator);
            if (injectionValue != null)
            {
                field.claim(annotation);
            }
        }
    }

    
    private String getLookupName(EJB annotation, String fieldType, final JNDIObjectLocator locator)
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

        lookupname = locator.prependPortableName(lookupname);
        return lookupname;
    }
    
    
    private Object lookupBean(final PlasticField field, final String typeName, final String fieldName,
            final String lookupname, final String mappedName, final Stateful stateful,
            final JNDIObjectLocator locator) throws NamingException
    {
        if(stateful != null)
        {
            field.setConduit(new EJBFieldConduit(locator, lookupname, 
                    stateful, stateful.isSessionAttribute()? fieldName : typeName, fieldName));              
            return true;
        }
        else if(typeName.toUpperCase().endsWith("REMOTE"))
        {
            field.setConduit(new EJBFieldConduit(locator, lookupname, 
                    null, "", fieldName));              
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

    
    @SneakyThrows(NamingException.class)
    private JNDIObjectLocator getConfiguredLocator(String mappedName)
    {
        JNDIConfigurer configBean = JNDIConfigurer.getInstance();
        JNDIConfigurer.Config config = configBean.getConfiguration().get(mappedName);
        
        if(config == null)
        {
            log.error("Tapestry @EJB Location Failed: mappedName %s is not configured", mappedName);
            return new JNDIObjectLocator();
        }
        
        Hashtable<String, String> env = new Hashtable<>();
        if(!isBlankOrNull(config.getHostname()))
        {
            env.put("org.omg.CORBA.ORBInitialHost", config.getHostname());           
        }
        if(config.getPort() != null)
        {
            env.put("org.omg.CORBA.ORBInitialPort", config.getPort().toString());           
        }
        for(Map.Entry<String, String> entry : config.getAdditionalProperties().entrySet())
        {
            env.put(entry.getKey(), entry.getValue());
        }
        JNDIObjectLocator locator;
        if(env.isEmpty())
        {
            locator = new JNDIObjectLocator();
        }
        else
        {
            locator = new JNDIObjectLocator(new InitialContext(env));
        }
        if(!isBlankOrNull(config.getPrefix()))
        {
            locator.setPortableNamePrefix(config.getPrefix());
        }
        return locator;
    }
    
    
    private class EJBFieldConduit implements FieldConduit<Object>
    {
        public EJBFieldConduit(final JNDIObjectLocator locator, String lookupname, Stateful stateful,
                String attributeName, String fieldName)
        {
            this.lookupname = lookupname;
            this.stateful = stateful;
            this.attributeName = "ejb:" + attributeName;
            this.fieldName = fieldName;
            this.locator = locator;
        }

        
        @Override
        @SneakyThrows({NamingException.class})
        public Object get(Object instance, InstanceContext context)
        {
            if(stateful == null)
            {
                return locator.getJNDIObject(lookupname, true);
            }
            
            final Session session = rg.getRequest().getSession(true);
        
            Object rv = session.getAttribute(attributeName);
            if(rv == null)
            {
                rv = locator.getJNDIObject(lookupname, stateful != null); 
                session.setAttribute(attributeName, rv);
            }
            return rv;
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
        protected final JNDIObjectLocator locator;
    }
    
    
    private @Inject RequestGlobals rg;
}
