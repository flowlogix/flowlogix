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
     */
    public static void pingStateful(Session session)
    {
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
                    session.removeAttribute(attrName);
                }
            }
        }
    }
    
    
    public static final String ejbPrefix = "ejb:";
    public static final Pattern ejbPattern = Pattern.compile(String.format("^%s.*", ejbPrefix));
}
