/*
 * Copyright 2016 lprimak.
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
package com.flowlogix.security.cdi;

import com.flowlogix.cdi.annotations.ShiroSecure;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * Automatically apply Shiro security to all EJBs
 * 
 * @author lprimak
 */
public class ShiroSecurityExtension implements Extension
{
    public<T> void addSecurity(@Observes ProcessAnnotatedType<T> pat)
    {
        if(pat.getAnnotatedType().isAnnotationPresent(Stateless.class)
           || pat.getAnnotatedType().isAnnotationPresent(Stateful.class)
           || pat.getAnnotatedType().isAnnotationPresent(Singleton.class))
        {
            pat.setAnnotatedType(new Wrapper<>(pat.getAnnotatedType(), () -> ShiroSecure.class));
        }
    }
    
    
    private static class Wrapper<T> implements AnnotatedType<T>
    {
        public Wrapper(AnnotatedType<T> wrapped, Annotation... additionalAnnotations)
        {
            this.wrapped = wrapped;
            annotations = ImmutableSet.<Annotation>builder().addAll(wrapped.getAnnotations())
                    .add(additionalAnnotations).build();
        }
        
        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType)
        {
            return annotations.stream().anyMatch(annotation -> annotationType.isInstance(annotation));
        }
        
        interface Exclusions
        {
            boolean isAnnotationPresent(Class<? extends Annotation> annotationType);
            boolean getAnnotations();
        }
        
        private abstract class AT implements AnnotatedType<T> { };
        
        private final @Delegate(types = {AT.class, Annotated.class}, excludes = Exclusions.class) AnnotatedType<T> wrapped;
        private final @Getter Set<Annotation> annotations;
    }
}
