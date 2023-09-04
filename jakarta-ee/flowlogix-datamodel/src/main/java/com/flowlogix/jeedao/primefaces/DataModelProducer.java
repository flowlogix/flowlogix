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
package com.flowlogix.jeedao.primefaces;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.ParameterizedType;
import java.util.List;

@Dependent
@SuppressWarnings("HideUtilityClassConstructor")
public class DataModelProducer {
    @Produces
    public static <TT, KK> JPALazyDataModel<TT, KK> produceDataModel(InjectionPoint injectionPoint) {
        return produceDataModelWithConfig(injectionPoint);
    }

    @Produces
    @LazyModelConfig
    public static <TT, KK> JPALazyDataModel<TT, KK> produceDataModelWithConfig(InjectionPoint injectionPoint) {
        var parameterizedType = (ParameterizedType) injectionPoint.getType();
        @SuppressWarnings("unchecked")
        var entityClass = (Class<TT>) parameterizedType.getActualTypeArguments()[0];
        var optionalConfig = injectionPoint.getQualifiers().stream()
                .filter(c -> c.annotationType().isAssignableFrom(LazyModelConfig.class))
                .map(LazyModelConfig.class::cast).findFirst();
        return new JPALazyDataModel<TT, KK>().initialize(builder -> {
            builder.entityClass(entityClass);
            optionalConfig.ifPresent(config -> {
                builder.caseSensitiveFilter(!config.caseInsensitive());
                builder.entityManagerQualifiers(List.of(config.entityManagerSelector()));
            });
            return builder.build();
        });
    }
}
