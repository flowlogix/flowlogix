# CLI: flowlogix-starter

A command line client of start.flowlogix.com (Quarkus + picocli). It sends the options to the
generator, so the generator alone decides which values are valid and the result matches the web
page. Source: github.com/flowlogix/apps → `starter-generator/cli` (artifact `starter-cli`).

## Getting it

Check first: `command -v flowlogix-starter`. If it isn't installed, it can be built from source
(needs a JDK and network access to GitHub and Maven Central):

```shell
git clone --depth 1 https://github.com/flowlogix/apps.git
cd apps && ./mvnw -q -pl starter-generator/cli -am package -DskipTests
java -jar starter-generator/cli/target/quarkus-app/quarkus-run.jar --help
```

Building it just to generate one project is rarely worth it; Route 2 (curl) in `SKILL.md` gives
the same project. Prefer the CLI when it is already installed, or when the user asks for it.

## Usage

```
flowlogix-starter [options]
```

| Option | Meaning |
|---|---|
| `-g, --group <id>` | Maven groupId |
| `-a, --artifact <id>` | artifactId and project directory name |
| `-p, --package <package>` | base Java package; the group when left out |
| `--archetypeVersion <version>` | archetype version |
| `--baseType`, `--packagingType`, `--projectName <value>` | archetype properties, one option each |
| `--projectVersion <value>` | the archetype's `version` (renamed: `--version` prints the CLI's version) |
| `--useShiro[=<true\|false>]` and the other `use*` flags | bare flag means `true` |
| `-o, --output-dir <dir>` | where the project directory is created; `.` by default |
| `--generator-url <url>` | generator to call; `https://start.flowlogix.com/sg/download/` by default |
| `--dry-run` | print the equivalent curl command and exit |
| `-h, --help`, `-V, --version` | |

Options left out take the generator's defaults ("Parameters" in `SKILL.md` says where they come
from). There is one option per archetype property, generated from the archetype's descriptor, so
`--help` on the installed version is the authoritative list, defaults included.

A bare boolean flag means `true`, so turning a feature **off** always needs an explicit value:
`--usePrimeFaces=false`.

## Examples

```shell
# All defaults, in ./orders
flowlogix-starter -g com.acme -a orders

# Backend jar on the minimal infra parent, no UI libraries, into ~/src
flowlogix-starter -g com.acme -a billing -p com.acme.billing \
  --baseType=infra --packagingType=jar \
  --usePrimeFaces=false --useLazyModel=false --useArquillianGraphene=false \
  -o ~/src

# Show the request without calling the generator (useful when the generator can't be reached from here)
flowlogix-starter -g com.acme -a orders --dry-run
```

On success it prints the created path and `cd <dir> && ./mvnw verify`; run that to confirm.

## Exit codes

| Code | Meaning | What to do |
|---|---|---|
| 0 | project created (or curl printed with `--dry-run`) | |
| 1 | target directory exists, generator failed, or generator unreachable | pick another `--artifact`/`--output-dir` (don't delete the user's directory without asking); on a network error fall back to `--dry-run` and let the user run it |
| 2 | invalid input, rejected by the CLI or the generator | read the message, fix the value — the generator's validation rules are under "Parameters" in `SKILL.md` |
