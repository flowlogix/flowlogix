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

import org.omnifaces.util.JNDIObjectLocator;
import com.flowlogix.web.services.annotations.Stateful;
import javax.ejb.EJB;
import javax.naming.NamingException;
import javax.servlet.http.HttpSession;
import lombok.SneakyThrows;
import org.apache.shiro.util.StringUtils;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.FieldConduit;
import org.apache.tapestry5.plastic.InstanceContext;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticField;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;
import org.omnifaces.util.JNDI;

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
            final String mappedName = annotation.mappedName();

            final JNDIObjectLocator locator = JNDIObjectLocator.builder().build();
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
        if (StringUtils.hasText(annotation.lookup()))
        {
            lookupname = annotation.lookup();
        } //try name
        else
        {
            if (StringUtils.hasText(annotation.name()))
            {
                lookupname = annotation.name();
            } else
            {
                if (StringUtils.hasText(annotation.beanName()))
                {
                    lookupname = annotation.beanName();
                }
            }
        }

        //use type
        if (lookupname == null)
        {
            lookupname = JNDI.guessJNDIName(fieldType);
        }

        lookupname = locator.prependNamespaceIfNecessary(lookupname);
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
            Object rv = locator.getObject(lookupname);
            if(rv != null)
            {
                field.inject(rv);
            }
            return rv;
        }
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
        public Object get(Object instance, InstanceContext context)
        {
            if(stateful == null)
            {
                return locator.getObjectNoCache(lookupname);
            }

            final HttpSession session = rg.getHTTPServletRequest().getSession(true);

            Object rv;
            synchronized(session.getId().intern())
            {
                rv = session.getAttribute(attributeName);
                if (rv == null)
                {
                    if (stateful != null) {
                        rv = locator.getObjectNoCache(lookupname);
                    } else {
                        rv = locator.getObject(lookupname);
                    }
                    session.setAttribute(attributeName, rv);
                }
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
