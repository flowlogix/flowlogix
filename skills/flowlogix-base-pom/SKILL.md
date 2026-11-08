---
name: flowlogix-base-pom
description: Configures, upgrades and troubleshoots Maven builds on the Flow Logix parent POMs (com.flowlogix base-pom, infra-pom, payara-pom, glassfish-pom) and com.flowlogix.depchain dependency chains — overriding properties, activating profiles such as server-local, server-docker and coverage, JaCoCo coverage, Arquillian and Testcontainers integration tests, server:dev, updating versions, and adopting a Flow Logix parent in an existing project. Use this skill whenever a pom.xml has a com.flowlogix parent, or the user mentions base-pom, infra-pom, payara-pom, glassfish-pom, depchain or server:dev, even if they don't say "Flow Logix".
license: Apache-2.0
compatibility: Works with any agent that supports Agent Skills and can run shell commands. Needs the project's Maven wrapper or Maven, network access to Maven Central, and a JDK at least the parent POM's maven.compiler.release (25 as of 2026-10).
metadata:
  source: https://github.com/flowlogix/flowlogix/tree/main/skills/flowlogix-base-pom
---

# Flow Logix parent POMs

A Flow Logix parent POM carries the build (compiler, tests, coverage, checkstyle, server
lifecycle), so the project's own `pom.xml` stays as small as possible; anything not needed can be
overridden or opted out.

## Contents
- Which situation is this?
- Changing the build, in order of preference
- Choosing a parent
- Minimal POM and the parent version
- Properties worth knowing
- Profiles
- Integration tests and application servers
- Code coverage (JaCoCo)
- Dependency chains (depchain) and the BOM
- Adopting the parent POM in an existing project
- Updating versions
- Things that trip people up

## Which situation is this?

- **`pom.xml` has `<parent><groupId>com.flowlogix</groupId>`** → already a Flow Logix project.
  Change it by overriding properties or activating profiles, not by copying plugin configuration in.
- **Existing project without a Flow Logix parent** → adopting one is a real migration: the parent
  brings its own plugin versions, checkstyle and enforcer rules (JDK and Maven minimums). Confirm
  with the user first, then follow "Adopting the parent POM".
- **No project yet** → generating one gives the parent ready-made; that's the
  `flowlogix-new-project` skill.

## Changing the build, in order of preference

1. **Override a property** (`<maven.compiler.release>`, `<test.redirect.to.file>`,
   `<maven.build.cache.enabled>`, versions like `<payara.version>`).
2. **Activate a profile** (`-Pserver-docker`, `-Pcoverage`, `-Dcheckstyle.skip`).
3. **Configure a plugin the parent already declares**, only with the settings you change (e.g.
   JaCoCo `<includes>`), without repeating its version.
4. Only then add new plugins or dependencies. Versions of common dependencies come from the parent
   and the Flow Logix BOM, so omit `<version>` when the parent manages it.

## Choosing a parent

```
base-pom ─► infra-pom ─► server-common ─┬─► payara-pom
                                        └─► glassfish-pom
```

| Parent | Use when | Adds |
|---|---|---|
| `base-pom` | any project, minimal; also as the parent of your own parent POM | compiler, sane plugin versions; no Maven 4 warnings |
| `infra-pom` | opinionated build without an app server | Surefire/Failsafe (parallel), JaCoCo, checkstyle, enforcer (JDK/Maven minimum), Mockito agent, `src/demo` sources, release/deploy profiles, Flow Logix BOM |
| `payara-pom` / `glassfish-pom` | Jakarta EE apps tested against a real server | `infra` + server download/start/stop for ITs, Arquillian container, EclipseLink metamodel processor, `server:dev` |

When unsure, the Flow Logix FAQ says start with `base`. For a Jakarta EE app with integration
tests, `payara` or `glassfish` saves the most configuration.

## Minimal POM and the parent version

```xml
<parent>
    <groupId>com.flowlogix</groupId>
    <artifactId>payara-pom</artifactId>
    <version>N</version> <!-- the latest release; LATEST does not work for a parent -->
</parent>
```

Versions are plain integers shared by all modules of base-pom, released about weekly, so find the
current one instead of copying a number:

- a generated project already has it;
- an existing Flow Logix project: `./mvnw versions:update-parent` (see "Updating versions");
- adopting the parent: ask Maven Central for the latest release,
  `curl -s https://repo.maven.apache.org/maven2/com/flowlogix/base-pom/maven-metadata.xml | grep -o '<release>[^<]*'`.

## Properties worth knowing

Override these in `<properties>` instead of reconfiguring plugins. Their defaults move with the
parent and a project may already override them, so ask the build for the value in effect:
`./mvnw -q help:evaluate -Dexpression=<property> -DforceStdout`.

| Property | Meaning |
|---|---|
| `maven.compiler.release` | target Java release; also the enforced minimum JDK (`jdk.minimum.version`) |
| `maven.minimum.version` | enforced minimum Maven; profile `maven4-required` raises it to Maven 4 |
| `jakarta.ee.version`, `lombok.version`, `mockito.version` | managed versions |
| `test.redirect.to.file` | test output to files; the starter sets `false` to see it on the console |
| `junit5.parallel.tests.enabled`, `test.parallel`, `it.test.parallel`, `test.thread.count` | test parallelism |
| `checkstyle.configLocation`, `checkstyle.headerLocation`, `checkstyle.suppressionsLocation`, `checkstyle.excludes` | checkstyle inputs |
| `maven.build.cache.enabled` | Maven build cache extension switch (when the project uses it) |
| `arquillian.container`, `drone.browser` | IT container and browser |
| `jacocoAddress`, `jacocoPort`, `jacocoReset` | JaCoCo agent for coverage from a running server |
| `javadoc.doclint`, `japicmp.skip`, `jar.skip-if-empty`, `deploy.at-end` | misc build toggles |
| `payara.version` / `glassfish.version` | server version (server POMs) |
| `server.http.port`, `server.https.port` | server ports used by ITs (server POMs) |
| `server.start.skip`, `testcontainers.skip` | server lifecycle for ITs |

The complete tables are on https://docs.flowlogix.com/base-pom#section-variables.

## Profiles

| Profile / switch | Effect |
|---|---|
| *(default, server POMs)* | download the server, start a domain, run ITs, stop it |
| `-Pserver-local` | use an already-running local server (no download/start) |
| `-Pserver-remote` | use a remote server (HTTPS port 443) |
| `-Pserver-docker` | run the server in Docker via Testcontainers |
| `-Pcoverage` | JaCoCo agent on the tests, and `jacoco:merge` set to collect `**/*.exec`; merge and report run as explicit goals (below) |
| `-Dcheckstyle.skip` | activates `skip-checkstyle` |
| `-Dflowlogix.bom.skip` | don't import the Flow Logix BOM (`include-flowlogix-bom` is on by default) |
| `-Pmaven4-required` | require Maven 4 |
| `-Dskinny.war` | activates `skinny-war`: WAR without `WEB-INF/lib` jars (for EARs) |
| `-Pdocs`, `-Prelease-plugins`, release profiles | publishing; rarely needed in user projects |

`-DskipITs` skips integration tests and, in the server POMs, also skips starting the server.

## Integration tests and application servers

- Unit tests: `*Test.java`, run by Surefire in `test`.
- Integration tests: `*IT.java`, run by Failsafe in `verify`, not `test`; the starter puts them in
  `<package>.its`. They use Arquillian with `@ArquillianSuiteDeployment` (one deployment for the
  whole suite) and Flow Logix's `@AppServerLifecycle`, building the archive with
  `ShrinkWrapManipulator.createDeployment(WebArchive.class)` (or `JavaArchive`/`EnterpriseArchive`).
- First `verify` with a server POM downloads the server into `target/dependency`, so it is slow
  once and quick after.

## Code coverage (JaCoCo)

Unit and integration coverage (including code running inside the app server) are merged into one
report. The project only needs to say which packages are its own:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <configuration>
        <includes><include>com/acme/**</include></includes>
        <excludes>
          <exclude>com/acme/*Test</exclude><exclude>com/acme/*Test$*</exclude>
          <exclude>com/acme/*IT</exclude><exclude>com/acme/*IT$*</exclude>
        </excludes>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Run locally against Payara/GlassFish:

```shell
curl -sSL https://raw.githubusercontent.com/flowlogix/base-pom/main/scripts/jacoco-init.sh | bash -p   # once
./mvnw verify -Dmaven.build.cache.enabled=false -Pserver-local,coverage
./mvnw -N -Pcoverage initialize jacoco:dump jacoco:merge jacoco:report -DjacocoPort=14948
```

(Older docs write `-Ppayara-server-local`; the profile in current base-pom is `server-local`,
shared by Payara and GlassFish.)

Piping a remote script into bash changes the user's server setup, so show the command and let the
user decide rather than running it unasked. The script also needs `groovy` on the PATH.

## Dependency chains (depchain) and the BOM

`com.flowlogix.depchain` artifacts are POM-packaged dependency bundles, used with
`<type>pom</type>` or as plain dependencies:

| Artifact | Brings |
|---|---|
| `unit-test` | JUnit 5, AssertJ, Mockito (JUnit integration), rerunner-jupiter |
| `integration-test` | `unit-test` + Arquillian (JUnit 5 container, servlet protocol), Testcontainers, Payara Testcontainers support |
| `integration-test-ng` | TestNG flavor |
| `shiro-jakarta` | Apache Shiro (core, web, jakarta-ee, cdi, jaxrs; `jakarta` classifier), commons-configuration2, OmniFaces |

The starter declares `integration-test` at test scope and imports `com.flowlogix:flowlogix-bom`
in `<dependencyManagement>`, so OmniFaces, PrimeFaces, commons-configuration2 and similar need no
`<version>`. Flow Logix intentionally manages versions in the parent rather than stacking BOMs
(Maven 4 warns about conflicting BOMs); add other BOMs only when needed.

## Adopting the parent POM in an existing project

1. Confirm with the user: the parent enforces a JDK/Maven minimum and checkstyle (with an Apache
   license header check) and may change plugin versions.
2. Add the `<parent>` at the latest release (see "Minimal POM and the parent version") and remove
   from the project what the parent now provides: plugin versions and configuration for compiler,
   Surefire, Failsafe, JaCoCo, enforcer, source/javadoc; version properties the parent defines;
   dependency versions the parent/BOM manages.
3. Keep what is genuinely project-specific (name, SCM, developers, extra plugins).
4. Build with `./mvnw verify`. Typical first failures: checkstyle (fix, or set
   `checkstyle.suppressionsLocation`; `-Dcheckstyle.skip` only as a temporary measure) and the
   enforcer (JDK too old → lower `maven.compiler.release` or upgrade the JDK).
5. If the project already has its own corporate parent, make `base-pom` the parent of *that*
   parent instead.

## Updating versions

Use the Flow Logix versions ruleset rather than a bare `versions:use-latest-releases`; it filters
out alpha, beta, milestone, RC and dated snapshot-like versions:

```shell
# dependencies, properties and the parent (plugins come from the parent)
./mvnw -U -ntp versions:use-latest-releases versions:update-parent versions:update-properties \
  -DgenerateBackupPoms=false \
  -Dmaven.version.rules=https://raw.githubusercontent.com/flowlogix/base-pom/main/versions-ruleset.xml

# see whether plugins declared in the project are outdated
./mvnw -U -ntp versions:display-plugin-updates -DgenerateBackupPoms=false -DprocessUnboundPlugins=true
```

For GitHub repositories, base-pom also ships a reusable Dependabot auto-merge workflow and setup
scripts (`scripts/create-automerge-token.sh`, `scripts/setup-repository.sh`); the starter already
includes the workflow. They create tokens, secrets and webhooks, so walk the user through them
rather than running them.

## Things that trip people up

- The parent's `<version>` must be a concrete release: Maven does not resolve `LATEST` for parents.
- `infra-pom` and above enforce a minimum JDK (`maven.compiler.release`) and Maven version; an
  enforcer failure is usually an old JDK, not a bug. Check the value in effect with
  `./mvnw -q help:evaluate -Dexpression=maven.compiler.release -DforceStdout`, and lower it in the
  project if the user must stay on an older JDK.
- With the Maven build cache on, a "nothing happened" build may be a cache hit; add
  `-Dmaven.build.cache.enabled=false` when measuring coverage or debugging the build.

## Sources

Docs: https://docs.flowlogix.com/base-pom, https://docs.flowlogix.com/depchains.
Code: github.com/flowlogix/base-pom, github.com/flowlogix/depchain. When a detail here disagrees
with those sources, the sources win.
