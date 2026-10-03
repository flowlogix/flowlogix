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

import com.flowlogix.starter.GeneratorClient.GeneratorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorClientTest {
    private static final String ARCHIVE_TYPE = "application/octet-stream";

    private static GeneratorException failure(URI generatorUrl, Path dir) {
        var client = new GeneratorClient(generatorUrl);
        return assertThrows(GeneratorException.class,
                () -> client.generate(client.request(Map.of("artifact", "app")), "app", dir.resolve("app")));
    }

    @Test
    void putsTheParametersInThePath() {
        var parameters = new LinkedHashMap<String, String>();
        parameters.put("group", "com.acme");
        parameters.put("artifact", "my-app");

        assertEquals("https://example.com/sg/download/;group=com.acme;artifact=my-app",
                new GeneratorClient(URI.create("https://example.com/sg/download")).request(parameters).toString());
    }

    @Test
    void encodesValuesThatWouldBreakThePath() {
        URI request = new GeneratorClient(URI.create("https://example.com/"))
                .request(Map.of("projectName", "My Project; v=2/3"));

        assertEquals("/;projectName=My%20Project%3B%20v%3D2%2F3", request.getRawPath());
    }

    @Test
    void unpacksTheProjectAndKeepsTheWrapperExecutable(@TempDir Path dir) throws IOException, InterruptedException {
        byte[] archive = GeneratorStub.archive(dir, "app/pom.xml", "app/mvnw", "app/src/main/java/g/App.java");
        try (var generator = new GeneratorStub(HTTP_OK, ARCHIVE_TYPE, archive)) {
            var client = new GeneratorClient(generator.url());
            Path project = dir.resolve("out/app");

            client.generate(client.request(Map.of("artifact", "app")), "app", project);

            assertEquals(List.of("/sg/download/;artifact=app"), generator.requests());
            assertEquals("app/src/main/java/g/App.java", Files.readString(project.resolve("src/main/java/g/App.java")));
            assertTrue(Files.isExecutable(project.resolve("mvnw")));
            assertFalse(Files.isExecutable(project.resolve("pom.xml")));
        }
    }

    @Test
    void reportsMavenOutputWhenTheGeneratorFails(@TempDir Path dir) throws IOException {
        byte[] output = "BUILD FAILURE".getBytes(StandardCharsets.UTF_8);
        try (var generator = new GeneratorStub(HTTP_INTERNAL_ERROR, "text/plain", output)) {
            GeneratorException failure = failure(generator.url(), dir);

            assertEquals("The generator failed (HTTP 500)." + System.lineSeparator() + "BUILD FAILURE",
                    failure.getMessage());
            assertFalse(failure.rejected());
            assertFalse(Files.exists(dir.resolve("app")));
        }
    }

    @Test
    void marksARejectedRequestAndLeavesOutTheErrorPage(@TempDir Path dir) throws IOException {
        byte[] page = "<html>Bad Request</html>".getBytes(StandardCharsets.UTF_8);
        try (var generator = new GeneratorStub(HTTP_BAD_REQUEST, "text/html", page)) {
            GeneratorException failure = failure(generator.url(), dir);

            assertEquals("The generator rejected the request (HTTP 400).", failure.getMessage());
            assertTrue(failure.rejected());
        }
    }

    @Test
    void failsWhenTheGeneratorCannotBeReached(@TempDir Path dir) throws IOException {
        URI url;
        try (var generator = new GeneratorStub(HTTP_OK, ARCHIVE_TYPE, new byte[0])) {
            url = generator.url();
        }

        GeneratorException failure = failure(url, dir);

        assertTrue(failure.getMessage().startsWith("Could not reach the generator at " + url), failure.getMessage());
        assertFalse(failure.rejected());
    }

    @Test
    void leavesNothingBehindWhenTheArchiveHasNoProject(@TempDir Path dir) throws IOException {
        try (var generator = new GeneratorStub(HTTP_OK, ARCHIVE_TYPE, GeneratorStub.archive(dir, "other/pom.xml"))) {
            GeneratorException failure = failure(generator.url(), dir);

            assertEquals("Could not unpack the generated project: the archive has no 'app' directory",
                    failure.getMessage());
            assertFalse(Files.exists(dir.resolve("app")));
        }
    }

    @Test
    void keepsADirectoryThatWasAlreadyThere(@TempDir Path dir) throws IOException {
        Path project = Files.createDirectory(dir.resolve("app"));
        Files.writeString(project.resolve("pom.xml"), "mine");
        Files.writeString(project.resolve("notes.txt"), "mine");
        try (var generator = new GeneratorStub(HTTP_OK, ARCHIVE_TYPE, GeneratorStub.archive(dir, "app/pom.xml"))) {
            GeneratorException failure = failure(generator.url(), dir);

            assertEquals("Could not unpack the generated project: '%s' already exists".formatted(project),
                    failure.getMessage());
            assertEquals("mine", Files.readString(project.resolve("pom.xml")));
            assertEquals("mine", Files.readString(project.resolve("notes.txt")));
        }
    }
}
