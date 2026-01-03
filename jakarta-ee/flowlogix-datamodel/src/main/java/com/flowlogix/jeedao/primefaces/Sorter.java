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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import lombok.Getter;
import org.primefaces.model.SortMeta;

/**
 * Sorter Hook
 * @param <TT> Entity Type
 */
@FunctionalInterface
public interface Sorter<TT> {
    /**
     * return type for the sorter hook
     *
     * @author lprimak
     */
    @Getter
    class SortData {
        /**
         * Sort based on fields
         */
        private final Map<String, SortMeta> sortMeta;
        /**
         * global sort order added by the client
         */
        private final List<Order> sortOrder = new ArrayList<>();

        public SortData(Map<String, SortMeta> sm) {
            sortMeta = new HashMap<>(sm);
        }
    }

    /**
     * Hook for sort criteria application
     * can remove elements from the SortMeta lists and do your own action
     * any elements left in SortMeta will be done via the default mechanism
     *
     * @param sortData
     * @param cb
     * @param root
     * @return true if sort by the UI (default) or application-based sort (false)
     */
    boolean sort(SortData sortData, CriteriaBuilder cb, Root<TT> root);
}
