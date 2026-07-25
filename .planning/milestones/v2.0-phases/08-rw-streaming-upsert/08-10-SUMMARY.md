---
phase: 08-rw-streaming-upsert
plan: 10
subsystem: test
tags: [csv, json, streaming, chunked, upsert, nyquist, calcite]

requires: [08-04, 08-05]
provides:
  - Nyquist/coverage unit tests for CSV/JSON STREAMING and CHUNKED pipelines (D-04)
  - Composite upsertKeys PG/MySQL SQL builder matrix (D-13)
  - TemplateV2Runner CHUNKED CSV injected-parser path with empty result rows
affects: [08-11, 08-12]

tech-stack:
  added: []
  patterns:
    - "500-row / chunk-100 streaming peakRowsInMemory guard (Nyquist)"
    - "Per-chunk aggregate SQL proves no cross-chunk materialization"
    - "Composite upsertKeys ON CONFLICT / ON DUPLICATE KEY UPDATE tests"

key-files:
  modified:
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/StreamingPipelineTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerTests.java

key-decisions:
  - "Per-chunk SQL proof uses MIN/MAX/COUNT aggregate (window OVER unsupported in V2 SQL skeleton)"
  - "Small fixtures only (≤3k rows) — 10 MB OOM proof deferred to plan 08-09"

patterns-established:
  - "StreamingPipelineTests streamsCsvSourceInBatches + streamsNdjsonSourceInBatches Nyquist pair"

requirements-completed: [RW-01, RW-02, RW-03]

duration: 35min
completed: 2026-06-29
---

# Phase 08 Plan 10 Summary

**Calcite unit test coverage for CSV/JSON streaming pipelines and upsert SQL builder dialect matrix**

## Performance

- **Duration:** 35 min
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added `streamsCsvSourceInBatches` (500 rows / chunk 100, `peakRowsInMemory <= 100`) and `streamsNdjsonSourceInBatches` NDJSON streaming Nyquist tests
- Renamed multi-source rejection to `rejectsMultipleCsvSourcesInStreamingMode` (preserves v1 scope)
- Added `chunkedCsvSourceAppliesSqlPerChunk` using per-chunk aggregate SQL (3 summary rows × 100 rows each)
- Added `chunkedJsonArraySourceWritesAllRows` for JSON array CHUNKED path (500 rows / 5 chunks)
- Extended `JdbcSinkSqlBuilderTests` with composite `[id, tenant_id]` PG/MySQL upsert matrix (9 upsert-related methods)
- Added `readsCsvSourceChunkedModeViaInjectedParser` — CHUNKED mode returns empty `rows()`, metrics show chunks

## Task Commits

1. **Streaming/chunked pipeline + upsert SQL + runner tests** — feat commit

## Files Created/Modified

- `StreamingPipelineTests.java` — CSV/NDJSON streaming Nyquist + multi-source rejection rename
- `ChunkedPipelineTests.java` — per-chunk SQL aggregate proof + JSON array chunked run
- `JdbcSinkSqlBuilderTests.java` — composite upsertKeys PG/MySQL tests
- `TemplateV2RunnerTests.java` — CHUNKED CSV injected-parser integration

## Decisions Made

- Per-chunk independence proven via `COUNT(*)` aggregate per chunk (not `COUNT(*) OVER ()` — unsupported operator)
- Reused small temp-dir fixtures from 08-01/08-02 patterns; no 10 MB fixtures in this plan

## Deviations from Plan

- `chunkedCsvSourceAppliesSqlPerChunk` uses `MIN/MAX/COUNT` aggregate instead of window `COUNT(*) OVER ()` because V2 SQL skeleton rejects `OVER`
- `streamsCsvSourceToConsoleSinkInBatches` retained alongside plan-named `streamsCsvSourceInBatches` (different row/chunk sizes for broader coverage)

## Issues Encountered

- Maven `-am` requires quoted `-Dtest=...` and `-Dsurefire.failIfNoSpecifiedTests=false` on Windows PowerShell

## Self-Check: PASSED

- `StreamingPipelineTests` + `ChunkedPipelineTests`: 14 tests, 0 failures
- `JdbcSinkSqlBuilderTests` + `TemplateV2RunnerTests`: all tests green
- `peakRowsInMemory <= 100` for 500-row CSV streaming at chunk size 100
- `result.getRows().isEmpty()` for CHUNKED CSV injected-parser test
- JdbcSinkSqlBuilderTests has 9 upsert-related methods (≥6 required)
- IN_MEMORY `readsCsvSourceThroughSqlTransform` / `readsJsonSourceThroughSqlTransform` unchanged and passing

## Next Phase Readiness

- Plan 08-11 can wire run-report sink metrics to console using metrics already exercised here
- Plan 08-09 OOM IT can reuse streaming test patterns at 10 MB scale

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
