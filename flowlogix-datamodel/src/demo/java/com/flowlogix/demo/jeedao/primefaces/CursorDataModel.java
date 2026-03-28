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
import com.flowlogix.jeedao.primefaces.CursorPagination;
import com.flowlogix.jeedao.primefaces.CursorPagination.Field;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import java.io.Serializable;
import java.util.List;

// @start region="cursorUsage"
// tag::optimized[] // @replace regex='.*\n' replacement=""
@Named
@ViewScoped
public class CursorDataModel implements Serializable {
    @Inject
    @Getter
    JPALazyDataModel<UserEntity> userModel;

    @PostConstruct
    void initialize() {
        // configure cursor pagination by id field
        userModel.initialize(builder -> builder
                .cursor(CursorPagination.create(config -> config
                        .supportedFields(List.of(
                                new Field<>(UserEntity_.id.getName(), UserEntity::getId)))
                        .build()))
                .build());
    }
}
// end::optimized[] // @replace regex='.*\n' replacement=""
// @end
