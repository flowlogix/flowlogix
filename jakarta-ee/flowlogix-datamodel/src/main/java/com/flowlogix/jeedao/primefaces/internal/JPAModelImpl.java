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

import com.flowlogix.jeedao.DaoHelper;
import com.flowlogix.jeedao.querycriteria.QueryCriteria;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import com.flowlogix.jeedao.primefaces.interfaces.Filter;
import com.flowlogix.jeedao.primefaces.interfaces.Sorter;
import com.flowlogix.jeedao.primefaces.support.FilterData;
import com.flowlogix.jeedao.primefaces.support.SortData;
import com.flowlogix.util.TypeConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import static lombok.Builder.Default;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

/**
 * JPA DAO facade implementation for the PrimeFaces lazy table model
 * @author lprimak
 * @param <TT>
 * @param <KK>
 */
@SuperBuilder
public class JPAModelImpl<TT, KK> extends DaoHelper<TT, KK> {
    /**
     * convert String key into {@link KK} object
     */
    private final @Getter @NonNull Function<String, KK> converter;
    /**
     * adds {@link Filter} object
     */
    @Default
    private final @Getter @NonNull Filter<TT> filter = (a, b, c) -> { };
    /**
     * adds {@link Sorter} object
     */
    @Default
    private final @Getter @NonNull Sorter<TT> sorter = (a, b, c) -> { };
    /**
     * add optimizer hints here
     */
    @Default
    private final @Getter @NonNull Function<TypedQuery<TT>, TypedQuery<TT>> optimizer = (a) -> a;


    public int count(Map<String, FilterMeta> filters) {
        return super.count(Parameters.<TT>builder()
                .countQueryCriteria(cqc -> cqc.getQuery().where(getFilters(filters, cqc.getBuilder(), cqc.getRoot())))
                .build());
    }

    public List<TT> findRows(int first, int pageSize, Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        return super.findRange(first, first + pageSize,
                Parameters.<TT>builder()
                        .queryCriteria(qc -> addToCriteria(qc, filters, sortMeta))
                        .hints(tq -> optimizer.apply(tq))
                        .build());
    }

    private void addToCriteria(QueryCriteria<TT> qc, Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        qc.getQuery().where(getFilters(filters, qc.getBuilder(), qc.getRoot()));
        qc.getQuery().orderBy(getSort(sortMeta, qc.getBuilder(), qc.getRoot()));
        qc.getRoot().alias(JPALazyDataModel.RESULT);
    }

    private Predicate getFilters(Map<String, FilterMeta> filters, CriteriaBuilder cb, Root<TT> root) {
        Map<String, FilterData> predicates = new HashMap<>();
        filters.forEach((key, value) -> {
            Predicate cond = null;
            try {
                Class<?> fieldType = root.get(key).getJavaType();
                if (fieldType == String.class) {
                    cond = cb.like(root.get(key), String.format("%%%s%%", value));
                } else {
                    if (TypeConverter.checkType(value.toString(), fieldType)) {
                        cond = cb.equal(root.get(key), value);
                    }
                }
            }
            catch(IllegalArgumentException e) { /* ignore possibly extra filter fields */}
            predicates.put(key, new FilterData(value.toString(), cond));
        });
        filter.filter(predicates, cb, root);
        return cb.and(predicates.values().stream().map(FilterData::getPredicate)
                .filter(Objects::nonNull).toArray(Predicate[]::new));
    }

    private List<Order> getSort(Map<String, SortMeta> sortCriteria, CriteriaBuilder cb, Root<TT> root) {
        SortData sortData = new SortData(sortCriteria);
        sorter.sort(sortData, cb, root);

        List<Order> sortMetaOrdering = processSortMeta(sortData.getSortMeta(), cb, root);
        List<Order> rv = new ArrayList<>();
        if(sortData.isAppendSortOrder()) {
            rv.addAll(sortMetaOrdering);
            rv.addAll(sortData.getSortOrder());
        } else {
            rv.addAll(sortData.getSortOrder());
            rv.addAll(sortMetaOrdering);
        }
        return rv;
    }

    private List<Order> processSortMeta(Map<String, SortMeta> sortMeta, CriteriaBuilder cb, Root<TT> root) {
        List<Order> sortMetaOrdering = new ArrayList<>();
        sortMeta.forEach((field, order) -> {
            switch(order.getOrder()) {
                case ASCENDING:
                    sortMetaOrdering.add(cb.asc(root.get(order.getField())));
                    break;
                case DESCENDING:
                    sortMetaOrdering.add(cb.desc(root.get(order.getField())));
                    break;
            }
        });
        return sortMetaOrdering;
    }
}
