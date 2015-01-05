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
package com.flowlogix.security;

import com.flowlogix.security.internal.aop.AopHelper;
import com.flowlogix.security.internal.aop.SecurityInterceptor;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.io.DefaultSerializer;
import org.apache.shiro.io.Serializer;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;

/**
 * Translate EJB Security Principal to Shiro
 * 
 * <a href="http://code.google.com/p/flowlogix/wiki/TLShiroSecurityInterceptor"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Slf4j
public class ShiroSecurityInterceptor implements Serializable
{
    @AroundInvoke
    public Object propagateShiroSecurity(final InvocationContext ctx) throws Exception
    {
        Subject subject = null;
        try
        {
            SubjectWrapper sw = Iterables.getOnlyElement(javax.security.auth.Subject
                    .getSubject(AccessController.getContext()).getPrincipals(SubjectWrapper.class));
            subject = new Subject.Builder().principals(sw.buildPrincipals()).buildSubject();
        } catch(Throwable e)
        {
            log.debug("Failed Translating Shiro/EJB Security", e);
        }
        if (subject != null)
        {
            return subject.execute(new Callable<Object>()
            {
                @Override
                public Object call() throws Exception
                {
                    checkPermissions(ctx);
                    return ctx.proceed();
                }
            });
        } else
        {
            checkPermissions(ctx);
            return ctx.proceed();
        }            
    }
    
        
    private void checkPermissions(final InvocationContext ctx) throws Exception
    {
        List<SecurityInterceptor> siList = AopHelper.createSecurityInterceptors(ctx.getMethod(), 
                ctx.getMethod().getDeclaringClass());
        for(SecurityInterceptor si : siList)
        {
            si.intercept();
        }
    }
    
        
    public static javax.security.auth.Subject buildSubject()
    {
        Set<Principal> prinSet = new HashSet<>();
        prinSet.add(new ShiroSecurityInterceptor.SubjectWrapper(SecurityUtils.getSubject()));
        return new javax.security.auth.Subject(true, prinSet, perms, perms);
    }

    
    public @EqualsAndHashCode static class SubjectWrapper implements Principal, Serializable
    {
        public SubjectWrapper(org.apache.shiro.subject.Subject subject)
        {
            PrincipalCollection principals = subject.getPrincipals();
            if(principals == null)
            {
                principals = new SimplePrincipalCollection();
            }
            name = Base64.encodeToString(ser.serialize(principals));    
        }
        
        
        private PrincipalCollection buildPrincipals()
        {
            return ser.deserialize(Base64.decode(name));
        }


        private final @Getter String name;
        private static final Serializer<PrincipalCollection> ser = new DefaultSerializer<>();
        private static final long serialVersionUID = 1L;
    }

    
    private static final long serialVersionUID = 1L;
    private static final Set<?> perms = Collections.unmodifiableSet(new HashSet<>());
}
