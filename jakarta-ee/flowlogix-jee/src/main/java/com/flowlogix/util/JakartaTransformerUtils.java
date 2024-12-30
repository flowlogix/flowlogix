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

import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Transform strings that start with javax package into jakarta strings,
 * if it's detected to be in jakarta environment
 * <p>
 * <em>Examples:</em>
 * {@snippet class="com.flowlogix.demo.ui.JakartifyDemo" region="jakartifyServlet"}
 * {@snippet class="com.flowlogix.demo.ui.JakartifyDemo" region="jakartifyError"}
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("HideUtilityClassConstructor")
public class JakartaTransformerUtils {
    /**
     * Returns true is Jakarta environment is detected
     */
    @Getter
    @SuppressWarnings("ConstantName")
    private static final boolean jakarta = HttpServletRequest.class.getPackageName().startsWith("jakarta");
    private static final Pattern REPLACE_JAVA_WITH_JAKARTA_PATTERN = Pattern.compile("javax\\.(\\w+)\\.");

    /**
     * Transform javax string into jakarta string, if it's detected to be in jakarta environment,
     * otherwise returns original string
     *
     * @param javaxString string that starts with javax package
     * @return string optionally transformed into Jakarta namespace
     */
    public static String jakartify(String javaxString) {
        return REPLACE_JAVA_WITH_JAKARTA_PATTERN.matcher(javaxString).replaceAll(
                isJakarta() ? "jakarta.$1." : "javax.$1.");
    }
}
