# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```

1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]

```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

<!-- GSD:project-start source:PROJECT.md -->

## Project

**data-generator**

**data-generator** is a Java/Maven synthetic-data platform for operators who define Template V2 pipelines (sources → transforms → sinks) and run them via a Spring Boot service with an embedded React operator console. The codebase is a mature brownfield monorepo (`3.0.0-SNAPSHOT`) with pluggable readers, writers, iterators, stages, Calcite SQL transforms, GraalJS/Velocity/SpEL scripting, and AI-assisted generation.

This GSD milestone extends the platform with **operator-uploadable multi-form UDFs**, **stronger transform operators and SQL capabilities**, and a **quality-first automated test harness** whose coverage ramps in phases. Reader/Writer expansion, datasource abstraction overhaul, and template-level orchestration are explicitly deferred.

**Core Value:** Operators can define, extend, and trust data-generation pipelines: register custom logic (UDFs), apply rich transforms, and verify behavior through an automated test harness before shipping.

### Constraints

- **Tech stack**: Java 25, Maven, Spring Boot 4.x — no framework downgrade; build via `mvnw-jdk25.ps1` / `.mvn/settings-jdk25.xml`
- **Compatibility**: Extend existing Template V2 schema and PF4J/GraalJS paths; avoid breaking published console APIs without migration notes
- **Security**: UDF upload must be governed (no arbitrary classpath execution without sandbox/review hooks); align with `SecretResolver` / template governance patterns
- **Testing**: Prefer embedded-first tests (H2, embedded Kafka, WireMock) per `docs/testing-embedded-components.md`; Playwright/Podman for console E2E where live infra needed
- **Documentation**: Public Java APIs follow repository copyright/Javadoc rules (`.cursor/rules/java-copyright-class-javadoc.mdc`)

<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

## Technology Stack

## Languages

- **Java 25** — All backend modules (`data-generator-*`), enforced by `maven-enforcer-plugin` in root `pom.xml` (`requireJavaVersion [25,)`). Compiler release set via `<java.version>25</java.version>` and `<maven.compiler.release>`.
- **TypeScript ~5.7** — Operator console SPA in `data-generator-console-web/` (`tsconfig.json`, `package.json`).
- **JavaScript** — GraalJS runtime for Template V2 `js` transformers (`data-generator-scripter-javascript/`, `data-generator-calcite/`); Vite/React build tooling.
- **YAML** — Template V2 pipeline definitions (`sources` / `transform` / `sink` / `workflow`); service config in `data-generator-service/src/main/resources/application*.yaml`.
- **SQL** — Apache Calcite dialect for in-pipeline transforms (`data-generator-calcite/`); JDBC queries in readers/writers.
- **Shell / PowerShell** — Packaging and verification scripts (`scripts/`, `data-generator-service/script/`, `mvnw-jdk25.ps1`).

## Runtime

- **JDK 25** (Temurin in CI; local helper `mvnw-jdk25.ps1` pins `JAVA_HOME` to a Zulu JDK 25 install).
- **Spring Boot 4.0.5** — Runnable service in `data-generator-service/`; entry class `org.gensokyo.data.DataGeneratorApplication` (`data-generator-service/pom.xml`).
- **Distributed worker** — Separate JVM profile via `org.gensokyo.data.DataGeneratorWorkerApplication` (`application-distributed-worker.yaml`, `service.env.example`).
- **Node.js ≥ 22** — Required by `data-generator-console-web/package.json` `engines.node`.
- **Vite 6.x dev server** on port **5173** (`data-generator-console-web/vite.config.ts`); production bundle output `data-generator-console-web/target/console-dist/`.
- **Maven 3.6.3+** — Multi-module aggregator (`pom.xml`, 16 top-level modules). Wrapper: `mvnw`, `mvnw.cmd`.
- **Maven Wrapper settings** — `.mvn/settings-jdk25.xml` disables default HTTP blocker for internal Nexus; local repo path configured there.
- **frontend-maven-plugin 1.15.1** — Downloads Node v22.14.0 / npm 10.9.2 during `data-generator-console-web` Maven build (`data-generator-console-web/pom.xml`).
- Group: `org.gensokyo.data.generator`
- Version: `3.0.0-SNAPSHOT` (`<revision>` in root `pom.xml`)
- Internal BOM: `data-generator-dependencies/pom.xml`

## Frameworks

- **Spring Boot 4.x** starters in `data-generator-service/pom.xml`:
- **Spring Kafka** — Producer integration via `spring-kafka` in `data-generator-common/data-generator-core/pom.xml`; multi-cluster config under `spring.kafka.multiple` in `application.yaml`.
- **MyBatis-Plus 3.5.7** + **MyBatis-Flex 1.9.5** BOM — ORM stack (Flex processor in compiler annotation paths; Plus starter for Spring Boot 4).
- **dynamic-datasource-spring-boot4-starter 4.5.0** — Multi-tenant JDBC routing (`baomidou`).
- **Alibaba Druid 1.2.23** — Connection pooling for all JDBC datasources.
- **Apache Calcite 1.41.0** — SQL transform runtime (`data-generator-calcite/pom.xml`: `calcite-core`, `calcite-babel`).
- **GraalJS 24.2.2** — Polyglot JS (`org.graalvm.polyglot:js-community`, `js-scriptengine`) for `js` transformers.
- **Spring Expression Language (SpEL)** — `data-generator-scripter-spel/`, `data-generator-reader-spel/`.
- **Apache Velocity 2.3** — `data-generator-scripter-velocity/`.
- **PF4J 3.15.0** — Custom transformer plugins (`data-generator-calcite/pom.xml`; default framework in `DataGeneratorProperties.v2PluginFramework`).
- **DataFaker 2.2.2** — Synthetic field values (`data-generator-faker/`, root `pom.xml`).
- **JGraphT 1.5.2** — Transform DAG / workflow graph (`data-generator-core/pom.xml`).
- **GeoTools 26.3** + **JTS 1.19.0** — GeoJSON/shapefile (`data-generator-geo/`, root `pom.xml`).
- **EasyExcel 4.0.2** / **Commons CSV 1.11.0** — File readers/writers.
- **React 19** + **react-dom 19** — SPA (`data-generator-console-web/package.json`).
- **React Router 7** — Client routing under `/console/*`.
- **Ant Design 5.22** — UI components.
- **TanStack React Query 5** — Server state / API caching.
- **i18next 24** + **react-i18next 15** — Internationalization.
- **CodeMirror 6** (`@uiw/react-codemirror`, `@codemirror/lang-yaml`) — YAML template editor.
- **JUnit 5** — Standard across Java modules (`spring-boot-starter-test` in root `pom.xml`).
- **Mockito 5.17.0** — With Java agent line for Surefire/Failsafe (`mockito.agentLine` in root `pom.xml`).
- **Testcontainers 1.20.6** — MySQL, PostgreSQL, ClickHouse slices (`data-generator-calcite/pom.xml`).
- **spring-kafka-test** — Embedded Kafka Kraft broker (`EmbeddedKafkaTestSupport` in `data-generator-calcite/src/test/`).
- **Playwright 1.49** — Console UI/E2E (`data-generator-console-web/package.json`, `e2e/specs/`).
- **ClassGraph 4.8.184** — Test-scope classpath introspection (root `pom.xml`).
- **maven-assembly-plugin** — Tarball/zip distribution (`data-generator-service/assembly.xml`: `bin/`, `lib/`, `conf/`, `jdbc-bundled/`).
- **maven-dependency-plugin** — Bundles JDBC drivers into `jdbc-bundled/` at package time (`data-generator-service/pom.xml`).
- **maven-antrun-plugin** — Copies `console-dist` into `classpath:static/console/` during `process-classes`.
- **auto-service 1.1.1** — SPI registration for readers/writers/iterators.
- **Lombok 1.18.44** — Boilerplate reduction (provided scope).

## Key Dependencies

| Dependency | Version | Role | Primary location |
|------------|---------|------|------------------|
| Spring Boot BOM | 4.0.5 | Service runtime, autoconfig | root `pom.xml` |
| Jackson 3.x | 3.1.0 | JSON/YAML binding (`tools.jackson.*`) | root `pom.xml`, `data-generator-core` |
| Apache Calcite | 1.41.0 | Template V2 SQL engine | `data-generator-calcite/pom.xml` |
| GraalJS | 24.2.2 | JavaScript transforms | `data-generator-scripter-javascript/` |
| dynamic-datasource | 4.5.0 | Multi-JDBC routing | `data-generator-service/pom.xml` |
| elasticsearch-rest-client | 7.17.8 | ES read/write adapters | `data-generator-dependencies/pom.xml` |
| spring-kafka | (Boot BOM) | Kafka producers | `data-generator-core/pom.xml` |
| PF4J | 3.15.0 | Plugin framework | `data-generator-calcite/pom.xml` |
| Hibernate Validator | 8.0.1.Final | Bean/template validation | `data-generator-service/pom.xml` |
| jsonschema-generator (victools) | 4.35.0 | JSON Schema for console forms | root `pom.xml` |
| org.gensokyo:kits | 1.0.0 | Internal utilities (`StrKit`, etc.) | `data-generator-dependencies/pom.xml` |

- MySQL `mysql-connector-j` 8.0.31
- PostgreSQL `postgresql` 42.5.1
- ClickHouse `clickhouse-jdbc` 0.6.2
- H2 2.2.224 (metadata + tests)
- Dameng `dm-jdbc` 1.8
- Kingbase8 8.6.1 / Kingbase9 9.0.0
- HighGo `HgdbJdbc` 6.2.4
- `OllamaAiRuntimeBridge`, `OpenAiCompatibleRuntimeBridge`, `CompositeAiRuntimeBridge` in `data-generator-service/src/main/java/org/gensokyo/data/ai/runtime/`
- Config prefix: `data.generator.ai-runtime.*` (`DataGeneratorProperties.java`)
- `spring-ai` 1.0.0-SNAPSHOT declared but BOM import commented in root `pom.xml`
- Spring AI BOM and `spring-ai-ollama-spring-boot-starter` (root `pom.xml`)
- Caffeine, Guava, Eclipse Collections (commented in dependencyManagement)
- MyBatis-Flex Spring Boot starter (commented; processor still on annotation path)

## Module Topology

| Module | Purpose |
|--------|---------|
| `data-generator-dependencies/` | Internal artifact version BOM |
| `data-generator-common/` | `data-generator-core`, `data-generator-database-core` |
| `data-generator-datasource/` | Datasource abstractions |
| `data-generator-console-web/` | React SPA (packaging `pom`, Node build) |
| `data-generator-service/` | Spring Boot app, REST, console APIs, assembly |
| `data-generator-stage/` | Legacy V1 stage model (runtime retired) |
| `data-generator-faker/` | DataFaker integration |
| `data-generator-geo/` | JTS/geo utilities |
| `data-generator-iterator/` | 8 iterator submodules (number, date, geo, DB, …) |
| `data-generator-generator/` | sync/async generation orchestration |
| `data-generator-reader/` | JDBC, CSV, Excel, JSON, ES, SpEL, AI readers |
| `data-generator-scripter/` | GraalJS, SpEL, Velocity |
| `data-generator-writer/` | JDBC, Kafka, ES, CSV, Excel, JSON writers |
| `data-generator-converter/` | Type conversion |
| `data-generator-calcite/` | Template V2 runtime, SQL, PF4J, sinks |

## Configuration

- Base: `data-generator-service/src/main/resources/application.yaml` (port **9876**, H2 file metadata DB, sample business datasources).
- Profiles:
- Test slice: `data-generator-service/src/test/resources/application-phase7-test.yaml` (in-memory H2, `server.port: 0`).
- Packaged runtime env template: `data-generator-service/src/main/resources/service.env.example` → copied to `conf/service.env` in distribution.
- `data.generator.*` — Thread pools, plugin dirs, preview limits, governance, AI runtime/quota
- `data.generator.console-security.*` — Header RBAC (`enabled`, `role-header`, `actor-header`)
- `data.generator.distributed.*` — Coordinator/worker toggles, lease, heartbeat
- `data.generator.schedule.*` — Cron scheduler
- `spring.datasource.dynamic.*` — Named JDBC pools (Druid)
- `spring.kafka.multiple.*` — Named Kafka clusters (`primary`, `clusters.*.bootstrap-servers`)
- Root enforcer: JDK 25+, Maven 3.6.3+ (`pom.xml` `maven-enforcer-plugin`)
- Console embed skip flag: `-Dskip.console.frontend=true` (`data-generator-service/pom.xml`)
- JDBC bundle output: `${project.build.outputDirectory}/jdbc-bundled`
- Frontend proxy: `data-generator-console-web/vite.config.ts` proxies `/api` → `http://localhost:9876`
- Internal Nexus (HTTP): `http://172.25.20.192:8081/nexus/...` — releases + snapshots (`pom.xml` `<repositories>`, `<distributionManagement>`)
- Maven Central, Spring Milestones, Spring Snapshots (for Boot 4 / optional Spring AI)
- SCM: internal GitLab-style host (`pom.xml` `<scm>`)
- `data-generator-service/src/main/resources/logback-spring.xml` — Console + rolling file appenders under `../logs/`

## Platform Requirements

- **JDK 25** (mandatory)
- **Node.js 22+** and npm (for console dev; Maven can install Node via `frontend-maven-plugin`)
- **Maven 3.6.3+** via wrapper
- **Windows:** prefer `.\mvnw-jdk25.ps1` (sets `JAVA_HOME`, applies `.mvn/settings-jdk25.xml`)
- **Docker** (optional): Testcontainers integration tests; Podman for local E2E (`scripts/e2e-podman.ps1`)
- **Ollama** (optional): Live AI ITs when `localhost:11434` reachable (`docs/jdk25-upgrade.md`)
- Deliverable: Maven assembly **tar.gz / zip** (`data-generator-service/assembly.xml`), not fat Spring Boot JAR (`spring-boot-maven-plugin` repackage skipped).
- Layout: `bin/*.jar`, `lib/*`, `conf/application*.yaml`, `jdbc-bundled/**`, `bin/*.sh` / `*.ps1`.
- Default service port: **9876** (`application.yaml`); staging profile uses **8080**.
- Console static assets embedded at `classpath:static/console/` (built from `data-generator-console-web/target/console-dist`).
- JVM heap defaults in `service.env.example`: `DG_HEAP_MIN=512m`, `DG_HEAP_MAX=1g`.
- Roles: `DG_SERVICE_ROLE=coordinator` (default) or `worker` with `DG_MAIN_CLASS=org.gensokyo.data.DataGeneratorWorkerApplication`.
- `.github/workflows/maven-test.yml` — JDK 25 Temurin, console-web package, full `mvn test` with `-Dskip.console.frontend=true`
- `.github/workflows/console-verify.yml` — JDK 25 + Node 22 + Podman + PowerShell; runs `scripts/verify-console.ps1`
- Console UI: `http://localhost:9876/console/`
- Console / legacy APIs: `http://localhost:9876/api/*`, `/template/*`, `/task/*`
- H2 console (dev): `http://localhost:9876/h2`
- Health: `/healthz` (referenced in `service.env.example`)

## Notable Technical Decisions

- **Template V2 only** for task execution; V1 retired (`data.generator.v1-execution.enabled` ignored for runs per `DataGeneratorProperties`).
- **Embedded-first testing** — H2 metadata, embedded Kafka, in-process ES HTTP stub; see `docs/testing-embedded-components.md`.
- **No Redis module** in repo yet (`docs/testing-embedded-components.md` notes future embedded-redis/Testcontainers).
- **Jackson 3.x** (`tools.jackson`) coexists with `com.fasterxml.jackson.annotations` for compatibility.
- **Console security is opt-in** — `data.generator.console-security.enabled=false` by default; staging enables header RBAC.
- **Secrets stored in metadata DB** via JPA (`SecretService`, `SecretEntryRepository`), not filesystem `.env` (no `.env` files in repo).

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

## 1. Behavioral baseline (all contributors)

## 2. Repository layout and module naming

| Pattern | Example | Purpose |
|---------|---------|---------|
| `data-generator-<concern>/` | `data-generator-calcite/` | Top-level Maven module for a domain concern |
| `data-generator-<concern>-<adapter>/` | `data-generator-writer-kafka/` | Pluggable adapter submodule |
| Package root | `org.gensokyo.data.<area>` | All Java sources |
| Service entry | `org.gensokyo.data.DataGeneratorApplication` | Spring Boot main in `data-generator-service` |
| Console REST | `org.gensokyo.data.api.console.*` | Operator console `/api/*` facades |
| Legacy/admin REST | `org.gensokyo.data.controller.*` | Non-console HTTP controllers |
| V2 pipeline runtime | `org.gensokyo.data.calcite.runtime.*` | Template V2 execution engine |
| Model VOs | `org.gensokyo.data.model.v2.*` | Template V2 YAML/JSON binding types |
| Shared response | `org.gensokyo.data.model.vo.R` | Standard API envelope |

## 3. Java source file layout (mandatory)

### 3.1 Order

### 3.2 Copyright block (do not alter company text)

### 3.3 Type-level Javadoc

### 3.4 Public API Javadoc

### 3.5 Inline comments

## 4. Naming conventions

### 4.1 Classes and types

| Kind | Pattern | Example |
|------|---------|---------|
| Spring `@RestController` (console) | `Console<Feature>Controller` | `ConsoleJobController` |
| Spring `@RestController` (legacy) | `<Feature>Controller` | `TaskController` |
| Service layer | `<Feature>Service` | `TaskExecutionService` |
| Configuration | `<Feature>Config` / `*Properties` | `ConsoleWebConfig`, `DataGeneratorProperties` |
| Factory / registry | `<Thing>Factory`, `<Thing>Registry` | `QuerySourceFactory`, `TemplateV2RuntimeRegistry` |
| Value objects (V2) | `*VO` suffix | `TemplateV2VO`, `ExecutionPolicyVO` |
| Persistence | `*PO`, `*Repository` | `TemplatePO`, `TemplateRepository` |
| DTO (console) | under `api.console.dto` | `JobExecutionDetail` |
| Utility | `*Kit`, `*Support` | `TemplateKit`, `EmbeddedKafkaTestSupport` |
| Domain exceptions | descriptive noun + base `ScaleLimitExceededException` in `data-generator-calcite` |

### 4.2 Methods and fields

- **Controllers:** HTTP verb names or domain verbs — `list`, `get`, `cancel`, `findById`.
- **Tests:** JUnit 5 `@Test` methods use `camelCase` describing behavior — `list_returnsExecutions`, `chunkedModeWritesAllRowsInBatches`, `singleTableSelectIsRowLocal`.
- **Constants:** `UPPER_SNAKE_CASE` in `static final` fields (see `ChunkedPipelineTests.ROW_COUNT`).

### 4.3 Test class naming (see `TESTING.md` for detail)

| Suffix | Meaning | Example |
|--------|---------|---------|
| `Tests` (plural) | Unit or integration test class | `ExecutionShapeClassifierTests` |
| `Test` (singular) | Also used (legacy/console slice) | `ConsoleJobControllerTest` |
| `IT` | Integration test (full or partial Spring context) | `ConsoleWebEndpointIT`, `V2ScenarioTemplateIT` |

## 5. Language, build, and dependencies

- **Java 25** — `<java.version>25</java.version>`, `maven.compiler.release` in root `pom.xml`.
- **Enforcer** requires JDK `[25,)` and Maven `[3.6.3,)` (root `pom.xml` `maven-enforcer-plugin`).
- **Build on Windows:** use `.\mvnw-jdk25.ps1` or `.\mvnw.cmd -s .mvn\settings-jdk25.xml` (internal Nexus uses HTTP).
- **Lombok** (`1.18.44`): `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Slf4j` are common in service and console code.
- **Spring Boot** `4.0.5` aggregated in `data-generator-service`.
- **Jackson 3.x** for JSON/YAML binding.

## 6. Spring and REST patterns

### 6.1 Console controllers

- Package: `org.gensokyo.data.api.console`
- Base path: `/api/<resource>` (e.g. `/api/jobs`)
- Constructor injection via `@RequiredArgsConstructor` + `final` fields
- Return type: **`R<T>`** envelope (never raw entities at the top level)

### 6.2 Standard response envelope `R<T>`

- Success: `R.ok(data)` → `success=true`, HTTP 200 semantics in `code`
- Failure: `R.fail(message)` → `success=false`
- Factory methods: `R.ok(...)`, `R.fail(...)`, nested `R.Page` for pagination

### 6.3 Validation

- Jakarta Validation on parameters: `@NotNull`, `@PathVariable`, `@RequestParam`
- Domain validation in services/runtime: throw **`IllegalArgumentException`** for client errors (unknown id, invalid template shape)
- **`IllegalStateException`** for internal invariant violations (pipeline misconfiguration)
- **`UnsupportedOperationException`** for unimplemented execution modes
- **`ScaleLimitExceededException`** for policy limit breaches in V2 runtime (`data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/`)

## 7. Error handling

### 7.1 Console API — centralized advice

| Exception | HTTP status | Response |
|-----------|-------------|----------|
| `IllegalArgumentException` | 400 BAD_REQUEST | `R.fail(ex.getMessage())` |
| `Exception` (catch-all) | 500 INTERNAL_SERVER_ERROR | `R.fail(...)` + `log.error` |

### 7.2 Logging

### 7.3 Fail-fast vs. envelope

- **Console `/api/*`:** exceptions → `ConsoleApiAdvice` → `R.fail`
- **Legacy controllers:** may return `R` directly from controller methods; some paths use `R.fail` in-controller
- **Runtime/pipeline:** throw typed exceptions; let upper layers translate to HTTP or run reports

## 8. V2 template and pipeline conventions

- Template model types live in `org.gensokyo.data.model.v2.*` with `*VO` suffix.
- Runtime orchestration: `TemplateV2Runner`, `ChunkedPipeline`, `StreamingPipeline` in `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/`.
- Execution modes: `CHUNKED`, streaming, partitioned compute — policy in `ExecutionPolicyVO`.
- Factories implement plugin-style registration (`QuerySourceFactory`, `JdbcSinkFactory`, etc.) wired through `TemplateV2RuntimeRegistry`.

## 9. Git commit conventions

- **Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- **Subject:** ~72 chars, imperative, lowercase start, no trailing period
- **Footer:** always include `AI-Assisted-by` and `Co-authored-by` (use `git config user.name` / `user.email` for the latter)

## 10. Operator console (TypeScript / React)

| Concern | Convention |
|---------|------------|
| Runtime | Node **22+**, `"type": "module"` in `package.json` |
| Build | `tsc -p tsconfig.json --noEmit && vite build` |
| UI library | Ant Design 5, React 19, React Router 7 |
| Data fetching | TanStack React Query |
| i18n | `i18next` + `react-i18next` |
| Page components | `export function <Name>Page()` in `src/app/pages/` |
| Shared UI | `src/components/` |
| Theme | `src/theme/ThemeProvider.tsx`, dual-theme support |
| Static embed | Built to `target/console-dist`, embedded in service JAR at `classpath:static/console/` |

## 11. CodeGraph and exploration

## 12. Checklist for new Java public API

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

## 1. Executive summary

## 2. High-level system diagram

```

```

## 3. Architectural patterns

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Multi-module Maven reactor** | Root `pom.xml` | Isolates concerns; `data-generator-service` aggregates all runtime plugins |
| **Factory + generic type dispatch** | `StageFactory`, `IteratorFactory`, `ReaderFactory`, `WriterFactory`, `GeneratorFactory` in `data-generator-core` | Resolve implementation from VO class at runtime |
| **AutoService SPI for VOs** | `*VO.java` in plugin modules (e.g. `CsvReaderVO`, `KafkaWriterVO`) | Classpath discovery of config subtypes without central registry edits |
| **Spring bean autowiring on instances** | Factories call `autowireBean` after `newInstance` | Stages/iterators get `@Autowired` collaborators without being `@Component` |
| **Pipeline (chain of responsibility)** | `DefaultRowPipeline`, `DefaultFieldPipeline`, `DefaultWritePipeline`, `AbstractPipeline` | Sequential stage execution over `Value` |
| **DAG topological ordering** | `DefaultRowPipelineFactory.sort` (JGraphT) | Field dependency order for V1 row generation |
| **Producer/consumer batching** | `AbstractGenerator` | Async/sync generators queue rows, batch-write via `DefaultWritePipelineFactory` |
| **Template V2 runtime registry** | `TemplateV2RuntimeRegistry`, `RefreshableTemplateV2RuntimeRegistryProvider` | Composes source/transform/sink factories; supports PF4J extensions |
| **Execution policy modes** | `EffectiveExecutionPolicy` → `STREAMING` / `IN_MEMORY` / `CHUNKED` | Trade memory for throughput in Calcite pipelines |
| **Embedded SPA** | `ConsoleWebConfig` | Serves React console from JAR; SPA fallback to `index.html` |

## 4. Layer model

### 4.1 Presentation layer

| Surface | Location | Notes |
|---------|----------|-------|
| React operator console | `data-generator-console-web/src/` | Built to `target/console-dist`, embedded at `classpath:/static/console/` |
| Console REST API | `data-generator-service/src/main/java/org/gensokyo/data/api/console/` | Prefix `/api/*` — templates, jobs, datasources, schedules, audit, AI catalog |
| Legacy/admin REST | `data-generator-service/src/main/java/org/gensokyo/data/controller/` | `/task`, `/template`, `/datasource`, `/healthz` |
| Health | `HealthController.java` | `GET /healthz` |

### 4.2 Application / orchestration layer (`data-generator-service`)

- **Template CRUD & lifecycle** — `TemplateController`, `ConsoleTemplateController`, `TemplateLifecycleService`
- **Run orchestration** — `TaskController.run()` detects V1 vs V2 via `TemplateDefinitionDetector`, queues `TaskExecutionPO`, submits async work
- **V2 execution tracking** — `TaskExecutionService`, `RunReportCollector`, cancel/pause via `WorkflowRunControl`
- **Distributed runs** — optional `DistributedJobService` when `distributedExecutionProperties.isEnabled()`
- **AI governance** — `AiQuotaService`, `AiUsageService`, rate limiters in `org.gensokyo/data/ai/`
- **Datasource registry** — dynamic JDBC/ES/Kafka client registries wired in `CoreConfig`
- **Audit** — `AuditService` records operator actions

### 4.3 Domain / execution layer

#### Template V2 (active path)

```

```
| Abstraction | Package | Role |
|-------------|---------|------|
| `RowSource` | `org.gensokyo.data.calcite.source` | Read rows (JDBC query, CSV, JSON, iterator bridge, AI, inline) |
| `RowSink` | `org.gensokyo.data.calcite.sink` | Write rows (JDBC bulk, CSV, JSON, console, Kafka, ES) |
| `CalciteRowTransformer` | `org.gensokyo.data.calcite.sql` | SQL/SpEL/JS transforms via Calcite |
| `TemplateV2RuntimeRegistry` | `org.gensokyo.data.calcite.runtime` | Factory registry for sources/transforms/sinks |
| `TemplateV2RuntimePlugin` | `org.gensokyo.data.calcite.plugin` | PF4J / ServiceLoader extension points |

#### Template V1 (retired execution, retained libraries)

- **Template** — `TemplateVO` with `fields`, `iterator`, `generator`, `output`
- **Iterator tree** — nested `IteratorVO` with optional `choose`/`otherwise` conditions (script-evaluated)
- **Per-field stage chains** — READ → SELECT → SCRIPT → CONVERT → …
- **Row assembly** — `DefaultRowPipelineFactory` builds one `MapValue` row per iteration
- **Write** — `DefaultWritePipelineFactory` invokes `WriteStage` → `Writer` implementations

### 4.4 Infrastructure / plugin layer

```

```
| Module family | Interface (core) | Discovery |
|---------------|------------------|-----------|
| `data-generator-iterator-*` | `Iterator<T extends IteratorVO>` | `IteratorFactory` + `ClassKit` |
| `data-generator-reader-*` | `Reader<S,T>` | `ReaderFactory` + Spring beans |
| `data-generator-writer-*` | `Writer<S,T>` | `WriterFactory` |
| `data-generator-stage` | `Stage<T extends StageVO>` | `StageFactory` + `ClassKit` |
| `data-generator-scripter-*` | `Script` | `ScriptFactory` |
| `data-generator-generator-*` | `Generator<G>` via `AbstractGenerator` | `GeneratorFactory` |
| `data-generator-converter` | `Converter` | Spring `@Bean` lookup in `ConvertStage` |

- **`data-generator-faker`** — DataFaker integration, SpEL variables (`DataFaker.java`, providers)
- **`data-generator-geo`** — GeoJSON/WKT generation, used by iterator and Calcite SQL functions
- **`data-generator-common/data-generator-database-core`** — JDBC dialects (`Dialect`, `DialectFactory`)

## 5. Core data abstractions

### 5.1 `Value` hierarchy

- `SingleValue`, `ListValue`, `MapValue` — universal row/cell representation through V1 pipelines
- V2 uses `org.gensokyo.data.model.v2.Row` and `RowSchema` in Calcite runtime

### 5.2 Context objects

| Context | Package | Carries |
|---------|---------|---------|
| `TemplateContext` | `org.gensokyo.data.context` | Template + input dataset |
| `GeneratorContext` | same | Template + generator config |
| `IteratorContext` | same | Template + iterator VO + parent values |
| `StageContext` | same | Template + optional field + stage VO |

### 5.3 Stage types (V1)

| Type | Core class | Module extensions |
|------|------------|-------------------|
| `READ` | `ReadStage` | Uses `ReaderFactory` |
| `SELECT` | `SelectStage` | Value selection strategies |
| `SCRIPT` | `ScriptStage` | SpEL/JS via `ScriptFactory` |
| `WRITE` | `WriteStage` | Uses `WriterFactory` |
| — | `ConvertStage`, `ConditionStage`, `MappingStage`, `LogStage`, `PauseStage`, `SharedStage` | `data-generator-stage` |

### 5.4 Configuration models

| Generation | Primary types | Location |
|------------|---------------|----------|
| V1 | `TemplateVO`, `FieldVO`, `IteratorVO`, `GeneratorVO` | `model/vo/` |
| V2 | `TemplateV2VO`, `TemplateV2DraftVO`, `SourceVO`, `TransformVO`, `SinkVO`, workflow VOs | `model/v2/` |
| Shared reporting | `RunReportVO`, `AiCallMetricVO` | `model/v2/` |

## 6. Data flow — Template V2 pipeline execution

```

```

## 7. Data flow — Template V1 generator (library path, not service-run)

```

```

## 8. Entry points

| Entry | Path | Description |
|-------|------|-------------|
| **Main** | `DataGeneratorApplication.main` | Spring Boot bootstrap, `@EnableScheduling` |
| **V2 run** | `TemplateV2Runner.run(TemplateV2VO)` | Calcite pipeline dispatch |
| **HTTP run** | `TaskController` `/task/run/{id}` | Operator/API trigger |
| **Console run** | `ConsoleTemplateController` `/api/templates/{id}/run` | Same backend, JSON API |
| **Scheduled run** | `TaskController.triggerScheduledRun` | Invoked by schedule poller |
| **Health** | `GET /healthz` | Liveness |
| **Console UI** | `GET /console/` | SPA entry (`ConsoleWebConfig`) |

## 9. Cross-cutting concerns

| Concern | Implementation |
|---------|----------------|
| **Secrets** | `SecretResolver` in core; `ConsoleSecretController` at `/api/secrets` |
| **Template governance** | Plaintext password rejection, lifecycle states (`TemplateLifecycleStatus`) |
| **AI rate limits & quotas** | `AiRateLimiter` (in-memory or JDBC), `AiQuotaService`, scoped daily usage repos |
| **Run lineage** | `RunLineageSupport` snapshots template hash, plugin set, datasource config hash on queue |
| **Caching** | `DataSet` per template instance (V1 in-memory reader datasets); `Templates` cache in service |
| **JSON/YAML** | `YamlParser`, `JacksonParser`, `TemplateJsonCodec` in service |
| **Testing** | Embedded H2/Kafka preferred; `@SpringBootTest` with `classpath:/application-phase7-test.yaml` |

## 10. Dependency direction (allowed imports)

```

```

## 11. Technology map

| Concern | Libraries |
|---------|-----------|
| Web | Spring Boot 4.x Web MVC |
| Persistence | Spring Data JDBC/JPA repos in service (`repository/`, `model/po/`) |
| SQL engine | Apache Calcite 1.41 (`data-generator-calcite`) |
| Graph (V1 fields) | JGraphT (`DefaultRowPipelineFactory`) |
| Scripting | GraalJS, Spring SpEL, Apache Velocity (module-dependent) |
| Faker | DataFaker 2.x (`data-generator-faker`) |
| Geo | GeoTools (`data-generator-geo`) |
| Messaging | Spring Kafka (`data-generator-writer-kafka`) |
| Search | Elasticsearch REST client (`data-generator-writer-elasticsearch`) |
| Excel/CSV | EasyExcel, Commons CSV |
| Plugins | PF4J (`PathBasedPf4jRuntimeExtensionLocator`), Google AutoService |

## 12. Evolution notes (2026-06-17)

- **V1 execution retired** at service boundary; V1 code remains for types, tests, and V2 bridges (e.g. iterator source).
- **V2 is the product path**: workflow/compute blocks, execution policies, Calcite SQL transforms, AI sources with quota tracing.
- **Console is first-class**: template editor, job center, datasource admin, schedules, audit — all backed by `/api/*` controllers.
- **PF4J** enables out-of-tree Template V2 runtime extensions; sample at `samples/template-v2-pf4j-plugin/`.

<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
