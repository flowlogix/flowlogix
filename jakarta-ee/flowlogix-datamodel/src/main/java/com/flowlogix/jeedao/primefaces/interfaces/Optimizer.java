/*
 * Copyright 2015 lprimak.
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
package com.flowlogix.jeedao.primefaces.interfaces;

import java.io.Serializable;
import javax.persistence.TypedQuery;

/**
 * Hook to add hints to the JPA query
 *
 * @param <TT> Entity Type
 */
@FunctionalInterface
public interface Optimizer<TT> extends Serializable 
{
    /**
     * Add hints to the JPA query
     * Mostly used for batch fetch
     *
     * @param query to add hints to
     * @return the same query
     */
    TypedQuery<TT> addHints(TypedQuery<TT> query);   
}
