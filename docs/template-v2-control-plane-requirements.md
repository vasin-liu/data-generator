# Template V2 Control Plane Requirements

## Purpose

This document defines the control-plane requirements for Template V2.

The execution core alone is not enough for a product-grade V2 path. The product also needs validation, preview, explainability, run lifecycle management, and observable run reports.

This document answers five questions:

- what control-plane capabilities V2 must expose
- how templates should be validated and previewed before execution
- how runs should be started, retried, replayed, cancelled, and observed
- what reports and diagnostics should exist for sources, transformers, and sinks
- how the control plane should stay aligned with plugins, datasource refresh, and V1 migration

Related references:

- `docs/template-v2-product-roadmap.md`
- `docs/template-v2-transformer-strategy.md`
- `docs/template-v2-datasource-and-secret-governance.md`
- `docs/calcite-implementation-status.md`
- `docs/calcite-refactor-plan.md`

## Goal

Template V2 should expose a control plane that makes templates:

- understandable before runtime
- debuggable during authoring
- observable during execution
- diagnosable after failure
- governable across plugin and datasource refresh boundaries

## Design Principles

- validation should fail early and with actionable diagnostics
- preview should be cheap, bounded, and safe by default
- explain output should clarify what sources, transformers, and sinks will do before the run starts
- run reports should be scenario-oriented, not just stack traces
- control-plane APIs should remain stable even if the runtime engine evolves
- plugin, datasource, and template version snapshots should be visible in run metadata

## Scope

In scope:

- validate
- explain
- preview
- dry-run
- run lifecycle
- retry and replay
- cancellation
- run reporting
- source / transformer / sink diagnostics

Not in scope:

- visual UI design
- scheduler infrastructure internals
- cluster-wide resource management outside product-level reporting and policy

## Control Plane Capability Model

The control plane should expose at least the following product actions.

### 1. Validate

Purpose:

- check template structure, source definitions, transformer chain, sink definitions, policy rules, and extension resolution before execution

Expected output:

- `valid`
- errors
- warnings
- compatibility notes

Typical use:

- author saves or publishes a template
- migration assistant emits a draft
- plugin or datasource refresh changes capability resolution

### 2. Explain

Purpose:

- show the execution plan at a human-readable level before running

Expected output:

- source inventory
- source schemas
- transformer chain summary
- sink execution summary
- plugin and connection references involved

Typical use:

- preflight review
- migration review
- production change approval

### 3. Preview

Purpose:

- execute a bounded sample to show representative rows and schema handoff without a full task run

Expected output:

- sample rows
- source and transformer output schema snapshots
- warnings when preview differs from full-run semantics

Typical use:

- authoring and debugging
- V1 to V2 migration comparison

### 4. Dry-run

Purpose:

- validate as much runtime behavior as possible without performing final external side effects

Expected output:

- resolved sources
- resolved transformer chain
- sink preflight status
- no committed write side effects

Typical use:

- operational preflight
- production safety check before first run

### 5. Run

Purpose:

- perform full execution and produce a durable run report

Expected output:

- run id
- lifecycle status
- start and end timestamps
- source / transformer / sink metrics
- final success or failure summary

### 6. Retry / Replay / Cancel

Purpose:

- recover from transient failures
- rerun historical templates under explicit version snapshots
- stop long or faulty runs

Expected output:

- a new run record referencing the prior one for retry or replay
- cancellation outcome and safe-stop status

## Validation Requirements

Validation should be layered.

### Structural validation

- template shape valid
- sources present and uniquely named
- transformers present and validly ordered
- sinks present and non-empty

### Capability validation

- transform subtype resolvable
- source and sink families resolvable
- plugin-provided types and factories present

### Schema validation

- each source exposes schema
- each transformer publishes output schema
- sink mapping or writer contract matches available output

### Policy validation

- datasource governance rules satisfied
- secret policy satisfied
- plugin policy satisfied
- environment restrictions satisfied

### Compatibility validation

- V1 migration approximations or unsupported semantics surfaced as warnings, not hidden

## Explain Requirements

Explain output should not be a raw engine dump only.

Minimum sections:

- template identity and version
- source list and schema summary
- connection or provider references used
- transformer chain summary with type and name
- sink plan and failure policy
- plugin set and extension contributors if non-default components are involved

Recommended future additions:

- relational SQL summary for SQL transforms
- cost or risk hints for heavy joins or large materialization
- migration-boundary notes where exact V1 parity is not guaranteed

## Preview Requirements

Preview should be bounded and safe.

Default rules:

- bounded row count
- bounded source read scope
- no final external writes
- explicit note when preview omits sink side effects or full-scale semantics

Preview should support:

- source sample preview
- per-transformer intermediate preview
- final output preview

Special rules:

- AI source preview should capture parser and provider diagnostics
- plugin-provided transformers should still show their declared schema and stage name

## Dry-Run Requirements

Dry-run is broader than preview.

It should aim to:

- resolve runtime dependencies
- test connector reachability where safe
- compile SQL and transformer chains
- preflight sink capability where possible
- stop short of irreversible side effects

Dry-run should record:

- what was fully validated
- what was only structurally validated
- what was intentionally skipped

## Run Lifecycle Requirements

The control plane should standardize run states.

Recommended states:

- `CREATED`
- `VALIDATING`
- `READY`
- `RUNNING`
- `SUCCEEDED`
- `FAILED`
- `CANCELLING`
- `CANCELLED`

Recommended metadata:

- run id
- template id and version
- runtime registry snapshot id
- plugin snapshot
- datasource snapshot or connection refs
- trigger type such as manual, scheduled, retry, or replay

## Retry And Replay Requirements

Retry and replay should not be conflated.

### Retry

- re-executes after failure
- typically uses the same template version unless explicitly changed
- should reference the failed run id

### Replay

- intentionally reruns a prior template version or prior logical run context
- should record whether it uses the historical runtime snapshot or current environment snapshot

Recommended product rule:

- make snapshot choice explicit for replay

## Cancellation Requirements

Cancellation should be cooperative where possible.

Requirements:

- signal stop to long-running source reads, transformer execution, and sink writes
- record whether cancellation happened before or after any sink side effect
- preserve partial run report even for cancelled executions

## Run Reporting Requirements

Run reporting is the main product feedback surface after execution.

Minimum run report fields:

- run id
- status
- start and end times
- duration
- template identity and version
- trigger type
- failure summary if failed

### Source report section

- rows read
- schema used
- connection or provider reference used
- source warnings and diagnostics

### Transformer report section

- transformer name and type
- input and output row counts where meaningful
- schema handoff summary
- warning and error diagnostics

### Sink report section

- sink name or index
- writer type and target
- attempted rows
- succeeded rows
- failed rows
- failure policy in effect

### AI-specific report additions

- provider and model used
- parser result status
- timeout or retry diagnostics

### Plugin-specific report additions

- plugin id
- extension class
- version

## Diagnostics Requirements

Diagnostics should be actionable and layer-aware.

Minimum categories:

- validation errors
- validation warnings
- runtime warnings
- runtime failures
- compatibility warnings

Every major diagnostic should include:

- template identity
- stage kind such as source, transformer, or sink
- stage name or index
- model type
- factory or plugin identity where relevant

## Refresh And Snapshot Requirements

The control plane should make refresh boundaries visible.

Requirements:

- each run records the runtime registry snapshot used
- datasource refresh affects only later runs
- plugin refresh affects only later runs
- reports should state whether a run used a historical or current snapshot in replay scenarios

## V1 Migration Support Requirements

The control plane should help V1 retirement, not only V2 runtime use.

Requirements:

- validation warnings for approximate V1 mappings
- preview support for side-by-side migration review
- explain output that highlights source policy approximations and transformer choices
- links from migration analysis to runtime validation results where possible

## Recommended Delivery Plan

### Phase C0. Standardize control-plane vocabulary

- define validate, explain, preview, dry-run, retry, replay, and cancel semantics
- define run states and run metadata

### Phase C1. Deliver authoring preflight

- implement validate
- implement explain
- implement bounded preview

### Phase C2. Deliver runtime reporting

- standardize run reports
- add source / transformer / sink metrics
- add plugin and datasource snapshot reporting

### Phase C3. Deliver retry, replay, and cancellation semantics

- implement retry and replay metadata model
- implement cancellation behavior and reporting

## Acceptance Criteria

The control-plane design is successful when:

- template authors can validate and preview before running
- operators can explain what a run will do before it starts
- each run produces a useful source / transformer / sink report
- retry, replay, and cancellation are explicit product behaviors instead of ad hoc operations
- plugin and datasource refresh boundaries are visible in run metadata

## Non-Goals

The near-term control-plane design should avoid:

- exposing only raw engine internals instead of product-oriented explain and report models
- unbounded preview that behaves like a full run accidentally
- hiding plugin, datasource, or snapshot differences from run reports
- claiming exact V1 parity where the migration is only approximate
