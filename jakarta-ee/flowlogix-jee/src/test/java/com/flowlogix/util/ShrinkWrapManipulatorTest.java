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
package com.flowlogix.util;

import org.junit.jupiter.api.Test;
import java.net.MalformedURLException;
import java.net.URI;
import static com.flowlogix.util.ShrinkWrapManipulator.DEFAULT_SSL_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ShrinkWrapManipulatorTest {
    @Test
    void httpsUrl() throws MalformedURLException {
        var httpsUrl = ShrinkWrapManipulator.toHttpsURL(URI.create("http://localhost:1234").toURL());
        assertEquals(URI.create(String.format("https://localhost:%s", getPortFromProperty())).toURL(), httpsUrl);
    }

    @Test
    void alreadyHttpsUrl() throws MalformedURLException {
        var url = URI.create("https://localhost:1234").toURL();
        var httpsUrl = ShrinkWrapManipulator.toHttpsURL(url);
        assertSame(url, httpsUrl);
    }

    @Test
    void withoutPort() throws MalformedURLException {
        var httpsUrl = ShrinkWrapManipulator.toHttpsURL(URI.create("http://localhost").toURL());
        assertEquals(URI.create(String.format("https://localhost:%s", getPortFromProperty())).toURL(), httpsUrl);
    }

    @Test
    void alreadyHttpsWithoutPort() throws MalformedURLException {
        var url = URI.create("https://localhost").toURL();
        var httpsUrl = ShrinkWrapManipulator.toHttpsURL(url);
        assertSame(url, httpsUrl);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static int getPortFromProperty() {
        return Integer.parseInt(System.getProperty(DEFAULT_SSL_PROPERTY, String.valueOf(8181)));
    }
}
