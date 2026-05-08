# Template V2 Transformer Capability Metadata Model Proposal

## Purpose

This document proposes a concrete metadata model for Template V2 transformers.

The goal is to let the runtime, validation layer, explain output, and plugin system understand what a transformer can do without having to inspect implementation details.

It answers six questions:

- how a transformer should describe its execution shape
- how SQL, row-local non-SQL, and custom plugin transformers should declare capabilities
- how capability metadata should support validation and planning
- how plugin-provided transformers should participate
- how the model should stay lightweight enough to remain practical
- how the proposal aligns with the current roadmap

Related references:

- `docs/template-v2-transformer-strategy.md`
- `docs/template-v2-policy-to-runtime-mapping-guide.md`
- `docs/template-v2-product-roadmap.md`
- `docs/template-v2-execution-scalability-plan.md`
- `docs/calcite-implementation-status.md`

## Goal

Template V2 transformers should be able to declare:

- execution shape
- input contract
- output contract
- supported composition rules
- plugin provenance

This metadata should help the platform answer:

- can this transformer run in the selected mode
- does this transformer need full materialization
- can this transformer be chained safely
- does this transformer depend on a plugin capability

## Design Principles

- keep the capability model small and structured
- keep it descriptive, not behaviorally huge
- expose enough metadata for validation and explain
- let plugins contribute capability data through the same host contract
- avoid a separate bespoke metadata schema for every transformer family unless a family genuinely needs it

## Why This Is Needed

Without capability metadata:

- explain output can only describe the transform type at a surface level
- validation cannot easily warn about scale risks
- execution policy cannot know whether a transformer supports chunked execution
- plugin transforms are harder to reason about

## Proposed Core Metadata Shape

Illustrative direction:

```java
public class TransformerCapabilityVO implements Serializable {
    private String name;
    private String type;
    private String family;
    private String executionShape;
    private boolean requiresSingleInput;
    private boolean requiresMaterialization;
    private boolean supportsChunking;
    private boolean supportsStreaming;
    private RowSchema inputSchema;
    private RowSchema outputSchema;
}
```

## Field Semantics

### name

- human-readable transformer name
- may be optional for a single transformer, but should be visible in reports where available

### type

- transformer subtype identity
- should match the `TransformVO` subtype or factory-supported type

### family

Possible examples:

- `sql`
- `row-script`
- `custom`
- plugin-defined family id

### executionShape

Recommended values:

- `ROW_LOCAL`
- `CHUNK_LOCAL`
- `MATERIALIZATION_REQUIRED`

Semantics:

- `ROW_LOCAL` means each row can be processed independently
- `CHUNK_LOCAL` means bounded batches are acceptable and useful
- `MATERIALIZATION_REQUIRED` means the transformer needs a full relation or substantial preloading

### requiresSingleInput

- indicates whether the transformer reads one active input relation only

### requiresMaterialization

- indicates whether full materialization or a full preloaded relation is necessary

### supportsChunking

- indicates whether chunked execution is meaningful and safe

### supportsStreaming

- indicates whether streaming-oriented execution is a realistic contract for this transformer

### inputSchema

- declared or resolved input schema

### outputSchema

- declared or resolved output schema

## Recommended Capability Classification Rules

### SQL transformer

Typical metadata:

- family: `sql`
- executionShape: often `MATERIALIZATION_REQUIRED` for joins, grouping, ordering, and aggregates
- supportsChunking: maybe, but only where the runtime can prove safety
- requiresSingleInput: false for multi-source SQL, true for simple one-input SQL

### Row-script transformer

Typical metadata:

- family: `row-script`
- executionShape: `ROW_LOCAL`
- requiresSingleInput: true
- supportsChunking: yes
- supportsStreaming: potentially yes if side-effect-free and schema-stable

### Custom transformer

Typical metadata:

- family: `custom`
- executionShape: declared by plugin or repository implementation
- requiresSingleInput / requiresMaterialization / supportsChunking set explicitly

## Metadata Source

The metadata can come from one or more places.

### 1. Model-declared metadata

Best when:

- the author should be able to see the behavior directly in the template

### 2. Factory-declared metadata

Best when:

- the capability is better known by the runtime implementation than by template authors

### 3. Validation-resolved metadata

Best when:

- the runtime resolves the effective schema or capability from the active environment

Recommended rule:

- the runtime should prefer declared metadata where available and supplement it with factory-resolved metadata where necessary

## Validation Uses

Capability metadata should support validation decisions such as:

- whether the transformer chain can remain chunked
- whether a transformer requires full materialization
- whether a transformer can participate in a given execution mode
- whether a plugin transform is missing required capability declarations

Example validation questions:

- can this transformer run with `CHUNKED` execution policy
- does the output schema exist before the next transformer is validated
- is this plugin transform allowed in the current environment

## Explain Uses

Explain output should surface:

- transformer family
- execution shape
- materialization requirement
- chunking support
- streaming support if relevant
- plugin provenance if the transformer is external

This helps users understand why a template is expensive or why a custom transformer is allowed or blocked.

## Runtime Planning Uses

Execution planning can use metadata to:

- reject unsupported execution mode combinations
- warn about materialization-heavy transforms
- decide whether chunking or fallback is possible
- surface whether a transformer needs a different execution path

## Plugin Integration

Plugin-provided transformers should be able to contribute capability metadata through the same host-side contract.

Recommended plugin responsibilities:

- declare capability metadata
- declare family and subtype identity
- declare schema expectations
- declare execution shape

Recommended host responsibilities:

- validate capability metadata against policy
- expose capability metadata in diagnostics
- preserve plugin identity in reports

## Recommended Model Direction

### Option A. Keep only implicit capability behavior

Strengths:

- simplest short-term implementation

Weaknesses:

- hard to plan for scale
- weak explainability
- poor plugin introspection

Recommendation:

- not sufficient for the target product

### Option B. Add a small explicit capability metadata model

Strengths:

- supports validation, explain, and execution planning
- useful for both built-in and plugin transformers

Weaknesses:

- requires some extra model surface

Recommendation:

- preferred direction

## Recommended Delivery Plan

### Phase C0. Define capability vocabulary

- finalize execution shape values
- decide which capability fields are mandatory

### Phase C1. Add metadata to built-in transformers

- annotate SQL and row-script transformer families
- surface metadata in explain output

### Phase C2. Add plugin capability metadata path

- allow PF4J-provided transformers to contribute the same metadata
- validate missing or conflicting capability data

### Phase C3. Use metadata in execution planning

- let policy and runtime planning consume the capability model

## Acceptance Criteria

The metadata proposal is useful when:

- validators can warn about unsupported execution shapes
- explain output can show whether a transformer is row-local or materialization-heavy
- plugin transformers can advertise their behavior without custom ad hoc code
- execution policy can make smarter decisions from the same metadata

## Non-Goals

The proposal should avoid:

- making metadata so detailed that it duplicates the whole implementation
- forcing every transformer family into the exact same field semantics
- encoding plugin behavior in a way that bypasses validation and governance
