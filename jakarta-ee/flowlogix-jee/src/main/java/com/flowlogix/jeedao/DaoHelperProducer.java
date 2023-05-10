/*
 * Copyright (C) 2011-2023 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.jeedao;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.persistence.EntityManager;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.omnifaces.util.Beans;

/**
 * Enables CDI Injection of DaoHelper
 */
@Dependent
@SuppressWarnings("HideUtilityClassConstructor")
public class DaoHelperProducer {
    @Produces
    public static <TT> DaoHelper<TT>
    produceDaoHelper(InjectionPoint injectionPoint) {
        return doProduceDaoHelper(injectionPoint, List.of());
    }

    @Produces
    @EntityManagerSelector(Any.class)
    public static <TT> DaoHelper<TT>
    produceDaoHelperEntityManagerSelector(InjectionPoint injectionPoint) {
        var selector = injectionPoint.getQualifiers().stream()
                .filter(c -> c.annotationType().isAssignableFrom(EntityManagerSelector.class))
                .map(EntityManagerSelector.class::cast).findFirst().get();
        var qualifiers = Arrays.stream(selector.value()).map(value -> (Annotation) () -> value).toList();
        return doProduceDaoHelper(injectionPoint, qualifiers);
    }

    /**
     * Finds a reference to entity manager via CDI
     *
     * @param qualifiers for the entiry manager, or empty list
     * @return {@link EntityManager}
     * @throws IllegalStateException if entity manager cannot be found
     */
    public static EntityManager findEntityManager(List<Annotation> qualifiers) throws IllegalStateException {
        return Optional.ofNullable(Beans.getReference(EntityManager.class,
                qualifiers.toArray(Annotation[]::new))).orElseThrow(() -> new IllegalStateException(
                String.format("Unable to find EntityManager with qualifiers: %s",
                        qualifiers.stream().map(Annotation::annotationType).toList())));
    }

    private static <TT> DaoHelper<TT>
    doProduceDaoHelper(InjectionPoint injectionPoint, List<Annotation> qualifiers) {
        var entityManager = findEntityManager(qualifiers);
        var parameterizedType = (ParameterizedType) injectionPoint.getType();
        @SuppressWarnings("unchecked")
        var entityClass = (Class<TT>) parameterizedType.getActualTypeArguments()[0];
        return new DaoHelper<>(() -> entityManager, entityClass);
    }
}
