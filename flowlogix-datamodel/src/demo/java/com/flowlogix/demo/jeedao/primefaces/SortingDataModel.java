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
package com.flowlogix.demo.jeedao.primefaces;

import com.flowlogix.demo.jeedao.entities.UserEntity;
import com.flowlogix.demo.jeedao.entities.UserEntity_;
import com.flowlogix.demo.viewscoped.ViewScoped;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import lombok.Getter;

// @start region="sorting"
// tag::sorting[] // @replace regex='.*\n' replacement=""
@Named
@ViewScoped
public class SortingDataModel implements Serializable {
    @Inject
    @Getter
    JPALazyDataModel<UserEntity> userModel;

    @PostConstruct
    void initialize() {
        userModel.initialize(builder -> builder.sorter((sortData, cb, root) ->
                // Example: add an ascending zip code-based sort order
                sortData.applicationSort(UserEntity_.zipCode.getName(),
                                var -> cb.asc(root.get(UserEntity_.zipCode)))
                        // Example: if user requests zip code-based sort order,
                        // add address-based sort order to mirror the zip code-based sort order
                        .applicationSort(UserEntity_.zipCode.getName(), UserEntity_.address.getName(), sortData,
                                () -> cb.asc(root.get(UserEntity_.address)),
                                () -> cb.desc(root.get(UserEntity_.address)), () -> null)).build());
    }
}
// end::sorting[] // @replace regex='.*\n' replacement=""
// @end
