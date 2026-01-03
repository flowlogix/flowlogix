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

import com.flowlogix.demo.viewscoped.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import java.io.Serializable;

@Named
@ViewScoped
@Getter
public class DataModelWrapper implements Serializable {
    @Inject
    BasicDataModel basic;

    @Inject
    DirectCreationDataModel direct;

    @Inject
    ConverterDataModel converter;

    @Inject
    FilteringDataModel filtering;

    @Inject
    OptimizedDataModel optimized;

    @Inject
    EnrichedDataModel enriched;

    @Inject
    QualifiedDataModel qualified;

    @Inject
    SortingDataModel sorting;

    @Inject
    InvalidDataModel invalid;
}
