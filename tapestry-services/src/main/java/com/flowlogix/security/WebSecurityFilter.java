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
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.util.ClassUtils;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
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
    @SneakyThrows({ClassNotFoundException.class, InstantiationException.class, IllegalAccessException.class,
                    NoSuchMethodException.class, InvocationTargetException.class})
    public void init(FilterConfig fc) throws ServletException
    {
        String backupShiroInitStr = fc.getInitParameter("backupShiroInit");
        if(backupShiroInitStr != null)
        {
            backupShiroInit = Boolean.valueOf(backupShiroInitStr);
        }
        
        if(backupShiroInit)
        {
            final List<Class<?>> realmClasses = parseRealms(fc.getInitParameter("realms"));  
            final List<Realm> realmInstances = new LinkedList<>();

            Class<?> securityManagerClass = DefaultWebSecurityManager.class;
            String securitiyManagerClassStr = fc.getInitParameter("securityManager");
            if(securitiyManagerClassStr != null)
            {
                securityManagerClass = ClassUtils.forName(securitiyManagerClassStr);
            }
            
            for (Class<?> realmClass : realmClasses)
            {
                realmInstances.add((Realm)realmClass.newInstance());
            }

            Constructor<?> constr = securityManagerClass.getConstructor(Collection.class);
            securityManager = (org.apache.shiro.mgt.SecurityManager) constr.newInstance(realmInstances);
        }
    }

    
    @Override
    @SneakyThrows(Throwable.class)
    public void doFilter(final ServletRequest sr, final ServletResponse sr1, final FilterChain fc) throws IOException, ServletException
    {
        boolean forcedShiroInit = backupShiroInit();
        
        if (ThreadContext.getSecurityManager() != null && ThreadContext.getSecurityManager() instanceof WebSecurityManager
                && (SecurityUtils.getSubject() instanceof WebSubject) == false)
        {
            WebSubject subject = new WebSubject.Builder(SecurityUtils.getSecurityManager(), sr, sr1).buildWebSubject();
            try {
                subject.execute(new Callable<Void>()
                {
                    @Override
                    public Void call() throws Exception
                    {
                        if (fc != null)
                        {
                            fc.doFilter(sr, sr1);
                        }
                        return null;
                    }
                });
            }            
            catch(ExecutionException e)
            {
                // unwrap Shiro's ExecutionException, interferes
                // with global exceptino handling mechanisms
                Throwable cause = e.getCause();
                if(cause != null)
                {
                    throw cause;
                }
                else
                {
                    throw e;
                }
            }
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
                ThreadContext.bind(securityManager);
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
    
    
    private List<Class<?>> parseRealms(String realmsConfigStr) throws ClassNotFoundException
    {
        List<Class<?>> realmClasses = new LinkedList<>();
        if(realmsConfigStr != null)
        {
            for(String realm : realmsConfigStr.split("[, ]"))
            {
                if(!realm.isEmpty())
                {
                    realmClasses.add(ClassUtils.forName(realm));
                }
            }
        }
        return realmClasses;
    }


    
    private boolean backupShiroInit = false;
    private org.apache.shiro.mgt.SecurityManager securityManager;
}
