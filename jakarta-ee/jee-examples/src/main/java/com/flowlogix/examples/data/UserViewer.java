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
package com.flowlogix.examples.data;

import com.flowlogix.demo.jeedao.entities.UserEntity;
import com.flowlogix.demo.jeedao.entities.UserEntity_;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.replaceFilter;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;

/**
 *
 * @author lprimak
 */
@Named
@ViewScoped
public class UserViewer implements Serializable {
    private static final long serialVersionUID = 1L;

    // @start region="simpleLazyDataModelUsage"
    // tag::simpleLazyDataModelUsage[] // @replace regex='.*\n' replacement=""
    private @Getter final JPALazyDataModel<UserEntity, Long> lazyModel =
            JPALazyDataModel.create(builder -> builder
                    .entityClass(UserEntity.class)
                    // the line below is optional, default is case-sensitive (true)
                    .caseSensitiveQuery(false)
                    // tag::simpleOptionalLazyDataModelUsage[] // @replace regex='.*\n' replacement=""
                    // the following 2 lines are optional
//                    .sorter(UserViewer::sorter) // @replace regex="^\/\/" replacement=""
//                    .filter(UserViewer::filter) // @replace regex="^\/\/" replacement=""
                    // end::simpleOptionalLazyDataModelUsage[] // @replace regex='.*\n' replacement=""
                    .build());
    // end::simpleLazyDataModelUsage[] // @replace regex='.*\n' replacement=""
    // @end

    public String getUsers() {
        return lazyModel.getEntityManager().get()
                .createQuery("select u from UserEntity u", lazyModel.getEntityClass()).getResultStream()
                .map(UserEntity::getFullName).collect(Collectors.joining(", "));
    }

    private static boolean sorter(SortData sortData, CriteriaBuilder cb, Root<UserEntity> root) {
        sortData.getSortOrder().add(cb.asc(root.get(UserEntity_.address)));
        return false;
    }

    private static void filter(Map<String, FilterData> filters, CriteriaBuilder cb, Root<UserEntity> root) {
        replaceFilter(filters, UserEntity_.zipCode.getName(),
                (Predicate predicate, Integer value) -> cb.greaterThan(root.get(UserEntity_.zipCode), value));
    }
}
