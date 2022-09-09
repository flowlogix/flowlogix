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

import com.flowlogix.jeedao.primefaces.support.FilterData;
import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;

/**
 * Filter Hook
 *
 * @param <TT> Entity Type
 */
@FunctionalInterface
public interface Filter<TT> {
    /**
     * hook to supply custom filter
     *
     * @param filters user input
     * @param cb
     * @param root
     */
    void filter(Map<String, FilterData> filters, CriteriaBuilder cb, Root<TT> root);
}
