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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
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
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Delegate;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Lazy.SerializableSupplier;

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
 * <p>
 * <em>Simple Example:</em>
 * {@snippet class="com.flowlogix.jeedao.ExampleDAO" region="simpleExampleDAO"}
 * <p>
 * <em>Injected Example:</em>
 * {@snippet class="com.flowlogix.jeedao.InjectedDAO" region="injectedExampleDAO"}
 * <p>
 * <em>Injected Example with non-default EntityManager:</em>
 * {@snippet class="com.flowlogix.jeedao.InjectedNonDefaultDAO" region="injectedNonDefaultExampleDAO"}
 *
 * @param <TT> Entity Type
 *
 * @author lprimak
 */
public final class DaoHelper<TT> implements Serializable {
    private static final long serialVersionUID = 3L;

    public record QueryCriteria<TT>(CriteriaBuilder builder, Root<TT> root, CriteriaQuery<TT> query) { }
    public record CountQueryCriteria<TT>(CriteriaBuilder builder, Root<TT> root, CriteriaQuery<Long> query) { }

    /**
     * Convenience interface for use with {@link Delegate} when forwarding methods
     * of {@link EntityManager} so DaoHelper's own methods get exposed correctly
     * <p>
     * {@snippet class = "com.flowlogix.jeedao.ExampleDelegateDAO" region = "delegateDAO"}
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
    private final @NonNull @Getter Class<TT> entityClass;

    @Builder
    public DaoHelper(@NonNull SerializableSupplier<EntityManager> entityManager, @NonNull Class<TT> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    @Builder
    public static class Parameters<TT> {
        /**
         * add hints to queries here
         */
        @Default
        private final Consumer<TypedQuery<TT>> hints = (tq) -> { };
        /**
         * add query criteria here
         */
        @Default
        private final Consumer<QueryCriteria<TT>> queryCriteria = (c) -> { };
        /**
         * add query criteria to count operation here
         */
        @Default
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
        CriteriaQuery<Long> cq = em().getCriteriaBuilder().createQuery(Long.class);
        Root<TT> rt = cq.from(entityClass);
        cq.select(em().getCriteriaBuilder().count(rt));
        params.countQueryCriteria.accept(new CountQueryCriteria<>(em().getCriteriaBuilder(), rt, cq));
        TypedQuery<Long> q = em().createQuery(cq);
        return q.getSingleResult().intValue();
    }

    /**
     * Entity Manager cannot be saved because it's not thread-safe
     *
     * @return {@link Supplier} of {@link EntityManager}
     */
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

    public QueryCriteria<TT> buildQueryCriteria() {
        return buildQueryCriteria(entityClass);
    }

    public <RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls) {
        CriteriaBuilder cb = em().getCriteriaBuilder();
        CriteriaQuery<RR> cq = cb.createQuery(cls);
        return new QueryCriteria<>(cb, cq.from(cls), cq);
    }

    public TypedNativeQuery createNativeQuery(String sql, Class<?> resultClass) {
        Query q = em().createNativeQuery(sql, resultClass);
        return new TypedNativeQuery(q);
    }

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


    private TypedQuery<TT> createFindQuery(Parameters<TT> params) {
        CriteriaQuery<TT> cq = em().getCriteriaBuilder().createQuery(entityClass);
        Root<TT> root = cq.from(entityClass);
        cq.select(root);
        params.queryCriteria.accept(new QueryCriteria<>(em().getCriteriaBuilder(), root, cq));
        return em().createQuery(cq);
    }
}
