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
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CursorPaginationTest {
    static class Entity { }
    CursorPagination<Entity> cursor = CursorPagination.create(Map.of("id", e -> "one"));

    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Root<Entity> root;

    @BeforeEach
    void setUp() {
        cursor.setCurrentColumn("id");
    }

    @Test
    void save() {
        var rawCursor = CursorPagination.create(Map.of("id", e -> "one"));
        assertThatThrownBy(() -> rawCursor.save(0, new Entity())).isInstanceOf(NullPointerException.class);
        rawCursor.setCurrentColumn("id");
        rawCursor.save(2, new Entity());
        assertThat(rawCursor.cursorOffset(1)).isEqualTo(1);
        assertThat(rawCursor.cursorOffset(3)).isEqualTo(1);
        assertThat(rawCursor.cursorOffset(0)).isZero();
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
        assertThat(cursor.isSupported(Map.of(), Map.of("id", SortMeta.builder().field("id")
                .build()))).isTrue();
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void predicate() {
        cursor.save(3, new Entity());
        assertThat(cursor.cursorPredicate(0, cb, root, Map.of("id", SortMeta.builder().field("id").build()))).isNull();
        assertThat(cursor.cursorPredicate(4, cb, root, Map.of("id", SortMeta.builder().field("id").build()))).isNull();
        verify(cb).greaterThan(root.get("id"), "one");

        assertThat(cursor.cursorPredicate(4, cb, root, Map.of("id", SortMeta.builder().field("id")
                .order(SortOrder.DESCENDING).build()))).isNull();
        verify(cb).lessThan(root.get("id"), "one");

        verify(root, times(4)).get("id");
        verifyNoMoreInteractions(cb, root);
    }

    @Test
    void valueForLogging() {
        assertThat(CursorData.valueForLogging(null).get()).isNull();
    }

    @Test
    void cursorOffsetWithNoop() {
        var noop = CursorPagination.noop();
        assertThat(noop.cursorOffset(0)).isZero();
        assertThat(noop.cursorOffset(1)).isEqualTo(1);
    }

    @Test
    void cursorPredicateWithNoop() {
        var noop = CursorPagination.noop();
        assertThat(noop.cursorPredicate(0, null, null, null)).isNull();
        assertThat(noop.cursorPredicate(1, null, null, null)).isNull();
    }

    @Test
    void miscWithNoop() {
        var noop = CursorPagination.noop();
        assertThat(noop.columns()).isEmpty();
        assertThat(noop.isSupported(null, null)).isFalse();
        assertThatThrownBy(() -> noop.setCurrentColumn(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> noop.setCurrentColumn("hello")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> noop.save(0, null)).isInstanceOf(UnsupportedOperationException.class);
    }
}
