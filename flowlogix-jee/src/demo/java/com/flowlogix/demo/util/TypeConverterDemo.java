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
package com.flowlogix.demo.util;

import com.flowlogix.util.TypeConverter;
import com.flowlogix.util.TypeConverter.CheckedValue;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("HideUtilityClassConstructor")
@Slf4j
public class TypeConverterDemo {
    public static <TT> TT fromString(String input, Class<TT> cls) {
        // @start region="typeConvert"
        // tag::typeConvert[] // @replace regex='.*\n' replacement=""
        TT convertedValue = TypeConverter.valueOf(input, cls);
        // end::typeConvert[] // @replace regex='.*\n' replacement=""
        // @end
        return convertedValue;
    }

    public static <TT> CheckedValue<TT> checkFromString(String input, Class<TT> cls) {
        // @start region="checkTypeConvert"
        // tag::checkTypeConvert[] // @replace regex='.*\n' replacement=""
        CheckedValue<TT> checkedValue = TypeConverter.checkAndConvert(input, cls);
        if (checkedValue.isValid()) {
            // get and operate on a value in a type-safe way
            TT value = checkedValue.getValue();
        }
        // end::checkTypeConvert[] // @replace regex='.*\n' replacement=""
        // @end
        return checkedValue;
    }

    public static int convertOne() {
        // @start region="typeConverterOne"
        int one = TypeConverter.valueOf("1", int.class);
        // @end
        return one;
    }
}
