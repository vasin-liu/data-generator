# Template V2 Migration Program

## Purpose

This document defines how the repository should migrate from V1 templates to Template V2 as a managed program rather than a loose collection of feature rewrites.

It answers six questions:

- how migration work should be grouped and prioritized
- which V1 scenarios should migrate directly, which should adapt, and which should remain compatibility-only for now
- how parity should be measured
- what the migration assistant should and should not do
- how dual-run validation and rollout should work
- what gates should be met before V1 retirement is considered

Related references:

- `docs/template-v2-product-roadmap.md`
- `docs/template-v2-transformer-strategy.md`
- `docs/template-v2-datasource-and-secret-governance.md`
- `docs/template-v2-control-plane-requirements.md`
- `docs/calcite-v1-parity-scorecard.md`
- `docs/calcite-v1-v2-mapping.md`
- `docs/calcite-v1-v2-migration-examples.md`
- `docs/calcite-implementation-status.md`

## Goal

The migration program should move the product from:

- V1 as the legacy but still necessary template path

to:

- V2 as the default and preferred template path
- V1 retained only for explicit compatibility-only scenarios during the migration window

Migration should be:

- scenario-driven
- evidence-based
- explicit about approximation boundaries
- supported by tooling, examples, and validation

## Migration Principles

- do not measure success only by feature count; measure it by scenario coverage
- migrate simple and high-value template families first
- do not promise exact parity where V2 intentionally uses a cleaner model
- classify migrations clearly as exact, adapted, approximate, or compatibility-only
- use SQL first, then UDF, then built-in non-SQL transformer, then custom transformer
- keep V1 available during the migration window for scenarios that still need orchestration-heavy behavior

## Migration Units

The program should operate on four levels.

### 1. Scenario family

Examples:

- synthetic data generation
- multi-source enrichment
- file or message conversion
- AI-assisted structured generation
- orchestration-heavy legacy flows

### 2. Template family

Examples:

- query-source lookup templates
- faker-heavy generation templates
- multi-sink fan-out templates
- script-heavy mapping templates

### 3. Individual template

- one concrete template definition and its business output expectations

### 4. Capability gap

- one missing function, transformer, source policy semantic, or governance feature that blocks migration

## Migration Classification Model

Every migrated template should be classified into one of these outcomes.

### Exact migration

- V2 can reproduce the intended V1 semantics with no material approximation

### Adapted migration

- V2 uses a cleaner but still trustworthy replacement shape
- output and business meaning remain acceptable

### Approximate migration

- V2 can replace most behavior, but at least one known semantic edge differs
- warnings and review are required

### Compatibility-only

- the scenario should remain on V1 until V2 gets a separate orchestration or control feature

## Scenario Waves

The migration program should move in waves.

### Wave 1. Straightforward declarative rewrites

Target scenarios:

- iterator-backed generation
- simple SQL projection and filters
- mapping, condition, and convert rewrites
- first faker/UDF replacements
- console/file/basic sink scenarios

Reason:

- fastest confidence-building path

### Wave 2. Query-backed and multi-source business templates

Target scenarios:

- JDBC query sources
- lookup joins
- grouped or deduplicated outputs
- multi-sink fan-out

Reason:

- these are high-value business templates where V2 is structurally better than V1

### Wave 3. Script-heavy and extension-heavy templates

Target scenarios:

- long-tail SpEL
- project-specific utility chains
- templates that need custom transformer guidance

Reason:

- this is where the transformer strategy and plugin model must prove themselves

### Wave 4. Compatibility-boundary templates

Target scenarios:

- orchestration-heavy V1 flows
- iterator branching
- pause, shared state, or procedural script semantics

Reason:

- these should not be forced into V2 before there is a real orchestration design

## Scenario Inventory Requirements

Before migration execution is declared healthy, the repository should maintain an inventory with at least:

- template id or name
- scenario family
- source families used
- transformer pattern used
- sink families used
- current migration classification
- blocking gaps
- validation evidence or migration example reference

## Parity Gates

V1 retirement should depend on gates, not on optimism.

### Gate P1. Technical parity gate

- the main scenario family is runnable in V2
- required sources, transformers, sinks, and UDFs exist
- validation and diagnostics are acceptable

### Gate P2. Operational parity gate

- preview, explain, run report, and failure diagnostics exist
- datasource and plugin governance fit the scenario
- retry or replay behavior is acceptable where needed

### Gate P3. Business parity gate

- representative outputs are reviewed and accepted
- scenario-specific approximations are documented
- dual-run or comparison evidence exists where the scenario is sensitive

### Gate P4. Retirement gate

- the scenario family has enough V2 coverage and migration guidance
- remaining V1 templates are explicitly compatibility-only or low-value

## Migration Assistant Scope

The migration assistant should help authors, not pretend to replace review.

### In scope

- detect V1 vs V2 template shape
- extract sources and likely source families
- infer candidate SQL for simple field DAGs and lookup patterns
- emit join hints, source rewrite hints, and alias suggestions
- classify likely migration path: SQL, SQL + UDF, non-SQL transformer, custom transformer, or compatibility-only
- emit approximation warnings where exact semantics are not covered

### Out of scope

- guaranteeing final business correctness without review
- silently rewriting orchestration-heavy templates into misleading V2 forms
- inventing unsupported source policy or control-flow semantics

## Recommended Migration Workflow

### Step 1. Analyze

- detect scenario family
- detect blockers
- classify expected migration type

### Step 2. Draft

- generate candidate V2 sources, transformers, and sinks
- generate warnings and review hints

### Step 3. Validate

- run template validation
- run explain
- run bounded preview

### Step 4. Compare

- compare with V1 outputs where needed
- record exact, adapted, or approximate outcome

### Step 5. Promote

- publish V2 template
- keep V1 fallback during migration window if required

## Dual-Run And Comparison Strategy

Not every template needs full dual-run validation, but sensitive scenarios should support it.

Recommended cases:

- financial or heavily validated data outputs
- complex lookup templates
- script-heavy templates converted into new transformer shapes
- templates with approximate source policy semantics

Comparison dimensions:

- row count
- key field equality
- aggregate metrics
- representative sample review
- sink side-effect equivalence where meaningful

## Control-Plane Support Needed By The Migration Program

The migration program depends on control-plane features.

Required:

- validation with warnings
- explain output
- preview
- run report
- versioned replay or re-execution tracking

Helpful future additions:

- automated V1 vs V2 comparison reports
- scenario-family dashboards
- migration scorecards per environment

## Program Deliverables

The migration program should produce and maintain:

- scenario inventory
- parity scorecard
- migration example library
- migration assistant outputs
- dual-run evidence where needed
- explicit list of compatibility-only scenarios

## Recommended Delivery Plan

### Phase M0. Establish inventory and classification

- create or refresh the scenario inventory
- classify current templates into scenario families and migration classes

### Phase M1. Close P0 migration blockers

- align transformer strategy
- align datasource governance
- align control-plane preflight capabilities

### Phase M2. Expand exemplar migrations

- add representative V1 to V2 examples by scenario family
- keep exact versus approximate notes visible

### Phase M3. Deliver migration assistant improvements

- improve analysis and draft generation
- surface transformer and source-policy recommendations

### Phase M4. Dual-run and acceptance evidence

- collect comparison evidence for sensitive scenarios
- promote V2 templates where gates are met

### Phase M5. Retirement readiness review

- identify remaining V1-only scenarios
- decide whether to keep them as compatibility-only or fund further V2 work

## Acceptance Criteria

The migration program is successful when:

- template migration is tracked by scenario family instead of guesswork
- exact, adapted, approximate, and compatibility-only outcomes are explicit
- migration assistant output reduces manual authoring time without hiding risk
- representative scenario waves have documented examples and acceptance evidence
- V1 retirement is discussed with scorecards and gates rather than intuition

## Non-Goals

The migration program should avoid:

- forcing all V1 templates to migrate on one deadline
- claiming exact parity for approximate scenarios
- generating opaque V2 drafts that cannot be explained or reviewed
- treating orchestration-heavy V1 behavior as a simple SQL gap
