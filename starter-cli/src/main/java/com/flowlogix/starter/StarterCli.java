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
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;

/**
 * CLI front end for the Flow Logix starter generator.
 *
 * <p>It hands the options to the start.flowlogix.com generator through {@link GeneratorClient},
 * so the same options produce the same project as the web page does.
 * The validation lives here because the generator checks only the artifact id: any other
 * bad value, an unknown base type for one, would produce a broken project and still exit with 0.
 */
@Command(name = "flowlogix-starter", mixinStandardHelpOptions = true, versionProvider = StarterCli.BuildVersion.class,
        sortOptions = false, usageHelpAutoWidth = true,
        description = "Scaffolds a new Jakarta EE project with the Flow Logix starter generator.",
        exitCodeListHeading = "%nExit codes:%n",
        exitCodeList = {"0:Project created (or request printed, with --dry-run)",
                "1:Generation failed: target directory exists, or the generator failed or could not be reached",
                "2:Invalid input: unknown option, invalid value, or missing --group-id/--artifact-id"})
public class StarterCli implements Callable<Integer> {
    /** Same rule, length included, the starter generator applies to artifact ids. */
    private static final Pattern ARTIFACT_ID = Pattern.compile("[a-zA-Z0-9_-]{1,100}");
    /** Maven's own rule for ids, which the generator does not check. */
    private static final Pattern MAVEN_ID = Pattern.compile("[A-Za-z0-9_.-]+");
    /** Characters that need no quoting when the command is printed for copy and paste. */
    private static final Pattern SHELL_SAFE = Pattern.compile("[\\w./:=@,+-]+");

    @Spec
    CommandSpec spec;

    @Option(names = {"-g", "--group-id"}, paramLabel = "<id>", required = true, description = "Maven groupId")
    String groupId;

    @Option(names = {"-a", "--artifact-id"}, paramLabel = "<id>", required = true,
            description = "Maven artifactId and project directory name")
    String artifactId;

    @Option(names = {"-n", "--name"}, paramLabel = "<text>", description = "Human-readable project name")
    String projectName;

    @Option(names = {"-p", "--package"}, paramLabel = "<pkg>", description = "Base Java package (default: groupId)")
    String packageName;

    @Option(names = {"-v", "--project-version"}, paramLabel = "<ver>", description = "Version of the new project")
    String projectVersion;

    @Option(names = {"-t", "--base-type"}, paramLabel = "<type>", defaultValue = "payara",
            description = "Parent POM: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    BaseType baseType;

    @Option(names = {"-k", "--packaging"}, paramLabel = "<type>", defaultValue = "jar",
            description = "Packaging: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    Packaging packaging;

    @Option(names = {"--archetype-version"}, paramLabel = "<ver>", defaultValue = "LATEST",
            description = "Pin the archetype version (default: ${DEFAULT-VALUE})")
    String archetypeVersion;

    @Option(names = {"--shiro"}, description = "Apache Shiro security")
    boolean useShiro;

    @Option(names = {"--agentic-ai"}, description = "Jakarta Agentic AI API")
    boolean useAgenticAI;

    @Option(names = {"--omnifaces"}, description = "OmniFaces")
    boolean useOmniFaces;

    @Option(names = {"--primefaces"}, description = "PrimeFaces")
    boolean usePrimeFaces;

    @Option(names = {"--lazy-model"}, description = "JPA Lazy DataModel")
    boolean useLazyModel;

    @Option(names = {"--maven-cache"}, description = "Maven build cache")
    boolean useMavenCache;

    @Option(names = {"--code-coverage"}, description = "JaCoCo code coverage")
    boolean useCodeCoverage;

    @Option(names = {"--arquillian-graphene"}, description = "Arquillian Graphene tests")
    boolean useArquillianGraphene;

    @Option(names = {"-o", "--output-dir"}, paramLabel = "<dir>", defaultValue = ".",
            description = "Where the project directory is created (default: current directory)")
    Path outputDir;

    @Option(names = {"--generator-url"}, paramLabel = "<url>",
            defaultValue = "https://start.flowlogix.com/sg/download/",
            description = "Starter generator to call (default: ${DEFAULT-VALUE})")
    URI generatorUrl;

    @Option(names = {"--dry-run"}, description = "Validate the options, print the equivalent curl command and exit")
    boolean dryRun;

    public static void main(String[] args) {
        System.exit(new CommandLine(new StarterCli()).execute(args));
    }

    /** Parent POM flavors the archetype accepts. */
    enum BaseType { base, payara, glassfish, infra }

    enum Packaging { jar, war, ear }

    /** Reports the version the build stamps into the jar manifest. */
    static class BuildVersion implements IVersionProvider {
        @Override
        public String[] getVersion() {
            String version = StarterCli.class.getPackage().getImplementationVersion();
            return new String[] {"flowlogix-starter " + Objects.requireNonNullElse(version, "(development build)")};
        }
    }

    @Override
    public Integer call() throws IOException, InterruptedException {
        validate();

        Path projectDir = outputDir.resolve(artifactId).toAbsolutePath().normalize();
        if (Files.exists(projectDir)) {
            spec.commandLine().getErr().printf("Directory '%s' already exists. "
                    + "Choose another --artifact-id or --output-dir.%n", projectDir);
            return 1;
        }

        var generator = new GeneratorClient(generatorUrl);
        URI request = generator.request(parameters());
        if (dryRun) {
            spec.commandLine().getOut().println(GeneratorClient.curlCommand(request, artifactId).stream()
                    .map(StarterCli::quote).collect(Collectors.joining(" ")));
            return 0;
        }

        spec.commandLine().getOut().printf("Generating %s through %s, this takes a few seconds...%n",
                projectDir, generatorUrl.getHost());
        try {
            generator.generate(request, artifactId, projectDir);
        } catch (GeneratorException e) {
            spec.commandLine().getErr().println(e.getMessage());
            // 2 only for a rejection, so that it keeps meaning invalid input
            return e.rejected() ? 2 : 1;
        }
        spec.commandLine().getOut().printf("Created %s%n  cd %s && ./mvnw verify%n", projectDir, projectDir);
        return 0;
    }

    /** The checks the generator does not make; see the class comment. */
    private void validate() {
        if (!MAVEN_ID.matcher(groupId).matches()) {
            throw invalid("--group-id", groupId, "use only letters, digits, '.', '-' and '_'");
        }
        if (!ARTIFACT_ID.matcher(artifactId).matches()) {
            throw invalid("--artifact-id", artifactId, "use 1 to 100 letters, digits, '-' or '_'");
        }
        if (!isBlank(packageName) && !SourceVersion.isName(packageName)) {
            throw invalid("--package", packageName,
                    "not a valid Java package name: each part must be an identifier, not a Java keyword");
        }
        if (isBlank(packageName) && !SourceVersion.isName(groupId)) {
            throw invalid("--group-id", groupId,
                    "the base package defaults to the groupId, which is not a valid Java package name; "
                    + "set one with --package");
        }
        String scheme = generatorUrl.getScheme();
        if (generatorUrl.getHost() == null || !("https".equals(scheme) || "http".equals(scheme))) {
            throw invalid("--generator-url", generatorUrl.toString(), "expected an http or https URL");
        }
    }

    private ParameterException invalid(String option, String value, String reason) {
        return new ParameterException(spec.commandLine(),
                "Invalid value for option '%s': '%s' (%s)".formatted(option, value, reason));
    }

    /** The options the user set, under the generator's parameter names. */
    Map<String, String> parameters() {
        var parameters = new LinkedHashMap<String, String>();
        parameters.put("group", groupId);
        parameters.put("artifact", artifactId);
        // always sent: left out, the generator uses com.example.starter instead of the groupId
        parameters.put("package", isBlank(packageName) ? groupId : packageName);
        parameters.put("projectName", projectName);
        parameters.put("version", projectVersion);
        parameters.put("baseType", baseType.name());
        parameters.put("packagingType", packaging.name());
        parameters.put("archetypeVersion", archetypeVersion);
        parameters.put("useShiro", flag(useShiro));
        parameters.put("useAgenticAI", flag(useAgenticAI));
        parameters.put("useOmniFaces", flag(useOmniFaces));
        parameters.put("usePrimeFaces", flag(usePrimeFaces));
        parameters.put("useLazyModel", flag(useLazyModel));
        parameters.put("useMavenCache", flag(useMavenCache));
        parameters.put("useCodeCoverage", flag(useCodeCoverage));
        parameters.put("useArquillianGraphene", flag(useArquillianGraphene));
        parameters.values().removeIf(StarterCli::isBlank);
        return parameters;
    }

    private static String flag(boolean value) {
        return value ? "true" : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Single-quotes an argument for display, so a printed command can be pasted into a shell. */
    static String quote(String argument) {
        return SHELL_SAFE.matcher(argument).matches() ? argument : "'" + argument.replace("'", "'\\''") + "'";
    }
}
