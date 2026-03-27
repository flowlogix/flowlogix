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
package com.flowlogix.jeedao.primefaces.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import java.util.Map;
import java.util.Optional;
import static com.flowlogix.jeedao.primefaces.Accessors.mergeSortOrder;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SortingTest {
    @Mock
    Order order;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    EntityManager em;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Root<Object> root;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    CriteriaBuilder cb;

    private final JPAModelImpl<Object> model = JPAModelImpl.builder()
            .entityClass(Object.class).build();

    @Test
    void sortOrderWithInvalidApplicationSort() {
        assertThatThrownBy(() -> model.processSortOrder(Map.of("id", mergeSortOrder(null, null, false)),
                null, null, true)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cursorSupportedWithApplicationSort() {
        assertThatNoException().isThrownBy(() -> model.processSortOrder(Map.of("id",
                        mergeSortOrder(SortMeta.builder().order(SortOrder.ASCENDING).field("xxx").build(), null, false)),
                cb, root, true));
        JPAModelImpl.processApplicationSortOrder(true, mergeSortOrder(null, order, true), null);
    }

    @Test
    void invalidPUReflection() {
        var model = JPAModelImpl.<Record>builder().entityManager(() -> em)
                .entityClass(Record.class).build();
        assertThatThrownBy(() -> model.getPrimaryKey(Optional.empty())).isInstanceOf(ReflectiveOperationException.class);
    }
}
