# Architecture — data-generator

**Generated:** 2026-06-17  
**Entry point:** `data-generator-service/src/main/java/org/gensokyo/data/DataGeneratorApplication.java`  
**Stack:** Java 25, Maven multi-module, Spring Boot 4.x, React operator console (Vite)

---

## 1. Executive summary

The **data-generator** monorepo is a modular synthetic-data platform. Operators define **Template V2** YAML/JSON documents (sources → transforms → sinks, optional workflow/compute blocks). The Spring Boot service in `data-generator-service` persists templates, exposes REST APIs and an embedded React console, and executes runs asynchronously via **Apache Calcite–backed pipelines** in `data-generator-calcite`.

A legacy **Template V1** field/iterator/stage model remains in `data-generator-common/data-generator-core` for configuration types and internal reuse, but **V1 task execution is retired** — `TaskController` rejects V1-only templates at run time.

The dominant architectural pattern is **plugin-style extensibility**: configuration value objects (VOs) are registered with `@AutoService`, runtime implementations are discovered via generic type matching (`TypeKit`, `ClassKit`) and wired through Spring `ApplicationContext` factories.

---

## 2. High-level system diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Operator / API clients                              │
│   Browser SPA (/console/**)          REST (/api/**, /task/**, /template/**) │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    data-generator-service (Spring Boot)                     │
│  Controllers ──► Template/Task services ──► TaskExecutionService (JDBC)     │
│  CoreConfig ──► TemplateV2Runner, AI bridges, PF4J plugin registry          │
│  ConsoleWebConfig ──► classpath:/static/console/ (React build artifact)     │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────────┐
│ data-generator-  │  │ data-generator-  │  │ Pluggable runtime modules     │
│ calcite          │  │ common/core      │  │ reader / writer / iterator /  │
│ (V2 execution)   │  │ (shared types,   │  │ stage / scripter / generator  │
│                  │  │  V1 pipelines)   │  │ / faker / geo                   │
└──────────────────┘  └──────────────────┘  └──────────────────────────────┘
          │                     │                     │
          └─────────────────────┴─────────────────────┘
                                │
                                ▼
                    JDBC, Kafka, Elasticsearch, files
                    (CSV/JSON/Excel), AI providers (Ollama/OpenAI-compatible)
```

---

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

---

## 4. Layer model

### 4.1 Presentation layer

| Surface | Location | Notes |
|---------|----------|-------|
| React operator console | `data-generator-console-web/src/` | Built to `target/console-dist`, embedded at `classpath:/static/console/` |
| Console REST API | `data-generator-service/src/main/java/org/gensokyo/data/api/console/` | Prefix `/api/*` — templates, jobs, datasources, schedules, audit, AI catalog |
| Legacy/admin REST | `data-generator-service/src/main/java/org/gensokyo/data/controller/` | `/task`, `/template`, `/datasource`, `/healthz` |
| Health | `HealthController.java` | `GET /healthz` |

Console routes (client-side): defined in `data-generator-console-web/src/app/App.tsx` under `/console/*`.

### 4.2 Application / orchestration layer (`data-generator-service`)

Responsible for:

- **Template CRUD & lifecycle** — `TemplateController`, `ConsoleTemplateController`, `TemplateLifecycleService`
- **Run orchestration** — `TaskController.run()` detects V1 vs V2 via `TemplateDefinitionDetector`, queues `TaskExecutionPO`, submits async work
- **V2 execution tracking** — `TaskExecutionService`, `RunReportCollector`, cancel/pause via `WorkflowRunControl`
- **Distributed runs** — optional `DistributedJobService` when `distributedExecutionProperties.isEnabled()`
- **AI governance** — `AiQuotaService`, `AiUsageService`, rate limiters in `org.gensokyo/data/ai/`
- **Datasource registry** — dynamic JDBC/ES/Kafka client registries wired in `CoreConfig`
- **Audit** — `AuditService` records operator actions

Key configuration: `CoreConfig.java`, `DataGeneratorProperties.java`, `FactoryConfig.java` (re-exported from core).

### 4.3 Domain / execution layer

#### Template V2 (active path)

Entry: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2Runner.java`

```
TemplateV2VO
    │
    ├─ workflow != null ──► WorkflowRunner
    │                         (pause, log, branch, shared scope, compute blocks)
    │
    └─ else by executionPolicy.mode:
           STREAMING  ──► StreamingPipeline   (chunked JDBC read, row-local SQL, batched sink write)
           IN_MEMORY  ──► InMemoryPipeline   (materialize sources, transform, write)
           CHUNKED    ──► ChunkedPipeline     (partitioned compute for large joins)
```

Runtime abstractions in `data-generator-calcite`:

| Abstraction | Package | Role |
|-------------|---------|------|
| `RowSource` | `org.gensokyo.data.calcite.source` | Read rows (JDBC query, CSV, JSON, iterator bridge, AI, inline) |
| `RowSink` | `org.gensokyo.data.calcite.sink` | Write rows (JDBC bulk, CSV, JSON, console, Kafka, ES) |
| `CalciteRowTransformer` | `org.gensokyo.data.calcite.sql` | SQL/SpEL/JS transforms via Calcite |
| `TemplateV2RuntimeRegistry` | `org.gensokyo.data.calcite.runtime` | Factory registry for sources/transforms/sinks |
| `TemplateV2RuntimePlugin` | `org.gensokyo.data.calcite.plugin` | PF4J / ServiceLoader extension points |

Built-in factories registered in `CoreConfig` include `CsvSourceFactory`, `JsonSourceFactory`, `IteratorSourceFactory`, `AiSourceFactory`, `SqlTransformFactory`, `SpelTransformFactory`, `JsTransformFactory`, JDBC/CSV/JSON/Console sink factories, plus plugin providers for JDBC templates, Kafka, Elasticsearch, and PF4J directory scanning.

#### Template V1 (retired execution, retained libraries)

V1 model lives in `data-generator-common/data-generator-core`:

- **Template** — `TemplateVO` with `fields`, `iterator`, `generator`, `output`
- **Iterator tree** — nested `IteratorVO` with optional `choose`/`otherwise` conditions (script-evaluated)
- **Per-field stage chains** — READ → SELECT → SCRIPT → CONVERT → …
- **Row assembly** — `DefaultRowPipelineFactory` builds one `MapValue` row per iteration
- **Write** — `DefaultWritePipelineFactory` invokes `WriteStage` → `Writer` implementations

V1 generator loop (`AbstractGenerator`):

1. `IteratorFactory` drives nested iteration
2. Iterator-level stages run through `DefaultRowPipeline`
3. `defaultRowPipelineFactory.startup` builds full row (field DAG)
4. Producer queues rows; consumer batches and calls `defaultWritePipelineFactory`

**Service policy:** `TaskController.run()` throws if only V1 fields are detected — operators must migrate to V2.

### 4.4 Infrastructure / plugin layer

Pluggable modules follow a consistent layout:

```
data-generator-{concern}/
  pom.xml                    # aggregator
  data-generator-{concern}-{kind}/
    src/main/java/org/gensokyo/data/{package}/
      *VO.java               # @AutoService registration
      *Impl.java             # Reader/Writer/Iterator/Stage/Script bean or class
      *Config.java           # optional Spring @Configuration
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

Supporting libraries:

- **`data-generator-faker`** — DataFaker integration, SpEL variables (`DataFaker.java`, providers)
- **`data-generator-geo`** — GeoJSON/WKT generation, used by iterator and Calcite SQL functions
- **`data-generator-common/data-generator-database-core`** — JDBC dialects (`Dialect`, `DialectFactory`)

Empty aggregator placeholders (no submodules yet): `data-generator-datasource/pom.xml`, `data-generator-converter/pom.xml`.

---

## 5. Core data abstractions

### 5.1 `Value` hierarchy

Package: `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/value/`

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

Defined in `Const.StageType` (`data-generator-core/.../constant/Const.java`):

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

Detection logic: `TemplateDefinitionDetector.detect(TemplateVO, TemplateV2DraftVO)`.

---

## 6. Data flow — Template V2 pipeline execution

Detailed run path from HTTP trigger to sink write:

```
1. POST /task/run/{templateId}  OR  Console → /api/templates/{id}/run
        │
        ▼
2. TaskController.run(entity)
        │ parse YAML → TemplateV2DraftVO
        │ TemplateDefinitionDetector → V2
        │ TemplateV2Normalizer.normalize → TemplateV2VO
        │ TemplateV2Validator.validate (+ governance)
        ▼
3. TaskExecutionService.queueExecution (status=QUEUED, lineage snapshots)
        │
        ├─ distributed enabled → DistributedJobService.enqueue
        └─ else → ThreadPoolTaskExecutor.submit(runV2Tracked)
        ▼
4. runV2Tracked(template, instanceId)
        │ TaskExecutionService.markRunning
        │ WorkflowRunContext.bind(instanceId, control)
        ▼
5. TemplateV2Runner.run(template)
        │ AiExecutionScope.bind (tenant/template context for AI metrics:        │ EffectiveExecutionPolicy.resolve
        │
        ├─ template.workflow != null
        │       └─ WorkflowRunner.run → ComputeBlockRunner → sinks
        │
        └─ else (transformers required)
                ├─ STREAMING  → StreamingPipeline
                │     RowSource (chunk read) → CalciteRowTransformer → RowSink (batch write)
                ├─ IN_MEMORY  → InMemoryPipeline
                │     materialize sources → transform DAG → sink
                └─ CHUNKED    → ChunkedPipeline
                      partitioned compute blocks
        ▼
6. TemplateV2RunResult (metrics, row count, optional preview rows)
        │
        ▼
7. TaskExecutionService.markCompleted / markFailed
   RunReportCollector persists RunReportVO (incl. AI metrics)
   AuditService records completion
```

**Streaming mode constraints** (see `StreamingPipeline` Javadoc): single JDBC query source, row-local SQL, bounded in-memory chunk size — enforced to prevent OOM on large sources.

---

## 7. Data flow — Template V1 generator (library path, not service-run)

Retained for iterator/reader/writer modules and `IteratorSourceFactory` bridge into V2:

```
IteratorFactory.newInstance(IteratorContext)
    → nested doIteration (conditions via ScriptFactory)
    → per-iterator stages: DefaultRowPipeline (StageFactory chain)
    → leaf: defaultRowPipelineFactory.startup (field DAG, topological sort)
    → queue row
    → consumer batch → defaultWritePipelineFactory.startup
    → WriteStage → WriterFactory → Writer (JDBC/CSV/Kafka/…)
```

Sync vs async: `SyncGenerator` vs `AsyncGenerator` in `data-generator-generator-sync` / `-async` — differ in `doJob` / thread pool sizing on `AbstractGenerator`.

---

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

---

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

---

## 10. Dependency direction (allowed imports)

```
data-generator-service
    → data-generator-calcite
    → data-generator-core, database-core
    → all *-reader, *-writer, *-iterator, *-stage, *-scripter, *-generator, faker, geo plugin JARs

data-generator-calcite
    → data-generator-core (model v2, shared utilities)
    → may call into iterator/reader bridges (IteratorSourceFactory)

Plugin modules (*-reader, *-writer, …)
    → data-generator-core ONLY (never service)

data-generator-console-web
    → no Java dependency; HTTP client to `/api/*` only
```

---

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

---

## 12. Evolution notes (2026-06-17)

- **V1 execution retired** at service boundary; V1 code remains for types, tests, and V2 bridges (e.g. iterator source).
- **V2 is the product path**: workflow/compute blocks, execution policies, Calcite SQL transforms, AI sources with quota tracing.
- **Console is first-class**: template editor, job center, datasource admin, schedules, audit — all backed by `/api/*` controllers.
- **PF4J** enables out-of-tree Template V2 runtime extensions; sample at `samples/template-v2-pf4j-plugin/`.

---

*Document generated by GSD codebase mapper. Re-scan after major module additions or template schema changes.*
