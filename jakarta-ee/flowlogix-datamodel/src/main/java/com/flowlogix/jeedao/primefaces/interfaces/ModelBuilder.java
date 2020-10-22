/*
 * Copyright 2020 lprimak.
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

import com.flowlogix.jeedao.primefaces.internal.JPAModelImpl;
import com.flowlogix.jeedao.primefaces.internal.JPAModelImpl.JPAModelImplBuilder;
import java.io.Serializable;

/**
 * this interfaces only exists because it needs to be forced serializable,
 * the only serializable interface here because build() has to be
 * portable over-the-wire
 * 
 * @author lprimak
 */
@FunctionalInterface
public interface ModelBuilder<TT, KK> extends Serializable {
    JPAModelImpl<TT, KK> build(JPAModelImplBuilder<TT, KK, ?, ?> builder);
}
