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

import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnifaces.util.Faces;
import java.io.IOException;
import static com.flowlogix.ui.livereload.AutoReloadViewHandler.MyResponseWriter.HTTPS_SCHEME;
import static com.flowlogix.ui.livereload.AutoReloadViewHandler.MyResponseWriter.X_FORWARDED_PROTO;
import static com.flowlogix.ui.livereload.AutoReloadViewHandler.MyResponseWriter.toHttpsURL;
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
    @Mock
    ViewHandler viewHandler;
    @Mock
    UIViewRoot viewRoot;

    @Test
    void requestContextPathNoBeginningSlash() throws Exception {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::getRequestContextPath).thenReturn("noslash");
            facesMock.when(Faces::getRequest).thenReturn(httpServletRequest);
            facesMock.when(() -> Faces.getRealPath("/")).thenReturn(null);

            new AutoReloadViewHandler.MyResponseWriter(responseWriter, facesContext)
                    .endElement("body");
            facesMock.verify(Faces::getRequestContextPath, times(2));
            verify(facesContext).getResponseWriter();
            verify(facesContext.getResponseWriter()).write(anyString());
            verify(responseWriter).endElement("body");
            verifyNoMoreInteractions(responseWriter, facesContext);
        }
    }

    @Test
    void sendDeploymentKey() throws Exception {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::getRequestContextPath).thenReturn("/context");
            facesMock.when(Faces::getRequest).thenReturn(httpServletRequest);
            facesMock.when(() -> Faces.getRealPath("/")).thenReturn("/opt/payara/deployments/finalName/");

            new AutoReloadViewHandler.MyResponseWriter(responseWriter, facesContext)
                    .endElement("body");

            ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
            verify(facesContext.getResponseWriter()).write(scriptCaptor.capture());
            assertThat(scriptCaptor.getValue())
                    .contains("ws.send('finalName');");
        }
    }

    @Test
    void injectsLiveReloadErrorFallback() throws Exception {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::getRequestContextPath).thenReturn("/context");
            facesMock.when(Faces::getRequest).thenReturn(httpServletRequest);
            facesMock.when(() -> Faces.getRealPath("/")).thenReturn("/opt/payara/deployments/finalName/");

            new AutoReloadViewHandler.MyResponseWriter(responseWriter, facesContext)
                    .endElement("body");

            ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
            verify(facesContext.getResponseWriter()).write(scriptCaptor.capture());
            assertThat(scriptCaptor.getValue())
                    .contains("console.error(message);")
                    .contains("window.flowlogixLiveReloadAlertShown")
                    .contains("alert(")
                    .contains("FlowLogix error banner helper script is not loaded.");
        }
    }

    @Test
    void fallbackDeploymentKeyUsesContextPathWhenRealPathIsInvalid() throws Exception {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::getRequestContextPath).thenReturn("/context");
            facesMock.when(Faces::getRequest).thenReturn(httpServletRequest);
            facesMock.when(() -> Faces.getRealPath("/")).thenReturn("\u0000bad");

            new AutoReloadViewHandler.MyResponseWriter(responseWriter, facesContext)
                    .endElement("body");

            ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
            verify(facesContext.getResponseWriter()).write(scriptCaptor.capture());
            assertThat(scriptCaptor.getValue()).contains("ws.send('context');");
            facesMock.verify(Faces::getRequestContextPath, times(2));
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

    /**
     * The response content type belongs to the view handler, which negotiates it and applies
     * {@code <f:view contentType>}. Live reload only decorates the writer it produces.
     */
    @Test
    void responseContentTypeIsLeftToTheViewHandler() throws IOException {
        renderView();

        verify(facesContext.getExternalContext(), never()).setResponseContentType(anyString());
        verify(facesContext.getExternalContext(), never()).setResponseCharacterEncoding(anyString());
    }

    /**
     * The writer the view handler creates is decorated rather than replaced, so that the content type,
     * encoding and buffer size it applies while creating that writer all survive.
     */
    @Test
    void viewHandlerResponseWriterIsWrapped() throws IOException {
        renderView().setResponseWriter(responseWriter);

        ArgumentCaptor<ResponseWriter> wrapped = ArgumentCaptor.forClass(ResponseWriter.class);
        verify(facesContext).setResponseWriter(wrapped.capture());
        assertThat(wrapped.getValue()).isInstanceOf(AutoReloadViewHandler.MyResponseWriter.class);
    }

    /**
     * Renderers swap the response writer while rendering and then put the original back, handing an
     * already decorated writer back to the context. Decorating it a second time injects the script
     * twice and opens two WebSocket connections per page.
     */
    @Test
    void alreadyDecoratedResponseWriterIsNotDecoratedAgain() throws IOException {
        FacesContext rendered = renderView();
        rendered.setResponseWriter(responseWriter);

        ArgumentCaptor<ResponseWriter> decorated = ArgumentCaptor.forClass(ResponseWriter.class);
        verify(facesContext).setResponseWriter(decorated.capture());

        // what MenuRenderer does after rendering the options of a select menu
        rendered.setResponseWriter(decorated.getValue());

        ArgumentCaptor<ResponseWriter> bothCalls = ArgumentCaptor.forClass(ResponseWriter.class);
        verify(facesContext, times(2)).setResponseWriter(bothCalls.capture());
        assertThat(bothCalls.getAllValues().get(1)).isSameAs(bothCalls.getAllValues().get(0));
    }

    /**
     * Reloading resubmits the POST that produced the page, against view state the redeployment has
     * already discarded, so the client navigates to the same URL instead.
     */
    @Test
    void navigatesRatherThanReloading() throws Exception {
        assertThat(injectedScript())
                .contains("location.replace(location.href)")
                .doesNotContain("location.reload()");
    }

    private FacesContext renderView() throws IOException {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::isDevelopment).thenReturn(true);
            facesMock.when(Faces::isAjaxRequest).thenReturn(false);
            new AutoReloadViewHandler(viewHandler).renderView(facesContext, viewRoot);
        }

        ArgumentCaptor<FacesContext> rendered = ArgumentCaptor.forClass(FacesContext.class);
        verify(viewHandler).renderView(rendered.capture(), eq(viewRoot));
        return rendered.getValue();
    }

    private String injectedScript() throws IOException {
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            facesMock.when(Faces::getRequestContextPath).thenReturn("/context");
            facesMock.when(Faces::getRequest).thenReturn(httpServletRequest);
            facesMock.when(() -> Faces.getRealPath("/")).thenReturn("/opt/payara/deployments/finalName/");

            new AutoReloadViewHandler.MyResponseWriter(responseWriter, facesContext).endElement("body");
        }

        ArgumentCaptor<String> script = ArgumentCaptor.forClass(String.class);
        verify(facesContext.getResponseWriter()).write(script.capture());
        return script.getValue();
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
