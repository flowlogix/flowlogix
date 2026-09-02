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

import com.flowlogix.jeedao.primefaces.CursorPagination.Field;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.MatchMode;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CursorPaginationTest {
    static class Entity { }
    CursorPagination<Entity> cursor = CursorPagination.<Entity>create(builder ->
            builder.supportedFields(List.of(new Field<>(() -> "id", e -> "one")))
                    .defaultDescendingSort(true).build()).get();

    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Root<Entity> root;

    @Test
    void save() {
        var rawCursor = CursorPagination.<Entity>create(builder ->
                builder.supportedFields(List.of(new Field<>(() -> "id", e -> "one"))).build()).get();
        rawCursor.save(2, new Entity(), Map.of());
        assertThat(rawCursor.cursorOffset(1)).isEqualTo(1);
        assertThat(rawCursor.cursorOffset(3)).isEqualTo(1);
        assertThat(rawCursor.cursorOffset(0)).isZero();
    }

    @Test
    void saveWithEvictionBehind() {
        var rawCursor = CursorPagination.<Entity>create(builder ->
                builder.supportedFields(List.of(new Field<>(() -> "id", e -> "one")))
                        .evictCursorCacheBehind(true).behindCursorWindowSize(-1).build()).get();
        rawCursor.save(2, new Entity(), Map.of());
        CursorData<Entity> data = (CursorData<Entity>) rawCursor;
        assertThat(data.cursorCache).isEmpty();
    }

    @Test
    void saveWithEvictionAhead() {
        var rawCursor = CursorPagination.<Entity>create(builder ->
                builder.supportedFields(List.of(new Field<>(() -> "id", e -> "one")))
                        .evictCursorCacheAhead(true).aheadCursorWindowSize(-1).build()).get();
        rawCursor.save(2, new Entity(), Map.of());
        CursorData<Entity> data = (CursorData<Entity>) rawCursor;
        assertThat(data.cursorCache).isEmpty();
    }

    @Test
    void createWithDefaultColumn() {
        assertThatThrownBy(() -> CursorPagination.create(builder -> builder
                .supportedFields(List.of()).build()).get()).isInstanceOf(IllegalArgumentException.class);
        CursorPagination.create(builder -> builder
                .supportedFields(List.of(new Field<>(() -> "hello", e -> "one"))).build()).get()
                .save(0, new Entity(), Map.of());
    }

    @Test
    void supportedWhenNoSortRequested() {
        assertThatThrownBy(() -> cursor.isSupported(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cursor.isSupported(Map.of(), null)).isInstanceOf(NullPointerException.class);
        assertThat(cursor.isSupported(Map.of(), Map.of())).isTrue();
        assertThat(cursor.isSupported(Map.of(), Map.of())).isTrue();
    }

    @Test
    void supportedWhenSortRequested() {
        assertThat(cursor.isSupported(Map.of(), Map.of())).isTrue();
        assertThat(cursor.isSupported(Map.of("id", FilterMeta.builder()
                .field("id").build()), Map.of())).isFalse();
        assertThat(cursor.isSupported(Map.of("id", FilterMeta.builder()
                .field("id").build()), Map.of("id", SortMeta.builder()
                .field("id").build()))).isFalse();
        assertThat(cursor.isSupported(Map.of("id", FilterMeta.builder()
                .field("id").build()), Map.of("id", SortMeta.builder()
                .field("id").build()))).isTrue();
    }

    @Test
    void unsupported() {
        assertThat(cursor.isSupported(Map.of(), Map.of("other", SortMeta.builder().field("other").build()))).isFalse();
    }

    @Test
    void descending() {
        assertThat(cursor.isSupported(Map.of(), Map.of("id", SortMeta.builder().field("id")
                .order(SortOrder.DESCENDING).build()))).isTrue();
        // sort order flip is a criteria change, cursor cache is discarded
        assertThat(cursor.isSupported(Map.of(), Map.of("id", SortMeta.builder().field("id")
                .build()))).isFalse();
        assertThat(cursor.isSupported(Map.of(), Map.of("id", SortMeta.builder().field("id")
                .build()))).isTrue();
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void predicate() {
        cursor.save(3, new Entity(), Map.of());
        assertThat(cursor.cursorPredicate(0, cb, root, Map.of("id", SortMeta.builder().field("id").build()))).isNull();
        assertThat(cursor.cursorPredicate(4, cb, root, Map.of("id", SortMeta.builder().field("id").build()))).isNull();
        verify(cb).greaterThan(root.get("id"), "one");

        assertThat(cursor.cursorPredicate(4, cb, root, Map.of("id", SortMeta.builder().field("id")
                .order(SortOrder.DESCENDING).build()))).isNull();
        verify(cb).lessThan(root.get("id"), "one");

        verify(root, times(6)).get("id");
        verifyNoMoreInteractions(cb, root);
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void predicateWithDuplicatesUsesTiebreaker() {
        record Row(int id, String lastName) { }
        var rowCursor = CursorPagination.<Row>create(builder -> builder.supportedFields(
                List.of(new Field<>(() -> "id", Row::id),
                        new Field<>(() -> "lastName", Row::lastName))).build()).get();
        var sort = Map.of("lastName", SortMeta.builder().field("lastName").build());
        rowCursor.save(3, new Row(7, "C"), sort);

        @SuppressWarnings("unchecked")
        Root<Row> rowRoot = mock(Root.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        assertThat(rowCursor.cursorPredicate(3, cb, rowRoot, sort)).isNull();
        verify(cb).greaterThan(rowRoot.get("lastName"), "C");
        verify(cb).equal(rowRoot.get("lastName"), "C");
        verify(cb).greaterThan(rowRoot.get("id"), 7);
        verify(cb).and(any(), any());
        verify(cb).or(any(), any());
        verify(rowRoot, times(3)).get("lastName");
        verify(rowRoot, times(2)).get("id");
        verifyNoMoreInteractions(cb, rowRoot);
    }

    @Test
    void defaultSort() {
        assertThat(cursor.defaultSort(cb, root)).containsExactly((Order) null);
        verify(cb).desc(root.get("id"));
        verify(root, times(2)).get("id");
        verifyNoMoreInteractions(cb, root);
    }

    @Test
    void tiebreakerSort() {
        assertThat(cursor.tiebreakerSort(cb, root)).isNull();
        verify(cb).asc(root.get("id"));
        verify(root, times(2)).get("id");
        verifyNoMoreInteractions(cb, root);
    }

    @Test
    void valueForLogging() {
        assertThat(CursorData.valueForLogging(null).get()).isNull();
    }

    @Test
    void cursorOffsetWithNoop() {
        var noop = CursorPagination.noop().get();
        assertThat(noop.cursorOffset(0)).isZero();
        assertThat(noop.cursorOffset(1)).isEqualTo(1);
    }

    @Test
    void cursorPredicateWithNoop() {
        var noop = CursorPagination.noop().get();
        assertThat(noop.cursorPredicate(0, null, null, null)).isNull();
        assertThat(noop.cursorPredicate(1, null, null, null)).isNull();
    }

    @Test
    void miscWithNoop() {
        var noop = CursorPagination.noop().get();
        assertThat(noop.columns()).isEmpty();
        assertThat(noop.isSupported(null, null)).isFalse();
        assertThatThrownBy(() -> noop.save(0, null, Map.of())).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> noop.defaultSort(null, null)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(noop.tiebreakerSort(null, null)).isNull();
    }

    @Test
    void duplicateColumn() {
        assertThat(CursorPagination.create(builder -> builder
                .supportedFields(List.of(new Field<>(() -> "id", e -> "one"),
                new Field<>(() -> "id", e -> "two"))).build()).get().columns()).hasSize(1);
    }

    @Test
    void emptyColumn() {
        assertThatThrownBy(() -> CursorPagination.create(builder -> builder
                .supportedFields(List.of(new Field<>(() -> "", e -> "one"))).build()).get().columns())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void filterValueChangeClearsCursorCache() {
        var rawCursor = CursorPagination.<Entity>create(builder ->
                builder.supportedFields(List.of(new Field<>(() -> "id", e -> "one"))).build()).get();
        assertThat(rawCursor.isSupported(Map.of("age", FilterMeta.builder()
                .field("age").filterValue(0).matchMode(MatchMode.GREATER_THAN).build()), Map.of())).isTrue();
        rawCursor.save(3, new Entity(), Map.of());
        CursorData<Entity> data = (CursorData<Entity>) rawCursor;
        assertThat(data.cursorCache).isNotEmpty();
        assertThat(rawCursor.isSupported(Map.of("age", FilterMeta.builder()
                .field("age").filterValue(90).matchMode(MatchMode.GREATER_THAN).build()), Map.of())).isFalse();
        assertThat(data.cursorCache).isEmpty();
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void filterMatchModeChangeClearsCursorCache() {
        var rawCursor = CursorPagination.<Entity>create(builder ->
                builder.supportedFields(List.of(new Field<>(() -> "id", e -> "one"))).build()).get();
        assertThat(rawCursor.isSupported(Map.of("age", FilterMeta.builder()
                .field("age").filterValue(5).matchMode(MatchMode.GREATER_THAN).build()), Map.of())).isTrue();
        rawCursor.save(3, new Entity(), Map.of());
        assertThat(rawCursor.isSupported(Map.of("age", FilterMeta.builder()
                .field("age").filterValue(5).matchMode(MatchMode.LESS_THAN).build()), Map.of())).isFalse();
        assertThat(((CursorData<Entity>) rawCursor).cursorCache).isEmpty();
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void inPlaceFilterMutationClearsCursorCache() {
        var rawCursor = CursorPagination.<Entity>create(builder ->
                builder.supportedFields(List.of(new Field<>(() -> "id", e -> "one"))).build()).get();
        var filterMeta = FilterMeta.builder()
                .field("age").filterValue(0).matchMode(MatchMode.GREATER_THAN).build();
        var filters = new HashMap<String, FilterMeta>();
        filters.put("age", filterMeta);
        assertThat(rawCursor.isSupported(filters, Map.of())).isTrue();
        rawCursor.save(3, new Entity(), Map.of());
        filterMeta.setFilterValue(90);
        assertThat(rawCursor.isSupported(filters, Map.of())).isFalse();
        assertThat(((CursorData<Entity>) rawCursor).cursorCache).isEmpty();
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void sortOrderChangeClearsCursorCache() {
        var rawCursor = CursorPagination.<Entity>create(builder ->
                builder.supportedFields(List.of(new Field<>(() -> "id", e -> "one"))).build()).get();
        assertThat(rawCursor.isSupported(Map.of(), Map.of("id", SortMeta.builder()
                .field("id").order(SortOrder.ASCENDING).build()))).isTrue();
        rawCursor.save(3, new Entity(), Map.of("id", SortMeta.builder()
                .field("id").order(SortOrder.ASCENDING).build()));
        assertThat(rawCursor.isSupported(Map.of(), Map.of("id", SortMeta.builder()
                .field("id").order(SortOrder.DESCENDING).build()))).isFalse();
        assertThat(((CursorData<Entity>) rawCursor).cursorCache).isEmpty();
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void pagesDoNotOverlapAfterFilterNarrowing() {
        record Row(int id, String lastName, int age) { }
        var rows = List.of(
                new Row(1, "A", 10), new Row(2, "B", 20), new Row(3, "C", 30),
                new Row(4, "D", 40), new Row(5, "E", 50), new Row(6, "F", 60),
                new Row(7, "G", 70), new Row(8, "H", 80), new Row(9, "I", 90),
                new Row(10, "J", 95));
        var cursorPagination = CursorPagination.<Row>create(builder -> builder.supportedFields(
                List.of(new Field<>(() -> "lastName", Row::lastName))).build()).get();
        var sort = Map.of("lastName", SortMeta.builder()
                .field("lastName").order(SortOrder.ASCENDING).build());

        // page forward with a wide filter
        var wideFilter = Map.of("age", FilterMeta.builder()
                .field("age").filterValue(0).matchMode(MatchMode.GREATER_THAN).build());
        assertThat(cursorPagination.isSupported(wideFilter, sort)).isTrue();
        var page1 = rows.stream().filter(row -> row.age() > 0).limit(3).toList();
        page1.forEach(row -> cursorPagination.save(rows.indexOf(row) + 1, row, sort));
        assertThat(cursorPagination.isSupported(wideFilter, sort)).isTrue();
        assertThat(cursorPagination.cursorOffset(3)).isZero();

        // narrow the filter, cursor cache must be discarded
        var narrowFilter = Map.of("age", FilterMeta.builder()
                .field("age").filterValue(90).matchMode(MatchMode.GREATER_THAN).build());
        assertThat(cursorPagination.isSupported(narrowFilter, sort)).isFalse();
        assertThat(cursorPagination.isSupported(narrowFilter, sort)).isTrue();
        var narrowed = rows.stream().filter(row -> row.age() > 90).toList();
        var narrowedPage1 = narrowed.subList(0, 1);
        narrowedPage1.forEach(row -> cursorPagination.save(1, row, sort));
        assertThat(cursorPagination.isSupported(narrowFilter, sort)).isTrue();
        // page 2 starts after the only matching row, so it must be empty
        var narrowedPage2 = narrowed.subList(Math.min(1 + cursorPagination.cursorOffset(1), narrowed.size()), narrowed.size());
        assertThat(narrowedPage2).isEmpty();
    }
}
