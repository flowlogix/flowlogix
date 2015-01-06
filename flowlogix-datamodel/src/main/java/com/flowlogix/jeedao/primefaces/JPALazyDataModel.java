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

import com.flowlogix.jeedao.primefaces.interfaces.Sorter;
import com.flowlogix.jeedao.primefaces.interfaces.Filter;
import com.flowlogix.jeedao.primefaces.interfaces.KeyConverter;
import com.flowlogix.jeedao.primefaces.interfaces.EntityManagerGetter;
import com.flowlogix.jeedao.primefaces.support.FilterData;
import com.flowlogix.jeedao.primefaces.interfaces.Optimizer;
import com.flowlogix.jeedao.primefaces.interfaces.FilterReplacer;
import com.flowlogix.jeedao.primefaces.internal.JPAFacadeLocal;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.apache.commons.lang.StringUtils;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

/**
 * Easy implementation of PrimeFaces lazy data model
 * using Lambdas
 * 
 * @author lprimak
 * @param <KK> Key Type
 * @param <TT> Data Type
 */
@Dependent
public class JPALazyDataModel<KK, TT> extends LazyDataModel<TT>
{
    /**
     * Set up this particular instance of the data model
     * with entity manager, class and key converter
     * 
     * @param emg
     * @param entityClass
     * @param converter 
     */
    public void setup(EntityManagerGetter emg, Class<TT> entityClass, KeyConverter<KK> converter)
    {
        this.emg = emg;
        this.entityClass = entityClass;
        this.converter = converter;
    }


    /**
     * set filter hook
     * 
     * @param filter 
     */
    public void setFilter(Filter<TT> filter)
    {
        this.filter = Optional.of(filter);
    }
    
    
    /**
     * remove filter hook
     */
    public void removeFilter()
    {
        filter = Optional.absent();
    }
    
     
    /**
     * Utility method for replacing a predicate in the filter list
     * 
     * @param filters filter list
     * @param element element to be replace
     * @param fp lambda to get the new Filter predicate
     */
    public void replaceFilter(Map<String, FilterData> filters, String element, FilterReplacer fp)
    {
        FilterData elt = filters.get(element);
        if (elt != null && StringUtils.isNotBlank(elt.getFieldValue()))
        {
            filters.replace(element, new FilterData(elt.getFieldValue(), 
                    fp.get(elt.getPredicate(), elt.getFieldValue())));
        }
    }

    
    /**
     * set sorter hook
     * 
     * @param sorter 
     */
    public void setSorter(Sorter<TT> sorter)
    {
        this.sorter = Optional.of(sorter);
    }
    
    
    /**
     * remove sorter hook
     */
    public void removeSorter()
    {
        sorter = Optional.absent();
    }
    

    /**
     * add hints to JPA query
     * 
     * @param optimizier 
     */
    public void addOptimizerHints(Optimizer<TT> optimizier)
    {
        this.optimizer = Optional.of(optimizier);
    }

    
    /**
     * remove hints from JPA query
     */
    public void removeOptimizerHints()
    {
        this.optimizer = Optional.absent();
    }
    
    
    /**
     * transforms JPA entity field to format suitable for hints
     * 
     * @param val
     * @return JPA field suitable for hints
     */
    public String getResultField(String val)
    {
        return String.format("%s.%s", RESULT, val);
    }
    
    
    @Override
    @SuppressWarnings("unchecked")
    public KK getRowKey(TT entity)
    {
        return (KK)emg.get().getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
    }

    
    @Override
    @Transactional
    public TT getRowData(String rowKey)
    {   
        facade.setup(emg, entityClass, optimizer, filter, sorter);
        return facade.find(converter.convert(rowKey));
    }

    
    @Override
    @Transactional
    public List<TT> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters)
    {
        SortMeta sm = new SortMeta();
        sm.setSortField(sortField);
        sm.setSortOrder(sortOrder);
        return load(first, pageSize, sortField == null? ImmutableList.of() : ImmutableList.of(sm), filters);
    }    

    
    @Override
    public List<TT> load(int first, int pageSize, List<SortMeta> multiSortMeta, Map<String, Object> filters)
    {
        facade.setup(emg, entityClass, optimizer, filter, sorter);
        setRowCount(facade.count(filters));
        return facade.findRows(first, pageSize, filters, multiSortMeta == null? ImmutableList.of() : multiSortMeta);
    }

    
    private @Inject JPAFacadeLocal<TT, KK> facade;
    private EntityManagerGetter emg;
    private Class<TT> entityClass;
    private KeyConverter<KK> converter;
    private Optional<Filter<TT>> filter = Optional.absent();
    private Optional<Sorter<TT>> sorter = Optional.absent();
    private Optional<Optimizer<TT>> optimizer = Optional.absent();
    public static final String RESULT = "result";

    private static final long serialVersionUID = 1L;
}
