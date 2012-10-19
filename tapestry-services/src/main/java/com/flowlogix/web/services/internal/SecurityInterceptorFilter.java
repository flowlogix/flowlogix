/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

import com.flowlogix.security.ShiroSecurityInterceptor;
import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import lombok.SneakyThrows;
import org.apache.shiro.SecurityUtils;
import org.apache.tapestry5.services.ComponentEventRequestParameters;
import org.apache.tapestry5.services.ComponentRequestFilter;
import org.apache.tapestry5.services.ComponentRequestHandler;
import org.apache.tapestry5.services.PageRenderRequestParameters;

/**
 * Propagates EJB security context into Shiro and Tapestry realm
 * 
 * @author lprimak
 */
public class SecurityInterceptorFilter implements ComponentRequestFilter
{
    public SecurityInterceptorFilter(ComponentRequestFilter filter)
    {
        this.filter = filter;
    }

    
    @Override
    public void handleComponentEvent(final ComponentEventRequestParameters parameters, final ComponentRequestHandler handler) throws IOException
    {
        Subject.doAs(buildSubject(), new PrivilegedAction<Void>() 
        {
            @Override
            @SneakyThrows(IOException.class)
            public Void run()
            {
                filter.handleComponentEvent(parameters, handler);
                return null;
            }
        });
    }

    
    @Override
    public void handlePageRender(final PageRenderRequestParameters parameters, final ComponentRequestHandler handler) throws IOException
    {
        Subject.doAs(buildSubject(), new PrivilegedAction<Void>() 
        {
            @Override
            @SneakyThrows(IOException.class)
            public Void run()
            {
                filter.handlePageRender(parameters, handler);
                return null;
            }
        });
    }
    
    
    private Subject buildSubject()
    {
        Set<Principal> prinSet = new HashSet<>();
        prinSet.add(new ShiroSecurityInterceptor.SubjectWrapper(SecurityUtils.getSubject()));
        return new Subject(true, prinSet, perms, perms);
    }
    
        
    private final ComponentRequestFilter filter;
    private final Set<?> perms = Collections.unmodifiableSet(new HashSet<>());
}
