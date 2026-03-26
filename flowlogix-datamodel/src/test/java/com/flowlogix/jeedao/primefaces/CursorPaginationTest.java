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

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorPaginationTest {
    static class Entity { }

    @Test
    void save() {
        var cursor = CursorPagination.create(Map.of("id", e -> "one"));
        assertThatThrownBy(() -> cursor.save(0, new Entity())).isInstanceOf(NullPointerException.class);
        cursor.setCurrentColumn("id");
        cursor.save(2, new Entity());
        assertThat(cursor.cursorOffset(1)).isEqualTo(1);
        assertThat(cursor.cursorOffset(3)).isEqualTo(1);
        assertThat(cursor.cursorOffset(0)).isZero();
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
