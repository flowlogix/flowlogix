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
package com.flowlogix.jeedao;

import com.flowlogix.util.SerializeTester;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import lombok.experimental.Delegate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import org.junit.jupiter.api.Test;
import org.omnifaces.util.Beans;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 *
 * @author lprimak
 */
class FacadeTest implements Serializable {
    private final EntityManager em = mock(EntityManager.class, withSettings().serializable()
            .defaultAnswer(RETURNS_DEEP_STUBS));
    class MyControl implements Serializable {
        @Delegate
        final DaoHelper<Integer> facade = DaoHelper.<Integer>builder()
                .entityClass(Integer.class)
                .entityManager(() -> em)
                .build();

        @SuppressWarnings("MagicNumber")
        MyControl() {
            when(em.find(facade.getEntityClass(), 1L)).thenReturn(5);
        }

        Integer find(Long key) {
            return em.find(facade.getEntityClass(), key);
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "MagicNumber"})
    void usage() {
        assertThat(new MyControl().find(1L)).isEqualTo(5);
        when(em.createQuery(any(CriteriaQuery.class)).getResultList()).thenReturn(Arrays.asList(1, 2));
        assertThat(new MyControl().findRange(5, 7).getResultList()).isEqualTo(Arrays.asList(1, 2));

        when(em.createQuery(any(CriteriaQuery.class)).getSingleResult()).thenReturn(2L);
        assertThat(new MyControl().count()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings({"unchecked", "MagicNumber"})
    void findAll() {
        var query = mock(TypedQuery.class);
        when(em.createQuery(any(CriteriaQuery.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(10L));
        assertThat(new MyControl().findAll().getResultList()).isEqualTo(List.of(10L));
    }

    @Test
    void findEntityManager() {
        try (var mock = mockStatic(Beans.class)) {
            var entityManager = DaoHelper.findEntityManager();
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(entityManager::get);
        }
    }

    @Test
    void findEntityManagerNullQualifiers() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            DaoHelper.findEntityManager(null);
        });
    }

    @Test
    @SuppressWarnings({"unchecked", "MagicNumber"})
    void nativeQuery() {
        var query = mock(Query.class);
        when(em.createNativeQuery(any(String.class), eq(Long.class))).thenReturn(query);
        when(em.createNativeQuery(any(String.class), any(String.class))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(5L);
        when(query.getResultStream()).thenAnswer(a -> Stream.of(3L));
        var tnq = new MyControl().createNativeQuery("hello", Long.class);
        assertThat(tnq.<Long>getSingleResult()).isEqualTo(5L);
        assertThat(tnq.getResultStream().findFirst().get()).isEqualTo(3L);
        var tnq2 = new MyControl().createNativeQuery("hello", "mapping");
        assertThat(tnq2.<Long>getSingleResult()).isEqualTo(5L);
        assertThat(tnq2.getResultStream().findFirst().get()).isEqualTo(3L);
    }

    @Test
    void nulls() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            new DaoHelper<Long>(() -> null, null);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void inheritableDao() {
        InheritableDaoHelper<Integer> helper = new InheritableDaoHelper<>();
        assertThat(helper.jpaFinder).isNull();
        helper.jpaFinder = DaoHelper.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .build();
        when(em.createQuery(any(CriteriaQuery.class)).getSingleResult()).thenReturn(2L);
        assertThat(helper.count()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("MagicNumber")
    void serialize() throws IOException, ClassNotFoundException {
        var mc = SerializeTester.serializeAndDeserialize(new MyControl());
        assertThat(mc.facade.em()).isNotNull();
        assertThat(mc.find(1L)).isEqualTo(5);
        assertThat(mc.find(2L)).isNull();
    }

    @Test
    void nullEntityManager() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            new DaoHelper<>(null, null);
        });
    }

    @Test
    void nullEntityClass() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            new DaoHelper<>(() -> em, null);
        });
    }
}
