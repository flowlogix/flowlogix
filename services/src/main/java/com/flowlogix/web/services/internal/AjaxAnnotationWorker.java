/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

import com.flowlogix.web.services.annotations.AJAX;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticMethod;
import org.apache.tapestry5.services.ComponentSource;
import org.apache.tapestry5.services.Request;
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
            if(method.isVoid() == false)
            {
                method.addAdvice(new MethodAdvice() 
                {
                    @Override
                    public void advise(MethodInvocation invocation)
                    {
                        invocation.proceed();
                        Object result = invocation.getReturnValue();
                        if (!request.isXHR())
                        {
                            if(result != null)
                            {
                                result = defaultForReturnType(result.getClass());
                            }
                        }
                        else if(annotation.discardAfter())
                        {
                            cs.getActivePage().getComponentResources().discardPersistentFieldChanges();
                        }
                        invocation.setReturnValue(result);
                    }
                });
            }
            else
            {
                throw new RuntimeException(
                "@AJAX can be applied to non-void event handlers only"); 
            }
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
}
