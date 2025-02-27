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

import com.flowlogix.examples.jndi.ejbs.AnotherEJB;
import com.flowlogix.examples.jndi.ejbs.NumberGetter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import javax.naming.NamingException;
import com.flowlogix.testcontainers.PayaraServerLifecycleExtension;
import lombok.Lombok;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author lprimak
 */
@ExtendWith(PayaraServerLifecycleExtension.class)
@ExtendWith(ArquillianExtension.class)
@SuppressWarnings("MagicNumber")
class LookupIT {
    static final String DEPLOYMENT_NAME = "LookupIT";
    private JndiExample example;


    @BeforeEach
    void before() {
        example = new JndiExample();
    }

    @AfterEach
    void after() {
        example = null;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_NAME)
    void happyPath() {
        assertThat(example.getNumber()).isEqualTo(5);
        assertThat(example.getLocator().getObject(AnotherEJB.class)).as("should not be null").isNotNull();
        assertThat(example.createLocatorWithEnvironment().getObject(AnotherEJB.class)).as("should not be null").isNotNull();
        assertThat(example.createLocatorWithNoCaching().getObject(AnotherEJB.class)).as("should not be null").isNotNull();
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_NAME)
    void unhappyPath() throws NamingException {
        assertNull(example.getLocator().getObject("hello"));
    }

    @Test
    @Timeout(10)
    @Tag("StressTest")
    @OperateOnDeployment(DEPLOYMENT_NAME)
    void stressTest() throws InterruptedException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ExecutorService exec = Executors.newFixedThreadPool(50
                * Runtime.getRuntime().availableProcessors());
        AtomicBoolean failed = new AtomicBoolean();
        IntStream.rangeClosed(1, 10000).forEach(ii -> exec.submit(() -> {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            try {
                NumberGetter target1 = example.getLocator().getObjectNoCache("java:module/NumberGetter");
                assertEquals(5, target1.getNumber());
                NumberGetter target2 = example.getLocator().getObject(NumberGetter.class);
                assertEquals(5, target2.getNumber());
                assertNotNull(example.getLocator().getObject(AnotherEJB.class));
                assertNull(example.getLocator().getObject("hello"));
            } catch (Throwable thr) {
                failed.set(true);
                throw Lombok.sneakyThrow(thr);
            } finally {
                Thread.currentThread().setContextClassLoader(null);
            }
        }));
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(failed.get()).as("stress test failed").isFalse();
    }

    @Deployment(name = DEPLOYMENT_NAME)
    static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, String.format("%s-%s", UUID.randomUUID(), "LookupTest.war"))
                .addPackages(true, "org.omnifaces")
                .addPackages(true, "org.assertj")
                .addPackages(true, "com.flowlogix")
                .deletePackages(true, "com.flowlogix.demo")
                .deletePackages(true, "com.flowlogix.examples.data")
                .deletePackages(true, "com.flowlogix.examples.entities")
                .deletePackages(true, "com.flowlogix.examples.ui")
                .deletePackages(true, "com.flowlogix.logcapture")
                .deletePackages(true, "com.flowlogix.jeedao")
                .deleteClass(ExceptionPageIT.class);
    }
}
