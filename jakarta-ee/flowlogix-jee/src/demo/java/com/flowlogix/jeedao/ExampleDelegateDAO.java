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

import com.flowlogix.jeedao.DaoHelper.EntityManagerExclusions;
import com.flowlogix.jeedao.entities.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import lombok.experimental.Delegate;

// @start region="delegateDAO"
// tag::delegateDAO[] // @replace regex='.*\n' replacement=""
@Stateless
public class ExampleDelegateDAO {
    @Inject
    @Delegate(excludes = EntityManagerExclusions.class)
    EntityManager entityManager;
    @Inject
    @Delegate
    DaoHelper<UserEntity> helper;
}
// end::delegateDAO[] // @replace regex='.*\n' replacement=""
// @end
