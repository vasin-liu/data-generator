# data-generator

A **Java data-generation platform** for test data, sample datasets, and batch data pipelines. Define **read → transform → write** flows in YAML templates, connect JDBC / Elasticsearch / Kafka / files, and trigger runs from the **operator console** or **REST API**.

Current release line: **3.0.0-SNAPSHOT** (**Template V2**). The legacy V1 field-stage model is retired; task execution accepts V2 templates only.

**Languages:** [中文](README.md) · **English** · [Quickstart](docs/quickstart.md)

---

## Key capabilities

| Capability | Description |
|------------|-------------|
| **Template V2** | Declarative `sources` / `transform` / `sink`; SQL (Calcite), SpEL, and JavaScript transforms |
| **Workflow & compute blocks** | L2 `workflow` steps (log, pause, branch, shared scope) + L1 `computeBlocks` / `transformGraph` DAG |
| **Multi-source reads** | JDBC queries, iterators, CSV/Excel/JSON, Elasticsearch, GeoJSON/PostGIS, AI generation, and more |
| **Multi-target writes** | MySQL, PostgreSQL, ClickHouse, Elasticsearch, Kafka, CSV/Excel/JSON, console |
| **Operator console** | React SPA: template editing, datasource admin, job history, cron schedules, audit |
| **Governance** | Draft/publish lifecycle, secret references, structured run reports and metrics |
| **Scheduling** | Cron-driven template triggers (feature-flagged) |
| **Distributed execution** | Coordinator/worker queue with lease and heartbeat (feature-flagged) |
| **Geospatial** | Synthetic GEO iterators, GeoJSON/PostGIS sources, V2 geo SQL functions |
| **Plugins** | PF4J custom transformers; modular Reader / Writer / Iterator extensions |

---

## Tech stack

| Area | Technology |
|------|------------|
| Language & build | Java **25**, Maven multi-module, `mvnw` / `mvnw-jdk25.ps1` |
| Runtime | Spring Boot **4.x** (`data-generator-service`) |
| Console UI | React 19, Vite, Ant Design (`data-generator-console-web`) |
| Transform engine | Apache Calcite, GraalJS, SpEL, DataFaker |
| Data access | Dynamic JDBC (Druid), Elasticsearch, Kafka |
| Serialization | Jackson 3.x, YAMLBeans, JSON Schema |

---

## Repository layout

```
data-generator/
├── data-generator-service/          # Runnable Spring Boot app (REST + embedded console)
├── data-generator-console-web/      # Operator console React sources
├── data-generator-calcite/          # Template V2 runtime and SQL engine
├── data-generator-common/           # Shared models and utilities
├── data-generator-datasource/       # Datasource abstractions
├── data-generator-stage/            # V1 stage module (runtime retired)
├── data-generator-reader/           # Readers (JDBC, CSV, Excel, ES, AI, …)
├── data-generator-writer/           # Writers (JDBC, Kafka, ES, files, …)
├── data-generator-iterator/         # Iterators (number, datetime, GEO, database, …)
├── data-generator-scripter/         # Script engines (GraalJS, SpEL, Velocity)
├── data-generator-geo/              # Geospatial utilities and predicates
├── data-generator-faker/            # DataFaker integration
├── data-generator-converter/        # Type converters
├── data-generator-generator/        # Generation orchestration
├── data-generator-dependencies/     # Dependency BOM
├── docs/                            # Design notes, migration, and topic guides
└── samples/                         # Sample plugins, etc.
```

---

## Quick start

See **[docs/quickstart.md](docs/quickstart.md)** for a step-by-step first-run guide.

### Prerequisites

- **JDK 25**
- **Node.js 22+** (only when developing the console UI locally)
- Maven settings: `.mvn/settings-jdk25.xml` (internal Nexus may use HTTP)

### Build and run

`data-generator-service` embeds `data-generator-console-web/target/console-dist` during `process-classes`. **Build the console frontend before packaging the service alone**, or Maven fails with `console-dist does not exist`.

```powershell
# Show Maven / Java versions
.\mvnw-jdk25.ps1 -v

# Recommended: frontend + service in one reactor (skip tests)
.\mvnw-jdk25.ps1 -pl data-generator-console-web,data-generator-service -am -DskipTests package

# Start the service (default port 9876; requires a successful package)
.\mvnw-jdk25.ps1 -pl data-generator-service spring-boot:run
```

Backend-only iteration without `/console/` static assets (not for console verification):

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests -Dskip.console.frontend=true package
```

### Endpoints

| Surface | URL |
|---------|-----|
| Operator console | http://localhost:9876/console/ |
| Console API | http://localhost:9876/api/… |
| Legacy REST | `/template/…`, `/task/…`, `/datasource/…` |
| H2 console (dev) | http://localhost:9876/h2 |

### Local UI dev server

Keep the backend on port **9876**, then in another terminal:

```powershell
cd data-generator-console-web
npm install
npm run dev
```

Open http://localhost:5173/console/ — Vite proxies `/api` to the backend.

### Verification

```powershell
# Full test run: build console-dist first, then test the whole repo
.\mvnw-jdk25.ps1 -pl data-generator-console-web -DskipTests package
.\mvnw-jdk25.ps1 test

# Or one reactor (console-web + service and dependencies)
.\mvnw-jdk25.ps1 -pl data-generator-console-web,data-generator-service -am test

# Faster console slice (web build + console-related Java tests)
.\scripts\verify-console-unit.ps1 -IncludeWebBuild
```

More build notes: [`docs/jdk25-upgrade.md`](docs/jdk25-upgrade.md).

---

## Operator console

The console manages the full **Template V2** lifecycle. V1 templates and the migration workbench are no longer available.

Recommended flow:

1. **Datasources** — register JDBC connections (common driver presets supported)
2. **Templates** — wizard: General → Sources → Transform → Sinks → Execution
3. **Validate & publish** — Review tab: Validate → Save → Publish (`PUBLISHED` required for production runs)
4. **Run** — manual Run or cron schedule
5. **Jobs** — inspect run reports, error samples, and distributed job metadata

Full UI guide: [`docs/operator-console-usage.md`](docs/operator-console-usage.md)

---

## Template V2 overview

V2 pipelines follow **sources → transform (or transformers / transformGraph) → sink**. Complex scenarios combine **workflow + computeBlocks**.

### Minimal example (synthetic data + SQL projection + console sink)

```yaml
name: demo-synthetic
sources:
  seed:
    type: iterator
    iterator:
      type: number
      from: 1
      to: 5
      step: 1
transform:
  type: sql
  sql: SELECT value AS id, value * 10 AS score FROM seed
sink:
  writers:
    - type: console
```

More samples: `data-generator-service/src/main/resources/template/v2-scenarios/` (e.g. `scenario-a-synthetic.yaml`, `scenario-b-lookup-join.yaml`).

### Capability layers

| Layer | Purpose | YAML location |
|-------|---------|---------------|
| **Linear pipeline** | Single sources → transform → sink | Root `sources` / `transform` / `sink` |
| **L1 transform DAG** | Multi-node transform graph | `transformGraph` or `computeBlocks[].transformGraph` |
| **L2 workflow** | Step orchestration, branches, pauses | Root `workflow.steps` + `computeBlocks` |

Topic guides:

- Workflow authoring: [`docs/template-v2-workflow-authoring-guide.md`](docs/template-v2-workflow-authoring-guide.md)
- Transformer strategy (SQL / SpEL / JS): [`docs/template-v2-transformer-strategy.md`](docs/template-v2-transformer-strategy.md)
- Scenario catalog: [`docs/template-v2-scenario-template-catalog.md`](docs/template-v2-scenario-template-catalog.md)
- JDBC chunked execution: [`docs/template-v2-jdbc-chunked-execution-guide.md`](docs/template-v2-jdbc-chunked-execution-guide.md)
- Streaming execution: [`docs/template-v2-streaming-execution-guide.md`](docs/template-v2-streaming-execution-guide.md)

---

## Datasource configuration

The service uses **dynamic multi-datasources**. Each entry has a unique **name** referenced in templates via `dataSourceId` or inline `dataSource`.

### JDBC (metadata store + business databases)

```yaml
spring:
  datasource:
    dynamic:
      primary: data-generator
      datasource:
        data-generator:
          url: jdbc:h2:file:./db/data-generator
          username: sa
          password: ""
          driver-class-name: org.h2.Driver
          type: com.alibaba.druid.pool.DruidDataSource
          init:
            schema: classpath:db/schema.sql
        my-mysql:
          url: jdbc:mysql://localhost:3306/demo?useSSL=false&serverTimezone=UTC
          username: demo
          password: "${DB_PASSWORD}"
          driver-class-name: com.mysql.cj.jdbc.Driver
          type: com.alibaba.druid.pool.DruidDataSource
```

Supported JDBC dialects include **MySQL, PostgreSQL, ClickHouse, H2, DM (Dameng), Kingbase, HighGo**, and others (see `data-generator-writer-database` modules).

In production, use console **secret references** or environment variables for passwords — avoid plaintext secrets in template YAML. See [`docs/template-v2-datasource-and-secret-governance.md`](docs/template-v2-datasource-and-secret-governance.md).

### Elasticsearch

```yaml
spring:
  elasticsearch:
    multiple:
      primary: es1
      clusters:
        es1:
          uris:
            - https://localhost:9200
          username: elastic
          password: "${ES_PASSWORD}"
```

### Kafka

```yaml
spring:
  kafka:
    multiple:
      primary: kafka1
      clusters:
        kafka1:
          bootstrap-servers:
            - localhost:9092
```

You can also register JDBC datasources dynamically in the console **Datasources** page without editing `application.yaml`.

---

## Running tasks

### Option 1: Operator console (recommended)

Click **Run** on the template Review tab or list page, then open the job detail for `SUCCESS` / `FAILED` status and the structured run report.

### Option 2: REST API

```http
# Start by template id (POST preferred)
POST /task/run/{templateId}

# Start by unique template name
GET  /task/runByName/{templateName}

# List templates
GET  /task/list
```

When `data.generator.governance.require-published-for-task-run=true` (default), only **published** templates are accepted by `/task/run`; draft runs from the editor use console-specific endpoints.

---

## Runtime configuration

Common `application.yaml` settings (`data.generator` prefix):

```yaml
data:
  generator:
    v1-execution:
      enabled: false          # V1 path retired; kept for config compatibility
    governance:
      require-published-for-task-run: true
      reject-plaintext-passwords-in-templates: true
    schedule:
      enabled: false          # set true to enable cron polling
      poll-delay-ms: 60000
    distributed:
      enabled: false          # set true for queue-backed execution
      worker-enabled: false
      lease-seconds: 30
    preview-max-rows: 100
    v2-plugin-directories: [] # PF4J plugin directories
```

Distributed deployment: [`docs/staging-distributed-deployment.md`](docs/staging-distributed-deployment.md)

Query runtime flags via `GET /api/console/runtime`; the console home page shows schedule and distributed mode status.

---

## Geospatial

Synthetic point generation, GeoJSON/PostGIS reads, and built-in `V2_GEO_*` Calcite SQL functions (distance, containment, buffer, etc.).

- Overview: [`docs/geospatial-overview.md`](docs/geospatial-overview.md)
- Usage: [`docs/geospatial-phase1-usage.md`](docs/geospatial-phase1-usage.md)

---

## Extending the platform

| Extension | Module | Notes |
|-----------|--------|-------|
| Reader | `data-generator-reader-*` | Implement the `Reader` interface |
| Writer | `data-generator-writer-*` | Implement the `Writer` interface |
| Iterator | `data-generator-iterator-*` | Register iterator types |
| Transformer plugin | PF4J | See `docs/template-v2-pf4j-custom-transform-guide.md` |
| Calcite UDF | `data-generator-calcite` | SQL function registration |

Sample PF4J plugin: `samples/template-v2-pf4j-plugin/`

AI reader tests are skipped automatically when no model endpoint (e.g. Ollama on `localhost:11434`) is reachable.

---

## Documentation index

| Topic | Document |
|-------|----------|
| Quickstart | [`docs/quickstart.md`](docs/quickstart.md) |
| Console usage | [`docs/operator-console-usage.md`](docs/operator-console-usage.md) |
| Template V2 roadmap | [`docs/template-v2-product-roadmap.md`](docs/template-v2-product-roadmap.md) |
| Calcite status | [`docs/calcite-implementation-status.md`](docs/calcite-implementation-status.md) |
| V1 → V2 mapping | [`docs/calcite-v1-v2-mapping.md`](docs/calcite-v1-v2-mapping.md) |
| Embedded testing | [`docs/testing-embedded-components.md`](docs/testing-embedded-components.md) |
| JDK 25 upgrade | [`docs/jdk25-upgrade.md`](docs/jdk25-upgrade.md) |
| V1 migration archive | [`docs/archive/migration/`](docs/archive/migration/) |
| Agent contributor guide | [`AGENTS.md`](AGENTS.md) |

---

## License

Copyright © 2021–2026 PCI Technology Group Co.,Ltd. All Rights Reserved.

Internal Maven distribution and SCM settings are defined in the root `pom.xml`.
