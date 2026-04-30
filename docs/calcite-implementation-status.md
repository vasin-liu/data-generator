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

### 1. Baseline and scope

The repository has already completed the Boot 4 / JDK 25 baseline transition needed for the V2 path.

The current active direction is:

- keep V1 runnable
- add a parallel V2 execution path
- use Calcite for SQL parsing and validation
- keep the first V2 runtime as a lightweight in-memory row engine instead of introducing full Calcite physical execution too early

### 2. V2 model and parsing

Implemented model set under `data-generator-common/data-generator-core`:

- `TemplateV2VO`
- `TemplateV2DraftVO`
- `SourceVO`
- `IteratorSourceVO`
- `QuerySourceVO`
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
  - JDBC query source

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
- single-table `FROM`
- `FROM table AS alias`
- `INNER JOIN`
- `ON` condition
- qualified column references such as `l.value`

Current execution model:

- Calcite is used for parse + validate
- row execution is still repository-local in-memory evaluation
- this is intentionally a skeleton runtime, not full relational execution

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

- `WriterVO.template` is not used yet
- no explicit upsert / merge / conflict strategy

### 8. Multi-source / multi-transform / multi-sink status

Current status:

- multiple `sources` are supported
- multiple `transformers` are supported as an ordered linear chain
- multiple `sinks` are supported as sequential execution

What is implemented today:

- multiple sources can coexist in the same SQL context
- multiple sources can participate in `INNER JOIN`
- sinks are executed one after another

What is not implemented yet:

- sink failure strategy switching
- partial-success policy control
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

## Immediate Next Work

The next work should be done in the following order.

### Next 1. JDBC sink column/template mapping

Goal:

- make JDBC sink usable when transform output column names do not exactly equal target table column names

Recommended implementation:

- start using `WriterVO.template`
- define a V2-safe interpretation for `template`
- support at least:
  - direct column mapping
  - reordered insert columns

Suggested acceptance:

- V2 JDBC sink can write to a table with different physical column names than the transform output
- add dedicated H2 test coverage

### Next 2. Sink execution policy

Goal:

- make multiple sinks production-usable instead of best-effort sequential only

Recommended implementation:

- wire `SinkExecutionPolicyVO` into `TemplateV2Runner`
- define at least two policies:
  - fail-fast
  - continue-on-error

Suggested acceptance:

- one test for fail-fast
- one test for continue-on-error
- runner result or log path clearly reflects sink-level failures

### Next 3. Query-backed source convergence

Goal:

- move V2 closer to the final decision that `DatabaseIterator` and `JdbcReader` converge into one query-backed source family

Recommended implementation:

- review current `DatabaseIterator` and `JdbcReader`
- identify what can become:
  - query source options
  - source policy options
  - helper/factory concerns

Suggested acceptance:

- one documented convergence mapping
- at least one additional V2 source case migrated or represented by `QuerySourceVO`

### Next 4. Join capability hardening

Goal:

- move from minimal `INNER JOIN` support to a safer multi-source SQL subset

Recommended implementation:

- support `LEFT JOIN`
- support multi-condition `ON`
- define ambiguity rules for unqualified columns

Suggested acceptance:

- tests for `LEFT JOIN`
- tests for compound `ON` conditions
- clear error behavior for ambiguous column references

### Next 5. Service-level V2 execution test

Goal:

- verify that the service path does not only parse V2, but also runs it end-to-end

Recommended implementation:

- add controller or integration test that exercises:
  - V2 upload or task payload
  - template detection
  - normalization
  - validation
  - V2 runner dispatch

Suggested acceptance:

- one service-level test proving V2 task execution path is live

## Deferred Work

The following items remain intentionally deferred after the current step:

- full Calcite physical execution
- UDF expansion
- AI source real execution path
- Kafka sink on the V2 path
- Elasticsearch sink on the V2 path
- source policy runtime semantics
- V1 retirement

## Current Recommendation

The most pragmatic next implementation sequence is:

1. JDBC sink `template` mapping
2. `SinkExecutionPolicyVO` runtime wiring
3. query-backed source convergence
4. join hardening
5. service-level V2 end-to-end execution test

This sequence keeps the current V2 path moving from prototype to usable execution while staying aligned with the longer-term Calcite refactor direction.
