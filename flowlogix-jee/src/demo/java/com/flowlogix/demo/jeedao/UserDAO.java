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
package com.flowlogix.demo.jeedao;

import com.flowlogix.api.dao.JPANativeQuery;
import com.flowlogix.api.dao.JPAFinder.QueryEnhancement;
import com.flowlogix.demo.jeedao.entities.UserEntity;
import com.flowlogix.demo.jeedao.entities.UserEntity_;
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
    JPANativeQuery<UserEntity> jpaFinder;
    // @start region="daoParameters"
    // tag::daoParameters[] // @replace regex='.*\n' replacement=""
    public record CountAndList(long count, List<UserEntity> list) { };
    public CountAndList countAndList(String userName) {
        // add "where fullName = 'userName'" clause
        QueryEnhancement<UserEntity> enhancement = (partial, criteria) -> criteria
                .where(partial.builder().equal(partial.root()
                        .get(UserEntity_.fullName), userName));

        return new CountAndList(jpaFinder.count(enhancement::accept),
                jpaFinder.findAll(enhancement::accept)
                .setHint(QueryHints.BATCH_TYPE, BatchFetchType.IN)
                .getResultList());
    }
    // end::daoParameters[] // @replace regex='.*\n' replacement=""
    // @end

    // @start region="daoExtractedParameters"
    // tag::daoExtractedParameters[] // @replace regex='.*\n' replacement=""
    public CountAndList extractedCountAndList(String userName) {
        // add "where fullName = 'userName'" clause
        QueryEnhancement<UserEntity> enhancement = (partial, criteria) -> criteria
                .where(partial.builder().equal(partial.root()
                        .get(UserEntity_.fullName), userName));
        // descending order for queries
        QueryEnhancement<UserEntity> orderBy = (partial, criteria) -> criteria
                .orderBy(partial.builder().desc(partial.root().get(UserEntity_.fullName)));

        return new CountAndList(jpaFinder.count(enhancement::accept),
                jpaFinder.findAll(enhancement.andThen(orderBy)::accept)
                .getResultList());
    }
    // end::daoExtractedParameters[] // @replace regex='.*\n' replacement=""
    // @end

    // @start region="nativeQuery"
    // tag::nativeQuery[] // @replace regex='.*\n' replacement=""
    public List<UserEntity> findByNative(String sql) {
        return jpaFinder.createNativeQuery(sql, jpaFinder.getEntityClass()).getResultList();
    }
    // end::nativeQuery[] // @replace regex='.*\n' replacement=""
    // @end
}
