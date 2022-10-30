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

import com.flowlogix.examples.jndi.ejbs.NumberGetter;
import org.omnifaces.util.JNDIObjectLocator;

/**
 * Examples of JNDI object locator, ServiceLocator pattern
 * Locator is thread-safe, caching and serializable
 *
 * @author lprimak
 */
public class JndiExample {
    /**
     * Create an object locator via builder interface
     */
    private final JNDIObjectLocator locator = JNDIObjectLocator.builder().build();

    /**
     * @return new thread-safe locator
     */
    JNDIObjectLocator getLocator() {
        return locator;
    }

    /**
     *
     * @return new thread-safe locator with environment
     */
    JNDIObjectLocator createLocatorWithEnvironment() {
        return JNDIObjectLocator.builder().environment("oneKey", "oneValue")
                .environment("twoKey", "twoValue")
                .build();
    }

    /**
     * @return new thread-safe locator with no caching
     */
    JNDIObjectLocator createLocatorWithNoCaching() {
        return JNDIObjectLocator.builder().noCaching().build();
    }

    /**
     * get a number via locator
     * @return number
     */
    int getNumber() {
        return getLocator().getObject(NumberGetter.class).getNumber();
    }
}
