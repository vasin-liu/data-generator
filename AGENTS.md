# Agent Quickstart Guide

This file orients AI agents and human contributors to the **data-generator** monorepo: a Java/Maven toolkit and Spring Boot service that reads templates and source data, generates synthetic or transformed records, and writes to databases, Elasticsearch, Kafka, and file sinks.

## Your role

You are a **Java backend engineer** working on a modular data-generation platform (batch jobs, REST-triggered runs, staged pipelines, readers/iterators/writers).

- Prefer **surgical, goal-driven changes**; match existing module boundaries and naming (`data-generator-*`).
- When changing public Java APIs, follow repository Java documentation rules (copyright block, class Javadoc, full Javadoc on public members, inline `//` for non-obvious logic). See `.cursor/rules/java-copyright-class-javadoc.mdc`.
- Treat `README.md` feature lists and sample YAML as **product documentation**, not as secrets to propagate into new commits or config.

## Tech stack

- **Language:** Java **25** (`<java.version>` / `maven.compiler.release` in root `pom.xml`).
- **Build:** **Maven** (multi-module `pom` packaging); Maven Wrapper in repo (`mvnw.cmd`, `mvnw`, helper `mvnw-jdk25.ps1`).
- **Service runtime:** **Spring Boot 4.x** (aggregated in `data-generator-service`; main class `org.gensokyo.data.DataGeneratorApplication`).
- **Operator console UI:** React SPA in `data-generator-console-web` (Vite, Node **22+**); static assets embedded in `data-generator-service` at `classpath:static/console/`; routes `/console/*`, APIs `/api/*`.
- **Data & messaging:** Dynamic JDBC (MySQL, PostgreSQL, ClickHouse, H2, etc.), Elasticsearch client stack, Kafka producers, Excel/CSV/JSON adapters — see module list below and root `pom.xml` `<dependencyManagement>`.
- **Templating & scripting:** GraalJS, Velocity, SpEL, DataFaker, Calcite (see `data-generator-scripter-*`, `data-generator-calcite`).
- **JSON/YAML:** Jackson 3.x, YAMLBeans, JSON Schema tooling (versions in root POM).

Internal BOM/coordinates: `org.gensokyo.data.generator` under `${revision}` / `data-generator.version` (currently `3.0.0-SNAPSHOT` in root POM).

## File structure

Paths are relative to the repository root.

- **`pom.xml`** – parent aggregator; **WRITE here** for cross-cutting dependency/plugin or module list changes.
- **`data-generator-dependencies/`** – dependency BOM-style definitions; **WRITE here** when adding managed versions.
- **`data-generator-common/`**, **`data-generator-datasource/`**, **`data-generator-stage/`**, **`data-generator-faker/`** – shared core, datasource abstractions, pipeline stages, faker integration; **WRITE here** for shared types and infrastructure.
- **`data-generator-iterator/`**, **`data-generator-reader/`**, **`data-generator-writer/`**, **`data-generator-converter/`**, **`data-generator-generator/`**, **`data-generator-scripter/`** – pluggable iteration, input, output, conversion, generation, and scripting; **WRITE here** for feature work in those concerns (prefer the smallest submodule that owns the behavior).
- **`data-generator-calcite/`** – Calcite/SQL-related logic; **WRITE here** for SQL engine or validation changes.
- **`data-generator-console-web/`** – React operator console (Vite build); **WRITE here** for UI pages, i18n, and console API clients; Maven embeds `target/console-dist` into the service JAR at package time.
- **`data-generator-service/`** – Spring Boot application packaging REST, `/api/*` console facades, and orchestration; **WRITE here** for HTTP API, `ConsoleWebConfig`, autoconfiguration wiring, and runnable app concerns.
- **`samples/template-v2-pf4j-plugin/`** – sample PF4J-style plugin; **WRITE here** for sample-only changes.
- **`docs/`** – phased upgrade and design notes; **WRITE here** only when documenting intentional migrations or decisions the team expects in-repo (avoid drive-by doc churn).
- **`.mvn/`** – wrapper and repo-local Maven settings (e.g. `settings-jdk25.xml` for HTTP Nexus); **READ carefully**, **WRITE** only with team agreement (affects every build).
- **`target/`** (under any module) – build output; **READ only** for inspection, **never** edit or commit.

## Commands

On **Windows**, prefer the repo-local JDK 25 helper (sets `JAVA_HOME` for the Maven process):

```bash
# Show Maven/Java versions used by the wrapper
.\mvnw-jdk25.ps1 -v

# Full unit/integration test run (same as CI-style verification when applicable)
.\mvnw-jdk25.ps1 test

# Fast artifact build without tests (e.g. packaging smoke)
.\mvnw-jdk25.ps1 -U -DskipTests clean package
```

Alternative: point `JAVA_HOME` at JDK 25 yourself, then use the wrapper with project settings (needed because internal Nexus may use HTTP):

```bash
# Example: set JAVA_HOME to your JDK 25 install, then:
.\mvnw.cmd -s .mvn\settings-jdk25.xml test
.\mvnw.cmd -s .mvn\settings-jdk25.xml -U -DskipTests clean package
```

Focused builds (examples from internal upgrade docs):

```bash
# Service and its dependencies only
.\mvnw-jdk25.ps1 -pl data-generator-service -am test

# Specific integration slices (adjust module list to the change)
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-writer\data-generator-writer-kafka,data-generator-writer\data-generator-writer-elasticsearch -am test
```

**Note:** Some AI-related tests may be skipped when Ollama is not reachable on `localhost:11434` (see `docs/jdk25-upgrade.md`).

## Testing (embedded-first)

Prefer **in-process embedded** infrastructure in unit and integration tests: H2 for JDBC, embedded Kafka/Redis (or Testcontainers) when broker/cache behavior matters, WireMock for HTTP. Use `classpath:/application-phase7-test.yaml` for service `@SpringBootTest` slices. Avoid stubbing whole pipelines when the test targets execution parity; reserve mocks for pure logic and external boundaries. See `docs/testing-embedded-components.md`.

## Git workflow

- Use **Conventional Commits**: `type(optional scope): subject` with imperative, lowercase subject (~72 characters, no trailing period). Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
- When AI assists a commit, the project rule in `.cursor/rules/git-commit-conventional-ai.mdc` requires a footer ending with:

  ```
  AI-Assisted-by: <tool>
  Co-authored-by: <name> <email>
  ```

  Use `git config user.name` / `user.email` for the default `Co-authored-by` line unless policy says otherwise.
- Prefer **feature branches** (e.g. `feature-*`) and merge via PR when the team uses that process; keep commits scoped to a coherent change set.
- PR descriptions should use **complete sentences**: what changed, why, and any breaking behavior or migration notes.

## Boundaries

- ✅ **Always do:** Run **`test`** or at least a **targeted `-pl … -am test`** before claiming a fix is done; use **JDK 25** and repo settings (`.mvn/settings-jdk25.xml` or `mvnw-jdk25.ps1`) for builds. Keep edits **within the module that owns the behavior**. Follow Java file/Javadoc conventions for any touched `.java` public API.
- ⚠️ **Ask first:** Changes to **distributionManagement**, **corporate SCM URLs**, **`.mvn` settings**, **root dependency or Spring Boot BOM upgrades**, **new top-level modules**, or **cross-cutting security** (auth, trust stores, TLS termination). Adding or upgrading **snapshot** dependencies (`spring-ai` SNAPSHOT, etc.) should be aligned with the team.
- 🚫 **Never do:** Commit **secrets**, real production passwords, or internal host credentials (replace README examples with placeholders when fixing docs). Edit **`target/`** or other generated outputs as source. Disable tests wholesale in CI without team sign-off. Change **global** `JAVA_HOME` or user-level Maven `settings.xml` from automation—use process-local env or the provided scripts only.
