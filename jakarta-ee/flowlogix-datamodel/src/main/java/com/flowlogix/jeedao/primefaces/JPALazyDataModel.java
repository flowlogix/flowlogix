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
package com.flowlogix.jeedao.primefaces;

import com.flowlogix.jeedao.primefaces.internal.JPAModelImpl;
import com.flowlogix.jeedao.primefaces.internal.JPAModelImpl.BuilderInitializer;
import com.flowlogix.jeedao.primefaces.internal.JPAModelImpl.JPAModelImplBuilder;
import com.flowlogix.jeedao.primefaces.internal.InternalQualifierJPALazyModel;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.view.ViewScoped;
import jakarta.transaction.Transactional;
import lombok.NonNull;
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
 * <p>
 * <em>Direct Creation Example:</em>
 * {@snippet class = "com.flowlogix.demo.jeedao.primefaces.DirectCreationDataModel" region = "basicUsage"}

 * @author lprimak
 * @param <TT> Data Type
 */
@Dependent
@Slf4j
@InternalQualifierJPALazyModel
public class JPALazyDataModel<TT> extends LazyDataModel<TT> {
    /**
     * Automatic field that's added to the JPA's root object
     * and can be used with {@link #getResultField(String)} for result fields
     */
    public static final String RESULT = "result";
    private static final long serialVersionUID = 4L;
    @Delegate
    private JPAModelImpl<TT> impl;
    private transient PartialBuilderConsumer<TT> partialBuilder;

    /**
     * Prevent direct creation
     */
    JPALazyDataModel() { }

    /**
     * Set up this particular instance of the data model
     * with entity manager, class and key converter
     *
     * @param <TT> Value Type
     * @param builder serializable lambda for creation
     * @return newly-created data model
     */
    public static <TT> JPALazyDataModel<TT> create(@NonNull BuilderFunction<TT> builder) {
        @SuppressWarnings("unchecked")
        JPALazyDataModel<TT> model = Beans.getReference(JPALazyDataModel.class, InternalQualifierJPALazyModel.LITERAL);
        return model.initialize(builder);
    }

    /**
     * used in case-insensitive queries to convert case to lower / upper case
     */
    @SuppressWarnings("checkstyle:JavadocVariable")
    public enum FilterCaseConversion {
        UPPER, LOWER
    }

    JPALazyDataModel<TT> partialInitialize(@NonNull PartialBuilderConsumer<TT> builder) {
        if (partialBuilder != null) {
            throw new IllegalStateException("partial builder already initialized");
        }
        partialBuilder = builder;
        return initialize(JPAModelImplBuilder::build, false);
    }

    /**
     * Initialize JPA Lazy Data model. Used to set parameters to already-injected instance
     *
     * @param builder serializable lambda for creation
     * @return current instance for fluent operations
     */
    public JPALazyDataModel<TT> initialize(@NonNull BuilderFunction<TT> builder) {
        return initialize(builder, true);
    }

    /**
     * Serializable builder lambda to easily facilitate creation of {@link JPALazyDataModel}
     * @param <TT> Entity Type
     */
    public interface BuilderFunction<TT> extends Function<JPAModelImplBuilder<TT>,
            JPAModelImpl<TT>>, Serializable { }

    /**
     * Internal - do not use
     *
     * @hidden
     * @param <TT>
     */
    public interface PartialBuilderConsumer<TT> extends Consumer<JPAModelImplBuilder<TT>>, Serializable { }

    /**
     * Transforms JPA entity field to format suitable for hints.
     * Used with {@code JPAModelImplBuilder#optimizer(Function)} method
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

    private JPALazyDataModel<TT> initialize(BuilderFunction<TT> builder, boolean resetPartialBuilder) {
        impl = JPAModelImpl.create(new BuilderInitializer<>(builder, partialBuilder));
        impl.setX_do_not_use_in_builder(new BuilderInitializer<>(builder, partialBuilder));
        if (resetPartialBuilder) {
            partialBuilder = null;
        }
        return this;
    }
}
