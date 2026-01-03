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
package com.flowlogix.shiro.ee.filters;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.shiro.crypto.cipher.CipherService;
import org.apache.shiro.lang.util.ByteSource;
import org.apache.shiro.mgt.AbstractRememberMeManager;

/**
 * Combines seamless support for Shiro 1/2
 *
 * @author lprimak
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
@SuppressWarnings("HideUtilityClassConstructor")
class CryptoSupport {
    private static final MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
    private static final MethodType getClonedBytesMT = MethodType.methodType(byte[].class);
    private static final MethodHandle getClonedBytesHandle;
    private static final MethodHandle decryptHandle;

    static {
        MethodHandle getClonedMH = null;
        MethodHandle decryptMH = null;
        try {
            Class<?> byteSourceBroker = ClassUtils.getClass("org.apache.shiro.crypto.cipher.ByteSourceBroker");
            getClonedMH = publicLookup.findVirtual(byteSourceBroker, "getClonedBytes", getClonedBytesMT);
            MethodType getDecryptMT = MethodType.methodType(byteSourceBroker, byte[].class, byte[].class);
            decryptMH = publicLookup.in(CipherService.class).findVirtual(CipherService.class, "decrypt", getDecryptMT);
        } catch (ReflectiveOperationException ex) {
            log.debug("Shiro 2 initialization failed, falling back to Shiro 1", ex);
            try {
                getClonedMH = publicLookup.findVirtual(ByteSource.class, "getBytes", getClonedBytesMT);
                MethodType getDecryptMT = MethodType.methodType(ByteSource.class, byte[].class, byte[].class);
                decryptMH = publicLookup.in(CipherService.class).findVirtual(CipherService.class, "decrypt", getDecryptMT);
            } catch (ReflectiveOperationException ex1) {
                ExceptionUtils.asRuntimeException(ex1);
            }
        } finally {
            getClonedBytesHandle = getClonedMH;
            decryptHandle = decryptMH;
        }
    }

    static String decrypt(byte[] encrypted, AbstractRememberMeManager rememberMeManager) {
        return new String(getClonedBytes(decrypt(rememberMeManager.getCipherService(),
                encrypted, rememberMeManager.getDecryptionCipherKey())),
                StandardCharsets.UTF_8);
    }

    @SneakyThrows
    private static byte[] getClonedBytes(Object byteSourceBroker) {
        return (byte[]) getClonedBytesHandle.invoke(byteSourceBroker);
    }

    @SneakyThrows
    private static Object decrypt(CipherService cipherService, byte[] encrypted, byte[] key) {
        return decryptHandle.invoke(cipherService, encrypted, key);
    }
}
