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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.omnifaces.util.Lazy;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/// TODO
public interface CursorPagination<TT> extends Serializable {
    void save(int offset, TT entity);
    int cursorOffset(int offset);
    Predicate cursorPredicate(int offset, CriteriaBuilder cb, Root<TT> root,
                                                                  Map<String, SortMeta> sortMeta);
    default boolean isSupported(Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        return false;
    }

    default Map<String, Function<TT, ?>> columns() {
        return Collections.emptyMap();
    }

    default void setCurrentColumn(String columnName) {

    }

    /// TODO
    static <TT> CursorPagination<TT> noop() {
        return new CursorPagination<TT>() {
            @Override
            public int cursorOffset(int offset) {
                return offset;
            }

            @Override
            public Predicate cursorPredicate(int offset, CriteriaBuilder cb, Root<TT> root, Map<String, SortMeta> sortMeta) {
                return null;
            }

            @Override
            public void save(int offset, TT entity) { }
        };
    }

    @Slf4j
    @RequiredArgsConstructor
    class CursorData<TT> implements CursorPagination<TT> {
        @Serial
        private static final long serialVersionUID = 2L;

        private final Lazy.SerializableSupplier<Map<String, Function<TT, ?>>> columns;
        private final NavigableMap<Integer, Object> cursorCache = new TreeMap<>();
        private Map<String, FilterMeta> cursorFilters = new LinkedHashMap<>();
        private Map<String, SortMeta> cursorSorts = new LinkedHashMap<>();
        @Setter
        private String currentColumn;
        private boolean isDescending;

        @Override
        public Map<String, Function<TT, ?>> columns() {
            return columns.get();
        }

        public void save(int offset, TT entity) {
            var value = columns().get(currentColumn).apply(entity);
            log.info("Saving cursor for offset {} and entity id {}", offset, value);
            cursorCache.put(offset, value);
        }

        @Override
        public int cursorOffset(int offset) {
            int returnedOffset = Optional.ofNullable(cursorCache.floorKey(offset))
                    .map(key -> offset - key).orElse(offset);
            log.info("Cursor offset {}, floorKey = {} returned {}", offset, cursorCache.floorKey(offset), returnedOffset);
            return returnedOffset;
        }

        @Override
        public boolean isSupported(Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
            if (cursorFilters == null || cursorSorts == null) {
                cursorFilters = filters;
                cursorSorts = sortMeta;
            }
            if (!cursorFilters.equals(filters) || !cursorSorts.equals(sortMeta)) {
                cursorFilters = filters;
                cursorSorts = sortMeta;
                cursorCache.clear();
                return false;
            }
            var requestedSort = sortMeta.keySet().stream().findFirst()
                    .filter(columns().keySet()::contains);
            requestedSort.ifPresent(this::setCurrentColumn);
            if (!sortMeta.isEmpty() && requestedSort.isEmpty()) {
                log.warn("Cursor pagination only supports sorting by {} columns, requested sort: {}",
                        columns().keySet(), sortMeta.keySet());
                cursorCache.clear();
                return false;
            } else if (isDescending != Optional.ofNullable(sortMeta.get(currentColumn))
                    .map(sort -> sort.getOrder().isDescending()).orElse(false)) {
                isDescending = !isDescending;
                cursorCache.clear();
            }

            return true;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Predicate cursorPredicate(int offset, CriteriaBuilder cb, Root<TT> root,
                                                                             Map<String, SortMeta> sortMeta) {
            log.info("Creating cursor predicate for offset {} - cache = {}", offset,
                    Optional.ofNullable(cursorCache.floorEntry(offset))
                            .map(Map.Entry::getValue).orElse(null));
            boolean descending = Optional.ofNullable(sortMeta.get(currentColumn))
                    .map(order -> order.getOrder().isDescending())
                    .orElse(false);
            return Optional.ofNullable(cursorCache.floorEntry(offset))
                    .map(entry -> descending
                            ? cb.lessThan(root.get(currentColumn), (Comparable) entry.getValue())
                            : cb.greaterThan(root.get(currentColumn), (Comparable) entry.getValue())).orElse(null);
        }
    }
}
