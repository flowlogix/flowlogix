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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import com.flowlogix.jeedao.DaoHelper.Parameters.ParametersBuilder;
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
import static java.lang.Math.toIntExact;

/**
 * Lightweight wrapper around common JPA methods
 * This is the primary class in the {@link com.flowlogix.jeedao} package
 * <p>
 * Main value-add is ability to easily query enhancement criteria to
 * {@link #findAll()} and {@link #findRange(long, long)} methods,
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

    /**
     * QueryCriteria record contains {@link CriteriaBuilder}, {@link Root} and {@link CriteriaQuery}
     * @param <TT> Entity Type of Criteria
     * @param builder
     * @param root
     * @param query
     */
    public record QueryCriteria<TT>(CriteriaBuilder builder, Root<TT> root, CriteriaQuery<TT> query) {
        /**
         * @return partial query criteria, without the JPA {@link CriteriaQuery} object
         */
        public PartialQueryCriteria<TT> partial() {
            return new PartialQueryCriteria<>(builder, root);
        }
    }
    /**
     * Specialized <b>Count</b>QueryCriteria record contains
     * {@link CriteriaBuilder}, {@link Root} and {@link CriteriaQuery}{@code <Long>}
     * @param <TT> Entity Type of Criteria
     * @param builder
     * @param root
     * @param query
     */
    public record CountQueryCriteria<TT>(CriteriaBuilder builder, Root<TT> root, CriteriaQuery<Long> query) {
        /**
         * @return partial query criteria, without the JPA {@link CriteriaQuery} object
         */
        public PartialQueryCriteria<TT> partial() {
            return new PartialQueryCriteria<>(builder, root);
        }
    }

    /**
     * Partial query criteria, only {@link CriteriaBuilder} and {@link Root}
     * Used for common enhancing query methods / lambdas
     * <p>
     * {@snippet class = "com.flowlogix.jeedao.UserDAO" region = "daoParameters"}
     *
     * @param builder
     * @param root
     * @param <TT>
     */
    public record PartialQueryCriteria<TT>(CriteriaBuilder builder, Root<TT> root) { }

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
     * Convenience interface for use with {@link PartialQueryCriteria}
     * <p>
     * {@snippet class = "com.flowlogix.jeedao.UserDAO" region = "daoParameters"}

     * @param <TT> Entity Type
     */
    public interface QueryEnhancement<TT> extends BiConsumer<PartialQueryCriteria<TT>, CriteriaQuery<?>> {
        /**
         * Convenience method for creating parameters to
         * {@link #count(Function)} and {@link #findAll(Function)} methods and friends.
         * Useful when the same enhanced queries are used for both count() and find() methods.
         *
         * @param builder
         * @return
         */
        default Parameters<TT> build(ParametersBuilder<TT> builder) {
            return builder.queryCriteria(this::accept).countQueryCriteria(this::accept)
                    .build();
        }

        /**
         * Convenience method for using {@link Parameters#queryCriteria} parameters
         * @param criteria
         */
        default void accept(QueryCriteria<TT> criteria) {
            accept(criteria.partial(), criteria.query());
        }

        /**
         * Convenience method for using {@link Parameters#countQueryCriteria} parameters
         * @param criteria
         */
        default void accept(CountQueryCriteria<TT> criteria) {
            accept(criteria.partial(), criteria.query());
        }

        /**
         * Allows for combinations of enhancements via method references
         * @see BiConsumer#andThen(BiConsumer)
         *
         * @param after
         * @return combination lambda
         */
        default QueryEnhancement<TT> andThen(QueryEnhancement<TT> after) {
            return (l, r) -> BiConsumer.super.andThen(after).accept(l, r);
        }
    }

    /**
     * Convenience interface to extract parameter builder into a lambda
     * @param <TT> Entity Type
     * <p>
     * {@snippet class = "com.flowlogix.jeedao.UserDAO" region = "daoParameters"}
     */
    @FunctionalInterface
    public interface ParameterFunction<TT> extends Function<ParametersBuilder<TT>, Parameters<TT>> { }

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

    /**
     * Parameters for enriching
     * {@link #count(Function)}, {@link #findAll(Function)} and {@link #findRange(long, long, Function)}
     * methods with additional criteria
     * <p>
     * {@snippet class = "com.flowlogix.jeedao.UserDAO" region = "daoParameters"}
     *
     * @param <TT> Entity Type
     */
    @Builder
    public static class Parameters<TT> {
        /**
         * query criteria enhancement
         */
        @Default
        @NonNull
        private final Consumer<QueryCriteria<TT>> queryCriteria = c -> { };
        /**
         * query criteria enhancement for count operation here
         */
        @Default
        @NonNull
        private final Consumer<CountQueryCriteria<TT>> countQueryCriteria = c -> { };
    }

    public TypedQuery<TT> findAll() {
        return findAll(builder -> builder.build());
    }

    /**
     * find all with enriched criteria
     * <p>
     * Example:
     * <p>
     * {@code findAll(builder -> builder.build())}
     * <p>
     * {@snippet class = "com.flowlogix.jeedao.UserDAO" region = "daoParameters"}
     *
     * @param paramsBuilder
     * @return query
     */
    public <FF extends Function<ParametersBuilder<TT>, Parameters<TT>>> TypedQuery<TT>
    findAll(FF paramsBuilder) {
        var params = paramsBuilder.apply(Parameters.builder());
        return createFindQuery(params);
    }

    public TypedQuery<TT> findRange(long min, long max) {
        return findRange(min, max, builder -> builder.build());
    }

    /**
     * find range with enriched criteria
     *
     * @param min
     * @param max
     * @param paramsBuilder
     * @return query
     */
    public <FF extends Function<ParametersBuilder<TT>, Parameters<TT>>>
    TypedQuery<TT> findRange(long min, long max, FF paramsBuilder) {
        var params = paramsBuilder.apply(Parameters.builder());
        TypedQuery<TT> tq = createFindQuery(params);
        tq.setMaxResults(toIntExact(max - min));
        tq.setFirstResult(toIntExact(min));
        return tq;
    }

    /**
     * count rows
     * @return row count
     */
    public long count() {
        return count(builder -> builder.build());
    }

    /**
     * count with enriched criteria
     * @param paramsBuilder
     * @return row count
     */
    public <FF extends Function<ParametersBuilder<TT>, Parameters<TT>>>
    long count(FF paramsBuilder) {
        var params = paramsBuilder.apply(Parameters.builder());
        var criteriaBuilder = em().getCriteriaBuilder();
        CriteriaQuery<Long> cq = criteriaBuilder.createQuery(Long.class);
        Root<TT> rt = cq.from(entityClass);
        cq.select(criteriaBuilder.count(rt));
        params.countQueryCriteria.accept(new CountQueryCriteria<>(criteriaBuilder, rt, cq));
        TypedQuery<Long> q = em().createQuery(cq);
        return q.getSingleResult();
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

    /**
     * Convenience method for building {@link QueryCriteria} record, which contains
     * {@link CriteriaBuilder}, {@link Root} and {@link CriteriaQuery}
     *
     * @return QueryCriteria of Entity Type
     */
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
    public <RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls) {
        CriteriaBuilder cb = em().getCriteriaBuilder();
        CriteriaQuery<RR> cq = cb.createQuery(cls);
        return new QueryCriteria<>(cb, cq.from(cls), cq);
    }

    /**
     * Creates a type-safe JPA native query
     * <p>
     * {@snippet class = "com.flowlogix.jeedao.UserDAO" region = "nativeQuery"}
     *
     * @param sql
     * @param resultClass {@link EntityManager#createNativeQuery(String, Class)}
     * @return {@link TypedNativeQuery}
     */
    public TypedNativeQuery createNativeQuery(String sql, Class<?> resultClass) {
        Query q = em().createNativeQuery(sql, resultClass);
        return new TypedNativeQuery(q);
    }

    /**
     * Creates a type-safe JPA native query
     * <p>
     * {@snippet class = "com.flowlogix.jeedao.UserDAO" region = "nativeQuery"}
     *
     * @param sql
     * @param resultMapping {@link EntityManager#createNativeQuery(String, String)}
     * @return {@link TypedNativeQuery}
     */
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
        var qc = buildQueryCriteria();
        qc.query().select(qc.root());
        params.queryCriteria.accept(new QueryCriteria<>(qc.builder(), qc.root(), qc.query()));
        return em().createQuery(qc.query());
    }
}
