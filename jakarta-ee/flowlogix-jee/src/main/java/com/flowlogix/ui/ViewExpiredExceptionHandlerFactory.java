/*
 * Copyright 2014-2020 lprimak.
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
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.event.ExceptionQueuedEvent;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandlerFactory;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Faces;

/**
 * Redirects back to the same page in case of {@link ViewExpiredException}
 * <p>
 * Other handlers alone will display a message, which, from the user's perspective is useless.
 * This handler goes a step further, and will redirect to the same page, which will probably
 * either establish a new session, redirect to a login screen, or something similar,
 * which will remove one additional click for the users, and removes a cryptic message that's meaningless.
 * <p>
 * If used alone or with PrimeFaces, it will prevent exceptions from processing or logging,
 * as specified in web.xml via <br>
 * {@code org.omnifaces.EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING} property.
 * <p>
 * If used in conjunction with OmniFaces exception handler, it will delegate this filtering to OmniFaces.
 * See {@link FullAjaxExceptionHandlerFactory}
 * <p>
 * Works together with OmniFaces and PrimeFaces exceptions handlers, or separately on it's own
 * <p>
 * Example:
 * <pre>
 * faces-config.xml:
 * {@code
 *   <factory>
 *       <!-- the following line is optional, or could be PrimeFaces instead of OmniFaces exception handler
 *            The order is important, as ViewExpiredExceptionHandlerFactory needs to appear last
 *       -->
 *       <exception-handler-factory>org.omnifaces.exceptionhandler.FullAjaxExceptionHandlerFactory</exception-handler-factory>
 *       <exception-handler-factory>com.flowlogix.ui.ViewExpiredExceptionHandlerFactory</exception-handler-factory>
 *   </factory>
 * }
 *
 * web.xml:
 * {@code
 *   <error-page>
 *       <error-code>500</error-code>
 *       <location>/WEB-INF/errorpages/invalidErrorPage.xhtml</location>
 *   </error-page>
 *   <context-param>
 *       <param-name>org.omnifaces.EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING</param-name>
 *       <param-value>java.nio.channels.ClosedByInterruptException, java.nio.channels.llegalSelectorException</param-value>
 *   </context-param>
 * }
 *
 * </pre>
 * @author lprimak
 */
public class ViewExpiredExceptionHandlerFactory extends ExceptionHandlerFactory {
    private final Map<Class<?>, Function<ExceptionQueuedEvent, Boolean>> handlers;


    public ViewExpiredExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
        super(wrapped);
        Function<ExceptionQueuedEvent, Boolean> viewExpired = this::viewExpiredFn;
        Function<ExceptionQueuedEvent, Boolean> ignored = this::ignoredFn;

        Builder<Map.Entry<Class<?>, Function<ExceptionQueuedEvent, Boolean>>> handlerStreamBuilder = Stream.builder();
        Set<Class<?>> existingClasses = new HashSet<>();

        // trigger same-page redirect for ViewExpired exception
        addToStreamBuilderIfNew(handlerStreamBuilder, existingClasses, ViewExpiredException.class, viewExpired);

        // now add ignored logging instances
        if (!isIgnoreLoggingAlreadyHandled(wrapped, FullAjaxExceptionHandlerFactory.class)) {
            for (Class<? extends Throwable> cls : getTypesToIgnore()) {
                addToStreamBuilderIfNew(handlerStreamBuilder, existingClasses, cls, ignored);
            }
        }

        handlers = handlerStreamBuilder.build().
                collect(Collectors.collectingAndThen(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue),
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

    private static void addToStreamBuilderIfNew(
            Builder<Map.Entry<Class<?>, Function<ExceptionQueuedEvent, Boolean>>> handlerStreamBuilder,
            Set<Class<?>> existing, Class<?> cls, Function<ExceptionQueuedEvent, Boolean> fn) {
        if (!existing.contains(cls)) {
            existing.add(cls);
            handlerStreamBuilder.add(new AbstractMap.SimpleEntry<>(cls, fn));
        }
    }

    static boolean isIgnoreLoggingAlreadyHandled(ExceptionHandlerFactory wrapped, Class<?> target) {
        boolean matched = false;
        while (wrapped != null && !matched) {
            matched = target.isInstance(wrapped);
            wrapped = wrapped.getWrapped();
        }
        return matched;
    }

    static Class<? extends Throwable>[] getTypesToIgnore() {
        return FullAjaxExceptionHandler.getExceptionTypesToIgnoreInLogging(Faces.getServletContext());
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

                Optional<Function<ExceptionQueuedEvent, Boolean>> handler =
                        Stream.of(unwrappedException, pureRootCause).map(thr -> handlers.get(thr.getClass()))
                                .filter(Objects::nonNull).findFirst();
                if (handler.isPresent()) {
                    // an exception is matched, remove it from the queue and handle it next
                    it.remove();
                    if (!handler.get().apply(event)) {
                        return;
                    }
                }
            }
            getWrapped().handle();
        }
    }
}
