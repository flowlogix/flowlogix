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
package com.flowlogix.examples.ui.servlets;

import com.flowlogix.logcapture.LogCapture;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.LogRecord;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 *
 * @author lprimak
 */
@WebServlet("/lastException")
public class ExceptionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings("EmptyBlock")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        resp.setContentType(TEXT_PLAIN);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        LogRecord logRecord = LogCapture.get().poll();
        while (logRecord != null) {
            if (logRecord.getThrown() != null) {
                var thrownString = logRecord.getThrown().toString();
                if (thrownString.contains("Unsupported class file major version")
                    || logRecord.getMessage().contains("class was compiled with an unsupported JDK")) {
                    // do not report outdated ASM class file errors
                } else {
                    out.printf("%s: %s", logRecord.getLevel(), thrownString);
                    out.print(System.lineSeparator());
                }
            }
            logRecord = LogCapture.get().poll();
        }
        out.flush();
    }
}
