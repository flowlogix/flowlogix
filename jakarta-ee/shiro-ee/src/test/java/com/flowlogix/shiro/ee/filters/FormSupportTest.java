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

import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.extractJSFNewViewState;
import static com.flowlogix.shiro.ee.filters.FormResubmitSupport.isJSFStatefulFormForm;
import static com.flowlogix.shiro.ee.filters.Forms.getReferer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import javax.servlet.http.HttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author lprimak
 */
@ExtendWith(MockitoExtension.class)
public class FormSupportTest {
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

    @Test
    void viewStatePattern() {
        String statefulFormData
                = "j_idt5%3Dj_idt5%26j_idt5%3Aj_idt7%3Daaa%26j_idt5%3Aj_idt9%3Dbbb%26j_idt5%3A"
                + "j_idt11%3DSubmit+...%26javax.faces.ViewState%3D-8335355445345003673%3A-6008443334776649058";
        assertTrue(isJSFStatefulFormForm(decode(statefulFormData)));
        String statelessFormData
                = "j_idt5%3Dj_idt5%26j_idt5%3Aj_idt7%3Daaa%26j_idt5%3Aj_idt9%3Dbbb%26j_idt5%3A"
                + "j_idt11%3DSubmit+...%26javax.faces.ViewState%3Dstateless";
        assertFalse(isJSFStatefulFormForm(statelessFormData));
        assertThrows(NullPointerException.class, () -> isJSFStatefulFormForm(null));
        String nonJSFFormData
                = "j_idt5%3Dj_idt5%26j_idt5%3Aj_idt7%3Daaa%26j_idt5%3Aj_idt9%3Dbbb%26j_idt5%3A"
                + "j_idt11%3DSubmit+...";
        assertFalse(isJSFStatefulFormForm("xxx"));
        assertFalse(isJSFStatefulFormForm(nonJSFFormData));
    }

    @Test
    void extractViewState() {
        assertThrows(NullPointerException.class, () -> extractJSFNewViewState(null, null));
        assertEquals("hello", extractJSFNewViewState("", "hello"));
        assertEquals("javax.faces.ViewState=stateless&hello=bye",
                extractJSFNewViewState("xxx", "javax.faces.ViewState=stateless&hello=bye"));
        assertEquals("javax.faces.ViewState=stateless&hello=bye",
                extractJSFNewViewState("<input name=\"javax.faces.ViewState\" value=\"123:456\"/>",
                        "javax.faces.ViewState=stateless&hello=bye"));
        assertEquals("aaa=bbb&javax.faces.ViewState=xxx:yyy&hello=bye",
                extractJSFNewViewState("<input name=\"javax.faces.ViewState\" value=\"123:456\"/>",
                        "aaa=bbb&javax.faces.ViewState=xxx:yyy&hello=bye"));
        assertEquals("javax.faces.ViewState=123:456&hello=bye",
                extractJSFNewViewState("<input name=\"javax.faces.ViewState\" value=\"123:456\"/>",
                        "javax.faces.ViewState=987:654&hello=bye"));
        assertEquals("javax.faces.ViewState=-123:-456&hello=bye",
                extractJSFNewViewState("<input name=\"javax.faces.ViewState\" value=\"-123:-456\"/>",
                        "javax.faces.ViewState=987:654&hello=bye"));
        assertEquals("javax.faces.ViewState=-123:-456&hello=bye",
                extractJSFNewViewState("<input name=\"javax.faces.ViewState\" value=\"-123:-456\"/>",
                        "javax.faces.ViewState=-987:-654&hello=bye"));
        assertEquals("aaa=bbb&javax.faces.ViewState=-123:-456&hello=bye",
                extractJSFNewViewState("<input name=\"javax.faces.ViewState\" value=\"-123:-456\"/>",
                        "aaa=bbb&javax.faces.ViewState=-987:-654&hello=bye"));
        assertEquals("aaa=bbb&javax.faces.ViewState=-123:-456",
                extractJSFNewViewState("<input name=\"javax.faces.ViewState\" value=\"-123:-456\"/>",
                        "aaa=bbb&javax.faces.ViewState=-987:-654"));
    }

    private static String decode(String plain) {
        return URLDecoder.decode(plain, StandardCharsets.UTF_8);
    }
}
