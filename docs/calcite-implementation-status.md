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
- `docs/calcite-v1-v2-migration-examples.md`
- `docs/calcite-v1-parity-scorecard.md`
- `docs/calcite-skeleton-implementation-plan.md`
- `docs/calcite-templatev2-model-design.md`
- `docs/template-v2-product-roadmap.md`

## Current Status

### Progress snapshot

As of the current implementation checkpoint:

- Phase 1 minimum viable V2 is complete for number/constant/datetime iterator-backed seeds
- Phase 2 practical source/sink coverage is mostly complete for JDBC, Kafka, Elasticsearch, AI source runtime bridge, source policy, and multi-sink failure policy
- Phase 2 now also includes first-pass Excel source and sink support through `ExcelSourceVO` / `ExcelRowSource` and `ExcelSinkFactory` / `ExcelRowSinkAdapter`
- Phase 3 transformation migration coverage has a usable baseline through SQL conditional/null/string/conversion/date functions, the shared UDF registry, a first high-frequency faker compatibility batch, and a first repository-backed V1-to-V2 migration example set
- the largest remaining gaps are official non-SQL transformer families beyond the current SQL-first path, broader SpEL/faker compatibility coverage beyond the first batch, richer business-family parity validation, exact V1 selection semantics beyond current source-policy aliases, and broader provider/plugin examples around the AI bridge

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
- `ExcelSourceVO`
- `ExcelSheetSourceVO`
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
- `ConstantIteratorVO` path through `IteratorRowSource`
- `DateTimeIteratorVO` path through `IteratorRowSource`

Current status:

- usable as V2 seed source for number, constant, and datetime iterators
- constant iterator materializes finite repeated datasets; `repeat=-1` is rejected explicitly in V2 finite materialization
- datetime iterator materializes inclusive `from -> to` ranges and can participate in SQL timestamp comparison
- verified in runner-level and source-level tests

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
- supports `charset`, single-character `delimiter`, `header`, `strictColumns`, `maxRows`, and optional explicit `RowSchema`
- supports quoted fields and escaped quotes in the lightweight built-in parser
- rejects invalid multi-character delimiters and unclosed quoted fields with source path and line diagnostics
- rejects row-width mismatch by default with source path, line number, expected column count, and actual column count; `strictColumns=false` restores loose null-fill/truncate behavior
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
- rejects root selector misses and out-of-range array indexes with selector, segment, and source path diagnostics instead of silently producing empty rows
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

Current transformer architecture note:

- the runtime contract already supports ordered transformer chains and plugin-provided transform factories
- the built-in transformer family is still SQL-only today
- the next architecture step is to add at least one official non-SQL transformer family without recreating V1-style stage sprawl
- custom transformers should continue to land as typed `TransformVO` subtypes plus `V2TransformFactory` implementations

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
- rejects invalid multi-character delimiters with target path diagnostics
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
- serializes rows through the current `RowJsonCodec`
- supports `options.charset` and `options.mode`
- `options.mode=ARRAY` writes the default JSON array output
- `options.mode=NDJSON` writes one JSON object per line for line-oriented downstream consumers
- rejects unsupported JSON sink modes with target path diagnostics
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
- the runtime registry and runner already accept non-SQL/plugin-provided transformer factories by contract
- sinks are executed one after another
- sink failure policy supports `FAIL_FAST` and `CONTINUE_ON_ERROR`
- fail-fast sink write failures include sink index, writer index, writer type, model class, and target diagnostics

What is not implemented yet:

- a repository-owned built-in transformer family beyond `sql`
- clear authoring guidance for when to use SQL/UDF versus a custom transformer
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
21. Remote AI source execution now has a concrete Ollama-backed runtime bridge in the service layer; tests cover prompt/options handoff, parser resolution, Spring runtime wiring, and scalar/map/list output materialization without live network calls.
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
35. CSV and JSON V2 file sinks are runnable through `WriterVO.type=CSV` / `JSON`, with file output adapters registered in both built-in and Spring runtime paths; CSV sink delimiter diagnostics now align with CSV source.
36. PF4J external plugin jars can now contribute SQL UDFs through `TemplateV2RuntimePlugin.sqlFunctions()` into the built-in SQL transform path; descriptor-aware and composite plugin wrappers preserve registry-aware transform factories and UDF lists.
37. PF4J plugin diagnostics now cover duplicate plugin-provided SQL UDF names with both plugin ids, null plugin-provider returns include the PF4J extension class in the failure chain, and locator start/load failures include the PF4J locator class plus failing phase.
38. JSON V2 source now supports lightweight root selection before row materialization, covering nested object/array payloads without coupling the core source to a full JSONPath dependency yet.
39. CSV V2 source diagnostics now reject invalid multi-character delimiters and unclosed quoted fields with source path and line number context.
40. CSV V2 source row materialization now validates row-width mismatch by default through `strictColumns=true`, while keeping an explicit loose mode for imperfect files.
41. JSON V2 source root selector diagnostics now fail fast on missing path segments and array index misses, avoiding silent empty datasets caused by bad source configuration.
42. JSON V2 sink now supports `options.mode=ARRAY|NDJSON` and rejects unsupported modes with target diagnostics.
43. The built-in V2 faker datetime compatibility layer now covers the highest-frequency V1 relative-time shapes: single-argument and value-based `minus/plus` variants, default `format(x)`, and `before/after` range helpers across day/hour/minute/second where needed for migration.
44. The built-in V2 faker compatibility layer now also covers `#faker.phoneNumber.cellPhone` through `FAKER_PHONE_CELL()`, closing one of the remaining high-frequency repository template gaps outside datetime handling.
45. Repository-backed V1-to-V2 migration examples now exist for query-source convergence, faker datetime migration, mapping/condition SQL rewrites, and multi-sink fan-out in `docs/calcite-v1-v2-migration-examples.md`.
46. The migration examples now also cover the repository AI reader path through a first-class `AiSourceVO` / `OLLAMA` example, and the docs now explicitly record that `SourcePolicyVO` currently models ordered/random materialization aliases plus `limit`, not full V1 consumptive `SELECT` semantics.
47. Query-source migration analysis is now V1-aware for selection and reader-pool semantics: `ONCE_ORDER`, `ONCE_RANDOM`, `MULTIPLE_ORDER`, and multi-reader `EQUAL` / `WEIGHT` cases emit explicit approximation warnings instead of silently appearing fully compatible.
48. V1 query-source extraction no longer overwrites multiple JDBC readers declared under the same field; migrated drafts now emit stable unique source names such as `customer_lookup` and `customer_lookup_2`.
49. The migration examples now also cover repository-real selection-heavy templates, showing when `SourcePolicyVO` approximation is acceptable and when V1 selector behavior should instead be rewritten explicitly as relational V2 SQL.
50. Multi-source migration candidates now infer practical join predicates from parameter names, query predicates, and runtime-resolved source schemas; lookup skeletons can emit preflight-valid joins such as `s0.customer_id = s1.id` instead of only `ON 1 = 1`.
51. Multi-source candidate SQL now expands explicit projection aliases from resolved source schemas, producing more stable authoring drafts such as `iterator_id` and `customer_lookup_name` instead of relying on ambiguous `s0.*`, `s1.*` output.
52. Multi-source join inference now also covers structural business patterns beyond direct params: source-name-derived foreign keys such as `customer_id = id` and shared scope columns such as `tenant_id` are appended automatically when both sides expose compatible schema.
53. Structural join inference now treats entity keys such as `id`, `code`, `type`, and `version` as composable business keys, so candidate SQL can emit composite joins like `customer_id + customer_type + tenant_id` when the source schemas support them.
54. Multi-source migration candidates now also infer simple date-window predicates, allowing rule/lookup sources with `start_time`/`end_time` style schemas to produce preflight-valid conditions such as `event_time >= start_time AND event_time <= end_time`.
55. Parameterized lookup candidates now emit more explicit relational rewrite guidance: when a join condition can be inferred, the analysis API tells authors to remove per-row params from the source definition and reshape the lookup into a relational source that joins on the inferred business condition.
56. Candidate SQL projection aliases are now less mechanical: source names such as `customer_lookup` are normalized toward business stems like `customer`, while primary iterator columns can keep simple names such as `id` when no collision exists.
57. Query-source migration hints now also surface lightweight field-role guidance on inferred joins, highlighting likely join keys such as `id` / `tenant_id` and likely business output columns such as `name` so multi-source V2 drafts are easier to review before hand-tuning.
58. Parameterized lookup candidate metadata can now also emit a conservative `suggestedSql` rewrite for the source itself, stripping simple per-row parameter predicates while keeping static filters and ordering so authors can reshape V1 row-parameterized reads into V2 relational sources faster.
59. Lookup migration `joinHints` now surface that `suggestedSql` directly as source-level authoring guidance, so the analysis API tells authors not only how to join, but also how to rewrite the lookup source definition itself.
60. Conservative lookup source rewrites now also cover simple `IN (:param)` predicates in the same `AND` chain; more complex boolean groups and `OR` shapes remain intentionally manual-review territory.
61. Conservative lookup source rewrites now also cover simple parameterized comparison predicates such as `>= :startTime` and `<= :endTime` in the same `AND` chain, which is enough for first-pass time-window lookup rewrites while still leaving `BETWEEN` and more complex boolean shapes to explicit author review.
62. The in-memory Calcite V2 runtime now executes `LEFT JOIN` in addition to `INNER JOIN`, including null-padding for unmatched right-side rows, so lookup-style V2 templates are not limited to migration previews and can run end to end in the current runner.

## Immediate Next Work

The next work should be done in the following order. This supersedes the older AI-first next-step recommendation because the transformation core and UDF extension surface have advanced.

### Next 1. Broaden the transformer surface beyond SQL

Goal:

- keep SQL as the default V2 authoring path, but stop making it appear to be the only transformer path

Recommended implementation:

- define the first official non-SQL built-in transformer family for deterministic residual logic that does not fit cleanly into SQL + UDF
- formalize the decision rule for SQL/UDF vs built-in non-SQL transformer vs project/plugin-specific custom transformer
- require non-SQL/custom transformers to declare or infer output schema explicitly so multi-transform chaining stays predictable
- add one repository-owned non-SQL transformer implementation and one PF4J-based custom transformer sample or fixture
- keep validation, refresh, diagnostics, and class-isolation behavior consistent across SQL and non-SQL transformers
- avoid reintroducing one transformer type per old V1 stage; only repeated business scenarios should justify additional built-in families

### Next 2. Expand business-scenario migration rewrites

Goal:

- convert the remaining high-value V1 business templates into trustworthy V2 authoring patterns, especially where direct `SourcePolicyVO` parity does not exist

Recommended implementation:

- keep extending `docs/calcite-v1-v2-migration-examples.md` with repository-real templates rather than synthetic cases
- rewrite selection-heavy templates explicitly when their real intent is dimensional expansion or weighted branching
- keep `SourcePolicyVO` only for cases where ordered/shuffled materialization is good enough
- keep documenting exact-vs-approximate migration boundaries so the preview/analyze APIs do not over-promise parity
- identify which residual V1 script/business patterns should migrate through SQL/UDF and which should become non-SQL/custom transformer candidates

### Next 3. Harden multi-source SQL migration ergonomics

Goal:

- make multi-source V2 authoring easier for real business templates that currently rely on field DAGs and implicit V1 dependency flow

Recommended implementation:

- continue beyond the new param-name/schema-based join inference for source-level authoring suggestions that can eventually feed an automatic rewrite path
- add better aliasing and projection guidance for multi-query-source migration, especially where projected business names should differ from raw source names
- keep validating candidate transforms with Calcite preflight before persistence

### Next 4. AI bridge hardening and provider expansion

Goal:

- harden the concrete `AiRuntimeBridge` path and keep room for additional providers without coupling core V2 execution to network clients

Recommended implementation:

- keep the new Ollama-backed bridge as the default optional runtime provider
- add timeout, retry, and model/provider diagnostics where business scenarios require them
- preserve mock / deterministic bridge tests as the primary no-network acceptance path
- define timeout and model error diagnostics
- decide whether additional providers should stay in `data-generator-service`, move into a dedicated V2 AI runtime module, or be offered as external PF4J plugins

### Next 5. Plugin diagnostics hardening

Goal:

- improve failure diagnostics now that PF4J source/transform/sink/UDF contribution paths are all executable

Recommended implementation:

- add malformed PF4J jar fixtures if a real-world PF4J packaging failure cannot be diagnosed clearly from the current locator start/load wrapper
- add plugin descriptor mismatch or missing repository descriptor diagnostics if needed
- add one focused fixture for plugin-provided CSV parser replacement only if parser customization becomes a concrete requirement

### Next 6. Business-scenario parity expansion and scorecard hardening

Goal:

- validate that the new SQL/UDF/source/sink surface covers more business scenarios than the current migration-example batch

Recommended implementation:

- keep `docs/calcite-v1-parity-scorecard.md` current as each V1 capability lands in V2
- continue adding representative V2 examples only where a new business-family gap appears
- document unsupported direct migrations for log/pause/shared/procedural JavaScript paths
- keep the `SourcePolicyVO` boundary explicit until a decision is made on whether V2 should absorb V1 consumptive selection semantics
- extend the faker/UDF compatibility catalog only when repository templates or business examples show uncovered high-frequency expressions
- keep the future non-SQL/custom transformer backlog grounded in observed repository templates instead of speculative transformer proliferation

## Deferred Work

The following items remain intentionally deferred after the current step:

- full Calcite physical execution
- a broad transformer catalog beyond the first repository-owned non-SQL/custom path
- UDF expansion
- source policy caching/materialization modes beyond row post-processing
- nested JSON serialization for Kafka/Elasticsearch row payloads if complex values become required
- V1 retirement

## Current Recommendation

The most pragmatic next implementation sequence is:

1. define and implement the first official non-SQL transformer path plus one plugin-provided custom transformer example
2. expand repository-real migration rewrites for selection-heavy, script-heavy, and multi-source templates
3. harden multi-source SQL candidate generation beyond the current `INNER JOIN` subset
4. harden the concrete Ollama bridge and add additional provider implementations only when justified by business scenarios
5. prove plugin-provided UDFs and non-SQL/custom transformers through the external PF4J packaging path
6. keep the parity scorecard aligned against business scenarios instead of broad speculative feature work
7. replace `RowJsonCodec` with a Jackson-backed codec only when nested object payloads become a concrete requirement

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
