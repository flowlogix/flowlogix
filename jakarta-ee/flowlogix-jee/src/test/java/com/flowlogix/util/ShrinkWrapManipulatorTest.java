/*
 * Copyright (C) 2011-2024 Flow Logix, Inc. All Rights Reserved.
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
import java.net.URL;
import static com.flowlogix.util.ShrinkWrapManipulator.DEFAULT_SSL_PORT;
import static com.flowlogix.util.ShrinkWrapManipulator.DEFAULT_SSL_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShrinkWrapManipulatorTest {
    @Test
    void httpsUrl() throws MalformedURLException {
        String port = System.getProperty(DEFAULT_SSL_PROPERTY, String.valueOf(DEFAULT_SSL_PORT));
        var httpsUrl = ShrinkWrapManipulator.toHttpsURL(new URL("http://localhost:1234"));
        assertEquals(new URL(String.format("https://localhost:%s", port)), httpsUrl);
    }
}
