/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.viewscoped;

import com.flowlogix.demo.viewscoped.ViewScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.CDI;
import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * Mock out ViewScoped via RequestScoped
 */
public class MockViewScope implements Context, Serializable {
    @Override
    public Class<? extends Annotation> getScope() {
        return ViewScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        Context ctx = CDI.current().getBeanManager().getContext(RequestScoped.class);
        return ctx.get(contextual, creationalContext);
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        Context ctx = CDI.current().getBeanManager().getContext(RequestScoped.class);
        return ctx.get(contextual);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
