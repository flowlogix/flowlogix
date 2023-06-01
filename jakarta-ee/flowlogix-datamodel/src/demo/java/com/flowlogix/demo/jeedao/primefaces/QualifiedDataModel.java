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
package com.flowlogix.demo.jeedao.primefaces;

import com.flowlogix.demo.jeedao.NonDefault;
import com.flowlogix.demo.jeedao.entities.UserEntity;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;

// @start region="qualifier"
// tag::qualifier[] // @replace regex='.*\n' replacement=""
@Named
@ViewScoped
public class QualifiedDataModel implements Serializable {
    @Getter
    private final JPALazyDataModel<UserEntity, Long> userModel = JPALazyDataModel
            .create(builder -> builder
                    .entityManagerQualifiers(List.of(NonDefault.class))
                    .entityClass(UserEntity.class)
                    .build());
}
// end::qualifier[] // @replace regex='.*\n' replacement=""
// @end
