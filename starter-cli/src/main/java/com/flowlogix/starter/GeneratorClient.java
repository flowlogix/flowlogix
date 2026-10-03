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
package com.flowlogix.starter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Client of the starter generator's download endpoint, the one behind start.flowlogix.com:
 * it requests a project and unpacks the archive that comes back.
 */
class GeneratorClient {
    private static final String ARCHIVE_TYPE = "application/octet-stream";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** The generator runs Maven for every request, which takes several seconds. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);
    /** Entries carry no permissions unless the generator marked them executable. */
    private static final Map<String, ?> ARCHIVE_OPTIONS = Map.of("enablePosixFileAttributes", true,
            "defaultPermissions", "rw-r--r--");

    private final URI generatorUrl;

    GeneratorClient(URI generatorUrl) {
        this.generatorUrl = generatorUrl;
    }

    /** A failed generation, with a message fit to show the user. */
    static class GeneratorException extends IOException {
        private static final long serialVersionUID = 1L;
        private final boolean rejected;

        GeneratorException(String message, boolean rejected) {
            super(message);
            this.rejected = rejected;
        }

        /** Whether the generator refused the parameters, rather than failing on its own. */
        boolean rejected() {
            return rejected;
        }
    }

    /**
     * Builds the request. The generator takes its parameters as matrix parameters
     * of the path, and applies its own defaults to the ones left out.
     */
    URI request(Map<String, String> parameters) {
        String base = generatorUrl.toString();
        return URI.create((base.endsWith("/") ? base : base + "/") + parameters.entrySet().stream()
                .map(parameter -> ";%s=%s".formatted(parameter.getKey(), encode(parameter.getValue())))
                .collect(Collectors.joining()));
    }

    /** Requests the project and unpacks its directory, which the generator names after the artifact id. */
    void generate(URI request, String artifactId, Path projectDir) throws IOException, InterruptedException {
        Path archive = Files.createTempFile("flowlogix-starter", ".zip");
        try {
            HttpResponse<Path> response;
            try {
                response = download(request, archive);
            } catch (IOException e) {
                throw new GeneratorException("Could not reach the generator at %s: %s"
                        .formatted(generatorUrl, describe(e)), false);
            }
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw failure(response);
            }
            try {
                unpack(archive, artifactId, projectDir);
            } catch (IOException e) {
                throw new GeneratorException("Could not unpack the generated project: " + describe(e), false);
            }
        } finally {
            deleteQuietly(archive);
        }
    }

    private static HttpResponse<Path> download(URI request, Path archive) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        return client.send(HttpRequest.newBuilder(request).timeout(REQUEST_TIMEOUT)
                .header("Accept", ARCHIVE_TYPE).build(), BodyHandlers.ofFile(archive));
    }

    /** The same request as a curl command, for a dry run to print: -L because redirects are followed here too. */
    static List<String> curlCommand(URI request, String artifactId) {
        return List.of("curl", "-fsSL", "-H", "Accept: " + ARCHIVE_TYPE, "-o", artifactId + ".zip", request.toString());
    }

    private static GeneratorException failure(HttpResponse<Path> response) throws IOException {
        boolean rejected = response.statusCode() == HttpURLConnection.HTTP_BAD_REQUEST;
        String message = (rejected ? "The generator rejected the request (HTTP %d)."
                : "The generator failed (HTTP %d).").formatted(response.statusCode());
        // Maven's output arrives as plain text, but a rejected parameter comes back as
        // the server's HTML error page, which does not carry the reason
        if (response.headers().firstValue("Content-Type").orElse("").startsWith("text/plain")) {
            message += System.lineSeparator()
                    + new String(Files.readAllBytes(response.body()), StandardCharsets.UTF_8).strip();
        }
        return new GeneratorException(message, rejected);
    }

    /** Unpacks the project, keeping the executable bit the generator sets on the Maven wrapper. */
    private static void unpack(Path archive, String artifactId, Path projectDir) throws IOException {
        try (FileSystem zip = FileSystems.newFileSystem(archive, ARCHIVE_OPTIONS)) {
            Path source = zip.getPath("/", artifactId);
            if (!Files.isDirectory(source)) {
                throw new IOException("the archive has no '%s' directory".formatted(artifactId));
            }
            createNew(projectDir);
            try (Stream<Path> entries = Files.walk(source)) {
                for (Path entry : (Iterable<Path>) entries::iterator) {
                    Path target = projectDir.resolve(source.relativize(entry).toString());
                    if (Files.isDirectory(entry)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(entry, target);
                        if (Files.getPosixFilePermissions(entry).contains(PosixFilePermission.OWNER_EXECUTE)) {
                            target.toFile().setExecutable(true, false);
                        }
                    }
                }
            } catch (IOException | UncheckedIOException e) {
                deleteQuietly(projectDir);
                throw e;
            }
        }
    }

    /** Refuses a directory that is already there, so that a failed unpack deletes only what it wrote. */
    private static void createNew(Path projectDir) throws IOException {
        Files.createDirectories(projectDir.toAbsolutePath().getParent());
        try {
            Files.createDirectory(projectDir);
        } catch (FileAlreadyExistsException e) {
            throw new IOException("'%s' already exists".formatted(projectDir));
        }
    }

    /** Best effort: a leftover file must not turn a generated project into a failure. */
    private static void deleteQuietly(Path path) {
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException | UncheckedIOException e) {
            // whatever is left stays where it is
        }
    }

    private static String describe(IOException e) {
        return Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());
    }

    /** Percent-encodes a matrix parameter value; in a path, '+' would not mean a space. */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
