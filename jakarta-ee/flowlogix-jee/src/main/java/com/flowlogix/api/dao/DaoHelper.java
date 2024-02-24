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

import com.flowlogix.api.dao.DaoHelper.Parameters.ParametersBuilder;
import com.flowlogix.jeedao.InheritableDaoHelper;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Main value-add is ability to easily query enhancement criteria to
 * {@link #findAll()} and {@link #findRange(long, long)} methods,
 * as well as {@link #count()} methods
 * <p>
 * Another differentiator is that this class doesn't require inheritance,
 * although some use cases could inherit from {@link InheritableDaoHelper} class.
 * <p>
 * <em>Simple Example:</em>
 * {@snippet class="com.flowlogix.demo.jeedao.ExampleDAO" region="simpleExampleDAO"}
 * <p>
 * <em>Injected Example:</em>
 * {@snippet class="com.flowlogix.demo.jeedao.InjectedDAO" region="injectedExampleDAO"}
 * <p>
 * <em>Injected Example with non-default EntityManager:</em>
 * {@snippet class="com.flowlogix.demo.jeedao.InjectedNonDefaultDAO" region="injectedNonDefaultExampleDAO"}
 *
 * @param <TT> Entity Type
 *
 * @author lprimak
 */
public interface DaoHelper<TT> {
    /**
     * QueryCriteria record contains {@link CriteriaBuilder}, {@link Root} and {@link CriteriaQuery}
     * @param <TT> Entity Type of Criteria
     * @param builder
     * @param root
     * @param query
     */
    record QueryCriteria<TT>(CriteriaBuilder builder, Root<TT> root, CriteriaQuery<TT> query) {
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
    record CountQueryCriteria<TT>(CriteriaBuilder builder, Root<TT> root, CriteriaQuery<Long> query) {
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
     * {@snippet class = "com.flowlogix.demo.jeedao.UserDAO" region = "daoParameters"}
     *
     * @param builder
     * @param root
     * @param <TT>
     */
    record PartialQueryCriteria<TT>(CriteriaBuilder builder, Root<TT> root) { }

    /**
     * Convenience interface for use with {@link PartialQueryCriteria}
     * <p>
     * {@snippet class = "com.flowlogix.demo.jeedao.UserDAO" region = "daoParameters"}

     * @param <TT> Entity Type
     */
    interface QueryEnhancement<TT> extends BiConsumer<PartialQueryCriteria<TT>, CriteriaQuery<?>> {
        /**
         * Convenience method for creating parameters to
         * {@link #count(Function)} and {@link #findAll(Function)} methods and friends.
         * Useful when the same enhanced queries are used for both count() and find() methods.
         *
         * @param builder
         * @return
         */
        default com.flowlogix.jeedao.DaoHelper.Parameters<TT> build(ParametersBuilder<TT> builder) {
            return builder.queryCriteria(this::accept).countQueryCriteria(this::accept)
                    .build();
        }

        /**
         * Convenience method for using {@link com.flowlogix.jeedao.DaoHelper.Parameters#queryCriteria} parameters
         * @param criteria
         */
        default void accept(QueryCriteria<TT> criteria) {
            accept(criteria.partial(), criteria.query());
        }

        /**
         * Convenience method for using {@link com.flowlogix.jeedao.DaoHelper.Parameters#countQueryCriteria} parameters
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
     * {@snippet class = "com.flowlogix.demo.jeedao.UserDAO" region = "daoParameters"}
     */
    @FunctionalInterface
    interface ParameterFunction<TT> extends Function<ParametersBuilder<TT>, com.flowlogix.jeedao.DaoHelper.Parameters<TT>> { }

    /**
     * Parameters for enriching
     * {@link #count(Function)}, {@link #findAll(Function)} and {@link #findRange(long, long, Function)}
     * methods with additional criteria
     * <p>
     * {@snippet class = "com.flowlogix.demo.jeedao.UserDAO" region = "daoParameters"}
     *
     * @param <TT> Entity Type
     */
    @Builder
    @Getter
    class Parameters<TT> {
        /**
         * query criteria enhancement
         */
        @Builder.Default
        @NonNull
        private final Consumer<QueryCriteria<TT>> queryCriteria = c -> { };
        /**
         * query criteria enhancement for count operation here
         */
        @Builder.Default
        @NonNull
        private final Consumer<CountQueryCriteria<TT>> countQueryCriteria = c -> { };

        /**
         * @hidden
         * just for javadoc
         * @param <TT>
         */
        public static class ParametersBuilder<TT> { }
    }

    TypedQuery<TT> findAll();
    <FF extends Function<ParametersBuilder<TT>, Parameters<TT>>> TypedQuery<TT> findAll(FF paramsBuilder);
    TypedQuery<TT> findRange(long min, long max);
    <FF extends Function<ParametersBuilder<TT>, Parameters<TT>>> TypedQuery<TT> findRange(long min, long max, FF paramsBuilder);

    long count();
    <FF extends Function<ParametersBuilder<TT>, Parameters<TT>>> long count(FF paramsBuilder);
    QueryCriteria<TT> buildQueryCriteria();
    <RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls);

}
