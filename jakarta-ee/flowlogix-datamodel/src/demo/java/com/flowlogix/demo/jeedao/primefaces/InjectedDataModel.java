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
import com.flowlogix.jeedao.primefaces.LazyModelConfig;
import com.flowlogix.demo.viewscoped.ViewScoped;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import java.io.Serializable;
import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.FilterCaseConversion.LOWER;
import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.FilterCaseConversion.UPPER;

@Named
@ViewScoped
@Getter
public class InjectedDataModel implements Serializable {
    @Inject
    JPALazyDataModel<UserEntity, Long> injectedModel;

    @Inject
    JPALazyDataModel<UserEntity, Long> injectedOverriddenModel;

    @Inject
    @LazyModelConfig(caseInsensitive = true)
    JPALazyDataModel<UserEntity, Long> injectedCaseInsensitiveModel;

    @Inject
    @LazyModelConfig(caseInsensitive = true, filterCaseConversion = LOWER)
    JPALazyDataModel<UserEntity, Long> injectedCaseInsensitiveLowerModel;

    @Inject
    @LazyModelConfig(caseInsensitive = true, filterCaseConversion = UPPER)
    JPALazyDataModel<UserEntity, Long> injectedCaseInsensitiveUpperModel;

    @Inject
    @LazyModelConfig(entityManagerSelector = NonDefault.class)
    JPALazyDataModel<UserEntity, Long> injectedNonDefaultModel;

    @PostConstruct
    void postConstruct() {
        injectedOverriddenModel.initialize(builder -> builder.caseSensitiveFilter(false).build());
    }
}
