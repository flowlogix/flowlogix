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
package com.flowlogix.api.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.function.Supplier;

public interface JPAFinderHelper<TT> extends JPAFinder<TT> {
    Supplier<EntityManager> getEntityManager();
    Class<TT> getEntityClass();

    QueryCriteria<TT> buildQueryCriteria();
    <RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls);

    /**
     * Convenience interface for use with {@link lombok.experimental.Delegate} when forwarding methods
     * of {@link EntityManager} so DaoHelper's own methods get exposed correctly
     * <p>
     * {@snippet class = "com.flowlogix.demo.jeedao.ExampleDelegateDAO" region = "delegateDAO"}
     */
     interface EntityManagerExclusions {
        Query createNativeQuery(String sql, Class<?> resultClass);
        Query createNativeQuery(String sql, String resultMapping);
    }
}
