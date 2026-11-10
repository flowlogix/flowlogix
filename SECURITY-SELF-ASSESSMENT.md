# Flow Logix Security Self-Assessment

This is a self-assessment of the Flow Logix libraries, following the
[OpenSSF security self-assessment template](https://security-assessments.openssf.org/). It describes the project as maintained by its
maintainers and is not an independent audit.

## Metadata

| | |
| --- | --- |
| Software | Flow Logix (`com.flowlogix:flowlogix-jee`, `flowlogix-datamodel`, `flowlogix-bom`) |
| Repository | <https://github.com/flowlogix/flowlogix> |
| Security contact | <security@flowlogix.com>, [policy](SECURITY.adoc) |
| License | Apache-2.0 |
| Last reviewed | 2026-11-09 |

## Overview

Flow Logix is a set of Java libraries that fill gaps in Jakarta EE, PrimeFaces and OmniFaces. They provide JPA data access helpers, a
lazy data model for PrimeFaces data tables, resource handlers for Jakarta Faces, type conversion utilities and test helpers for
ShrinkWrap and Arquillian. The libraries run inside the runtime of the application that embeds them.

### Actors

- **Application developers** add the libraries as Maven dependencies and are responsible for the security of their own applications.
- **Application end users** interact only with the embedding application, never with the libraries directly.
- **Maintainers** build and publish releases to Maven Central.

### Actions

- Build JPA queries, including native SQL queries, from developer-supplied criteria and parameters.
- Serve and transform Jakarta Faces resources such as minified JavaScript and CSS.
- Convert strings to typed values and test objects for serializability.

### Goals

- Never turn untrusted input into executable queries when the documented parameter-binding APIs are used.
- Not weaken the security of the underlying Jakarta EE, JPA and Faces specifications.
- Publish dependencies that are current and free of known vulnerabilities.

### Non-goals

- The libraries provide no authentication, authorization or session management. Applications use the platform or Apache Shiro for that.
- They do not sanitize input that developers concatenate into queries themselves.
- Developer-only helpers, such as the test helpers, are not hardened for use in production.

## Self-assessment use

This document gives adopters and reviewers a starting point for evaluating the project's security. It does not replace their own
review of how they use the libraries.

## Security functions and features

- Query APIs bind parameters, instead of requiring string concatenation.
- The libraries do not store credentials or secrets.
- Resource handling uses the standard Jakarta Faces resource mechanisms.

## Project compliance

The project is licensed under Apache-2.0. It follows the
[OpenSSF Best Practices](https://www.bestpractices.dev/projects/8431) criteria and publishes
[OpenSSF Scorecard](https://deps.dev/maven/com.flowlogix:flowlogix) results.

## Secure development practices

- All changes are made through pull requests, which the maintainers review ([CONTRIBUTING](CONTRIBUTING.adoc)).
- The [Flow Logix Jenkins](https://jenkins.flowlogix.com/job/flowlogix-ee-integration/) CI server runs the build and the Arquillian
  integration tests on every change, and also builds and publishes releases.
- CodeQL static analysis runs on pushes, pull requests and weekly.
- SonarCloud analysis is run on the project.
- Dependabot opens daily pull requests for dependency updates, per the [dependency policy](CONTRIBUTING.adoc#dependency-policy).
- Releases are published to Maven Central and have reproducible builds
  ([Reproducible Central](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/com/flowlogix/README.md)).

## Security issue resolution

Vulnerabilities are reported privately by email or GitHub private vulnerability reporting, as described in
[SECURITY.adoc](SECURITY.adoc). Reports are acknowledged within 48 hours, and fixes are aimed for within 30 days of confirmation.
Fixes are released as new versions on Maven Central, and the reporter is credited if they wish.

## Threats and mitigations

| Threat | Mitigation |
| --- | --- |
| Injection through queries built from untrusted input | Parameter-binding APIs. Documentation tells developers not to concatenate input |
| Vulnerable third-party dependencies | Dependabot updates and a dependency policy that requires current versions |
| Compromised release | Releases are built by maintainers from reviewed code and are reproducible |
| Insecure code introduced by a contribution | Maintainer review, CodeQL and SonarCloud analysis, and automated tests |
