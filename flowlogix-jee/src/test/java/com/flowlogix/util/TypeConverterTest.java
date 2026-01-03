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
package com.flowlogix.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import org.junit.jupiter.api.Test;

/**
 *
 * @author lprimak
 */
class TypeConverterTest {
    @Test
    @SuppressWarnings("MagicNumber")
    void natives() {
        assertThat(TypeConverter.valueOf("20", Integer.class)).isEqualTo(20);
        assertThat(TypeConverter.valueOf("20", int.class)).isEqualTo(20);
        assertThat(TypeConverter.valueOf("20", Long.class)).isEqualTo(20L);
        assertThat(TypeConverter.valueOf("20", long.class)).isEqualTo(20L);
        assertThat(TypeConverter.valueOf("20.0", float.class)).isEqualTo(20.0f);
        assertThat(TypeConverter.valueOf("20.0", double.class)).isEqualTo(20.0d);
        assertThat(TypeConverter.valueOf("20.0", Double.class)).isEqualTo(20.0d);
        assertThat(TypeConverter.valueOf("20", Short.class)).isEqualTo((short) 20);
        assertThat(TypeConverter.valueOf("20", short.class)).isEqualTo((short) 20);
    }

    @Test
    @SuppressWarnings("MagicNumber")
    void checkDoubleValue() {
        var value = 1.23456789123456d;
        var converted = TypeConverter.checkAndConvert(Double.toString(value), double.class);
        assertThat(converted.isValid()).isTrue();
        assertThat(converted.getValue()).isEqualTo(value);
    }

    @Test
    @SuppressWarnings("MagicNumber")
    void checkFloatValue() {
        var value = 1.2345678f;
        var converted = TypeConverter.checkAndConvert(Float.toString(value), float.class);
        assertThat(converted.isValid()).isTrue();
        assertThat(converted.getValue()).isEqualTo(value);
    }

    @Test
    void cornerCases() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.valueOf(null, int.class));
        assertThat(TypeConverter.valueOf("", Double.class)).isZero();
        assertThat(TypeConverter.valueOf("", double.class)).isZero();
        assertThat(TypeConverter.valueOf("", int.class)).isZero();
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.valueOf(null, String.class));
        assertThat(TypeConverter.valueOf("hello", String.class)).isEqualTo("hello");
        assertThat(TypeConverter.valueOf("19:12:54", Time.class)).isEqualTo(Time.valueOf("19:12:54"));
        assertThat(TypeConverter.valueOf(Double.toString(Double.NaN), Double.class)).isNaN();
        assertThat(TypeConverter.valueOf(Double.toString(Double.POSITIVE_INFINITY), Double.class))
                .isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(TypeConverter.valueOf(Double.toString(Double.NEGATIVE_INFINITY), Double.class))
                .isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(TypeConverter.valueOf("nan", float.class)).isNaN();
        assertThat(TypeConverter.valueOf("inf", float.class)).isEqualTo(Float.POSITIVE_INFINITY);
        assertThat(TypeConverter.valueOf("-inf", float.class)).isEqualTo(Float.NEGATIVE_INFINITY);
    }

    @Test
    void nonNumbers() {
        assertThat(TypeConverter.valueOf("true", boolean.class)).isTrue();
        assertThat(TypeConverter.valueOf("true", Boolean.class)).isTrue();
    }

    @Test
    void bigNumbers() {
        assertThat(TypeConverter.valueOf(BigInteger.ONE.toString(), BigInteger.class)).isEqualTo(BigInteger.ONE);
        assertThat(TypeConverter.valueOf(BigDecimal.ONE.toPlainString(), BigDecimal.class)).isEqualTo(BigDecimal.ONE);
    }

    @SuppressWarnings("checkstyle:JavadocVariable")
    private enum MyEnum {
        ONE, TWO;
    }

    @Test
    void enums() {
        assertThat(TypeConverter.valueOf(MyEnum.ONE.name(), MyEnum.class)).isEqualTo(MyEnum.ONE);
    }

    @Test
    void dates() {
        LocalDate today = LocalDate.now();
        assertThat(TypeConverter.valueOf(today.toString(), LocalDate.class)).isEqualTo(today);
    }

    @Test
    void stringClassName() {
        assertThat(TypeConverter.valueOf("true", Boolean.class.getName())).isEqualTo(Boolean.TRUE);
    }

    @Test
    void invalidArgument() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> TypeConverter.valueOf("abc", Integer.class));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> TypeConverter.valueOf("abc", Integer.class.getName()));
        assertThat(TypeConverter.valueOf("abc", "invalid")).isEqualTo("abc");
    }

    @Test
    void checkType() {
        assertThat(TypeConverter.checkType("true", boolean.class)).isTrue();
        assertThat(TypeConverter.checkType("5", boolean.class)).isFalse();
    }

    @Test
    void valueOfNull() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.valueOf(null, String.class));
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.valueOf("abc", (Class<?>) null));

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.valueOf(null, String.class.getName()));
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.valueOf("abc", (String) null));

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.checkType(null, String.class));
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.checkType("abc", (Class<?>) null));

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.checkAndConvert(null, String.class));
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> TypeConverter.checkAndConvert("abc", (Class<?>) null));
    }

    @Test
    void stringShortcut() {
        assertThat(TypeConverter.valueOf("abc", String.class.getName())).isEqualTo("abc");
    }

    @Test
    void reflectiveException() {
        class Value { }
        assertThatExceptionOfType(IllegalAccessException.class)
                .isThrownBy(() -> TypeConverter.valueOf("abc", Value.class));
    }
}
