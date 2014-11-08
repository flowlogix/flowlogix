package com.flowlogix.ejb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;
import javax.ejb.Remote;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * JNDIObjectLocator is used to centralize all JNDI lookups. It minimizes the overhead of JNDI lookups by caching the
 * objects it looks up.
 * 
 * <a href="http://code.google.com/p/flowlogix/wiki/TLJNDIObjectLocator"
 *    target="_blank">See Documentation</a>
 * 
 * @author Geoff Callendar
 * Enhancements by Lenny Primak
 */
public class JNDIObjectLocator
{
    public JNDIObjectLocator()
    {
        this(PORTABLE_NAME_PREFIX);
    }
    
    
    public JNDIObjectLocator(String portableNamePrefix)
    {
        this(null, portableNamePrefix);
    }

    
    public JNDIObjectLocator(Hashtable<String, String> env)
    {
        this(env, PORTABLE_NAME_PREFIX);
    }

    
    public JNDIObjectLocator(Hashtable<String, String> env, String portableNamePrefix)
    {
        this.env = env;
        this.portableNamePrefix = portableNamePrefix;
    }
    
    
    @SneakyThrows(NamingException.class)
    public<T> T getObject(Class<T> beanClass)
    {
        boolean remote = beanClass.isAnnotationPresent(Remote.class);
        String name = guessByType(beanClass.getName());
        return getObject(prependPortableName(name), remote && !cacheRemote);
    }
    
    
    public<T> T getObject(String jndiName) throws NamingException
    {
        return getObject(jndiName, false);
    }

    
    @SuppressWarnings("unchecked")
    public<T> T getObject(String jndiName, boolean noCaching) throws NamingException
    {
        return (T)getJNDIObject(jndiName, noCaching);
    }

    
    public synchronized void clear()
    {
        jndiObjectCache.clear();
    }
    
    
    public<T> T getJNDIObject(String jndiName) throws NamingException
    {
        return getJNDIObject(jndiName, false);
    }
    
    
    public<T> T getJNDIObject(String jndiName, boolean noCaching) throws NamingException
    {
        if(noCaching || this.noCaching)
        {
            return lookup(jndiName, true);
        }
        
        @SuppressWarnings("unchecked")
        T jndiObject = (T)jndiObjectCache.get(jndiName);

        if (jndiObject == null && !jndiObjectCache.containsKey(jndiName))
        {
            try
            {
                jndiObject = lookup(jndiName, false);
                jndiObjectCache.put(jndiName, jndiObject);
            } catch (NamingException e)
            {
                clear();
                throw e;
            }
        }
        return jndiObject;
    }
    
    
    public String prependPortableName(String lookupname)
    {
        //convert to jndi name
        if (!lookupname.startsWith("java:")) 
        {
            lookupname = portableNamePrefix + "/" + lookupname;
        }
        return lookupname;
    }
    
    
    public static String guessByType(String type) 
    {
        String lookupname = type.substring(type.lastIndexOf(".") + 1);
        // support naming convention that strips Local/Remote from the
        // end of an interface class to try to determine the actual bean name,
        // to avoid @EJB(beanName="myBeanName"), and just use plain old @EJB
        String uc = lookupname.toUpperCase();
        if (uc.endsWith(LOCAL) || uc.endsWith(REMOTE)) {
            lookupname = StripInterfaceSuffixPattern.matcher(lookupname).replaceFirst("");
        }
        return lookupname + "!" + type;
    }
    
    
    private InitialContext getInitialContext() throws NamingException
    {
        if(env == null)
        {
            return new InitialContext();
        }
        else
        {
            return new InitialContext(env);
        }
    }

    
    @SuppressWarnings("unchecked")
    private synchronized<T> T lookup(String name, boolean noCaching) throws NamingException
    {
        if(this.noCaching || noCaching)
        {
            return (T)getInitialContext().lookup(name);
        }
        // Recheck the cache because the name we're looking for may have been added while we were waiting for sync.

        if (!jndiObjectCache.containsKey(name))
        {
            try
            {
                return (T)getInitialContext().lookup(name);
            } catch (NameNotFoundException e)
            {
                clear();
                throw e;
            }
        } else
        {
            return (T)jndiObjectCache.get(name);
        }
    }

    
    private final Hashtable<String, String> env;
    private @Getter @Setter String portableNamePrefix;
    private @Getter @Setter boolean noCaching = false;
    private @Getter @Setter boolean cacheRemote = false;
    private final Map<String, Object> jndiObjectCache = Collections.synchronizedMap(new HashMap<String, Object>());
        
    public static final String REMOTE = "REMOTE";
    public static final String LOCAL = "LOCAL";
    private static final String PORTABLE_NAME_PREFIX = "java:module";
    public static final Pattern StripInterfaceSuffixPattern = Pattern.compile(LOCAL + "|" + REMOTE, Pattern.CASE_INSENSITIVE);
}
