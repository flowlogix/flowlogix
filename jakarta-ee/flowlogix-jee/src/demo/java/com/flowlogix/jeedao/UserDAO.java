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
package com.flowlogix.jeedao;

import com.flowlogix.jeedao.DaoHelper.ParameterFunction;
import com.flowlogix.jeedao.DaoHelper.QueryEnhancement;
import com.flowlogix.jeedao.entities.UserEntity;
import com.flowlogix.jeedao.entities.UserEntity_;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import lombok.experimental.Delegate;
import org.eclipse.persistence.annotations.BatchFetchType;
import org.eclipse.persistence.config.QueryHints;
import java.util.List;

/**
 * Demonstrates enhanced queries
 */
@Stateless
public class UserDAO {
    @Inject
    @Delegate
    DaoHelper<UserEntity> helper;
    // @start region="daoParameters"
    // tag::daoParameters[] // @replace regex='.*\n' replacement=""
    public record CountAndList(long count, List<UserEntity> list) { };
    public CountAndList countAndList(String userName) {
        // add "where userName = 'userName'" clause
        QueryEnhancement<UserEntity> enhancement = (partial, criteria) -> criteria
                .where(partial.builder().equal(partial.root()
                        .get(UserEntity_.userName), userName));

        return new CountAndList(helper.count(enhancement::build), helper.findAll(enhancement::build)
                .setHint(QueryHints.BATCH_TYPE, BatchFetchType.IN)
                .getResultList());
    }
    // end::daoParameters[] // @replace regex='.*\n' replacement=""
    // @end

    // @start region="daoExtractedParameters"
    // tag::daoExtractedParameters[] // @replace regex='.*\n' replacement=""
    public CountAndList extractedCountAndList(String userName) {
        // add "where userName = 'userName'" clause
        QueryEnhancement<UserEntity> enhancement = (partial, criteria) -> criteria
                .where(partial.builder().equal(partial.root()
                        .get(UserEntity_.userName), userName));
        // descending order for queries
        QueryEnhancement<UserEntity> orderBy = (partial, criteria) -> criteria
                .orderBy(partial.builder().desc(partial.root().get(UserEntity_.userName)));

        ParameterFunction<UserEntity> params = builder -> builder
                .countQueryCriteria(enhancement::accept)
                .queryCriteria(enhancement.andThen(orderBy)::accept)
                .build();

        return new CountAndList(helper.count(params), helper.findAll(params)
                .getResultList());
    }
    // end::daoExtractedParameters[] // @replace regex='.*\n' replacement=""
    // @end

    // @start region="nativeQuery"
    // tag::nativeQuery[] // @replace regex='.*\n' replacement=""
    public List<UserEntity> findByNative(String someCriteria) {
        return helper.createNativeQuery(someCriteria, UserEntity.class.getName()).getResultList();
    }
    // end::nativeQuery[] // @replace regex='.*\n' replacement=""
    // @end
}
