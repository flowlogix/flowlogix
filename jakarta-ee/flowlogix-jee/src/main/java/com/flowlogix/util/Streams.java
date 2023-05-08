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
package com.flowlogix.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Read String from Stream
 * Does not close the stream afterward
 *
 * @author lprimak
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("HideUtilityClassConstructor")
public class Streams {
    /**
     * Converts InputStream to String
     *
     * @param strm Stream
     * @return Stream converted to String
     * @throws IOException
     */
    public static String readString(InputStream strm) throws IOException {
        return readString(strm, StandardCharsets.UTF_8);
    }

    /**
     * Converts InputStream to String
     *
     * @param strm Stream
     * @param charset Charset to convert with
     * @return Stream converted to String
     * @throws IOException
     */
    public static String readString(InputStream strm, Charset charset) throws IOException {
        return new String(strm.readAllBytes(), charset);
    }
}
