/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

import com.flowlogix.session.SessionTrackerSSO;
import com.flowlogix.web.services.annotations.AJAX;
import java.io.IOException;
import lombok.SneakyThrows;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticMethod;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.ComponentSource;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

/**
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
    public void transform(final PlasticClass plasticClass, TransformationSupport support, MutableComponentModel model)
    {
        for(final PlasticMethod method : plasticClass.getMethodsWithAnnotation(AJAX.class))
        {
            final AJAX annotation = method.getAnnotation(AJAX.class);
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
                        SessionTrackerSSO sso = stateMgr.getIfExists(SessionTrackerSSO.class);
                        if (sso != null && sso.isValidSession(rg.getActivePageName()))
                        {
                            invocation.proceed();
                        } else
                        {
                            SessionTrackerSSO.redirectToSelf(rg, linkSource, isSecure);
                            invocation.setReturnValue(null);
                            return;
                        }
                    }

                    Object result = invocation.getReturnValue();
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
                    invocation.setReturnValue(result);
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
    private @Inject ApplicationStateManager stateMgr;
    private @Inject RequestGlobals rg;
    private @Inject PageRenderLinkSource linkSource;
    private @Inject @Symbol(SymbolConstants.SECURE_ENABLED) boolean isSecure;  
}
