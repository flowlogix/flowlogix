/*
 * Copyright 2011 lprimak.
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
package com.flowlogix.web.services.annotations;

import java.lang.annotation.*;

/**
 * Marks an EJB that's injected into Tapestry as Stateful session bean<br>
 * 
 * <a href="http://code.google.com/p/flowlogix/wiki/TLEJBAnnotation"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Documented
@Target(ElementType.FIELD) 
@Retention(RetentionPolicy.RUNTIME) 
public @interface Stateful
{
    /**
     * In the future, we may want to put stateful beans as session attributes as opposed to App State Manager objects
     */
    boolean isSessionAttribute() default false;
    /**
     * Optionally override the session attribute key, only works if isSessionAttribute is true
     */
    String sessionKey() default "";
}
