/*
 * Copyright (C) 2011-2022 Flow Logix, Inc. All Rights Reserved.
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

import com.flowlogix.examples.entities.UserEntity;
import com.flowlogix.examples.entities.UserEntity_;
import com.flowlogix.jeedao.primefaces.Filter.FilterData;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.replaceFilter;
import com.flowlogix.jeedao.primefaces.Sorter.SortData;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.Getter;

/**
 *
 * @author lprimak
 */
@Named
@ViewScoped
public class UserViewer implements Serializable {
    private static final long serialVersionUID = 1L;
    @PersistenceContext
    private EntityManager em;

    private @Getter final JPALazyDataModel<UserEntity, Long> lazyModel =
            JPALazyDataModel.create(builder -> builder
                    .entityManagerSupplier(() -> em)
                    .entityClass(UserEntity.class)
                    .converter(Long::parseLong)
                    .caseSensitiveQuery(false)
//                    .sorter(UserViewer::sorter)
//                    .filter(UserViewer::filter)
                    .build());

    public String getUsers() {
        return em.createQuery("select u from UserEntity u", UserEntity.class).getResultStream()
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
