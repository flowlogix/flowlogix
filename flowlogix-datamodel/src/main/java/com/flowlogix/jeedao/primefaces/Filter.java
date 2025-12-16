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

import java.util.Map;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.function.BiFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Filter Hook
 * <p>
 * <em>Example:</em>
 * {@snippet class = "com.flowlogix.demo.jeedao.primefaces.FilteringDataModel" region = "filtering"}
 *
 * @param <TT> Entity Type
 */
@FunctionalInterface
public interface Filter<TT> {
    /**
     * Interface that directly inherits from {@link Map} and adds
     * {@link #replaceFilter(String, BiFunction)} method
     */
    interface FilterData extends Map<String, FilterColumnData> {
        /**
         * Replacing a predicate in the filter list by field name
         *
         * @param <TT> type of value
         * @param fieldName element to be replace
         * @param fp lambda to get the new Filter predicate
         * @return true if succeeded
         */
        @SuppressWarnings("unchecked")
        default <TT> boolean replaceFilter(String fieldName, BiFunction<Predicate, TT, Predicate> fp) {
            FilterColumnData elt = get(fieldName);
            if (elt != null && elt.getFilterValue() != null) {
                return replace(fieldName, new FilterColumnData(elt.getFilterValue(),
                        fp.apply(elt.getPredicate(), (TT) elt.getFilterValue()))) != null;
            }
            return false;
        }
    }

    /**
     * filter data this is what you replace with your own filter
     */
    @RequiredArgsConstructor
    @Getter
    class FilterColumnData {
        /**
         * filter field value
         */
        private final Object filterValue;
        /**
         * Existing or null predicate, can replace with custom
         */
        private final Predicate predicate;
    }

    /**
     * hook to supply custom filter
     *
     * @param filterData user input
     * @param cb
     * @param root
     */
    void filter(FilterData filterData, CriteriaBuilder cb, Root<TT> root);
}
