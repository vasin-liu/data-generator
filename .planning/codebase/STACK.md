# Technology Stack

**Analysis Date:** 2026-06-17

## Languages

**Primary:**
- **Java 25** — All backend modules (`data-generator-*`), enforced by `maven-enforcer-plugin` in root `pom.xml` (`requireJavaVersion [25,)`). Compiler release set via `<java.version>25</java.version>` and `<maven.compiler.release>`.
- **TypeScript ~5.7** — Operator console SPA in `data-generator-console-web/` (`tsconfig.json`, `package.json`).

**Secondary:**
- **JavaScript** — GraalJS runtime for Template V2 `js` transformers (`data-generator-scripter-javascript/`, `data-generator-calcite/`); Vite/React build tooling.
- **YAML** — Template V2 pipeline definitions (`sources` / `transform` / `sink` / `workflow`); service config in `data-generator-service/src/main/resources/application*.yaml`.
- **SQL** — Apache Calcite dialect for in-pipeline transforms (`data-generator-calcite/`); JDBC queries in readers/writers.
- **Shell / PowerShell** — Packaging and verification scripts (`scripts/`, `data-generator-service/script/`, `mvnw-jdk25.ps1`).

## Runtime

**Backend environment:**
- **JDK 25** (Temurin in CI; local helper `mvnw-jdk25.ps1` pins `JAVA_HOME` to a Zulu JDK 25 install).
- **Spring Boot 4.0.5** — Runnable service in `data-generator-service/`; entry class `org.gensokyo.data.DataGeneratorApplication` (`data-generator-service/pom.xml`).
- **Distributed worker** — Separate JVM profile via `org.gensokyo.data.DataGeneratorWorkerApplication` (`application-distributed-worker.yaml`, `service.env.example`).

**Frontend environment:**
- **Node.js ≥ 22** — Required by `data-generator-console-web/package.json` `engines.node`.
- **Vite 6.x dev server** on port **5173** (`data-generator-console-web/vite.config.ts`); production bundle output `data-generator-console-web/target/console-dist/`.

**Build tooling:**
- **Maven 3.6.3+** — Multi-module aggregator (`pom.xml`, 16 top-level modules). Wrapper: `mvnw`, `mvnw.cmd`.
- **Maven Wrapper settings** — `.mvn/settings-jdk25.xml` disables default HTTP blocker for internal Nexus; local repo path configured there.
- **frontend-maven-plugin 1.15.1** — Downloads Node v22.14.0 / npm 10.9.2 during `data-generator-console-web` Maven build (`data-generator-console-web/pom.xml`).

**Package / artifact coordinates:**
- Group: `org.gensokyo.data.generator`
- Version: `3.0.0-SNAPSHOT` (`<revision>` in root `pom.xml`)
- Internal BOM: `data-generator-dependencies/pom.xml`

## Frameworks

**Core backend:**
- **Spring Boot 4.x** starters in `data-generator-service/pom.xml`:
  - `spring-boot-starter-web` — Legacy REST (`/template/`, `/task/`, `/datasource/`) and health.
  - `spring-boot-starter-webflux` — Reactive HTTP for AI provider bridges (`data-generator-reader-ai/`, `OpenAiCompatibleRuntimeBridge`).
  - `spring-boot-starter-data-jpa` — Metadata persistence (templates, jobs, secrets, AI usage).
  - `spring-boot-starter-logging` — Logback (`logback-spring.xml`).
- **Spring Kafka** — Producer integration via `spring-kafka` in `data-generator-common/data-generator-core/pom.xml`; multi-cluster config under `spring.kafka.multiple` in `application.yaml`.
- **MyBatis-Plus 3.5.7** + **MyBatis-Flex 1.9.5** BOM — ORM stack (Flex processor in compiler annotation paths; Plus starter for Spring Boot 4).
- **dynamic-datasource-spring-boot4-starter 4.5.0** — Multi-tenant JDBC routing (`baomidou`).
- **Alibaba Druid 1.2.23** — Connection pooling for all JDBC datasources.

**Template V2 engine:**
- **Apache Calcite 1.41.0** — SQL transform runtime (`data-generator-calcite/pom.xml`: `calcite-core`, `calcite-babel`).
- **GraalJS 24.2.2** — Polyglot JS (`org.graalvm.polyglot:js-community`, `js-scriptengine`) for `js` transformers.
- **Spring Expression Language (SpEL)** — `data-generator-scripter-spel/`, `data-generator-reader-spel/`.
- **Apache Velocity 2.3** — `data-generator-scripter-velocity/`.
- **PF4J 3.15.0** — Custom transformer plugins (`data-generator-calcite/pom.xml`; default framework in `DataGeneratorProperties.v2PluginFramework`).

**Data generation & geo:**
- **DataFaker 2.2.2** — Synthetic field values (`data-generator-faker/`, root `pom.xml`).
- **JGraphT 1.5.2** — Transform DAG / workflow graph (`data-generator-core/pom.xml`).
- **GeoTools 26.3** + **JTS 1.19.0** — GeoJSON/shapefile (`data-generator-geo/`, root `pom.xml`).
- **EasyExcel 4.0.2** / **Commons CSV 1.11.0** — File readers/writers.

**Operator console (frontend):**
- **React 19** + **react-dom 19** — SPA (`data-generator-console-web/package.json`).
- **React Router 7** — Client routing under `/console/*`.
- **Ant Design 5.22** — UI components.
- **TanStack React Query 5** — Server state / API caching.
- **i18next 24** + **react-i18next 15** — Internationalization.
- **CodeMirror 6** (`@uiw/react-codemirror`, `@codemirror/lang-yaml`) — YAML template editor.

**Testing:**
- **JUnit 5** — Standard across Java modules (`spring-boot-starter-test` in root `pom.xml`).
- **Mockito 5.17.0** — With Java agent line for Surefire/Failsafe (`mockito.agentLine` in root `pom.xml`).
- **Testcontainers 1.20.6** — MySQL, PostgreSQL, ClickHouse slices (`data-generator-calcite/pom.xml`).
- **spring-kafka-test** — Embedded Kafka Kraft broker (`EmbeddedKafkaTestSupport` in `data-generator-calcite/src/test/`).
- **Playwright 1.49** — Console UI/E2E (`data-generator-console-web/package.json`, `e2e/specs/`).
- **ClassGraph 4.8.184** — Test-scope classpath introspection (root `pom.xml`).

**Build / packaging:**
- **maven-assembly-plugin** — Tarball/zip distribution (`data-generator-service/assembly.xml`: `bin/`, `lib/`, `conf/`, `jdbc-bundled/`).
- **maven-dependency-plugin** — Bundles JDBC drivers into `jdbc-bundled/` at package time (`data-generator-service/pom.xml`).
- **maven-antrun-plugin** — Copies `console-dist` into `classpath:static/console/` during `process-classes`.
- **auto-service 1.1.1** — SPI registration for readers/writers/iterators.
- **Lombok 1.18.44** — Boilerplate reduction (provided scope).

## Key Dependencies

**Critical (platform understanding):**

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

**JDBC drivers (managed versions in root `pom.xml`, bundled in service package):**
- MySQL `mysql-connector-j` 8.0.31
- PostgreSQL `postgresql` 42.5.1
- ClickHouse `clickhouse-jdbc` 0.6.2
- H2 2.2.224 (metadata + tests)
- Dameng `dm-jdbc` 1.8
- Kingbase8 8.6.1 / Kingbase9 9.0.0
- HighGo `HgdbJdbc` 6.2.4

**AI runtime (custom bridges, Spring AI BOM commented out):**
- `OllamaAiRuntimeBridge`, `OpenAiCompatibleRuntimeBridge`, `CompositeAiRuntimeBridge` in `data-generator-service/src/main/java/org/gensokyo/data/ai/runtime/`
- Config prefix: `data.generator.ai-runtime.*` (`DataGeneratorProperties.java`)
- `spring-ai` 1.0.0-SNAPSHOT declared but BOM import commented in root `pom.xml`

**Commented / optional (present in POM but not active by default):**
- Spring AI BOM and `spring-ai-ollama-spring-boot-starter` (root `pom.xml`)
- Caffeine, Guava, Eclipse Collections (commented in dependencyManagement)
- MyBatis-Flex Spring Boot starter (commented; processor still on annotation path)

## Module Topology

Maven reactor modules (root `pom.xml` `<modules>`):

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

Pluggable components use **Java SPI** via `@AutoService` + `ServiceLoader` pattern across reader/writer/iterator modules.

## Configuration

**Spring Boot application config:**
- Base: `data-generator-service/src/main/resources/application.yaml` (port **9876**, H2 file metadata DB, sample business datasources).
- Profiles:
  - `application-staging.yaml` — RBAC + governance + schedules (port 8080)
  - `application-e2e.yaml`, `application-e2e-rbac.yaml` — E2E / Playwright
  - `application-distributed-coordinator.yaml`, `application-distributed-worker.yaml` — Distributed queue
  - `application-distributed-staging.yaml`, `application-e2e-distributed.yaml`
- Test slice: `data-generator-service/src/test/resources/application-phase7-test.yaml` (in-memory H2, `server.port: 0`).
- Packaged runtime env template: `data-generator-service/src/main/resources/service.env.example` → copied to `conf/service.env` in distribution.

**Key property prefixes (`DataGeneratorProperties`, `ConsoleSecurityProperties`):**
- `data.generator.*` — Thread pools, plugin dirs, preview limits, governance, AI runtime/quota
- `data.generator.console-security.*` — Header RBAC (`enabled`, `role-header`, `actor-header`)
- `data.generator.distributed.*` — Coordinator/worker toggles, lease, heartbeat
- `data.generator.schedule.*` — Cron scheduler
- `spring.datasource.dynamic.*` — Named JDBC pools (Druid)
- `spring.kafka.multiple.*` — Named Kafka clusters (`primary`, `clusters.*.bootstrap-servers`)

**Build configuration:**
- Root enforcer: JDK 25+, Maven 3.6.3+ (`pom.xml` `maven-enforcer-plugin`)
- Console embed skip flag: `-Dskip.console.frontend=true` (`data-generator-service/pom.xml`)
- JDBC bundle output: `${project.build.outputDirectory}/jdbc-bundled`
- Frontend proxy: `data-generator-console-web/vite.config.ts` proxies `/api` → `http://localhost:9876`

**Repository / artifact resolution:**
- Internal Nexus (HTTP): `http://172.25.20.192:8081/nexus/...` — releases + snapshots (`pom.xml` `<repositories>`, `<distributionManagement>`)
- Maven Central, Spring Milestones, Spring Snapshots (for Boot 4 / optional Spring AI)
- SCM: internal GitLab-style host (`pom.xml` `<scm>`)

**Logging:**
- `data-generator-service/src/main/resources/logback-spring.xml` — Console + rolling file appenders under `../logs/`

## Platform Requirements

**Development:**
- **JDK 25** (mandatory)
- **Node.js 22+** and npm (for console dev; Maven can install Node via `frontend-maven-plugin`)
- **Maven 3.6.3+** via wrapper
- **Windows:** prefer `.\mvnw-jdk25.ps1` (sets `JAVA_HOME`, applies `.mvn/settings-jdk25.xml`)
- **Docker** (optional): Testcontainers integration tests; Podman for local E2E (`scripts/e2e-podman.ps1`)
- **Ollama** (optional): Live AI ITs when `localhost:11434` reachable (`docs/jdk25-upgrade.md`)

**Production / packaging:**
- Deliverable: Maven assembly **tar.gz / zip** (`data-generator-service/assembly.xml`), not fat Spring Boot JAR (`spring-boot-maven-plugin` repackage skipped).
- Layout: `bin/*.jar`, `lib/*`, `conf/application*.yaml`, `jdbc-bundled/**`, `bin/*.sh` / `*.ps1`.
- Default service port: **9876** (`application.yaml`); staging profile uses **8080**.
- Console static assets embedded at `classpath:static/console/` (built from `data-generator-console-web/target/console-dist`).
- JVM heap defaults in `service.env.example`: `DG_HEAP_MIN=512m`, `DG_HEAP_MAX=1g`.
- Roles: `DG_SERVICE_ROLE=coordinator` (default) or `worker` with `DG_MAIN_CLASS=org.gensokyo.data.DataGeneratorWorkerApplication`.

**CI (GitHub Actions):**
- `.github/workflows/maven-test.yml` — JDK 25 Temurin, console-web package, full `mvn test` with `-Dskip.console.frontend=true`
- `.github/workflows/console-verify.yml` — JDK 25 + Node 22 + Podman + PowerShell; runs `scripts/verify-console.ps1`

**Access URLs (local default):**
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

---

*Stack analysis: 2026-06-17*
*Update after major dependency changes (Spring Boot, JDK, React, Calcite upgrades)*
