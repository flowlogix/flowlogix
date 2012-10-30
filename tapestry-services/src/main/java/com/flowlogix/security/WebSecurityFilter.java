/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.security;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import lombok.SneakyThrows;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.util.ClassUtils;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.subject.WebSubject;

/**
 * allows access to shiro security subject within unrelated servlets<br>
 * <a href="http://code.google.com/p/flowlogix/wiki/TLWebSecurityFilter"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@WebFilter(filterName = "WebSecurityFilter", urlPatterns = {"/*"} )
public class WebSecurityFilter implements Filter
{
    @Override
    @SneakyThrows(ClassNotFoundException.class)
    public void init(FilterConfig fc) throws ServletException
    {
        String backupShiroInitStr = fc.getInitParameter("backupShiroInit");
        if(backupShiroInitStr != null)
        {
            backupShiroInit = Boolean.valueOf(backupShiroInitStr);
        }
        
        if(backupShiroInit)
        {
            parseRealms(fc.getInitParameter("realms"));
            String securitiyManagerClassStr = fc.getInitParameter("securityManager");
            if(securitiyManagerClassStr != null)
            {
                securityManagerClass = (Class<? extends org.apache.shiro.mgt.SecurityManager>) ClassUtils.forName(securitiyManagerClassStr);
            }
        }
    }

    
    @Override
    public void doFilter(final ServletRequest sr, final ServletResponse sr1, final FilterChain fc) throws IOException, ServletException
    {
        boolean forcedShiroInit = backupShiroInit();
        
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
        
        undoShiroInit(forcedShiroInit);
    }

    
    @Override
    public void destroy()
    {
        // blank
    }   
    
    
    @SneakyThrows({ InstantiationException.class, IllegalAccessException.class, 
        NoSuchMethodException.class, InvocationTargetException.class })
    private boolean backupShiroInit()
    {
        boolean forcedShiroActivateion = false;
        if (backupShiroInit)
        {
            try
            {
                SecurityUtils.getSecurityManager();
            } catch (UnavailableSecurityManagerException e)
            {
                List<Realm> realmInstances = new LinkedList<>();
                for (Class<Realm> realmClass : realmClasses)
                {
                    realmInstances.add(realmClass.newInstance());
                }

                Constructor<org.apache.shiro.mgt.SecurityManager> constr = (Constructor<org.apache.shiro.mgt.SecurityManager>) 
                        securityManagerClass.getConstructor(Collection.class);
                ThreadContext.bind(constr.newInstance(realmInstances));

                forcedShiroActivateion = true;
            }
        }
        return forcedShiroActivateion;
    }
    
    
    private void undoShiroInit(boolean forcedShiroActivateion)
    {
        if(forcedShiroActivateion)
        {
            ThreadContext.unbindSecurityManager();
        }
    }
    
    
    private void parseRealms(String realmsConfigStr) throws ClassNotFoundException
    {
        if(realmsConfigStr != null)
        {
            for(String realm : realmsConfigStr.split("[, ]"))
            {
                if(!realm.isEmpty())
                {
                    realmClasses.add((Class<Realm>)ClassUtils.forName(realm));
                }
            }
        }
    }


    
    private boolean backupShiroInit = false;
    private final List<Class<Realm>> realmClasses = new LinkedList<>();
    private Class<? extends org.apache.shiro.mgt.SecurityManager> securityManagerClass = DefaultWebSecurityManager.class;
}
