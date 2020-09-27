package com.flowlogix.jndi;

import com.flowlogix.util.Lazy;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import javax.ejb.Remote;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Lombok;
import lombok.NonNull;
import lombok.Singular;
import lombok.SneakyThrows;

/**
 * JNDIObjectLocator is used to centralize all JNDI lookups. It minimizes the overhead of JNDI lookups by caching the
 * objects it looks up.
 *
 * Implements the ServiceLocator design pattern
 *
 * Major features are:
 * - thread-safe
 * - immutable
 * - serializable
 * - selectively disables the cache if objects are remote
 *
 * <a href="http://code.google.com/p/flowlogix/wiki/TLJNDIObjectLocator
 * " *    target="_blank">See Documentation</a>
 *
 * @author Geoff Callendar Enhancements by Lenny Primak
 */
@Builder(toBuilder = true)
public class JNDIObjectLocator implements Serializable {
    /**
     * to be passed into InitialContext()
     */
    private final @Singular("environment") Map<String, String> environment;
    /**
     * Used in construction of portable JNDI names
     * usually java:module (default)
     */
    private final @Builder.Default String portableNamePrefix = PORTABLE_NAME_PREFIX;
    /**
     * whether to disable cache. Default is false
     */
    private final boolean noCaching;
    /**
     * whether to cache remote EJBs, usually false
     */
    private final boolean cacheRemote;

    @Getter(AccessLevel.PACKAGE)
    private transient final Map<String, Object> jndiObjectCache = new ConcurrentHashMap<>();
    private transient final Lazy<InitialContext> initialContext = new Lazy<>(this::createInitialContext);
    private transient final Lock initialContextLock = new ReentrantLock();

    /**
     * naming convention suffix for remote beans
     */
    public static final String REMOTE = "REMOTE";
    /**
     * naming convention suffix for local beans
     */
    public static final String LOCAL = "LOCAL";
    static final String PORTABLE_NAME_PREFIX = "java:module";
    /**
     * pattern matcher for stripping local or remote suffix from beans
     */
    public static final Pattern StripInterfaceSuffixPattern = Pattern.compile(LOCAL + "|" + REMOTE, Pattern.CASE_INSENSITIVE);
    private static final long serialVersionUID = 2L;


    /**
     * Returns an object from JNDI based on beanClass
     * Uses portable object names and convention to derive appropriate JNDI name
     *
     * @param <T> object type
     * @param beanClass type of object to look up in JNDI
     * @return resulting object
     * @throws NamingException (sneaky-throws)
     */
    @SneakyThrows(NamingException.class)
    public <T> T getObject(Class<T> beanClass) {
        boolean remote = beanClass.isAnnotationPresent(Remote.class);
        String name = guessByType(beanClass.getName());
        return getObject(prependPortableName(name), remote && !cacheRemote);
    }

    /**
     * Returns an object based on JNDI name
     *
     * @param <T>
     * @param jndiName
     * @return
     * @throws NamingException
     */
    public <T> T getObject(String jndiName) throws NamingException {
        return getObject(jndiName, false);
    }

    /**
     * Return an object based on JNDI name,
     * uses the caching parameter to control whether this object is cached
     *
     * @param <T>
     * @param jndiName
     * @param noCaching
     * @return
     * @throws NamingException
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String jndiName, boolean noCaching) throws NamingException {
        return (T) getJNDIObject(jndiName, noCaching);
    }

    /**
     * clears object cache
     */
    public void clearCache() {
        jndiObjectCache.clear();
    }

    /**
     * converts class name into java portable JNDI lookup name
     *
     * @param lookupname
     * @return portable JNDI name
     */
    public String prependPortableName(String lookupname) {
        //convert to jndi name
        if (!lookupname.startsWith("java:")) {
            lookupname = portableNamePrefix + "/" + lookupname;
        }
        return lookupname;
    }

    /**
     * adds initial host property to builder
     *
     * @param builder
     * @param initialHost
     * @return builder
     */
    public static JNDIObjectLocatorBuilder initialHost(JNDIObjectLocatorBuilder builder, @NonNull String initialHost) {
        return builder.environment("org.omg.CORBA.ORBInitialHost", initialHost);
    }

    /**
     * adds initial port property to builder
     *
     * @param builder
     * @param initialPort
     * @return builder
     */
    public static JNDIObjectLocatorBuilder initialPort(JNDIObjectLocatorBuilder builder, int initialPort) {
        return builder.environment("org.omg.CORBA.ORBInitialPort", Integer.toString(initialPort));
    }

    /**
     * returns JNDI name based on the class (type) name
     *
     * @param type name of the class
     * @return JNDI lookup name
     */
    public static String guessByType(String type) {
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

    @SneakyThrows(NamingException.class)
    private InitialContext createInitialContext() {
        if (environment.isEmpty()) {
            return new InitialContext();
        } else {
            return new InitialContext(new Hashtable<>(environment));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getJNDIObject(String jndiName, boolean noCaching) throws NamingException {
        if (noCaching || this.noCaching) {
            initialContextLock.lock();
            try {
                return (T) initialContext.get().lookup(jndiName);
            } finally {
                initialContextLock.unlock();
            }
        }

        T jndiObject = (T) jndiObjectCache.computeIfAbsent(jndiName, (key) -> {
            initialContextLock.lock();
            try {
                return (T) initialContext.get().lookup(jndiName);
            } catch (NamingException ex) {
                clearCache();
                throw Lombok.sneakyThrow(ex);
            } finally {
                initialContextLock.unlock();
            }
        });

        return jndiObject;
    }

    /**
     * this deals with transient final fields correctly
     *
     * @return new object
     */
    private Object readResolve() {
        return toBuilder().build();
    }
}
