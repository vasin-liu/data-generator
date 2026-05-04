# Calcite / Template V2 Implementation Status

## Purpose

This document captures:

- what has already been implemented on the current Template V2 / Calcite path
- what should be implemented next in execution order

It is a status document for the in-repo implementation, not a replacement for the longer design and refactor plans.

Related references:

- `docs/calcite-refactor-plan.md`
- `docs/calcite-coverage-matrix.md`
- `docs/calcite-v1-v2-mapping.md`
- `docs/calcite-skeleton-implementation-plan.md`
- `docs/calcite-templatev2-model-design.md`

## Current Status

### Progress snapshot

As of the current implementation checkpoint:

- Phase 1 minimum viable V2 is effectively complete except for additional iterator adapters beyond number
- Phase 2 practical source/sink coverage is mostly complete for JDBC, Kafka, Elasticsearch, AI model shape, source policy, and multi-sink failure policy
- Phase 3 transformation migration coverage has a usable baseline through SQL conditional/null/string/conversion/date functions and the shared UDF registry
- the largest remaining gaps are file-backed sink adapters, Excel source, concrete remote AI bridge, richer V1-to-V2 migration examples, and parity scorecard work

### 1. Baseline and scope

The repository has already completed the Boot 4 / JDK 25 baseline transition needed for the V2 path.

The current active direction is:

- treat V2 as the only target architecture
- keep V1 runnable only as a temporary migration scaffold
- use Calcite for SQL parsing and validation
- keep the first V2 runtime as a lightweight in-memory row engine until the new architecture fully absorbs the old feature surface
- implement every new V2 capability behind an explicit extension point wherever practical, so built-in behavior, Spring wiring, and future plugin-provided behavior can share the same contract
- avoid adding new hard-coded source / transform / sink / UDF branches unless they are built-in defaults behind a registry, provider, or runtime contract

### 2. V2 model and parsing

Implemented model set under `data-generator-common/data-generator-core`:

- `TemplateV2VO`
- `TemplateV2DraftVO`
- `SourceVO`
- `IteratorSourceVO`
- `QuerySourceVO`
- `CsvSourceVO`
- `JsonSourceVO`
- `AiSourceVO`
- `AiProviderVO`
- `TransformVO`
- `SqlTransformVO`
- `SourcePolicyVO`
- `SinkExecutionPolicyVO`
- `ColumnDef`
- `RowSchema`
- `Row`

Implemented parsing / normalization / validation support under `data-generator-service`:

- `TemplateDefinitionKind`
- `TemplateDefinitionDetector`
- `TemplateV2Normalizer`
- `TemplateV2Validator`

Implemented codec registration:

- V2 source and transform subtypes are registered in YAML and JSON codec paths
- V2 and V1 template parsing now coexist in the same service layer

### 3. Service integration

The service layer already supports V1 / V2 split execution:

- `TaskController` can detect V1 vs V2 template shape
- `TemplateController` accepts V1 or V2 upload / update payloads
- `Templates` supports cache / reload for V1 or V2 definitions
- `TemplateDTO` supports V2 content decoding

Current runtime wiring:

- `TemplateV2Runner` is provided by `CoreConfig`
- source factories currently wired:
  - iterator source
  - CSV source
  - JSON source
  - JDBC query source
- sink factories currently wired:
  - console sink
  - CSV sink
  - JSON sink
  - JDBC / Kafka / Elasticsearch runtime providers when their runtime services are available

### 4. Calcite module skeleton

Implemented `data-generator-calcite` module:

- `CalciteExecutionContext`
- `CalciteSchemaFactory`
- `CalciteSqlValidator`
- `CalciteSqlValidationResult`
- `CalcitePlanCompiler`
- `CalciteCompiledPlan`
- `CalciteRowTransformer`
- `RowSource`
- `RowSink`
- `IteratorRowSource`
- `QueryRowSource`
- `IteratorSourceFactory`
- `QuerySourceFactory`
- `ConsoleRowSinkAdapter`
- `JdbcRowSinkAdapter`
- `CsvRowSinkAdapter`
- `JsonRowSinkAdapter`
- `TemplateV2Runner`
- `TemplateV2RunResult`

### 5. Source capability currently implemented

#### Iterator source

Implemented:

- `IteratorSourceVO`
- `NumberIteratorVO` path through `IteratorRowSource`

Current status:

- usable as V2 seed source
- verified in runner-level tests

#### Query source

Implemented:

- `QuerySourceVO`
- `QueryRowSource`
- `QuerySourceFactory`

Current behavior:

- uses `NamedParameterJdbcTemplate`
- switches datasource with `DynamicDataSourceContextHolder`
- normalizes result column keys to lowercase
- infers schema when schema is not explicitly provided

Parameter binding status:

- no longer placeholder-only
- `ParamVO.language == null` maps to `null`
- `plain` script content is bound as a constant parameter value
- non-plain scripts go through a lightweight SpEL evaluation path

#### CSV source

Implemented:

- `CsvSourceVO`
- `CsvRowSource`
- `CsvSourceFactory`

Current behavior:

- reads local CSV files from a template-provided `path`
- supports `charset`, single-character `delimiter`, `header`, `maxRows`, and optional explicit `RowSchema`
- supports quoted fields and escaped quotes in the lightweight built-in parser
- rejects invalid multi-character delimiters and unclosed quoted fields with source path and line diagnostics
- CSV parsing is behind the `CsvParser` contract, with `DefaultCsvParser` as the built-in implementation and `CsvSourceFactory(CsvParser)` as the injection point
- is registered as a built-in default source factory while still fitting the runtime plugin source extension contract

Current limitation:

- no streaming read mode yet
- no multiline quoted-field support yet
- no external PF4J parser fixture yet; parser replacement is currently available through factory injection

#### JSON source

Implemented:

- `JsonSourceVO`
- `JsonRowSource`
- `JsonSourceFactory`

Current behavior:

- reads local JSON files from a template-provided `path`
- supports `charset`, `root`, `maxRows`, and optional explicit `RowSchema`
- supports a JSON object array as multiple rows and a single JSON object as one row
- supports lightweight root selection with dot paths and array indexes, for example `payload.items` or `payload.items[0]`
- maps scalar root payloads to a single `value` column
- serializes nested objects and arrays as JSON strings for the current flat row model
- JSON parsing is behind the `JsonParser` contract, with `DefaultJsonParser` as the built-in Jackson 3 implementation and `JsonSourceFactory(JsonParser)` as the injection point
- is registered as a built-in default source factory and as a Spring runtime source factory

Current limitation:

- no streaming read mode yet
- no full JSONPath dependency yet; root selection is intentionally lightweight until nested/structured row semantics are finalized
- nested objects are not expanded into structured row values yet; this remains deferred until the row model intentionally supports complex values

### 6. Transform capability currently implemented

Implemented transform family:

- `SqlTransformVO`
- `CalciteSqlValidator`
- `CalciteRowTransformer`

Current SQL support level:

- projection
- alias via `AS`
- `WHERE`
- arithmetic operators: `+ - * /`
- comparison operators
- `AND` / `OR`
- `CAST` passthrough
- `CAST NOT NULL` passthrough for Calcite-injected nullability casts
- null/string functions: `COALESCE`, `CONCAT`, `UPPER`, `LOWER`, `TRIM`
- conversion-oriented functions: `NULLIF`, `CHAR_LENGTH`, `SUBSTRING`, `ABS`, `FLOOR`, `CEIL`, `ROUND`
- date-oriented functions: `V2_FORMAT_DATE`, `V2_DATE_ADD`, `V2_DATE_SUB`, `V2_DATE_DIFF`, plus `YEAR`, `MONTH`, and `DAYOFMONTH` through `EXTRACT`
- single-table `FROM`
- `FROM table AS alias`
- `INNER JOIN`
- `ON` condition
- qualified column references such as `l.value`

Current execution model:

- Calcite is used for parse + validate
- row execution is still repository-local in-memory evaluation
- this is intentionally a skeleton runtime, not full relational execution
- V2 SQL functions now have a shared `TemplateV2SqlFunctionRegistry` used by both validator and row evaluator
- custom UDFs can be injected through `SqlTransformFactory(TemplateV2SqlFunctionRegistry)` without bypassing Calcite validation
- runtime plugins can now contribute `TemplateV2SqlFunction` definitions, and the registry factory merges them into the default SQL transform path with duplicate-name diagnostics

### 7. Sink capability currently implemented

#### Console sink

Implemented:

- reuse of `WriteStageVO`
- `ConsoleWriterVO`
- `ConsoleRowSinkAdapter`

#### JDBC sink

Implemented:

- `Const.WriterType.JDBC`
- `JdbcWriterVO`
- `JdbcRowSinkAdapter`

Current JDBC sink behavior:

- reuses existing `WriterVO` fields:
  - `dataSourceId`
  - `target`
- inserts rows through `NamedParameterJdbcTemplate.batchUpdate`
- builds `insert into target(columns...) values(:columns...)`
- assumes output column names already match target table column names

Current limitation:

- no explicit upsert / merge / conflict strategy

#### CSV sink

Implemented:

- `Const.WriterType.CSV`
- `CsvSinkFactory`
- `CsvRowSinkAdapter`

Current behavior:

- writes transformed rows to `WriterVO.target`
- uses output schema column order when available
- supports `options.charset`, `options.delimiter`, `options.header`, and `options.append`
- creates parent directories before writing
- is registered as a built-in default sink factory and as a Spring runtime sink factory

Current limitation:

- no streaming writer mode yet
- no external parser/formatter fixture yet
- no old `CsvWriterVO.custom` format compatibility bridge yet

#### JSON sink

Implemented:

- `Const.WriterType.JSON`
- `JsonSinkFactory`
- `JsonRowSinkAdapter`

Current behavior:

- writes transformed rows to `WriterVO.target`
- serializes rows as a JSON array using the current `RowJsonCodec`
- supports `options.charset`
- creates parent directories before writing
- is registered as a built-in default sink factory and as a Spring runtime sink factory

Current limitation:

- no streaming writer mode yet
- uses the current flat/scalar `RowJsonCodec`; nested object fidelity remains deferred until the row model intentionally supports complex values

### 8. Multi-source / multi-transform / multi-sink status

Current status:

- multiple `sources` are supported
- multiple `transformers` are supported as an ordered linear chain
- multiple `sinks` are supported as sequential execution

What is implemented today:

- multiple sources can coexist in the same SQL context
- multiple sources can participate in `INNER JOIN`
- sinks are executed one after another
- sink failure policy supports `FAIL_FAST` and `CONTINUE_ON_ERROR`
- fail-fast sink write failures include sink index, writer index, writer type, model class, and target diagnostics

What is not implemented yet:

- parallel sink execution
- rich partial-success reporting in `TemplateV2RunResult`
- source policy semantics beyond model reservation

### 9. Test status

Implemented and passing focused tests include:

- `CalciteSqlValidatorTests`
- `CalciteExecutionFlowTests`
- `QueryRowSourceTests`
- `TemplateV2RunnerQuerySourceTests`
- `TemplateV2RunnerTests`
- `TemplateV2SupportTests`

The current focused validation command that passed most recently:

```powershell
.\mvnw-jdk25.ps1 --% -pl data-generator-calcite -am -Dtest=TemplateV2RunnerTests,QueryRowSourceTests,TemplateV2RunnerQuerySourceTests,CalciteExecutionFlowTests,CalciteSqlValidatorTests -Dsurefire.failIfNoSpecifiedTests=false test
```

## Completed Work Summary

The following implementation milestones are complete:

1. V2 model skeleton is in place.
2. V1 / V2 parsing and routing are in place.
3. Calcite validation and plan compilation skeleton are in place.
4. V2 row / schema runtime model is in place.
5. Iterator source is runnable.
6. JDBC query source is runnable.
7. Query-source parameter binding is no longer a placeholder.
8. SQL transform supports the first usable subset.
9. Multi-source `INNER JOIN` is runnable.
10. Console sink is runnable.
11. JDBC sink is runnable.
12. JDBC sink template-based column mapping is runnable.
13. Multi-sink sequential fan-out is runnable.
14. Sink failure strategy switching is runnable.
15. Query-backed source convergence is implemented at the model/mapping entrypoint level.
16. PF4J external plugin loading, class isolation, subtype parsing, runtime execution, mixed execution, refresh, and first-pass diagnostics are implemented.
17. Kafka V2 sink factory is runnable through `TemplateV2RuntimeServices.kafkaTemplate(...)` with `WriterVO.type=KAFKA`.
18. Elasticsearch V2 sink factory is runnable through `TemplateV2RuntimeServices.elasticsearchClient(...)` with `WriterVO.type=ELASTICSEARCH` or `ES`.
19. AI V2 source factory is runnable for deterministic `INLINE` / `STATIC` / `ECHO` providers and exposes rows to SQL transforms.
20. Source policy runtime semantics are active as source materialization post-processing for ordered/random selection and `limit`.
21. Remote AI source execution now has a runtime bridge contract and provider hook; tests cover prompt/options handoff and scalar/map/list output materialization without live network calls.
22. Kafka and Elasticsearch blank-template row publishing now share `RowJsonCodec`, with focused coverage for null, primitive, and escaped string values.
23. V2 writer-specific `options` are available on `WriterVO`; Kafka supports resolved `key` / `headers`, and Elasticsearch supports resolved `id` / `routing` plus `upsert`.
24. Runtime registry build failures now include provider index/class, plugin descriptor, factory collection phase, and refresh/initialization context while preserving the last good registry on refresh failure.
25. In-flight refresh policy is defined and implemented: a `TemplateV2Runner` run uses the registry snapshot captured at run start for source, transform, and sink execution; refresh affects only later runs.
26. SQL transform now supports `CASE WHEN`, `IS NULL`, and `IS NOT NULL`, covering the first V2 path for V1-style conditional/null handling.
27. SQL transform now enables the Calcite standard / Calcite / MySQL operator tables and executes the first null/string function batch: `COALESCE`, `CONCAT`, `UPPER`, `LOWER`, and `TRIM`.
28. SQL transform now executes the first conversion-oriented function batch: `NULLIF`, `CHAR_LENGTH`, `SUBSTRING`, `ABS`, `FLOOR`, `CEIL`, and `ROUND`.
29. SQL transform now has a first repository-owned V2 UDF namespace for date conversion helpers: `V2_FORMAT_DATE`, `V2_DATE_ADD`, `V2_DATE_SUB`, and `V2_DATE_DIFF`.
30. SQL transform now has a shared UDF registry abstraction so future repository or plugin-provided functions can register Calcite validation metadata and runtime evaluators through one extension point.
31. Runtime plugins can now contribute SQL UDFs through `TemplateV2RuntimePlugin.sqlFunctions()`, and the default SQL transform factory receives the merged built-in + plugin UDF registry.
32. CSV V2 source is runnable through `CsvSourceVO` / `CsvRowSource` / `CsvSourceFactory` and can participate in SQL transforms with explicit schema.
33. CSV parsing is now isolated behind `CsvParser`, keeping the built-in parser replaceable by repository or plugin-provided parser implementations.
34. JSON V2 source is runnable through `JsonSourceVO` / `JsonRowSource` / `JsonSourceFactory`, supports object arrays and single objects, and keeps parsing replaceable through `JsonParser`.
35. CSV and JSON V2 file sinks are runnable through `WriterVO.type=CSV` / `JSON`, with file output adapters registered in both built-in and Spring runtime paths.
36. PF4J external plugin jars can now contribute SQL UDFs through `TemplateV2RuntimePlugin.sqlFunctions()` into the built-in SQL transform path; descriptor-aware and composite plugin wrappers preserve registry-aware transform factories and UDF lists.
37. PF4J plugin diagnostics now cover duplicate plugin-provided SQL UDF names with both plugin ids, null plugin-provider returns include the PF4J extension class in the failure chain, and locator start/load failures include the PF4J locator class plus failing phase.
38. JSON V2 source now supports lightweight root selection before row materialization, covering nested object/array payloads without coupling the core source to a full JSONPath dependency yet.
39. CSV V2 source diagnostics now reject invalid multi-character delimiters and unclosed quoted fields with source path and line number context.

## Immediate Next Work

The next work should be done in the following order. This supersedes the older AI-first next-step recommendation because the transformation core and UDF extension surface have advanced.

### Next 1. Continue file-backed source/sink adapters

Goal:

- close the largest remaining practical source/sink gap after JDBC/Kafka/Elasticsearch

Recommended implementation:

- harden remaining CSV source options and diagnostics around schema/header mismatch and row-width mismatch
- add a PF4J or provider-level fixture for custom CSV parser replacement if parser customization becomes a concrete plugin requirement
- harden JSON source diagnostics and nested value strategy; root selection now has a lightweight built-in baseline
- keep source configuration Seatunnel-style: connection/path/read options live inside the source definition
- harden CSV/JSON sink options and diagnostics, then decide whether Excel source or external UDF fixture should be next

### Next 2. Plugin diagnostics hardening

Goal:

- improve failure diagnostics now that PF4J source/transform/sink/UDF contribution paths are all executable

Recommended implementation:

- add malformed PF4J jar fixtures if a real-world PF4J packaging failure cannot be diagnosed clearly from the current locator start/load wrapper
- add plugin descriptor mismatch or missing repository descriptor diagnostics if needed
- add one focused fixture for plugin-provided CSV parser replacement only if parser customization becomes a concrete requirement

### Next 3. Concrete AI runtime bridge

Goal:

- provide a real remote implementation behind the `AiRuntimeBridge` contract without coupling core V2 execution to network clients

Recommended implementation:

- add an Ollama or Spring-AI-backed bridge as an optional runtime provider
- keep mock / deterministic bridge tests as the primary no-network acceptance path
- define timeout and model error diagnostics
- decide whether bridge implementation lives in `data-generator-reader-ai` or a new V2 AI runtime module

### Next 4. V1-to-V2 migration examples and parity scorecard

Goal:

- validate that the new SQL/UDF/source/sink surface covers real business scenarios instead of only synthetic runner tests

Recommended implementation:

- add representative V2 examples for mapping, condition, convert, SpEL-expression subset, JDBC source, Kafka sink, and Elasticsearch sink
- document unsupported direct migrations for log/pause/shared/procedural JavaScript paths
- create a parity scorecard that tracks V1 stage/reader/writer families by `covered`, `partial`, `compatibility-only`, or `not started`

## Deferred Work

The following items remain intentionally deferred after the current step:

- full Calcite physical execution
- UDF expansion
- concrete Ollama/Spring-AI bridge implementation for `AiRuntimeBridge`
- source policy caching/materialization modes beyond row post-processing
- nested JSON serialization for Kafka/Elasticsearch row payloads if complex values become required
- V1 retirement

## Current Recommendation

The most pragmatic next implementation sequence is:

1. continue file-backed source/sink adapters, starting with CSV hardening and JSON source
2. prove plugin-provided UDFs through the external PF4J packaging path
3. add the concrete Ollama/Spring-AI implementation behind `AiRuntimeBridge`
4. build V1-to-V2 migration examples and a parity scorecard against business scenarios
5. harden multi-source SQL semantics beyond the current `INNER JOIN` subset
6. replace `RowJsonCodec` with a Jackson-backed codec only when nested object payloads become a concrete requirement

The plugin-framework recommendation is:

- keep the current V2 runtime abstraction layer
- do not rewrite the V2 runtime around PF4J-native types now
- use PF4J as the preferred external plugin lifecycle/classloading layer
- keep the old shared-classloader ServiceLoader path only as fallback

Current validation result:

- PF4J external plugin path has been wired into the service runtime
- focused isolation tests now show PF4J plugins load with separate classloaders
- the old ServiceLoader external plugin path does not provide true plugin isolation and can mis-handle multi-plugin descriptor attribution
- a minimal PF4J external plugin sample skeleton is now available under `samples/template-v2-pf4j-plugin`
- plugin-provided template model subtypes now refresh into the shared YAML/JSON codec path through the host-side subtype registry
- focused integration tests have validated PF4J-provided `SourceVO`, `TransformVO`, and `WriterVO` subtype parsing end to end
- focused integration tests have also validated one real V2 execution path where PF4J-provided source/transform/sink factories participate in runtime execution together
- focused integration tests have also validated mixed execution where built-in source factories and PF4J-provided transform/sink factories run in one template
- PF4J locator refresh now unloads and reloads plugin jars before rebuilding the runtime registry
- focused integration tests have validated that a plugin jar added after initial startup becomes executable after subtype and registry refresh
- runtime registry now wraps matched factory failures with node kind, template type, model class, and factory class diagnostics
- runtime registry build now wraps provider create failures, plugin descriptor failures, and plugin factory collection failures with actionable diagnostics
- refreshable registry provider now wraps initialization/refresh failures and keeps the last good registry when refresh fails
- runner execution now keeps one registry snapshot for the entire in-flight run, including sink creation, so refresh is visible only to later runs
- focused registry tests cover transform factory failure diagnostics, build failure diagnostics, and refresh failure behavior
- runner sink-write failures now include sink index, writer index, writer type, model class, and target diagnostics under fail-fast policy
- focused runner tests cover sink write diagnostics and continue-on-error behavior
- the next PF4J gap is in-flight task refresh policy and more PF4J-specific malformed jar/load-failure fixtures

See:

- `docs/calcite-plugin-framework-evaluation.md`
- `docs/calcite-pf4j-plugin-packaging.md`

This sequence keeps the current V2 path moving from prototype to usable execution while staying aligned with the longer-term Calcite refactor direction and the future external pluginization goal.
