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
import com.flowlogix.jeedao.primefaces.CursorPagination.CursorData;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import com.flowlogix.jeedao.primefaces.LazyModelConfig;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
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

    /**
     * Enable cursor pagination, and optionally sort and filter
     */
    @PostConstruct
    void initialize() {
        lazyModel.initialize(builder -> builder
                .cursor(new CursorData<>(() -> Map.of(
                        UserEntity_.id.getName(), UserEntity::getId,
                        UserEntity_.zipCode.getName(), UserEntity::getZipCode)))
                .sorter(this::cursorSorter)
                // uncomment the below line to activate application sort and filter
//                 .sorter(this::sorter).filter(UserViewer::filter)
                .build());
    }

    public String getUsers() {
        return lazyModel.getEntityManager().get()
                .createQuery("select u from UserEntity u", lazyModel.getEntityClass()).getResultStream()
                .map(u -> "%d -> %s".formatted(u.getId(), u.getFullName())).collect(Collectors.joining(", "));
    }

    private void cursorSorter(SortData sortData, CriteriaBuilder cb, Root<UserEntity> root) {
        if (sortData.getSortOrder().isEmpty()) {
            lazyModel.getCursor().setCurrentColumn(UserEntity_.id.getName());
            sortData.applicationSort(UserEntity_.id.getName(), true, sortMeta -> cb.asc(root.get(UserEntity_.id)));
        }
    }

    private void sorter(SortData sortData, CriteriaBuilder cb, Root<UserEntity> root) {
        sortData.applicationSort(UserEntity_.address.getName(), sortMeta -> cb.asc(root.get(UserEntity_.address)));
        cursorSorter(sortData, cb, root);
    }

    private static void filter(FilterData filterData, CriteriaBuilder cb, Root<UserEntity> root) {
        filterData.replaceFilter(UserEntity_.zipCode.getName(),
                (Predicate predicate, Integer value) -> cb.greaterThan(root.get(UserEntity_.zipCode), value));
    }
}
