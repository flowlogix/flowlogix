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

/**
 * Basic Facade API
 * 
 * @author lprimak
 * @param <TT> Entity Type
 * @param <KT> Primary Key Type
 */
public interface FacadeAPI<TT, KT>
{
    void create(TT entity);
    void edit(TT entity);
    void remove(TT entity);
    TT find(KT id);
    List<TT> findAll();
    List<TT> findRange(int[] range);
    int count();
    boolean isXA();
    void markForXA(boolean tf);
    public Class<TT> getEntityClass();
}
