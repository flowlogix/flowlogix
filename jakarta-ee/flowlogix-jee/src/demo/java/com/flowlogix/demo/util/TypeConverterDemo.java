/*
 * Copyright (C) 2011-2023 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.demo.util;

import com.flowlogix.util.TypeConverter;

@SuppressWarnings("HideUtilityClassConstructor")
public class TypeConverterDemo {
    // @start region="typeConvert"
    // tag::typeConvert[] // @replace regex='.*\n' replacement=""
    public static <TT> TT fromString(String input, Class<TT> cls) {
        return TypeConverter.valueOf(input, cls);
    }
    // end::typeConvert[] // @replace regex='.*\n' replacement=""
    // @end

    // @start region="checkTypeConvert"
    // tag::checkTypeConvert[] // @replace regex='.*\n' replacement=""
    public static <TT> TypeConverter.CheckedValue<TT> checkFromString(String input, Class<TT> cls) {
        return TypeConverter.checkAndConvert(input, cls);
    }
    // end::checkTypeConvert[] // @replace regex='.*\n' replacement=""
    // @end

    public static int convertOne() {
        // @start region="typeConverterOne"
        int one = TypeConverter.valueOf("1", int.class);
        // @end
        return one;
    }
}
