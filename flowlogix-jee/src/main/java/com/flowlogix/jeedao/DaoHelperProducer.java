/*
 * Copyright (C) 2011-2026 Flow Logix, Inc. All Rights Reserved.
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
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import com.flowlogix.api.dao.JPAFinder;
import lombok.NoArgsConstructor;
import static com.flowlogix.jeedao.DaoHelper.findEntityManager;

/**
 * Enables CDI Injection of {@link JPAFinder} instances
 */
@Dependent
@SuppressWarnings("HideUtilityClassConstructor")
@NoArgsConstructor
class DaoHelperProducer {
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
        return doProduceDaoHelper(injectionPoint, Arrays.asList(selector.value()));
    }

    private static <TT> DaoHelper<TT>
    doProduceDaoHelper(InjectionPoint injectionPoint, List<Class<? extends Annotation>> qualifiers) {
        var entityManagerSupplier = findEntityManager(qualifiers);
        var parameterizedType = (ParameterizedType) injectionPoint.getType();
        @SuppressWarnings("unchecked")
        var entityClass = (Class<TT>) parameterizedType.getActualTypeArguments()[0];
        return new DaoHelper<>(entityManagerSupplier, entityClass);
    }
}
