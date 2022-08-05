/*
 * Copyright 2022 lprimak.
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
package com.flowlogix.shiro.ee.filters;

import static com.flowlogix.shiro.ee.filters.PassThruAuthenticationFilter.getReferer;
import javax.servlet.http.HttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author lprimak
 */
@ExtendWith(MockitoExtension.class)
public class PassThruAuthenticationFilterTest {
    @Mock
    private HttpServletRequest request;

    @Test
    void nullReferer() {
        when(request.getHeader("referer")).thenReturn(null);
        assertNull(getReferer(request));
    }

    @Test
    void plainStringReferer() {
        when(request.getHeader("referer")).thenReturn("hello");
        assertEquals("hello", getReferer(request));
    }

    @Test
    void switchToHttps() {
        when(request.getHeader("referer")).thenReturn("http://example.com");
        assertEquals("https://example.com", getReferer(request));
    }

    @Test
    void dontSwitchToHttpsWhenCustomPort() {
        when(request.getHeader("referer")).thenReturn("http://example.com:8080/");
        assertEquals("http://example.com:8080/", getReferer(request));
    }

    @Test
    void dontSwitchToHttpsWhenCustomPortNoTrailingSlash() {
        when(request.getHeader("referer")).thenReturn("http://example.com:8080");
        assertEquals("http://example.com:8080", getReferer(request));
    }
}
