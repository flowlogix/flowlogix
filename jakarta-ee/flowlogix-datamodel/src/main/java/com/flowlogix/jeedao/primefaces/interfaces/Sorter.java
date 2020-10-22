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

import com.flowlogix.jeedao.primefaces.support.SortData;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;

/**
 * Sorter Hook
 * @param <TT> Entity Type
 */
@FunctionalInterface
public interface Sorter<TT>
{
    /**
     * Hook for sort criteria application
     * can remove elements from the SortMeta lists and do your own action
     * any elements left in SortMeta will be done via the default mechanism
     *
     * @param sortData
     * @param cb
     * @param root
     */
    void sort(SortData sortData, CriteriaBuilder cb, Root<TT> root);
}
