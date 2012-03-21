/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.security;

import com.flowlogix.web.services.internal.SecurityInterceptorFilter;
import java.io.Serializable;
import java.security.AccessController;
import java.security.Principal;
import java.util.concurrent.Callable;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.apache.shiro.subject.Subject;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLShiroSecurityInterceptor"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Interceptor
public class ShiroSecurityInterceptor implements Serializable
{
    @AroundInvoke
    public Object propagateShiroSecurity(final InvocationContext ctx) throws Exception
    {
        Subject subject = null;
        try
        {
            Principal principal = javax.security.auth.Subject.getSubject(AccessController.getContext()).
                    getPrincipals().iterator().next();
            subject = ((SecurityInterceptorFilter.SubjectWrapper) principal).getSubject();
        } finally
        {
            // intentionally left blank
        }
        if (subject != null)
        {
            return subject.execute(new Callable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    return ctx.proceed();
                }
            });

        } else
        {
            return ctx.proceed();
        }
    }

    
    private static final long serialVersionUID = 1L;
}
