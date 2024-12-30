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
package org.omnifaces.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 *
 * @author lprimak
 */
class LazyTest {
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
    @Tag("StressTest")
    @Timeout(10)
    void threadedLazy() throws InterruptedException {
        final int numInstances = 5000;
        List<Lazy<Expensive>> cheap = IntStream.rangeClosed(1, numInstances)
                .mapToObj(ii -> new Lazy<>(Expensive::new)).toList();
        assertThat(numCreations.get()).isZero();
        final int numThreads = 50 * Runtime.getRuntime().availableProcessors();
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        cheap.forEach(ii -> IntStream.rangeClosed(1, numThreads)
                .forEach(iter -> exec.submit(() -> ii.get())));
        exec.shutdown();
        await(exec);
        assertThat(numCreations.get()).isEqualTo(numInstances);
    }

    @SuppressWarnings("MagicNumber")
    private void await(ExecutorService exec) throws InterruptedException {
        exec.awaitTermination(10, TimeUnit.SECONDS);
    }
}
