/*
 * Copyright (C) 2011-2022 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.shiro.ee.filters;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.Lombok;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.shiro.mgt.AbstractRememberMeManager;
import org.apache.shiro.util.ByteSource;

/**
 *
 * @author lprimak
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("HideUtilityClassConstructor")
class CryptoSupport {
    private static final MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
    private static final MethodType methodType = MethodType.methodType(byte[].class);
    private static final MethodHandle getClonedBytesHandle;

    static {
        MethodHandle mh = null;
        try {
            mh = publicLookup.findVirtual(ByteSource.class, "getClonedBytes", methodType);
        } catch (ReflectiveOperationException ex) {
            try {
                mh = publicLookup.findVirtual(ByteSource.class, "getBytes", methodType);
            } catch (ReflectiveOperationException ex1) {
                Lombok.sneakyThrow(ex1);
            }
        } finally {
            getClonedBytesHandle = mh;
        }
    }

    static String decrypt(byte[] encrypted, AbstractRememberMeManager rememberMeManager) {
        return new String(getClonedBytes(rememberMeManager.getCipherService()
                .decrypt(encrypted, rememberMeManager.getDecryptionCipherKey())),
                StandardCharsets.UTF_8);
    }

    @SneakyThrows
    static byte[] getClonedBytes(ByteSource byteSource) {
        return (byte[]) getClonedBytesHandle.invokeExact(byteSource);
    }
}
