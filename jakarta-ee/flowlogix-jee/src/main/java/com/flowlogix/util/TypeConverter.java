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
package com.flowlogix.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Lombok;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Converts from any String to any type dynamically
 * Example:
 *
 * {@code
 * int one = TypeConverter.valueOf("1", int.class);
 * }
 *
 * @author lprimak
 */
@Slf4j
public class TypeConverter {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final Map<Class<?>, MethodHandle> valueOfMethod = new ConcurrentHashMap<>();

    /**
     *
     * @param <TT> type of result objects
     * @param strValue input
     * @param type type of result
     * @return value
     */
    @SuppressWarnings("unchecked")
    public static <TT> TT valueOf(@NonNull String strValue, @NonNull Class<TT> type) {
        if (type.equals(String.class)) {
            return (TT) strValue;
        }
        String method = "valueOf";
        Class<?> targetType = null;
        if (type.equals(Double.class) || type.equals(double.class)) {
            strValue = processNumbers(strValue);
            targetType = Double.class;
        } else if (type.equals(Float.class) || type.equals(float.class)) {
            strValue = processNumbers(strValue);
            targetType = Float.class;
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            strValue = processNumbers(strValue);
            targetType = Integer.class;
        } else if (type.equals(Long.class) || type.equals(long.class)) {
            strValue = processNumbers(strValue);
            targetType = Long.class;
        }
        return (TT) callMethod(strValue, method, targetType != null ? targetType : type);
    }

    /**
     * Convert string to object given a type name
     *
     * @param strValue
     * @param type
     * @return object after conversion
     * @throws IllegalArgumentException
     */
    public static Object valueOf(@NonNull String strValue, @NonNull String type) throws IllegalArgumentException {
        String dataTypeString = type;
        if (type.equals(String.class.getName())) {
            // special case for strings
            dataTypeString = null;
        }
        Object value = strValue;
        if (dataTypeString != null) {
            try {
                Class<?> dataObjectClass = Class.forName(dataTypeString);
                value = valueOf(strValue, dataObjectClass);
            } catch (ClassNotFoundException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.append(e.toString() + " Value: " + strValue + ", Type: " + type + ", Stack Trace: ");
                e.printStackTrace(pw);
                pw.flush();
                log.warn(pw.toString());
            }
        }

        return value;
    }

    /**
     * Check if conversion will succeed
     *
     * @param value
     * @param type
     * @return true if conversion is good
     */
    public static boolean checkType(@NonNull String value, @NonNull Class<?> type) {
        try {
            Object cv = TypeConverter.valueOf(value, type);
            if (value.equals(cv.toString())) {
                return true;
            }
        } catch (IllegalArgumentException e) {
        }
        return false;
    }

    /**
     * @param strValue
     * @param type
     * @return
     */
    private static <TT> TT callMethod(String strValue, String methodName, Class<?> type) {
        try {
            MethodHandle method = valueOfMethod.computeIfAbsent(type, (k) -> {
                try {
                    return lookup.findStatic(type, methodName, MethodType.methodType(type, String.class));
                } catch (ReflectiveOperationException ex) {
                    throw Lombok.sneakyThrow(ex);
                }
            });
            return (TT)method.invoke(strValue);
        } catch(Throwable thr) {
            throw Lombok.sneakyThrow(thr);
        }
    }

    private static String processNumbers(String strValue) {
        switch (strValue) {
            case "nan":
                strValue = "NaN";
                break;
            case "inf":
                strValue = "Infinity";
                break;
            case "-inf":
                strValue = "-Infinity";
                break;
            case "":
                strValue = "0";
                break;
        }
        return strValue;
    }
}
