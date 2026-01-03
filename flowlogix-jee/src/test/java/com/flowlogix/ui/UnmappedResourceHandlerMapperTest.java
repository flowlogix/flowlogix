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

import jakarta.faces.application.ResourceHandler;
import jakarta.faces.webapp.FacesServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static com.flowlogix.ui.UnmappedResourceHandlerMapper.COM_FLOWLOGIX_ADD_UNMAPPED_RESOURCES;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnmappedResourceHandlerMapperTest {
    private final UnmappedResourceHandlerMapper mapper = new UnmappedResourceHandlerMapper();
    @Mock
    private ServletContext servletContext;
    @Mock
    private ServletContextEvent servletContextEvent;
    @Mock
    private ServletRegistration facesServletRegistration;
    @Mock
    private ServletRegistration anotherServletRegistration;

    @BeforeEach
    void initMocks() {
        when(servletContextEvent.getServletContext()).thenReturn(servletContext);
        when(servletContext.getInitParameter(COM_FLOWLOGIX_ADD_UNMAPPED_RESOURCES)).thenReturn("true");
        when(facesServletRegistration.getClassName()).thenReturn(FacesServlet.class.getName());
        when(servletContext.getServletRegistrations()).thenAnswer(invocation ->
                Map.of("another", anotherServletRegistration, "actualFaces", facesServletRegistration));
    }

    @Test
    void testContextInitialized() {
        mapper.contextInitialized(servletContextEvent);
        verify(facesServletRegistration, atMostOnce()).getClassName();
        verify(facesServletRegistration).addMapping(ResourceHandler.RESOURCE_IDENTIFIER + "/*");
        verify(anotherServletRegistration, atMostOnce()).getClassName();
        verifyNoMoreInteractions(facesServletRegistration, anotherServletRegistration);
    }
}
