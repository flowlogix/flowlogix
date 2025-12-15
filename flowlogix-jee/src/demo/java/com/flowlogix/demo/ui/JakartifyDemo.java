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
package com.flowlogix.demo.ui;

import com.flowlogix.util.JakartaTransformerUtils;

@SuppressWarnings("HideUtilityClassConstructor")
public class JakartifyDemo {
    public static String jakartifyServlet() {
        // @start region="jakartifyServlet"
        // tag::jakartifyServlet[] // @replace regex='.*\n' replacement=""
        // returns "jakarta.servlet.Servlet" in Jakarta artifacts
        String jakartaServlet = JakartaTransformerUtils.jakartify("javax.servlet.Servlet");
        // end::jakartifyServlet[] // @replace regex='.*\n' replacement=""
        // @end
        return jakartaServlet;
    }

    public static String jakartifyError() {
        // @start region="jakartifyError"
        // tag::jakartifyError[] // @replace regex='.*\n' replacement=""
        // returns "jakarta.faces.FacesException: message X" in Jakarta artifacts
        String jakartaError = JakartaTransformerUtils.jakartify("javax.faces.FacesException: message X");
        // end::jakartifyError[] // @replace regex='.*\n' replacement=""
        // @end
        return jakartaError;
    }
}
