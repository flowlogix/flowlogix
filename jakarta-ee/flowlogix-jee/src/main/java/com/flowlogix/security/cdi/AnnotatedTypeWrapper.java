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

import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * Wraps annotation types to facilitate additional annotations for CDI
 * 
 * @author lprimak
 * @param <T> type of annotated class
 */
public class AnnotatedTypeWrapper<T> implements AnnotatedType<T> 
{
    public AnnotatedTypeWrapper(AnnotatedType<T> wrapped, Annotation... additionalAnnotations)
    {
        this(wrapped, true, additionalAnnotations);
    }
    
    
    public AnnotatedTypeWrapper(AnnotatedType<T> wrapped, boolean keepOriginalAnnotations,
            Annotation... additionalAnnotations)
    {
        this.wrapped = wrapped;
        ImmutableSet.Builder<Annotation> builder = ImmutableSet.<Annotation>builder();
        if(keepOriginalAnnotations)
        {
            builder.addAll(wrapped.getAnnotations());
        }
        annotations = builder.add(additionalAnnotations).build();
    }

    
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType)
    {
        return annotations.stream().anyMatch((annotation) -> annotationType.isInstance(annotation));
    }
    

    interface Exclusions
    {
        boolean isAnnotationPresent(Class<? extends Annotation> annotationType);
        boolean getAnnotations();
    }

    
    private abstract class AT implements AnnotatedType<T> { }
    private final @Delegate(types = { AT.class, Annotated.class }, 
            excludes = Exclusions.class) AnnotatedType<T> wrapped;
    private final @Getter Set<Annotation> annotations;
}
