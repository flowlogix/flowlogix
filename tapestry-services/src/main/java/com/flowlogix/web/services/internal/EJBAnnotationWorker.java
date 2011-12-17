/**
 * @(#)EJBAnnotationWorker.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 * Enhanced by Lenny Primak
 */
package com.flowlogix.web.services.internal;

import com.flowlogix.ejb.JNDIObjectLocator;
import com.flowlogix.web.services.annotations.Stateful;
import java.util.regex.Pattern;
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
            final String lookupname = getLookupName(annotation, fieldType);

            Object injectionValue = lookupBean(field, fieldType, lookupname, stateful);

            if (injectionValue != null)
            {
                field.claim(annotation);
            }
        }
    }
    
    
    public static String guessByType(String type) 
    {
        String lookupname = type.substring(type.lastIndexOf(".") + 1);
        // support naming convention that strips Local/Remote from the
        // end of an interface class to try to determine the actual bean name,
        // to avoid @EJB(beanName="myBeanName"), and just use plain old @EJB
        String uc = lookupname.toUpperCase();
        if (uc.endsWith(LOCAL) || uc.endsWith(REMOTE)) {
            lookupname = StripLocalPattern.matcher(lookupname).replaceFirst("");
        }
        return lookupname;
    }
    
    
    public static String prependPortableName(String lookupname)
    {
        //convert to jndi name
        if (!lookupname.startsWith("java:")) 
        {
            lookupname = "java:module/" + lookupname;
        }
        return lookupname;
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
            lookupname = guessByType(fieldType);
        }

        lookupname = prependPortableName(lookupname);
        return lookupname;
    }
    
    
    private Object lookupBean(final PlasticField field, final String typeName, 
            final String lookupname, final Stateful stateful) throws NamingException
    {
        if(stateful != null)
        {
            if(stateful.isSessionAttribute())
            {
                field.setConduit(new SessionAttributeFieldConduit(lookupname, stateful, field, typeName));              
            }
            else
            {
                field.setConduit(new AppStateFieldConduit(lookupname, stateful, field, typeName));
            }
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
    
    
    private class AppStateFieldConduit implements FieldConduit<Object>
    {
        public AppStateFieldConduit(String lookupname, Stateful stateful, PlasticField field, String typeName)
        {
            this.lookupname = lookupname;
            this.stateful = stateful;
            this.field = field;
            this.typeName = typeName;
            this.type = classCache.forName(typeName);
        }

        
        @Override
        @SneakyThrows(NamingException.class)
        public Object get(Object instance, InstanceContext context)
        {
            if(asm.exists(type))
            {
                return asm.get(type);
            }
            else
            {
                Object bean = locator.getJNDIObject(lookupname, stateful != null);
                asm.set((Class<Object>) type, bean);
                return bean;
            }
        }

        
        @Override
        @SneakyThrows(IllegalAccessException.class)
        public void set(Object instance, InstanceContext context, Object newValue)
        {
            throw new IllegalAccessException(String.format("Field %s is Read Only", field.getName()));
        }
        
        
        protected final String lookupname;
        protected final Stateful stateful;
        protected final PlasticField field;
        protected final String typeName;
        protected final Class<?> type;
    }
    
    
    private class SessionAttributeFieldConduit extends AppStateFieldConduit
    {
        public SessionAttributeFieldConduit(String lookupname, Stateful stateful, PlasticField field, String typeName)
        {
            super(lookupname, stateful, field, typeName);
        }

        
        @Override
        @SneakyThrows(NamingException.class)
        public Object get(Object instance, InstanceContext context)
        {
            final Session session = rg.getRequest().getSession(true);
            final String key = "".equals(stateful.sessionKey())? field.getName() : stateful.sessionKey();
        
            Object rv = session.getAttribute(key);
            if(rv == null)
            {
                rv = locator.getJNDIObject(lookupname, stateful != null);
                session.setAttribute(key, rv);
            }
            return rv;
        }
    }
    

    private final JNDIObjectLocator locator = new JNDIObjectLocator();
    private @Inject ApplicationStateManager asm;
    private @Inject ComponentClassCache classCache;
    private @Inject RequestGlobals rg;
    
    private static final String REMOTE = "REMOTE";
    private static final String LOCAL = "LOCAL";
    public static final Pattern StripLocalPattern = Pattern.compile(LOCAL + "|" + REMOTE, Pattern.CASE_INSENSITIVE);
}
