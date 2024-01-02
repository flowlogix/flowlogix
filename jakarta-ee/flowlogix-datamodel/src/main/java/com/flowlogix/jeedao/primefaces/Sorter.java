/*
 * Copyright (C) 2011-2024 Flow Logix, Inc. All Rights Reserved.
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
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.primefaces.model.SortMeta;

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
        private final Map<String, MergedSortOrder> sortData;

        public SortData(Map<String, SortMeta> sm) {
            sortData = sm.values().stream().sorted()
                    .collect(Collectors.toMap(SortMeta::getField, value ->
                                    new MergedSortOrder(value, null, false),
                            (v1, v2) -> v1, LinkedHashMap::new));
        }

        /**
         * Replaces, or adds application sort criteria to the existing UI sort criteria
         * If the sort criteria is new, it is placed at the lowest sort order
         *
         * @param fieldName element to be replaced or added
         * @param fp        lambda to get the application sort criteria
         */
        public void applicationSort(String fieldName, Function<Optional<SortMeta>, Order> fp) {
            applicationSort(fieldName, false, fp);
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
         */
        public void applicationSort(String fieldName, boolean highPriority,
                                    Function<Optional<SortMeta>, Order> fp) {
            getSortData().compute(fieldName, (key, value) -> new MergedSortOrder(null,
                    Objects.requireNonNull(fp.apply(Optional.ofNullable(value != null
                                    ? value.getRequestedSortMeta() : null)),
                            "Sort Criteria cannot be null"), highPriority));
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
     * from the {@link SortData#sortData} map and add your own sort order
     * via {@link SortData#applicationSort(String, Function)} method
     *
     * @param sortData merged sort criteria
     * @param cb {@link CriteriaBuilder}
     * @param root {@link Root}
     */
    void sort(SortData sortData, CriteriaBuilder cb, Root<TT> root);
}
