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
package com.flowlogix.jeedao.primefaces.internal;

import com.flowlogix.jeedao.impl.AbstractFacade;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import com.flowlogix.jeedao.primefaces.interfaces.EntityManagerGetter;
import com.flowlogix.jeedao.primefaces.interfaces.Filter;
import com.flowlogix.jeedao.primefaces.interfaces.Optimizer;
import com.flowlogix.jeedao.primefaces.interfaces.Sorter;
import com.flowlogix.jeedao.primefaces.support.FilterData;
import com.flowlogix.util.TypeConverter;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.SortMeta;

/**
 * JPA DAO facade implementation for the PrimeFaces lazy table model
 * @author lprimak
 * @param <TT>
 * @param <KK>
 */
@Stateless @Slf4j
public class JPAFacade<TT, KK> extends AbstractFacade<TT, KK> implements JPAFacadeLocal<TT, KK>
{
    @Override
    public void setup(EntityManagerGetter emg, Class<TT> entityClass, Optional<Optimizer<TT>> optimizer,
            Optional<Filter<TT>> filter, Optional<Sorter<TT>> sorter)
    {
        getState().setEmg(emg);
        getState().setEntityClass(entityClass);
        getState().setOptimizer(optimizer);
        getState().setFilterHook(filter);
        getState().setSorterHook(sorter);
    }

    
    @Override
    protected EntityManager getEntityManager()
    {
        return getState().getEmg().get();
    }

    
    @Override
    public Class<TT> getEntityClass()
    {
        return getState().getEntityClass();
    }
    
    
    @Override
    public int count(Map<String, Object> filters)
    {
        getState().setFilters(filters);
        getState().setSortMeta(Lists.newLinkedList());
        return super.count();
    }

    
    @Override
    public List<TT> findRows(int first, int pageSize, Map<String, Object> filters, List<SortMeta> sortMeta)
    {
        getState().setFilters(filters);
        getState().setSortMeta(sortMeta);
        return super.findRange(new int[] { first, first + pageSize });
    }

    
    @Override
    protected void addToCriteria(CriteriaBuilder cb, Root<TT> root, CriteriaQuery<TT> cq)
    {
        cq.where(getFilters(getState().getFilters(), cb, root));
        cq.orderBy(getSort(getState().getSortMeta(), cb, root));
        root.alias(JPALazyDataModel.RESULT);
    }
    
    
    @Override
    protected void addToCountCriteria(CriteriaBuilder cb, Root<TT> root, CriteriaQuery<Long> cq)
    {
        cq.where(getFilters(getState().getFilters(), cb, root));
    }

    
    @Override
    protected void addHints(TypedQuery<TT> tq, boolean isRange)
    {
        if(getState().getOptimizer().isPresent())
        {
            getState().getOptimizer().get().addHints(tq);
        }
    }
    
    
    private Predicate getFilters(Map<String, Object> filters, CriteriaBuilder cb, Root<TT> root)
    {
        Map<String, FilterData> predicates = Maps.newHashMap();        
        filters.forEach((key, value) ->
        {
            Predicate cond = null;
            try
            {
                Class<?> fieldType = root.get(key).getJavaType();
                if (fieldType == String.class)
                {
                    cond = cb.like(root.get(key), String.format("%%%s%%", value));
                } else
                {
                    if (TypeConverter.checkType(value.toString(), fieldType))
                    {
                        cond = cb.equal(root.get(key), value);
                    }
                }
            }
            catch(IllegalArgumentException e) { /* ignore possibly extra filter fields */}
            predicates.put(key, new FilterData(value.toString(), cond));
        });
        if(getState().getFilterHook().isPresent())
        {
            getState().getFilterHook().get().filter(predicates, cb, root);
        }
        
        return cb.and(FluentIterable.from(predicates.values())
                .filter(it -> it.getPredicate() != null)
                .transform(it -> it.getPredicate()).toArray(Predicate.class));
    }
    
    
    private List<Order> getSort(List<SortMeta> sortCriteria, CriteriaBuilder cb, Root<TT> root)
    {
        Iterator<SortMeta> it;
        if(getState().getSorterHook().isPresent())
        {
            it = getState().getSorterHook().get().sort(Lists.newLinkedList(sortCriteria).iterator(), cb, root);          
        }
        else
        {
            it = sortCriteria.iterator();
        }
        List<Order> rv = Lists.newLinkedList();
        it.forEachRemaining(sm -> 
        {
            switch(sm.getSortOrder())
            {
                case ASCENDING:
                    rv.add(cb.asc(root.get(sm.getSortField())));
                    break;
                case DESCENDING:
                    rv.add(cb.desc(root.get(sm.getSortField())));
                    break;
            }
        });
        return rv;
    }
    
    
    @SuppressWarnings("unchecked")
    private JPAFacadeTypedState<TT> getState()
    {
        return (JPAFacadeTypedState<TT>) state.getTypedState();
    }
    
    
    private @Inject JPAFacadeState state;
}
