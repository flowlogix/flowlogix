/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.examples;

import com.flowlogix.examples.jndi.ejbs.AnotherEJB;
import com.flowlogix.examples.jndi.ejbs.NumberGetter;
import com.flowlogix.test.ArquillianTest;
import com.flowlogix.test.StressTest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
@Category(ArquillianTest.class)
public class LookupTest {
    private JndiExample example;


    @Before
    public void before() {
        example = new JndiExample();
    }

    @After
    public void after() {
        example = null;
    }

    @Test
    public void happyPath() {
        assertEquals(5, example.getNumber());
        assertNotNull("should not be null", example.getLocator().getObject(AnotherEJB.class));
    }

    @Test
    public void unhappyPath() throws NamingException {
        assertThrows(NamingException.class, () -> example.getLocator().getObject("hello"));
    }

    @Test(timeout = 10 * 1000)
    @Category(StressTest.class)
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
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "LookupTest.war")
                .addPackages(true, "com.flowlogix");
    }
}
