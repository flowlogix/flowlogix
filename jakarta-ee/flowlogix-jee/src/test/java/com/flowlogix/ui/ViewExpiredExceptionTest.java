/*
 * Copyright 2020 lprimak.
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

import static com.flowlogix.ui.AttributeKeys.SESSION_EXPIRED_KEY;
import java.nio.channels.ClosedByInterruptException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedStatic;
import org.mockito.MockedStatic.Verification;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.omnifaces.util.Faces;

/**
 *
 * @author lprimak
 */
public class ViewExpiredExceptionTest {
    ExceptionHandlerFactory mockedFactory;
    ExceptionHandler mockedHandler;
    FacesContext facesContext;
    ViewExpiredExceptionHandlerFactory fact;

    @BeforeEach
    void init() {
        mockedFactory = mock(ExceptionHandlerFactory.class);
        mockedHandler = mock(ExceptionHandler.class);
        facesContext = mock(FacesContext.class);
        when(mockedFactory.getExceptionHandler()).thenReturn(mockedHandler);
        fact = new ViewExpiredExceptionHandlerFactory(mockedFactory);
    }

    @Test
    void wrapper() {
        List<ExceptionQueuedEvent> exceptions = Stream.of(
                build(new SQLException("sql")),
                buildWithoutWrapping(new SQLException("sql")),
                build(new ClosedByInterruptException()),
                buildWithoutWrapping(new ClosedByInterruptException()),
                build(new ViewExpiredException("expired", "myView")))
                .collect(Collectors.toList());
        when(mockedHandler.getUnhandledExceptionQueuedEvents()).thenReturn(exceptions);
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            Verification flashVerification = () -> Faces.setFlashAttribute(any(String.class), any(Boolean.class));
            facesMock.when(flashVerification).then((inv) -> {
                assertEquals(SESSION_EXPIRED_KEY, inv.getArgument(0));
                assertTrue(inv.getArgument(1, Boolean.class));
                return null;
            });
            fact.getExceptionHandler().handle();
            facesMock.verify(flashVerification);
        }
        assertEquals(2, exceptions.size());
    }

    @Test
    void returnFromRedirect() {
        List<ExceptionQueuedEvent> exceptions = Stream.of(
                build(new ViewExpiredException("expired", "myView")),
                build(new SQLException("sql")),
                build(new ViewExpiredException("expired", "myView")),
                buildWithoutWrapping(new SQLException("sql")),
                build(new ClosedByInterruptException()),
                buildWithoutWrapping(new ClosedByInterruptException()))
                .collect(Collectors.toList());
        when(mockedHandler.getUnhandledExceptionQueuedEvents()).thenReturn(exceptions);
        try (MockedStatic<Faces> facesMock = mockStatic(Faces.class)) {
            fact.getExceptionHandler().handle();
        }
        assertEquals(5, exceptions.size());
    }

    private ExceptionQueuedEvent build(Exception exc) {
        return new ExceptionQueuedEvent(new ExceptionQueuedEventContext(facesContext, new FacesException("facesExc", exc)));
    }

    private ExceptionQueuedEvent buildWithoutWrapping(Exception exc) {
        return new ExceptionQueuedEvent(new ExceptionQueuedEventContext(facesContext, exc));
    }
}
