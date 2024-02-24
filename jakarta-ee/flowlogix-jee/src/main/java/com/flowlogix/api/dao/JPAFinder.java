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

import com.flowlogix.jeedao.InheritableDaoHelper;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
public interface JPAFinder<TT> {
    TypedQuery<TT> findAll();
    TypedQuery<TT> findAll(Consumer<QueryCriteria<TT>> queryCriteria);
    TypedQuery<TT> findRange(long min, long max);
    TypedQuery<TT> findRange(long min, long max, Consumer<QueryCriteria<TT>> queryCriteria);

    long count();
    long count(Consumer<CountQueryCriteria<TT>> countQueryCriteria);

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

    QueryCriteria<TT> buildQueryCriteria();
    <RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls);
}
