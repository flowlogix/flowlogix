/*
 * Copyright (C) 2011-2026 Flow Logix, Inc. All Rights Reserved.
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

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import java.io.Serializable;

/**
 * Mock out ViewScoped via RequestScoped
 */
public class MockViewScopeExtension implements Extension, Serializable {
    private static final MockViewScope SCOPE = new MockViewScope();

    void addScope(@Observes final BeforeBeanDiscovery event) {
        event.addScope(SCOPE.getScope(), true, true);
    }

    void registerContext(@Observes final AfterBeanDiscovery event) {
        event.addContext(SCOPE);
    }
}
