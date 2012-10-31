package com.flowlogix.web.services;

import com.flowlogix.ejb.JNDIObjectLocator;
import com.flowlogix.ejb.Pingable;
import com.flowlogix.web.services.internal.EJBAnnotationWorker;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.util.ClassUtils;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Primary;
import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.HttpServletRequestHandler;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integrate EJB3 Beans into the Web site
 *
 * @author lprimak
 */
public class EjbModule
{
    @Contribute(ComponentClassTransformWorker2.class)
    @Primary
    public static void provideClassTransformWorkers(OrderedConfiguration<ComponentClassTransformWorker2> configuration)
    {
        configuration.addInstance("EJB", EJBAnnotationWorker.class, "before:Property");
    }


    /**
     * ping all session beans in all requests so they don't time out before the session
     * @param config
     */
    public void contributeHttpServletRequestHandler(OrderedConfiguration<HttpServletRequestFilter> config)
    {
        config.add("PingSessionBeansFilter", new HttpServletRequestFilter()
        {
            @Override
            public boolean service(HttpServletRequest request, HttpServletResponse response, HttpServletRequestHandler handler) throws IOException
            {
                HttpSession session = request.getSession(false);
                if(session != null)
                {
                    @SuppressWarnings("unchecked")
                    final List<String> _allAttrs = Collections.list(session.getAttributeNames());
                    final Collection<String> allAttrs = Collections2.filter(_allAttrs, Predicates.contains(ejbPattern));
                    final JNDIObjectLocator locator = allAttrs.isEmpty() ? null : new JNDIObjectLocator();
                    for (String attrName : allAttrs)
                    {
                        Class<?> ejbClass = ClassUtils.forName(attrName.replaceFirst("ejb:", ""));
                        try
                        {
                            Object _pingable = locator.getObject(ejbClass);
                            if (_pingable instanceof Pingable)
                            {
                                Pingable pingable = (Pingable) _pingable;
                                pingable.ping();
                            }
                        }
                        catch (Exception e)
                        {
                            log.debug("Failed to Ping Stateful EJBs", e);
                            session.removeAttribute(attrName);
                        }
                    }
                }
                return handler.service(request, response);
            }


            private final Pattern ejbPattern = Pattern.compile("^ejb:.*");
        }, "after:IgnoredPathsFilter");
    }


    private static final Logger log = LoggerFactory.getLogger(EjbModule.class);
}
