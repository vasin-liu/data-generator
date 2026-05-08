# Template V2 Execution Scalability Plan

## Purpose

This document defines how Template V2 should evolve from the current lightweight in-memory execution path toward a more scalable runtime.

It answers six questions:

- what scalability boundaries exist in the current runtime
- when full in-memory materialization is still acceptable
- when chunked or streaming execution is required
- how sources, transformers, and sinks should expose scalable execution behavior
- what memory and batching controls the product should add
- how scalability planning should stay compatible with the current product roadmap

Related references:

- `docs/template-v2-product-roadmap.md`
- `docs/template-v2-control-plane-requirements.md`
- `docs/template-v2-datasource-and-secret-governance.md`
- `docs/template-v2-transformer-strategy.md`
- `docs/calcite-implementation-status.md`
- `docs/calcite-refactor-plan.md`

## Goal

Template V2 should support:

- small and medium in-memory runs with low complexity
- larger source and sink workloads without unsafe unbounded materialization
- explicit execution-mode choices where scenario scale demands them

The goal is not to jump immediately to a full distributed engine. The goal is to grow the current V2 runtime into a bounded and observable product-grade execution model.

## Current Baseline

The current V2 path is intentionally lightweight.

Current characteristics:

- SQL validation and planning through Calcite
- repository-local row execution
- primarily in-memory materialization
- sequential multi-sink fan-out
- limited explicit scale controls at the template level

This is acceptable for:

- small synthetic generation
- bounded lookup and enrichment
- first-pass V1 replacement scenarios

It becomes risky for:

- large query result sets
- large file conversions
- high-row-count sink fan-out
- heavy join or aggregate shapes with poor cardinality awareness

## Scalability Principles

- keep the simple in-memory path for scenarios where it is sufficient
- add bounded execution controls before adding complex engine behaviors
- expose scale-related behavior explicitly in model or runtime policy where needed
- prefer chunking and streaming where full materialization is unsafe
- keep preview and explain able to warn about scale risks
- do not hide expensive execution shapes behind the same behavior as small runs

## Execution Modes

Template V2 should gradually support more than one execution mode.

### Mode 1. In-memory materialized

Best for:

- small and medium datasets
- synthetic generation
- simple lookup-style templates
- debugging and preview

Strengths:

- simple semantics
- easiest explain and preview
- good fit for early V2

Risks:

- memory pressure at large sizes
- poor fit for very large source or sink workloads

### Mode 2. Chunked execution

Best for:

- larger query results
- large file conversion
- sink batching

Strengths:

- bounded memory growth
- simpler than full streaming semantics

Risks:

- some transformer semantics may still require materialization
- chunk boundary behavior must be explicit

### Mode 3. Streaming-oriented execution

Best for:

- line-oriented or event-oriented flows
- very large or continuous source streams
- low-latency sink delivery scenarios

Strengths:

- strongest memory behavior
- natural future path for some source and sink families

Risks:

- not every SQL or transformer shape is stream-friendly
- much more careful semantic planning required

Recommended product direction:

- keep in-memory as the baseline
- add chunked execution first
- add streaming-oriented execution only for scenario families that truly need it

## Source Scalability Requirements

### Query sources

Required future capabilities:

- paged read mode
- configurable fetch size
- bounded materialization policy
- explicit warning when a source may return unbounded rows

### File sources

Required future capabilities:

- line or row chunking for CSV and JSON where feasible
- bounded row-window preview
- progress visibility for larger files

### AI sources

Required future capabilities:

- concurrency and rate controls
- bounded request fan-out
- explicit cost and timeout controls

### Plugin-defined sources

Required future capabilities:

- source capability metadata should indicate whether the source supports chunked or stream-friendly behavior

## Transformer Scalability Requirements

Scalability depends heavily on transformer shape.

### SQL transformer

The product should distinguish:

- row-local projections and filters
- join-heavy or aggregate-heavy shapes
- distinct and ordering shapes that may require stronger materialization

Planning requirements:

- explain should warn when transformer shape is likely to require full materialization
- future execution policy should mark which SQL patterns are safe for chunked operation and which are not

### Non-SQL and custom transformers

Requirements:

- transformer family should declare whether it is row-local, chunk-local, or materialization-required
- custom transformers should not silently assume full unbounded materialization without declaring it

Suggested classification:

- `ROW_LOCAL`
- `CHUNK_LOCAL`
- `MATERIALIZATION_REQUIRED`

## Sink Scalability Requirements

### JDBC sink

Required future capabilities:

- configurable batch size
- transaction boundary policy
- retry and idempotency policy where supported

### Kafka sink

Required future capabilities:

- producer buffering policy
- flush policy
- bounded failure reporting

### Elasticsearch sink

Required future capabilities:

- bulk batch sizing
- retry and backoff policy
- bounded bulk failure collection

### File sinks

Required future capabilities:

- chunked write behavior
- append policy visibility
- progress reporting for large outputs

## Memory Protection Requirements

The product should add explicit protection instead of relying only on environment limits.

Recommended controls:

- maximum preview rows
- maximum in-memory row count for a run or stage
- maximum estimated source read size where measurable
- maximum sink batch size
- explicit fail-fast mode when limits are exceeded

Recommended product behavior:

- reject obviously unsafe templates at validation or preflight where possible
- fail with clear scale diagnostics instead of generic out-of-memory crashes

## Explain And Preview Integration

Scalability planning should be visible before run time.

Explain should eventually surface:

- likely materialization boundaries
- risky sort, distinct, join, or aggregate shapes
- source families that do not support bounded reads

Preview should:

- remain bounded even when full execution could be huge
- clearly state when preview does not reflect full-scale sink or aggregation cost

## Runtime Reporting Requirements

Run reports should capture scale-relevant facts.

Recommended fields:

- rows read per source
- rows emitted per transformer
- batch counts per sink
- peak or bounded memory policy used
- execution mode used
- warnings when fallback from preferred scalable mode occurred

## Recommended Execution Policy Model

The product should eventually expose a lightweight execution policy model.

Illustrative direction:

```yaml
executionPolicy:
  mode: IN_MEMORY
  maxRowsInMemory: 100000
  sourceChunkSize: 5000
  sinkBatchSize: 1000
  failOnLimitExceeded: true
```

The exact shape can evolve, but the product should plan for explicit execution controls.

## Scenario Guidance

### Synthetic generation

- in-memory is acceptable for many cases
- related-record generation may still need bounded controls at larger scales

### Query enrichment

- chunked query reads should be prioritized
- join and aggregate explain warnings are important

### File conversion

- chunked source read and sink write should be prioritized early

### AI generation

- scalability is more about concurrency, rate, and cost than raw row streaming

### Extension-heavy scenarios

- custom transformers and sources should declare execution behavior explicitly

## Recommended Delivery Plan

### Phase S0. Make scale visible

- add explain and report vocabulary for execution mode and scale warnings
- document the current in-memory boundary clearly

### Phase S1. Add bounded execution controls

- add preview limits
- add in-memory row limits
- add sink batch controls

### Phase S2. Add chunked source and sink support

- start with query and file sources
- add JDBC, Kafka, and Elasticsearch batch controls

### Phase S3. Add transformer capability metadata

- classify transformers by execution shape
- use metadata in explain and runtime planning

### Phase S4. Re-evaluate streaming-oriented execution

- only expand here when scenario evidence justifies the extra complexity

## Acceptance Criteria

The scalability plan is useful when:

- operators can tell whether a scenario should stay in-memory or move to chunked execution
- the product has explicit memory and batch controls
- larger query or file workloads no longer depend on unbounded materialization alone
- explain and run reports expose scale-relevant behavior clearly
- custom extensions can declare execution behavior instead of silently assuming one mode

## Non-Goals

The near-term scalability plan should avoid:

- pretending the current runtime is already a streaming engine
- introducing distributed-engine complexity without scenario evidence
- hiding materialization-heavy execution behind the same reporting as bounded execution
- forcing every scenario away from the simple in-memory path before it is necessary
