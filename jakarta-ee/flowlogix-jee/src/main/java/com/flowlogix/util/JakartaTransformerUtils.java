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
package com.flowlogix.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ClassUtils;

/**
 *
 * @author lprimak
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("HideUtilityClassConstructor")
public class JakartaTransformerUtils {
    public static String jakartify(String className) {
        boolean isJakarta = false;
        try {
            isJakarta = ClassUtils.getClass("jakarta.servlet.http.HttpServletRequest") != null;
        } catch (ClassNotFoundException ex) {
        }
        return className.replaceFirst("javax.faces.", isJakarta ? "jakarta.faces." : "javax.faces.");
    }
}
