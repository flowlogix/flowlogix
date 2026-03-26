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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.omnifaces.util.Lazy;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/// Interface defining cursor pagination behavior for PrimeFaces JPA LazyDataModel
public interface CursorPagination<TT> extends Serializable {
    void save(int offset, TT entity);
    int cursorOffset(int offset);
    Predicate cursorPredicate(int offset, CriteriaBuilder cb, Root<TT> root,
                              Map<String, SortMeta> sortMeta);
    boolean isSupported(Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta);
    Map<String, Function<TT, Comparable<?>>> columns();
    void setCurrentColumn(@NonNull String columnName);

    /// Returns a no-op implementation of CursorPagination that can be used when cursor pagination is not supported or desired
    static <TT> CursorPagination<TT> noop() {
        return new NoopCursorData<>();
    }

    /// Creates a default implementation of cursor pagination
    static <TT> CursorPagination<TT> create(Map<String, Function<TT, Comparable<?>>> supportedColumns) {
        return new CursorData<>(() -> supportedColumns);
    }
}

@Slf4j
@RequiredArgsConstructor
class CursorData<TT> implements CursorPagination<TT> {
    @Serial
    private static final long serialVersionUID = 2L;

    private final Lazy.SerializableSupplier<Map<String, Function<TT, Comparable<?>>>> columns;
    private final NavigableMap<Integer, Comparable<?>> cursorCache = new TreeMap<>();
    private Map<String, FilterMeta> cursorFilters;
    private Map<String, SortMeta> cursorSorts;
    @Setter(onMethod = @__(@NonNull))
    private String currentColumn;
    private boolean isDescending;

    @Override
    public Map<String, Function<TT, Comparable<?>>> columns() {
        return columns.get();
    }

    public void save(int offset, TT entity) {
        Objects.requireNonNull(currentColumn, "Current column must be set before saving cursor");
        var value = columns().get(currentColumn).apply(entity);
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
            log.atDebug().setMessage("Cursor pagination only supports sorting by {} columns, requested sort: {}")
                            .addArgument(columns()::keySet).addArgument(sortMeta::keySet).log();
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
        Objects.requireNonNull(currentColumn, "Current column must be set before creating cursor predicate");
        var floor = cursorCache.floorEntry(offset);
        log.atDebug().setMessage("Creating cursor predicate for offset {} - cache = {}")
                .addArgument(offset).addArgument(() -> Optional.ofNullable(floor).map(Map.Entry::getValue)
                        .orElse(null)).log();
        boolean descending = Optional.ofNullable(sortMeta.get(currentColumn))
                .map(order -> order.getOrder().isDescending())
                .orElse(false);
        return Optional.ofNullable(floor).map(entry -> descending
                        ? cb.lessThan(root.get(currentColumn), (Comparable) entry.getValue())
                        : cb.greaterThan(root.get(currentColumn), (Comparable) entry.getValue())).orElse(null);
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
    public Map<String, Function<TT, Comparable<?>>> columns() {
        return Collections.emptyMap();
    }

    @Override
    public void setCurrentColumn(@NonNull String columnName) {
        throw new UnsupportedOperationException("NoopCursorData does not support setting current column");
    }

    @Override
    public void save(int offset, TT entity) {
        throw new UnsupportedOperationException("NoopCursorData does not support saving cursor state");
    }
}
