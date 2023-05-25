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
package com.flowlogix.jeedao.primefaces;

import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.JPAModelImpl.JPAModelImplBuilder;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.view.ViewScoped;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.omnifaces.util.Beans;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

/**
 * Easy implementation of PrimeFaces lazy data model using Lambdas, This is the main class in
 * {@link com.flowlogix.jeedao.primefaces} package
 * <p>
 * An instance of this class is to be put into your {@link ViewScoped} or {@link SessionScoped} beans
 * The implementation is serializable and works with failover to a different server instances
 * <p>
 * <em>Simple Example:</em>
 * {@snippet class = "com.flowlogix.examples.data.UserViewer" region = "simpleLazyDataModelUsage"}
 *
 * @author lprimak
 * @param <TT> Data Type
 * @param <KK> Key Type
 */
@Dependent
@Slf4j
public class JPALazyDataModel<TT, KK> extends LazyDataModel<TT> {
    public static final String RESULT = "result";
    private static final long serialVersionUID = 2L;
    @Delegate
    private transient JPAModelImpl<TT, KK> impl;
    @SuppressWarnings("serial")
    private Function<JPAModelImplBuilder<TT, KK>, JPAModelImpl<TT, KK>> builder;

    /**
     * Set up this particular instance of the data model
     * with entity manager, class and key converter
     *
     * @param <TT> Value Type
     * @param <KK> Key Type
     * @param <FF> serializable lambda for creation
     * @param builder
     * @return newly-created data model
     */
    public static <TT, KK, FF extends Function<JPAModelImplBuilder<TT, KK>,
        JPAModelImpl<TT, KK>> & Serializable> JPALazyDataModel<TT, KK> create(FF builder) {
        @SuppressWarnings("unchecked")
        JPALazyDataModel<TT, KK> model = Beans.getReference(JPALazyDataModel.class);
        model.builder = builder;
        model.impl = builder.apply(JPAModelImpl.builder());
        return model;
    }

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

    /**
     * Overridden from {@link LazyDataModel#getRowKey(Object)}
     */
    @Override
    public String getRowKey(TT key) {
        return impl.getKeyConverter().apply(key);
    }

    /**
     * Overridden from {@link LazyDataModel#getRowData(String)}
     */
    @Override
    @Transactional
    public TT getRowData(String rowKey) {
        return impl.getEntityManager().get().find(impl.getEntityClass(), impl.getConverter().apply(rowKey));
    }

    /**
     * Overridden from {@link LazyDataModel#load(int, int, Map, Map)}
     */
    @Override
    @Transactional
    public List<TT> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        return impl.findRows(first, pageSize, filterBy, sortBy);
    }

    /**
     * Overridden from {@link LazyDataModel#count(Map)}
     */
    @Override
    public int count(Map<String, FilterMeta> map) {
        return impl.count(map);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        impl = builder.apply(JPAModelImpl.builder());
    }
}
