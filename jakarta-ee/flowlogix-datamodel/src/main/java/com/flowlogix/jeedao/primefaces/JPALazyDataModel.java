/*
 * Copyright 2014 lprimak.
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

import com.flowlogix.jeedao.primefaces.interfaces.ModelBuilder;
import com.flowlogix.jeedao.primefaces.internal.JPAModelImpl;
import com.flowlogix.jeedao.primefaces.support.FilterData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.SessionScoped;
import javax.faces.view.ViewScoped;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
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
 *           JPALazyDataModel.createModel(builder -> builder
 *                   .entityManagerSupplier(() -> em)
 *                   .entityClass(UserEntity.class)
 *                   .converter(Long::parseLong)
 *                   .build());
 * }
 * </pre>
 */
@Dependent
public class JPALazyDataModel<TT, KK> extends LazyDataModel<TT> {
    public static final String RESULT = "result";
    private static final long serialVersionUID = 2L;
    private transient JPAModelImpl<TT, KK> impl;
    private ModelBuilder<TT, KK> builder;

    /**
     * Set up this particular instance of the data model
     * with entity manager, class and key converter
     *
     * @param <TT> Value Type
     * @param <KK> Key Type
     * @param builder
     * @return newly-created data model
     */
    public static<TT, KK> JPALazyDataModel<TT, KK> create(ModelBuilder<TT, KK> builder)
    {
        @SuppressWarnings("unchecked")
        JPALazyDataModel<TT, KK> model = Beans.getReference(JPALazyDataModel.class);
        model.builder = builder;
        model.impl = builder.build(JPAModelImpl.builder());
        return model;
    }

    /**
     * Utility method for replacing a predicate in the filter list
     *
     * @param filters filter list
     * @param element element to be replace
     * @param fp lambda to get the new Filter predicate
     */
    public static void replaceFilter(Map<String, FilterData> filters, String element,
            BiFunction<Predicate, Object, Predicate> fp)
    {
        FilterData elt = filters.get(element);
        if (elt != null && StringUtils.isNotBlank(elt.getFieldValue()))
        {
            filters.replace(element, new FilterData(elt.getFieldValue(),
                    fp.apply(elt.getPredicate(), elt.getFieldValue())));
        }
    }

    /**
     * transforms JPA entity field to format suitable for hints
     *
     * @param val
     * @return JPA field suitable for hints
     */
    public static String getResultField(String val)
    {
        return String.format("%s.%s", RESULT, val);
    }


    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public KK getRowKey(TT entity)
    {
        return (KK)impl.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
    }


    @Override
    @Transactional
    public TT getRowData(String rowKey)
    {
        return impl.getEntityManager().find(impl.getEntityClass(), impl.getConverter().apply(rowKey));
    }


    @Override
    @Transactional
    public List<TT> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy)
    {
        setRowCount(impl.count(filterBy));
        return impl.findRows(first, pageSize, filterBy, sortBy);
    }


    void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
         stream.defaultReadObject();
         impl = builder.build(JPAModelImpl.builder());
    }
}
