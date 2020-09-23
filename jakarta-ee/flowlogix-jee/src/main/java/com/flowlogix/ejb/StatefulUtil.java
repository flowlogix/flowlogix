/*
 * Copyright 2014 lprimak.
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

package com.flowlogix.ejb;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import java.util.List;
import java.util.regex.Pattern;
import javax.ejb.EJBException;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.Session;

/**
 * Stateful Session Bean (SFSB) Utilities
 * 
 * @author lprimak
 */
@Slf4j
public class StatefulUtil
{
    /**
     * Pings all pingable SFSBs in the session
     * 
     * @param session 
     * @return true if successful, false if any of the pings failed
     */
    public static boolean pingStateful(Session session)
    {
        boolean rv = true;
        
        List<String> attrNames = FluentIterable.from(session.getAttributeKeys()).transform(new Function<Object, String>()
        {
            @Override
            public String apply(Object f)
            {
                if(f instanceof String)
                {
                    return (String)f;
                }
                else
                {
                    return null;
                }
            }
        }).filter(Predicates.and(Predicates.notNull(), Predicates.contains(ejbPattern))).toList();
        for (String attrName : attrNames)
        {
            synchronized (session.getId().toString().intern())
            {
                try
                {
                    Object _pingable = session.getAttribute(attrName);
                    if (_pingable instanceof Pingable)
                    {
                        Pingable pingable = (Pingable) _pingable;
                        pingable.ping();
                    }
                } 
                catch (EJBException e)
                {
                    log.debug("Failed to Ping Stateful EJB: ", e);
                    rv = false; // signal failure if any of the pings fail
                    session.removeAttribute(attrName);
                }
            }
        }
        
        return rv;
    }
    
    
    /**
     * Retrieve Stateful EJB, Create if it doesn't exist
     * @param <T> EJB Type
     * @param session
     * @param cls Type of EJB
     * 
     * @return Stateful EJB
     */
    static public<T> T getStatefulEJB(Session session, Class<T> cls)
    {
        return getStatefulEJB(session, cls, true);
    }


    /**
     * Retrieve Stateful EJB, or null if its not available and not created
     * 
     * @param <T> Type of EJB
     * @param session
     * @param cls Class of EJB to be created / retrieved
     * @param doCreate Create EJB if does not exist
     * 
     * @return EJB, or null if doCreate is false and EJB does not exist
     */
    @SuppressWarnings("unchecked")
    static public<T> T getStatefulEJB(Session session, Class<T> cls, boolean doCreate)
    {
        final String attrName = StatefulUtil.ejbPrefix + cls.getName();
        T rv;
        synchronized(session.getId().toString().intern())
        {
            rv = (T) session.getAttribute(attrName);
            if (doCreate && rv == null)
            {
                rv = new JNDIObjectLocator().getObject(cls);
                session.setAttribute(attrName, rv);
            }
        }
        return rv;
    }

    
    public static final String ejbPrefix = "ejb:";
    public static final Pattern ejbPattern = Pattern.compile(String.format("^%s.*", ejbPrefix));
}
