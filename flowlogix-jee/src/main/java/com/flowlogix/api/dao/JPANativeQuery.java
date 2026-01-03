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

import com.flowlogix.jeedao.TypedNativeQuery;
import jakarta.persistence.EntityManager;

/**
 * Interface for creating type-safe JPA native queries
 *
 * @param <TT> entity type
 */
public interface JPANativeQuery<TT> extends JPAFinderHelper<TT> {
    /**
     * Creates a type-safe JPA native query
     * <p>
     * {@snippet class = "com.flowlogix.demo.jeedao.UserDAO" region = "nativeQuery"}
     *
     * @param sql
     * @param resultClass {@link EntityManager#createNativeQuery(String, Class)}
     * @return {@link TypedNativeQuery}
     */

    TypedNativeQuery createNativeQuery(String sql, Class<?> resultClass);
    /**
     * Creates a type-safe JPA native query
     * <p>
     * {@snippet class = "com.flowlogix.demo.jeedao.UserDAO" region = "nativeQuery"}
     *
     * @param sql
     * @param resultMapping {@link EntityManager#createNativeQuery(String, String)}
     * @return {@link TypedNativeQuery}
     */
    TypedNativeQuery createNativeQuery(String sql, String resultMapping);
}
