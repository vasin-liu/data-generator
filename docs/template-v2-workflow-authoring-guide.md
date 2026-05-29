# Template V2 Workflow Authoring Guide

## Purpose

This guide explains how to author **greenfield** Template V2 definitions that use the **L2 workflow** layer (pause, log, branch, shared scope, compute block invocation) and **L1 transform DAG / JavaScript** inside compute blocks.

Companion docs:

- `docs/template-v2-scenario-template-catalog.md` — scenario library index
- `docs/template-v2-transformer-strategy.md` — SQL, SpEL, JS transform roles
- `docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md` — capability layers (L1/L2/L3)

Automated evidence: `data-generator-service/src/test/java/org/gensokyo/data/template/V2WorkflowScenarioIT.java`

## Capability layers (quick reference)

| Layer | Scope | YAML location |
|-------|--------|---------------|
| **L2 Workflow** | Ordered steps in one template | Root `workflow.steps` |
| **L1 Transform DAG** | Multi-node transforms inside one block | `computeBlocks[].transformGraph` |
| **L1 Linear transforms** | Chained SQL / SpEL / JS inside one block | `computeBlocks[].transformers` |
| **Legacy linear** | Single pipeline (no workflow) | Root `sources`, `transformers`, `sinks` |

When `workflow` is present, **do not** define top-level `sources`, `transformers`, or `sinks`. Put compute inside `computeBlocks` and invoke blocks from workflow steps.

## Minimal workflow template

```yaml
name: my-workflow-export
workflow:
  steps:
    - type: log
      id: log-start
      level: INFO
      message: export-start
    - type: invoke_compute_block
      id: invoke-main
      computeBlockId: main-block
    - type: log
      id: log-end
      level: INFO
      message: export-complete
computeBlocks:
  - id: main-block
    sources:
      seed:
        type: iterator
        iterator:
          type: number
          from: 1
          to: 10
          step: 1
    transformers:
      - type: sql
        sql: SELECT value FROM seed
    sinks:
      - writers:
          - type: console
```

## Workflow step types

| YAML `type` | Purpose | Key fields |
|-------------|---------|------------|
| `log` | Structured diagnostic + run-report warning | `level`, `message`, optional `fields` |
| `pause` | Wait on worker thread | `durationMs`, or `until` (ISO-8601), or `condition` (SpEL) |
| `branch` | Conditional routing | `condition` (SpEL), `thenSteps` / `elseSteps`, optional `thenComputeBlockId` |
| `shared_scope` | Per-run named map | `scopeId`, `action` (`open` / `write` / `read` / `close`), optional `entries` |
| `invoke_compute_block` | Run one compute block | `computeBlockId` |

### Pause + log scenario

See `template/v2-scenarios/scenario-wf-pause-log.yaml` (`GF-WF-A`):

- `LOG` → `pause` (50 ms) → `invoke_compute_block` → `LOG`
- Log messages appear in `RunMetrics.warnings` as `[LOG][INFO][step-id] message`

### Branch scenario

See `template/v2-scenarios/scenario-wf-branch.yaml` (`GF-WF-B`):

- SpEL condition `"true"` selects `thenSteps`
- Use `#shared['scope-id']['key']` after `shared_scope` write steps for cross-step state

## Compute blocks

Each block is a self-contained **sources → transforms → sinks** unit.

| Field | Required | Notes |
|-------|----------|-------|
| `id` | Yes | Referenced by `invoke_compute_block.computeBlockId` |
| `sources` | Yes | Same source types as linear V2 (`iterator`, `query`, `csv`, …) |
| `transformers` | One of | Linear chain; downstream steps read alias `input` |
| `transformGraph` | One of | L1 DAG (see below); **mutually exclusive** with `transformers` |
| `sinks` | No | Omit when workflow only needs in-memory block output |
| `sharedScopeId` | No | Opens named scope for block-local SpEL |

## L1 transform DAG

Define transforms once, wire nodes with edges. Downstream SQL nodes use table alias **`input`** for upstream output.

See `template/v2-scenarios/scenario-dag-join.yaml` (`GF-DAG`):

```yaml
transformGraph:
  transforms:
    filter-high:
      type: sql
      name: filter-high
      sql: SELECT value FROM seed WHERE value >= 4
    shift-values:
      type: sql
      name: shift-values
      sql: SELECT value, value + 10 AS shifted FROM input
  nodes:
    - id: n1
      transformId: filter-high
      outputAlias: filtered
    - id: n2
      transformId: shift-values
      outputAlias: output
  edges:
    - fromNodeId: n1
      fromPort: out
      toNodeId: n2
      toPort: in
```

Validation rejects cyclic graphs at author time (`TemplateV2Validator` + topological sort).

## JavaScript transform

Use GraalJS row-local scripts inside a compute block. Scripts receive a `row` map binding only (sandboxed; no arbitrary IO).

See `template/v2-scenarios/scenario-js-transform.yaml` (`GF-JS`):

```yaml
transformers:
  - type: sql
    sql: SELECT value AS amount FROM seed
  - type: js
    script: row.amount = row.amount * 2
```

Limits (enforced by validator and runtime):

- Max script size: 64 KiB UTF-8
- Default timeout: 5 s per row (`timeoutMs` optional)

## Validation checklist

Before publish/run:

1. `name` and `workflow.steps` are non-empty.
2. Every `invoke_compute_block.computeBlockId` matches a `computeBlocks[].id`.
3. Each compute block has sources and either `transformers` or `transformGraph.nodes`.
4. Transform DAG is acyclic; JS scripts are non-blank and within size limits.
5. Linear legacy fields are absent when using workflow mode.

Run `TemplateV2Validator.validate(template)` from the control plane or tests.

## Greenfield scenario library (workflow)

| Greenfield Id | Template Path | Primary shape | Evidence |
|---------------|---------------|---------------|----------|
| `GF-WF-A` | `template/v2-scenarios/scenario-wf-pause-log.yaml` | LOG + PAUSE + invoke | `V2WorkflowScenarioIT` |
| `GF-WF-B` | `template/v2-scenarios/scenario-wf-branch.yaml` | branch + invoke | `V2WorkflowScenarioIT` |
| `GF-DAG` | `template/v2-scenarios/scenario-dag-join.yaml` | transform DAG in block | `V2WorkflowScenarioIT` |
| `GF-JS` | `template/v2-scenarios/scenario-js-transform.yaml` | SQL + JS in block | `V2WorkflowScenarioIT` |

## Related linear scenarios

Pre-workflow greenfield scenarios (iterator/SQL/JDBC only) remain under the same directory and are covered by `V2ScenarioTemplateIT`:

- `scenario-a-synthetic.yaml` through `scenario-e-streaming-jdbc.yaml`

## Policy note

New templates should use workflow steps for PAUSE, LOG, branching, and multi-block orchestration. Historical V1 stage orchestration remains archive-only; see `docs/migration/orchestration-retirement-boundary.md`.
