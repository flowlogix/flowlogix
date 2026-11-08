---
name: flowlogix-new-project
description: Creates new Jakarta EE projects with JStarter, the Jakarta EE Starter by Flow Logix — the start.flowlogix.com generator, the jstarter CLI or the com.flowlogix.archetypes:starter Maven archetype — on Payara, GlassFish or no application server, with options such as Shiro, OmniFaces, PrimeFaces, a JPA lazy data model and Arquillian tests. Use this skill whenever the user wants to scaffold, generate or bootstrap a new Jakarta EE, Payara or GlassFish project, mentions Flow Logix, JStarter, jstarter, start.flowlogix.com or the starter archetype, or describes an app and asks which starter options fit it.
license: Apache-2.0
compatibility: Works with any agent that supports Agent Skills and can run shell commands. Needs curl and unzip with network access to start.flowlogix.com, or Maven with access to Maven Central; a JDK 17+ for the bundled script; and, to build the generated project, a JDK at least the parent POM's maven.compiler.release (25 as of 2026-10).
metadata:
  source: https://github.com/flowlogix/flowlogix/tree/main/skills/flowlogix-new-project
---

# Creating a Flow Logix project

JStarter is the Jakarta EE Starter by Flow Logix. The generator uses the Maven archetype
`com.flowlogix.archetypes:starter`. start.flowlogix.com runs it server-side and returns the project
as a zip. The web page, the `jstarter` CLI and the
download endpoint accept the same parameters, so the same choices produce the same project. The
project builds on a Flow Logix parent POM; changing that build later is the `flowlogix-base-pom`
skill's job.

## Contents
- Settle the choices first
- Parameters
- What each option changes
- Generating: pick a route
- What gets generated
- After generating

## Settle the choices first

Regenerating means deleting the directory, so agree on the coordinates (`group`, `artifact`,
`package`), the parent POM (`baseType`), `packagingType` and the `use*` feature flags before
generating.

If the user gave only a name, leave the rest out and say which defaults the project got, read back
from the generated `pom.xml` (parent, packaging, dependencies) or from the CLI's `--dry-run`, which
prints every value it sends. Defaults change between releases, so don't quote them from memory.

If they described the app, map that to options: a REST or backend-only service usually wants `jar`
or `war` without PrimeFaces, lazy model or Graphene; a server-rendered admin app wants `war` with
PrimeFaces and the lazy model; anything without an application server wants `base` or `infra`.

## Parameters

Generator-level parameters (not archetype properties):

| Parameter | Maps to | Rule |
|---|---|---|
| `group` | `groupId` | letters, digits, `.`, `-`, `_` |
| `artifact` | `artifactId`, project directory | `[a-zA-Z0-9_-]+`, 1–100 chars |
| `package` | base Java package | valid Java package name; always send it (the CLI uses the group), since a left-out package becomes `com.example.starter` |
| `archetypeVersion` | archetype version | the latest release when left out |

Archetype properties:

| Property | Values |
|---|---|
| `baseType` | `base`, `infra`, `payara`, `glassfish` |
| `packagingType` | `jar`, `war`, `ear` |
| `projectName` | free text |
| `version` | the project's version |
| `useShiro`, `useAgenticAI`, `useOmniFaces`, `usePrimeFaces`, `useLazyModel`, `useMavenCache`, `useCodeCoverage`, `useArquillianGraphene` | `true`, `false` |

Leave out what the user didn't ask about: the generator fills it with its own defaults, which
differ on purpose from the archetype's for some properties (the web page pre-selects a
full-featured app). Read the defaults from the source when you need them, which is for Route 3 (a
bare `mvn archetype:generate` uses the archetype's defaults, so pass every property) and to tell the
user what was chosen:

- installed CLI: `jstarter --help` shows each option's default, `--dry-run` every value sent;
- generator: `DEFAULTS` in
  https://raw.githubusercontent.com/flowlogix/apps/main/starter-generator/common/src/main/java/com/flowlogix/starter/common/StarterParameters.java;
  a property it doesn't list takes the archetype's default;
- archetype: https://raw.githubusercontent.com/flowlogix/archetype/main/src/main/resources/META-INF/maven/archetype-metadata.xml;
- after generating, the project's `pom.xml` shows what was used.

The archetype also has version properties (`basePomVersion`, `flowlogixBomVersion`,
`depchainVersion`, `flowlogixVersion`, `shiroVersion`, `jakartaAgenticAIVersion`,
`arqGrapheneVersion`, `cacheExtensionVersion`, `mavenVersion`, `mavenWrapperVersion`). Their
defaults are the versions the archetype release was built with; leave them alone unless the user
asks to pin something.

## What each option changes

- **baseType** — the parent POM (`com.flowlogix:<baseType>-pom`). `base`: minimal, any project.
  `infra`: adds tests, coverage, checkstyle, release config. `payara` / `glassfish`: `infra` plus a
  managed application server for integration tests and `server:dev`.
- **packagingType** — `war` keeps `src/main/webapp` (Faces pages, `web.xml`, error page); `jar`
  and `ear` delete it. Integration-test deployments switch between `JavaArchive`, `WebArchive` and
  `EnterpriseArchive` to match.
- **useShiro** — Apache Shiro (`shiro-jakarta-ee`) and `WEB-INF/shiro.ini`.
- **useOmniFaces** — OmniFaces, and the Flow Logix library (`flowlogix-jee`, or
  `flowlogix-datamodel` with the lazy model) at compile scope; without it that library is test
  scope only.
- **usePrimeFaces** — PrimeFaces (`annotated` classifier).
- **useLazyModel** — `flowlogix-datamodel` (JPA lazy data model for PrimeFaces DataTable) instead
  of `flowlogix-jee`. Only meaningful with PrimeFaces.
- **useAgenticAI** — the Jakarta Agentic AI API (`jakarta.agentic-ai-api`, provided scope).
- **useMavenCache** — Maven build cache extension in `.mvn/extensions.xml`; when off,
  `maven.build.cache.enabled=false` is written into the POM.
- **useCodeCoverage** — JaCoCo `<includes>/<excludes>` for the project's package, with unit and
  integration coverage merged.
- **useArquillianGraphene** — Selenium / Drone / Graphene for UI integration tests.

## Generating: pick a route

Take the first route that works here. If start.flowlogix.com or Maven Central is unreachable from
where you are running, say so and give the user the exact command to run themselves (the CLI's
`--dry-run` or `StarterUrl.java` prints it).

### Route 1: the CLI

`jstarter` on PATH (or its `quarkus-run.jar`) validates input, refuses to overwrite an
existing directory, unpacks and makes `mvnw` executable. Read `references/cli.md` for its options,
examples and exit codes.

### Route 2: the download endpoint (curl)

Needs only `curl` and `unzip`; Maven runs server-side. The endpoint takes parameters as **matrix
parameters** on the path (`;name=value`), not a query string:

```
https://start.flowlogix.com/sg/api/generate/;group=com.acme;artifact=orders;package=com.acme.orders;packagingType=jar
```

Build it with the bundled script, which runs from source with no build, percent-encodes values
(e.g. spaces in `projectName`) the same way the CLI does and prints a ready curl command:

```shell
java scripts/StarterUrl.java group=com.acme artifact=orders package=com.acme.orders \
  packagingType=jar usePrimeFaces=false useLazyModel=false useArquillianGraphene=false
```

Without `package`, the script adds the group as the package (as the CLI does); `--url-only` prints
just the URL. Run the printed curl, then unpack — the zip contains a top-level directory named
after the artifact:

```shell
unzip -q orders.zip && rm orders.zip && chmod +x orders/mvnw
```

`unzip` does not restore the executable bit on `mvnw`, hence the `chmod`. On error the endpoint
answers `400` with a plain-text reason (bad value) or `500` with Maven's output.

The shortest forms in the project READMEs also work:
`curl -sSL https://start.flowlogix.com/generate -o starter.zip`.

### Route 3: mvn archetype:generate

Needs Maven and Maven Central, not start.flowlogix.com. Pass every property, since the archetype's
defaults differ from the generator's:

```shell
mvn archetype:generate -DarchetypeGroupId=com.flowlogix.archetypes -DarchetypeArtifactId=starter \
  -DarchetypeVersion=LATEST -DinteractiveMode=false \
  -DgroupId=com.acme -DartifactId=orders -Dpackage=com.acme.orders \
  -DbaseType=payara -DpackagingType=war -DprojectName="Orders" \
  -DuseShiro=true -DuseOmniFaces=true -DusePrimeFaces=true -DuseLazyModel=true -DuseAgenticAI=false
```

Omitting `-DinteractiveMode=false` makes Maven prompt for every property, which hangs a
non-interactive shell.

## What gets generated

```
<artifact>/
├── pom.xml                  parent com.flowlogix:<baseType>-pom, Flow Logix BOM, depchain integration-test
├── mvnw, mvnw.cmd, .mvn/    Maven wrapper, build-cache extension, settings
├── lombok.config
├── README.adoc              how to run, test and regenerate
├── .github/                 dependabot + auto-merge workflow (pinned to a commit)
├── .jenkins_payara
└── src/
    ├── main/resources/META-INF/   beans.xml, persistence.xml
    ├── main/webapp/               (war only) index.xhtml, web.xml, faces-config.xml, error page, shiro.ini
    ├── test/java/<package>/StarterTest.java       unit test (JUnit 5, AssertJ, Mockito)
    ├── test/java/<package>/its/StarterIT.java     Arquillian integration test (@AppServerLifecycle)
    ├── test/resources/            arquillian.xml, test persistence.xml
    └── checkstyle/                Apache license header + suppressions
```

Every Java file gets the Apache license header from `src/checkstyle/apache-header.txt`;
checkstyle enforces it, so new Java files need it too. Lombok is on the classpath with a generated
`lombok.config`; prefer it over handwritten boilerplate, matching the generated code's style.

## After generating

Verify instead of assuming:

```shell
cd <artifact>
./mvnw verify                 # unit + integration tests
./mvnw package server:dev     # payara/glassfish: run with live reload
```

Integration tests for `payara`/`glassfish` start a real server, so the first `verify` downloads it
and takes a few minutes. If the enforcer rejects the JDK, the project needs a newer one;
`./mvnw -q help:evaluate -Dexpression=maven.compiler.release -DforceStdout` shows which.

`server:dev` is wired in by the server parent POMs (`server-common`, which declares
`com.flowlogix.plugins:container-deploy-plugin`), so expect it to work only with the `payara` and
`glassfish` base types; for `base` / `infra` run the app however the project deploys it.

## Sources

Code: github.com/flowlogix/archetype (archetype), github.com/flowlogix/apps → `starter-generator/`
(web generator + CLI). When a detail here disagrees with those sources, the sources win.
