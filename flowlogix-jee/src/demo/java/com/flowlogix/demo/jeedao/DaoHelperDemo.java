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

import com.flowlogix.demo.jeedao.UserDAO.CountAndList;
import com.flowlogix.demo.jeedao.entities.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ApplicationScoped
public class DaoHelperDemo {
    @Inject
    ExampleDAO dao;
    @Inject
    InjectedDAO injectedDAO;
    @Inject
    ExampleDelegateDAO daoWithDelegate;
    @Inject
    InheritedDAO inheritedDAO;
    @Inject
    UserDAO userDAO;

    public long count() {
        return dao.count();
    }

    public long injectedCount() {
        return injectedDAO.count();
    }

    public long inheritedCount() {
        return inheritedDAO.count();
    }

    public UserEntity findById(Long id) {
        return daoWithDelegate.find(daoWithDelegate.getEntityClass(), id);
    }

    public CountAndList enhancedFind(String userName) {
        return userDAO.countAndList(userName);
    }

    public UserEntity nativeFind(String sql) {
        return userDAO.findByNative(sql).stream().findFirst().orElseThrow();
    }
}
