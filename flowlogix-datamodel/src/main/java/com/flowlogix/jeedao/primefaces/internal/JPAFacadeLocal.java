/*
 * Copyright (C) 2001-2014, Bett-A-Way, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are prohibited unless authorized in writing by Bett-A-Way, Inc.
 */
package com.flowlogix.jeedao.primefaces.internal;

import com.flowlogix.jeedao.FacadeAPI;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel.EntityManagerGetter;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel.Filter;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel.Optimizer;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel.Sorter;
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
