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
package com.flowlogix.jeedao.primefaces.interfaces;

import java.io.Serializable;

/**
 * Convert Key String to Key Type
 *
 * @param <KK> Key Type
 */
@FunctionalInterface
public interface KeyConverter<KK> extends Serializable 
{
    /**
     * return the key in the appropriate type
     *
     * @param keyStr
     * @return key type
     */
    KK convert(String keyStr);
}
