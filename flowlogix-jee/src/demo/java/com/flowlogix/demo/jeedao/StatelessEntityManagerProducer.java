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

import jakarta.annotation.Priority;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Getter;
import static jakarta.ejb.TransactionAttributeType.SUPPORTS;
import static jakarta.interceptor.Interceptor.Priority.APPLICATION;

// @start region="statelessEntityProducer"
// tag::statelessEntityProducer[] // @replace regex='.*\n' replacement=""
@Stateless
@Alternative
@Priority(APPLICATION + 1)
@TransactionAttribute(SUPPORTS)
public class StatelessEntityManagerProducer {
    @Getter(onMethod = @__({@Produces, @NonDefault}))
    @PersistenceContext(unitName = "nonDefault")
    EntityManager entityManager;
}
// end::statelessEntityProducer[] // @replace regex='.*\n' replacement=""
// @end
