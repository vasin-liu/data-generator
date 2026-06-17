# Structure — data-generator

**Generated:** 2026-06-17  
**Repository root:** `D:/Work/99_Code/01_Java/data-generator`  
**Coordinates:** `org.gensokyo.data.generator` / `${revision}` (currently `3.0.0-SNAPSHOT`)

---

## 1. Repository layout (top level)

```
data-generator/
├── pom.xml                          # Root aggregator + dependencyManagement
├── mvnw, mvnw.cmd, mvnw-jdk25.ps1    # Maven wrapper (JDK 25 helper on Windows)
├── .mvn/settings-jdk25.xml          # Corporate Nexus / HTTP repo settings
├── AGENTS.md, CLAUDE.md, README.md  # Contributor / agent guides
├── .cursor/rules/                   # Cursor policy (Java Javadoc, commits, CodeGraph)
├── .github/workflows/               # CI (incl. console-verify.yml)
├── .planning/codebase/              # Architecture docs (this directory)
├── docs/                            # Upgrade notes, testing guides
├── scripts/                         # verify-*.ps1, e2e-podman.ps1
├── samples/template-v2-pf4j-plugin/ # PF4J extension sample
├── uploaded-drivers/, uploaded-sources/  # Runtime upload dirs (local dev)
│
├── data-generator-dependencies/     # BOM-style managed versions
├── data-generator-common/           # Shared core JARs
├── data-generator-calcite/          # Template V2 Calcite runtime
├── data-generator-service/          # Runnable Spring Boot app ★
├── data-generator-console-web/      # React SPA (build → embedded in service JAR)
│
├── data-generator-stage/            # Extended V1 stage implementations
├── data-generator-iterator/         # Iterator plugins (aggregator)
├── data-generator-reader/             # Reader plugins (aggregator)
├── data-generator-writer/           # Writer plugins (aggregator)
├── data-generator-generator/        # Sync/async generator plugins
├── data-generator-scripter/         # SpEL + JavaScript scripters
├── data-generator-faker/            # DataFaker integration
├── data-generator-geo/              # GeoJSON / synthetic geometry
│
├── data-generator-datasource/       # Empty aggregator (placeholder)
└── data-generator-converter/        # Empty aggregator (Converter impls live in core)
```

**Rule of thumb:** Feature code belongs in the **smallest owning module**. Only `data-generator-service` packages a runnable application. Cross-cutting dependency version changes go in root `pom.xml` and/or `data-generator-dependencies/pom.xml`.

---

## 2. Maven module tree

| Module | Packaging | Child modules / artifacts |
|--------|-----------|---------------------------|
| `data-generator` | `pom` | Lists all top-level modules (see root `pom.xml` `<modules>`) |
| `data-generator-dependencies` | `pom` | Version BOM imported by parent |
| `data-generator-common` | `pom` | `data-generator-core`, `data-generator-database-core` |
| `data-generator-iterator` | `pom` | `-constant`, `-csv`, `-database`, `-datetime`, `-excel`, `-geo`, `-json`, `-number` |
| `data-generator-reader` | `pom` | `-ai`, `-csv`, `-database`, `-elasticsearch`, `-excel`, `-json`, `-spel` |
| `data-generator-writer` | `pom` | `-csv`, `-database`, `-elasticsearch`, `-excel`, `-json`, `-kafka` |
| `data-generator-generator` | `pom` | `-async`, `-sync` |
| `data-generator-scripter` | `pom` | `-javascript`, `-spel`, `-velocity` (velocity submodule exists in tree) |
| `data-generator-datasource` | `pom` | *(none)* |
| `data-generator-converter` | `pom` | *(none)* |
| `data-generator-stage` | `jar` | Single module |
| `data-generator-calcite` | `jar` | Single module |
| `data-generator-faker` | `jar` | Single module |
| `data-generator-geo` | `jar` | Single module |
| `data-generator-service` | `jar` (boot) | Pulls all runtime plugins + embeds console |
| `data-generator-console-web` | `pom` (frontend) | npm/Vite project |

---

## 3. Naming conventions

### 3.1 Maven artifacts

- Pattern: `data-generator-{domain}` or `data-generator-{domain}-{implementation}`
- GroupId: `org.gensokyo.data.generator` (consistent across modules)
- Parent reference: `${revision}` from root (currently `3.0.0-SNAPSHOT`)

### 3.2 Java packages

| Layer | Base package | Example |
|-------|--------------|---------|
| Service app | `org.gensokyo.data` | `org.gensokyo.data.controller.TaskController` |
| Console API | `org.gensokyo.data.api.console` | `ConsoleJobController` |
| Core domain | `org.gensokyo.data.{stage,reader,writer,iterator,generator,pipeline,value}` | `org.gensokyo.data.stage.ReadStage` |
| V2 models | `org.gensokyo.data.model.v2` | `TemplateV2VO` |
| V1 models | `org.gensokyo.data.model.vo` | `TemplateVO`, `FieldVO` |
| Calcite runtime | `org.gensokyo.data.calcite.{runtime,source,sink,sql,plugin,transform}` | `TemplateV2Runner` |
| Faker | `org.gensokyo.data.faker` | `DataFaker` |
| Geo | `org.gensokyo.data.geo` | `GeoSyntheticGenerator` |

### 3.3 Type naming in plugin modules

| Suffix | Role | Example file |
|--------|------|--------------|
| `*VO` | YAML/JSON config subtype; `@AutoService` target | `CsvReaderVO.java` |
| `*Stage` / `*StageVO` | V1 pipeline stage impl / config | `ConvertStage.java`, `MappingStageVO.java` |
| `*Reader` / `*Writer` | I/O implementation | `JdbcReader` (in `-reader-database`) |
| `*Iterator` | Iteration driver | `NumberIterator` (in `-iterator-number`) |
| `*Factory` | Runtime wiring | `CsvSourceFactory` (Calcite) |
| `*Config` | Spring `@Configuration` for module | `SpelScriptConfig.java` |
| `*PO` | Service persistence entity | `TaskExecutionPO.java` |
| `*Dto` | REST response shape (console) | `AiQuotaStatusDto.java` |

### 3.4 Frontend (console-web)

- Pages: `src/app/pages/{Name}Page.tsx`
- API clients: `src/api/{resource}.ts` → calls `/api/...`
- Editor wizard steps: `src/app/editor/steps/{Name}Step.tsx`
- i18n: `src/i18n/locales/en.json`, `zh-CN.json`

---

## 4. Key locations by concern

### 4.1 Application entry & config

| What | Path |
|------|------|
| Main class | `data-generator-service/src/main/java/org/gensokyo/data/DataGeneratorApplication.java` |
| Spring beans (V2 runtime, AI, parsers) | `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java` |
| Factory beans (V1) | `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/config/FactoryConfig.java` |
| App properties | `data-generator-service/src/main/java/org/gensokyo/data/config/DataGeneratorProperties.java` |
| Console static hosting | `data-generator-service/src/main/java/org/gensokyo/data/config/ConsoleWebConfig.java` |
| Default YAML | `data-generator-service/src/main/resources/application.yaml` |

### 4.2 REST API surface

**Legacy / task APIs** (`controller/`):

| Controller | Base path | File |
|------------|-----------|------|
| `TaskController` | `/task` | `.../controller/TaskController.java` |
| `TaskExecutionController` | `/task` | `.../controller/TaskExecutionController.java` |
| `TemplateController` | `/template` | `.../controller/TemplateController.java` |
| `TemplateEditorController` | `/template/v2/editor` | `.../controller/TemplateEditorController.java` |
| `DataSourceController` | `/datasource` | `.../controller/DataSourceController.java` |
| `DistributedJobController` | `/task/distributed/jobs` | `.../controller/DistributedJobController.java` |
| `HealthController` | `/healthz` | `.../controller/HealthController.java` |

**Console APIs** (`api/console/`):

| Controller | Base path | File |
|------------|-----------|------|
| `ConsoleTemplateController` | `/api/templates` | `.../api/console/ConsoleTemplateController.java` |
| `ConsoleTemplateEditorController` | `/api/templates` | `.../api/console/ConsoleTemplateEditorController.java` |
| `ConsoleTemplateEditorActionsController` | `/api/templates` | `.../api/console/ConsoleTemplateEditorActionsController.java` |
| `ConsoleJobController` | `/api/jobs` | `.../api/console/ConsoleJobController.java` |
| `ConsoleDataSourceController` | `/api/datasources` | `.../api/console/ConsoleDataSourceController.java` |
| `ConsoleScheduleController` | `/api/console/schedules` | `.../api/console/ConsoleScheduleController.java` |
| `ConsoleRuntimeController` | `/api/console` | `.../api/console/ConsoleRuntimeController.java` |
| `ConsoleAiCatalogController` | `/api/console` | `.../api/console/ConsoleAiCatalogController.java` |
| `ConsoleUploadController` | `/api/console/uploads` | `.../api/console/ConsoleUploadController.java` |
| `ConsoleAuditController` | `/api/console/audit` | `.../api/console/ConsoleAuditController.java` |
| `Console.identitySecretController` | `/api/secrets` | `.../api/console/ConsoleSecretController.java` |
| `ConsoleDistributedController` | `/api/console/distributed` | `.../api/console/ConsoleDistributedController.java` |

### 4.3 Service internal packages

```
data-generator-service/src/main/java/org/gensokyo/data/
├── ai/              # AI runtime bridges, quotas, usage, pricing
├── api/console/     # Console REST + dto/
├── audit/           # AuditService
├── cache/           # Templates cache
├── config/          # Spring configuration
├── controller/      # Legacy REST
├── datasource/      # Dynamic DS helpers (service-side)
├── json/            # TemplateJsonCodec
├── messaging/       # Schedule / job messaging hooks
├── model/           # po/, dto/ (service-specific)
├── repository/      # Spring Data repos (Template, TaskExecution, AI quota, …)
├── secret/          # Secret resolution adapters
├── security/        # Console authorization filter
├── task/            # TaskExecutionService, RunReportCollector, distributed jobs
├── template/        # Lifecycle, V2 normalizer/validator, scenario catalog
└── yaml/            # YamlParser, JacksonParser
```

### 4.4 Core shared library (`data-generator-core`)

```
data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/
├── cache/           # DataSet (in-memory reader cache)
├── config/          # FactoryConfig
├── constant/        # Const (stage/reader/writer type strings)
├── context/         # TemplateContext, StageContext, IteratorContext, GeneratorContext
├── converter/       # Converter interface, StringConverter
├── event/           # CompletionEventListener
├── exception/       # DataGeneratorException, NotEnoughElementException
├── generator/       # Generator, AbstractGenerator, GeneratorFactory
├── iterator/        # Iterator, IteratorFactory
├── model/vo/        # V1 template model
├── model/v2/        # V2 template model + workflow + RunReportVO
├── pipeline/        # Row/field/write pipeline factories
├── reader/          # Reader, ReaderFactory, select strategies
├── script/          # Script, ScriptFactory (interfaces)
├── selector/        # Value select strategies
├── stage/           # Stage interface, ReadStage, SelectStage, ScriptStage, WriteStage, StageFactory
├── util/            # ClassKit, TypeKit, RandomKit, DatasetKit, …
├── value/           # Value, SingleValue, ListValue, MapValue
└── writer/          # Writer, WriterFactory, ConsoleWriter
```

### 4.5 Database dialects (`data-generator-database-core`)

```
data-generator-common/data-generator-database-core/src/main/java/org/gensokyo/data/database/
├── DbType.java, DbTypeKit.java
├── dialect/Dialect.java, DialectFactory.java
└── dialect/impl/    # MySQL, Postgres, ClickHouse, Oracle, SQL Server, DM, DB2, …
```

### 4.6 Template V2 runtime (`data-generator-calcite`)

```
data-generator-calcite/src/main/java/org/gensokyo/data/calcite/
├── runtime/         # TemplateV2Runner, *Pipeline, WorkflowRunner, metrics
├── source/          # Row sources + *SourceFactory
├── sink/            # Row sinks + JDBC bulk helpers
├── sql/             # CalcitePlanCompiler, CalciteRowTransformer, *TransformFactory
├── transform/       # JsTransformFactory
├── plugin/          # PF4J / ServiceLoader plugin providers
├── AiRuntimeBridge.java
└── TemplateV2RuntimeRegistry*.java
```

### 4.7 Operator console (frontend)

```
data-generator-console-web/
├── package.json, vite.config.ts
├── src/
│   ├── main.tsx, index.css
│   ├── app/App.tsx              # Route definitions
│   ├── app/pages/               # Home, Templates, Jobs, Datasources, …
│   ├── app/editor/              # Template V2 wizard + YAML panel
│   ├── api/                     # Typed fetch clients → /api/*
│   ├── components/              # Shared UI
│   ├── i18n/                    # en + zh-CN
│   └── theme/                   # Light/dark theming
├── e2e/specs/                   # Playwright tests
└── target/console-dist/         # Production build output (embedded by service)
```

Service embed path: `classpath:/static/console/` (copied during `data-generator-service` package from `../data-generator-console-web/target/console-dist`).

---

## 5. Plugin module anatomy (prescriptive template)

When adding a new **reader**, **writer**, **iterator**, **stage**, or **scripter** implementation:

### Step 1 — Create submodule

Under the appropriate aggregator, add `data-generator-{domain}-{kind}/` with:

```
data-generator-{domain}-{kind}/
├── pom.xml                        # Depends on data-generator-core
└── src/main/java/org/gensokyo/data/{package}/
    ├── {Kind}{Name}VO.java        # extends base *VO, @AutoService(BaseVO.class)
    ├── {Kind}{Name}.java          # implements Reader/Writer/Iterator/Stage/Script
    └── {Kind}{Name}Config.java    # @Configuration + @Bean if Spring bean needed
```

Reference examples:

- Reader: `data-generator-reader/data-generator-reader-csv/src/main/java/org/gensokyo/data/reader/CsvReaderVO.java`
- Writer: `data-generator-writer/data-generator-writer-kafka/src/main/java/org/gensokyo/data/writer/KafkaWriterVO.java`
- Iterator: `data-generator-iterator/data-generator-iterator-number/src/main/java/org/gensokyo/data/iterator/NumberIteratorVO.java`
- Stage: `data-generator-stage/src/main/java/org/gensokyo/data/stage/MappingStageVO.java`
- Scripter: `data-generator-scripter/data-generator-scripter-javascript/src/main/java/org/gensokyo/data/script/JsScriptVO.java`

### Step 2 — Register in aggregator `pom.xml`

Add `<module>` to `data-generator-reader/pom.xml` (or writer/iterator parent).

### Step 3 — Wire into service

Add dependency block in `data-generator-service/pom.xml` (mirror existing reader/writer sections ~lines 34–180).

### Step 4 — Managed dependency (if new artifact)

Add entry to `data-generator-dependencies/pom.xml` if the team uses explicit version management for internal artifacts.

### Step 5 — Tests

- Unit tests colocated: `src/test/java/...`
- Service integration: `data-generator-service/src/test/java/...` with embedded H2

**Do not** import `data-generator-service` from plugin modules.

---

## 6. Adding Template V2 Calcite extensions

For V2 **sources**, **transforms**, or **sinks**:

| Kind | Add class | Register in |
|------|-----------|-------------|
| Source | `{Name}SourceFactory` implements factory pattern in `calcite/source/` | `CoreConfig` bean list or `TemplateV2RuntimePlugin` |
| Transform | `{Name}TransformFactory` in `calcite/sql/` or `calcite/transform/` | Same |
| Sink | `{Name}SinkFactory` in `calcite/sink/` | Same |
| PF4J plugin | Separate JAR + `TemplateV2RuntimePlugin` impl | `samples/template-v2-pf4j-plugin/` as reference |

Runtime entry remains `TemplateV2Runner` — new modes require a new `*Pipeline` class and branch in `TemplateV2Runner.runBound`.

---

## 7. Adding console features

| Change | Where |
|--------|-------|
| New page | `data-generator-console-web/src/app/pages/{Name}Page.tsx` + route in `App.tsx` |
| API client | `data-generator-console-web/src/api/{name}.ts` |
| Backend endpoint | Prefer `data-generator-service/.../api/console/Console{Name}Controller.java` |
| i18n strings | `src/i18n/locales/en.json`, `zh-CN.json` |
| E2E test | `data-generator-console-web/e2e/specs/` |
| Verification | `scripts/verify-console.ps1` |

Console routes are **client-side** under `/console/`; API calls use **`/api/**`** (not `/console/api`).

---

## 8. Adding service-domain features (non-plugin)

| Feature type | Location |
|--------------|----------|
| New persisted entity | `model/po/`, `repository/`, Flyway/Liquibase if used (check `resources/db/`) |
| Business service | `task/`, `template/`, or new subpackage under `org.gensokyo.data` |
| REST (operator) | `api/console/` + `dto/` |
| REST (legacy compat) | `controller/` only if backward compatibility required |
| Scheduled job | `@Scheduled` in service + config properties |

---

## 9. Configuration & build files

| File | Purpose |
|------|---------|
| `pom.xml` (root) | Module list, Java 25, Spring Boot BOM, property versions |
| `data-generator-dependencies/pom.xml` | Internal + third-party managed deps |
| `.mvn/settings-jdk25.xml` | Maven settings for corporate Nexus |
| `mvnw-jdk25.ps1` | Windows helper setting JAVA_HOME for builds |
| `data-generator-service/src/main/resources/application.yaml` | Primary runtime config |
| `data-generator-service/src/test/resources/application-phase7-test.yaml` | Integration test profile |
| `scripts/verify-*.ps1` | Focused verification pipelines (console, AI phases, execution reliability) |

---

## 10. Test layout conventions

| Scope | Location |
|-------|----------|
| Core unit tests | `{module}/src/test/java/` mirroring main package |
| Service `@SpringBootTest` | `data-generator-service/src/test/java/org/gensokyo/data/` |
| Console unit (Java) | `.../api/console/*Test.java`, `scripts/verify-console-unit.ps1` |
| Console E2E | `data-generator-console-web/e2e/specs/*.spec.ts`, Podman via `scripts/e2e-podman.ps1` |
| Calcite container ITs | e.g. `ChunkedPipelineMySqlContainerTests.java`, Postgres variants |

Prefer **embedded** H2/Kafka/WireMock over external infra in unit tests (see `docs/testing-embedded-components.md`).

---

## 11. Samples & extension points

| Path | Purpose |
|------|---------|
| `samples/template-v2-pf4j-plugin/` | PF4J plugin skeleton for custom V2 runtime components |
| `docs/jdk25-upgrade.md` | JDK/Spring upgrade notes |
| `docs/testing-embedded-components.md` | Embedded test policy |

---

## 12. Quick decision guide — where does my code go?

| I need to… | Module / path |
|------------|---------------|
| Add JDBC writer for new DB flavor | `data-generator-writer/data-generator-writer-database/` |
| Add file format reader | New `data-generator-reader-{format}/` |
| Add iteration strategy | New `data-generator-iterator-{kind}/` |
| Add V1 stage type | `data-generator-stage/` (+ core `StageVO` if new base type) |
| Add SpEL/JS function for V1 scripts | `data-generator-scripter-*` or `data-generator-faker` |
| Add SQL transform for V2 | `data-generator-calcite/.../sql/` or `transform/` |
| Add V2 JDBC/CSV/Kafka sink | `data-generator-calcite/.../sink/` + plugin provider |
| Add operator UI page | `data-generator-console-web/src/app/pages/` |
| Add job/quota/audit logic | `data-generator-service/src/main/java/org/gensokyo/data/` |
| Add shared VO used by V1 and V2 | `data-generator-core/.../model/` |
| Add SQL dialect helper | `data-generator-database-core/.../dialect/impl/` |
| Change REST URL for console | `api/console/*Controller.java` + matching `console-web/src/api/*.ts` |

---

## 13. Java source file requirements (repo policy)

Every `.java` file must include (see `.cursor/rules/java-copyright-class-javadoc.mdc`):

1. PCI copyright block above `package`
2. Class-level Javadoc on the primary public type (`@author`, `@since` / `@version`)
3. Javadoc on all `public` methods and constructors
4. Inline `//` comments on non-obvious logic

---

## 14. Static & runtime directories (do not commit generated output)

| Path | Notes |
|------|-------|
| `{module}/target/` | Maven build output — never edit |
| `data-generator-console-web/node_modules/` | npm install artifact |
| `data-generator-console-web/target/console-dist/` | Vite production bundle consumed by service |
| `uploaded-drivers/`, `uploaded-sources/` | Local runtime uploads for JDBC drivers / source files |
| `logs/` | Runtime logs (local) |

---

*Document generated by GSD codebase mapper. Update when modules are added, retired, or API prefixes change.*
