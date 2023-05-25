/*
 * Copyright (C) 2011-2023 Flow Logix, Inc. All Rights Reserved.
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
import static com.flowlogix.demo.ui.JakartifyDemo.jakartifyError;
import static com.flowlogix.demo.ui.JakartifyDemo.jakartifyServlet;
import static com.flowlogix.util.JakartaTransformerUtils.isJakarta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JakartifyTest {
    @Test
    void checkJakarta() {
        assertTrue(isJakarta());
    }

    @Test
    void servlet() {
        assertEquals(isJakarta() ? "jakarta.servlet.Servlet" : "javax.servlet.Servlet", jakartifyServlet());
    }

    @Test
    void error() {
        assertEquals(isJakarta() ? "jakarta.faces.FacesException: message X" : "javax.faces.FacesException: message X",
                jakartifyError());
    }
}
