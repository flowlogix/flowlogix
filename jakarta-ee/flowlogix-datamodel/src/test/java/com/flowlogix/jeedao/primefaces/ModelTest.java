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
import com.flowlogix.jeedao.primefaces.impl.JPAModelImpl;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
        var fm = new FilterMeta();
        when(rootObject.get(any(String.class)).getJavaType()).thenAnswer(a -> String.class);
        fm.setFilterValue("hello");
        impl.getFilters(Map.of("column", fm), cb, rootObject);
    }

    @Test
    @SuppressWarnings("MagicNumber")
    void integerFilter() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        var fm = new FilterMeta();
        when(rootInteger.get(any(String.class)).getJavaType()).thenAnswer(a -> Integer.class);
        fm.setFilterValue(5);
        impl.getFilters(Map.of("column", fm), cb, rootInteger);
    }

    @Test
    void collectionFilter() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        var fm = new FilterMeta();
        when(rootInteger.get(any(String.class)).getJavaType()).thenAnswer(a -> List.class);
        fm.setFilterValue(List.of("one", "two"));
        fm.setMatchMode(MatchMode.IN);
        impl.getFilters(Map.of("column", fm), cb, rootInteger);
    }

    private static void filter(FilterData filterData, CriteriaBuilder cb, Root<Object> root) {
        filterData.replaceFilter("column",
                (Predicate predicate, String value) -> cb.greaterThan(root.get("column2"), value));
    }

    @Test
    @SuppressWarnings("MagicNumber")
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
    @SuppressWarnings("MagicNumber")
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
    void jsfConversionTest() {
        var impl = JPAModelImpl.<Integer, Long>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        var fm = new FilterMeta();
        when(rootInteger.get(any(String.class)).getJavaType()).thenAnswer(a -> Integer.class);
        fm.setFilterValue("xxx");
        try (var mockedStatic = mockStatic(Faces.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS))) {
            impl.getFilters(Map.of("column", fm), cb, rootInteger);
        }
    }

    @RequiredArgsConstructor
    public static class MyEntity {
        final Long id;
        @SuppressWarnings("MagicNumber")
        public MyEntity() {
            this.id = 1L;
        }
    }

    @Test
    @SuppressWarnings("MagicNumber")
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
    @SuppressWarnings("MagicNumber")
    void serialization() throws IOException, ClassNotFoundException {
        JPALazyDataModel<MyEntity, Long> model;
        try (var mockedStatic = mockStatic(Beans.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS))) {
            mockedStatic.when(() -> Beans.getReference(JPALazyDataModel.class))
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
