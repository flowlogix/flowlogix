/*
 * Copyright (C) 2011-2022 Flow Logix, Inc. All Rights Reserved.
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

import org.omnifaces.util.Lazy;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates Lazy initialization utility, with double-checked locking
 *
 * @author lprimak
 */
public class LazyExample {
    final AtomicInteger numInitialized = new AtomicInteger();

    class Expensive {
        Expensive() {
            numInitialized.incrementAndGet();
        }
    };

    // without Lazy
    private final Expensive expensiveInitialization = new Expensive();

    // with Lazy
    private final Lazy<Expensive> cheapInitialization = new Lazy<>(Expensive::new);

    public void usingLazy() {
        cheapInitialization.get();
    }
}
