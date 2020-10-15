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

import com.flowlogix.jeedao.TypedNativeQuery;
import java.util.List;
import javax.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 *
 * @author lprimak
 * @param <T> Type of Query
 */
@RequiredArgsConstructor
public class TypedNativeQueryImpl<T> implements TypedNativeQuery<T>
{
    @Override
    @SuppressWarnings("unchecked")
    public T getSingleResult()
    {
        return (T)q.getSingleResult();
    }


    @Override
    @SuppressWarnings("unchecked")
    public List<T> getResultList()
    {
        return q.getResultList();
    }


    private final @Delegate Query q;
}
