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
package com.flowlogix.jeedao;

import com.flowlogix.util.SerializeTester;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import lombok.experimental.Delegate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 *
 * @author lprimak
 */
public class FacadeTest implements Serializable {
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
        assertEquals(5, new MyControl().find(1L));
        when(em.createQuery(any(CriteriaQuery.class)).getResultList()).thenReturn(Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2), new MyControl().findRange(5, 7));

        when(em.createQuery(any(CriteriaQuery.class)).getSingleResult()).thenReturn(2L);
        assertEquals(2, new MyControl().count());
    }

    @Test
    void nulls() {
        assertThrows(NullPointerException.class, () -> {
            DaoHelper<Long> facade = new DaoHelper<>(() -> null, null);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void inheritableDao() {
        InheritableDaoHelper<Integer> helper = new InheritableDaoHelper<>();
        assertNull(helper.daoHelper);
        helper.daoHelper = DaoHelper.<Integer>builder()
                .entityManager(() -> em)
                .entityClass(Integer.class)
                .build();
        when(em.createQuery(any(CriteriaQuery.class)).getSingleResult()).thenReturn(2L);
        assertEquals(2, helper.count());
    }

    @Test
    @SuppressWarnings("MagicNumber")
    void serialize() throws IOException, ClassNotFoundException {
        var mc = SerializeTester.serializeAndDeserialize(new MyControl());
        assertNotNull(mc.getEntityManager());
        assertEquals(5, mc.find(1L));
        assertNull(mc.find(2L));
    }
}
