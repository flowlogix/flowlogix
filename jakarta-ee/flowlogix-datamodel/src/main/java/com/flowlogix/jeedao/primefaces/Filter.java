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

import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Filter Hook
 *
 * @param <TT> Entity Type
 */
@FunctionalInterface
public interface Filter<TT> {
    /**
     * filter data this is what you replace with your own filter
     */
    @RequiredArgsConstructor
    @Getter
    class FilterData {
        /**
         * filter field value
         */
        private final Object fieldValue;
        /**
         * Existing or null predicate, can replace with custom
         */
        private final Predicate predicate;
    }

    /**
     * hook to supply custom filter
     *
     * @param filters user input
     * @param cb
     * @param root
     */
    void filter(Map<String, FilterData> filters, CriteriaBuilder cb, Root<TT> root);
}
