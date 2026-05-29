# Template V2 Streaming Execution Guide

## Purpose

This guide explains when to use `executionPolicy.mode: STREAMING` for **single JDBC query → row-local SQL → JDBC sink** templates, and how it differs from `IN_MEMORY` and `CHUNKED`.

Companion docs:

- `docs/template-v2-jdbc-chunked-execution-guide.md`
- `docs/template-v2-execution-policy-model-proposal.md`

## When to use each mode

| Mode | Best for | Memory profile | Supported shapes (v1) |
|------|----------|----------------|------------------------|
| `IN_MEMORY` | Previews, small datasets, multi-source SQL, aggregates, joins that need full materialization | Holds all source rows + transform output | All shapes the runner supports |
| `CHUNKED` | Large JDBC export (Pattern S) or fact + small dimension broadcast join (Pattern B) | Bounded by `sourceChunkSize` per read; dimension may be materialized for Pattern B | `ROW_LOCAL`, `BROADCAST_JOIN` |
| `STREAMING` | Large JDBC export with **strict peak memory** tracking (Pattern S only) | Bounded by `sourceChunkSize`; reports `peakRowsInMemory` | Single `QuerySourceVO`, row-local SQL, JDBC sink |

### Choose `IN_MEMORY` when

- Running preview/analyze flows or templates under a few thousand rows.
- SQL requires `GROUP BY`, `DISTINCT`, unbounded `ORDER BY`, or multi-source joins without broadcast classification.

### Choose `CHUNKED` when

- Exporting a large table with row-local SQL (same as STREAMING), **or**
- You need Pattern B (large fact + small bounded dimension via `broadcastMaxRows`).

### Choose `STREAMING` when

- You have **one** JDBC query source and row-local SQL (projection/filter/expressions).
- You want explicit **`peakRowsInMemory`** metrics for capacity planning.
- You do **not** need broadcast join or multi-source templates.

## Execution policy example

```yaml
name: orders-streaming-export
executionPolicy:
  mode: STREAMING
  sourceChunkSize: 100      # JDBC fetchSize and max rows per read chunk
  sinkBatchSize: 100        # JDBC batchUpdate size per flush
  maxRowsInMemory: 500000   # cumulative rows read cap (fail-fast)
  failOnLimitExceeded: true

sources:
  t:
    type: query
    sql: select id, name from orders order by id

transformers:
  - type: sql
    sql: select id, name from t where id > 0

sinks:
  - writers:
      - type: jdbc
        target: orders_out
```

**Tip:** Set `sourceChunkSize` near your acceptable peak heap for transformed rows. `peakRowsInMemory` reflects the largest transformed chunk processed.

## Run metrics

`STREAMING` runs return `TemplateV2RunResult` with an **empty row list** (same as `CHUNKED`). Inspect `metrics`:

| Field | Meaning |
|-------|---------|
| `executionMode` | `STREAMING` |
| `totalRowsRead` | Cumulative source rows read |
| `rowsWritten` | Cumulative rows written to sinks |
| `peakRowsInMemory` | Largest transformed chunk held in memory |
| `chunksProcessed` | Source chunks consumed |

## v1 limitations

`StreamingPipeline` rejects at runtime with `IllegalArgumentException` when:

- More than one source is declared.
- SQL classifies as `BROADCAST_JOIN` or non-`ROW_LOCAL`.
- The source is not a chunked JDBC `QuerySourceVO`.

For Pattern B (broadcast join) or aggregate SQL, use `CHUNKED` or `IN_MEMORY` instead.

## Related

- Implementation: `StreamingPipeline`, `TemplateV2Runner`
- Tests: `StreamingPipelineTests`, `TemplateV2RunnerTests`
