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
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Builder;
import lombok.Generated;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static com.flowlogix.jeedao.primefaces.CursorPagination.requestedSort;
import static com.flowlogix.jeedao.primefaces.internal.JPAModelImpl.resolveField0;

/// Interface defining cursor pagination behavior for PrimeFaces JPA LazyDataModel
/// @param <TT> Entity type for pagination
public interface CursorPagination<TT> extends Serializable {
    /// Checks if cursor pagination is supported for the given filters and sort metadata,
    /// this method is called before every query execution and should return quickly.
    /// Implementations can use this method to detect changes in filters or sort order
    /// and clear cached cursor state as needed
    /// @param filters the current filter metadata,
    /// used to determine if cursor pagination is still valid based on supported filters
    /// @param sortMeta the current sort metadata,
    /// used to determine if cursor pagination is still valid based on supported sort fields and order
    /// @return true if cursor pagination is supported and can be applied to the query with the
    /// given filters and sort metadata, false if cursor pagination should not
    boolean isSupported(Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta);

    /// Returns columns that are supported for cursor pagination.
    /// Implementation must preserve the order of the columns, as the default sort is applied to the first column.
    /// @return a map of column names to functions that extract the comparable value from the entity for that column.
    Map<String, SerializableFunction<TT, Comparable<?>>> columns();

    /// Calculate the adjusted offset based on cached cursor state
    /// @return the adjusted offset to be used in the query based on the cached cursor state
    /// @param offset the original offset requested by the client
    int cursorOffset(int offset);

    /// Compute the JPA predicate to apply to the query for cursor pagination based on the cached cursor state
    /// @param offset the original offset requested by the client
    /// @param cb the CriteriaBuilder to use for constructing the predicate
    /// @param root the Root of the query, used to resolve the field for the predicate
    /// @param sortMeta the current sort metadata, used to determine which field is being sorted on for predicate construction
    /// @return the JPA Predicate to apply to the query for cursor pagination, or null if no predicate is needed
    Predicate cursorPredicate(int offset, CriteriaBuilder cb, Root<TT> root,
                              Map<String, SortMeta> sortMeta);

    /// Caches the cursor value for the given offset and entity
    /// @param offset the offset that was requested by the client
    /// @param entity the entity being loaded at that offset, used to extract the cursor value
    /// @param sortMeta the current sort metadata, used to determine which field is being sorted on for cursor extraction
    void save(int offset, TT entity, Map<String, SortMeta> sortMeta);

    /// Creates a JPA sort {@link Order} to apply to the query when no explicit sort is requested by the client,
    /// based on the first supported column and configured sort direction
    /// @param cb the CriteriaBuilder to use for constructing the Order
    /// @param root the Root of the query, used to resolve the field for the Order
    /// @return the JPA {@link Order} to apply to the query for default sorting when no explicit sort is requested by the client
    Order defaultSort(CriteriaBuilder cb, Root<TT> root);

    /// Used to configure columns for cursor pagination.
    /// First column is always used by default if no sort order is specified by the client,
    /// so it is recommended to put the most stable and unique column first (e.g. id or createdDate).
    /// Any fields specified should be indexed, so cursor pagination can be most efficient.
    /// @param fieldName the name of the column, used to match against requested sort fields from the client
    /// @param fieldMethod a function that extracts the comparable value from the entity for that field,
    /// used for caching cursor state and constructing predicates
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

    /// Returns a no-op implementation of CursorPagination that can be used when cursor pagination is not supported or desired.
    /// This is the default behavior of {@link JPALazyDataModel}
    /// @return a Lazy containing a no-op implementation of CursorPagination
    static <TT> Lazy<CursorPagination<TT>> noop() {
        return new Lazy<>(NoopCursorData::new);
    }

    /// Configuration class for cursor pagination, used to create a CursorPagination instance
    /// with specified supported fields and options.
    /// The following are configuration fields:
    /// * `supportedFields`: a list of fields that are supported for cursor pagination,
    /// each field consists of a name and a method to extract the comparable value from the entity
    /// * `conditional`: a supplier that determines whether cursor pagination should be enabled based on dynamic conditions,
    /// default is always true
    /// * `defaultDescendingSort`: a flag to indicate if the default sort order for the first supported field
    /// should be descending, default is false (ascending)
    /// * `evictCursorCacheBehind`: a flag to indicate whether to evict the cursor cache behind the current offset,
    /// defined by a window
    /// * `evictCursorCacheAhead`: a flag to indicate whether to evict the cursor cache ahead the current offset,
    /// defined by a window
    /// * `behindCursorWindowSize`: specified how wide the window behind the current offset should be
    /// for cursor cache eviction, default is 1000
    /// * `aheadCursorWindowSize`: specified how wide the window ahead of the current offset should be
    /// for cursor cache eviction, default is 1000
    @Builder
    class Config<TT> implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /**
         * config builder, just for javadoc
         * @hidden
         * @param <TT>
         */
        public static class ConfigBuilder<TT> { }

        private @NonNull List<Field<TT>> supportedFields;
        @NonNull @Builder.Default
        private Lazy.SerializableSupplier<Boolean> conditional = () -> true;
        private boolean defaultDescendingSort;
        private boolean evictCursorCacheBehind;
        private boolean evictCursorCacheAhead;
        @SuppressWarnings("checkstyle:MagicNumber")
        private @Builder.Default int behindCursorWindowSize = 1_000;
        @SuppressWarnings("checkstyle:MagicNumber")
        private @Builder.Default int aheadCursorWindowSize = 1_000;
    }

    /// Creates a default implementation of cursor pagination
    /// {@snippet class = "com.flowlogix.demo.jeedao.primefaces.CursorDataModel" region = "cursor"}
    /// @param configConsumer a function that accepts a ConfigBuilder and returns a {@link Config}
    /// with the desired configuration for cursor pagination
    /// @return a Lazy containing a CursorPagination instance configured via provided {@link Config}
    static <TT> Lazy<CursorPagination<TT>> create(Function<Config.ConfigBuilder<TT>, Config<TT>> configConsumer) {
        var config = configConsumer.apply(new Config.ConfigBuilder<>());
        return new Lazy<>(() -> config.conditional.get()
                ? new CursorData<>(config.supportedFields, config.defaultDescendingSort,
                config.evictCursorCacheBehind, config.evictCursorCacheAhead,
                config.behindCursorWindowSize, config.aheadCursorWindowSize)
                : new NoopCursorData<>());
    }

    /// Calculates the requested sort field from the sort metadata and supported columns,
    /// with an option to fall back to the default column if no valid sort is requested.
    /// @param sortMeta the current sort metadata, used to determine which field is being sorted on by the client
    /// @param columns the supported columns for cursor pagination,
    /// used to validate the requested sort field and determine the default field
    /// @param useDefault flag to indicate whether to return the default column (first supported column)
    /// if no valid sort field is requested by the client
    /// @return the name of the requested sort field if it is valid and supported
    /// otherwise the default column if useDefault is true,
    /// or an empty string if no valid sort field is requested and useDefault is false
    static <TT> String requestedSort(Map<String, SortMeta> sortMeta,
                                     Map<String, SerializableFunction<TT, Comparable<?>>> columns,
                                     boolean useDefault) {
        return sortMeta.keySet().stream().findFirst().filter(columns.keySet()::contains)
                .orElseGet(() -> useDefault ? columns.keySet().stream().findFirst().get() : "");
    }
}

/// Default implementation of cursor pagination
@Slf4j
class CursorData<TT> implements CursorPagination<TT> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, SerializableFunction<TT, Comparable<?>>> columns;
    private final boolean isDescendingDefault;
    private final boolean evictCursorCacheBehind;
    private final boolean evictCursorCacheAhead;
    private final int behindCursorWindowSize;
    private final int aheadCursorWindowSize;

    final NavigableMap<Integer, Comparable<?>> cursorCache = new TreeMap<>();
    private Map<String, FilterMeta> cursorFilters;
    private Map<String, SortMeta> cursorSorts;
    private boolean isDescendingState;

    CursorData(List<Field<TT>> columns, boolean isDescendingDefault,
               boolean evictCursorCacheBehind, boolean evictCursorCacheAhead,
               int behindCursorWindowSize, int aheadCursorWindowSize) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Cursor Pagination requires at least one column");
        }
        this.columns = Collections.unmodifiableMap(columns.stream()
                .collect(Collectors.toMap(Field::fieldName, Field::fieldMethod,
                (v1, v2) -> v1,
                LinkedHashMap::new)));
        this.isDescendingDefault = isDescendingDefault;
        this.evictCursorCacheBehind = evictCursorCacheBehind;
        this.evictCursorCacheAhead = evictCursorCacheAhead;
        this.behindCursorWindowSize = behindCursorWindowSize;
        this.aheadCursorWindowSize = aheadCursorWindowSize;
    }

    @Override
    public Map<String, SerializableFunction<TT, Comparable<?>>> columns() {
        return columns;
    }

    public void save(int offset, TT entity, Map<String, SortMeta> sortMeta) {
        var value = columns().get(requestedSort(sortMeta, columns, true)).apply(entity);
        log.debug("Saving cursor for offset {} and entity id {}", offset, value);
        cursorCache.put(offset, value);
        if (evictCursorCacheBehind) {
            // clear too far behind of the current offset
            cursorCache.headMap(Math.max(0, offset - behindCursorWindowSize)).clear();
        }
        if (evictCursorCacheAhead) {
            // clear too far ahead of the current offset
            cursorCache.tailMap(offset + aheadCursorWindowSize + 1).clear();
        }
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
        initializeFilters(filters, sortMeta);
        if (criteriaChanged(filters, sortMeta)) {
            return false;
        }
        var requestedSort = requestedSort(sortMeta, columns(), false);
        if (!sortMeta.isEmpty() && requestedSort.isEmpty()) {
            log.atDebug().setMessage("Cursor pagination only supports sorting by {} columns, requested sort: {}")
                            .addArgument(columns()::keySet).addArgument(sortMeta::keySet).log();
            cursorCache.clear();
            return false;
        } else if (isDescendingState != Optional.ofNullable(sortMeta.get(requestedSort))
                .map(sort -> sort.getOrder().isDescending()).orElse(false)) {
            isDescendingState = !isDescendingState;
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
                .orElse(isDescendingDefault);
        return Optional.ofNullable(floor).map(entry -> descending
                        ? cb.lessThan(resolveField0(root, currentColumn), (Comparable) entry.getValue())
                        : cb.greaterThan(resolveField0(root, currentColumn), (Comparable) entry.getValue())).orElse(null);
    }

    @Override
    public Order defaultSort(CriteriaBuilder cb, Root<TT> root) {
        var sortField = requestedSort(Map.of(), columns(), true);
        return isDescendingDefault ? cb.desc(resolveField0(root, sortField)) : cb.asc(resolveField0(root, sortField));
    }

    static Supplier<Comparable<?>> valueForLogging(Map.Entry<Integer, Comparable<?>> entry) {
        return () -> Optional.ofNullable(entry).map(Map.Entry::getValue).orElse(null);
    }

    private void initializeFilters(Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        if (cursorFilters == null) {
            cursorFilters = filters;
            checkUnexpectedInitializationState();
            cursorSorts = sortMeta;
        }
    }

    @Generated
    private void checkUnexpectedInitializationState() {
        if (this.cursorSorts != null) {
            throw new IllegalStateException("Cursor filters were not initialized, but sorts were - this should never happen");
        }
    }

    private boolean criteriaChanged(Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        if (!cursorFilters.equals(filters) || !cursorSorts.equals(sortMeta)) {
            cursorFilters = filters;
            cursorSorts = sortMeta;
            cursorCache.clear();
            return true;
        }
        return false;
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
    public Order defaultSort(CriteriaBuilder cb, Root<TT> root) {
        throw new UnsupportedOperationException("NoopCursorData does not support default sorting");
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
