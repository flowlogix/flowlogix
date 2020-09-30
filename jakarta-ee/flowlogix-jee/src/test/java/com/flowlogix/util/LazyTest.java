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
package com.flowlogix.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;


/**
 *
 * @author lprimak
 */
public class LazyTest {
    private final AtomicInteger numCreations = new AtomicInteger();

    @BeforeEach
    void before() {
        numCreations.set(0);
    }

    class Expensive {
        Expensive() {
            numCreations.incrementAndGet();
        }
    }

    @Test
    void lazy() {
        Expensive expensive = new Expensive();
        Lazy<Expensive> cheap = new Lazy<>(Expensive::new);
        assertEquals(1, numCreations.get());
        assertEquals(Expensive.class, cheap.get().getClass());
        assertEquals(2, numCreations.get());
        assertEquals(Expensive.class, cheap.get().getClass());
        assertEquals(2, numCreations.get());
    }

    @Test
    @Tag("StressTest")
    @Timeout(10)
    void threadedLazy() throws InterruptedException {
        int numInstances = 5000;
        List<Lazy<Expensive>> cheap = IntStream.rangeClosed(1, numInstances)
                .mapToObj(ii -> new Lazy<>(Expensive::new)).collect(Collectors.toList());
        assertEquals(0, numCreations.get());
        ExecutorService exec = Executors.newFixedThreadPool(500);
        cheap.forEach(ii -> IntStream.rangeClosed(1, 500).forEach(iter -> exec.submit(() -> ii.get())));
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(numInstances, numCreations.get());
    }
}
