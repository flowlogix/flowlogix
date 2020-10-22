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
package com.flowlogix.jeedao.primefaces.internal;

import java.io.Serializable;

/**
 *
 * @author lprimak
 */
public class MySer<KK, TT> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final ModelBuilder<KK, TT> builder;
    private final transient MyModel<KK, TT> model;

    public MySer(ModelBuilder<KK, TT> builder)
    {
        this.builder = builder;
        this.model = builder.build(MyModel.builder());
    }



    @FunctionalInterface
    public interface ModelBuilder<KK, TT> extends Serializable {
        MyModel<KK, TT> build(MyModel.MyModelBuilder<KK, TT> builder);
    }

    MySer<KK, TT> readResolve() {
        return new MySer<>(builder);
    }
}
