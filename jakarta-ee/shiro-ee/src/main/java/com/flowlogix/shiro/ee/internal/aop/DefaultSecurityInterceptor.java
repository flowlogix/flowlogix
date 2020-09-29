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
package com.flowlogix.shiro.ee.internal.aop;

import java.lang.annotation.Annotation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.aop.AuthorizingAnnotationHandler;

/**
 * Enhanced from Tynamo Security
 */
@RequiredArgsConstructor
class DefaultSecurityInterceptor implements SecurityInterceptor 
{
    private final AuthorizingAnnotationHandler handler;
    private @Getter final Annotation annotation;

    /**
     * Initialize {@link #handler} field use annotation.
     *
     * @param annotation annotation for create handler and use during
     * {@link #intercept()} invocation.
     */
    public DefaultSecurityInterceptor(Annotation annotation) {

        this.annotation = annotation;
        AuthorizingAnnotationHandler _handler = AopHelper.createHandler(annotation);
        if (_handler == null) {
            throw new IllegalStateException("No handler for " + annotation + "annotation");
        }
        this.handler = _handler;
    }

    /* (non-Javadoc)
     * @see org.tynamo.shiro.extension.authz.aop.SecurityInterceptor#intercept()
     */
    @Override
    public void intercept() {
        handler.assertAuthorized(getAnnotation());
    }
}
