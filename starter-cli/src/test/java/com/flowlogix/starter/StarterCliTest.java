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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarterCliTest {
    private static final int MAX_ARTIFACT_ID = 100;
    private static final String ARCHIVE_TYPE = "application/octet-stream";
    private static final Set<String> CLI_ONLY = Set.of("--output-dir", "--generator-url", "--dry-run",
            "--help", "--version");

    private record Result(int exitCode, String out, String err) { }

    private static Map<String, String> parameters(String... args) {
        var command = new StarterCli();
        new CommandLine(command).parseArgs(args);
        return command.parameters();
    }

    private static Result run(String... args) {
        var out = new StringWriter();
        var err = new StringWriter();
        int exitCode = new CommandLine(new StarterCli())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err))
                .execute(args);
        return new Result(exitCode, out.toString(), err.toString());
    }

    private static Result generate(GeneratorStub generator, Path dir) {
        return run("-g", "g", "-a", "app", "-o", dir.toString(), "--generator-url", generator.url().toString());
    }

    @Test
    void sendsOnlyWhatTheUserSet() {
        assertEquals(Map.of(
                "group", "com.acme",
                "artifact", "my-app",
                "package", "com.acme",
                "baseType", "payara",
                "packagingType", "jar",
                "archetypeVersion", "LATEST"),
                parameters("-g", "com.acme", "-a", "my-app"));
    }

    @Test
    void sendsOnlyTheTogglesThatAreOn() {
        Map<String, String> parameters = parameters("-g", "g", "-a", "a", "--primefaces");

        assertEquals("true", parameters.get("usePrimeFaces"));
        assertFalse(parameters.containsKey("useShiro"), parameters.toString());
    }

    @Test
    void sendsEveryOptionUnderTheGeneratorsName() {
        Map<String, String> parameters = parameters("-g", "com.acme", "-a", "my-app", "-p", "com.acme.app",
                "-n", "My App", "-v", "1.0", "-t", "infra", "-k", "war", "--archetype-version", "148",
                "--shiro", "--agentic-ai", "--omnifaces", "--primefaces", "--lazy-model", "--maven-cache",
                "--code-coverage", "--arquillian-graphene");

        assertEquals(Map.ofEntries(
                Map.entry("group", "com.acme"),
                Map.entry("artifact", "my-app"),
                Map.entry("package", "com.acme.app"),
                Map.entry("projectName", "My App"),
                Map.entry("version", "1.0"),
                Map.entry("baseType", "infra"),
                Map.entry("packagingType", "war"),
                Map.entry("archetypeVersion", "148"),
                Map.entry("useShiro", "true"),
                Map.entry("useAgenticAI", "true"),
                Map.entry("useOmniFaces", "true"),
                Map.entry("usePrimeFaces", "true"),
                Map.entry("useLazyModel", "true"),
                Map.entry("useMavenCache", "true"),
                Map.entry("useCodeCoverage", "true"),
                Map.entry("useArquillianGraphene", "true")), parameters);
        // an option left out of parameters() would be accepted and then ignored
        assertEquals(parameters.size(), new CommandLine(new StarterCli()).getCommandSpec().options().stream()
                .filter(option -> !CLI_ONLY.contains(option.longestName())).count());
    }

    @Test
    void rejectsAnArtifactIdTheGeneratorWouldReject() {
        Result result = run("-g", "com.acme", "-a", "my app", "--dry-run");

        assertEquals(2, result.exitCode());
        assertTrue(result.err().contains("Invalid value for option '--artifact-id'"), result.err());
    }

    @Test
    void requiresAPackageWhenTheGroupIdCannotBeOne() {
        Result result = run("-g", "com.my-company", "-a", "app", "--dry-run");

        assertEquals(2, result.exitCode());
        assertTrue(result.err().contains("set one with --package"), result.err());
        assertEquals(0, run("-g", "com.my-company", "-a", "app", "-p", "com.mycompany", "--dry-run").exitCode());
    }

    @Test
    void rejectsJavaKeywordsInThePackage() {
        Result fromGroupId = run("-g", "com.acme.new", "-a", "app", "--dry-run");
        Result explicit = run("-g", "com.acme", "-a", "app", "-p", "org.example.default", "--dry-run");

        assertEquals(2, fromGroupId.exitCode());
        assertTrue(fromGroupId.err().contains("set one with --package"), fromGroupId.err());
        assertEquals(2, explicit.exitCode());
        assertTrue(explicit.err().contains("Invalid value for option '--package'"), explicit.err());
    }

    @Test
    void checksTheGroupIdEvenWhenThePackageIsSet() {
        Result result = run("-g", "com acme", "-a", "app", "-p", "com.acme", "--dry-run");

        assertEquals(2, result.exitCode());
        assertTrue(result.err().contains("Invalid value for option '--group-id'"), result.err());
    }

    @Test
    void limitsTheArtifactIdLengthLikeTheGenerator() {
        assertEquals(0, run("-g", "g", "-a", "a".repeat(MAX_ARTIFACT_ID), "--dry-run").exitCode());
        assertEquals(2, run("-g", "g", "-a", "a".repeat(MAX_ARTIFACT_ID + 1), "--dry-run").exitCode());
    }

    @Test
    void rejectsAnUnknownPackaging() {
        Result result = run("-g", "g", "-a", "a", "-k", "zip", "--dry-run");

        assertEquals(2, result.exitCode());
        assertTrue(result.err().contains("expected one of [jar, war, ear]"), result.err());
    }

    @Test
    void rejectsAGeneratorUrlThatIsNotHttp() {
        Result result = run("-g", "g", "-a", "a", "--generator-url", "ftp://example.com/", "--dry-run");

        assertEquals(2, result.exitCode());
        assertTrue(result.err().contains("Invalid value for option '--generator-url'"), result.err());
    }

    @Test
    void requiresTheGroupIdAndTheArtifactId() {
        Result result = run("--dry-run");

        assertEquals(2, result.exitCode());
        assertTrue(result.err().contains("Missing required options: '--group-id=<id>', '--artifact-id=<id>'"),
                result.err());
    }

    @Test
    void printsACommandThatSurvivesCopyAndPaste() {
        Result result = run("-g", "g", "-a", "a", "--dry-run");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().startsWith("curl -fsSL -H 'Accept: application/octet-stream' -o a.zip "
                + "'https://start.flowlogix.com/sg/download/;group=g;artifact=a;"), result.out());
        assertEquals("'it'\\''s'", StarterCli.quote("it's"));
    }

    @Test
    void refusesAnExistingProjectDirectoryBeforeCallingTheGenerator(@TempDir Path dir) throws IOException {
        Files.createDirectory(dir.resolve("app"));
        try (var generator = new GeneratorStub(HTTP_OK, ARCHIVE_TYPE, GeneratorStub.archive(dir, "app/pom.xml"))) {
            Result result = generate(generator, dir);
            Result dryRun = run("-g", "g", "-a", "app", "-o", dir.toString(), "--dry-run");

            assertEquals(1, result.exitCode());
            assertTrue(result.err().contains("already exists"), result.err());
            assertEquals(1, dryRun.exitCode());
            assertEquals(List.of(), generator.requests());
        }
    }

    @Test
    void createsTheProject(@TempDir Path dir) throws IOException {
        try (var generator = new GeneratorStub(HTTP_OK, ARCHIVE_TYPE, GeneratorStub.archive(dir, "app/pom.xml"))) {
            Result result = generate(generator, dir.resolve("out"));

            assertEquals(0, result.exitCode(), result.err());
            assertEquals(List.of("/sg/download/;group=g;artifact=app;package=g;baseType=payara;packagingType=jar"
                    + ";archetypeVersion=LATEST"), generator.requests());
            assertTrue(Files.exists(dir.resolve("out/app/pom.xml")));
            assertTrue(result.out().contains("Created "), result.out());
        }
    }

    @Test
    void treatsARejectedRequestAsInvalidInput(@TempDir Path dir) throws IOException {
        byte[] page = "<html>Bad Request</html>".getBytes(StandardCharsets.UTF_8);
        try (var generator = new GeneratorStub(HTTP_BAD_REQUEST, "text/html", page)) {
            Result result = generate(generator, dir);

            assertEquals(2, result.exitCode());
            assertTrue(result.err().contains("The generator rejected the request (HTTP 400)."), result.err());
        }
    }

    @Test
    void showsWhyTheGeneratorFailed(@TempDir Path dir) throws IOException {
        byte[] output = "BUILD FAILURE".getBytes(StandardCharsets.UTF_8);
        try (var generator = new GeneratorStub(HTTP_INTERNAL_ERROR, "text/plain", output)) {
            Result result = generate(generator, dir);

            assertEquals(1, result.exitCode());
            assertTrue(result.err().contains("BUILD FAILURE"), result.err());
        }
    }
}
