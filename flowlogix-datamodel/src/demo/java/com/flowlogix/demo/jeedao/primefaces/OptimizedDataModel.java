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
import org.eclipse.persistence.annotations.BatchFetchType;
import org.eclipse.persistence.config.QueryHints;
import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.getResultField;

// @start region="optimized"
// tag::optimized[] // @replace regex='.*\n' replacement=""
@Named
@ViewScoped
public class OptimizedDataModel implements Serializable {
    @Inject
    @Getter
    JPALazyDataModel<UserEntity> userModel;

    @PostConstruct
    void initialize() {
        // optimize query by batching relationship fetching
        userModel.initialize(builder -> builder.optimizer(query -> query
                        .setHint(QueryHints.BATCH, getResultField(UserEntity_.userSettings.getName()))
                        .setHint(QueryHints.BATCH, getResultField(UserEntity_.alternateEmails.getName()))
                        .setHint(QueryHints.BATCH_TYPE, BatchFetchType.IN))
                .build());
    }
}
// end::optimized[] // @replace regex='.*\n' replacement=""
// @end
