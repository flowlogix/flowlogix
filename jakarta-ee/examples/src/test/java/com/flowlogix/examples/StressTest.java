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
package com.flowlogix.examples;

import com.flowlogix.examples.jndi.ejbs.AnotherEJB;
import com.flowlogix.examples.jndi.ejbs.NumberGetter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 *
 * @author lprimak
 */
@RunWith(Arquillian.class)
@Category(com.flowlogix.test.StressTest.class)
public class StressTest {
    private JndiExample example;


    @Before
    public void before() {
        example = new JndiExample();
    }

    @After
    public void after() {
        example = null;
    }

    @Test(timeout = 10 * 1000)
    public void stressTest() throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(500);
        AtomicBoolean failed = new AtomicBoolean();
        IntStream.rangeClosed(1, 10000).forEach(ii -> exec.submit(() -> {
            try {
                NumberGetter target1 = example.getLocator().getObject("java:module/NumberGetter", true);
                assertEquals(5, target1.getNumber());
                NumberGetter target2 = example.getLocator().getObject(NumberGetter.class);
                assertEquals(5, target2.getNumber());
                assertNotNull(example.getLocator().getObject(AnotherEJB.class));
                assertThrows(NamingException.class, () -> example.getLocator().getObject("hello"));
            } catch (Throwable thr) {
                failed.set(true);
                throw new RuntimeException(thr);
            }
        }));
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);
        assertFalse("stress test failed", failed.get());
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, "com.flowlogix");
    }
}
