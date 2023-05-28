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

import com.flowlogix.jeedao.primefaces.impl.JPAModelImpl;
import com.flowlogix.jeedao.primefaces.impl.JPAModelImpl.JPAModelImplBuilder;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.view.ViewScoped;
import jakarta.transaction.Transactional;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
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
 * <p>
 * userviewer.xhtml:
 * {@snippet class = "com.flowlogix.demo.jeedao.primefaces.BasicDataModel" region = "basicUsageHtml"}
 * {@snippet class = "com.flowlogix.demo.jeedao.primefaces.BasicDataModel" region = "basicUsage"}
 *
 * @author lprimak
 * @param <TT> Data Type
 * @param <KK> Key Type
 */
@Dependent
@Slf4j
public class JPALazyDataModel<TT, KK> extends LazyDataModel<TT> {
    /**
     * Automatic field that's added to the JPA's root object
     * and can be used with {@link #getResultField(String)} for result fields
     */
    public static final String RESULT = "result";
    private static final long serialVersionUID = 4L;
    @Delegate
    private JPAModelImpl<TT, KK> impl;

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
        model.impl.setX_do_not_use_in_builder(builder);
        return model;
    }

    /**
     * Serializable builder lambda to easily facilitate creation of {@link JPALazyDataModel}
     * @param <TT> Entity Type
     * @param <KK> Key Type
     */
    public interface BuilderFunction<TT, KK> extends Function<JPAModelImplBuilder<TT, KK>,
            JPAModelImpl<TT, KK>>, Serializable { }

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
        return impl.getEntityManager().get().find(impl.getEntityClass(), impl.getStringToKeyConverter().apply(rowKey));
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
}
