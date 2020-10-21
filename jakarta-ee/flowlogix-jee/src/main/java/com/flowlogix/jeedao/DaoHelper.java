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
package com.flowlogix.jeedao;

import java.util.List;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;


/**
 * @param <TT> Entity Type
 * @param <KT> Primary Key Type
 *
 * @author lprimak
 */
@RequiredArgsConstructor
public class DaoHelper<TT, KT>
{
    private final @NonNull Supplier<EntityManager> entityManagerSupplier;
    private final @NonNull @Getter Class<TT> entityClass;


    public List<TT> findAll()
    {
        CriteriaQuery<TT> cq = getEntityManager().getCriteriaBuilder().createQuery(entityClass);
        Root<TT> root = cq.from(entityClass);
        cq.select(root);
        addToCriteria(getEntityManager().getCriteriaBuilder(), root, cq);
        TypedQuery<TT> tq = getEntityManager().createQuery(cq);
        addHints(tq, false);
        return tq.getResultList();
    }

    public List<TT> findRange(int min, int max)
    {
        CriteriaQuery<TT> cq = getEntityManager().getCriteriaBuilder().createQuery(entityClass);
        Root<TT> root = cq.from(entityClass);
        cq.select(root);
        addToCriteria(getEntityManager().getCriteriaBuilder(), root, cq);
        TypedQuery<TT> q = getEntityManager().createQuery(cq);
        q.setMaxResults(max - min);
        q.setFirstResult(min);
        addHints(q, true);
        return q.getResultList();
    }

    public int count()
    {
        CriteriaQuery<Long> cq = getEntityManager().getCriteriaBuilder().createQuery(Long.class);
        Root<TT> rt = cq.from(entityClass);
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        addToCountCriteria(getEntityManager().getCriteriaBuilder(), rt, cq);
        TypedQuery<Long> q = getEntityManager().createQuery(cq);
        return q.getSingleResult().intValue();
    }

    public EntityManager getEntityManager() {
        return entityManagerSupplier.get();
    }


    protected QueryCriteria<TT> buildQueryCriteria()
    {
        return buildQueryCriteria(entityClass);
    }


    protected<RR> QueryCriteria<RR> buildQueryCriteria(Class<RR> cls)
    {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<RR> cq = cb.createQuery(cls);
        return new QueryCriteria<>(cb, cq.from(cls), cq);
    }


    protected TypedNativeQuery createNativeQuery(String sql, Class<?> resultClass)
    {
        Query q = getEntityManager().createNativeQuery(sql, resultClass);
        return new TypedNativeQuery(q);
    }


    protected TypedNativeQuery createNativeQuery(String sql, String resultMapping)
    {
        Query q = getEntityManager().createNativeQuery(sql, resultMapping);
        return new TypedNativeQuery(q);
    }

    /**
     * Add additional criteria
     * @param cb
     * @param root
     * @param cq
     */
    protected void addToCriteria(CriteriaBuilder cb, Root<TT> root, CriteriaQuery<TT> cq) { /* override */ };
    /**
     * Add additional criteria for count() operation
     * @param cb
     * @param root
     * @param cq
     */
    protected void addToCountCriteria(CriteriaBuilder cb, Root<TT> root, CriteriaQuery<Long> cq) { /* override */ };
    /**
     * add hints to query
     * @param tq
     * @param isRange
     */
    protected void addHints(TypedQuery<TT> tq, boolean isRange) {}
}
