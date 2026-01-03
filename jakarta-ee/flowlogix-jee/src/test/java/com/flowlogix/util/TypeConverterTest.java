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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author lprimak
 */
public class TypeConverterTest {
    @Test
    @SuppressWarnings("MagicNumber")
    void natives() {
        assertEquals(20, TypeConverter.valueOf("20", Integer.class));
        assertEquals(20, TypeConverter.valueOf("20", int.class));
        assertEquals(20L, TypeConverter.valueOf("20", Long.class));
        assertEquals(20L, TypeConverter.valueOf("20", long.class));
        assertEquals(20.0f, TypeConverter.valueOf("20.0", float.class));
        assertEquals(20.0d, TypeConverter.valueOf("20.0", double.class));
        assertEquals(20.0d, TypeConverter.valueOf("20.0", Double.class));
        assertEquals((short) 20, TypeConverter.valueOf("20", Short.class));
        assertEquals((short) 20, TypeConverter.valueOf("20", short.class));
    }

    @Test
    void cornerCases() {
        assertThrows(NullPointerException.class, () -> TypeConverter.valueOf(null, int.class));
        assertEquals(0, TypeConverter.valueOf("", Double.class));
        assertEquals(0, TypeConverter.valueOf("", double.class));
        assertEquals(0, TypeConverter.valueOf("", int.class));
        assertThrows(NullPointerException.class, () -> TypeConverter.valueOf(null, String.class));
        assertEquals("hello", TypeConverter.valueOf("hello", String.class));
        assertEquals(Time.valueOf("19:12:54"), TypeConverter.valueOf("19:12:54", Time.class));
        assertEquals(Double.NaN, TypeConverter.valueOf(Double.toString(Double.NaN), Double.class));
        assertEquals(Double.POSITIVE_INFINITY, TypeConverter.valueOf(Double.toString(Double.POSITIVE_INFINITY), Double.class));
        assertEquals(Double.NEGATIVE_INFINITY, TypeConverter.valueOf(Double.toString(Double.NEGATIVE_INFINITY), Double.class));
        assertEquals(Float.NaN, TypeConverter.valueOf("nan", float.class));
        assertEquals(Float.POSITIVE_INFINITY, TypeConverter.valueOf("inf", float.class));
        assertEquals(Float.NEGATIVE_INFINITY, TypeConverter.valueOf("-inf", float.class));
    }

    @Test
    void nonNumbers() {
        assertEquals(true, TypeConverter.valueOf("true", boolean.class));
        assertEquals(true, TypeConverter.valueOf("true", Boolean.class));
    }

    @Test
    void bigNumbers() {
        assertEquals(BigInteger.ONE, TypeConverter.valueOf(BigInteger.ONE.toString(), BigInteger.class));
        assertEquals(BigDecimal.ONE, TypeConverter.valueOf(BigDecimal.ONE.toPlainString(), BigDecimal.class));
    }

    @SuppressWarnings("checkstyle:JavadocVariable")
    private enum MyEnum {
        ONE, TWO;
    }

    @Test
    void enums() {
        assertEquals(MyEnum.ONE, TypeConverter.valueOf(MyEnum.ONE.name(), MyEnum.class));
    }

    @Test
    void dates() {
        LocalDate today = LocalDate.now();
        assertEquals(today, TypeConverter.valueOf(today.toString(), LocalDate.class));
    }

    @Test
    void stringClassName() {
        assertEquals(true, TypeConverter.valueOf("true", Boolean.class.getName()));
    }

    @Test
    void invalidArgument() {
        assertThrows(IllegalArgumentException.class, () -> TypeConverter.valueOf("abc", Integer.class));
        assertThrows(IllegalArgumentException.class, () -> TypeConverter.valueOf("abc", Integer.class.getName()));
        assertEquals("abc", TypeConverter.valueOf("abc", "invalid"));
    }

    @Test
    void checkType() {
        assertTrue(TypeConverter.checkType("true", boolean.class));
        assertFalse(TypeConverter.checkType("5", boolean.class));
    }
}
