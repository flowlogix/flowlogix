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
package com.flowlogix.examples.data;

import com.flowlogix.demo.jeedao.entities.AlternateEmails;
import com.flowlogix.demo.jeedao.entities.LastNameEntity;
import com.flowlogix.demo.jeedao.entities.UserSettings;
import com.flowlogix.jeedao.DaoHelper;
import com.flowlogix.jeedao.EntityManagerSelector;
import com.flowlogix.demo.jeedao.entities.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Stream;
import jakarta.transaction.Transactional;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class Initializer {
    /** sort column has duplicates spanning a page boundary: A B C C C D E F G H. */
    public static final List<String> LAST_NAMES = List.of("A", "B", "C", "C", "C", "D", "E", "F", "G", "H");

    @Inject
    @EntityManagerSelector(AnotherEntityManager.class)
    DaoHelper<UserEntity> helper;

    @SuppressWarnings("MagicNumber")
    void init(@Observes Startup init) {
        if (helper.count() == 0) {
            Stream.of(
                    UserEntity.builder().userId("lprimak").fullName("Lenny Primak")
                            .address("Tree-Lined Blvd").zipCode(68502)
                            .userSettings(List.of(UserSettings.builder()
                                            .settingName("LennySettingOne").settingValue("Setting1Value").build(),
                                    UserSettings.builder().settingName("lennySettingTwo")
                                            .settingValue("Setting1TwoValue").build()))
                            .alternateEmails(List.of(AlternateEmails.builder().email("one@one.com").build(),
                                    AlternateEmails.builder().email("two@two.com").build()))
                            .build(),
                    UserEntity.builder().userId("jprimak").fullName("Lovely Lady")
                            .address("Tree-Lined Street").zipCode(68502).build(),
                    UserEntity.builder().userId("anya").fullName("Lovely Daughter")
                            .address("Tree-Lined Hill").zipCode(68502).build(),
                    UserEntity.builder().userId("friend").fullName("Friendly Pal")
                            .address("NY, Somewhere").zipCode(10012).build(),
                    UserEntity.builder().userId("cousin").fullName("Cool Cousin")
                            .address("Beastly Court").zipCode(68502).build()
            ).forEach(helper.getEntityManager().get()::merge);

            for (int i = 0; i < LAST_NAMES.size(); i++) {
                helper.getEntityManager().get().merge(LastNameEntity.builder()
                        .userId("dup" + i).fullName(LAST_NAMES.get(i)).zipCode(68501).build());
            }
        }
    }
}
