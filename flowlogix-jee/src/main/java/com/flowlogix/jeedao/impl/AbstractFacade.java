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
package com.flowlogix.jeedao.impl;

import com.flowlogix.jeedao.QueryCriteria;
import com.flowlogix.jeedao.TypedNativeQuery;
import java.util.List;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.TransactionSynchronizationRegistry;
import lombok.RequiredArgsConstructor;


/**
 * Base class for DAOs
 * 
 * @author Petro
 * @param <T> Entity Type
 * @param <I> Primary Key Type
 */
public @RequiredArgsConstructor abstract class AbstractFacade<T, I>
{
    public AbstractFacade()
    {
        entityClass = null;
    }

    public void create(T entity)
    {
        getEntityManager().persist(entity);
    }

    public void edit(T entity)
    {
        getEntityManager().merge(entity);
    }

    public void remove(T entity)
    {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    public T find(I id)
    {
        return getEntityManager().find(getEntityClass(), id);
    }

    public List<T> findAll()
    {
        CriteriaQuery<T> cq = getEntityManager().getCriteriaBuilder().createQuery(getEntityClass());
        Root<T> root = cq.from(getEntityClass());
        cq.select(root);
        addToCriteria(getEntityManager().getCriteriaBuilder(), root, cq);
        TypedQuery<T> tq = getEntityManager().createQuery(cq);
        addHints(tq, false);
        return tq.getResultList();
    }

    public List<T> findRange(int[] range)
    {
        CriteriaQuery<T> cq = getEntityManager().getCriteriaBuilder().createQuery(getEntityClass());
        Root<T> root = cq.from(getEntityClass());
        cq.select(root);
        addToCriteria(getEntityManager().getCriteriaBuilder(), root, cq);
        TypedQuery<T> q = getEntityManager().createQuery(cq);
        q.setMaxResults(range[1] - range[0]);
        q.setFirstResult(range[0]);
        addHints(q, true);
        return q.getResultList();
    }

    public int count()
    {
        CriteriaQuery<Long> cq = getEntityManager().getCriteriaBuilder().createQuery(Long.class);
        Root<T> rt = cq.from(getEntityClass());
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        TypedQuery<Long> q = getEntityManager().createQuery(cq);
        return q.getSingleResult().intValue();
    }  
    
    
    protected QueryCriteria<T> buildQueryCriteria()
    {
        return buildQueryCriteria(getEntityClass());
    }

    
    protected<RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls)
    {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<RR> cq = cb.createQuery(cls);
        return new QueryCriteria<>(cb, cq.from(cls), cq);
    }
    
    
    protected<R> TypedNativeQuery<R> createNativeQuery(String sql, Class<R> resultClass)
    {
        Query q = getEntityManager().createNativeQuery(sql, resultClass);
        return new TypedNativeQueryImpl<>(q);
    }

    
    protected<R> TypedNativeQuery<R> createNativeQuery(String sql, String resultMapping)
    {
        Query q = getEntityManager().createNativeQuery(sql, resultMapping);
        return new TypedNativeQueryImpl<>(q);
    }

    
    /**
     * 
     * @return true of XA is enabled for this transaction
     */
    public boolean isXA()
    {
        checkTSR();
        return Boolean.TRUE.equals(tsr.getResource(AbstractFacade.XA_EM_KEY));
    }
    
    
    /**
     * mark transaction for XA
     * @param tf True/False
     */
    public void markForXA(boolean tf)
    {
        checkTSR();
        tsr.putResource(AbstractFacade.XA_EM_KEY, tf && xaEnabled);        // mark transaction as XA
    }
    
    
    /**
     * Override the default entity class getter
     * @return Entity class
     */
    public Class<T> getEntityClass()
    {
        if(entityClass == null)
        {
            throw new IllegalStateException("Entity Class not overridden and is not set");
        }
        return entityClass;
    }
    
    
    /**
     * Return Entity Manager
     * @return Entity Manager
     */
    protected abstract EntityManager getEntityManager();
    /**
     * Add additional criteria
     * @param cb
     * @param root
     * @param cq 
     */
    protected void addToCriteria(CriteriaBuilder cb, Root<T> root, CriteriaQuery<T> cq) { /* override */ };
    /**
     * add hints to query
     * @param tq
     * @param isRange 
     */
    protected void addHints(TypedQuery<T> tq, boolean isRange) {}
    
    
    private void checkTSR()
    {
        if(tsr == null)
        {
            throw new IllegalStateException("TransactionSynchronizationRegistry not available - Container-managed object?");
        }
    }
    
    
    public void checkForRequiredXA()
    {
        if(xaEnabled && !isXA())
        {
            throw new IllegalStateException("XA Transaction Required but not enabled - forgot to call markForXA(true)?");
        }
    }
            
    
    private final Class<T> entityClass;   
    private @Resource TransactionSynchronizationRegistry tsr;

    private static final boolean xaEnabled = Boolean.getBoolean(AbstractFacade.XA_ENABLED_PROP);
    private static final String XA_EM_KEY = "com.flowlogix.XA_EM_USED";
    public static final String XA_ENABLED_PROP = "com.flowlogix.XA_ENABLED";
}
