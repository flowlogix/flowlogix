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
package com.flowlogix.shiro.ee.cdi;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        Stream.Builder<Annotation> builder = Stream.builder();
        if(keepOriginalAnnotations)
        {
            wrapped.getAnnotations().forEach(builder::add);
        }
        Stream.of(additionalAnnotations).forEach(builder::add);
        annotations = builder.build().collect(Collectors.toSet());
    }


    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType)
    {
        return annotations.stream().anyMatch(annotation -> annotationType.isInstance(annotation));
    }


    private final @Delegate(types = AnnotatedType.class) AnnotatedType<T> wrapped;
    private final @Getter Set<Annotation> annotations;
}
