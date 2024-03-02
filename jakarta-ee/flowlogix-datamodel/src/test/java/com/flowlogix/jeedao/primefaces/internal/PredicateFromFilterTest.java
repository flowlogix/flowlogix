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
package com.flowlogix.jeedao.primefaces.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.FilterMeta;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.primefaces.model.MatchMode.NOT_STARTS_WITH;
import static org.primefaces.model.MatchMode.STARTS_WITH;

@ExtendWith(MockitoExtension.class)
class PredicateFromFilterTest {
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Expression<?> expression;
    @Mock
    private EntityManager em;

    private JPAModelImpl<Integer> model;

    @BeforeEach
    void buildModel() {
        model = JPAModelImpl.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
    }

    @Test
    void startsWith() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(STARTS_WITH).build(), "abc");
        verify(cb).like(any(), eq("abc%"));
        verifyNoMoreInteractions(cb);
    }

    @Test
    void notStartsWith() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(NOT_STARTS_WITH).build(), "abc");
        verify(cb).notLike(any(), eq("abc%"));
        verifyNoMoreInteractions(cb);
    }
}
