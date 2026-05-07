# Template V2 Transformer Strategy

## Purpose

This document defines how Template V2 should evolve its transformer layer beyond the current SQL-first baseline.

It answers five questions:

- what transformer families V2 should support
- how SQL, UDFs, built-in non-SQL transformers, and custom transformers should be separated
- what the first official non-SQL transformer should be
- what validation, schema, plugin, and runtime rules all transformers should share
- how residual V1 script-heavy logic should migrate without recreating V1 stage sprawl

Related references:

- `docs/calcite-refactor-plan.md`
- `docs/calcite-templatev2-model-design.md`
- `docs/calcite-implementation-status.md`
- `docs/calcite-v1-parity-scorecard.md`
- `docs/calcite-v1-v2-mapping.md`
- `docs/template-v2-product-roadmap.md`
- `docs/calcite-plugin-framework-evaluation.md`

## Goal

Template V2 should be:

- SQL-first for relational logic
- not SQL-only for all future transformation logic
- open to repository-owned and plugin-provided transformer families
- explicit about when logic belongs in SQL, UDFs, built-in non-SQL transformers, or custom transformers

The transformer layer should not regress into V1's field-stage explosion.

## Design Principles

- keep SQL as the default authoring path for projection, filtering, joining, grouping, ordering, and lookup assembly
- do not force every residual business rule into SQL or UDFs once authoring becomes unnatural
- add built-in transformer families only when repeated scenario evidence justifies them
- keep built-in and plugin-provided transformers on one typed runtime contract
- prefer typed subtypes and explicit schema rules over opaque config maps
- keep transformer execution linear and ordered; avoid arbitrary DAG transformers
- keep transformer logic deterministic and side-effect-light unless a later scenario explicitly requires otherwise

## Transformer Taxonomy

V2 should treat transformers as one family of typed nodes, not one monolithic "script box".

### 1. SQL transformer

Primary type:

- `SqlTransformVO`

Best for:

- relational projection
- joins and lookups
- grouping and aggregation
- row filtering
- declarative field derivation
- most mapping, condition, and convert migrations

Current status:

- already implemented as the primary built-in transformer

### 2. Built-in non-SQL transformer

Primary goal:

- cover repeated residual business logic that is too awkward in SQL + UDF form, without reopening the old V1 stage model

Best for:

- deterministic row-local logic
- explicit row reshaping when SQL expression authoring becomes too noisy
- limited residual script scenarios that do not justify an external plugin

Current status:

- not implemented yet
- should be the next official transformer family after SQL

### 3. Custom transformer

Primary goal:

- support project-specific or industry-specific logic that should not become a built-in repository transformer

Best for:

- domain enrichment
- proprietary payload shaping
- externalized business rules
- specialized row-set logic owned by one project or extension package

Current status:

- the runtime contract already allows transform factories and plugin-provided transform subtypes
- the product still needs a formal strategy and sample path

## UDFs Are Not Transformers

This boundary should stay explicit.

UDFs are best used for:

- expression helpers inside SQL
- reusable scalar or small-function logic
- compact compatibility helpers for faker, date, string, and conversion behavior

UDFs should not become the answer for:

- large residual row-shaping flows
- whole-template business pipelines
- logic that needs its own schema contract, lifecycle, or diagnostics

## Recommended Decision Rules

The following rules should guide future template authoring and feature planning.

### Use SQL when

- more than one logical relation is involved
- joins, grouping, ordering, filtering, or aggregation are central
- the desired logic reads naturally as declarative relational projection
- the transformation should stay easy to analyze with Calcite validation and explain tooling

### Use SQL + UDF when

- the logic is still expression-like
- the missing capability is local to one or more scalar expressions
- the same helper should be reusable inside many SQL templates

### Use a built-in non-SQL transformer when

- the logic is still common across many templates
- SQL + UDF authoring becomes awkward or hard to maintain
- the logic is deterministic and fits a clear row-to-row or row-set contract
- promoting it to a built-in family reduces authoring complexity without adding V1-style sprawl

### Use a custom transformer when

- the logic is project-specific or industry-specific
- the behavior is not broadly reusable enough for the core repository
- a dedicated package, plugin lifecycle, or version boundary is desirable

### Keep logic out of transformers when

- it is orchestration control such as pause, scheduling, or branching
- it is primarily observability or logging behavior
- it introduces shared mutable runtime state that belongs in orchestration instead of transformation

## Recommended First Built-In Non-SQL Transformer

The first official non-SQL transformer should be a row-local deterministic transformer rather than a generic free-form engine.

### Candidate options

| Candidate | Strengths | Risks | Recommendation |
|---|---|---|---|
| Generic `ScriptTransformVO` | broad flexibility, easy V1 script story | can become an unbounded replacement for disciplined design | not recommended as the first shape |
| `RowScriptTransformVO` | explicit row-local scope, clear migration target for residual scripts, simpler schema contract | still needs careful language and sandbox design | recommended first built-in non-SQL transformer |
| Dedicated mapping/condition/convert transformers | simple mental model | recreates V1 stage families under new names | do not pursue |
| Payload-template transformer | useful later for document shaping | depends on a richer nested row model | defer |

### Recommended shape

Recommended first official family:

- `RowScriptTransformVO`

Primary semantics:

- input: one current relation
- execution: one row in, one row out
- scope: row-local deterministic transformation only
- output: explicit or inferable tabular schema
- side effects: not allowed
- external I/O: not allowed in the built-in implementation

Why this is the best first step:

- it covers residual V1 SpEL and script-heavy logic better than forcing more SQL growth
- it avoids reopening arbitrary multi-relation semantics that SQL already handles better
- it gives custom transformer authors a concrete typed contract to follow

## Recommended Transformer Model

All transformer families should remain subtypes of one base model.

Base type:

```java
public abstract class TransformVO implements Serializable {
    private String name;
    private String type;
}
```

Existing relational subtype:

```java
public class SqlTransformVO extends TransformVO {
    private String dialect;
    private String sql;
}
```

Recommended first non-SQL subtype:

```java
public class RowScriptTransformVO extends TransformVO {
    private String engine;
    private String script;
    private RowSchema outputSchema;
}
```

Model guidance:

- `engine` should be repository-curated, not arbitrary by default
- `outputSchema` may be optional only if the engine can infer schema reliably
- transformer names remain optional for a single transformer and required for multi-transform chains

## Recommended Runtime Contract Evolution

The current runtime already has a transform factory SPI. The next step is to make validation and schema behavior first-class.

The strategy should stay centered on one typed extension boundary:

- `V2TransformFactory`

Recommended future responsibilities for transform factories:

- declare whether the factory supports the transform subtype
- validate the transform configuration before execution
- resolve or validate output schema
- execute against the current runtime context

Possible refinement directions:

1. extend `V2TransformFactory` with default methods for validation and schema resolution
2. keep `V2TransformFactory` lean and introduce companion validator/schema interfaces

Current recommendation:

- prefer default methods on the main factory interface if the implementation remains simple
- only split interfaces if the factory API becomes too crowded in practice

## Schema Strategy

Transformer chaining is only maintainable if schema rules are explicit.

### SQL transformer schema

- infer output schema from validated SQL projection where practical
- allow explicit overrides only if the repository later needs them

### Row-script transformer schema

- require explicit `outputSchema` in the first version unless inference is trivial and reliable
- treat schema mismatch as a validation error, not a late runtime surprise

### Custom transformer schema

- require one of:
  - explicit `outputSchema` in the model
  - factory-provided schema inference
  - factory-provided schema validation against a declared input/output contract

### Common rule

- every transformer in a chain must expose a stable output schema before the next transformer is validated

## Execution Semantics

Transformer execution should remain linear and explicit.

Shared rules:

- transformers execute in declared order
- each transformer consumes the current relation snapshot plus any explicitly allowed named relations
- each transformer publishes one named output relation for the next stage
- in-flight runs keep the runtime registry snapshot captured at run start

Specific guidance:

- SQL remains the only built-in family that should freely compose across multiple relations
- the first built-in non-SQL transformer should consume one current relation only
- if a scenario fundamentally depends on multi-relation assembly, prefer SQL over non-SQL transformers

## Validation Requirements

All transformer families should follow comparable validation discipline.

Required validation fields:

- transform type resolvable
- transform configuration complete and non-blank where required
- transformer name rules satisfied
- output schema available before downstream validation

Additional SQL validation:

- SQL parse and semantic validation
- source and column resolution
- function and aggregate rules

Additional row-script validation:

- engine allowed by repository policy
- script compiles or parses successfully
- script contract matches declared output schema
- no forbidden side-effect APIs or unsupported language modes

Additional custom transformer validation:

- subtype and factory both resolve through the runtime registry
- plugin version and capability checks pass
- input/output contract is available to the host

## Plugin Strategy

Built-in and plugin-provided transformers should converge on the same host runtime model.

Requirements:

- plugin-provided transform subtypes register into the same codec path as built-in transforms
- plugin-provided transform factories participate in the same runtime registry
- validation and diagnostics should identify plugin id, transform subtype, and factory class
- PF4J remains the preferred external plugin loading and isolation path
- in-flight task behavior during plugin refresh should remain snapshot-based

Recommended minimum plugin sample:

- one custom `TransformVO` subtype
- one `V2TransformFactory`
- one test fixture proving subtype parsing, validation, execution, refresh, and failure diagnostics

## Migration Guidance From V1

The migration program should use the following transformer choices.

### Migrate to SQL first

Best targets:

- mapping
- condition
- convert
- lookup joins
- grouped or deduplicated reshaping
- simple SpEL expressions

### Migrate to SQL + UDF next

Best targets:

- common faker/date/string helpers
- reusable scalar compatibility logic
- expression-like project utilities

### Migrate to row-script transformer when

- the logic is row-local but too awkward in SQL
- the rule chain is still deterministic
- promoting every part into a UDF would make the SQL unreadable

### Migrate to custom transformer when

- the logic is domain-specific
- the implementation belongs with one project or plugin package
- a typed extension path is more maintainable than growing the core transformer catalog

### Keep compatibility-only for now when

- the old logic is orchestration-heavy
- the old JavaScript depends on mutable state, side effects, or implicit runtime objects
- the scenario still needs a V2 orchestration design, not only a new transformer

## Phased Delivery Plan

### Phase T0. Finalize the strategy and contract

- define the official transformer taxonomy
- choose the first built-in non-SQL transformer family
- decide factory validation and schema responsibilities
- document SQL/UDF vs built-in non-SQL vs custom transformer decision rules

### Phase T1. Deliver the first built-in non-SQL transformer

- add the first transformer subtype
- add validation and schema rules
- add focused execution and chaining tests
- add migration examples that show why this transformer exists

### Phase T2. Deliver the formal custom transformer path

- add one PF4J-backed custom transformer sample
- add diagnostics and refresh coverage
- document authoring and packaging rules for custom transformers

### Phase T3. Re-evaluate further built-in transformer families

- review real template evidence before adding more families
- only add another built-in family if repeated use cases remain awkward after SQL, UDF, and row-script coverage

## Acceptance Criteria

The transformer strategy is successful when:

- V2 no longer appears SQL-only
- authors have a clear rule for choosing SQL, UDF, built-in non-SQL, or custom transformers
- the first non-SQL transformer reduces migration pressure from residual V1 scripts
- custom transformer examples can run through the same validation and plugin lifecycle path as built-in transforms
- the repository avoids rebuilding a one-to-one clone of V1 stages

## Non-Goals

The near-term strategy should explicitly avoid:

- arbitrary transformer DAG execution
- a built-in transformer family for every old stage type
- a single opaque `CustomTransformVO` catch-all model
- unrestricted built-in scripting with side effects and external I/O
- using custom transformers as a substitute for missing relational SQL features that should stay in Calcite
