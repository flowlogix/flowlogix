/*
 * Copyright (C) 2011-2022 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.examples.data;

import com.flowlogix.examples.entities.UserEntity;
import com.flowlogix.jeedao.DaoHelper;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author lprimak
 */
@Singleton @Startup
public class Initializer {
    @PersistenceContext
    private EntityManager em;

    @PostConstruct
    @SuppressWarnings("MagicNumber")
    void init() {
        var helper = DaoHelper.<UserEntity, Long>builder()
                .entityClass(UserEntity.class)
                .entityManagerSupplier(() -> em)
                .build();
        if (helper.count() == 0) {
            List<UserEntity> userList = Stream.of(
                    UserEntity.builder().userId("lprimak").fullName("Lenny Primak")
                            .address("Tree-Lined Blvd").zipCode(68502).build(),
                    UserEntity.builder().userId("jprimak").fullName("Lovely Lady")
                            .address("Tree-Lined Street").zipCode(68502).build(),
                    UserEntity.builder().userId("anya").fullName("Lovely Daughter")
                            .address("Tree-Lined Hill").zipCode(68502).build(),
                    UserEntity.builder().userId("friend").fullName("Friendly Pal")
                            .address("NY, Somewhere").zipCode(10012).build(),
                    UserEntity.builder().userId("cousin").fullName("Cool Cousin")
                            .address("Beastly Court").zipCode(68502).build()
            ).collect(Collectors.toList());
            userList.forEach(em::merge);
        }
    }
}
