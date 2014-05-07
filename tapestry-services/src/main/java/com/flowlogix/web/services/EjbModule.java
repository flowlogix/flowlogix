package com.flowlogix.web.services;

import com.flowlogix.ejb.Pingable;
import com.flowlogix.web.services.internal.EJBAnnotationWorker;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.ejb.EJBException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.Primary;
import org.apache.tapestry5.services.HttpServletRequestFilter;
import org.apache.tapestry5.services.HttpServletRequestHandler;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;

/**
 * Integrate EJB3 Beans into the Web site
 *
 * @author lprimak
 */
@Slf4j
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
                    synchronized(session.getId().intern())
                    {
                        List<String> attrNames = FluentIterable.from(Collections.list(session.getAttributeNames()))
                                .filter(Predicates.contains(ejbPattern)).toList();
                        for (String attrName : attrNames)
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
                                log.debug("Failed to Ping Stateful EJBs", e);
                                session.removeAttribute(attrName);
                            }
                        }
                    }
                }
                return handler.service(request, response);
            }


            private final Pattern ejbPattern = Pattern.compile("^ejb:.*");
        }, "after:IgnoredPathsFilter");
    }
}
