/*
 * Copyright 2020 lprimak.
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
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import java.io.Serializable;
import java.util.stream.Collectors;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import lombok.Getter;

/**
 *
 * @author lprimak
 */
@Named
@ViewScoped
public class UserViewer implements Serializable {
    private static final long serialVersionUID = 1L;
    @PersistenceContext
    private EntityManager em;


    public String getUsers() {
        return em.createQuery("select u from UserEntity u", UserEntity.class).getResultStream()
                .map(UserEntity::getFullName).collect(Collectors.joining(", "));
    }

    private @Getter final JPALazyDataModel<UserEntity, Long> lazyModel =
            JPALazyDataModel.create(builder -> builder
                    .entityManagerSupplier(() -> em)
                    .entityClass(UserEntity.class)
                    .converter(Long::parseLong)
                    .caseSensitiveQuery(false)
                    .build());
}
