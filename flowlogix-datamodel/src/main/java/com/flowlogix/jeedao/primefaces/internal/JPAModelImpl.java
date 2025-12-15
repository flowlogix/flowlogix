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
package com.flowlogix.jeedao.primefaces.internal;

import com.flowlogix.api.dao.JPAFinder.QueryCriteria;
import com.flowlogix.api.dao.JPAFinderHelper;
import com.flowlogix.jeedao.DaoHelper;
import com.flowlogix.jeedao.primefaces.Filter;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.Filter.FilterColumnData;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel.BuilderFunction;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel.FilterCaseConversion;
import com.flowlogix.jeedao.primefaces.Sorter;
import com.flowlogix.jeedao.primefaces.Sorter.MergedSortOrder;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import com.flowlogix.util.TypeConverter;
import jakarta.persistence.criteria.Join;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
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
import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.PartialBuilderConsumer;
import static java.lang.Math.toIntExact;
import static java.util.function.UnaryOperator.identity;
import static lombok.Builder.Default;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Lazy;
import org.omnifaces.util.Lazy.SerializableSupplier;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.primefaces.util.Constants;

/**
 * JPA DAO facade implementation for the PrimeFaces lazy table model
 * @author lprimak
 * @hidden
 * @param <TT>
 */
@Builder
@Slf4j
public class JPAModelImpl<TT> implements Serializable {
    private static final long serialVersionUID = 5L;
    /**
     * Return entity manager to operate on
     */
    private final transient SerializableSupplier<EntityManager> entityManager;
    /**
     * List of qualifiers to use when finding {@link EntityManager via CDI} (optional)
     */
    @Default
    @Getter
    private final List<Class<? extends Annotation>> entityManagerQualifiers = List.of();
    /**
     * entity class
     */
    private final @NonNull @Getter Class<TT> entityClass;
    private final Lazy<JPAFinderHelper<TT>> jpaFinder = new Lazy<>(this::createJPAFinder);
    /**
     * convert String key into key object
     */
    private final transient Function<String, ?> converter;
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
    private final transient @Getter @NonNull UnaryOperator<TypedQuery<TT>> optimizer = identity();

    /**
     * enriches the resulting model, by adding rows or post-processing columns
     */
    @Default
    private final transient @Getter @NonNull UnaryOperator<List<TT>> resultEnricher = identity();

    /**
     * Specifies whether String filters are case-sensitive
     */
    @Default
    private final @Getter boolean caseSensitiveFilter = true;

    /**
     * to which case (upper / lower) to convert during case-insensitive query
     */
    @Default
    private final @Getter FilterCaseConversion filterCaseConversion = FilterCaseConversion.UPPER;

    /**
     * Specifies whether wild cards are supported in string filters
     */
    @Default
    private final @Getter boolean wildcardSupport = false;

    private final Lazy<Function<String, ?>> defaultConverter = new Lazy<>(this::createConverter);
    private final Lazy<Function<TT, String>> defaultKeyConverter = new Lazy<>(this::createKeyConverter);

    /**
     * @hidden
     * Internal record, do not use
     * @param builder
     * @param partialBuilder
     * @param <TT>
     */
    public record BuilderInitializer<TT>(@NonNull BuilderFunction<TT> builder,
                                             PartialBuilderConsumer<TT> partialBuilder) implements Serializable { }

    /**
     * @hidden
     * Internal variable, do not use in builder
     */
    @SuppressWarnings({"DeclarationOrder", "MemberName"})
    @Setter
    private BuilderInitializer<TT> x_do_not_use_in_builder;

    private static final class FilterDataMap extends HashMap<String, FilterColumnData> implements FilterData { }

    /**
     * Private - do not use
     *
     * @hidden
     * @param initializer
     * @return
     * @param <TT>
     */
    public static <TT> JPAModelImpl<TT> create(@NonNull BuilderInitializer<TT> initializer) {
        var builderInstance = JPAModelImpl.<TT>builder();
        if (initializer.partialBuilder != null) {
            initializer.partialBuilder.accept(builderInstance);
        }
        return initializer.builder.apply(builderInstance);
    }

    /**
     * partial builder, just for javadoc
     * @hidden
     * @param <TT>
     */
    public static class JPAModelImplBuilder<TT> { }

    public int count(Map<String, FilterMeta> filters) {
        return toIntExact(jpaFinder.get().count(cqc -> cqc.query().where(getFilters(filters, cqc.builder(), cqc.root()))));
    }

    public List<TT> findRows(int first, int pageSize, Map<String, FilterMeta> filters, Map<String, SortMeta> sortMeta) {
        return resultEnricher.apply(optimizer.apply(
                jpaFinder.get().findRange(Integer.max(first, 0), Integer.max(first + pageSize, 1),
                        qc -> addToCriteria(qc, filters, sortMeta))).getResultList());
    }

    public Supplier<EntityManager> getEntityManager() {
        return jpaFinder.get().getEntityManager();
    }

    @SuppressWarnings("unchecked")
    public <KK> Function<String, KK> getStringToKeyConverter() {
        return (Function<String, KK>) (converter != null ? converter : defaultConverter.get());
    }

    public Function<TT, String> getKeyConverter() {
        return keyConverter != null ? keyConverter : defaultKeyConverter.get();
    }

    private JPAFinderHelper<TT> createJPAFinder() {
        if (entityManager != null) {
            return new DaoHelper<>(entityManager, entityClass);
        } else {
            return new DaoHelper<>(findEntityManager(entityManagerQualifiers), entityClass);
        }
    }

    private Function<String, ?> createConverter() {
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
                if (filterMeta.getFilterValue() != null) {
                    var filterMetas = processFilterMeta(cb, root, filterMeta.getField(), filterMeta);
                    predicates.put(filterMeta.getField(), new FilterColumnData(filterMetas.value(), filterMetas.cond()));
                }
            }
        });
        filter.filter(predicates, cb, root);
        return cb.and(predicates.values().stream().map(FilterColumnData::getPredicate)
                .filter(Objects::nonNull).toArray(Predicate[]::new));
    }

    public List<Order> getSort(Map<String, SortMeta> sortCriteria, CriteriaBuilder cb, Root<TT> root) {
        var sortData = new SortData(sortCriteria);
        sorter.sort(sortData, cb, root);
        return processSortOrder(sortData.getSortOrder(), cb, root);
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
        // traverse all dotted fields, and join each
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
        Object value = Objects.requireNonNullElse(filterMeta.getFilterValue(), Constants.EMPTY_STRING);
        try {
            var field = resolveField(root, key);
            Class<?> fieldType = field.getJavaType();
            Class<?> filterType = value.getClass();
            boolean compositeFilterType = filterType.isArray() || Collection.class.isAssignableFrom(filterType);
            if (fieldType == String.class) {
                value = value.toString();
                cond = predicateFromFilter(cb, field, filterMeta, value);
            } else if (fieldType.equals(filterType) || compositeFilterType) {
                cond = predicateFromFilterOrComparable(cb, field, filterMeta, value, fieldType, compositeFilterType);
            } else {
                value = convert(value, fieldType);
                if (value != null) {
                    cond = predicateFromFilterOrComparable(cb, field, filterMeta, value, fieldType, false);
                }
            }
        } catch (IllegalArgumentException e) { /* ignore possibly extra filter columns */ }
        return new FilterMetaResult(cond, value);
    }

    private Object convert(Object value, Class<?> fieldType) {
        Object convertedValue = null;
        var checkedConvertedValue = TypeConverter.checkAndConvert(value.toString(), fieldType);
        if (checkedConvertedValue.isValid()) {
            convertedValue = checkedConvertedValue.getValue();
        } else {
            try {
                Converter<?> valueConverter = Faces.getApplication().createConverter(fieldType);
                if (valueConverter != null) {
                    convertedValue = valueConverter.getAsObject(Faces.getContext(),
                            UIComponent.getCurrentComponent(Faces.getContext()), value.toString());
                }
            } catch (Exception e) {
                log.debug("unable to convert via Faces", e);
            }
        }
        return convertedValue;
    }

    private record FilterMetaResult(Predicate cond, Object value) { }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate predicateFromFilterOrComparable(CriteriaBuilder cb, Expression<?> field,
                                                      FilterMeta filterMeta, Object value, Class<?> fieldType,
                                                      boolean compositeFilterType) {
        Predicate cond;
        if (compositeFilterType) {
            if (value.getClass().isArray()) {
                value = Arrays.asList((Object[]) value);
            }
            List<?> listValue = (List<?>) value;
            value = listValue.stream().map(raw -> fieldType.isAssignableFrom(raw.getClass())
                    ? raw : Optional.ofNullable(convert(raw, fieldType)).orElseThrow(() ->
                    new IllegalArgumentException(String.format("Can't convert filter: %s to %s",
                            raw, fieldType)))).toList();
        }
        cond = predicateFromFilter(cb, field, filterMeta, value);
        if (cond == null && Comparable.class.isAssignableFrom(fieldType)) {
            Comparable<? super Comparable> cv = null;
            if (value instanceof Comparable<?>) {
                cv = (Comparable) value;
            }
            Expression<Comparable<? super Comparable>> comparableField = (Expression<Comparable<? super Comparable>>) field;
            cond = predicateFromFilterComparable(cb, comparableField, filterMeta, cv, value);
        }
        return cond;
    }

    private class ExpressionEvaluator {
        private final Expression<String> expression;
        private final String value;
        private final boolean hasWildcards;

        private record WildcardValue(boolean hasWildcards, String value) { }

        ExpressionEvaluator(CriteriaBuilder cb, Expression<?> expression, Object value) {
            WildcardValue wildcardValue = replaceWildcards(wildcardSupport, value.toString());
            hasWildcards = wildcardValue.hasWildcards;
            Expression<String> stringExpression = expression.as(String.class);
            if (caseSensitiveFilter) {
                this.expression = stringExpression;
                this.value = wildcardValue.value;
            } else {
                this.expression = switch (filterCaseConversion) {
                    case LOWER -> cb.lower(stringExpression);
                    case UPPER -> cb.upper(stringExpression);
                };
                var locale = Faces.getLocale();
                this.value = switch (filterCaseConversion) {
                    case LOWER -> wildcardValue.value.toLowerCase(locale);
                    case UPPER -> wildcardValue.value.toUpperCase(locale);
                };
            }
        }

        private static WildcardValue replaceWildcards(boolean wildcardSupport, String value) {
            return wildcardSupport ? new WildcardValue(value.contains("*") || value.contains("?"),
                    value.replace("*", "%").replace("?", "_"))
                    : new WildcardValue(false, value);
        }
    }

    @SuppressWarnings({"CyclomaticComplexity", "ReturnCount", "MissingSwitchDefault"})
    Predicate predicateFromFilter(CriteriaBuilder cb, Expression<?> expression,
            FilterMeta filter, Object filterValue) {
        var stringExpression = new Lazy<>(() -> new ExpressionEvaluator(cb, expression, filterValue));
        Lazy<Collection<?>> filterValueAsCollection = new Lazy<>(() -> (Collection<?>) filterValue);
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
                if (wildcardSupport && stringExpression.get().hasWildcards) {
                    return cb.like(stringExpression.get().expression, stringExpression.get().value);
                } else {
                    return cb.equal(expression, filterValue);
                }
            case EQUALS:
                return cb.equal(expression, filterValue);
            case NOT_EXACT, NOT_EQUALS:
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
    <TC extends Comparable<? super TC>> Predicate predicateFromFilterComparable(CriteriaBuilder cb,
            Expression<TC> objectExpression, FilterMeta filter, TC filterValue, Object filterValueCollection) {
        @SuppressWarnings("unchecked")
        Lazy<Collection<TC>> filterValueAsCollection = new Lazy<>(() -> (Collection<TC>) filterValueCollection);
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
    private List<Order> processSortOrder(Map<String, MergedSortOrder> sortMeta, CriteriaBuilder cb, Root<TT> root) {
        Deque<Order> sortMetaOrdering = new ArrayDeque<>();
        sortMeta.values().forEach(order -> {
            if (order.getRequestedSortMeta() != null) {
                if (order.getRequestedSortMeta().getOrder() == SortOrder.ASCENDING) {
                    sortMetaOrdering.add(cb.asc(resolveField(root, order.getRequestedSortMeta().getField())));
                } else if (order.getRequestedSortMeta().getOrder() == SortOrder.DESCENDING) {
                    sortMetaOrdering.add(cb.desc(resolveField(root, order.getRequestedSortMeta().getField())));
                }
            } else if (order.getApplicationSort() != null) {
                if (order.isHighPriority()) {
                    sortMetaOrdering.addFirst(order.getApplicationSort());
                } else {
                    sortMetaOrdering.add(order.getApplicationSort());
                }
            } else {
                throw new IllegalStateException("Neither application sort request, nor UI sort request is available");
            }
        });
        return sortMetaOrdering.stream().toList();
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private Object getPrimaryKey(Optional<TT> entry) {
        return jpaFinder.get().getEntityManager().get().getEntityManagerFactory().getPersistenceUnitUtil()
                .getIdentifier(entry.orElse(ConstructorUtils.invokeConstructor(getEntityClass())));
    }

    private Class<?> getPrimaryKeyClass() {
        return getPrimaryKey(Optional.empty()).getClass();
    }

    /**
     * Unfortunately, CDI beans like {@link JPALazyDataModel} cannot contain serialization-related methods
     * as Weld doesn't handle that well. We have to put all those methods here and make this class truly Serializable
     *
     * @return new, corrected object
     * @throws ObjectStreamException
     */
    Object readResolve() throws ObjectStreamException {
        var corrected = create(x_do_not_use_in_builder);
        corrected.x_do_not_use_in_builder = x_do_not_use_in_builder;
        return corrected;
    }
}
