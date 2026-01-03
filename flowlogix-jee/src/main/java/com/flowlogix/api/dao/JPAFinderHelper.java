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
package com.flowlogix.api.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.function.Supplier;

/**
 * Enhanced JPA Finder interface that provides access to the entity manager,
 * the entity class, and the ability to build a {@link QueryCriteria} record
 * <p>
 * Also contains convenience interface for use with {@link lombok.experimental.Delegate}
 *
 * @param <TT> entity type
 */
public interface JPAFinderHelper<TT> extends JPAFinder<TT> {
    /**
     * Entity Manager cannot be saved because it's not thread-safe
     * However, supplier can be returned
     *
     * @return {@link Supplier} of {@link EntityManager}
     */
    Supplier<EntityManager> getEntityManager();

    /**
     * Returns the entity class
     * @return entity class
     */
    Class<TT> getEntityClass();

    /**
     * Convenience method for building {@link QueryCriteria} record, which contains
     * {@link CriteriaBuilder}, {@link Root} and {@link CriteriaQuery}
     *
     * @return QueryCriteria of Entity Type
     */
    QueryCriteria<TT> buildQueryCriteria();

    /**
     * Convenience method for building {@link QueryCriteria} record of any type,
     * which contains {@link CriteriaBuilder}, {@link Root} and {@link CriteriaQuery}
     *
     * @param  cls Type of Query Criteria
     * @return QueryCriteria of the same Entity Type as the parameter
     */
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
