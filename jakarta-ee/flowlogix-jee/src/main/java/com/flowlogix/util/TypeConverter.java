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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import static java.util.Map.entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.Getter;
import lombok.Lombok;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Converts from any String to any type dynamically,
 * using static <p>{@code valueOf(String input)} method of the target type
 * <p>
 * Accepts database special values, such as {@literal inf, -inf, and nan}
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * int one = TypeConverter.valueOf("1", int.class);
 * }
 * </pre>
 * @author lprimak
 */
@Slf4j
public class TypeConverter {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final Map<Class<?>, MethodHandle> valueOfMethod = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Function<String, Result>> info = Map.ofEntries(
            // see https://ideone.com/WtNDN2
            entry(Double.class, value -> new Result(processNumbers(value), Double.class)),
            entry(double.class, value -> new Result(processNumbers(value), Double.class)),
            entry(Float.class, value -> new Result(processNumbers(value), Float.class)),
            entry(float.class, value -> new Result(processNumbers(value), Float.class)),
            entry(Integer.class, value -> new Result(processNumbers(value), Integer.class)),
            entry(int.class, value -> new Result(processNumbers(value), Integer.class)),
            entry(short.class, value -> new Result(processNumbers(value), Short.class)),
            entry(Long.class, value -> new Result(processNumbers(value), Long.class)),
            entry(long.class, value -> new Result(processNumbers(value), Long.class)),
            entry(Boolean.class, value -> new Result(value, boolean.class, "parseBoolean", Boolean.class)),
            entry(boolean.class, value -> new Result(value, boolean.class, "parseBoolean", Boolean.class)),
            entry(BigInteger.class, value -> new Result(new BigInteger(value))),
            entry(BigDecimal.class, value -> new Result(new BigDecimal(value))),
            entry(LocalDate.class, value -> new Result(value, "parse", CharSequence.class, LocalDate.class)),
            entry(LocalTime.class, value -> new Result(value, "parse", CharSequence.class, LocalTime.class)),
            entry(LocalDateTime.class, value -> new Result(value, "parse", CharSequence.class, LocalDateTime.class))
    );

    private static class Result {
        private final String value;
        private final Class<?> resultType;
        private final String method;
        private final Class<?> classToCall;
        private final Class<?> stringArgType;
        private final Object actualResult;

        public Result(String value, Class<?> resultType) {
            this(value, resultType, "valueOf", resultType);
        }

        public Result(String value, Class<?> resultType, String method, Class<?> classToCall) {
            this(value, resultType, method, classToCall, String.class, null);
        }

        public Result(String value, String method, Class<?> stringArgType, Class<?> resultType) {
            this(value, resultType, method, resultType, stringArgType, null);
        }

        public Result(Object actualResult) {
            this(null, null, null, null, null, actualResult);
        }

        public Result(String value, Class<?> resultType, String method, Class<?> classToCall,
                Class<?> stringArgType, Object actualResult) {
            this.value = value;
            this.resultType = resultType;
            this.method = method;
            this.classToCall = classToCall;
            this.stringArgType = stringArgType;
            this.actualResult = actualResult;
        }
    }

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
        Result result = info.getOrDefault(type, value -> new Result(value, type)).apply(strValue);
        if (result.actualResult != null) {
            return (TT) result.actualResult;
        } else {
            return (TT) callMethod(result.value, result.method, result.classToCall,
                    result.resultType, result.stringArgType);
        }
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
     * used by {@link TypeConverter#checkAndConvert(java.lang.String, java.lang.Class)}
     * @param <TT> type of value
     */
    @RequiredArgsConstructor
    public static class CheckedValue<TT> {
        /**
         * valid is true if conversion succeeded
         */
        private final @Getter boolean valid;
        /**
         * value of the converted object
         */
        private final @Getter TT value;
    }

    /**
     * Check if conversion will succeed
     *
     * @param <TT> type of class
     * @param value
     * @param type
     * @return true if conversion is good
     */
    public static <TT> boolean checkType(@NonNull String value, @NonNull Class<TT> type) {
        return checkAndConvert(value, type).valid;
    }

    /**
     *
     * @param <TT> type of class
     * @param value
     * @param type
     * @return checked type
     */
    public static <TT> CheckedValue<TT> checkAndConvert(@NonNull String value, @NonNull Class<TT> type) {
        try {
            TT cv = TypeConverter.valueOf(value, type);
            if (value.equals(cv.toString())) {
                return new CheckedValue<>(true, cv);
            }
        } catch (IllegalArgumentException e) {
        }
        return new CheckedValue<>(false, null);
    }

    /**
     * @param strValue
     * @param type
     * @return
     */
    private static <TT> TT callMethod(String strValue, String methodName, Class<?> type,
            Class<?> resultType, Class<?> stringArgType) {
        try {
            MethodHandle method = valueOfMethod.computeIfAbsent(type, (k) -> {
                try {
                    return lookup.findStatic(type, methodName, MethodType.methodType(resultType, stringArgType));
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
