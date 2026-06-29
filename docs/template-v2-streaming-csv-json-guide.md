# Template V2 Streaming CSV/JSON Guide

## Purpose

This guide explains how to run **large CSV and JSON** sources and sinks in Template V2 without loading entire files into heap. It complements:

- `docs/template-v2-streaming-execution-guide.md` — JDBC `STREAMING` / `CHUNKED` patterns
- `docs/template-v2-jdbc-chunked-execution-guide.md` — JDBC chunked export
- `docs/template-v2-jdbc-sink-guide.md` — JDBC sink options including upsert

Phase 8 delivers explicit `CHUNKED` and `STREAMING` support for file I/O. **Excel** sources and sinks remain out of scope for streaming (use `IN_MEMORY` for small workbooks).

## Explicit execution mode (required)

Large CSV/JSON pipelines **must** set `executionPolicy.mode` to `CHUNKED` or `STREAMING`. The engine does **not** auto-promote `IN_MEMORY` based on file size (D-01).

| Mode | When to use for CSV/JSON |
|------|--------------------------|
| `IN_MEMORY` | Small fixtures, previews, multi-source SQL, aggregates — unchanged from pre–Phase 8 behavior |
| `CHUNKED` | Large file export/import with row-local SQL; supports the same pipeline shapes as JDBC chunked runs |
| `STREAMING` | Large single-source file pipelines where you want **`peakRowsInMemory`** metrics and strict bounded chunks |

Both `CHUNKED` and `STREAMING` are first-class for CSV/JSON (D-02). Choose based on whether you need peak-memory reporting (`STREAMING`) or chunked export semantics (`CHUNKED`).

**Warn-only boundary:** Saving or publishing a template with `IN_MEMORY` and a large declared or estimated file (≥ 10 MB or high row count) emits a **warning** in the Console — not a hard block. Operators must opt in to `CHUNKED` or `STREAMING` explicitly (D-05, D-22).

## Execution policy defaults

When `sourceChunkSize` is omitted:

- **CSV/JSON file sources** default to **1000 rows** per read chunk (`DEFAULT_FILE_SOURCE_CHUNK_SIZE`, D-03).
- **JDBC query sources** keep the JDBC default of 5000 unless you set `sourceChunkSize` explicitly.

`sinkBatchSize` defaults to **1000** rows per flush (unchanged).

```yaml
executionPolicy:
  mode: CHUNKED          # or STREAMING
  sourceChunkSize: 1000  # optional; 1000 is the CSV/JSON default when unset
  sinkBatchSize: 1000
  maxTotalRows: 500000   # recommended cap for long-running exports
  failOnLimitExceeded: true
```

## CSV sources

### Encoding

Chunked CSV sources support **UTF-8 with optional BOM** only (D-09). Other charsets fail fast at run time.

### Example — chunked CSV → SQL → console

```yaml
name: orders-csv-chunked
executionPolicy:
  mode: CHUNKED
  sourceChunkSize: 1000
  maxTotalRows: 200000
sources:
  incoming:
    type: csv
    path: /data/large-orders.csv
    header: true
transform:
  type: sql
  sql: SELECT order_id, UPPER(customer) AS customer, amount FROM incoming
sink:
  writers:
    - type: console
```

Catalog reference: **GF-F** — `scenario-f-streaming-csv.yaml` (`V2ScenarioTemplateIT`).

## JSON sources

### NDJSON vs JSON array

JSON sources support (D-08):

| Format | Description | YAML |
|--------|-------------|------|
| **NDJSON** | One JSON object per line | `format: ndjson` or auto-detect from `.ndjson` extension |
| **Array** | Top-level `[{...},{...}]` | `format: array` or auto-detect |

When `format` is omitted, the runtime inspects the first non-whitespace byte (`[` → array, `{` → NDJSON).

### Example — streaming NDJSON

```yaml
name: events-ndjson-streaming
executionPolicy:
  mode: STREAMING
  sourceChunkSize: 1000
sources:
  events:
    type: json
    path: /data/events.ndjson
    format: ndjson
transform:
  type: sql
  sql: SELECT event_id, event_type, payload FROM events
sink:
  writers:
    - type: console
```

Catalog reference: **GF-F** — `scenario-f-streaming-ndjson.yaml`.

## CSV/JSON sinks (per-chunk flush)

In `CHUNKED` or `STREAMING` mode, file sinks **flush each chunk to disk** instead of rewriting the whole file per batch (D-10):

- **CSV:** append mode after the first chunk (header written once when `header: true`).
- **JSON NDJSON:** one object per line, appended per chunk.
- **JSON array:** opening `[` on first chunk; subsequent chunks append elements; pipeline finalization closes the array.

`IN_MEMORY` file sinks keep the prior whole-file write behavior (D-21).

## CHUNKED vs STREAMING trade-offs

| Concern | `CHUNKED` | `STREAMING` |
|---------|-----------|-------------|
| Peak memory metric | Chunk-bounded; no `peakRowsInMemory` emphasis | Reports `peakRowsInMemory` per transformed chunk |
| Multi-source SQL | Broader eligibility (same classifier as JDBC chunked) | **Single CSV/JSON source** + row-local SQL only |
| SQL per chunk | Transforms evaluate **within each read chunk** — no cross-chunk joins or aggregates (D-04) | Same per-chunk rule |
| Typical operator choice | Large export jobs, file → JDBC | Capacity planning, strict heap budgeting |

For `GROUP BY`, `DISTINCT`, or multi-source joins, use `IN_MEMORY` (or JDBC `CHUNKED` with broadcast classification where applicable).

## OOM fixture bar (operator expectation)

Documented proof target (D-06):

- **≥ 10 MB** file size and **~100k rows** must complete without OOM when `mode` is `CHUNKED` or `STREAMING`.
- CI proves this with `CsvJsonStreamingOomIT` at **`-Xmx256m`** (see `docs/testing-embedded-components.md` and `scripts/verify-phase8-uat-rw-streaming-upsert.ps1`).

Do not expect `IN_MEMORY` to survive the same fixture on a 256 MB heap.

## Run metrics

`CHUNKED` and `STREAMING` runs return an **empty row list** in `TemplateV2RunResult` (same as JDBC streaming). Inspect `metrics`:

| Field | Meaning |
|-------|---------|
| `executionMode` | `CHUNKED` or `STREAMING` |
| `totalRowsRead` | Cumulative source rows read |
| `rowsWritten` | Cumulative rows written to sinks |
| `peakRowsInMemory` | Largest transformed chunk (`STREAMING`; may be 0 for some `CHUNKED` paths) |
| `chunksProcessed` | Source chunks consumed |

Per-sink **`rowsUpserted`**, **`rowsSkipped`**, and actionable **`errorSample`** values appear in the job run report when JDBC upsert or partial sink failures occur (RW-04). See `docs/template-v2-jdbc-sink-guide.md`.

## Related scenarios and verification

| Catalog | Scenario | Validates |
|---------|----------|-----------|
| GF-F | `scenario-f-streaming-csv.yaml` | CSV `STREAMING` source |
| GF-F | `scenario-f-streaming-ndjson.yaml` | NDJSON `STREAMING` source |
| GF-G | `scenario-g-upsert-pg.yaml` / `scenario-g-upsert-mysql.yaml` | JDBC upsert (see JDBC sink guide) |

**Verification:**

```powershell
.\scripts\verify-phase8-uat-rw-streaming-upsert.ps1 -SkipPlaywright
```

Full UAT adds Podman + Playwright (`npm run e2e:phase8-rw-streaming-upsert`).

## Limitations (Phase 8)

- No Excel streaming (D-11).
- No automatic mode promotion from `IN_MEMORY` (D-01).
- No cross-chunk streaming SQL semantics (D-04).
- JDBC upsert dialects: PostgreSQL and MySQL only — Dameng, Kingbase, HighGo, ClickHouse upsert deferred to Phase 9.
