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

import com.flowlogix.jeedao.querycriteria.QueryCriteria;
import com.flowlogix.jeedao.querycriteria.CountQueryCriteria;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Lightweight wrapper around common JPA methods
 * This is the primary class in the {@link com.flowlogix.jeedao} package
 * <p>
 * Main value-add is ability to easily add hints and query criteria to
 * {@link #findAll()} and {@link #findRange(int, int)} methods,
 * as well as {@link #count()} methods
 * <p>
 * Another differentiator is that this class doesn't require inheritance,
 * although some use cases could inherit from {@link InheritableDaoHelper} class.
 *
 * @param <TT> Entity Type
 * @param <KT> Primary Key Type
 *
 * @author lprimak
 */
@SuperBuilder
@RequiredArgsConstructor
public final class DaoHelper<TT, KT> {
    /**
     * Return entity manager to operate on
     */
    private final @NonNull Supplier<EntityManager> entityManagerSupplier;
    /**
     * entity class
     */
    private final @NonNull @Getter Class<TT> entityClass;

    @Builder
    public static class Parameters<TT> {
        /**
         * add hints to queries here
         */
        @Builder.Default
        private final Consumer<TypedQuery<TT>> hints = (tq) -> { };
        /**
         * add query criteria here
         */
        @Builder.Default
        private final Consumer<QueryCriteria<TT>> queryCriteria = (c) -> { };
        /**
         * add query criteria to count operation here
         */
        @Builder.Default
        private final Consumer<CountQueryCriteria<TT>> countQueryCriteria = (c) -> { };
    }

    public List<TT> findAll() {
        return findAll(builder -> builder.build());
    }

    /**
     * find all with added criteria and hints
     * <p>
     * Example:
     * <p>
     * {@code findAll(builder -> builder.build())}
     *
     * @param paramsBuilder
     * @return
     */
    public <FF extends Function<Parameters.ParametersBuilder<TT>, Parameters<TT>>> List<TT> findAll(FF paramsBuilder) {
        var params = paramsBuilder.apply(Parameters.builder());
        TypedQuery<TT> tq = createFindQuery(params);
        params.hints.accept(tq);
        return tq.getResultList();
    }

    public List<TT> findRange(int min, int max) {
        return findRange(min, max, builder -> builder.build());
    }

    /**
     * find range with added criteria and hints
     *
     * @param min
     * @param max
     * @param paramsBuilder
     * @return
     */
    public <FF extends Function<Parameters.ParametersBuilder<TT>, Parameters<TT>>>
    List<TT> findRange(int min, int max, FF paramsBuilder) {
        var params = paramsBuilder.apply(Parameters.builder());
        TypedQuery<TT> tq = createFindQuery(params);
        tq.setMaxResults(max - min);
        tq.setFirstResult(min);
        params.hints.accept(tq);
        return tq.getResultList();
    }

    public int count() {
        return count(builder -> builder.build());
    }

    public <FF extends Function<Parameters.ParametersBuilder<TT>, Parameters<TT>>>
    int count(FF paramsBuilder) {
        var params = paramsBuilder.apply(Parameters.builder());
        CriteriaQuery<Long> cq = getEntityManager().getCriteriaBuilder().createQuery(Long.class);
        Root<TT> rt = cq.from(entityClass);
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        params.countQueryCriteria.accept(new CountQueryCriteria<>(getEntityManager().getCriteriaBuilder(), rt, cq));
        TypedQuery<Long> q = getEntityManager().createQuery(cq);
        return q.getSingleResult().intValue();
    }

    /**
     * @return entity manager
     */
    public EntityManager getEntityManager() {
        return entityManagerSupplier.get();
    }

    public QueryCriteria<TT> buildQueryCriteria() {
        return buildQueryCriteria(entityClass);
    }

    public <RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<RR> cq = cb.createQuery(cls);
        return new QueryCriteria<>(cb, cq.from(cls), cq);
    }

    public TypedNativeQuery createNativeQuery(String sql, Class<?> resultClass) {
        Query q = getEntityManager().createNativeQuery(sql, resultClass);
        return new TypedNativeQuery(q);
    }

    public TypedNativeQuery createNativeQuery(String sql, String resultMapping) {
        Query q = getEntityManager().createNativeQuery(sql, resultMapping);
        return new TypedNativeQuery(q);
    }

    private TypedQuery<TT> createFindQuery(Parameters<TT> params) {
        CriteriaQuery<TT> cq = getEntityManager().getCriteriaBuilder().createQuery(entityClass);
        Root<TT> root = cq.from(entityClass);
        cq.select(root);
        params.queryCriteria.accept(new QueryCriteria<>(getEntityManager().getCriteriaBuilder(), root, cq));
        return getEntityManager().createQuery(cq);
    }
}
