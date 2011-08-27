/**
 * @(#)EJBAnnotationWorker.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 */
package com.flowlogix.web.services.internal;

import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.naming.NamingException;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticField;
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
    @SneakyThrows(NamingException.class)
    public void transform(PlasticClass plasticClass,
            TransformationSupport support, MutableComponentModel model)
    {
        for (PlasticField field : plasticClass.getFieldsWithAnnotation(EJB.class))
        {
            final EJB annotation = field.getAnnotation(EJB.class);
            String fieldType = field.getTypeName();
            String lookupname = null;

            //try lookup
            if (!isBlankOrNull(annotation.lookup()))
            {
                lookupname = annotation.lookup();
            } //try name
            else if(!isBlankOrNull(annotation.name()))
            {
                lookupname = annotation.name();
            }
            else if(!isBlankOrNull(annotation.beanName()))
            {
                lookupname = annotation.beanName();
            }

            //use type
            if (lookupname == null)
            {
                lookupname = fieldType.substring(fieldType.lastIndexOf(".") + 1);
                // support naming convention that strips Local/Remote from the
                // end of an interface class to try to determine the actual bean name,
                // to avoid @EJB(beanName="myBeanName"), and just use plain old @EJB
                String uc = lookupname.toUpperCase();
                if(uc.endsWith(LOCAL) || uc.endsWith(REMOTE))
                {
                    lookupname = stripLocalPattern.matcher(lookupname).replaceFirst("");
                }
            }

            //convert to jndi name
            if (!lookupname.startsWith("java:"))
            {
                lookupname = "java:module/" + lookupname;
            }

            Object injectionValue = locator.getJNDIObject(lookupname);

            if (injectionValue != null)
            {
                field.inject(injectionValue);
                field.claim(annotation);
            }
        }
    }
    

    private boolean isBlankOrNull(String s)
    {
        return s == null || s.trim().equals("");
    }
    

    private final JNDIObjectLocator locator = new JNDIObjectLocator();
    private final String REMOTE = "REMOTE";
    private final String LOCAL = "LOCAL";
    private @Getter final Pattern stripLocalPattern = Pattern.compile(LOCAL + "|" + REMOTE, Pattern.CASE_INSENSITIVE);
}
