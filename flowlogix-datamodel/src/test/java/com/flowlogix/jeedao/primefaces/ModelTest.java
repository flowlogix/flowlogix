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

import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.RESULT;
import static com.flowlogix.util.SerializeTester.serializeAndDeserialize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import com.flowlogix.jeedao.primefaces.internal.JPAModelImpl;
import com.flowlogix.jeedao.primefaces.internal.InternalQualifierJPALazyModel;
import jakarta.faces.component.UIComponent;
import jakarta.faces.convert.Converter;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.primefaces.model.FilterMeta.GLOBAL_FILTER_KEY;
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
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:MethodCount"})
class ModelTest implements Serializable {
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
        assertThat(JPALazyDataModel.getResultField("hello")).isEqualTo(String.format("%s.hello", RESULT));
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
        var impl = JPAModelImpl.<Integer>builder()
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
    void typedCollectionFilter() {
        collectionFilter(List.of(1, 2), false);
    }

    @Test
    void typedArrayFilter() {
        collectionFilter(new Integer[] {1, 2}, false);
    }

    @Test
    void typedCollectionFilterConversion() {
        collectionFilter(List.of("1", "2"), false);
    }

    @Test
    void typedArrayFilterConversion() {
        collectionFilter(new String[] {"1", "2"}, false);
    }

    @Test
    void typedCollectionInvalidFilterConversion() {
        collectionFilter(List.of("abc", "def"), true);
    }

    private <TT> void collectionFilter(Object valueList, boolean checkAbsence) {
        var impl = JPAModelImpl.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.get(any(String.class))).thenAnswer(a -> integerPath);
        when(integerPath.getJavaType()).thenAnswer(a -> Integer.class);
        when(integerPath.in(any(List.class))).then(a -> {
            List<?> list = a.getArgument(0);
            assertEquals(Integer.class, list.get(0).getClass());
            assertEquals(Integer.class, list.get(1).getClass());
            return null;
        });
        var fm = FilterMeta.builder().field("aaa")
                .filterValue(valueList).matchMode(MatchMode.IN).build();
        impl.getFilters(Map.of("aaa", fm), cb, rootInteger);
        verify(rootInteger).get("aaa");
        if (checkAbsence) {
            verify(integerPath, never()).in(any(List.class));
        } else {
            verify(integerPath).in(any(List.class));
        }
    }

    @Test
    void betweenFilter() {
        var impl = JPAModelImpl.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.get(any(String.class)).getJavaType()).thenAnswer(a -> Integer.class);
        var fm = FilterMeta.builder().field("aaa")
                .filterValue(List.of(2, 3)).matchMode(MatchMode.BETWEEN).build();
        impl.getFilters(Map.of("aaa", fm), cb, rootInteger);
        verify(rootInteger).get("aaa");
    }

    @Test
    void similarConversionFilter() {
        var impl = JPAModelImpl.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.get(any(String.class)).getJavaType()).thenAnswer(a -> Integer.class);
        var fm = FilterMeta.builder().field("aaa").matchMode(MatchMode.GREATER_THAN).filterValue(5L).build();
        impl.getFilters(Map.of("aaa", fm), cb, rootInteger);
        verify(rootInteger).get("aaa");
    }

    @Test
    void missingFilter() {
        var clearFilters = new AtomicBoolean(false);
        var impl = JPAModelImpl.builder()
                .entityManager(() -> em)
                .entityClass(Object.class)
                .converter(s -> new Object())
                .filter((filterData, builder, root) -> filterData.replaceFilter(GLOBAL_FILTER_KEY,
                        (predicate, value) -> {
                            if (clearFilters.get()) {
                                filterData.clear();
                            }
                            return builder.isTrue(predicate);
                        }))
                .build();
        when(rootObject.get(any(String.class)).getJavaType()).thenAnswer(a -> String.class);
        verify(rootObject).get((String) null);
        var fm = FilterMeta.builder().field("ccc").filterValue("hello").build();
        impl.getFilters(Map.of("bbb", fm), cb, rootObject);
        var fm2 = FilterMeta.of(null, null, false);
        impl.getFilters(Map.of("bbb", fm2), cb, rootObject);
        var fm3 = FilterMeta.builder().field(GLOBAL_FILTER_KEY).build();
        impl.getFilters(Map.of("bbb", fm3), cb, rootObject);
        clearFilters.set(true);
        impl.getFilters(Map.of("bbb", FilterMeta.builder().field(GLOBAL_FILTER_KEY)
                .filterValue("bye").build()), cb, rootObject);
        verify(rootObject).get("ccc");
        verify(rootObject).get(GLOBAL_FILTER_KEY);
        verifyNoMoreInteractions(rootObject);
    }

    private static void filter(FilterData filterData, CriteriaBuilder cb, Root<Object> root) {
        assertThat(filterData.replaceFilter("aaa",
                (Predicate predicate, String value) -> cb.greaterThan(root.get("column2"), value))).isTrue();
        root.get("test");
    }

    @Test
    void sortAdding() {
        var impl = JPAModelImpl.<Integer>builder()
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
        assertThat(sortResult).hasSize(6);
        assertThat(sortResult.get(0).isAscending()).isFalse();
        assertThat(sortResult.get(1).isAscending()).isTrue();
        assertThat(sortResult.get(3).isAscending()).isTrue();
        assertThat(sortResult.get(2).isAscending()).isFalse();
        assertThat(sortResult.get(4).isAscending()).isTrue();
        assertThat(sortResult.get(5).isAscending()).isFalse();
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
        sortData.applicationSort("aaa", "randcol1",
                sortData, () -> cb.asc(root.get("randcol1")), null, null);
        sortData.applicationSort("bbb", "randcol2",
                sortData, null, () -> cb.desc(root.get("randcol2")), null);
    }

    @Test
    void sortReplacing() {
        var impl = JPAModelImpl.<Integer>builder()
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
        assertThat(sortResult).hasSize(2);
        assertThat(sortResult.get(0).isAscending()).isTrue();
        assertThat(sortResult.get(1).isAscending()).isTrue();
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
        var impl = JPAModelImpl.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.<Integer, Integer>join(any(String.class))).thenReturn(integerJoin);
        when(rootInteger.<Integer>get("a")).thenReturn(integerPath);
        assertThat(impl.resolveField(rootInteger, "a")).isEqualTo(integerPath);
        verify(rootInteger).get("a");
    }

    @Test
    void resolveJoinField() {
        var impl = JPAModelImpl.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.<Integer, Integer>join(any(String.class))).thenReturn(integerJoin);
        when(integerJoin.<Integer, Integer>join(any(String.class))).thenReturn(integerJoin);
        when(integerJoin.<Integer>get("c")).thenReturn(integerPath);
        assertThat(impl.resolveField(rootInteger, "a.b.c")).isEqualTo(integerPath);
        verify(rootInteger).join("a");
        verify(integerJoin).join("b");
        verify(integerJoin).get("c");
    }

    @Test
    void optimizer() {
        var impl = JPAModelImpl.<Integer>builder()
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
        var impl = JPAModelImpl.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .converter(Long::valueOf)
                .build();
        when(rootInteger.get(any(String.class)).getJavaType()).thenAnswer(a -> Integer.class);
        var fm = FilterMeta.builder().field("aaa").filterValue("xxx").build();
        @SuppressWarnings("unchecked")
        Converter<String> converter = mock(Converter.class);
        try (var mockedStatic = mockStatic(Faces.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS))) {
            try (var mockedUIComponent = mockStatic(UIComponent.class)) {
                when(UIComponent.getCurrentComponent(any())).thenReturn(null);
                when(Faces.getApplication().createConverter(Integer.class)).thenReturn(converter);
                when(converter.getAsObject(any(), any(), eq("xxx"))).thenReturn("aaa");
                impl.getFilters(Map.of("aaa", fm), cb, rootInteger);
            }
        }
        verify(rootInteger).get("aaa");
        verify(converter).getAsObject(any(), any(), eq("xxx"));
    }

    @SuppressWarnings("checkstyle:RedundantModifier")
    public static class MyEntity {
        final Long id;
        public MyEntity() {
            this.id = 1L;
        }
        public MyEntity(long id) {
            this.id = id;
        }
    }

    @Test
    void defaultConverters() {
        var impl = JPAModelImpl.<MyEntity>builder()
                .entityManager(() -> em)
                .entityClass(MyEntity.class)
                .build();
        when(em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(any(MyEntity.class)))
                .thenAnswer(entry -> entry.<MyEntity>getArgument(0).id);
        assertThat(impl.getStringToKeyConverter().apply("5")).isEqualTo(5L);
        assertThat(impl.getKeyConverter().apply(new MyEntity(10L))).isEqualTo("10");
    }

    @Test
    void serialization() throws IOException, ClassNotFoundException {
        JPALazyDataModel<MyEntity> model;
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
        assertThat(deserialized.getRowKey(new MyEntity())).isEqualTo("5");
    }

    @Test
    void serializeWithNonSerializedOptimizer() throws IOException, ClassNotFoundException {
        try (var ns = new NonSerializableRequestScopedMock(rootInteger)) {
            var model = new JPALazyDataModel<Integer>().initialize(builder ->
                    builder.entityClass(Integer.class).entityManager(() -> em)
                            .optimizer(optimize()).build());
            var deserialized = serializeAndDeserialize(model);
            deserialized.findRows(0, 10, Map.of(), Map.of());
        }
        verify(rootInteger).get("optimizer");
    }

    UnaryOperator<TypedQuery<Integer>> optimize() {
        return NonSerializableRequestScopedMock.INSTANCE.get()::optimizeQuery;
    }

    static class NonSerializableRequestScopedMock implements AutoCloseable {
        static final ThreadLocal<NonSerializableRequestScopedMock> INSTANCE = new ThreadLocal<>();
        private final Root<Integer> rootInteger;

        NonSerializableRequestScopedMock(Root<Integer> rootInteger) {
            this.rootInteger = rootInteger;
            INSTANCE.set(this);
        }

        TypedQuery<Integer> optimizeQuery(TypedQuery<Integer> query) {
            rootInteger.get("optimizer");
            return query;
        }

        @Override
        public void close() {
            INSTANCE.remove();
        }
    }

    @Test
    void partialInitialization() {
        var model = new JPALazyDataModel<Integer>()
                .partialInitialize(builder -> builder.entityClass(Integer.class).build());
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> model.partialInitialize(JPAModelImpl.JPAModelImplBuilder::build));
        assertThat(model.getEntityClass()).isEqualTo(Integer.class);
    }

    @Test
    void createNullModel() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> JPALazyDataModel.create(null));
    }

    @Test
    void initializeNullModel() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new JPALazyDataModel<Integer>().initialize(null));
    }

    @Test
    void partialInitializationWithNull() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new JPALazyDataModel<Integer>().partialInitialize(null));
    }

    @Test
    void createInternalModelWithNull() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> JPAModelImpl.create(null));
    }
}
