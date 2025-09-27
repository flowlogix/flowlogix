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
package com.flowlogix.ui.livereload;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.websocket.Session;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnifaces.util.Faces;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static com.flowlogix.ui.livereload.Configurator.DISABLE_CACHE_PARAM;
import static com.flowlogix.ui.livereload.Configurator.FACELETS_REFRESH_PERIOD_PARAM;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LiveReloadTest {
    @Mock
    ServletContext servletContext;
    @Mock
    ServletContextEvent servletContextEvent;
    @Mock
    Set<Session> mockSessions;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Session session;
    @Mock
    AtomicInteger maxSessions;
    @Mock
    AtomicBoolean needsAnotherReload;

    @Test
    void broadcastReloadFailsWhenNotAllowed() throws IOException {
        try (MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class)) {
            reloadMock.when(ReloadEndpoint::maxSessions).thenReturn(maxSessions);
            reloadMock.when(ReloadEndpoint::broadcastReload).thenCallRealMethod();

            assertThat(ReloadEndpoint.broadcastReload()).isFalse();
            verify(maxSessions).get();
            verifyNoMoreInteractions(maxSessions, mockSessions, needsAnotherReload);
        }
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void broadcastReloadDoesNotFailWhenNoSessions() throws IOException {
        try (MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class)) {
            reloadMock.when(ReloadEndpoint::sessions).thenReturn(Set.of());
            reloadMock.when(ReloadEndpoint::maxSessions).thenReturn(maxSessions);
            reloadMock.when(ReloadEndpoint::needsAnotherReload).thenReturn(needsAnotherReload);
            reloadMock.when(ReloadEndpoint::broadcastReload).thenCallRealMethod();
            when(maxSessions.get()).thenReturn(5);

            assertThat(ReloadEndpoint.broadcastReload()).isTrue();
            verify(needsAnotherReload).set(true);
            verifyNoMoreInteractions(maxSessions, mockSessions, needsAnotherReload);
        }
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void broadcastReloadDoesNotFailWhenOneSession() throws IOException {
        try (MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class)) {
            reloadMock.when(ReloadEndpoint::sessions).thenReturn(Set.of(session));
            reloadMock.when(ReloadEndpoint::maxSessions).thenReturn(maxSessions);
            reloadMock.when(ReloadEndpoint::needsAnotherReload).thenReturn(needsAnotherReload);
            reloadMock.when(ReloadEndpoint::broadcastReload).thenCallRealMethod();
            when(maxSessions.get()).thenReturn(5);

            assertThat(ReloadEndpoint.broadcastReload()).isTrue();
            verify(session).getId();
            verify(session.getBasicRemote()).sendText("reload");
            verify(session, times(2)).getBasicRemote();
            verifyNoMoreInteractions(maxSessions, mockSessions, needsAnotherReload, session);
        }
    }

    @Test
    void openSocketWhenNeedsAnotherReload() throws IOException {
        try (MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class)) {
            reloadMock.when(ReloadEndpoint::needsAnotherReload).thenReturn(needsAnotherReload);
            reloadMock.when(ReloadEndpoint::sessions).thenReturn(mockSessions);
            when(needsAnotherReload.getAndSet(false)).thenReturn(true);

            new ReloadEndpoint().onOpen(session);

            verify(mockSessions).add(session);
            verify(needsAnotherReload).getAndSet(false);
            verify(session).getBasicRemote();
            verify(session).getId();
            verify(session.getBasicRemote()).sendText("reload");
            verifyNoMoreInteractions(needsAnotherReload, session, mockSessions);
        }
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

            try (MockedStatic<Faces> facesMock = mockStatic(Faces.class);
                 MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class)) {
                facesMock.when(Faces::hasContext).thenReturn(true);
                facesMock.when(Faces::isDevelopment).thenReturn(false);
                reloadMock.when(ReloadEndpoint::maxSessions).thenReturn(maxSessions);

                new Configurator().contextInitialized(servletContextEvent);

                verify(servletContext, never()).setInitParameter(eq(FACELETS_REFRESH_PERIOD_PARAM), anyString());
                verify(maxSessions).set(0);
            }
        }

        @Test
        void doesNotSetFaceletsRefreshPeriodWhenNoContext() {
            when(servletContext.getInitParameter(DISABLE_CACHE_PARAM)).thenReturn("true");
            when(servletContextEvent.getServletContext()).thenReturn(servletContext);

            try (MockedStatic<Faces> facesMock = mockStatic(Faces.class);
                 MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class)) {
                facesMock.when(Faces::hasContext).thenReturn(false);
                facesMock.when(Faces::isDevelopment).thenReturn(false);
                reloadMock.when(ReloadEndpoint::maxSessions).thenReturn(maxSessions);

                new Configurator().contextInitialized(servletContextEvent);

                verify(servletContext, never()).setInitParameter(eq(FACELETS_REFRESH_PERIOD_PARAM), anyString());
                verify(maxSessions).set(0);
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

    @Nested
    class ReloadTriggerTest {
        @Mock
        Response response;
        @Mock
        ResponseBuilder responseBuilder;

        @Test
        void reloadReturnsOkWhenBroadcastSucceeds() throws Exception {
            try (MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class);
                 MockedStatic<Response> responseMock = mockStatic(Response.class)) {
                reloadMock.when(ReloadEndpoint::broadcastReload).thenReturn(true);
                responseMock.when(Response::ok).thenReturn(responseBuilder);
                when(responseBuilder.build()).thenReturn(response);
                when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());

                ReloadTrigger trigger = new ReloadTrigger();
                Response actualResponse = trigger.reload();

                assertThat(actualResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            }
        }

        @Test
        void reloadReturnsServiceUnavailableWhenBroadcastFails() throws Exception {
            try (MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class);
                 MockedStatic<Response> responseMock = mockStatic(Response.class)) {
                reloadMock.when(ReloadEndpoint::broadcastReload).thenReturn(false);
                responseMock.when(() -> Response.status(Response.Status.SERVICE_UNAVAILABLE)).thenReturn(responseBuilder);
                when(responseBuilder.entity("Live Reloading Disabled")).thenReturn(responseBuilder);
                when(responseBuilder.build()).thenReturn(response);
                when(response.getStatus()).thenReturn(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                when(response.getEntity()).thenReturn("Live Reloading Disabled");

                ReloadTrigger trigger = new ReloadTrigger();
                Response actualResponse = trigger.reload();

                assertThat(actualResponse.getStatus()).isEqualTo(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                assertThat(actualResponse.getEntity()).isEqualTo("Live Reloading Disabled");
            }
        }
    }

    @Nested
    @SuppressWarnings("checkstyle:MagicNumber")
    class ReloadEndpointConfiguratorTest {
        @AllArgsConstructor
        static class REP extends ReloadEndpointConfigurator {
            private final boolean returnValue;

            @Override
            boolean callSuperCheckOrigin(String originHeaderValue) {
                return returnValue;
            }
        }

        @Test
        void checkOriginReturnsSuperWhenSessionsBelowMax() {
            try (MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class)) {
                reloadMock.when(ReloadEndpoint::sessions).thenReturn(mockSessions);
                reloadMock.when(ReloadEndpoint::maxSessions).thenReturn(maxSessions);
                when(mockSessions.size()).thenReturn(1);
                when(maxSessions.get()).thenReturn(5);

                boolean result = new REP(true).checkOrigin("origin");
                assertThat(result).isTrue();

                assertThatThrownBy(() -> {
                    new ReloadEndpointConfigurator().checkOrigin("none");
                }).isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Cannot load platform configurator");
            }
        }

        @Test
        void checkOriginReturnsFalseWhenSessionsAtMax() {
            try (MockedStatic<ReloadEndpoint> reloadMock = mockStatic(ReloadEndpoint.class)) {
                reloadMock.when(ReloadEndpoint::sessions).thenReturn(mockSessions);
                reloadMock.when(ReloadEndpoint::maxSessions).thenReturn(maxSessions);
                when(mockSessions.size()).thenReturn(5);
                when(maxSessions.get()).thenReturn(5);

                boolean result = new REP(true).checkOrigin("origin");
                assertThat(result).isFalse();
            }
        }
    }
}
