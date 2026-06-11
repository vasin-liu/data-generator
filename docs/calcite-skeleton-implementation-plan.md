# Calcite Skeleton Implementation Plan

## Goal

Deliver the first implementation skeleton for the Calcite-based V2 path without replacing the current V1 execution chain.

This plan only targets the foundation needed for:

- V2 template parsing
- V1/V2 routing
- Calcite module bootstrap
- row/schema model introduction
- the first end-to-end skeleton path: `iterator -> transform.sql -> console`

## Phase Boundary

Included in this skeleton phase:

- model skeleton
- parser/codec registration
- module skeleton
- source abstraction skeleton
- transform abstraction skeleton
- sink adapter skeleton
- minimal tests

Not included in this skeleton phase:

- full SQL execution
- full UDF coverage
- DB/Kafka/ES sink support
- migration automation
- V1 capability retirement
- final multi-source/multi-transformer/multi-sink runtime support

## Deliverables

1. V2 model package
2. V2 template routing entry
3. `data-generator-calcite` module
4. row/schema abstraction
5. source/transform/sink interfaces
6. console-only skeleton runtime path
7. minimum parsing and routing tests

The skeleton should still preserve forward compatibility for the final end-state:

- `sources` stays plural from day 1
- `transform` should be introduced in a way that can later expand to `transformers`
- `sink` should be introduced in a way that can later expand to `sinks`
- all new skeleton capabilities should be introduced behind extension-friendly contracts, so later source / transformer / sink / UDF / plugin implementations can be added without rewriting the core runtime

## Target Structure

Suggested new packages and modules:

### Module: `data-generator-common/data-generator-core`

Add:

- `org.gensokyo.data.model.v2.TemplateV2VO`
- `org.gensokyo.data.model.v2.SourceVO`
- `org.gensokyo.data.model.v2.TransformVO`
- `org.gensokyo.data.model.v2.SqlTransformVO`
- `org.gensokyo.data.model.v2.SinkVO` or sink wrapper decision point
- `org.gensokyo.data.row.Row`
- `org.gensokyo.data.row.RowSchema`
- `org.gensokyo.data.row.ColumnDef`
- `org.gensokyo.data.source.RowSource`
- `org.gensokyo.data.transform.RowTransformer`
- `org.gensokyo.data.sink.RowSink`

### Module: `data-generator-calcite`

Add:

- `org.gensokyo.data.calcite.CalciteExecutionContext`
- `org.gensokyo.data.calcite.CalciteSchemaFactory`
- `org.gensokyo.data.calcite.CalciteSqlValidator`
- `org.gensokyo.data.calcite.CalcitePlanCompiler`
- `org.gensokyo.data.calcite.CalciteRowTransformer`

### Module: `data-generator-service`

Add or update:

- V2 YAML/JSON subtype registration
- template version detection
- V1/V2 routing helper
- V2 compatibility parsing tests

### Optional module after skeleton confirms direction

- `data-generator-pipeline-v2`

For the skeleton phase, this can stay inside core/service if we want lower module churn.

## Work Breakdown

## M0 - Freeze skeleton contract

### Task M0.1

Define the minimum V2 YAML contract.

Required shape:

```yaml
name: demo_sql_v2

sources:
  input:
    type: iterator
    iterator:
      type: NUMBER
      from: 1
      to: 3

transform:
  type: sql
  sql: |
    SELECT value FROM input

sink:
  writers:
    - type: CONSOLE
```

Acceptance:

- one documented minimum valid template
- one documented invalid template
- singular first-phase shape is documented as a compatibility form, not the permanent end-state

### Task M0.2

Freeze first-phase routing rule.

Suggested rule:

- V1 if `fields` exists
- V2 if `sources` and `transform` exist

Acceptance:

- routing rule written into documentation and tests

## M1 - Add the V2 model skeleton

### Task M1.1

Add `TemplateV2VO`.

Suggested fields:

- `Long id`
- `Long instanceId`
- `String name`
- `Map<String, SourceVO> sources`
- `TransformVO transform` as first-phase compatibility shape or `List<TransformVO> transformers` with a one-item constraint
- `WriteStageVO sink` as first-phase compatibility shape or `List<WriteStageVO> sinks` with a one-item constraint
- optional `GeneratorVO generator`

Acceptance:

- class compiles
- can be parsed from YAML
- the class design does not force a second breaking rewrite when multiple transformers and sinks arrive

### Task M1.2

Add `SourceVO` base type.

Suggested subtype plan:

- `IteratorSourceVO`
- later `ReaderSourceVO`

Acceptance:

- base type registered in YAML parser
- at least one subtype registered

### Task M1.3

Add `TransformVO` base type and `SqlTransformVO`.

Suggested fields for `SqlTransformVO`:

- `String type`
- `String sql`
- optional `String dialect`

Acceptance:

- transform YAML parses
- missing SQL fails validation clearly

### Task M1.4

Decide sink modeling.

Recommended skeleton decision:

- reuse `WriteStageVO` for `sink`

Forward-compatible recommendation:

- prefer `List<WriteStageVO> sinks` in the internal model even if first-phase YAML still accepts singular `sink`

Reason:

- lowest cost
- avoids early duplication
- reduces later model churn when sink fan-out is enabled

Acceptance:

- V2 template can reuse current console writer config unchanged

### Task M1.5

Reserve the final plural model shape.

Recommended end-state:

- `Map<String, SourceVO> sources`
- `List<TransformVO> transformers`
- `List<WriteStageVO> sinks`

Acceptance:

- this target shape is documented in the code comments or plan references
- singular first-phase parsing can be normalized into the plural in-memory model

## M2 - Add row/schema skeleton

### Task M2.1

Add `ColumnDef`.

Suggested fields:

- `String name`
- `String logicalType` or `Class<?> javaType`
- `boolean nullable`

Acceptance:

- schema metadata can be instantiated in tests

### Task M2.2

Add `RowSchema`.

Suggested fields:

- `List<ColumnDef> columns`

Suggested helper methods:

- `column(String name)`
- `contains(String name)`

Acceptance:

- schema lookup helpers covered by unit tests

### Task M2.3

Add `Row`.

Recommended first version:

```java
record Row(Map<String, Object> values) {}
```

Suggested helper methods:

- `Object get(String name)`
- `String getString(String name)`

Acceptance:

- row values can be created and read in tests

## M3 - Add runtime abstraction skeleton

### Task M3.1

Add `RowSource`.

Suggested methods:

- `String name()`
- `RowSchema schema()`
- `Stream<Row> stream()`

Acceptance:

- interface compiles
- at least one fake implementation used in tests

### Task M3.2

Add `RowTransformer`.

Suggested methods:

- `RowSchema outputSchema()`
- `Stream<Row> transform(Map<String, RowSource> sources)`

Acceptance:

- interface compiles

### Task M3.3

Add `RowSink`.

Suggested methods:

- `void write(Stream<Row> rows)`

Acceptance:

- interface compiles

## M4 - Create the Calcite module skeleton

### Task M4.1

Create `data-generator-calcite/pom.xml`.

Acceptance:

- module participates in reactor build

### Task M4.2

Add Calcite dependencies.

Keep first version minimal.

Acceptance:

- module compiles
- no dependency conflict with Boot 4 baseline

### Task M4.3

Add `CalciteSqlValidator`.

Skeleton behavior:

- accept SQL string
- parse
- return validation result object

Acceptance:

- valid SQL passes
- invalid SQL returns structured error message

### Task M4.4

Add placeholder `CalcitePlanCompiler`.

Skeleton behavior:

- expose compile method
- may return a stub plan object in first commit

Acceptance:

- code path compiles and is test-callable

### Task M4.5

Add placeholder `CalciteRowTransformer`.

Skeleton behavior:

- hold SQL string
- invoke validator
- throw unsupported exception until execution lands

Acceptance:

- transform object can be constructed in tests

## M5 - Add iterator source skeleton

### Task M5.1

Add `IteratorSourceVO`.

Suggested fields:

- `String type`
- `IteratorVO iterator`

Acceptance:

- YAML can represent iterator source explicitly

### Task M5.2

Add `IteratorRowSource`.

First supported subtype:

- `NumberIteratorVO`

Suggested row shape:

- one default column named `value`

Acceptance:

- `NUMBER` iterator can produce `Row(value=1)` style rows

### Task M5.3

Document source schema rule.

Skeleton rule:

- scalar iterator output maps to one column: `value`

Acceptance:

- rule documented in plan and test names

## M6 - Add console sink skeleton

### Task M6.1

Add `ConsoleRowSinkAdapter`.

Behavior:

- accept `Stream<Row>`
- convert each row into the current console writer input contract

Acceptance:

- compiles without changing current `ConsoleWriter`

### Task M6.2

Define row-to-value adaptation boundary.

Recommended first decision:

- V2 sink adapter converts `Row` into a `MapValue`-like structure only at sink boundary

Acceptance:

- decision documented
- no V2 transform logic depends on old `Value` model internally

## M7 - Add service-side routing skeleton

### Task M7.1

Add template version detector.

Suggested helper:

- `TemplateDefinitionKind detect(String yaml)`

Kinds:

- `V1`
- `V2`
- `UNKNOWN`

Acceptance:

- routing helper has focused unit tests

### Task M7.2

Update YAML parsing support.

Acceptance:

- service parser can parse V1 templates
- service parser can parse V2 templates
- invalid mixed-shape templates fail clearly

### Task M7.3

Add V2-compatible cache path placeholder.

Acceptance:

- cache layer can represent template version even if execution is still partial

## M8 - Add minimum test set

### Task M8.1

Add V2 YAML parsing tests.

Suggested test cases:

- valid minimum V2 template
- missing `transform.sql`
- missing `sources`
- V1/V2 routing split

### Task M8.2

Add row/schema tests.

Suggested test cases:

- column lookup
- row lookup
- schema contains helper

### Task M8.3

Add Calcite validator tests.

Suggested test cases:

- `SELECT value FROM input`
- malformed SQL
- missing column

### Task M8.4

Add iterator row source tests.

Suggested test cases:

- number iterator emits expected rows

Acceptance:

- all skeleton tests pass under JDK 25

## Suggested Commit Breakdown

1. `docs: add calcite skeleton implementation plan`
2. `feat: add v2 template model skeleton`
3. `feat: add row and schema abstraction skeleton`
4. `feat: add calcite module and validator skeleton`
5. `feat: add iterator source and console sink skeleton`
6. `test: add v2 parsing, routing, and calcite validator coverage`

## Suggested Class Creation Order

1. `TemplateV2VO`
2. `TransformVO`
3. `SqlTransformVO`
4. `SourceVO`
5. `IteratorSourceVO`
6. `Row`
7. `RowSchema`
8. `ColumnDef`
9. `RowSource`
10. `RowTransformer`
11. `RowSink`
12. `CalciteSqlValidator`
13. `IteratorRowSource`
14. `ConsoleRowSinkAdapter`
15. service routing helper

## Exit Criteria

The skeleton phase is complete when all of the following are true:

- V2 templates can be parsed and identified reliably
- the reactor contains a `data-generator-calcite` module
- the new row/schema abstraction exists and is test-covered
- `NUMBER` iterator can be adapted into a V2 row source
- `SELECT value FROM input` can be validated through Calcite
- a console-only V2 sink adapter skeleton exists
- no existing V1 template path regresses

## Recommended Next Step After Skeleton

After this skeleton lands, the next concrete implementation phase should be:

1. make `CalciteRowTransformer` execute simple projections
2. add first-phase UDF registration
3. complete the first end-to-end path:
   `NUMBER iterator -> transform.sql -> console`

## Final-Phase Expansion After Skeleton

After the first V2 path is stable, the final expansion phase should add:

1. multiple named sources in one template
2. ordered `transformers` instead of a singular `transform`
3. ordered `sinks` instead of a singular `sink`

Recommended implementation order for that phase:

1. normalize the in-memory model to plural forms
2. add multi-source schema registration in Calcite
3. add transformer chaining with named intermediate outputs
4. add sink fan-out on the final result
5. add validation and end-to-end tests for all three dimensions
