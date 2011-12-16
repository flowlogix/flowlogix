/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.security;

import java.io.IOException;
import java.util.concurrent.Callable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.subject.WebSubject;

/**
 * allows access to shiro security subject within unrelated servlets<br>
 * <a href="http://code.google.com/p/flowlogix/wiki/TLWebSecurityFilter"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@WebFilter(urlPatterns= {"/*"} )
public class WebSecurityFilter implements Filter
{
    @Override
    public void init(FilterConfig fc) throws ServletException
    {
        // blank
    }

    
    @Override
    public void doFilter(final ServletRequest sr, final ServletResponse sr1, final FilterChain fc) throws IOException, ServletException
    {
        if (ThreadContext.getSecurityManager() != null && (SecurityUtils.getSubject() instanceof WebSubject) == false)
        {
            WebSubject subject = new WebSubject.Builder(SecurityUtils.getSecurityManager(), sr, sr1).buildWebSubject();
            subject.execute(new Callable<Void>() {

                @Override
                public Void call() throws Exception 
                {
                    if(fc != null)
                    {
                        fc.doFilter(sr, sr1);
                    }
                    return null;
                }
            });
        }
        else
        {
            fc.doFilter(sr, sr1);
        }
    }

    
    @Override
    public void destroy()
    {
        // blank
    }    
}
