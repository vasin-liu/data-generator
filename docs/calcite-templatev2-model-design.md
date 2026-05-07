# TemplateV2 Model Design

## Goal

Define the final in-memory model shape for the Calcite-based V2 template path, while preserving backward-compatible parsing for the first-phase singular `transform` and `sink` forms.

The final model should treat `sql` as the default transformer family, not the only transformer family. It must leave room for additional repository-owned transformer types and typed custom/plugin-provided transformers without another model rewrite.

## Design Principles

- The in-memory model should target the final plural form from day 1.
- YAML compatibility can accept singular and plural forms during the migration window.
- Parsing normalization should happen as early as possible.
- Runtime execution should only see normalized V2 objects.
- Avoid a second model rewrite when multiple sources, transformers, and sinks are enabled.

## Final Target Shape

Recommended `TemplateV2VO` shape:

```java
public class TemplateV2VO implements Serializable {
    private Long id;
    private Long instanceId;
    private String name;
    private GeneratorVO generator;
    private Map<String, SourceVO> sources;
    private List<TransformVO> transformers;
    private List<WriteStageVO> sinks;
}
```

## YAML Compatibility Shapes

### First-phase compatibility form

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

### Final plural form

```yaml
name: demo_sql_v2_final

sources:
  input:
    type: iterator
    iterator:
      type: NUMBER
      from: 1
      to: 3
  dict_city:
    type: reader
    reader:
      type: JDBC

transformers:
  - name: normalize
    type: sql
    sql: |
      SELECT value AS id FROM input
  - name: enrich
    type: sql
    sql: |
      SELECT n.id, d.city_name
      FROM normalize n
      LEFT JOIN dict_city d ON n.id = d.id

sinks:
  - writers:
      - type: CONSOLE
  - writers:
      - type: KAFKA
        dataSourceId: kafka1
        target: demo_topic
```

## Parsing Compatibility Rules

### Rule 1

If `transform` exists and `transformers` does not exist:

- normalize into a one-item `transformers` list

### Rule 2

If `sink` exists and `sinks` does not exist:

- normalize into a one-item `sinks` list

### Rule 3

If both singular and plural forms are present:

- reject as invalid

Reason:

- avoid ambiguous precedence
- keep authoring rules simple

### Rule 4

If `sources` is empty or missing:

- reject as invalid

### Rule 5

If `transformers` is empty after normalization:

- reject as invalid

### Rule 6

If `sinks` is empty after normalization:

- reject as invalid

### Rule 7

If duplicate source names exist:

- reject as invalid

### Rule 8

If duplicate transformer names exist:

- reject as invalid

### Rule 9

Transformer names are required in plural form once the template contains more than one transformer.

Suggested rule:

- optional for one transformer
- required for two or more transformers

### Rule 10

Runtime should never execute singular `transform` or `sink` forms directly.

- all singular forms must be normalized first

## Source Model

Recommended base type:

```java
public abstract class SourceVO implements Serializable {
    private String type;
}
```

Recommended first subtypes:

```java
public class IteratorSourceVO extends SourceVO {
    private IteratorVO iterator;
}

public class ReaderSourceVO extends SourceVO {
    private ReaderVO reader;
    private List<ParamVO> params;
    private RowSchema schema;
}
```

Notes:

- `sources` should be a named map, not a list
- source name is the logical table name exposed to Calcite
- explicit schema is preferred where inference is weak
- `DatabaseIterator` and `JdbcReader` should converge into one long-term query-backed source family
- selection behavior should be modeled as source policy, not as transform SQL
- AI-backed generation should have an official `AiSourceVO`

### Additional source model recommendations

Suggested future source subtypes:

```java
public class QuerySourceVO extends SourceVO {
    private String dataSourceId;
    private String sql;
    private List<ParamVO> params;
    private RowSchema schema;
    private SourcePolicyVO policy;
}

public class AiSourceVO extends SourceVO {
    private String api;
    private ProviderVO provider;
    private String prompt;
    private String parser;
    private RowSchema schema;
    private SourcePolicyVO policy;
}
```

Suggested source policy holder:

```java
public class SourcePolicyVO implements Serializable {
    private Boolean inMemory;
    private String materialization;
    private String selectionStrategy;
    private Integer limit;
}
```

## Transform Model

Recommended base type:

```java
public abstract class TransformVO implements Serializable {
    private String name;
    private String type;
}
```

Recommended first subtype:

```java
public class SqlTransformVO extends TransformVO {
    private String dialect;
    private String sql;
}
```

Recommended future built-in subtype:

```java
public class ScriptTransformVO extends TransformVO {
    private String language;
    private String script;
    private RowSchema outputSchema;
}
```

Notes:

- `transformers` should be an ordered list
- each transformer consumes the current relation namespace
- each transformer may publish a named intermediate relation
- `sql` should stay the preferred built-in transformer for relational work
- V2 should still support a small number of additional built-in transformer families where repeated business scenarios are awkward in SQL/UDF form
- custom transformers should extend `TransformVO` through typed subtypes registered in the same YAML/JSON subtype registry path; do not rely on one generic opaque `CustomTransformVO` as the primary extension model
- every non-SQL/custom transformer should either infer its output schema reliably or require an explicit `outputSchema`
- additional transformer families should not recreate one-to-one equivalents of old V1 stages unless a repeated business scenario proves the need

### Recommended transformer family strategy

1. `SqlTransformVO`
   - default for projection, filter, join, aggregate, lookup, and most row-local expression rewrites
2. `ScriptTransformVO` or equivalent deterministic expression/script transformer
   - reserved for high-value residual logic that is too awkward to express as SQL + UDF
   - should stay deterministic and side-effect-light
3. typed custom transformer subtypes
   - repository-owned or PF4J-provided
   - should be used for project-specific enrichment, payload shaping, or dataset logic that does not fit the built-in families cleanly

Selection rule:

- prefer SQL first
- introduce an official non-SQL built-in transformer only after repeated scenario evidence
- use a typed custom transformer when the logic is business-specific and not broadly reusable across templates

## Sink Model

Recommended first-phase choice:

- reuse `WriteStageVO`

Final in-memory shape:

```java
private List<WriteStageVO> sinks;
```

Compatibility input shape:

- `sink` -> normalize into `sinks[0]`

Notes:

- sink fan-out should write the final transform result
- first implementation should execute sinks sequentially
- later parallel sink execution can be optional
- sink failure strategy should be configurable

Suggested sink execution policy:

```java
public class SinkExecutionPolicyVO implements Serializable {
    private String mode;
}
```

Suggested modes:

- `FAIL_FAST`
- `BEST_EFFORT`
- `CONTINUE_AND_REPORT`

## Intermediate Transformer Semantics

### Recommended execution rule

- `sources` populate the initial Calcite schema namespace
- transformer `0` reads from the source namespace
- transformer `n+1` can read:
  - all original sources
  - prior named transformer outputs

### First supported chaining model

- linear ordered chaining only

Example:

- `normalize`
- `enrich`
- `finalize`

Not supported in the first multi-transformer phase:

- arbitrary transformer DAG
- branching transformer graph
- cycle detection beyond linear naming validation

## Suggested Validation Rules

### Template-level validation

- `name` must not be blank
- `sources` must not be empty
- `transformers` must not be empty
- `sinks` must not be empty

### Source validation

- every source key must be unique
- source type must be resolvable
- source schema must be available before transform validation

### Transform validation

- every transformer type must be resolvable
- SQL text must not be blank
- transformer names must be unique when present
- if more than one transformer exists, all transformers must have names
- non-SQL/custom transformers must expose enough metadata to validate input/output schema handoff
- if a transformer cannot infer output schema, explicit schema should be required at model-validation time
- plugin-provided custom transformers should fail validation early if their subtype or factory is missing after registry refresh

### Sink validation

- each sink must contain at least one writer
- each writer must pass existing writer validation

## Suggested Class Additions

### Core model

- `TemplateV2VO`
- `SourceVO`
- `IteratorSourceVO`
- `ReaderSourceVO`
- `TransformVO`
- `SqlTransformVO`
- `ScriptTransformVO` or equivalent first non-SQL transformer type when the repository chooses one

### Parsing and normalization

- `TemplateV2Normalizer`
- `TemplateDefinitionKind`
- `TemplateDefinitionDetector`

### Validation

- `TemplateV2Validator`
- `TemplateV2ValidationResult`

## Suggested Responsibilities

### `TemplateDefinitionDetector`

Responsibilities:

- detect V1
- detect V2
- reject ambiguous mixed shapes

### `TemplateV2Normalizer`

Responsibilities:

- convert singular `transform` to plural `transformers`
- convert singular `sink` to plural `sinks`
- apply default names if allowed by policy

### `TemplateV2Validator`

Responsibilities:

- structural validation
- name conflict validation
- early source/transform/sink shape validation

## Suggested Implementation Order

1. add `TemplateV2VO`
2. add `SourceVO` and `IteratorSourceVO`
3. add `TransformVO` and `SqlTransformVO`
4. add plural `sinks` internal field strategy
5. add `TemplateDefinitionDetector`
6. add `TemplateV2Normalizer`
7. add `TemplateV2Validator`
8. add parsing tests for singular/plural compatibility

## Minimum Test Matrix

- [ ] singular `transform` normalizes into `transformers[0]`
- [ ] singular `sink` normalizes into `sinks[0]`
- [ ] plural form parses directly
- [ ] mixed singular and plural forms are rejected
- [ ] duplicate source names are rejected
- [ ] duplicate transformer names are rejected
- [ ] empty `sources` is rejected
- [ ] empty `transformers` is rejected
- [ ] empty `sinks` is rejected

## Recommendation

The code should adopt the plural final shape internally now, even if the first public YAML examples stay simple.

The transform model should also stay open from day 1 for:

- the built-in SQL path
- one or more future repository-owned non-SQL transformer families
- typed custom/plugin-provided transformer subtypes using the same registry and validation contract

That keeps the first V2 implementation easy to author while avoiding later churn when:

- multiple logical tables are introduced
- multiple ordered transforms are introduced
- final-result sink fan-out is enabled
