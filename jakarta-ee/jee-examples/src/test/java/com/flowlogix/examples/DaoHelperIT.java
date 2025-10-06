/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.examples;

import com.flowlogix.demo.jeedao.DaoHelperDemo;
import com.flowlogix.demo.jeedao.UserDAO;
import com.flowlogix.test.PayaraServerLifecycle;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import static com.flowlogix.examples.ExceptionPageIT.DEPLOYMENT_DEV_MODE;
import static org.assertj.core.api.Assertions.assertThat;

@PayaraServerLifecycle
class DaoHelperIT {
    @Inject
    UserDAO userDao;
    @Inject
    DaoHelperDemo demo;

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    @SuppressWarnings("MagicNumber")
    void extractedCountAndList() {
        assertThat(userDao.count()).isEqualTo(5);
        assertThat(userDao.countAndList("Cool Cousin")).isEqualTo(userDao.extractedCountAndList("Cool Cousin"));
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    @SuppressWarnings("MagicNumber")
    void demo() {
        assertThat(demo.count()).isEqualTo(5);
        var users = demo.enhancedFind("Lovely Lady");
        assertThat(users.count()).isEqualTo(1);
        var user = users.list().stream().findFirst().orElseThrow();
        assertThat(user.getUserId()).isEqualTo("jprimak");
        assertThat(demo.findById(user.getId()).getFullName()).isEqualTo(user.getFullName());
        assertThat(demo.injectedCount()).isEqualTo(5);
        assertThat(demo.inheritedCount()).isEqualTo(5);
        assertThat(demo.nativeFind("""
                        select * from userentity
                        where zipcode = 68502 order by userid limit 2""")
                .getUserId()).isEqualTo("anya");
    }

    @Deployment(name = DEPLOYMENT_DEV_MODE)
    static WebArchive createDeployment() {
        return ExceptionPageIT.createDeploymentDev();
    }
}
