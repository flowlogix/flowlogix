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
package com.flowlogix.jeedao.primefaces.internal;

import com.flowlogix.jeedao.FacadeAPI;
import com.flowlogix.jeedao.primefaces.interfaces.EntityManagerGetter;
import com.flowlogix.jeedao.primefaces.interfaces.Filter;
import com.flowlogix.jeedao.primefaces.interfaces.Optimizer;
import com.flowlogix.jeedao.primefaces.interfaces.Sorter;
import com.google.common.base.Optional;
import java.util.List;
import java.util.Map;
import javax.ejb.Local;
import org.primefaces.model.SortMeta;

/**
 * JPA DAO facade that supports PrimeFaces lazy table model
 * 
 * @author lprimak
 * @param <TT>
 * @param <KK>
 */
@Local
public interface JPAFacadeLocal<TT, KK> extends FacadeAPI<TT, KK>
{
    /**
     * Set up each call to JPA
     * 
     * @param emg
     * @param entityClass
     * @param optimizer
     * @param filter
     * @param sorter 
     */
    void setup(EntityManagerGetter emg, Class<TT> entityClass, Optional<Optimizer<TT>> optimizer,
            Optional<Filter<TT>> filter, Optional<Sorter<TT>> sorter);
    /**
     * row count
     * 
     * @param filters
     * @return count
     */
    int count(Map<String, Object> filters);
    
    /**
     * return paginated entity list to be used in the lazy table model
     * 
     * @param first
     * @param pageSize
     * @param filters
     * @param sortMeta
     * @return entity list
     */
    List<TT> findRows(int first, int pageSize, Map<String, Object> filters, List<SortMeta> sortMeta);
}
