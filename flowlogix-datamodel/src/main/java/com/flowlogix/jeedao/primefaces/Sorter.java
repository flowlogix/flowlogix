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

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.primefaces.model.SortMeta;
import static org.primefaces.model.SortOrder.UNSORTED;

/**
 * Sorter Hook
 * @param <TT> Entity Type
 * <p>
 * <em>Example:</em>
 * {@snippet class = "com.flowlogix.demo.jeedao.primefaces.SortingDataModel" region = "sorting"}
 */
@FunctionalInterface
public interface Sorter<TT> {
    /**
     * Manipulates sort criteria requested from the UI and possibly adds
     * or replaces it with application-based sort criteria
     * <p>
     * <em>Example:</em>
     * {@snippet class = "com.flowlogix.demo.jeedao.primefaces.SortingDataModel" region = "sorting"}
     *
     * @author lprimak
     */
    class SortData {
        /**
         * Sort based on fields, the map key is equivalent to {@link SortMeta#getField()}
         */
        @Getter
        private final Map<String, MergedSortOrder> sortOrder = new LinkedHashMap<>();

        public SortData(Map<String, SortMeta> sm) {
            sm.values().stream().sorted().forEach(value -> sortOrder.put(value.getField(),
                    new MergedSortOrder(value, null, false)));
        }

        /**
         * Replaces, or adds application sort criteria to the existing UI sort criteria
         * If the sort criteria is new, it is placed at the lowest sort order
         *
         * @param fieldName element to be replaced or added
         * @param fp        lambda to get the application sort criteria
         * @return this     fluent API
         */
        public SortData applicationSort(String fieldName, Function<Optional<SortMeta>, Order> fp) {
            return applicationSort(fieldName, false, fp);
        }

        /**
         * Replaces, or adds application sort criteria to the existing UI sort criteria
         * If the sort criteria is new, it is placed at either highest or lowest order,
         * depending on the highPriority parameter
         *
         * @param fieldName    field to be replaced or added
         * @param highPriority integer (starting with zero, highest priority)
         *                     where this sort directive is put into the array,
         *                     only if it's inserted, and not modified
         * @param fp           lambda to get the application sort criteria
         * @return this     fluent API
         */
        public SortData applicationSort(String fieldName, boolean highPriority,
                                        Function<Optional<SortMeta>, Order> fp) {
            getSortOrder().compute(fieldName, (key, value) -> new MergedSortOrder(null,
                    Objects.requireNonNull(fp.apply(Optional.ofNullable(value != null
                                    ? value.getRequestedSortMeta() : null)),
                            "Sort Criteria cannot be null"), highPriority));
            return this;
        }

        /**
         *  Helper method to apply application sort criteria based on the UI requested sort criteria
         *
         * @param sourceFieldName field to retrieve UI requested sort criteria from
         * @param fieldName     field to be replaced or added
         * @param sortData      current sort data
         * @param ascFn         lambda to get ascending sort criteria
         * @param descFn        lambda to get descending sort criteria
         * @param notFoundFn    lambda to get sort criteria when UI did not request sorting
         * @return this         fluent API
         */
        public SortData applicationSort(String sourceFieldName, String fieldName, SortData sortData,
                                             Supplier<Order> ascFn, Supplier<Order> descFn, Supplier<Order> notFoundFn) {
            return applicationSort(sourceFieldName, fieldName, sortData, false, ascFn, descFn, notFoundFn);
        }

        /**
         *  Helper method to apply application sort criteria based on the UI requested sort criteria
         *
         * @param sourceFieldName field to retrieve UI requested sort criteria from
         * @param fieldName     field to be replaced or added
         * @param sortData      current sort data
         * @param highPriority  high priority flag, see {@link #applicationSort(String, boolean, Function)}
         * @param ascFn         lambda to get ascending sort criteria
         * @param descFn        lambda to get descending sort criteria
         * @param notFoundFn    lambda to get sort criteria when UI did not request sorting
         * @return this         fluent API
         */
        public SortData applicationSort(String sourceFieldName, String fieldName, SortData sortData, boolean highPriority,
                                        Supplier<Order> ascFn, Supplier<Order> descFn, Supplier<Order> notFoundFn) {
            Order order = switch (Optional.ofNullable(sortData.getSortOrder().get(sourceFieldName))
                    .map(MergedSortOrder::getRequestedSortMeta).map(SortMeta::getOrder)
                    .orElse(UNSORTED)) {
                case ASCENDING -> ascFn.get();
                case DESCENDING -> descFn.get();
                case UNSORTED -> notFoundFn.get();
            };
            if (order != null) {
                applicationSort(fieldName, highPriority, var -> order);
            }
            return this;
        }
    }

    /**
     * Sort order requested by the UI lives in {@link #requestedSortMeta},
     * Sort order requested by the application lives in {@link #applicationSort}
     * Only one can exist, the other will always be null.
     */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @SuppressWarnings("DeclarationOrder")
    class MergedSortOrder {
        private final SortMeta requestedSortMeta;
        private final Order applicationSort;
        private final boolean highPriority;
    }

    /**
     * Hook for sort criteria manipulation. Application can inspect or remove elements
     * from the {@link SortData#sortOrder} map and add your own sort order
     * via {@link SortData#applicationSort(String, Function)} method
     *
     * @param sortData merged sort criteria
     * @param cb {@link CriteriaBuilder}
     * @param root {@link Root}
     */
    void sort(SortData sortData, CriteriaBuilder cb, Root<TT> root);
}
