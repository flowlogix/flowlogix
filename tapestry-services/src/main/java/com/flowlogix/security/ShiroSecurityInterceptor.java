/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.security;

import java.io.Serializable;
import java.security.AccessController;
import java.security.Principal;
import java.util.concurrent.Callable;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.io.DefaultSerializer;
import org.apache.shiro.io.Serializer;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;

/**
 * <a href="http://code.google.com/p/flowlogix/wiki/TLShiroSecurityInterceptor"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Interceptor
@Slf4j
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
            subject = new Subject.Builder().principals(SubjectWrapper.buildPrincipals(principal.getName())).buildSubject();
        } catch(Throwable e)
        {
            log.debug("Translating Shiro/EJB Security", e);
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
    
    
    public @EqualsAndHashCode static class SubjectWrapper implements Principal, Serializable
    {
        public SubjectWrapper(org.apache.shiro.subject.Subject subject)
        {
            Object primary = subject.getPrincipal();
            if(primary != null)
            {
                name = Base64.encodeToString(ser.serialize(subject.getPrincipals()));    
            }
            else
            {
                name = null;
            }
        }
        
        
        private static PrincipalCollection buildPrincipals(String name)
        {
            if(name == null)
            {
                return new SimplePrincipalCollection();
            }
            else
            {
                return ser.deserialize(Base64.decode(name));
            }
        }


        
        private final @Getter String name;
        private static final Serializer<PrincipalCollection> ser = new DefaultSerializer<>();
        private static final long serialVersionUID = 1L;
    }

    
    private static final long serialVersionUID = 1L;
}
