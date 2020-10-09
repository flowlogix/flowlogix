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
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.event.ExceptionQueuedEvent;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Faces;

/**
 * Redirects back to the same page in case of {@link ViewExpiredException}
 * <p>
 * Other libraries will display a message, which, from the user's perspective is useless.
 * This handler goes a step further, and will redirect to the same page, which will probably
 * either establish a new session, redirect to a login screen, or something similar,
 * which will remove one additional click for the users, and removes a cryptic message that's meaningless.
 * <p>
 * In addition, ignores {@link ClosedByInterruptException} exception, which sometimes happen
 * when browsers or other clients disconnect unexpectedly
 *
 * @author lprimak
 */
public class ViewExpiredExceptionHandlerFactory extends ExceptionHandlerFactory {
    private final Map<Class<?>, Function<ExceptionQueuedEvent, Boolean>> handlers;


    public ViewExpiredExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
        super(wrapped);
        Function<ExceptionQueuedEvent, Boolean> viewExpired = this::viewExpiredFn;
        Function<ExceptionQueuedEvent, Boolean> ignored = this::ignoredFn;

        handlers = Stream.of(
                // trigger same-page redirect for ViewExpired exception
                new AbstractMap.SimpleEntry<>(ViewExpiredException.class, viewExpired),
                // ignore unexpected client disconnects, nothing to do here
                new AbstractMap.SimpleEntry<>(ClosedByInterruptException.class, ignored))
                .collect(Collectors.collectingAndThen(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue),
                        Collections::<Class<?>, Function<ExceptionQueuedEvent, Boolean>>unmodifiableMap));
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new Handler(getWrapped().getExceptionHandler(), handlers);
    }

    private Boolean viewExpiredFn(ExceptionQueuedEvent evt) {
        Faces.setFlashAttribute(SESSION_EXPIRED_KEY, Boolean.TRUE);
        Faces.redirect(Faces.getRequestURIWithQueryString());
        return false;
    }

    private Boolean ignoredFn(ExceptionQueuedEvent evt) {
        return true;
    }

    private static class Handler extends ExceptionHandlerWrapper {
        private final Map<Class<?>, Function<ExceptionQueuedEvent, Boolean>> handlers;


        public Handler(ExceptionHandler wrapped, @NonNull Map<Class<?>, Function<ExceptionQueuedEvent, Boolean>> handlers) {
            super(wrapped);
            this.handlers = handlers;
        }

        /**
         * Either triggers actions, or ignores certain exceptions
         * if action returns false the pipeline processing is stopped at that point,
         * such as for {@link ViewExpiredException}
         * This is useful to hide spurious client-related exceptions from logs, which are not controllable on the server.
         *
         * @throws FacesException
         */
        @Override
        public void handle() throws FacesException {
            Iterator<ExceptionQueuedEvent> it = getUnhandledExceptionQueuedEvents().iterator();
            while (it.hasNext()) {
                ExceptionQueuedEvent event = it.next();
                Throwable queuedException = event.getContext().getException();
                Throwable unwrappedException = Exceptions.unwrap(queuedException);
                Throwable pureRootCause = ExceptionUtils.getRootCause(queuedException);

                List<Function<ExceptionQueuedEvent, Boolean>> handlerList =
                        Stream.of(unwrappedException, pureRootCause).map(thr -> handlers.get(thr.getClass()))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                if (!handlerList.isEmpty()) {
                    // an exception is matched, remove it from the queue and handle it next
                    it.remove();
                }
                for (Function<ExceptionQueuedEvent, Boolean> handler : handlerList) {
                    if (!handler.apply(event))
                    {
                        return;
                    }
                }
            }
            getWrapped().handle();
        }
    }
}
