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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnifaces.util.Faces;
import static com.flowlogix.ui.livereload.Configurator.DISABLE_CACHE_PARAM;
import static com.flowlogix.ui.livereload.Configurator.FACELETS_REFRESH_PERIOD_PARAM;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveReloadTest {
    @Mock
    ServletContext servletContext;
    @Mock
    ServletContextEvent servletContextEvent;

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
