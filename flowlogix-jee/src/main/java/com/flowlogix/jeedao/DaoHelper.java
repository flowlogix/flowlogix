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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.flowlogix.api.dao.JPAFinder;
import com.flowlogix.api.dao.JPANativeQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.Builder;
import lombok.NonNull;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Lazy.SerializableSupplier;
import static java.lang.Math.toIntExact;

/**
 * Lightweight wrapper around common JPA methods
 * This is the primary class in the {@link com.flowlogix.jeedao} package
 * Implementation of {@link JPAFinder} interface
 *
 * @param <TT>
 */
public final class DaoHelper<TT> implements JPANativeQuery<TT>, Serializable {
    private static final long serialVersionUID = 5L;

    /**
     * Return entity manager to operate on
     */
    private final @NonNull SerializableSupplier<EntityManager> entityManager;
    /**
     * entity class
     */
    private final @NonNull Class<TT> entityClass;

    @Builder
    public DaoHelper(@NonNull SerializableSupplier<EntityManager> entityManager, @NonNull Class<TT> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypedQuery<TT> findAll() {
        return findAll(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypedQuery<TT> findAll(Consumer<QueryCriteria<TT>> queryCriteria) {
        return createFindQuery(queryCriteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypedQuery<TT> findRange(long min, long max) {
        return findRange(min, max, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypedQuery<TT> findRange(long min, long max, Consumer<QueryCriteria<TT>> queryCriteria) {
        TypedQuery<TT> tq = createFindQuery(queryCriteria);
        tq.setMaxResults(toIntExact(max - min));
        tq.setFirstResult(toIntExact(min));
        return tq;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count() {
        return count(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count(Consumer<CountQueryCriteria<TT>> countQueryCriteria) {
        var criteriaBuilder = em().getCriteriaBuilder();
        CriteriaQuery<Long> cq = criteriaBuilder.createQuery(Long.class);
        Root<TT> rt = cq.from(entityClass);
        cq.select(criteriaBuilder.count(rt));
        if (countQueryCriteria != null) {
            countQueryCriteria.accept(new CountQueryCriteria<>(criteriaBuilder, rt, cq));
        }
        TypedQuery<Long> q = em().createQuery(cq);
        return q.getSingleResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Supplier<EntityManager> getEntityManager() {
        return entityManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<TT> getEntityClass() {
        return entityClass;
    }

    /**
     * Do not make this public because entity manager is not thread-safe
     *
     * @return Entity Manager
     */
    EntityManager em() {
        return entityManager.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryCriteria<TT> buildQueryCriteria() {
        return buildQueryCriteria(entityClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls) {
        CriteriaBuilder cb = em().getCriteriaBuilder();
        CriteriaQuery<RR> cq = cb.createQuery(cls);
        return new QueryCriteria<>(cb, cq.from(cls), cq);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypedNativeQuery createNativeQuery(String sql, Class<?> resultClass) {
        Query q = em().createNativeQuery(sql, resultClass);
        return new TypedNativeQuery(q);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypedNativeQuery createNativeQuery(String sql, String resultMapping) {
        Query q = em().createNativeQuery(sql, resultMapping);
        return new TypedNativeQuery(q);
    }

    /**
     * Finds a reference to entity manager via CDI
     *
     * @return {@link SerializableSupplier} of default {@link EntityManager}
     */
    public static SerializableSupplier<EntityManager> findEntityManager() {
        return findEntityManager(List.of());
    }

    /**
     * Finds a reference to entity manager via CDI
     *
     * @param qualifiers for the entity manager, or empty list
     * @return {@link SerializableSupplier} of {@link EntityManager}
     */
    public static SerializableSupplier<EntityManager>
    findEntityManager(@NonNull List<Class<? extends Annotation>> qualifiers) {
        var qualifierInstances = qualifiers.stream().map(value -> (Annotation) () -> value).toList();
        return () -> Optional.ofNullable(Beans.getReference(EntityManager.class,
                qualifierInstances.toArray(Annotation[]::new))).orElseThrow(() -> new IllegalStateException(
                String.format("Unable to find EntityManager with qualifiers: %s",
                        qualifierInstances.stream().map(Annotation::annotationType).toList())));
    }


    private TypedQuery<TT> createFindQuery(Consumer<QueryCriteria<TT>> queryCriteria) {
        var qc = buildQueryCriteria();
        qc.query().select(qc.root());
        if (queryCriteria != null) {
            queryCriteria.accept(new QueryCriteria<>(qc.builder(), qc.root(), qc.query()));
        }
        return em().createQuery(qc.query());
    }
}
