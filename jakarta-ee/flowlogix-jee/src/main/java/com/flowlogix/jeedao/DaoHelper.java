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
package com.flowlogix.jeedao;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.flowlogix.api.dao.JPAFinder;
import com.flowlogix.api.dao.JPAFinderNative;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Delegate;
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
public final class DaoHelper<TT> implements JPAFinderNative<TT>, Serializable {
    private static final long serialVersionUID = 5L;

    /**
     * Convenience interface for use with {@link Delegate} when forwarding methods
     * of {@link EntityManager} so DaoHelper's own methods get exposed correctly
     * <p>
     * {@snippet class = "com.flowlogix.demo.jeedao.ExampleDelegateDAO" region = "delegateDAO"}
     */
    public interface EntityManagerExclusions {
        Query createNativeQuery(String sql, Class resultClass);
        Query createNativeQuery(String sql, String resultMapping);
    }

    /**
     * Return entity manager to operate on
     */
    private final @NonNull SerializableSupplier<EntityManager> entityManager;
    /**
     * entity class
     */
    private final @NonNull @Getter(onMethod = @__(@Override)) Class<TT> entityClass;

    @Builder
    public DaoHelper(@NonNull SerializableSupplier<EntityManager> entityManager, @NonNull Class<TT> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    @Override
    public TypedQuery<TT> findAll() {
        return findAll(null);
    }

    /**
     * find all with enriched criteria
     * <p>
     * Example:
     * <p>
     * {@code findAll(builder -> builder.build())}
     * <p>
     * {@snippet class = "com.flowlogix.demo.jeedao.UserDAO" region = "daoParameters"}
     *
     * @param queryCriteria
     * @return query
     */
    @Override
    public TypedQuery<TT> findAll(Consumer<QueryCriteria<TT>> queryCriteria) {
        return createFindQuery(queryCriteria);
    }

    @Override
    public TypedQuery<TT> findRange(long min, long max) {
        return findRange(min, max, null);
    }

    /**
     * find range with enriched criteria
     *
     * @param min
     * @param max
     * @param queryCriteria
     * @return query
     */
    @Override
    public TypedQuery<TT> findRange(long min, long max, Consumer<QueryCriteria<TT>> queryCriteria) {
        TypedQuery<TT> tq = createFindQuery(queryCriteria);
        tq.setMaxResults(toIntExact(max - min));
        tq.setFirstResult(toIntExact(min));
        return tq;
    }

    /**
     * count rows
     * @return row count
     */
    @Override
    public long count() {
        return count(null);
    }

    /**
     * count with enriched criteria
     * @param countQueryCriteria
     * @return row count
     */
    @Override
    public long count(Consumer<CountQueryCriteria<TT>> countQueryCriteria) {
        var criteriaBuilder = em().getCriteriaBuilder();
        CriteriaQuery<Long> cq = criteriaBuilder.createQuery(Long.class);
        Root<TT> rt = cq.from(entityClass);
        cq.select(criteriaBuilder.count(rt));
        Objects.requireNonNullElse(countQueryCriteria, c -> { }).accept(new CountQueryCriteria<>(criteriaBuilder, rt, cq));
        TypedQuery<Long> q = em().createQuery(cq);
        return q.getSingleResult();
    }

    /**
     * Entity Manager cannot be saved because it's not thread-safe
     *
     * @return {@link Supplier} of {@link EntityManager}
     */
    @Override
    public Supplier<EntityManager> getEntityManager() {
        return entityManager;
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
     * Convenience method for building {@link QueryCriteria} record, which contains
     * {@link CriteriaBuilder}, {@link Root} and {@link CriteriaQuery}
     *
     * @return QueryCriteria of Entity Type
     */
    @Override
    public QueryCriteria<TT> buildQueryCriteria() {
        return buildQueryCriteria(entityClass);
    }

    /**
     * Convenience method for building {@link QueryCriteria} record, which contains
     * {@link CriteriaBuilder}, {@link Root} and {@link CriteriaQuery}
     *
     * @param  cls Type of Query Criteria
     * @return QueryCriteria of the same Entity Type as the parameter
     */
    @Override
    public <RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls) {
        CriteriaBuilder cb = em().getCriteriaBuilder();
        CriteriaQuery<RR> cq = cb.createQuery(cls);
        return new QueryCriteria<>(cb, cq.from(cls), cq);
    }

    /**
     * Creates a type-safe JPA native query
     * <p>
     * {@snippet class = "com.flowlogix.demo.jeedao.UserDAO" region = "nativeQuery"}
     *
     * @param sql
     * @param resultClass {@link EntityManager#createNativeQuery(String, Class)}
     * @return {@link TypedNativeQuery}
     */
    @Override
    public TypedNativeQuery createNativeQuery(String sql, Class<?> resultClass) {
        Query q = em().createNativeQuery(sql, resultClass);
        return new TypedNativeQuery(q);
    }

    /**
     * Creates a type-safe JPA native query
     * <p>
     * {@snippet class = "com.flowlogix.demo.jeedao.UserDAO" region = "nativeQuery"}
     *
     * @param sql
     * @param resultMapping {@link EntityManager#createNativeQuery(String, String)}
     * @return {@link TypedNativeQuery}
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
        Objects.requireNonNullElse(queryCriteria, c -> { }).accept(new QueryCriteria<>(qc.builder(), qc.root(), qc.query()));
        return em().createQuery(qc.query());
    }
}
