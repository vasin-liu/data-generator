---
phase: 08-rw-streaming-upsert
plan: 04
subsystem: api
tags: [csv, json, streaming, sink, chunked, pipeline, file-io]

requires: [08-03]
provides:
  - Per-chunk CSV/JSON file sink flush in CHUNKED/STREAMING runs (D-10)
  - SinkWriteSession reuses file sink adapters across chunks with StreamingFileRowSink
  - Pipeline finalize hook closes JSON ARRAY brackets via RowSink.finish() (B-01)
affects: [08-06, 08-08]

tech-stack:
  added: []
  patterns:
    - "StreamingFileRowSink.enableStreaming() for first-chunk truncate + subsequent append"
    - "SinkWriteExecutor.SinkWriteSession caches sinks per sink metric key"
    - "StreamingPipeline/ChunkedPipeline try/finally calls SinkWriteExecutor.closeSinks()"

key-files:
  created:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/StreamingFileRowSink.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/CsvJsonStreamingSinkTests.java
  modified:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/RowSink.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/CsvRowSinkAdapter.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JsonRowSinkAdapter.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/StreamingPipeline.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/ChunkedPipeline.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/StreamingPipelineTests.java

key-decisions:
  - "SinkWriteSession/open/closeSinks scaffold landed in 08-05; 08-04 wires adapters and pipeline finalize"
  - "NDJSON streaming append uses newline prefix between chunks only (no trailing newline per chunk)"
  - "IN_MEMORY pipeline unchanged — one-shot write path via sinkBatchSize=0 and no session"

patterns-established:
  - "File sink lifecycle: enableStreaming on first resolve → per-chunk write → finish() in pipeline finally"

requirements-completed: [RW-02]

duration: 45min
completed: 2026-06-29
---

# Phase 08 Plan 04 Summary

**Per-chunk CSV/JSON file sink flush with pipeline finalize hook for JSON ARRAY mode**

## Performance

- **Duration:** 45 min
- **Tasks:** 3
- **Files modified:** 8 (2 created)

## Accomplishments

- Refactored `CsvRowSinkAdapter` for streaming: first chunk writes header + rows (truncate); later chunks append rows only
- Refactored `JsonRowSinkAdapter` for NDJSON line append and ARRAY bracket lifecycle (`[` on first chunk, `]` in `finish()`)
- Added `StreamingFileRowSink` interface and `RowSink.finish()` default hook
- Wired `StreamingPipeline` and `ChunkedPipeline` to pass `SinkWriteSession` and call `closeSinks()` in `try/finally`
- Added `CsvJsonStreamingSinkTests` (5 tests) and CSV→CSV / CSV→NDJSON streaming pipeline integration tests

## Task Commits

1. **Streaming file sink adapters, pipeline finalize, tests** - feat commit (see git log)

## Files Created/Modified

- `StreamingFileRowSink.java` - marker interface for per-chunk file sink mode
- `RowSink.java` - `finish()` default no-op
- `CsvRowSinkAdapter.java` - streaming append with `initialized` state
- `JsonRowSinkAdapter.java` - NDJSON/ARRAY streaming append + ARRAY `finish()`
- `StreamingPipeline.java` / `ChunkedPipeline.java` - sink session + `closeSinks()` in finally
- `CsvJsonStreamingSinkTests.java` - unit/integration tests for 5k rows, incremental size, ARRAY validity
- `StreamingPipelineTests.java` - CSV→CSV and CSV→NDJSON 3000-row streaming scenarios

## Decisions Made

- Reuse `SinkWriteSession` from prior 08-05 scaffold rather than duplicate executor changes
- NDJSON chunk separator: prefix `\n` only when appending after first chunk (avoids blank lines from double newlines)
- Excel sinks untouched (D-11)

## Deviations from Plan

- `SinkWriteExecutor` session/`closeSinks()` already present from plan 08-05 — 08-04 focused on adapters, pipeline wiring, and tests
- Fixed pre-existing `JdbcUpsertSmokeTests` `RowSchema` construction locally to unblock `mvn test` compile (not committed in 08-04 scope)

## Issues Encountered

- NDJSON append initially wrote trailing newlines per chunk, producing extra blank lines; fixed by appending `\n` + body only between chunks
- Multi-module `-Dtest=...` requires `-Dsurefire.failIfNoSpecifiedTests=false` when using `-am`

## Self-Check: PASSED

- `CsvJsonStreamingSinkTests`: 5 tests, 0 failures
- `StreamingPipelineTests`: 8 tests, 0 failures (includes new CSV→CSV and CSV→NDJSON scenarios)
- Full `data-generator-calcite -am test`: exit 0

## Next Phase Readiness

- Plan 08-06+ can add run-report sink metrics UI and scenario YAML harness for large-file streaming exports

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
