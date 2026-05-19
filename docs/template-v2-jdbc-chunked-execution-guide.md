# Template V2 JDBC Chunked Execution Guide

## Purpose

This guide explains how to run **large JDBC read → SQL transform → batched sink write** templates under `executionPolicy.mode: CHUNKED` without loading the full result set into heap.

It is the operator-facing companion to:

- `docs/superpowers/specs/2026-05-19-jdbc-chunked-execution-design.md`
- `docs/template-v2-execution-policy-model-proposal.md`
- `docs/template-v2-execution-scalability-plan.md`
- `docs/calcite-implementation-status.md`

## When to use CHUNKED

Use `CHUNKED` when:

- a single `QuerySourceVO` exports a large table (Pattern **S**), or
- a large fact query joins a **small** dimension table (Pattern **B**), and
- transforms are row-local projection/filter/expression SQL (no `GROUP BY`, no unbounded `ORDER BY`, no two large JDBC sources).

Keep `IN_MEMORY` (default) for small runs, previews, and SQL shapes that require full materialization.

`STREAMING` is reserved; validation rejects it until implemented.

## Execution policy

### YAML example (recommended starting point)

```yaml
executionPolicy:
  mode: CHUNKED
  sourceChunkSize: 5000      # JDBC fetchSize and max rows per read chunk
  sinkBatchSize: 1000        # JDBC batchUpdate / Kafka flush / ES bulk size
  maxRowsInMemory: 500000    # cumulative rows read across the run (fail-fast cap)
  failOnLimitExceeded: true
  broadcastMaxRows: 50000    # optional; dimension cap for Pattern B
```

### Field reference

| Field | Role |
|-------|------|
| `mode` | `CHUNKED` selects `ChunkedPipeline`; omitted or `IN_MEMORY` keeps the legacy in-memory path |
| `sourceChunkSize` | Sets `PreparedStatement.setFetchSize` and bounds each `nextChunk()` call |
| `sinkBatchSize` | Partitions sink writes (JDBC `batchUpdate`, Kafka flush cadence, ES bulk) |
| `maxRowsInMemory` | **Cumulative** rows read in the run; exceeding it raises `ScaleLimitExceededException` when `failOnLimitExceeded` is true |
| `failOnLimitExceeded` | Currently always fail on exceed in v1; field exists for forward compatibility |
| `broadcastMaxRows` | Max rows for dimension materialization in Pattern B; default `min(50000, maxRowsInMemory / 10)` when omitted |
| `previewRowLimit` | Preview/analyze APIs only; does not affect chunked runs |

Repository defaults (when fields are omitted) are defined in `EffectiveExecutionPolicy`:

- `sourceChunkSize`: 5000
- `sinkBatchSize`: 1000
- `maxRowsInMemory`: 500000
- `broadcastMaxRows`: `min(50000, maxRowsInMemory / 10)`

### Run metrics

`CHUNKED` runs return `TemplateV2RunResult` with **no full row list** in memory. Use `metrics` instead:

- `executionMode` — `CHUNKED`
- `totalRowsRead` — cumulative source rows
- `chunksProcessed` — source chunks consumed
- per-sink batch counts where applicable

## Pattern S — single query export (ROW_LOCAL)

One JDBC source, row-local SQL (projection, filter, expressions). The pipeline loops: **read chunk → transform chunk → write sink batch(es)**.

```yaml
name: large-orders-export
executionPolicy:
  mode: CHUNKED
  sourceChunkSize: 5000
  sinkBatchSize: 1000
  maxRowsInMemory: 500000
  failOnLimitExceeded: true

sources:
  orders:
    type: query
    dataSourceId: reporting-db
    sql: "select id, status, amount from orders where status = :status"
    params:
      - name: status
        language: plain
        content: OPEN

transformers:
  - type: sql
    sql: "select id, status, amount from orders"

sinks:
  - writers:
      - type: JDBC
        dataSourceId: warehouse-db
        target: orders_staging
```

Requirements:

- Exactly **one** `QuerySourceVO` in `sources` for `ROW_LOCAL` chunked shape.
- SQL transform references that source (simple `FROM orders` / alias).
- `ORDER BY` on the source SQL is fine when the database can stream; unbounded `ORDER BY` in the **transform** SQL without `LIMIT`/`FETCH` is rejected at validation.

Evidence: `ChunkedPipelineTests` (H2, 10k rows, batched JDBC sink).

## Pattern B — broadcast join (fact + small dimension)

Large fact table streamed in chunks; small dimension loaded once into a bounded snapshot, then hash-joined per chunk.

```yaml
name: fact-with-dim-export
executionPolicy:
  mode: CHUNKED
  sourceChunkSize: 2000
  sinkBatchSize: 500
  maxRowsInMemory: 1000000
  broadcastMaxRows: 100000
  failOnLimitExceeded: true

sources:
  fact:
    type: query
    dataSourceId: reporting-db
    sql: "select id, dim_id from fact_t order by id"
  dim:
    type: query
    dataSourceId: reporting-db
    sql: "select id, name from dim_t order by id"
    maxRows: 100000          # must be <= broadcastMaxRows

transformers:
  - type: sql
    sql: "SELECT f.id, d.name FROM fact f LEFT JOIN dim d ON f.dim_id = d.id"

sinks:
  - writers:
      - type: JDBC
        dataSourceId: warehouse-db
        target: fact_enriched
```

Classification rules (`ExecutionShapeClassifier`):

- Exactly **two** sources in the template.
- SQL is a single `INNER` or `LEFT` join between those source names.
- **Dimension** side: `QuerySourceVO` with `maxRows` ≤ `broadcastMaxRows`, or a bounded `ConstantIteratorVO` seed.
- **Fact** side: `QuerySourceVO` **without** a restrictive `maxRows` (large/unbounded read).

At runtime:

1. Materialize dimension once (bounded by `broadcastMaxRows`).
2. Stream fact via `ChunkedQueryRowSource`.
3. Join each fact chunk against the dimension snapshot; apply projection.
4. Write sink batches.

Evidence: `BroadcastJoinExecutorTests.chunkedBroadcastJoinWritesAllFactRows`.

## MySQL: useCursorFetch

`ChunkedQueryRowSource` sets JDBC `fetchSize` from `sourceChunkSize`. On **MySQL Connector/J**, a positive fetch size alone does not always enable server-side cursors—the driver may still buffer the full result set client-side.

For true streaming reads on MySQL, configure the datasource URL (or pool property) with:

```text
jdbc:mysql://host:3306/db?useCursorFetch=true&defaultFetchSize=5000
```

Align `defaultFetchSize` with `executionPolicy.sourceChunkSize`.

Notes:

- **PostgreSQL** — fetch size is usually effective; prefer non-autocommit or holdable cursor settings on the pool when you see full-buffer behavior.
- **H2** — integration tests use in-memory H2; suitable for functional tests, not production MySQL cursor behavior.
- If the driver buffers all rows despite policy, treat it as an infrastructure misconfiguration; fix URL/pool flags before raising chunk sizes.

## What CHUNKED rejects

Validation fails **before run** when `mode: CHUNKED` is incompatible with the template shape or SQL.

### SQL features (MATERIALIZATION_REQUIRED)

Detected in the first `SqlTransformVO` and rejected with a message listing features, for example:

- `GROUP BY`
- `SELECT DISTINCT`
- `ORDER BY` without `LIMIT` / `FETCH`
- `JOIN` that is not a valid **broadcast** join (see below)

Example error:

```text
CHUNKED execution policy is incompatible with SQL requiring materialization: GROUP BY
```

### Two large JDBC queries

`CHUNKED` cannot stream two unbounded `QuerySourceVO` sides:

- two fact-sized queries with a join → validation error (not `BROADCAST_JOIN`)
- multi-way joins (more than one join in the FROM tree) → `MATERIALIZATION_REQUIRED`

Use `IN_MEMORY` with a lower `maxRowsInMemory`, rewrite SQL (pre-join in the database), or export in two steps.

### Oversized broadcast dimension

If the dimension source exceeds `broadcastMaxRows` (no `maxRows` cap, or `maxRows` too high), the template does not classify as `BROADCAST_JOIN` and chunked mode is rejected or falls into materialization-required handling. Set an explicit `maxRows` on the dimension query ≤ `broadcastMaxRows`.

### Other limits

- `STREAMING` mode — not implemented
- Multiple transforms requiring full prior output — not supported under `CHUNKED` in v1
- Window functions, correlated subqueries, and general aggregates — treat as `IN_MEMORY` or rewrite SQL

## Troubleshooting

| Symptom | Likely cause | Action |
|---------|----------------|--------|
| OOM despite `CHUNKED` | MySQL buffering full result | Add `useCursorFetch=true` and `defaultFetchSize` |
| Validation mentions `GROUP BY` | Aggregate SQL | Use `IN_MEMORY` or push aggregation to source SQL |
| Validation mentions `JOIN` | Two large sources or multi-join | Pattern B: cap dim with `maxRows`; or pre-join in DB |
| `ScaleLimitExceededException` on `maxRowsInMemory` | Run exceeded cumulative cap | Raise cap intentionally or add source `maxRows` / filters |
| Empty `result.rows` | Expected for `CHUNKED` | Use `metrics.totalRowsRead` and sink row counts |

## Related tests

- `ChunkedPipelineTests` — Pattern S, metrics, batched sink
- `BroadcastJoinExecutorTests` — Pattern B end-to-end
- `TemplateV2SupportTests` — `CHUNKED` + `GROUP BY` validation
- `EffectiveExecutionPolicyTests` — default resolution
