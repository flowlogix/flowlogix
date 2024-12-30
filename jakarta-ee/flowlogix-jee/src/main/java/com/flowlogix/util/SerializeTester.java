/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Serialization test mechanism
 * <p>
 * <em>Example:</em>
 * {@snippet class="com.flowlogix.demo.util.SerializeDemo" region="serialize"}
 *
 * @author lprimak
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("HideUtilityClassConstructor")
public class SerializeTester {
    @SuppressWarnings("unchecked")
    public static <TT> TT serializeAndDeserialize(TT original)
            throws IOException, ClassNotFoundException {
        // serialize
        var byteArrayOutputStream = new ByteArrayOutputStream();
        try (var outputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            outputStream.writeObject(original);
        }

        // deserialize
        try (var inputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
            return (TT) inputStream.readObject();
        }
    }
}
