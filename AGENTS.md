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
# Operator console: unit + frontend build + Podman Playwright UI/E2E (fixed pipeline)
.\scripts\verify-console.ps1

# One-click full package (console SPA embedded + service assembly tar.gz/zip)
.\scripts\package-full.ps1 -SkipTests

# Package, build Podman image, and start local container (profile e2e)
.\scripts\run-podman-local.ps1 -SkipTests -KeepContainer

# Test harness: matrix-linked Maven slice + coverage summary (see docs/test-harness.md)
.\scripts\verify-harness.ps1

# Pack 3 execution reliability (Maven slice + optional Playwright against running console)
.\scripts\verify-execution-reliability.ps1 -SkipPlaywright

# Phase 7 datasource governance & hot-reload UAT (Maven IT slice + optional Podman Playwright)
.\scripts\verify-phase7-uat-datasource-governance.ps1 -SkipPlaywright
.\scripts\verify-phase7-uat-hot-reload.ps1 -SkipPlaywright

# Phase 8 RW streaming CSV/JSON + JDBC upsert UAT (Maven IT slice + optional Podman Playwright)
.\scripts\verify-phase8-uat-rw-streaming-upsert.ps1 -SkipPlaywright

# Phase 9 JDBC dialect expansion UAT (Maven dialect slice + optional Podman Playwright)
.\scripts\verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright

# Phase 11 v2.0 closeout hardening UAT (managed JDBC E2E IT + Kingbase dialect evidence pack; optional Podman Playwright)
.\scripts\verify-phase11-uat-closeout-hardening.ps1 -SkipPlaywright

# Phase 13 opt-in Dameng live IT (see docs/template-v2-jdbc-sink-guide.md Dameng live IT section for DG_DM_* setup)
.\scripts\verify-phase13-uat-dameng-live.ps1

# JDBC resolver ownership + call-site inventory (catalog-side vs V2 execute-path; see docs/jdbc-resolver-ownership.md)
# (no runnable script — documentation only)

# Multi-JVM worker E2E — host coordinator + worker, shared file H2 (DIST-01; see docs/staging-distributed-deployment.md#dist-01-local-verify-host-two-jvm)
.\scripts\verify-multi-jvm-worker.ps1

# AI P1 catalog + INLINE scenario (Maven slice + optional Playwright)
.\scripts\verify-ai-p1.ps1

# AI P2 cost tracing + run report AI metrics (Maven slice + optional live Ollama)
.\scripts\verify-ai-p2.ps1 -SkipLive

# AI P3 composite bridge + OpenAI-compatible provider
.\scripts\verify-ai-p3.ps1 -SkipPlaywright

# AI P4 apiKeySecretRef + platform AI usage rollup
.\scripts\verify-ai-p4.ps1 -SkipPlaywright

# AI P5 remote provider rate limits (minIntervalMs / requestsPerMinute)
.\scripts\verify-ai-p5.ps1 -SkipPlaywright

# AI P6 token pricing + estimated USD cost on reports and usage API
.\scripts\verify-ai-p6.ps1 -SkipPlaywright

# AI P7 distributed JDBC rate-limit coordination (multi-JVM)
.\scripts\verify-ai-p7.ps1 -SkipPlaywright

# AI P8 platform daily AI quotas + console quota status
.\scripts\verify-ai-p8.ps1 -SkipPlaywright

# AI P9 scoped provider/template quotas + quota audit alert hooks
.\scripts\verify-ai-p9.ps1 -SkipPlaywright

# AI P10 tenant-scoped quotas + quota notification webhooks
.\scripts\verify-ai-p10.ps1 -SkipPlaywright

# Unit tests only (console REST, health, static resources)
.\scripts\verify-console-unit.ps1 -IncludeWebBuild

# Podman E2E only (after package exists)
.\scripts\e2e-podman.ps1 -SkipBuild
```

Other focused builds:

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

### Merge criteria (P0 regression gate)

Pull requests are **blocked when any P0 matrix row is not green**. The gate is enforced by `.\scripts\verify-harness.ps1` (reads `p0.pass` from `target/test-matrix-summary.json`) via the **Harness verify** workflow (`.github/workflows/harness-verify.yml`) on `pull_request`. P1/P2 row failures are tracked in the summary but do **not** block merge this phase.

The P0 set is defined in `.planning/test-matrix.yaml` (`tier: P0`); see `docs/test-harness.md` for tier semantics, evidence bars, and the COV-01 completion target. Phase 10 expanded the gate from 7 to **15 rows**, adding Phase 8 streaming/upsert and Phase 9 dialect capabilities:

- **New P0 rows (8):** `v2-streaming-csv`, `v2-streaming-json`, `v2-jdbc-upsert-pg-mysql`, `v2-dialect-dameng`, `v2-dialect-kingbase`, `v2-dialect-highgo`, `v2-dialect-postgres`, `v2-dialect-clickhouse`
- **Plus 7 legacy harness rows:** `calcite-scenario-v2`, `udf-sql`, `udf-script`, `udf-java-plugin`, `transform-json`, `transform-mask`, `transform-lookup`

Phase 8/9/11 UAT scripts (`verify-phase8-uat-rw-streaming-upsert.ps1`, `verify-phase9-uat-jdbc-dialect.ps1`, `verify-phase11-uat-closeout-hardening.ps1`) are **supplementary UAT** — useful for operator sign-off, but **not** the merge gate. Use `.\scripts\verify-harness.ps1` as the canonical pre-merge check.

## Boundaries

- ✅ **Always do:** Run **`test`** or at least a **targeted `-pl … -am test`** before claiming a fix is done; use **JDK 25** and repo settings (`.mvn/settings-jdk25.xml` or `mvnw-jdk25.ps1`) for builds. Keep edits **within the module that owns the behavior**. Follow Java file/Javadoc conventions for any touched `.java` public API.
- ⚠️ **Ask first:** Changes to **distributionManagement**, **corporate SCM URLs**, **`.mvn` settings**, **root dependency or Spring Boot BOM upgrades**, **new top-level modules**, or **cross-cutting security** (auth, trust stores, TLS termination). Adding or upgrading **snapshot** dependencies (`spring-ai` SNAPSHOT, etc.) should be aligned with the team.
- 🚫 **Never do:** Commit **secrets**, real production passwords, or internal host credentials (replace README examples with placeholders when fixing docs). Edit **`target/`** or other generated outputs as source. Disable tests wholesale in CI without team sign-off. Change **global** `JAVA_HOME` or user-level Maven `settings.xml` from automation—use process-local env or the provided scripts only.
