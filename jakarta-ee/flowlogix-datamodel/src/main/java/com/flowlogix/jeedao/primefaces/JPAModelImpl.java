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

import com.flowlogix.jeedao.DaoHelper;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel.BuilderFunction;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import com.flowlogix.jeedao.querycriteria.QueryCriteria;
import com.flowlogix.util.TypeConverter;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.faces.component.UIComponent;
import javax.faces.convert.Converter;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import static lombok.Builder.Default;
import lombok.Generated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Lazy;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

/**
 * JPA DAO facade implementation for the PrimeFaces lazy table model
 * @author lprimak
 * @param <TT>
 * @param <KK>
 */
@SuperBuilder
@Slf4j
public class JPAModelImpl<TT, KK> extends DaoHelper<TT, KK> implements Serializable {
    private static final long serialVersionUID = 3L;
    /**
     * convert String key into {@link KK} object
     */
    private final transient Function<String, KK> converter;
    /**
     * convert typed key to String
     */
    private final transient Function<TT, String> keyConverter;
    /**
     * adds {@link Filter} object
     */
    @Default
    private final transient @Getter @NonNull Filter<TT> filter = (a, b, c) -> { };
    /**
     * adds {@link Sorter} object
     */
    @Default
    private final transient @Getter @NonNull Sorter<TT> sorter = (a, b, c) -> true;
    /**
     * add optimizer hints here
     */
    @Default
    private final transient @Getter @NonNull Function<TypedQuery<TT>, TypedQuery<TT>> optimizer = a -> a;

    /**
     * whether string queries are case-sensitive
     */
    @Default
    private final @Getter boolean caseSensitiveQuery = true;

    private final Lazy<Function<String, KK>> defaultConverter = new Lazy<>(this::createConverter);
    private final Lazy<Function<TT, String>> defaultKeyConverter = new Lazy<>(this::createKeyConverter);

    /**
     * prevent from direct construction
     */
    @Generated
    JPAModelImpl() {
        super(null, null);
        this.converter = null;
        this.keyConverter = null;
        this.filter = null;
        this.sorter = null;
        this.optimizer = null;
        this.caseSensitiveQuery = false;
    }

    /** @hidden
     * Internal variable, do not use in builder
     */
    @SuppressWarnings({"DeclarationOrder", "MemberName"})
    BuilderFunction<TT, KK> x_do_not_use_in_builder;

    int count(Map<String, FilterMeta> filters) {
        return super.count(Parameters.<TT>builder()
                .countQueryCriteria(cqc -> cqc.getQuery().where(getFilters(filters, cqc.getBuilder(), cqc.getRoot())))
                .build());
    }

    List<TT> findRows(int first, int pageSize, Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        return super.findRange(Integer.max(first, 0), Integer.max(first + pageSize, 1),
                Parameters.<TT>builder()
                        .queryCriteria(qc -> addToCriteria(qc, filters, sortMeta))
                        .hints(optimizer::apply)
                        .build());
    }

    Function<String, KK> getConverter() {
        return converter != null ? converter : defaultConverter.get();
    }

    Function<TT, String> getKeyConverter() {
        return keyConverter != null ? keyConverter : defaultKeyConverter.get();
    }

    private Function<String, KK> createConverter() {
        return keyValue -> TypeConverter.valueOf(keyValue, getPrimaryKeyClass());
    }

    private Function<TT, String> createKeyConverter() {
        return entry -> getPrimaryKey(Optional.of(entry)).toString();
    }

    private void addToCriteria(QueryCriteria<TT> qc, Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        qc.getQuery().where(getFilters(filters, qc.getBuilder(), qc.getRoot()));
        qc.getQuery().orderBy(getSort(sortMeta, qc.getBuilder(), qc.getRoot()));
        qc.getRoot().alias(JPALazyDataModel.RESULT);
    }

    Predicate getFilters(Map<String, FilterMeta> filters, CriteriaBuilder cb, Root<TT> root) {
        Map<String, FilterData> predicates = new HashMap<>();
        filters.forEach((key, filterMeta) -> {
            Predicate cond = null;
            Object value = filterMeta.getFilterValue();
            try {
                Class<?> fieldType = root.get(key).getJavaType();
                if (fieldType == String.class) {
                    value = value.toString();
                    cond = predicateFromFilter(cb, root.get(key), filterMeta, value);
                } else if (fieldType.isArray() || Collection.class.isAssignableFrom(fieldType)) {
                    cond = predicateFromFilterOrComparable(cond, cb, root, key, filterMeta, value, fieldType);
                } else {
                    var convertedValue = TypeConverter.checkAndConvert(value.toString(), fieldType);
                    boolean valid = convertedValue.isValid();
                    if (valid) {
                        value = convertedValue.getValue();
                    } else {
                        try {
                            Converter<?> valueConverter = Faces.getApplication().createConverter(fieldType);
                            if (valueConverter != null) {
                                value = valueConverter.getAsObject(Faces.getContext(),
                                        UIComponent.getCurrentComponent(Faces.getContext()), value.toString());
                                valid = true;
                            }
                        } catch (Throwable e) {
                            log.debug("unable to convert via JSF", e);
                        }
                    }
                    if (valid) {
                        cond = predicateFromFilterOrComparable(cond, cb, root, key, filterMeta, value, fieldType);
                    }
                }
            } catch (IllegalArgumentException e) { /* ignore possibly extra filter fields */ }
            predicates.put(key, new FilterData(value, cond));
        });
        filter.filter(predicates, cb, root);
        return cb.and(predicates.values().stream().map(FilterData::getPredicate)
                .filter(Objects::nonNull).toArray(Predicate[]::new));
    }

    private Predicate predicateFromFilterOrComparable(Predicate cond, CriteriaBuilder cb, Root<TT> root,
            String key, FilterMeta filterMeta, Object value, Class<?> fieldType) {
        cond = predicateFromFilter(cb, root.get(key), filterMeta, value);
        if (cond == null && Comparable.class.isAssignableFrom(fieldType)) {
            @SuppressWarnings({"unchecked", "rawtypes"})
                    Comparable<? super Comparable> cv = (Comparable) value;
            cond = predicateFromFilterComparable(cb, root.get(key), filterMeta, cv);
        }
        return cond;
    }

    private class ExpressionEvaluator {
        private final Expression<String> expression;
        private final String value;

        ExpressionEvaluator(CriteriaBuilder cb, Expression<?> expression, Object value) {
            if (caseSensitiveQuery) {
                this.expression = expression.as(String.class);
                this.value = value.toString();
            } else {
                this.expression = cb.lower(expression.as(String.class));
                this.value = value.toString().toLowerCase();
            }
        }
    }

    @SuppressWarnings({"CyclomaticComplexity", "ReturnCount", "MissingSwitchDefault"})
    @Generated
    private Predicate predicateFromFilter(CriteriaBuilder cb, Expression<?> expression,
            FilterMeta filter, Object filterValue) {
        var stringExpression = new Lazy<>(() -> new ExpressionEvaluator(cb, expression, filterValue));
        Lazy<Collection<?>> filterValueAsCollection = new Lazy<>(
                () -> filterValue.getClass().isArray() ? Arrays.asList(filterValue)
                        : (Collection<?>) filterValue);
        switch (filter.getMatchMode()) {
            case STARTS_WITH:
                return cb.like(stringExpression.get().expression, stringExpression.get().value + "%");
            case NOT_STARTS_WITH:
                return cb.notLike(stringExpression.get().expression, stringExpression.get().value + "%");
            case ENDS_WITH:
                return cb.like(stringExpression.get().expression, "%" + stringExpression.get().value);
            case NOT_ENDS_WITH:
                return cb.notLike(stringExpression.get().expression, "%" + stringExpression.get().value);
            case CONTAINS:
                return cb.like(stringExpression.get().expression, "%" + stringExpression.get().value + "%");
            case NOT_CONTAINS:
                return cb.notLike(stringExpression.get().expression, "%" + stringExpression.get().value + "%");
            case EXACT:
            case EQUALS:
                return cb.equal(expression, filterValue);
            case NOT_EXACT:
            case NOT_EQUALS:
                return cb.notEqual(expression, filterValue);
            case IN:
                return filterValueAsCollection.get().size() == 1
                        ? cb.equal(expression, filterValueAsCollection.get().iterator().next())
                        : expression.in(filterValueAsCollection.get());
            case NOT_IN:
                return filterValueAsCollection.get().size() == 1
                        ? cb.notEqual(expression, filterValueAsCollection.get().iterator().next())
                        : expression.in(filterValueAsCollection.get()).not();
            case GLOBAL:
                throw new UnsupportedOperationException("MatchMode.GLOBAL currently not supported!");
        }
        return null;
    }

    @SuppressWarnings("MissingSwitchDefault")
    @Generated
    private <TC extends Comparable<? super TC>> Predicate predicateFromFilterComparable(CriteriaBuilder cb,
            Expression<TC> objectExpression, FilterMeta filter, TC filterValue) {
        @SuppressWarnings("unchecked")
        Lazy<Collection<TC>> filterValueAsCollection = new Lazy<>(
                () -> filterValue.getClass().isArray() ? Arrays.asList(filterValue)
                        : (Collection<TC>) filterValue);
        switch (filter.getMatchMode()) {
            case LESS_THAN:
                return cb.lessThan(objectExpression, filterValue);
            case LESS_THAN_EQUALS:
                return cb.lessThanOrEqualTo(objectExpression, filterValue);
            case GREATER_THAN:
                return cb.greaterThan(objectExpression, filterValue);
            case GREATER_THAN_EQUALS:
                return cb.greaterThanOrEqualTo(objectExpression, filterValue);
            case BETWEEN:
                return between(cb, objectExpression, filterValueAsCollection);
            case NOT_BETWEEN:
                return between(cb, objectExpression, filterValueAsCollection).not();
        }
        return null;
    }

    private <TC extends Comparable<? super TC>> Predicate between(CriteriaBuilder cb,
            Expression<TC> objectExpression, Lazy<Collection<TC>> filterValueAsCollection) {
        Iterator<TC> iterBetween = filterValueAsCollection.get().iterator();
        return cb.and(cb.greaterThanOrEqualTo(objectExpression, iterBetween.next()),
                cb.lessThanOrEqualTo(objectExpression, iterBetween.next()));
    }

    private List<Order> getSort(Map<String, SortMeta> sortCriteria, CriteriaBuilder cb, Root<TT> root) {
        var sortData = new SortData(sortCriteria);
        boolean appendSortOder = sorter.sort(sortData, cb, root);

        List<Order> sortMetaOrdering = processSortMeta(sortData.getSortMeta(), cb, root);
        List<Order> rv = new ArrayList<>();
        if (appendSortOder) {
            rv.addAll(sortMetaOrdering);
            rv.addAll(sortData.getSortOrder());
        } else {
            rv.addAll(sortData.getSortOrder());
            rv.addAll(sortMetaOrdering);
        }
        return rv;
    }

    @SuppressWarnings("MissingSwitchDefault")
    private List<Order> processSortMeta(Map<String, SortMeta> sortMeta, CriteriaBuilder cb, Root<TT> root) {
        List<Order> sortMetaOrdering = new ArrayList<>();
        sortMeta.forEach((field, order) -> {
            switch (order.getOrder()) {
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

    @SuppressWarnings("unchecked")
    @SneakyThrows(ReflectiveOperationException.class)
    private KK getPrimaryKey(Optional<TT> entry) {
        return (KK) getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil()
                .getIdentifier(entry.orElse(ConstructorUtils.invokeConstructor(getEntityClass())));
    }

    @SuppressWarnings("unchecked")
    private Class<KK> getPrimaryKeyClass() {
        return (Class<KK>) getPrimaryKey(Optional.empty()).getClass();
    }

    Object readResolve() throws ObjectStreamException {
        var corrected = x_do_not_use_in_builder.apply(JPAModelImpl.builder());
        corrected.x_do_not_use_in_builder = x_do_not_use_in_builder;
        return corrected;
    }
}
