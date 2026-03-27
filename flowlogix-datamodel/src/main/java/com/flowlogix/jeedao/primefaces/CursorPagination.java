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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.omnifaces.util.FunctionalInterfaces.SerializableFunction;
import org.omnifaces.util.Lazy;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static com.flowlogix.jeedao.primefaces.CursorPagination.requestedSort;

/// Interface defining cursor pagination behavior for PrimeFaces JPA LazyDataModel
public interface CursorPagination<TT> extends Serializable {
    void save(int offset, TT entity, Map<String, SortMeta> sortMeta);
    int cursorOffset(int offset);
    Predicate cursorPredicate(int offset, CriteriaBuilder cb, Root<TT> root,
                              Map<String, SortMeta> sortMeta);
    boolean isSupported(Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta);
    Map<String, SerializableFunction<TT, Comparable<?>>> columns();

    record Field<TT>(String fieldName, SerializableFunction<TT, Comparable<?>> fieldMethod) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public Field {
            if (Objects.requireNonNull(fieldName, "fieldName cannot be null").isEmpty()) {
                throw new IllegalArgumentException("fieldName cannot be empty");
            }
            Objects.requireNonNull(fieldMethod, "fieldMethod cannot be null");
        }
    }

    /// Returns a no-op implementation of CursorPagination that can be used when cursor pagination is not supported or desired
    static <TT> Lazy<CursorPagination<TT>> noop() {
        return new Lazy<>(NoopCursorData::new);
    }

    /// Creates a default implementation of cursor pagination
    static <TT> Lazy<CursorPagination<TT>> create(List<Field<TT>> supportedColumns) {
        return new Lazy<>(() -> new CursorData<>(supportedColumns));
    }

    static <TT> Lazy<CursorPagination<TT>> create(List<Field<TT>> supportedColumns,
                                                  Lazy.SerializableSupplier<Boolean> conditional) {
        return new Lazy<>(() -> conditional.get() ? new CursorData<>(supportedColumns) : new NoopCursorData<>());
    }

    static <TT> String requestedSort(Map<String, SortMeta> sortMeta,
                                     Map<String, SerializableFunction<TT, Comparable<?>>> columns,
                                     boolean useDefault) {
        return sortMeta.keySet().stream().findFirst().filter(columns.keySet()::contains)
                .orElseGet(() -> useDefault ? columns.keySet().stream().findFirst().get() : "");
    }
}

@Slf4j
class CursorData<TT> implements CursorPagination<TT> {
    @Serial
    private static final long serialVersionUID = 3L;

    private final Map<String, SerializableFunction<TT, Comparable<?>>> columns;
    private final NavigableMap<Integer, Comparable<?>> cursorCache = new TreeMap<>();
    private Map<String, FilterMeta> cursorFilters;
    private Map<String, SortMeta> cursorSorts;
    private boolean isDescending;

    CursorData(List<Field<TT>> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Cursor Pagination requires at least one column");
        }
        this.columns = columns.stream().collect(Collectors.toMap(Field::fieldName, Field::fieldMethod,
                (v1, v2) -> v1,
                LinkedHashMap::new));
    }

    @Override
    public Map<String, SerializableFunction<TT, Comparable<?>>> columns() {
        return columns;
    }

    public void save(int offset, TT entity, Map<String, SortMeta> sortMeta) {
        var value = columns().get(requestedSort(sortMeta, columns, true)).apply(entity);
        log.debug("Saving cursor for offset {} and entity id {}", offset, value);
        cursorCache.put(offset, value);
    }

    @Override
    public int cursorOffset(int offset) {
        var floor = cursorCache.floorKey(offset);
        int returnedOffset = Optional.ofNullable(floor).map(key -> offset - key).orElse(offset);
        log.debug("Cursor offset {}, floorKey = {} returned {}", offset, floor, returnedOffset);
        return returnedOffset;
    }

    @Override
    public boolean isSupported(@NonNull Map<String, FilterMeta> filters, @NonNull Map<String, SortMeta> sortMeta) {
        if (cursorFilters == null) {
            cursorFilters = filters;
            cursorSorts = sortMeta;
        }
        if (!cursorFilters.equals(filters) || !cursorSorts.equals(sortMeta)) {
            cursorFilters = filters;
            cursorSorts = sortMeta;
            cursorCache.clear();
            return false;
        }
        var requestedSort = requestedSort(sortMeta, columns(), false);
        if (!sortMeta.isEmpty() && requestedSort.isEmpty()) {
            log.atDebug().setMessage("Cursor pagination only supports sorting by {} columns, requested sort: {}")
                            .addArgument(columns()::keySet).addArgument(sortMeta::keySet).log();
            cursorCache.clear();
            return false;
        } else if (isDescending != Optional.ofNullable(sortMeta.get(requestedSort))
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
        var floor = cursorCache.floorEntry(offset);
        log.atDebug().setMessage("Creating cursor predicate for offset {} - cache = {}")
                .addArgument(offset).addArgument(valueForLogging(floor)).log();
        var currentColumn = requestedSort(sortMeta, columns(), true);
        boolean descending = Optional.ofNullable(sortMeta.get(currentColumn))
                .map(order -> order.getOrder().isDescending())
                .orElse(false);
        return Optional.ofNullable(floor).map(entry -> descending
                        ? cb.lessThan(root.get(currentColumn), (Comparable) entry.getValue())
                        : cb.greaterThan(root.get(currentColumn), (Comparable) entry.getValue())).orElse(null);
    }

    static Supplier<Comparable<?>> valueForLogging(Map.Entry<Integer, Comparable<?>> entry) {
        return () -> Optional.ofNullable(entry).map(Map.Entry::getValue).orElse(null);
    }
}

/// No-op implementation of cursor pagination, either not supported or disabled
class NoopCursorData<TT> implements CursorPagination<TT> {
    @Override
    public int cursorOffset(int offset) {
        return offset;
    }

    @Override
    public Predicate cursorPredicate(int offset, CriteriaBuilder cb, Root<TT> root, Map<String, SortMeta> sortMeta) {
        return null;
    }

    @Override
    public boolean isSupported(Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        return false;
    }

    @Override
    public Map<String, SerializableFunction<TT, Comparable<?>>> columns() {
        return Collections.emptyMap();
    }

    @Override
    public void save(int offset, TT entity, Map<String, SortMeta> sortMeta) {
        throw new UnsupportedOperationException("NoopCursorData does not support saving cursor state");
    }
}
