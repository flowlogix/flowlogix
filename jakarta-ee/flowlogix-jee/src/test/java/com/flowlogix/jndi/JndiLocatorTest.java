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
package com.flowlogix.jndi;

import com.flowlogix.jndi.JNDIObjectLocator.JNDIObjectLocatorBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import javax.ejb.Local;
import javax.ejb.Remote;
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
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.MockedConstruction;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

/**
 *
 * @author lprimak
 */
public class JndiLocatorTest
{
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
        assertEquals(JNDIObjectLocator.PORTABLE_NAME_PREFIX + "/hello", locator.prependPortableName("hello"));
        assertEquals("java:hello", locator.prependPortableName("java:hello"));
    }

    @Test
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
            JNDIObjectLocator.initialHost(builder, "myHost");
            JNDIObjectLocator.initialPort(builder, 12345);
            JNDIObjectLocator locator = builder.build();
            assertEquals("hello", locator.getObject(String.class));
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
            String myString = JNDIObjectLocator.builder().portableNamePrefix("abc")
                    .cacheRemote(true).build().getObject(String.class);
            assertEquals("hello", myString);
        }
    }

    @Local
    private class TestLocal { }

    @Remote
    private class TestRemote { }

    @Test
    void dontCacheRemote() throws NamingException {
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> when(icObject.lookup(anyString())).thenReturn(new TestRemote()))) {
            assertEquals(TestRemote.class, locator.getObject(TestRemote.class).getClass());
            assertTrue(locator.getJndiObjectCache().isEmpty(), "object should not be cached");
            locator.getObject(TestLocal.class);
            assertFalse(locator.getJndiObjectCache().isEmpty(), "object should be cached");
        }
    }

    @Test
    void guessByType() {
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> {
                    // the '$' denotes inner class
                    // i.e. java:module/JndiLocatorTest$Test!com.flowlogix.jndi.JndiLocatorTest$TestLocal
                    when(icObject.lookup(eq(String.format("java:module/%s$Test!%s$TestLocal",
                            getClass().getSimpleName(), getClass().getName())))).thenReturn(new TestLocal());
                    when(icObject.lookup(eq(String.format("java:module/%s$Test!%s$TestRemote",
                            getClass().getSimpleName(), getClass().getName())))).thenReturn(new TestRemote());
                })) {
            assertEquals(TestLocal.class, locator.getObject(TestLocal.class).getClass());
            assertEquals(TestRemote.class, locator.getObject(TestRemote.class).getClass());
            assertNull(locator.getObject(this.getClass()));
        }
    }

    @Test
    void serialization() throws IOException, NamingException, ClassNotFoundException {
        JNDIObjectLocator original = JNDIObjectLocator.builder().environment("one", "two")
                .cacheRemote(true)
                .build();
        ByteArrayOutputStream bostrm = new ByteArrayOutputStream();
        ObjectOutputStream ostrm = new ObjectOutputStream(bostrm);

        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> when(icObject.lookup(anyString())).thenReturn("hello"))) {
            assertEquals("hello", original.getObject("hello"));
            assertEquals(1, original.getJndiObjectCache().size());
        }
        ostrm.writeObject(original);
        ostrm.close();
        JNDIObjectLocator xferred = (JNDIObjectLocator)new ObjectInputStream(
                new ByteArrayInputStream(bostrm.toByteArray())).readObject();
        assertEquals(0, xferred.getJndiObjectCache().size());
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> when(icObject.lookup(anyString())).thenReturn("hello"))) {
            assertEquals("hello", original.getObject("hello"));
            assertEquals(1, original.getJndiObjectCache().size());
        }
    }

    @Test
    void caching() throws NamingException {
        TestLocal result = new TestLocal();
        AtomicInteger numInvocations = new AtomicInteger();
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> {
                    when(icObject.lookup(eq("hello"))).thenAnswer(iom -> {
                        numInvocations.incrementAndGet();
                        return result;
                    });
                    when(icObject.lookup(eq("hello2"))).thenAnswer(iom -> {
                        numInvocations.incrementAndGet();
                        return result;
                    });
                    when(icObject.lookup(eq("exception"))).thenThrow(NamingException.class);
                })) {
            assertEquals(result, locator.getObject("hello", true));
            assertTrue(locator.getJndiObjectCache().isEmpty(), "cache should be empty");
            assertEquals(result, locator.getObject("hello"));
            assertEquals(result, locator.getObject("hello"));
            assertEquals(1, locator.getJndiObjectCache().size());
            assertEquals(result, locator.getObject("hello2"));
            assertEquals(2, locator.getJndiObjectCache().size());
            assertThrows(NamingException.class, () -> locator.getObject("exception"));
            assertEquals(0, locator.getJndiObjectCache().size());
            assertEquals(3, numInvocations.get());
        }
    }

    @Test
    void simpleFailure() throws NamingException {
        assertThrows(NamingException.class, () -> JNDIObjectLocator.builder().build().getObject("invalid"));
    }

    @Test
    @Tag("StressTest")
    void stressTest() throws InterruptedException, NamingException {
        int numThreads = 500;
        int numIterations = 10000;
        AtomicBoolean failed = new AtomicBoolean();
        AtomicLong maxCached = new AtomicLong();
        AtomicInteger numInvocations = new AtomicInteger();
        TestLocal result = new TestLocal();
        try (MockedConstruction<InitialContext> mocked = mockConstruction(InitialContext.class,
                (icObject, context) -> {
                    when(icObject.lookup(eq("hello"))).thenAnswer(iom -> {
                        numInvocations.incrementAndGet();
                        return result;
                    });
                    when(icObject.lookup(eq("hello2"))).thenAnswer(iom -> {
                        numInvocations.incrementAndGet();
                        return result;
                    });
                    when(icObject.lookup(eq("exception"))).thenThrow(NamingException.class);
                })) {
            assertEquals(result, locator.getObject("hello", true));
            assertTrue(locator.getJndiObjectCache().isEmpty(), "cache should be empty");
        }
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        IntStream.rangeClosed(0, numIterations).forEach(ii -> exec.submit(() -> {
            try {
                assertEquals(result, locator.getObject("hello", true));
                maxCached.accumulateAndGet(locator.getJndiObjectCache().keySet().stream().count(), Math::max);
                assertEquals(result, locator.getObject("hello"));
                assertEquals(result, locator.getObject("hello"));
                maxCached.accumulateAndGet(locator.getJndiObjectCache().keySet().stream().count(), Math::max);
                assertEquals(result, locator.getObject("hello2"));
                maxCached.accumulateAndGet(locator.getJndiObjectCache().keySet().stream().count(), Math::max);
                assertThrows(NamingException.class, () -> locator.getObject("exception"));
                maxCached.accumulateAndGet(locator.getJndiObjectCache().keySet().stream().count(), Math::max);
            } catch (NamingException ex) {
                failed.set(true);
                throw Lombok.sneakyThrow(ex);
            } catch (Throwable thr) {
                failed.set(true);
                throw Lombok.sneakyThrow(thr);
            }
        }));
        exec.shutdown();
        assertTrue(exec.awaitTermination(10, TimeUnit.SECONDS), "timed out waiting for result");
        assertFalse(failed.get(), "somthing went wrong with stress test");
        assertEquals(2, maxCached.get());
        assertTrue(numInvocations.get() > numIterations * 1.5, "too few invocations");
    }
}
