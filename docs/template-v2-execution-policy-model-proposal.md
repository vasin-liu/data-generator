# Template V2 Execution Policy Model Proposal

## Purpose

This document proposes a concrete model direction for Template V2 execution policy.

It turns the scalability and control-plane planning into a candidate runtime model for:

- execution mode
- bounded memory behavior
- source chunking
- sink batching
- failure thresholds
- preview and dry-run limits

Related references:

- `docs/template-v2-execution-scalability-plan.md`
- `docs/template-v2-control-plane-requirements.md`
- `docs/template-v2-product-roadmap.md`
- `docs/template-v2-policy-to-runtime-mapping-guide.md`
- `docs/calcite-implementation-status.md`

## Goal

Template V2 should expose an execution policy model that is:

- explicit
- minimal
- extensible
- visible in validation, explain, and run reports

The policy model should make scalable execution choices observable without forcing every template into a complicated configuration surface.

## Design Principles

- keep defaults simple
- make scale-related overrides explicit
- fail early on unsupported combinations
- keep the policy expressive enough to cover in-memory, chunked, and future streaming-oriented execution
- ensure policy behavior is visible in control-plane reports
- avoid turning execution policy into a dumping ground for unrelated connector settings

## Current Baseline

The current runtime already has:

- in-memory execution behavior
- sink failure policy behavior
- source policy behavior
- registry snapshot semantics during a run

What is missing is a unified policy surface for:

- memory limits
- chunking
- batching
- row-count protection
- preview limits
- scale warnings

## Proposed Policy Shape

Recommended model direction:

```java
public class ExecutionPolicyVO implements Serializable {
    private String mode;
    private Integer maxRowsInMemory;
    private Integer previewRowLimit;
    private Integer sourceChunkSize;
    private Integer sinkBatchSize;
    private Boolean failOnLimitExceeded;
}
```

## Field Semantics

### mode

Possible values:

- `IN_MEMORY`
- `CHUNKED`
- `STREAMING`

Semantics:

- `IN_MEMORY` keeps the current lightweight behavior
- `CHUNKED` enables bounded read/write batches where supported
- `STREAMING` is reserved for future scenarios and should not be implied if the runtime cannot support it

### maxRowsInMemory

Purpose:

- upper bound for rows held in memory during a run or stage

Recommended rule:

- if exceeded and `failOnLimitExceeded=true`, fail early with a scale diagnostic
- if exceeded and policy permits fallback, use bounded behavior or warn explicitly

### previewRowLimit

Purpose:

- cap preview output so authoring and explain workflows remain bounded

Recommended default:

- small safe value such as 100 or a repository default

### sourceChunkSize

Purpose:

- chunk size for source reads when the source supports bounded reads

Recommended usage:

- query sources
- file sources
- future plugin-defined sources that support chunking

### sinkBatchSize

Purpose:

- batch size for sink writes where batching is supported

Recommended usage:

- JDBC
- Kafka
- Elasticsearch
- future plugin sinks

### failOnLimitExceeded

Purpose:

- decide whether the run should fail immediately when a limit is exceeded

Recommended default:

- `true` for safety in production-like environments

## Policy Ownership

Execution policy should be treated as a template-owned behavior with environment-aware defaults.

Recommended ownership split:

- template can override safe policy values when allowed
- environment can set lower or higher hard limits
- control plane should display the effective policy used by the run

## Validation Rules

Required validation rules:

- mode value known or plugin-supported
- numeric values positive where present
- illegal combinations rejected
- policy compatible with source and sink capabilities
- policy-compatible preview and dry-run behavior

Examples of invalid combinations:

- `STREAMING` mode with a source that only supports full materialization
- `sourceChunkSize` set for a source family that cannot chunk
- `sinkBatchSize` set to zero or negative
- `maxRowsInMemory` less than `previewRowLimit` if the repository chooses to enforce that as a policy

## Runtime Mapping

### Execution mode mapping

- validation determines whether the template may run in the selected mode
- execution layer selects the actual strategy
- explain reports should display the effective mode

### Memory limit mapping

- execution layer enforces row-count or materialization ceilings
- control plane reports limit breach or near-limit warnings

### Chunking and batching mapping

- source adapters and sink adapters should interpret the policy where they support it
- unsupported families should fail validation or emit clear warnings

## Control-Plane Visibility

The policy should be visible in:

- validate output
- explain output
- preview output
- run reports

Visible fields should include:

- effective execution mode
- source chunk size
- sink batch size
- preview limit
- memory limit
- whether fallback or limit failure was applied

## Source Integration

Source policies and execution policies should remain distinct but coordinated.

Recommended division:

- `SourcePolicyVO`
  - source materialization and selection behavior
- `ExecutionPolicyVO`
  - run-level memory and batching behavior

This prevents one model from becoming overloaded.

## Sink Integration

Sink execution policy should be able to coexist with execution policy.

Recommended direction:

- execution policy handles cross-cutting run limits and chunk sizing
- sink policy handles failure mode, partial success, and sink-specific delivery behavior

## Plugin Integration

Plugin-defined transformers or sources may declare whether they support execution policy fields.

Recommended metadata expectations:

- whether chunking is supported
- whether the source or sink can honor batch size
- whether the transformer requires materialization

## Recommended Delivery Plan

### Phase X0. Establish the policy surface

- define `ExecutionPolicyVO`
- define mode values and defaults
- decide template override versus environment default precedence

### Phase X1. Integrate preview and validation

- make policy visible in validation and explain
- enforce obvious invalid combinations

### Phase X2. Integrate source and sink batching

- source chunking support
- sink batch sizing support

### Phase X3. Integrate reporting and diagnostics

- show effective policy in run reports
- surface limit breaches and fallback decisions

## Acceptance Criteria

The execution policy proposal is useful when:

- template authors can control scale behavior explicitly
- preview and run outputs expose the effective policy
- large workloads are bounded by policy instead of relying on accident
- source and sink adapters can opt into policy support family by family

## Non-Goals

The proposal should avoid:

- turning every connector option into a generic execution policy field
- forcing streaming semantics before the runtime can truly support them
- hiding materialization-heavy behavior behind the same policy as bounded execution
