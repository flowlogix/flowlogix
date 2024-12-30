/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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

import com.flowlogix.jeedao.primefaces.JPALazyDataModel.FilterCaseConversion;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Optional Configuration annotation for {@link JPALazyDataModel}
 * Use to make queries case-insensitive and pick an {@link jakarta.persistence.EntityManager}
 */
@Qualifier
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyModelConfig {
    /**
     * case-sensitivity of queries
     */
    @Nonbinding
    boolean caseInsensitive() default false;

    /**
     * to which case (upper / lower) to convert during case-insensitive query
     */
    @Nonbinding
    FilterCaseConversion filterCaseConversion() default FilterCaseConversion.UPPER;

    /**
     * wildcard support for filter queries
     */
    @Nonbinding
    boolean wildcardSupport() default false;

    /**
     * Override entity manager for the data model
     */
    @Nonbinding
    Class<? extends Annotation> [] entityManagerSelector() default { };
}
