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
package com.flowlogix.jeedao.primefaces;

import com.flowlogix.jeedao.EntityManagerSelector;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.stream.Stream;

/**
 * CDI Injection Support, Do not use directly
 * @hidden
 */
@Dependent
@SuppressWarnings("HideUtilityClassConstructor")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
class DataModelProducer {
    @Produces
    @Default
    @LazyModelConfig
    @EntityManagerSelector(Any.class)
    static <TT> JPALazyDataModel<TT> produceDataModelWithConfig(InjectionPoint injectionPoint) {
        var parameterizedType = (ParameterizedType) injectionPoint.getType();
        @SuppressWarnings("unchecked")
        var entityClass = (Class<TT>) parameterizedType.getActualTypeArguments()[0];
        var config = injectionPoint.getQualifiers().stream()
                .filter(c -> c.annotationType().isAssignableFrom(LazyModelConfig.class))
                .map(LazyModelConfig.class::cast).findFirst().orElse(null);
        var selector = injectionPoint.getQualifiers().stream()
                .filter(c -> c.annotationType().isAssignableFrom(EntityManagerSelector.class))
                .map(EntityManagerSelector.class::cast).findFirst().orElse(null);
        return new JPALazyDataModel<TT>().partialInitialize(builder -> {
            builder.entityClass(entityClass);
            if (config != null) {
                builder.caseSensitiveFilter(!config.caseInsensitive());
                builder.filterCaseConversion(config.filterCaseConversion());
                builder.entityManagerQualifiers(selector == null ? List.of(config.entityManagerSelector())
                        : Stream.concat(Stream.of(config.entityManagerSelector()), Stream.of(selector.value())).toList());
                builder.wildcardSupport(config.wildcardSupport());
            }
        });
    }
}
