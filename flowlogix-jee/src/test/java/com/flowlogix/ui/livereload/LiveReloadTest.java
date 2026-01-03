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
package com.flowlogix.ui.livereload;

import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnifaces.util.Faces;
import static com.flowlogix.ui.livereload.AutoReloadPhaseListener.MyResponseWriter.HTTPS_SCHEME;
import static com.flowlogix.ui.livereload.AutoReloadPhaseListener.MyResponseWriter.X_FORWARDED_PROTO;
import static com.flowlogix.ui.livereload.AutoReloadPhaseListener.MyResponseWriter.toHttpsURL;
import static com.flowlogix.ui.livereload.AutoReloadPhaseListener.getResponseCharacterEncoding;
import static com.flowlogix.ui.livereload.AutoReloadPhaseListener.getResponseContentType;
import static com.flowlogix.ui.livereload.Configurator.DISABLE_CACHE_PARAM;
import static com.flowlogix.ui.livereload.Configurator.FACELETS_REFRESH_PERIOD_PARAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveReloadTest {
    @Mock
    ServletContext servletContext;
    @Mock
    ServletContextEvent servletContextEvent;
    @Mock(answer = RETURNS_DEEP_STUBS)
    FacesContext facesContext;
    @Mock
    ResponseWriter responseWriter;
    @Mock(answer = RETURNS_DEEP_STUBS)
    HttpServletRequest httpServletRequest;

    @Test
    void requestContextPathNoBeginningSlash() throws Exception {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::getRequestContextPath).thenReturn("noslash");
            facesMock.when(Faces::getRequest).thenReturn(httpServletRequest);

            new AutoReloadPhaseListener.MyResponseWriter(responseWriter, facesContext)
                    .endElement("body");
            facesMock.verify(Faces::getRequestContextPath, times(2));
            verify(facesContext).getResponseWriter();
            verify(facesContext.getResponseWriter()).write(anyString());
            verify(responseWriter).endElement("body");
            verifyNoMoreInteractions(responseWriter, facesContext);
        }
    }

    @Test
    void convertToHttpsWhenNotNeeded() {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::getRequest).thenReturn(httpServletRequest);
            when(httpServletRequest.getScheme()).thenReturn(HTTPS_SCHEME);
            assertThat(toHttpsURL("http://example.com/path")).isEqualTo("http://example.com/path");
        }
    }

    @Test
    void convertToHttpsWhenAlready() {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::getRequest).thenReturn(httpServletRequest);
            when(httpServletRequest.getScheme()).thenReturn(HTTPS_SCHEME);
            assertThat(toHttpsURL("https://example.com/path")).isEqualTo("https://example.com/path");
        }
    }

    @Test
    void convertToHttps() {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::getRequest).thenReturn(httpServletRequest);
            when(httpServletRequest.getHeader(X_FORWARDED_PROTO)).thenReturn(HTTPS_SCHEME);
            assertThat(toHttpsURL("http://example.com/path")).isEqualTo("https://example.com/path");
        }
    }

    @Test
    void responseContentType() {
        assertThat(getResponseContentType(facesContext)).isEqualTo("text/html");
    }

    @Test
    void responseNonHtmlContentType() {
        when(facesContext.getExternalContext().getRequestContentType()).thenReturn("application/xml");
        assertThat(getResponseContentType(facesContext)).isEqualTo("application/xml");
    }

    @Test
    void responseEncoding() {
        assertThat(getResponseCharacterEncoding(facesContext)).isEqualTo("UTF-8");
    }

    @Test
    void responseNonStandardEncoding() {
        when(facesContext.getExternalContext().getRequestCharacterEncoding()).thenReturn("UTF-22");
        assertThat(getResponseCharacterEncoding(facesContext)).isEqualTo("UTF-22");
    }

    @Nested
    class ConfiguratorTest {
        @Test
        void setsFaceletsRefreshPeriodWhenDevelopmentAndDisableCache() {
            when(servletContext.getInitParameter(DISABLE_CACHE_PARAM)).thenReturn(null);
            when(servletContextEvent.getServletContext()).thenReturn(servletContext);

            try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
                facesMock.when(Faces::hasContext).thenReturn(true);
                facesMock.when(Faces::isDevelopment).thenReturn(true);

                new Configurator().contextInitialized(servletContextEvent);

                verify(servletContext).setInitParameter(FACELETS_REFRESH_PERIOD_PARAM, "0");
            }
        }

        @Test
        void doesNotSetFaceletsRefreshPeriodWhenNotDevelopment() {
            when(servletContext.getInitParameter(DISABLE_CACHE_PARAM)).thenReturn("true");
            when(servletContextEvent.getServletContext()).thenReturn(servletContext);

            try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
                facesMock.when(Faces::hasContext).thenReturn(true);
                facesMock.when(Faces::isDevelopment).thenReturn(false);

                new Configurator().contextInitialized(servletContextEvent);

                verify(servletContext, never()).setInitParameter(eq(FACELETS_REFRESH_PERIOD_PARAM), anyString());
            }
        }

        @Test
        void doesNotSetFaceletsRefreshPeriodWhenNoContext() {
            when(servletContext.getInitParameter(DISABLE_CACHE_PARAM)).thenReturn("true");
            when(servletContextEvent.getServletContext()).thenReturn(servletContext);

            try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
                facesMock.when(Faces::hasContext).thenReturn(false);
                facesMock.when(Faces::isDevelopment).thenReturn(false);

                new Configurator().contextInitialized(servletContextEvent);

                verify(servletContext, never()).setInitParameter(eq(FACELETS_REFRESH_PERIOD_PARAM), anyString());
            }
        }

        @Test
        void doesNotSetFaceletsRefreshPeriodWhenDisableCacheFalse() {
            when(servletContext.getInitParameter(DISABLE_CACHE_PARAM)).thenReturn("false");
            when(servletContextEvent.getServletContext()).thenReturn(servletContext);

            try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
                facesMock.when(Faces::hasContext).thenReturn(true);
                facesMock.when(Faces::isDevelopment).thenReturn(true);

                new Configurator().contextInitialized(servletContextEvent);

                verify(servletContext, never()).setInitParameter(eq(FACELETS_REFRESH_PERIOD_PARAM), anyString());
            }
        }
    }
}
