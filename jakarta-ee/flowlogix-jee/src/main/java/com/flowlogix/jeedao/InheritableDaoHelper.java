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
package com.flowlogix.jeedao;

import lombok.experimental.Delegate;

/**
 * Data Access Object pattern implementation that is meant to be inherited by the user's classes.
 * This is an alternative to {@link DaoHelper}, which does not have requirements
 * for inheritance. {@link DaoHelper} is the preferred method of implementing DAOs.
 *
 * @see DaoHelper
 * @param <TT> Entity Type
 * @param <KT> Primary Key Type
 */
public class InheritableDaoHelper<TT, KT> {
    @Delegate
    protected DaoHelper<TT, KT> daoHelper;
}
