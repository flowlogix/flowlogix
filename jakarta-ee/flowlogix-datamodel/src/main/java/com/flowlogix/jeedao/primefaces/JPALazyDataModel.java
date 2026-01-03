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
package com.flowlogix.jeedao.primefaces;

import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.JPAModelImpl.JPAModelImplBuilder;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.SessionScoped;
import javax.faces.view.ViewScoped;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.omnifaces.util.Beans;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

/**
 * Easy implementation of PrimeFaces lazy data model
 * using Lambdas
 * <p>
 * An instance of this class is to be put into your {@link ViewScoped} or {@link SessionScoped} beans
 * The implementation is serializable and works with failover to a different server instances
 *
 * @author lprimak
 * @param <TT> Data Type
 * @param <KK> Key Type
 *
 * <p>
 * Example:
 * <pre>
 * {@code
 *     private @Getter final JPALazyDataModel<UserEntity, Long> lazyModel =
 *           JPALazyDataModel.create(builder -> builder
 *                   .entityManagerSupplier(() -> em)
 *                   .entityClass(UserEntity.class)
 *                   .converter(Long::parseLong)
 *                   .build());
 * }
 * </pre>
 */
@Dependent
@Slf4j
public class JPALazyDataModel<TT, KK> extends LazyDataModel<TT> {
    /**
     * Automatic field that's added to the JPA's root object
     * and can be used with {@link #getResultField(String)} for result fields
     */
    public static final String RESULT = "result";
    private static final long serialVersionUID = 3L;
    private JPAModelImpl<TT, KK> impl;
    @SuppressWarnings("serial")
    private Function<JPAModelImplBuilder<TT, KK, ?, ?>, JPAModelImpl<TT, KK>> builder;

    /**
     * Set up this particular instance of the data model
     * with entity manager, class and key converter
     *
     * @param <TT> Value Type
     * @param <KK> Key Type
     * @param builder serializable lambda for creation
     * @return newly-created data model
     */
    public static <TT, KK> JPALazyDataModel<TT, KK> create(BuilderFunction<TT, KK> builder) {
        @SuppressWarnings("unchecked")
        JPALazyDataModel<TT, KK> model = Beans.getReference(JPALazyDataModel.class);
        model.impl = builder.apply(JPAModelImpl.builder());
        model.impl.x_do_not_use_in_builder = builder;
        return model;
    }

    public interface BuilderFunction<TT, KK> extends Function<JPAModelImplBuilder<TT, KK, ?, ?>,
            JPAModelImpl<TT, KK>>, Serializable { }

    /**
     * Utility method for replacing a predicate in the filter list
     *
     * @param <TT> type of value
     * @param filters filter list
     * @param element element to be replace
     * @param fp lambda to get the new Filter predicate
     */
    @SuppressWarnings({"unchecked", "EmptyBlock"})
    public static <TT> void replaceFilter(Map<String, FilterData> filters, String element,
            BiFunction<Predicate, TT, Predicate> fp) {
        FilterData elt = filters.get(element);
        if (elt != null && elt.getFieldValue() != null) {
            if (elt.getFieldValue() instanceof String && isBlank((String) elt.getFieldValue())) {
                // do nothing if blank string
            } else {
                filters.replace(element, new FilterData(elt.getFieldValue(),
                        fp.apply(elt.getPredicate(), (TT) elt.getFieldValue())));
            }
        }
    }

    /**
     * transforms JPA entity field to format suitable for hints
     *
     * @param val
     * @return JPA field suitable for hints
     */
    public static String getResultField(String val) {
        return String.format("%s.%s", RESULT, val);
    }

    @Override
    public String getRowKey(TT key) {
        return impl.getKeyConverter().apply(key);
    }

    @Override
    @Transactional
    public TT getRowData(String rowKey) {
        return impl.getEntityManager().find(impl.getEntityClass(), impl.getConverter().apply(rowKey));
    }

    @Override
    @Transactional
    public List<TT> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        return impl.findRows(first, pageSize, filterBy, sortBy);
    }

    @Override
    public int count(Map<String, FilterMeta> map) {
        return impl.count(map);
    }
}
