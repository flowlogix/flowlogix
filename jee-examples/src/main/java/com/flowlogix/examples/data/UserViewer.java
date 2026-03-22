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
package com.flowlogix.examples.data;

import com.flowlogix.demo.jeedao.entities.UserEntity;
import com.flowlogix.demo.jeedao.entities.UserEntity_;
import com.flowlogix.jeedao.primefaces.CursorPagination;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import com.flowlogix.jeedao.primefaces.LazyModelConfig;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

/**
 *
 * @author lprimak
 */
@Slf4j
@Named
@ViewScoped
public class UserViewer implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    // @start region="simpleLazyDataModelUsage"
    // tag::simpleLazyDataModelUsage[] // @replace regex='.*\n' replacement=""
    @Inject
    @Getter
    // optional configuration annotation
    @LazyModelConfig(caseInsensitive = true)
    JPALazyDataModel<UserEntity> lazyModel;
    // end::simpleLazyDataModelUsage[] // @replace regex='.*\n' replacement=""
    // @end

    private final NavigableMap<Integer, Long> cursorCache = new TreeMap<>();
    private Map<String, FilterMeta> cursorFilters = new LinkedHashMap<>();
    private Map<String, SortMeta> cursorSorts = new LinkedHashMap<>();
    private boolean isDescending;

    /**
     * Enable cursor pagination, and optionally sort and filter
     */
    @PostConstruct
    void initialize() {
        lazyModel.initialize(builder -> builder.cursor(cursor())
                .sorter(UserViewer::cursorSorter)
                // uncomment the below line to activate application sort and filter
                // .sorter(UserViewer::sorter).filter(UserViewer::filter)
                .build());
    }

    public String getUsers() {
        return lazyModel.getEntityManager().get()
                .createQuery("select u from UserEntity u", lazyModel.getEntityClass()).getResultStream()
                .map(u -> "%d -> %s".formatted(u.getId(), u.getFullName())).collect(Collectors.joining(", "));
    }

    @SuppressWarnings("checkstyle:AnonInnerLength")
    private CursorPagination<UserEntity> cursor() {
        return new CursorPagination<>() {
            @Override
            public void save(int offset, UserEntity entity) {
                log.info("Saving cursor for offset {} and entity id {}", offset, entity.getId());
                cursorCache.put(offset, entity.getId());
            }

            @Override
            public int cursorOffset(int offset) {
                int returnedOffset = Optional.ofNullable(cursorCache.floorKey(offset))
                        .map(key -> offset - key).orElse(offset);
                log.info("Cursor offset {}, floorKey = {} returned {}", offset, cursorCache.floorKey(offset), returnedOffset);
                return returnedOffset;
            }

            @Override
            public Predicate cursorPredicate(int offset, CriteriaBuilder cb, Root<UserEntity> root,
                                             Map<String, SortMeta> sortMeta) {
                log.info("Creating cursor predicate for offset {} - cache = {}", offset,
                        Optional.ofNullable(cursorCache.floorEntry(offset))
                                .map(Map.Entry::getValue).orElse(null));
                boolean descending = Optional.ofNullable(sortMeta.get(UserEntity_.id.getName()))
                        .map(order -> order.getOrder().isDescending())
                        .orElse(false);
                return Optional.ofNullable(cursorCache.floorEntry(offset))
                        .map(entry -> descending
                                ? cb.lessThan(root.get(UserEntity_.id), entry.getValue())
                                : cb.greaterThan(root.get(UserEntity_.id), entry.getValue())).orElse(null);
            }

            @Override
            public boolean isSupported(Map<String, FilterMeta> filterMeta, Map<String, SortMeta> sortMeta) {
                if (cursorFilters == null || cursorSorts == null) {
                    cursorFilters = filterMeta;
                    cursorSorts = sortMeta;
                }
                if (!cursorFilters.equals(filterMeta) || !cursorSorts.equals(sortMeta)) {
                    cursorFilters = filterMeta;
                    cursorSorts = sortMeta;
                    cursorCache.clear();
                    return false;
                }
                if (!sortMeta.isEmpty() && sortMeta.keySet().stream().findFirst().map(String::toLowerCase)
                        .filter(UserEntity_.id.getName()::equals).isEmpty()) {
                    log.warn("Cursor pagination only supports sorting by id column, requested sort: {}", sortMeta.keySet());
                    cursorCache.clear();
                    return false;
                } else if (isDescending != Optional.ofNullable(sortMeta.get(UserEntity_.id.getName()))
                        .map(sort -> sort.getOrder().isDescending()).orElse(false)) {
                    isDescending = !isDescending;
                    cursorCache.clear();
                }

                return true;
            }
        };
    }

    private static void cursorSorter(SortData sortData, CriteriaBuilder cb, Root<UserEntity> root) {
        if (sortData.getSortOrder().isEmpty()) {
            sortData.applicationSort(UserEntity_.id.getName(), true, sortMeta -> cb.asc(root.get(UserEntity_.id)));
        }
    }

    private static void sorter(SortData sortData, CriteriaBuilder cb, Root<UserEntity> root) {
        sortData.applicationSort(UserEntity_.address.getName(), sortMeta -> cb.asc(root.get(UserEntity_.address)));
        cursorSorter(sortData, cb, root);
    }

    private static void filter(FilterData filterData, CriteriaBuilder cb, Root<UserEntity> root) {
        filterData.replaceFilter(UserEntity_.zipCode.getName(),
                (Predicate predicate, Integer value) -> cb.greaterThan(root.get(UserEntity_.zipCode), value));
    }
}
