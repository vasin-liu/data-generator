# Calcite Refactor Plan

## Goal

Introduce a Calcite-based V2 transformation path that simplifies the current field DAG and stage-heavy configuration model into a more declarative `source + transform + sink` flow, while keeping the existing V1 template path compatible during migration.

Final objective:

- V2 should eventually implement the functional surface of V1
- V2 should exceed V1 in template simplicity, composability, and extensibility

## Target Architecture

Current V1 model:

- `iterator + generator + fields(dependsOn + stages) + output`

Target V2 model:

- `sources + transform + sink`

Target end-state extension:

- `sources + transformers + sinks`

Example V2 template shape:

```yaml
name: demo_sql_v2

sources:
  input:
    type: iterator
    iterator:
      type: datetime
      from: 2025-01-01 00:00:00
      to: 2025-01-01 01:00:00
      step: 5
      unit: MINUTES

transform:
  type: sql
  sql: |
    SELECT
      ts,
      upper(device_name) AS device_name_upper,
      lng * 1.0 AS longitude,
      lat * 1.0 AS latitude
    FROM input

sink:
  writers:
    - type: CONSOLE
```

Final-phase multi-node shape:

```yaml
name: demo_sql_v2_advanced

sources:
  input:
    type: iterator
    iterator:
      type: NUMBER
      from: 1
      to: 10
  dict_city:
    type: reader
    reader:
      type: JDBC

transformers:
  - type: sql
    name: normalize
    sql: |
      SELECT value AS id FROM input
  - type: sql
    name: enrich
    sql: |
      SELECT n.id, c.city_name
      FROM normalize n
      LEFT JOIN dict_city c ON n.id = c.id

sinks:
  - writers:
      - type: CONSOLE
  - writers:
      - type: KAFKA
        dataSourceId: kafka1
        target: demo_topic
```

## Scope

In scope for the first implementation:

- V2 template model and routing
- Calcite-backed single-table SQL validation and execution
- row/schema abstraction for the V2 path
- iterator/reader source adaptation into logical tables
- reuse of the current writer stack through a row sink adapter
- repository-local UDF registration for common faker/date/string functions
- converging `DatabaseIterator` and `JdbcReader` into the same V2 `RowSource` family
- preserving `SelectStrategy` semantics as source policy instead of forcing them into SQL
- introducing an official `AiSourceVO`

Explicitly out of scope for the first implementation:

- `JOIN`
- `GROUP BY`
- window functions
- subqueries
- replacing V1 templates
- removing the current writer modules
- full support for multiple transformers and multiple sinks

## Design Decisions

- V1 and V2 run side by side.
- V2 is a new mainline path, not a new `StageVO`.
- Calcite should own SQL parsing and validation; do not translate SQL back into the old stage chain.
- The V2 execution path should use a simple row model instead of `Value / SingleValue / ListValue / MapValue` as its primary abstraction.
- Existing writer modules should be reused through adapters instead of rewritten first.
- Multiple sources should be a supported end-state because they are a natural fit for Calcite table semantics.
- Multiple transformers should be supported only as an ordered linear chain; do not allow arbitrary DAG transformers in V2.
- Multiple sinks should be supported as independent terminal fan-out nodes after the final transform result is materialized.
- The first implementation should still start with one `source entry set`, one `transform`, and one `sink`, but the model should avoid painting us into a single-node corner.
- `DatabaseIterator` and `JdbcReader` should converge into one V2 source abstraction instead of remaining two separate long-term concepts.
- `SelectStrategy` should remain a source policy concern in V2.
- Multi-sink failure handling should be configurable per template or per sink execution group.
- `AiSourceVO` should be an official V2 source type; Spring AI integration is an acceptable direction if it fits the repository runtime model.

## Milestones

### M0 - Freeze the V2 contract

- [ ] Finalize the V2 template structure: `sources + transform + sink`
- [ ] Finalize the first-phase SQL boundary: single-table `SELECT`
- [ ] Finalize V1/V2 routing rules during template loading
- [ ] Finalize V2 non-goals for phase 1
- [ ] Prepare 3 V2 sample templates for design review

Artifacts:

- `docs/calcite-refactor-plan.md`
- `docs/examples/template-v2-*.yaml` or equivalent review examples

Exit criteria:

- the team agrees that V2 is not stage-based
- the first phase syntax boundary is documented and stable

### M1 - Introduce the V2 model layer

- [ ] Add `TemplateV2VO`
- [ ] Add `SourceVO` hierarchy
- [ ] Add `TransformVO` hierarchy
- [ ] Add `SqlTransformVO`
- [ ] Decide whether `sink` reuses `WriteStageVO` directly or gets a thin `SinkVO`
- [ ] Add `Row`
- [ ] Add `RowSchema`
- [ ] Add `ColumnDef`
- [ ] Add V2 subtype registration to YAML/JSON parsing

Suggested package targets:

- `data-generator-common/data-generator-core/.../model/v2`
- `data-generator-service` parser/codec registration

Exit criteria:

- V2 YAML can be parsed successfully
- V2 JSON cache serialization/deserialization works

### M2 - Template loading, cache, and runtime routing

- [ ] Add V1/V2 template identification logic
- [ ] Extend template cache metadata to record template version
- [ ] Update template import/validation entry points
- [ ] Update template execution entry points
- [ ] Ensure startup cache loading accepts V1 and V2 templates together

Suggested routing rules:

- V1 if `fields` is present
- V2 if `sources` or `transform` is present

Exit criteria:

- service startup can load mixed V1 and V2 templates
- V2 templates can be validated and cached through the service APIs

### M3 - Add the Calcite infrastructure module

- [ ] Create `data-generator-calcite`
- [ ] Add Calcite dependencies
- [ ] Add `CalciteExecutionContext`
- [ ] Add `CalciteSchemaFactory`
- [ ] Add `CalciteSqlValidator`
- [ ] Add `CalcitePlanCompiler`
- [ ] Standardize SQL validation error formatting

Exit criteria:

- a simple `SELECT col FROM input` SQL string can be parsed and validated
- invalid SQL returns actionable file/template/column information

### M4 - Define source abstraction and adapt current inputs

- [ ] Add `RowSource` interface
- [ ] Add `IteratorRowSource`
- [ ] Add `ReaderRowSource`
- [ ] Define the convergence path from `DatabaseIterator` and `JdbcReader` into one V2 source model
- [ ] Define `SourcePolicy` semantics, including selection behavior
- [ ] Add `AiSourceVO` to the source model plan
- [ ] Define source-to-schema mapping rules
- [ ] Decide how iterator scalar outputs map into tabular columns
- [ ] Decide whether first-phase reader schemas are explicit or inferred

Recommended first-phase rule:

- prefer explicit schema where automatic inference is ambiguous

Exit criteria:

- iterator source can expose a stable logical table named `input`
- at least one reader-backed source can expose a stable schema
- `DatabaseIterator` and `JdbcReader` no longer diverge architecturally in the V2 design
- `SelectStrategy` behavior has a defined home in the V2 source layer

### M5 - Deliver the first SQL execution path

- [ ] Support `SELECT ... FROM input`
- [ ] Support projection aliases
- [ ] Support arithmetic expressions
- [ ] Support `CASE WHEN`
- [ ] Support `WHERE`
- [ ] Support null handling and basic casts
- [ ] Add output schema derivation

Exit criteria:

- `iterator -> sql -> console` path works end to end
- validation and execution errors identify the SQL fragment and failing symbol

### M6 - Reuse the sink stack through row adapters

- [ ] Add `RowSink` interface or adapter layer
- [ ] Adapt row output to `ConsoleWriter`
- [ ] Adapt row output to DB writer
- [ ] Adapt row output to Kafka writer
- [ ] Adapt row output to Elasticsearch writer
- [ ] Define a stable row-to-writer payload mapping contract
- [ ] Define configurable multi-sink failure behavior

Recommended implementation order:

1. console
2. database
3. Kafka
4. Elasticsearch

Exit criteria:

- `iterator -> sql -> console` passes
- `iterator -> sql -> db` passes
- multi-sink templates have an explicit execution and failure policy

### M7 - Add UDF support for repository-specific generation logic

- [ ] Add `UdfRegistry`
- [ ] Define UDF naming conventions
- [ ] Register first-phase string/date/null helpers
- [ ] Register first-phase faker helpers
- [ ] Inject UDFs into Calcite schema/context
- [ ] Add documentation for V1 script equivalents in SQL
- [ ] Decide how AI-backed generation is split between UDFs and `AiSourceVO`

Suggested first-phase functions:

- `faker_snowflake()`
- `faker_text(min, max)`
- `faker_date_past(days, pattern)`
- `faker_datetime_format(value, pattern)`
- `coalesce(...)` if not provided directly by the chosen Calcite execution path

Exit criteria:

- at least 5 UDFs are callable from V2 SQL templates
- UDF behavior has direct automated test coverage

### M8 - Build the V2 pipeline

- [ ] Add `TemplateV2PipelineFactory`
- [ ] Add `DefaultSourcePipelineFactory`
- [ ] Add `DefaultTransformPipelineFactory`
- [ ] Reuse or adapt `DefaultWritePipelineFactory`
- [ ] Add V2 execution logs and debug traces
- [ ] Separate V1 and V2 runtime diagnostics cleanly

Exit criteria:

- controller runtime can execute a V2 template directly
- logs clearly identify V2 source, SQL, and sink stages

### M9 - Error model and observability

- [ ] Add `TemplateValidationException`
- [ ] Add `SqlTransformException`
- [ ] Add `SourceSchemaException`
- [ ] Standardize error context fields: template, source, sql snippet, column
- [ ] Add debug logging for input and output schemas
- [ ] Add user-facing diagnostics for validation failures

Exit criteria:

- common V2 failures are diagnosable without inspecting a long stack trace

### M10 - Build the test matrix

- [ ] Add V2 YAML parsing tests
- [ ] Add Calcite SQL validation tests
- [ ] Add source schema tests
- [ ] Add UDF tests
- [ ] Add V2 end-to-end tests
- [ ] Add V1/V2 coexistence regression tests

Minimum end-to-end set:

- [ ] `iterator -> sql -> console`
- [ ] `iterator -> sql(case when) -> console`
- [ ] `reader -> sql -> db`
- [ ] invalid column name error
- [ ] invalid function error
- [ ] type mismatch error

Exit criteria:

- the first V2 regression suite is stable in CI
- the current Boot 4 regression suite still passes

### M11 - Prepare migration and adoption

- [ ] Document V1 to V2 mapping rules
- [ ] Document manual migration examples
- [ ] Prepare V1/V2 side-by-side examples
- [ ] Identify stage types that become compatibility-only over time
- [ ] Decide whether a partial migration assistant script is worth building
- [ ] Define the V1 parity scoreboard and the acceptance rule for saying V2 has reached functional equivalence

Typical mapping targets:

- `dependsOn + script` -> SQL projection expression
- `mapping` -> `CASE WHEN`
- `condition` -> `CASE WHEN`

Exit criteria:

- at least 2 representative templates have V1 and V2 side-by-side examples

### M12 - Finalize multi-source, multi-transformer, and multi-sink support

- [ ] Extend `TemplateV2VO` from singular `transform/sink` shape to final compatible `transformers/sinks` shape
- [ ] Keep backward-compatible parsing for the singular first-phase form
- [ ] Support multiple named logical tables in the Calcite schema
- [ ] Support multiple transformer stages as an ordered chain
- [ ] Define intermediate result naming and schema handoff between transformers
- [ ] Support multiple sinks as terminal fan-out nodes
- [ ] Define sink execution mode: sequential first, optional parallel later
- [ ] Define configurable multi-sink failure strategy modes
- [ ] Add validation rules to prevent illegal transformer graphs and duplicate names
- [ ] Add end-to-end tests for multi-source, multi-transformer, and multi-sink templates

Design constraints for M12:

- `sources` is a named map, not an ordered list
- `transformers` is an ordered list
- `sinks` is an ordered list
- each transformer output becomes a named logical relation for subsequent transformers
- only linear chaining is supported at first; no arbitrary branching transformer DAG
- one source family should cover both iterator-backed and query-backed tabular inputs
- selection behavior remains source policy, not transform SQL

Recommended final model shape:

- `Map<String, SourceVO> sources`
- `List<TransformVO> transformers`
- `List<WriteStageVO> sinks`

Exit criteria:

- one template can read from more than one source table
- one template can execute more than one ordered transformer
- one template can write the same final dataset to more than one sink
- validation rejects duplicate source/transformer names and unresolved references
- sink failure behavior can switch by configuration
- the V2 source model includes an official AI-backed source type

## Recommended Delivery Order

Sprint 1:

- M0
- M1
- M2
- M3

Sprint 2:

- M4
- M5
- M6 console path

Sprint 3:

- M7
- M8
- M10 minimum test matrix

Sprint 4:

- M6 db/kafka/es paths
- M9
- M11

Sprint 5:

- M12

## First 6 Implementation Tasks

1. Create the V2 model classes and parser registration.
2. Create the `data-generator-calcite` module skeleton.
3. Add `Row`, `RowSchema`, and `ColumnDef`.
4. Implement `IteratorRowSource` with a logical `input` table.
5. Implement `SqlTransformVO` and the first `SELECT` execution path.
6. Deliver `iterator -> sql -> console` end-to-end with tests.

## Success Criteria

- V2 templates materially reduce configuration complexity for row transformations.
- New row transformation logic no longer requires `FieldVO.dependsOn + multiple StageVO` chains by default.
- Calcite becomes the primary validation and transformation engine for V2 templates.
- The existing writer ecosystem remains reusable.
- V1 templates continue to run during the migration window.
- The final V2 model can scale from the first-phase singular form to multiple sources, ordered transformers, and fan-out sinks without a second model rewrite.
- The long-term target is V1 functional parity with better extensibility, not a permanently reduced subset.
