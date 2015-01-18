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
package com.flowlogix.jeedao.primefaces.internal;

import java.io.Serializable;
import javax.transaction.TransactionScoped;
import lombok.Getter;

/**
 * Transaction-scoped state for DataTable parameters
 * 
 * @author lprimak
 */
@TransactionScoped
public class JPAFacadeState implements Serializable
{    
    private final @Getter JPAFacadeTypedState<?> typedState = new JPAFacadeTypedState<>();
    private static final long serialVersionUID = 1L;
}
