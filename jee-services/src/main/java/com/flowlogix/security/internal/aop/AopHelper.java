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
package com.flowlogix.security.internal.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.authz.aop.AuthorizingAnnotationHandler;

/**
 * Enhanced from Tynamo Security
 */
public class AopHelper 
{
    /**
     * Create list of
     * {@link org.tynamo.shiro.extension.authz.aop.SecurityInterceptor}
     * instances for method. This method search all method and class annotations
     * and use annotation data for create interceptors.
     * <p/>
     * This method considers only those annotations that have been declared in
     * the set through parameters of the method and class, regardless of the
     * inheritance or interface implementations
     *
     * @param method
     * @param clazz
     * @return
     */
    public static List<SecurityInterceptor> createSecurityInterceptors(Method method, Class<?> clazz) {
        List<SecurityInterceptor> result = new ArrayList<>();

        if (isInterceptOnClassAnnotation(method.getModifiers())) {
            for (Class<? extends Annotation> ac
                    : getAutorizationAnnotationClasses()) {
                Annotation annotationOnClass = clazz.getAnnotation(ac);
                if (annotationOnClass != null) {
                    result.add(new DefaultSecurityInterceptor(annotationOnClass));
                }
            }
        }

        for (Class<? extends Annotation> ac
                : getAutorizationAnnotationClasses()) {
            Annotation annotation = method.getAnnotation(ac);
            if (annotation != null) {
                result.add(new DefaultSecurityInterceptor(annotation));
            }
        }

        return result;
    }

    /**
     * Create {@link org.apache.shiro.authz.aop.AuthorizingAnnotationHandler}
     * for annotation.
     *
     * @param annotation
     * @return
     */
    public static AuthorizingAnnotationHandler createHandler(Annotation annotation) {
        HandlerCreateVisitor visitor = new HandlerCreateVisitor();
        MethodAnnotationCaster.getInstance().accept(visitor, annotation);
        return visitor.getHandler();
    }

    /**
     * Rule under which determined the fate of the class contains annotation.
     * <p/>
     * All public and protected methods.
     * @param modifiers
     * @return 
     */
    public static boolean isInterceptOnClassAnnotation(int modifiers) {
        return Modifier.isPublic(modifiers)
                || Modifier.isProtected(modifiers);
    }

    public static Collection<Class<? extends Annotation>> getAutorizationAnnotationClasses() {
        return autorizationAnnotationClasses;
    }

    /**
     * List annotations classes which can be applied (either method or a class).
     */
    private final static Collection<Class<? extends Annotation>> autorizationAnnotationClasses;

    /**
     * Initialize annotations lists.
     */
    static {
        autorizationAnnotationClasses = new ArrayList<>(5);
        autorizationAnnotationClasses.add(RequiresPermissions.class);
        autorizationAnnotationClasses.add(RequiresRoles.class);
        autorizationAnnotationClasses.add(RequiresUser.class);
        autorizationAnnotationClasses.add(RequiresGuest.class);
        autorizationAnnotationClasses.add(RequiresAuthentication.class);
    }
}
