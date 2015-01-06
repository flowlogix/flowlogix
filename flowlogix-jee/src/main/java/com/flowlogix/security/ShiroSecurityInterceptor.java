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
import java.io.Serializable;
import java.util.List;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Enforce Shiro security on EJBs and CDI Beans
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
        checkPermissions(ctx);
        return ctx.proceed();
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
    
    
    private static final long serialVersionUID = 1L;
}
