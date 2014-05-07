package com.flowlogix.web.services;

import com.flowlogix.ejb.StatefulUtil;
import com.flowlogix.web.services.internal.EJBAnnotationWorker;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.web.session.HttpServletSession;
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
                    StatefulUtil.pingStateful(new HttpServletSession(session, null));
                }
                return handler.service(request, response);
            }
        }, "after:IgnoredPathsFilter");
    }
}
