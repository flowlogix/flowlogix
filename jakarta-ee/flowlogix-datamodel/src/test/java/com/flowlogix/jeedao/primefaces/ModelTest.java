/*
 * Copyright (C) 2011-2023 Flow Logix, Inc. All Rights Reserved.
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

import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.RESULT;
import static com.flowlogix.util.SerializeTester.serializeAndDeserialize;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import com.flowlogix.jeedao.primefaces.internal.JPAModelImpl;
import com.flowlogix.jeedao.primefaces.internal.InternalQualifierJPALazyModel;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.primefaces.model.SortOrder.ASCENDING;
import static org.primefaces.model.SortOrder.DESCENDING;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.MatchMode;
import org.primefaces.model.SortMeta;

/**
 *
 * @author lprimak
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("MagicNumber")
public class ModelTest implements Serializable {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS, serializable = true)
    EntityManager em;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    transient CriteriaBuilder cb;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    transient Root<Object> rootObject;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    transient Root<Integer> rootInteger;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    transient Path<Integer> integerPath;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    transient Join<Integer, Integer> integerJoin;

    @Test
    void resultField() {
        assertEquals(String.format("%s.hello", RESULT), JPALazyDataModel.getResultField("hello"));
    }

    @Test
    void stringFilter() {
        var impl = JPAModelImpl.builder()
                .entityManager(() -> em)
                .entityClass(Object.class)
                .converter(s -> new Object())
                .filter(ModelTest::filter)
                .build();
        when(rootObject.get(any(String.class)).getJavaType()).thenAnswer(a -> String.class);
        var fm = FilterMeta.builder().field("aaa").filterValue("hello").build();
        impl.getFilters(Map.of("aaa", fm), cb, rootObject);
        verify(rootObject).get("test");
    }

    @Test
    void integerFilter() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.get(any(String.class)).getJavaType()).thenAnswer(a -> Integer.class);
        var fm = FilterMeta.builder().field("aaa").filterValue(5).build();
        impl.getFilters(Map.of("aaa", fm), cb, rootInteger);
        verify(rootInteger).get("aaa");
    }

    @Test
    void collectionFilter() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.get(any(String.class)).getJavaType()).thenAnswer(a -> List.class);
        var fm = FilterMeta.builder().field("aaa")
                .filterValue(List.of("one", "two")).matchMode(MatchMode.IN).build();
        impl.getFilters(Map.of("aaa", fm), cb, rootInteger);
        verify(rootInteger).get("aaa");
    }

    private static void filter(FilterData filterData, CriteriaBuilder cb, Root<Object> root) {
        assertTrue(filterData.replaceFilter("aaa",
                (Predicate predicate, String value) -> cb.greaterThan(root.get("column2"), value)));
        root.get("test");
    }

    @Test
    void sortAdding() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .sorter(ModelTest::addingSorter)
                .converter(Long::valueOf)
                .build();

        var uiSortCriteria = Map.of(
                "col1", SortMeta.builder().field("aaa").order(ASCENDING).priority(3).build(),
                "col2", SortMeta.builder().field("bbb").order(DESCENDING).priority(4).build());
        when(rootInteger.get(any(String.class))).thenAnswer(a -> integerPath);
        var ascendingOrder = createOrder(true);
        when(cb.asc(any())).thenReturn(ascendingOrder);
        var descendingOrder = createOrder(false);
        when(cb.desc(any())).thenReturn(descendingOrder);
        var sortResult = impl.getSort(uiSortCriteria, cb, rootInteger);
        assertEquals(4, sortResult.size());
        assertFalse(sortResult.get(0).isAscending());
        assertTrue(sortResult.get(1).isAscending());
        assertTrue(sortResult.get(3).isAscending());
        assertFalse(sortResult.get(2).isAscending());
    }

    private static void addingSorter(SortData sortData, CriteriaBuilder cb, Root<Integer> root) {
        sortData.applicationSort("ccc", sortMeta -> {
            assertTrue(sortMeta.isEmpty());
            return cb.asc(root.get("zipcode"));
        });
        sortData.applicationSort("ddd", true, sortMeta -> {
            assertTrue(sortMeta.isEmpty());
            return cb.desc(root.get("zipcode"));
        });
    }

    @Test
    void sortReplacing() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .sorter(ModelTest::replacingSorter)
                .converter(Long::valueOf)
                .build();

        var uiSortCriteria = Map.of(
                "col1", SortMeta.builder().field("xxx").order(ASCENDING).priority(3).build(),
                "col2", SortMeta.builder().field("uuu").order(DESCENDING).priority(4).build());
        when(rootInteger.get(any(String.class))).thenAnswer(a -> integerPath);
        var ascendingOrder = createOrder(true);
        when(cb.asc(any())).thenReturn(ascendingOrder);
        var descendingOrder = createOrder(false);
        when(cb.desc(any())).thenReturn(descendingOrder);
        var sortResult = impl.getSort(uiSortCriteria, cb, rootInteger);
        assertEquals(2, sortResult.size());
        assertTrue(sortResult.get(0).isAscending());
        assertTrue(sortResult.get(1).isAscending());
    }

    private static void replacingSorter(SortData sortData, CriteriaBuilder cb, Root<Integer> root) {
        sortData.applicationSort("uuu", sortMeta -> {
            assertFalse(sortMeta.isEmpty());
            return cb.asc(root.get("zipcode"));
        });
    }

    private static Order createOrder(boolean isAscending) {
        var order = mock(Order.class, withSettings().strictness(Strictness.LENIENT));
        when(order.isAscending()).thenReturn(isAscending);
        return order;
    }

    @Test
    void resolveSimpleField() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.<Integer, Integer>join(any(String.class))).thenReturn(integerJoin);
        when(rootInteger.<Integer>get("a")).thenReturn(integerPath);
        assertEquals(integerPath, impl.resolveField(rootInteger, "a"));
        verify(rootInteger).get("a");
    }

    @Test
    void resolveJoinField() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.<Integer, Integer>join(any(String.class))).thenReturn(integerJoin);
        when(integerJoin.<Integer, Integer>join(any(String.class))).thenReturn(integerJoin);
        when(integerJoin.<Integer>get("c")).thenReturn(integerPath);
        assertEquals(integerPath, impl.resolveField(rootInteger, "a.b.c"));
        verify(rootInteger).join("a");
        verify(integerJoin).join("b");
        verify(integerJoin).get("c");
    }

    @Test
    void optimizer() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .optimizer(query -> {
                    rootInteger.get("optimizer");
                    return query;
                })
                .build();
        impl.findRows(0, 10, Map.of(), Map.of());
        verify(rootInteger).get("optimizer");
    }

    @Test
    void jsfConversion() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.get(any(String.class)).getJavaType()).thenAnswer(a -> Integer.class);
        var fm = FilterMeta.builder().field("aaa").filterValue("xxx").build();
        try (var mockedStatic = mockStatic(Faces.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS))) {
            impl.getFilters(Map.of("aaa", fm), cb, rootInteger);
        }
        verify(rootInteger).get("aaa");
    }

    @RequiredArgsConstructor
    public static class MyEntity {
        final Long id;
        public MyEntity() {
            this.id = 1L;
        }
    }

    @Test
    void defaultConverters() {
        var impl = JPAModelImpl.<MyEntity, Long>builder()
                .entityManager(() -> em)
                .entityClass(MyEntity.class)
                .build();
        when(em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(any(MyEntity.class)))
                .thenAnswer(entry -> entry.<MyEntity>getArgument(0).id);
        assertEquals(5L, impl.getStringToKeyConverter().apply("5"));
        assertEquals("10", impl.getKeyConverter().apply(new MyEntity(10L)));
    }

    @Test
    void serialization() throws IOException, ClassNotFoundException {
        JPALazyDataModel<MyEntity, Long> model;
        try (var mockedStatic = mockStatic(Beans.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS))) {
            mockedStatic.when(() -> Beans.getReference(eq(JPALazyDataModel.class), eq(InternalQualifierJPALazyModel.LITERAL)))
                    .thenReturn(new JPALazyDataModel<>());
            model = JPALazyDataModel.create(builder -> builder
                    .entityManager(() -> em).entityClass(MyEntity.class)
                    .build());
        }
        lenient().when(em.getEntityManagerFactory().getPersistenceUnitUtil()
                .getIdentifier(any(MyEntity.class))).thenReturn(5L);
        var deserialized = serializeAndDeserialize(model);
        assertEquals("5", deserialized.getRowKey(new MyEntity()));
    }
}
