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

/*
 * Builds a start.flowlogix.com download URL and the curl command to fetch it, by the same rules as
 * DownloadRequest in flowlogix/apps starter-generator/common.
 */
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StarterUrl {
    private static final String DEFAULT_ENDPOINT = "https://start.flowlogix.com/sg/download/";
    private static final String ARCHIVE_TYPE = "application/octet-stream";
    private static final String DEFAULT_ARTIFACT = "starter";

    public static void main(String[] args) {
        String endpoint = DEFAULT_ENDPOINT;
        boolean urlOnly = false;
        Map<String, String> parameters = new LinkedHashMap<>();
        Iterator<String> arguments = List.of(args).iterator();
        while (arguments.hasNext()) {
            String arg = arguments.next();
            if (arg.equals("--endpoint") && arguments.hasNext()) {
                endpoint = arguments.next();
            } else if (arg.equals("--url-only")) {
                urlOnly = true;
            } else if (arg.equals("-h") || arg.equals("--help")) {
                System.out.println("Usage: java StarterUrl.java [--endpoint URL] [--url-only] name=value ...");
                return;
            } else {
                int separator = arg.indexOf('=');
                if (separator < 1) {
                    System.err.println("Expected name=value, got: " + arg);
                    System.exit(1);
                }
                parameters.put(arg.substring(0, separator).trim(), arg.substring(separator + 1));
            }
        }
        defaultPackageToGroup(parameters);

        String url = url(endpoint, parameters);
        if (urlOnly) {
            System.out.println(url);
        } else {
            String artifact = parameters.getOrDefault("artifact", "").trim();
            System.out.println("curl -fsSL -X GET -H \"Accept: %s\" -o \"%s.zip\" \"%s\"".formatted(
                    ARCHIVE_TYPE, artifact.isEmpty() ? DEFAULT_ARTIFACT : artifact, url));
        }
    }

    /** Blank parameters are left out, so the generator applies its own defaults to them. */
    static String url(String endpoint, Map<String, String> parameters) {
        return (endpoint.endsWith("/") ? endpoint : endpoint + "/") + parameters.entrySet().stream()
                .filter(parameter -> !isBlank(parameter.getValue()))
                .map(parameter -> ";%s=%s".formatted(parameter.getKey(), encode(parameter.getValue().trim())))
                .collect(Collectors.joining());
    }

    /** Like the CLI: without a package, use the group rather than the generator's own default. */
    private static void defaultPackageToGroup(Map<String, String> parameters) {
        String group = parameters.get("group");
        if (isBlank(parameters.get("package")) && !isBlank(group)) {
            parameters.put("package", group);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Percent-encodes a matrix parameter value; in a path, '+' would not mean a space. */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
