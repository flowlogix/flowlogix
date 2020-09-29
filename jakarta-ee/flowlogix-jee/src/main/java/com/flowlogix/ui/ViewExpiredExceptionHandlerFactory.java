/*
 * Copyright 2014 lprimak.
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
import java.util.Iterator;
import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.event.ExceptionQueuedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Faces;

/**
 * establishes a session for ajax exceptions
 *
 * @author lprimak
 */
@RequiredArgsConstructor
public class ViewExpiredExceptionHandlerFactory  extends ExceptionHandlerFactory
{
    @Override
    public ExceptionHandler getExceptionHandler()
    {
        return new Handler(wrapped.getExceptionHandler());
    }


    @RequiredArgsConstructor
    private static class Handler extends ExceptionHandlerWrapper
    {
        @Override
        public void handle() throws FacesException
        {
            Iterator<ExceptionQueuedEvent> it = getUnhandledExceptionQueuedEvents().iterator();
            while(it.hasNext())
            {
                ExceptionQueuedEvent evt = it.next();
                Throwable ex = Exceptions.unwrap(evt.getContext().getException());

                Throwable pureRootCause = ExceptionUtils.getRootCause(evt.getContext().getException());
                if(pureRootCause == null)
                {
                    pureRootCause = ex;
                }

                if (ex instanceof ViewExpiredException)
                {
                    it.remove();
                    Faces.setFlashAttribute(SESSION_EXPIRED_KEY, Boolean.TRUE);
                    Faces.redirect(Faces.getRequestURIWithQueryString());
                    return;
                }
                else if(pureRootCause instanceof ClosedByInterruptException)
                {
                    // ignore browser exists
                    it.remove();
                }
            }
            getWrapped().handle();
        }


        private @Getter final ExceptionHandler wrapped;
    }


    private @Getter final ExceptionHandlerFactory wrapped;
}
