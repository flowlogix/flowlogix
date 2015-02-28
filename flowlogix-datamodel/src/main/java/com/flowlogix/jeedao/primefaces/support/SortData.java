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
package com.flowlogix.jeedao.primefaces.support;

import java.util.Iterator;
import java.util.List;
import javax.persistence.criteria.Order;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.primefaces.model.SortMeta;

/**
 * return type for the sorter hook
 * 
 * @author lprimak
 */
@RequiredArgsConstructor @Getter
public class SortData
{
    /**
     * Sort based on fields
     */
    private final Iterator<SortMeta> sortMeta;
    /**
     * global sort order added by the client
     */
    private final List<Order> sortOrder;
    /**
     * which goes first - sort by the client (default) or field-based sort
     */
    private final boolean appendSortOrder;
}
