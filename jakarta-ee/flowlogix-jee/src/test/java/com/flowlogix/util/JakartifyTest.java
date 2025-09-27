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
package com.flowlogix.util;

import org.junit.jupiter.api.Test;
import static com.flowlogix.demo.ui.JakartifyDemo.jakartifyError;
import static com.flowlogix.demo.ui.JakartifyDemo.jakartifyServlet;
import static com.flowlogix.util.JakartaTransformerUtils.isJakarta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class JakartifyTest {
    @Test
    void checkJakarta() {
        assertThat(isJakarta()).isTrue();
    }

    @Test
    void servlet() {
        assertThat(jakartifyServlet()).isEqualTo(isJakarta() ? "jakarta.servlet.Servlet" : "javax.servlet.Servlet");
    }

    @Test
    void javaxServlet() {
        try (var mocked = mockStatic(JakartaTransformerUtils.class)) {
            mocked.when(JakartaTransformerUtils::isJakarta).thenReturn(false);
            mocked.when(() -> JakartaTransformerUtils.jakartify(any())).thenCallRealMethod();
            assertThat(jakartifyServlet()).isEqualTo(isJakarta() ? "jakarta.servlet.Servlet" : "javax.servlet.Servlet");
        }
    }

    @Test
    void error() {
        assertThat(jakartifyError()).isEqualTo(isJakarta() ? "jakarta.faces.FacesException: message X"
                : "javax.faces.FacesException: message X");
    }
}
