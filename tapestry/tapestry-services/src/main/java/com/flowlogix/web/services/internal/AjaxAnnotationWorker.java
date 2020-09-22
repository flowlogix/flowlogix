/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

import com.flowlogix.session.internal.SessionTrackerUtil;
import com.flowlogix.web.mixins.SessionTracker;
import com.flowlogix.web.services.annotations.AJAX;
import java.io.IOException;
import lombok.SneakyThrows;
import org.apache.tapestry5.annotations.SessionAttribute;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticMethod;
import org.apache.tapestry5.services.ComponentSource;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

/**
 * Provides internal support for {@link com.flowlogix.web.services.annotations.AJAX} annotation
 * 
 * @author lprimak
 */
public class AjaxAnnotationWorker implements ComponentClassTransformWorker2
{
    public AjaxAnnotationWorker(Request request)
    {
        this.request = request;
    }


    @Override
    public void transform(PlasticClass plasticClass, TransformationSupport support, MutableComponentModel model)
    {
        boolean hasTrackerMixin = model.getMixinClassNames().contains(SessionTracker.class.getName());
        for(PlasticMethod method : plasticClass.getMethodsWithAnnotation(AJAX.class))
        {
            final AJAX annotation = method.getAnnotation(AJAX.class);
            final boolean isVoid = method.isVoid();
            if(annotation.requireSession() && (!hasTrackerMixin))
            {
                model.addMixinClassName(SessionTracker.class.getName());
                hasTrackerMixin = true;
            }
            method.addAdvice(new MethodAdvice()
            {
                @Override
                @SneakyThrows(IOException.class)
                public void advise(MethodInvocation invocation)
                {
                    if (!request.isXHR() || annotation.requireSession() == false)
                    {
                        invocation.proceed();
                    } else
                    {
                        // do not invoke on bad sessions
                        if (SessionTrackerUtil.isValidSession(rg.getActivePageName(), rg.getRequest().getSession(false)))
                        {
                            invocation.proceed();
                        } else
                        {
                            showSessionExpiredMessage = true;
                            SessionTrackerUtil.redirectToSelf(rg, linkSource);
                            if(!isVoid)
                            {
                                invocation.setReturnValue(null);
                            }
                            return;
                        }
                    }

                    Object result = null;
                    if(!isVoid)
                    {
                        result = invocation.getReturnValue();
                    }
                    if (!request.isXHR())
                    {
                        if (result != null)
                        {
                            result = defaultForReturnType(result.getClass());
                        }
                    } else
                    {
                        if (annotation.discardAfter())
                        {
                            cs.getActivePage().getComponentResources().discardPersistentFieldChanges();
                        }
                    }
                    if(!isVoid)
                    {
                        invocation.setReturnValue(result);
                    }
                }
            });
        }
    }



    private Object defaultForReturnType(Class<?> returnType)
    {
        if (!returnType.isPrimitive())
        {
            return null;
        }
        if (returnType.equals(boolean.class))
        {
            return false;
        }
        return 0;
    }


    private final Request request;
    private @Inject ComponentSource cs;
    private @Inject RequestGlobals rg;
    private @Inject PageRenderLinkSource linkSource;
    private @SessionAttribute Boolean showSessionExpiredMessage;
}
