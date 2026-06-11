# JDBC Chunked Execution Design

## Metadata

| Field | Value |
|-------|-------|
| Status | Approved (brainstorming) |
| Date | 2026-05-19 |
| Author | Gensokyo |
| Primary scenario | **A** — large JDBC query result export (DB → DB / Kafka / Elasticsearch) |
| Success criterion | **D** — bounded memory, chunked I/O, observable scale behavior |
| SQL workload mix | **A3** — single-table `SELECT` and large-table **JOIN** small dimension both common |

## Related documents

- `docs/template-v2-execution-scalability-plan.md`
- `docs/template-v2-execution-policy-model-proposal.md`
- `docs/template-v2-product-roadmap.md`
- `docs/calcite-implementation-status.md`
- `docs/calcite-v1-parity-scorecard.md`

## Problem statement

Template V2 today materializes entire JDBC query results in memory before transform and sink execution. `QueryRowSource` loads all rows via `ResultSetExtractor`; `TemplateV2Runner` ignores `ExecutionPolicyVO`; sinks receive full `List<Row>` instances. For large fact-table exports this causes memory pressure and OOM failures that are hard to diagnose.

Product roadmap P0 scalability items (chunked reads, memory limits, sink batching, scale reporting) are documented but not wired into the runtime.

## Goals

1. Support **large JDBC read → row-local transform → batched sink write** without unbounded heap growth.
2. Wire **`executionPolicy`** from template model into `TemplateV2Runner` with fail-fast diagnostics.
3. Support **A3 workload mix**:
   - **Pattern S** — single-source / row-local SQL (projection, filter, expressions).
   - **Pattern B** — **broadcast join**: chunked large (fact) query source + small dimension source materialized once.
4. Reject or constrain unsafe combinations (unbounded join, aggregate-heavy shapes under `CHUNKED`).
5. Expose **run metrics** (mode, rows read, chunk count, batch count, limit events).

## Non-goals (this program)

- Distributed or Flink-style streaming engine.
- Full explain / preview UI (only scale preflight and run metrics in this program).
- Official non-SQL transformer family (deferred).
- Governance plane (template approval, secrets catalog UI).
- Exact V1 `SourcePolicyVO` consumptive selection parity.
- Chunked execution for arbitrary multi-way joins, `GROUP BY`, `DISTINCT`, or `ORDER BY` without limits.

## Current baseline

| Component | Behavior |
|-----------|----------|
| `QueryRowSource` | Full `ResultSet` → `List<Row>` in constructor |
| `QuerySourceVO` | `pageIndex` / `pageSize` / `maxRows` affect SQL only; all rows still loaded |
| `fetchSize` | Not used anywhere in repository |
| `TemplateV2Runner` | In-memory only; uses `sinkExecutionPolicy` only |
| `ExecutionPolicyVO` | Model + `TemplateV2Validator`; not consumed at runtime |
| `JdbcRowSinkAdapter` | `batchUpdate` on entire row list at once |
| `CalciteRowTransformer` | `materialize()` for joins loads both sides fully |

## Design principles

- **Bounded before distributed** — chunking and policy limits first.
- **Explicit modes** — `IN_MEMORY`, `CHUNKED`; reserve `STREAMING` until implemented.
- **Honest semantics** — do not run `CHUNKED` on shapes that require full materialization without a documented strategy (broadcast join).
- **Fail with diagnostics** — `ScaleLimitExceededException` (or equivalent) includes policy field, stage, source name, row counts.
- **Defaults safe for small runs** — `IN_MEMORY` remains default when policy omitted.

## Execution modes

### `IN_MEMORY` (default)

Existing behavior. Suitable for small synthetic runs, preview-sized samples, and templates that use join/aggregate shapes not yet supported under `CHUNKED`.

Optional `maxRowsInMemory` still enforced when set (fail-fast before OOM).

### `CHUNKED`

Pipeline: read chunk → transform chunk → write sink batch(es) → repeat.

Requirements:

- All sources in template either implement chunked read **or** are classified **broadcast-small** (see below).
- Transform chain classified as **`ROW_LOCAL`** or **`BROADCAST_JOIN`** (see execution shape).
- Sinks accept incremental `writeBatch` calls.

### `STREAMING`

Reserved. Validator may accept the value for forward compatibility but runtime must reject with clear message until implemented.

## Execution policy (runtime)

Use existing `ExecutionPolicyVO` fields:

| Field | Runtime use |
|-------|----------------|
| `mode` | Select pipeline implementation |
| `sourceChunkSize` | JDBC `fetchSize` and max rows per `nextChunk()` |
| `sinkBatchSize` | JDBC `batchUpdate` size; Kafka flush cadence; ES bulk size |
| `maxRowsInMemory` | **Cumulative** rows read across all chunks in a run; also caps broadcast dimension size where applicable |
| `failOnLimitExceeded` | If true, abort run; if false, not supported in v1 (always fail) |
| `previewRowLimit` | Preview / analyze APIs only |

### Recommended defaults (large JDBC export templates)

```yaml
executionPolicy:
  mode: CHUNKED
  sourceChunkSize: 5000
  sinkBatchSize: 1000
  maxRowsInMemory: 500000
  failOnLimitExceeded: true
```

### Effective policy resolution

Introduce `EffectiveExecutionPolicy` (resolver):

1. Start from repository defaults per mode.
2. Overlay `template.executionPolicy` non-null fields.
3. Overlay system properties only if team later adds service-level caps (out of scope for v1).

Runner receives `EffectiveExecutionPolicy` at `run()` start and passes it through pipeline.

## Source layer

### `RowSource` (unchanged contract)

Keep `List<Row> rows()` for backward compatibility and small sources.

### `ChunkedRowSource` (new)

```java
public interface ChunkedRowSource extends RowSource {
    boolean supportsChunking();
    boolean hasNextChunk();
    List<Row> nextChunk(int maxRows);
    long rowsReadSoFar();
}
```

`QueryRowSource` remains for `IN_MEMORY` and small reads.

### `ChunkedQueryRowSource` (new)

Responsibilities:

- Execute `QuerySourceVO` SQL with `NamedParameterJdbcTemplate` + `RowCallbackHandler`.
- Set `fetchSize` to `effectivePolicy.sourceChunkSize()` on the underlying `PreparedStatement` (via `JdbcTemplate` fetch size or statement callback).
- Accumulate rows until chunk size or EOF; return chunk.
- Respect `min(querySource.maxRows, policy.maxRowsInMemory - rowsReadSoFar)`.
- Infer schema from first row or `inferSchemaWithoutRows` (reuse existing logic).
- Normalize column keys to lowercase (same as today).

**Datasource notes (documentation + tests):**

- PostgreSQL: fetch size typically effective with autocommit off or holdable cursor — document recommended pool settings.
- MySQL: may require `useCursorFetch=true` and `defaultFetchSize` on datasource URL for true server-side cursor; integration test must assert behavior or document fallback.

### Broadcast-small sources (Pattern B)

For join templates where one side is a **small dimension**:

**Classification (preflight):**

- Dimension source: `QuerySourceVO` with explicit `maxRows` ≤ `broadcastMaxRows` **or** estimated row count from preflight `COUNT(*)` wrapper ≤ threshold.
- Fact source: implements `ChunkedRowSource`.
- SQL join shape: single `INNER` or `LEFT` join, equi-join only in v1.

**Runtime:**

1. Materialize dimension source once into `Map<JoinKey, List<Row>>` or sorted list (size bounded by `broadcastMaxRows`, default 50_000 rows or 10% of `maxRowsInMemory`, whichever is smaller).
2. Stream fact chunks via `ChunkedQueryRowSource`.
3. For each fact row, probe dimension map; emit joined rows (left join: null-pad unmatched).
4. Apply row-local projection/filter on joined chunk if SQL plan decomposes to post-join select list.

**Threshold configuration:**

- `executionPolicy.broadcastMaxRows` (new optional field) or derive from `maxRowsInMemory / 10`.
- If dimension exceeds threshold → preflight **rejects `CHUNKED`**, suggests `IN_MEMORY` with lower limit or SQL rewrite.

### Iterator / CSV / JSON sources in join templates

- Iterator and constant seeds: always **broadcast-small** if row count ≤ threshold.
- CSV/JSON: not chunked in phase 1; if used as dimension, must pass `maxRows` / size preflight.

## Execution shape classification

Introduce `ExecutionShapeClassifier` analyzing validated Calcite plan / SQL AST:

| Shape | `CHUNKED` support | Description |
|-------|-------------------|-------------|
| `ROW_LOCAL` | Yes | Single query source or iterator; projection, filter, expressions; no join |
| `BROADCAST_JOIN` | Yes | One chunked fact + one broadcast-small side; equi-join |
| `MATERIALIZATION_REQUIRED` | No (use `IN_MEMORY` or reject) | Multi-join, `GROUP BY`, `DISTINCT`, `ORDER BY` without limit, correlated subqueries |

**A3 routing:**

| User pattern | Classifier result | Pipeline |
|--------------|-------------------|----------|
| Single-table export | `ROW_LOCAL` | ChunkedQuery → transform chunk → sink batch |
| Fact + small dim join | `BROADCAST_JOIN` | Materialize dim → chunked fact → hash join per chunk → transform → sink |
| Fact + fact join, or aggregate | `MATERIALIZATION_REQUIRED` | Reject `CHUNKED` at validation; suggest `IN_MEMORY` + `maxRowsInMemory` or SQL rewrite |

Validator change: if `mode == CHUNKED` and shape is `MATERIALIZATION_REQUIRED`, fail validation with actionable message listing detected SQL features.

## Transform layer

### Phase 1 (ROW_LOCAL)

- Reuse `CalciteRowTransformer` logic on **each chunk** as isolated input table `input`.
- No cross-chunk state.
- UDFs and built-in SQL functions unchanged.

### Phase 2 (BROADCAST_JOIN)

- Either extend `CalciteRowTransformer` with `transformChunk(factChunk, dimensionSnapshot)` or dedicated `BroadcastJoinTransformer`.
- Join keys extracted from validated `ON` clause.
- LEFT JOIN null-padding behavior matches current in-memory semantics.

### Explicitly unsupported under CHUNKED

- Multi-way joins (more than two sources in join tree).
- `GROUP BY`, `DISTINCT`, window functions.
- Transform chain with multiple transformers where second stage requires full prior output (defer multi-transform chunked until shape analysis exists).

## Sink layer

### `RowSink` extension

```java
public interface RowSink {
    void write(RowSchema schema, List<Row> rows);
    default void writeBatch(RowSchema schema, List<Row> rows, int batchSize) {
        // default: slice rows by batchSize and call batchUpdate per slice
    }
}
```

### JDBC

- `JdbcRowSinkAdapter.writeBatch`: partition `rows` into slices of `sinkBatchSize`; each slice one `batchUpdate`.
- Optional: one transaction per run vs per N batches (default: autocommit per batch for v1; document).

### Kafka

- `writeBatch`: send records; call `kafkaTemplate.flush()` after each batch (configurable later).

### Elasticsearch

- `writeBatch`: bulk API with `sinkBatchSize` documents per bulk request.

### Multi-sink fan-out

- Same transformed chunk passed sequentially to each sink (preserve current semantics).
- Metrics: per-sink batch counts.

## Runner changes

### `TemplateV2Runner.run(TemplateV2VO template)`

```
policy = EffectiveExecutionPolicy.resolve(template)
shape = ExecutionShapeClassifier.classify(template)
registry = runtimeRegistryProvider.current()

switch (policy.mode()) {
  case IN_MEMORY -> InMemoryPipeline.run(template, policy, registry)
  case CHUNKED -> ChunkedPipeline.run(template, policy, shape, registry)
  default -> throw unsupported mode
}
```

### `ChunkedPipeline` (new)

1. Build sources; attach `ChunkedQueryRowSource` where applicable.
2. If `BROADCAST_JOIN`, materialize dimension snapshot with limit checks.
3. Loop until no chunks:
   - Read chunk
   - Update `RunMetrics.rowsRead`
   - Check `maxRowsInMemory`
   - Transform chunk
   - For each sink: `writeBatch(..., sinkBatchSize)`
4. Return `TemplateV2RunResult` with metrics; **do not** retain full row list in heap (optional small sample for debug only).

### `TemplateV2RunResult` extension

Add fields:

- `executionMode`
- `rowsReadPerSource` (map)
- `chunksProcessed`
- `sinkBatchesWritten` (map by sink index)
- `warnings` (list of strings, e.g. fallback, approximation)
- `rows` — empty or capped sample when `CHUNKED`; full list when `IN_MEMORY`

## Preflight and validation

### `TemplateV2Validator`

- Existing policy field validation retained.
- Add: `CHUNKED` + `MATERIALIZATION_REQUIRED` → error.
- Add: `CHUNKED` + `BROADCAST_JOIN` without resolvable broadcast side → error.
- Add: dimension `maxRows` / `broadcastMaxRows` enforcement.

### Scale preflight API (minimal, phase 1)

Extend analyze/preview path (or new endpoint) to return:

- classified `executionShape`
- recommended `mode`
- estimated risk flags (unbounded SQL, missing `maxRows` on large query without policy)

No full explain plan required in v1.

## Error handling

### `ScaleLimitExceededException`

Fields: `policyField`, `limit`, `actual`, `stage` (`SOURCE_READ`, `BROADCAST_MATERIALIZE`, `TRANSFORM`, `SINK_WRITE`), `sourceName`.

HTTP/task layer maps to 4xx with message suitable for operators.

### Unsupported combination

Example: `CHUNKED` + two large query sources → validation error before run.

## Delivery phases

### Phase 1 — Bounded in-memory safety (≈1 iteration)

- [ ] `EffectiveExecutionPolicy` resolver
- [ ] `TemplateV2Runner` reads `executionPolicy`
- [ ] `maxRowsInMemory` enforced in existing in-memory path (count rows as read)
- [ ] `ScaleLimitExceededException` + tests
- [ ] `TemplateV2RunResult` metrics skeleton

**Exit:** Large query fails with clear limit before OOM when policy set, even before chunking.

### Phase 2a — Row-local chunked pipeline (quarter core)

- [ ] `ChunkedRowSource` + `ChunkedQueryRowSource` with `fetchSize`
- [ ] `ChunkedPipeline` for `ROW_LOCAL`
- [ ] `ExecutionShapeClassifier` for single-source SQL
- [ ] `JdbcRowSinkAdapter.writeBatch` (and Kafka/ES batching)
- [ ] Integration test: H2 large table → JDBC sink, heap bounded

**Exit:** Pattern S (A1) production-ready under `CHUNKED`.

### Phase 2b — Broadcast join (A3 second half)

- [ ] Preflight dimension size / `broadcastMaxRows`
- [ ] Dimension snapshot + chunked fact join
- [ ] `BROADCAST_JOIN` classifier rules
- [ ] Tests: small dim table + large fact query, LEFT and INNER

**Exit:** Pattern B common path works under `CHUNKED`.

### Phase 3 — Hardening

- [ ] MySQL cursor fetch integration test or documented limitation
- [ ] Multi-sink metrics in task reports
- [ ] Migration doc + scorecard row for large JDBC export
- [ ] Update `calcite-implementation-status.md`

## Acceptance criteria

| # | Criterion |
|---|-----------|
| 1 | Template with `executionPolicy.mode: CHUNKED` uses chunked pipeline at runtime |
| 2 | Single-table JDBC export of ≥100k rows: process completes without linear heap growth vs row count |
| 3 | Exceeding `maxRowsInMemory` aborts with `ScaleLimitExceededException` naming policy field and stage |
| 4 | JDBC sink issues multiple `batchUpdate` calls sized by `sinkBatchSize` |
| 5 | `CHUNKED` + two large query sources without broadcast classification → validation failure |
| 6 | Fact + small dimension join completes under `CHUNKED` with dimension row count ≤ `broadcastMaxRows` |
| 7 | `CHUNKED` + `GROUP BY` → validation failure with explicit message |
| 8 | Run result includes `executionMode`, `rowsRead`, `chunksProcessed` |

## Testing strategy

- **Unit:** policy resolver, classifier, chunk accumulator, broadcast join probe.
- **Integration (H2):** generate N rows; export with `CHUNKED`; assert row count and sink row count.
- **Integration (optional MySQL):** cursor fetch smoke if CI has instance; otherwise documented manual checklist.
- **Negative:** limit exceeded, unsupported shape, oversized dimension for broadcast.

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| MySQL does not stream without URL flags | Document + test; fail preflight if driver buffers all rows detectable |
| Users expect multi-join CHUNKED | Clear validation errors; roadmap item, not silent wrong results |
| Broadcast threshold wrong | Conservative default; template-level `broadcastMaxRows` override |
| Multi-transform chains | Phase 2a: single `SqlTransformVO` only under CHUNKED; expand later |

## Open decisions (defaults chosen)

| Decision | Choice |
|----------|--------|
| `broadcastMaxRows` field | Add optional on `ExecutionPolicyVO`; default `min(50000, maxRowsInMemory/10)` |
| CHUNKED multi-transform | Disallow in 2a; allow only if all stages row-local on chunk |
| Transaction boundaries JDBC | Autocommit per batch in v1 |
| Return full rows from CHUNKED run | No; metrics + optional 100-row sample |

## Spec self-review (completed)

- [x] No TBD placeholders in acceptance or phases
- [x] Consistent with scalability plan S1–S2 and policy proposal
- [x] A3 addressed via Phase 2a (S) + 2b (B), not pretending all joins chunk
- [x] Scope bounded for single implementation plan
- [x] `STREAMING` explicitly deferred
