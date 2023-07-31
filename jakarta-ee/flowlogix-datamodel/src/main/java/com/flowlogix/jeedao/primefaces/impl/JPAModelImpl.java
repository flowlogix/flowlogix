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
package com.flowlogix.jeedao.primefaces.impl;

import com.flowlogix.jeedao.DaoHelper;
import com.flowlogix.jeedao.DaoHelper.QueryCriteria;
import com.flowlogix.jeedao.primefaces.Filter;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.Filter.FilterColumnData;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel.BuilderFunction;
import com.flowlogix.jeedao.primefaces.Sorter;
import com.flowlogix.jeedao.primefaces.Sorter.MergedSortOrder;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import com.flowlogix.util.TypeConverter;
import jakarta.persistence.criteria.Join;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
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
import java.util.function.Supplier;
import jakarta.faces.component.UIComponent;
import jakarta.faces.convert.Converter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.NonNull;
import lombok.Builder;
import static com.flowlogix.jeedao.DaoHelper.findEntityManager;
import static java.lang.Math.toIntExact;
import static lombok.Builder.Default;
import lombok.Generated;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Lazy;
import org.omnifaces.util.Lazy.SerializableSupplier;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

/**
 * JPA DAO facade implementation for the PrimeFaces lazy table model
 * @author lprimak
 * @hidden
 * @param <TT>
 * @param <KK>
 */
@Builder
@Slf4j
public class JPAModelImpl<TT, KK> implements Serializable {
    private static final long serialVersionUID = 4L;
    /**
     * Return entity manager to operate on
     */
    private final SerializableSupplier<EntityManager> entityManager;
    /**
     * List of qualifiers to use when finding {@link EntityManager via CDI} (optional)
     */
    @Default
    private final List<Class<? extends Annotation>> entityManagerQualifiers = List.of();
    /**
     * entity class
     */
    private final @NonNull @Getter Class<TT> entityClass;
    private final Lazy<DaoHelper<TT>> daoHelper = new Lazy<>(this::createDaoHelper);
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
    private final transient @Getter @NonNull Sorter<TT> sorter = (a, b, c) -> { };
    /**
     * add optimizer hints here
     * <p>
     * <em>Example:</em>
     * {@snippet class = "com.flowlogix.demo.jeedao.primefaces.OptimizingDataModel" region = "optimizing"}
     */
    @Default
    private final transient @Getter @NonNull Function<TypedQuery<TT>, TypedQuery<TT>> optimizer = a -> a;

    /**
     * Specifies whether String filters are case-sensitive
     */
    @Default
    private final @Getter boolean caseSensitiveFilter = true;

    private final Lazy<Function<String, KK>> defaultConverter = new Lazy<>(this::createConverter);
    private final Lazy<Function<TT, String>> defaultKeyConverter = new Lazy<>(this::createKeyConverter);

    /**
     * @hidden
     * Internal variable, do not use in builder
     */
    @SuppressWarnings({"DeclarationOrder", "MemberName"})
    @Setter
    private BuilderFunction<TT, KK> x_do_not_use_in_builder;

    private static final class FilterDataMap extends HashMap<String, FilterColumnData> implements FilterData { }

    /**
     * partial builder, just for javadoc
     * @param <TT>
     * @param <KK>
     */
    public static class JPAModelImplBuilder<TT, KK> { }

    public int count(Map<String, FilterMeta> filters) {
        return toIntExact(daoHelper.get().count(builder -> builder
                .countQueryCriteria(cqc -> cqc.query().where(getFilters(filters, cqc.builder(), cqc.root())))
                .build()));
    }

    public List<TT> findRows(int first, int pageSize, Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        return optimizer.apply(
                daoHelper.get().findRange(Integer.max(first, 0), Integer.max(first + pageSize, 1),
                        builder -> builder.queryCriteria(qc -> addToCriteria(qc, filters, sortMeta))
                                .build())).getResultList();
    }

    public Supplier<EntityManager> getEntityManager() {
        return daoHelper.get().getEntityManager();
    }

    public Function<String, KK> getStringToKeyConverter() {
        return converter != null ? converter : defaultConverter.get();
    }

    public Function<TT, String> getKeyConverter() {
        return keyConverter != null ? keyConverter : defaultKeyConverter.get();
    }

    private DaoHelper<TT> createDaoHelper() {
        if (entityManager != null) {
            return new DaoHelper<>(entityManager, entityClass);
        } else {
            return new DaoHelper<>(findEntityManager(entityManagerQualifiers), entityClass);
        }
    }

    private Function<String, KK> createConverter() {
        return keyValue -> TypeConverter.valueOf(keyValue, getPrimaryKeyClass());
    }

    private Function<TT, String> createKeyConverter() {
        return entry -> getPrimaryKey(Optional.of(entry)).toString();
    }

    private void addToCriteria(QueryCriteria<TT> qc, Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        qc.query().where(getFilters(filters, qc.builder(), qc.root()));
        qc.query().orderBy(getSort(sortMeta, qc.builder(), qc.root()));
        qc.root().alias(JPALazyDataModel.RESULT);
    }

    public Predicate getFilters(Map<String, FilterMeta> filters, CriteriaBuilder cb, Root<TT> root) {
        FilterData predicates = new FilterDataMap();
        filters.values().forEach(filterMeta -> {
            if (filterMeta.isGlobalFilter()) {
                predicates.put(filterMeta.getField(), new FilterColumnData(filterMeta.getFilterValue(), null));
            } else {
                var filterMetas = processFilterMeta(cb, root, filterMeta.getField(), filterMeta);
                predicates.put(filterMeta.getField(), new FilterColumnData(filterMetas.value(), filterMetas.cond()));
            }
        });
        filter.filter(predicates, cb, root);
        return cb.and(predicates.values().stream().map(FilterColumnData::getPredicate)
                .filter(Objects::nonNull).toArray(Predicate[]::new));
    }

    public List<Order> getSort(Map<String, SortMeta> sortCriteria, CriteriaBuilder cb, Root<TT> root) {
        var sortData = new SortData(sortCriteria);
        sorter.sort(sortData, cb, root);
        return processSortMeta(sortData.getSortData(), cb, root);
    }

    /**
     * Recursively resolve field name, possibly by joining other tables,
     * based on a dotted notation of the field
     *
     * @param root Criteria root
     * @param fieldName field name
     * @return expression
     * @param <YY> expression type
     */
    public <YY> Expression<YY> resolveField(Root<TT> root, String fieldName) {
        Join<?, ?> join = null;
        // recursively traverse all dotted fields, and join each
        while (fieldName.contains(".")) {
            String partial = fieldName.substring(0, fieldName.indexOf("."));
            fieldName = fieldName.substring(partial.length() + 1);
            if (join == null) {
                join = root.join(partial);
            } else {
                join = join.join(partial);
            }
        }
        return join == null ? root.get(fieldName) : join.get(fieldName);
    }

    private FilterMetaResult processFilterMeta(CriteriaBuilder cb, Root<TT> root, String key, FilterMeta filterMeta) {
        Predicate cond = null;
        Object value = filterMeta.getFilterValue();
        try {
            var field = resolveField(root, key);
            Class<?> fieldType = field.getJavaType();
            if (fieldType == String.class) {
                value = value.toString();
                cond = predicateFromFilter(cb, field, filterMeta, value);
            } else if (fieldType.isArray() || Collection.class.isAssignableFrom(fieldType)) {
                cond = predicateFromFilterOrComparable(cond, cb, root, field, filterMeta, value, fieldType);
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
                    cond = predicateFromFilterOrComparable(cond, cb, root, field, filterMeta, value, fieldType);
                }
            }
        } catch (IllegalArgumentException e) { /* ignore possibly extra filter columns */ }
        return new FilterMetaResult(cond, value);
    }

    private record FilterMetaResult(Predicate cond, Object value) { }

    private Predicate predicateFromFilterOrComparable(Predicate cond, CriteriaBuilder cb, Root<TT> root, Expression<?> field,
                                                      FilterMeta filterMeta, Object value, Class<?> fieldType) {
        cond = predicateFromFilter(cb, field, filterMeta, value);
        if (cond == null && Comparable.class.isAssignableFrom(fieldType)) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Comparable<? super Comparable> cv = (Comparable) value;
            @SuppressWarnings({"unchecked", "rawtypes"})
            Expression<Comparable<? super Comparable>> comparableField = (Expression<Comparable<? super Comparable>>) field;
            cond = predicateFromFilterComparable(cb, comparableField, filterMeta, cv);
        }
        return cond;
    }

    private class ExpressionEvaluator {
        private final Expression<String> expression;
        private final String value;

        ExpressionEvaluator(CriteriaBuilder cb, Expression<?> expression, Object value) {
            if (caseSensitiveFilter) {
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

    @SuppressWarnings("MissingSwitchDefault")
    private List<Order> processSortMeta(Map<String, MergedSortOrder> sortMeta, CriteriaBuilder cb, Root<TT> root) {
        List<Order> sortMetaOrdering = new ArrayList<>();
        sortMeta.values().forEach(order -> {
            if (order.getRequestedSortMeta() != null) {
                switch (order.getRequestedSortMeta().getOrder()) {
                    case ASCENDING:
                        sortMetaOrdering.add(cb.asc(resolveField(root, order.getRequestedSortMeta().getField())));
                        break;
                    case DESCENDING:
                        sortMetaOrdering.add(cb.desc(resolveField(root, order.getRequestedSortMeta().getField())));
                        break;
                }
            } else if (order.getApplicationSort() != null) {
                if (order.isHighPriority()) {
                    sortMetaOrdering.add(0, order.getApplicationSort());
                } else {
                    sortMetaOrdering.add(order.getApplicationSort());
                }
            } else {
                throw new IllegalStateException("Neither application sort request, nor UI sort request is available");
            }
        });
        return sortMetaOrdering;
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows(ReflectiveOperationException.class)
    private KK getPrimaryKey(Optional<TT> entry) {
        return (KK) daoHelper.get().getEntityManager().get().getEntityManagerFactory().getPersistenceUnitUtil()
                .getIdentifier(entry.orElse(ConstructorUtils.invokeConstructor(getEntityClass())));
    }

    @SuppressWarnings("unchecked")
    private Class<KK> getPrimaryKeyClass() {
        return (Class<KK>) getPrimaryKey(Optional.empty()).getClass();
    }

    /**
     * Unfortunately, CDI beans like {@link JPALazyDataModel} cannot contain serialization-related methods
     * as Weld doesn't handle that well. We have to put all those methods here and make this class truly Serializable
     *
     * @return new, corrected object
     * @throws ObjectStreamException
     */
    Object readResolve() throws ObjectStreamException {
        var corrected = x_do_not_use_in_builder.apply(JPAModelImpl.builder());
        corrected.x_do_not_use_in_builder = x_do_not_use_in_builder;
        return corrected;
    }
}
