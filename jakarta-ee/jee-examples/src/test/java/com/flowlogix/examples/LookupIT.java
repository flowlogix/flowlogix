/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import lombok.Lombok;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
@ExtendWith(ArquillianExtension.class)
public class LookupIT {
    private JndiExample example;


    @BeforeEach
    public void before() {
        example = new JndiExample();
    }

    @AfterEach
    public void after() {
        example = null;
    }

    @Test
    public void happyPath() {
        assertEquals(5, example.getNumber());
        assertNotNull(example.getLocator().getObject(AnotherEJB.class), "should not be null");
    }

    @Test
    public void unhappyPath() throws NamingException {
        assertNull(example.getLocator().getObject("hello"));
    }

    @Test
    @Timeout(10)
    @Tag("StressTest")
    public void stressTest() throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(50 *
                Runtime.getRuntime().availableProcessors());
        AtomicBoolean failed = new AtomicBoolean();
        IntStream.rangeClosed(1, 10000).forEach(ii -> exec.submit(() -> {
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
            }
        }));
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);
        assertFalse(failed.get(), "stress test failed");
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "LookupTest.war")
                .addPackages(true, "org.omnifaces")
                .addPackages(true, "com.flowlogix")
                .deletePackages(true, "com.flowlogix.examples.data")
                .deletePackages(true, "com.flowlogix.examples.entities")
                .deletePackages(true, "com.flowlogix.examples.ui")
                .deletePackages(true, "com.flowlogix.logcapture")
                .deletePackages(true, "org.omnifaces.persistence")
                .deleteClass(ExceptionPageIT.class);
    }
}
