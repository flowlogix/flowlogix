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
package org.omnifaces.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jakarta.ejb.Local;
import jakarta.ejb.Remote;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.Lombok;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.MockedConstruction;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import org.omnifaces.util.JNDIObjectLocator.JNDIObjectLocatorBuilder;

/**
 *
 * @author lprimak
 */
class JndiLocatorTest {
    private JNDIObjectLocator locator;

    @BeforeEach
    void before() {
        locator = JNDIObjectLocator.builder().build();
    }

    @AfterEach
    void after() {
        locator = null;
    }

    @Test
    void portableNamePrefox() {
        assertEquals(JNDI.JNDI_NAMESPACE_MODULE + "/hello", locator.prependNamespaceIfNecessary("hello"));
        assertEquals("java:hello", locator.prependNamespaceIfNecessary("java:hello"));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void withEnvironment() throws Exception {
        Map<String, String> constructedEnvironment = new HashMap<>();
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class, (icObject, context) -> {
            if (context.getCount() == 1) {
                @SuppressWarnings("unchecked")
                Hashtable<String, String> env = (Hashtable<String, String>) context.arguments().get(0);
                constructedEnvironment.putAll(env);
                when(icObject.lookup(anyString())).thenReturn("hello");
            }
        })) {
            JNDIObjectLocatorBuilder builder = JNDIObjectLocator.builder()
                    .environment("oneKey", "oneValue")
                    .environment("twoKey", "twoValue");
            builder.initialHost("myHost");
            builder.initialPort(12345);
            JNDIObjectLocator envLocator = builder.build();
            assertEquals("hello", envLocator.getObject(String.class));
            assertEquals(4, constructedEnvironment.size());
            assertEquals("oneValue", constructedEnvironment.get("oneKey"));
            assertEquals("twoValue", constructedEnvironment.get("twoKey"));
            assertEquals("myHost", constructedEnvironment.get("org.omg.CORBA.ORBInitialHost"));
            assertEquals("12345", constructedEnvironment.get("org.omg.CORBA.ORBInitialPort"));
        }
    }

    @Test
    void basicLocator() throws Exception {
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> when(icObject.lookup(anyString())).thenReturn("hello"))) {
            assertEquals("hello", locator.getObject(String.class));
        }
    }

    @Test
    void basicLocatorBuilder() throws Exception {
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> when(icObject.lookup(anyString())).thenReturn("hello"))) {
            String myString = JNDIObjectLocator.builder().namespace("abc")
                    .cacheRemote().build().getObject(String.class);
            assertEquals("hello", myString);
        }
    }

    @Local
    private final class TestLocal { }

    @Remote
    private final class TestRemote { }

    @Test
    void dontCacheRemote() throws NamingException {
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> when(icObject.lookup(anyString())).thenReturn(new TestRemote()))) {
            assertEquals(TestRemote.class, locator.getObject(TestRemote.class).getClass());
            assertTrue(locator.getJNDIObjectCache().isEmpty(), "object should not be cached");
            locator.getObject(TestLocal.class);
            assertFalse(locator.getJNDIObjectCache().isEmpty(), "object should be cached");
        }
    }

    @Test
    void guessByType() {
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> {
                    // the '$' denotes inner class
                    // i.e. java:module/JndiLocatorTest$Test!com.flowlogix.jndi.JndiLocatorTest$TestLocal
                    when(icObject.lookup(String.format("java:module/%s$Test!%s$TestLocal",
                            getClass().getSimpleName(), getClass().getName()))).thenReturn(new TestLocal());
                    when(icObject.lookup(String.format("java:module/%s$Test!%s$TestRemote",
                            getClass().getSimpleName(), getClass().getName()))).thenReturn(new TestRemote());
                })) {
            assertEquals(TestLocal.class, locator.getObject(TestLocal.class).getClass());
            assertEquals(TestRemote.class, locator.getObject(TestRemote.class).getClass());
            assertNull(locator.getObject(this.getClass()));
        }
    }

    @Test
    void serialization() throws IOException, NamingException, ClassNotFoundException {
        JNDIObjectLocator original = JNDIObjectLocator.builder().environment("one", "two")
                .cacheRemote()
                .build();
        ByteArrayOutputStream bostrm = new ByteArrayOutputStream();
        ObjectOutputStream ostrm = new ObjectOutputStream(bostrm);

        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> when(icObject.lookup(anyString())).thenReturn("hello"))) {
            assertEquals("hello", original.getObject("hello"));
            assertEquals(1, original.getJNDIObjectCache().size());
        }
        ostrm.writeObject(original);
        ostrm.close();
        JNDIObjectLocator xferred = (JNDIObjectLocator) new ObjectInputStream(
                new ByteArrayInputStream(bostrm.toByteArray())).readObject();
        assertEquals(0, xferred.getJNDIObjectCache().size());
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> when(icObject.lookup(anyString())).thenReturn("hello"))) {
            assertEquals("hello", original.getObject("hello"));
            assertEquals(1, original.getJNDIObjectCache().size());
        }
    }

    @Test
    void caching() throws NamingException {
        TestLocal result = new TestLocal();
        AtomicInteger numInvocations = new AtomicInteger();
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> {
                    when(icObject.lookup("hello")).thenAnswer(iom -> {
                        numInvocations.incrementAndGet();
                        return result;
                    });
                    when(icObject.lookup("hello2")).thenAnswer(iom -> {
                        numInvocations.incrementAndGet();
                        return result;
                    });
                    when(icObject.lookup("exception")).thenThrow(NamingException.class);
                })) {
            assertEquals(result, locator.getObjectNoCache("hello"));
            assertTrue(locator.getJNDIObjectCache().isEmpty(), "cache should be empty");
            assertEquals(result, locator.getObject("hello"));
            assertEquals(result, locator.getObject("hello"));
            assertEquals(1, locator.getJNDIObjectCache().size());
            assertEquals(result, locator.getObject("hello2"));
            assertEquals(2, locator.getJNDIObjectCache().size());
            assertThrows(IllegalStateException.class, () -> locator.getObject("exception"));
            assertEquals(0, locator.getJNDIObjectCache().size());
            assertEquals(3, numInvocations.get());
        }
    }

    @Test
    void simpleFailure() throws NamingException {
        assertThrows(IllegalStateException.class, () -> JNDIObjectLocator.builder().build().getObject("invalid"));
    }

    @Test
    @Tag("StressTest")
    @SuppressWarnings("checkstyle:MagicNumber")
    void stressTest() throws InterruptedException, NamingException {
        int numThreads = 50 * Runtime.getRuntime().availableProcessors();
        int numIterations = 10000;
        AtomicBoolean failed = new AtomicBoolean();
        AtomicLong maxCached = new AtomicLong();
        AtomicInteger numInvocations = new AtomicInteger();
        TestLocal result = new TestLocal();
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> {
                    when(icObject.lookup("hello")).thenAnswer(iom -> {
                        numInvocations.incrementAndGet();
                        return result;
                    });
                    when(icObject.lookup("hello2")).thenAnswer(iom -> {
                        numInvocations.incrementAndGet();
                        return result;
                    });
                    when(icObject.lookup("exception")).thenThrow(NamingException.class);
                })) {
            assertEquals(result, locator.getObjectNoCache("hello"));
            assertTrue(locator.getJNDIObjectCache().isEmpty(), "cache should be empty");
        }
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        Set<Thread> runningThreads = Collections.newSetFromMap(new ConcurrentHashMap<Thread, Boolean>());

        IntStream.rangeClosed(0, numIterations).forEach(ii -> exec.submit(() -> {
            runningThreads.add(Thread.currentThread());
            try {
                assertEquals(result, locator.getObjectNoCache("hello"));
                maxCached.accumulateAndGet(locator.getJNDIObjectCache().keySet().stream().count(), Math::max);
                assertEquals(result, locator.getObject("hello"));
                assertEquals(result, locator.getObject("hello"));
                maxCached.accumulateAndGet(locator.getJNDIObjectCache().keySet().stream().count(), Math::max);
                assertEquals(result, locator.getObject("hello2"));
                maxCached.accumulateAndGet(locator.getJNDIObjectCache().keySet().stream().count(), Math::max);
                assertThrows(IllegalStateException.class, () -> locator.getObject("exception"));
                maxCached.accumulateAndGet(locator.getJNDIObjectCache().keySet().stream().count(), Math::max);
            } catch (Throwable thr) {
                failed.set(true);
                throw Lombok.sneakyThrow(thr);
            } finally {
                runningThreads.remove(Thread.currentThread());
            }
        }));
        exec.shutdown();
        boolean isShutdownCompleted = exec.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(isShutdownCompleted, runningThreads.isEmpty(), "completed but outstanding threads remain");
        if (!isShutdownCompleted) {
            System.out.printf("Unfinished threads: %d%n", runningThreads.size());
            for (Thread thr : runningThreads) {
                System.out.printf("%nStack Dump for Thread %d%n", thr.getId());
                Stream.of(thr.getStackTrace()).forEach(System.out::println);
            }
        }
        assertTrue(isShutdownCompleted, "timed out waiting for result");
        assertFalse(failed.get(), "somthing went wrong with stress test");
        assertEquals(2, maxCached.get());
        assertTrue(numInvocations.get() > numIterations * 1.5, "too few invocations");
    }
}
