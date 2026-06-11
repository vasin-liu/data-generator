# Template V2 Policy To Runtime Mapping Guide

## Purpose

This document maps the current Template V2 product, governance, transformer, datasource, control-plane, and migration policies onto concrete runtime and model surfaces.

It exists to prevent a gap between:

- high-level roadmap and governance decisions

and:

- actual model fields
- runtime contracts
- validation responsibilities
- control-plane APIs

This is the bridge between planning language and implementation language.

Related references:

- `docs/template-v2-product-roadmap.md`
- `docs/template-v2-transformer-strategy.md`
- `docs/template-v2-datasource-and-secret-governance.md`
- `docs/template-v2-control-plane-requirements.md`
- `docs/template-v2-migration-program.md`
- `docs/template-v2-execution-scalability-plan.md`
- `docs/calcite-templatev2-model-design.md`
- `docs/calcite-implementation-status.md`

## Goal

Every major policy decision should have a visible runtime landing zone.

This guide should answer:

- which policy belongs in model fields
- which policy belongs in validation only
- which policy belongs in runtime registry or provider logic
- which policy belongs in control-plane APIs and reporting
- which policy should remain documentation-only until the product genuinely needs it

## Mapping Principles

- keep product policies close to the model or runtime surface that enforces them
- do not encode every policy as a model field if validation or environment policy is the better home
- prefer typed model fields for stable reusable behavior
- prefer runtime metadata or capability declarations for extension-driven behavior
- keep control-plane reports able to show which policies were in effect for a run

## Mapping Layers

Policy-to-runtime mapping should be reasoned about across five layers.

### 1. Template model layer

Examples:

- `TemplateV2VO`
- `SourceVO`
- `TransformVO`
- sink or writer model
- `SourcePolicyVO`
- sink execution policy

Use this layer when:

- the policy needs to be authored by template authors
- the value should be serialized and versioned with the template

### 2. Validation layer

Examples:

- `TemplateV2Validator`
- transform validation
- datasource governance checks

Use this layer when:

- the policy should block invalid templates early
- the policy depends on environment or governance rules

### 3. Runtime registry and extension layer

Examples:

- source / transform / sink factories
- plugin-contributed capabilities
- runtime services

Use this layer when:

- the behavior depends on available capability providers
- the policy is extension-driven

### 4. Execution layer

Examples:

- runner behavior
- in-flight snapshot rules
- batching and execution mode
- sink retry or failure semantics

Use this layer when:

- the policy affects how a run behaves after validation has passed

### 5. Control-plane and reporting layer

Examples:

- validate / explain / preview / dry-run outputs
- run reports
- refresh and plugin lifecycle reporting

Use this layer when:

- the policy must be visible to operators or authors

## Policy Mapping Matrix

| Policy Topic | Primary Runtime Home | Secondary Home | Notes |
|---|---|---|---|
| source selection behavior | `SourcePolicyVO` | validation, explain/report | author-owned policy with runtime effect |
| sink failure behavior | sink execution policy model | runner and reporting | should be visible in run reports |
| SQL vs non-SQL vs custom transformer choice | docs + validation guidance | transformer subtype model | mostly design policy first, then type system |
| custom transformer output schema rule | transform subtype model or factory metadata | validation | must be enforceable before downstream validation |
| datasource managed vs inline policy | source model + governance validation | explain/report | author-visible and environment-governed |
| secret reference requirement | connector model fields | governance validation and redaction | should not stay as plain string convention only |
| plugin capability collisions | runtime registry | diagnostics/reporting | extension-driven |
| in-flight refresh snapshot policy | execution layer | run metadata/reporting | runtime behavior, but must be observable |
| execution scale mode | execution policy model | explain/report | should become author-visible if user-tunable |
| V1 approximation warnings | migration and validation layer | explain/report | usually not stored in model long-term |

## Source Policies

### Source selection and materialization

Policy:

- selection semantics belong to the source layer, not SQL

Recommended runtime mapping:

- model field: `SourcePolicyVO`
- validator responsibility: reject unsupported combinations
- execution responsibility: source post-processing or source-specific materialization logic
- control-plane visibility: explain and run report should show the effective source policy

### Managed versus inline endpoint policy

Policy:

- templates may reference managed connections or inline endpoints depending on environment rules

Recommended runtime mapping:

- model fields:
  - managed reference such as `dataSourceId`, `connectionRef`, or provider ref
  - inline endpoint block where supported
- validator responsibility:
  - reject illegal combinations
  - enforce environment policy
- execution responsibility:
  - resolve the effective endpoint snapshot
- control-plane visibility:
  - explain and run report should show which reference or inline snapshot was used

### Secret handling policy

Policy:

- production-sensitive values should resolve from secret references, not plain text

Recommended runtime mapping:

- model fields:
  - family-specific `...SecretRef` style fields until a richer shared secret model is introduced
- validator responsibility:
  - reject plain text where policy forbids it
- execution responsibility:
  - resolve secret values and redact them from logs
- control-plane visibility:
  - show secret reference usage, never resolved secret values

## Transformer Policies

### Transformer family choice

Policy:

- SQL is preferred, but not the only future transformer family

Recommended runtime mapping:

- model layer:
  - `TransformVO` subtype hierarchy
- validation layer:
  - guidance and warnings for inappropriate transformer choice can be added later
- runtime layer:
  - `V2TransformFactory` resolves by subtype
- control plane:
  - explain and reports should show transformer type and extension source

### Output schema requirement

Policy:

- every transformer must expose a stable output schema before downstream validation

Recommended runtime mapping:

- model layer:
  - explicit output schema where needed
- runtime extension layer:
  - factory-provided schema inference where supported
- validation layer:
  - fail early if downstream schema cannot be determined

### Custom transformer governance

Policy:

- custom transformers should be typed and plugin-friendly, not one opaque generic config blob

Recommended runtime mapping:

- model layer:
  - plugin-registered `TransformVO` subtype
- runtime layer:
  - plugin-provided `V2TransformFactory`
- validation layer:
  - subtype and factory resolution
- control plane:
  - plugin id and transform subtype visible in diagnostics

## Sink Policies

### Failure handling

Policy:

- multi-sink failure mode should be explicit and configurable

Recommended runtime mapping:

- model layer:
  - sink execution policy model or sink group policy
- execution layer:
  - runner behavior
- control plane:
  - run report must state effective failure mode

### Batching and retry

Policy:

- sink scale controls should become tunable where scenarios require them

Recommended runtime mapping:

- model layer:
  - sink options or future execution policy block
- execution layer:
  - sink adapter or runner controls
- control plane:
  - report effective batch size and retry outcomes

## Plugin Policies

### Capability registration

Policy:

- built-in and external capabilities share one host-side registry model

Recommended runtime mapping:

- runtime layer:
  - runtime plugin provider and registry provider
- validation layer:
  - subtype and capability resolution checks
- control plane:
  - plugin state and capability inventory in reports or admin views

### Refresh and rollback safety

Policy:

- refresh affects only later runs, and failed refreshes preserve last known good state

Recommended runtime mapping:

- execution layer:
  - snapshot-at-run-start behavior
- runtime layer:
  - last-known-good registry preservation
- control plane:
  - refresh result reporting and run snapshot metadata

## Control-Plane Policies

### Validate / explain / preview / dry-run semantics

Policy:

- these should be product-level capabilities, not incidental engine side effects

Recommended runtime mapping:

- control-plane API layer:
  - dedicated operations or service methods
- validation layer:
  - reusable validation core
- execution layer:
  - bounded preview and dry-run behavior

### Run lifecycle reporting

Policy:

- operators need run metadata, state, and diagnostics beyond success/failure

Recommended runtime mapping:

- execution layer:
  - run state transitions and report generation
- control plane:
  - run report DTO or reporting model
- runtime metadata:
  - plugin snapshot, datasource snapshot, and template version

## Migration Policies

### Approximate versus exact migration visibility

Policy:

- approximation boundaries must not be hidden

Recommended runtime mapping:

- validation layer:
  - warnings for known approximation areas
- control plane:
  - explain and migration report outputs
- documentation layer:
  - migration program and scenario catalog

### Scenario acceptance tracking

Policy:

- migration should be tracked by scenario family, not just by low-level feature delivery

Recommended runtime mapping:

- not primarily a runtime model concern
- best kept in docs, scorecards, and migration tooling
- may later map to scenario identifiers in migration assistant outputs

## Scalability Policies

### Execution mode choice

Policy:

- simple in-memory execution remains valid, but larger scenarios need chunked or more bounded behavior

Recommended runtime mapping:

- future model layer:
  - execution policy block if user-tunable
- execution layer:
  - actual mode selection and limits
- control plane:
  - explain warnings and run report fields

### Memory limits and sink batch limits

Policy:

- protection should be explicit rather than accidental

Recommended runtime mapping:

- future model layer:
  - execution policy fields if template-tunable
- environment config:
  - system defaults
- execution layer:
  - enforcement
- control plane:
  - limit-exceeded diagnostics

## Recommended Mapping Backlog

The following policy areas should be pushed into runtime-visible surfaces first.

### P0

- transformer output schema rules
- datasource managed versus inline rules
- secret reference and redaction rules
- sink failure policy visibility
- plugin snapshot and refresh visibility
- validate / explain / preview / dry-run semantics

### P1

- execution mode and batching controls
- richer custom transformer capability metadata
- scenario identifiers in migration outputs

### P2

- more formal shared endpoint model across connector families
- broader admin-facing reporting or policy dashboards

## Acceptance Criteria

This guide is useful when:

- major product policies can be traced to a runtime home
- implementers know whether a policy belongs in model, validation, runtime, or control-plane code
- future features can be reviewed for policy-to-runtime alignment instead of only code completeness
- roadmap documents no longer drift too far from concrete implementation surfaces

## Non-Goals

This guide should avoid:

- freezing every future model field too early
- turning every policy statement into a serialized template option
- using runtime code where documentation-only policy is still enough for the current phase
