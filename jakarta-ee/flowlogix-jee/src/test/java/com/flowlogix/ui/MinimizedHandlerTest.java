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
package com.flowlogix.ui;

import static com.flowlogix.ui.MinimizedHandler.parseExtensions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author lprimak
 */
public class MinimizedHandlerTest {
    @Test
    void js() {
        MinimizedHandler handler = new MinimizedHandler(null, "min", parseExtensions("css, js, ts"));
        assertEquals("my.min.js", handler.toMinimized("my.js"));
        assertEquals("my.js.xhtml", handler.toMinimized("my.js.xhtml"));
    }

    @Test
    void onlyOne() {
        MinimizedHandler handler = new MinimizedHandler(null, "min", parseExtensions(", js"));
        assertEquals("my.min.js", handler.toMinimized("my.js"));
        assertEquals("my.js.xhtml", handler.toMinimized("my.js.xhtml"));
    }

    @Test
    void css() {
        MinimizedHandler handler = new MinimizedHandler(null, "min", parseExtensions("css, js, ts"));
        assertEquals("my.min.css", handler.toMinimized("my.css"));
        assertEquals("my.css.xhtml", handler.toMinimized("my.css.xhtml"));
    }

    @Test
    void unhappyPath() {
        MinimizedHandler handler = new MinimizedHandler(null, "min", parseExtensions("css, js, ts"));
        assertEquals("", handler.toMinimized(""));
        assertEquals(" ", handler.toMinimized(" "));
        assertEquals("my.js.css2", handler.toMinimized("my.js.css2"));
    }

    @Test
    void none() {
        MinimizedHandler handler = new MinimizedHandler(null, "min", parseExtensions(""));
        assertEquals("my.js", handler.toMinimized("my.js"));
        assertEquals("my.js.xhtml", handler.toMinimized("my.js.xhtml"));
    }
}
