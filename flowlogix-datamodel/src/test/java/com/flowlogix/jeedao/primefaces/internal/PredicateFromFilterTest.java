/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.MatchMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.primefaces.model.MatchMode.BETWEEN;
import static org.primefaces.model.MatchMode.CONTAINS;
import static org.primefaces.model.MatchMode.ENDS_WITH;
import static org.primefaces.model.MatchMode.EQUALS;
import static org.primefaces.model.MatchMode.EXACT;
import static org.primefaces.model.MatchMode.GLOBAL;
import static org.primefaces.model.MatchMode.GREATER_THAN;
import static org.primefaces.model.MatchMode.IN;
import static org.primefaces.model.MatchMode.NOT_CONTAINS;
import static org.primefaces.model.MatchMode.NOT_ENDS_WITH;
import static org.primefaces.model.MatchMode.NOT_EQUALS;
import static org.primefaces.model.MatchMode.NOT_EXACT;
import static org.primefaces.model.MatchMode.NOT_IN;
import static org.primefaces.model.MatchMode.NOT_STARTS_WITH;
import static org.primefaces.model.MatchMode.STARTS_WITH;

@ExtendWith(MockitoExtension.class)
class PredicateFromFilterTest {
    @SuppressWarnings("checkstyle:ConstantName")
    private static final Set<MatchMode> untestedMatchModes = ConcurrentHashMap.newKeySet();
    @Mock(answer = RETURNS_DEEP_STUBS)
    private CriteriaBuilder cb;
    @Mock
    private Expression<?> expression;
    @Mock
    private Expression<String> stringExpression;
    @Mock
    private Predicate predicate;
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

    @BeforeAll
    static void fillUntestedMatchModes() {
        untestedMatchModes.addAll(Arrays.asList(MatchMode.values()));
        // the following are tested via integration tests
        untestedMatchModes.remove(CONTAINS);
        untestedMatchModes.remove(EQUALS);
        untestedMatchModes.remove(NOT_EXACT);
        untestedMatchModes.remove(GREATER_THAN);
        untestedMatchModes.remove(BETWEEN);
    }

    @AfterAll
    static void checkExhaustiveMatchModes() {
        assertThat(untestedMatchModes).isEmpty();
    }

    @Test
    void startsWith() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(STARTS_WITH).build(), "abc");
        verify(cb).like(any(), eq("abc%"));
        verify(expression).as(String.class);
        verifyNoMoreInteractions(cb, expression);
        untestedMatchModes.remove(STARTS_WITH);
    }

    @Test
    void notStartsWith() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(NOT_STARTS_WITH).build(), "abc");
        verify(cb).notLike(any(), eq("abc%"));
        verify(expression).as(String.class);
        verifyNoMoreInteractions(cb, expression);
        untestedMatchModes.remove(NOT_STARTS_WITH);
    }

    @Test
    void endsWith() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(ENDS_WITH).build(), "abc");
        verify(cb).like(any(), eq("%abc"));
        verify(expression).as(String.class);
        verifyNoMoreInteractions(cb, expression);
        untestedMatchModes.remove(ENDS_WITH);
    }

    @Test
    void notEndsWith() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(NOT_ENDS_WITH).build(), "abc");
        verify(cb).notLike(any(), eq("%abc"));
        verify(expression).as(String.class);
        verifyNoMoreInteractions(cb, expression);
        untestedMatchModes.remove(NOT_ENDS_WITH);
    }

    @Test
    void notContains() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(NOT_CONTAINS).build(), "abc");
        verify(cb).notLike(any(), eq("%abc%"));
        verify(expression).as(String.class);
        verifyNoMoreInteractions(cb, expression);
        untestedMatchModes.remove(NOT_CONTAINS);
    }

    @Test
    void exactWithoutWildcard() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(EXACT).build(), "abc");
        verify(cb).equal(any(), eq("abc"));
        verifyNoMoreInteractions(cb, expression);
        untestedMatchModes.remove(EXACT);
    }

    @Test
    void exactWithWildcard() {
        model = JPAModelImpl.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .wildcardSupport(true)
                .build();
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(EXACT).build(), "abc");
        verify(cb).equal(any(), eq("abc"));
        verify(expression).as(String.class);
        verifyNoMoreInteractions(cb, expression);
    }

    @Test
    void notEquals() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(NOT_EQUALS).build(), "abc");
        verify(cb).notEqual(any(), eq("abc"));
        verifyNoMoreInteractions(cb, expression);
        untestedMatchModes.remove(NOT_EQUALS);
    }

    @Test
    void in() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(IN).build(), List.of("abc"));
        verify(cb).equal(any(), eq("abc"));
        verifyNoMoreInteractions(cb, expression);
        untestedMatchModes.remove(IN);
    }

    @Test
    void notInSingle() {
        model.predicateFromFilter(cb, expression, FilterMeta.builder().field("aaa")
                .matchMode(NOT_IN).build(), List.of("abc"));
        verify(cb).notEqual(any(), eq("abc"));
        verifyNoMoreInteractions(cb, expression);
        untestedMatchModes.remove(NOT_IN);
    }

    @Test
    void notInMultiple() {
        when(stringExpression.in(any(Collection.class))).thenReturn(predicate);
        model.predicateFromFilter(cb, stringExpression, FilterMeta.builder().field("aaa")
                .matchMode(NOT_IN).build(), List.of("abc", "def"));
        verify(stringExpression).in(List.of("abc", "def"));
        verify(stringExpression.in(List.of())).not();
        verifyNoMoreInteractions(cb, stringExpression, predicate);
    }

    @Test
    void globalFilter() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
                model.predicateFromFilter(cb, expression, FilterMeta.builder().field("globalFilter")
                        .matchMode(GLOBAL).build(), "abc"));
        verifyNoMoreInteractions(cb);
        untestedMatchModes.remove(GLOBAL);
    }

    @Test
    void lessThan() {
        model.predicateFromFilterComparable(cb, stringExpression, FilterMeta.builder().field("aaa")
                .matchMode(MatchMode.LESS_THAN).build(), "abc", null);
        verify(cb).lessThan(eq(stringExpression), eq("abc"));
        verifyNoMoreInteractions(cb, stringExpression);
        untestedMatchModes.remove(MatchMode.LESS_THAN);
    }

    @Test
    void lessThanEquals() {
        model.predicateFromFilterComparable(cb, stringExpression, FilterMeta.builder().field("aaa")
                .matchMode(MatchMode.LESS_THAN_EQUALS).build(), "abc", null);
        verify(cb).lessThanOrEqualTo(eq(stringExpression), eq("abc"));
        verifyNoMoreInteractions(cb, stringExpression);
        untestedMatchModes.remove(MatchMode.LESS_THAN_EQUALS);
    }

    @Test
    void greaterThanEquals() {
        model.predicateFromFilterComparable(cb, stringExpression, FilterMeta.builder().field("aaa")
                .matchMode(MatchMode.GREATER_THAN_EQUALS).build(), "abc", null);
        verify(cb).greaterThanOrEqualTo(eq(stringExpression), eq("abc"));
        verifyNoMoreInteractions(cb, stringExpression);
        untestedMatchModes.remove(MatchMode.GREATER_THAN_EQUALS);
    }

    @Test
    void notBetween() {
        when(cb.and(any(), any())).thenReturn(predicate);
        model.predicateFromFilterComparable(cb, stringExpression, FilterMeta.builder().field("aaa")
                .matchMode(MatchMode.NOT_BETWEEN).build(), null, List.of("abc", "def"));
        verify(cb).greaterThanOrEqualTo(eq(stringExpression), eq("abc"));
        verify(cb).lessThanOrEqualTo(eq(stringExpression), eq("def"));
        verify(cb).and(any(), any());
        verify(predicate).not();
        verifyNoMoreInteractions(cb, stringExpression, predicate);
        untestedMatchModes.remove(MatchMode.NOT_BETWEEN);
    }
}
