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
import com.flowlogix.examples.ui.LazyModelParameters;
import com.flowlogix.jeedao.primefaces.CursorPagination;
import com.flowlogix.jeedao.primefaces.CursorPagination.Field;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import com.flowlogix.jeedao.primefaces.LazyModelConfig;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import org.omnifaces.util.Lazy;

/**
 *
 * @author lprimak
 */
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

    @Inject
    LazyModelParameters lazyModelParameters;

    @PostConstruct
    void initialize() {
        lazyModel.initialize(builder -> builder
                .sorter(this::sorter)
                .filter(this::filter)
                .resultEnricher(this::forceEmptyResult)
                .cursor(cursorPagination())
                .build());
    }

    public String getUsers() {
        return lazyModel.getEntityManager().get()
                .createQuery("select u from UserEntity u", lazyModel.getEntityClass()).getResultStream()
                .map(UserEntity::getFullName).collect(Collectors.joining(", "));
    }

    private void sorter(SortData sortData, CriteriaBuilder cb, Root<UserEntity> root) {
        if (lazyModelParameters.isSortingAndFiltering()) {
            sortData.applicationSort(UserEntity_.address.getName(), sortMeta -> cb.asc(root.get(UserEntity_.address)));
        }
    }

    private void filter(FilterData filterData, CriteriaBuilder cb, Root<UserEntity> root) {
        if (lazyModelParameters.isSortingAndFiltering()) {
            filterData.replaceFilter(UserEntity_.zipCode.getName(),
                    (Predicate predicate, Integer value) -> cb.greaterThan(root.get(UserEntity_.zipCode), value));
        }
    }

    private Lazy<CursorPagination<UserEntity>> cursorPagination() {
        return CursorPagination.create(builder -> builder
                .supportedFields(List.of(new Field<>(UserEntity_.id.getName(), UserEntity::getId)))
                .conditional(() -> lazyModelParameters.isDefaultCursorPagination()).build());
    }

    private List<UserEntity> forceEmptyResult(List<UserEntity> users) {
        if (lazyModelParameters.isForceEmptyResult()) {
            return List.of();
        }
        return users;
    }
}
