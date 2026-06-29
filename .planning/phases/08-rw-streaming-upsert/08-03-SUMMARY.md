---
phase: 08-rw-streaming-upsert
plan: 03
subsystem: api
tags: [csv, json, streaming, chunked, pipeline, registry, execution-policy]

requires: [08-01, 08-02]
provides:
  - StreamingPipeline and ChunkedPipeline accept single CsvSourceVO/JsonSourceVO ChunkedRowSource
  - TemplateV2RuntimeRegistry policy passthrough for CsvSourceFactory and JsonSourceFactory
  - EffectiveExecutionPolicy.fileSourceChunkSize() D-03 default 1000 for file sources
affects: [08-04, 08-08]

tech-stack:
  added: []
  patterns:
    - "soleChunkedFileOrQuerySource generalizes JDBC-only soleQuerySource gate"
    - "resolveSourceChunkSize uses fileSourceChunkSize for CSV/JSON in pipeline loop"
    - "Registry mirrors QuerySourceFactory policy passthrough for file factories"

key-files:
  created: []
  modified:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/StreamingPipeline.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/ChunkedPipeline.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2RuntimeRegistry.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/EffectiveExecutionPolicy.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/CsvSourceFactory.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/JsonSourceFactory.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/StreamingPipelineTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineTests.java

key-decisions:
  - "JDBC sourceChunkSize default stays 5000; CSV/JSON use fileSourceChunkSize 1000 when unset (D-03)"
  - "Pipeline loop resolves file chunk size separately from JDBC via resolveSourceChunkSize"
  - "Non-ChunkedRowSource for CSV/JSON in STREAMING throws IllegalArgumentException with explicit-mode guidance (D-01)"

patterns-established:
  - "Chunked file or query source eligibility: QuerySourceVO | CsvSourceVO | JsonSourceVO + ChunkedRowSource"

requirements-completed: [RW-01]

duration: 35min
completed: 2026-06-29
---

# Phase 08 Plan 03 Summary

**Wire CSV/JSON ChunkedRowSource into StreamingPipeline and ChunkedPipeline with registry policy passthrough and D-03 file chunk defaults**

## Performance

- **Duration:** 35 min
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Generalized `StreamingPipeline.soleChunkedFileOrQuerySource()` for Query/CSV/JSON single-source templates
- Relaxed `ChunkedPipeline` ROW_LOCAL validation for CSV/JSON `ChunkedRowSource`
- Passed `EffectiveExecutionPolicy` to `CsvSourceFactory` and `JsonSourceFactory` in `TemplateV2RuntimeRegistry`
- Added `EffectiveExecutionPolicy.fileSourceChunkSize()` (1000 when template omits `sourceChunkSize`; JDBC default 5000 unchanged)
- Added pipeline `resolveSourceChunkSize()` so chunk loop honors D-03 for file sources
- Added streaming/chunked CSV integration tests; preserved JDBC streaming regression coverage

## Task Commits

1. **Pipeline eligibility, registry wiring, D-03 chunk default, tests** - `<commit-hash>` (feat)

## Files Created/Modified

- `StreamingPipeline.java` - CSV/JSON source eligibility, actionable non-chunked error, file chunk size resolution
- `ChunkedPipeline.java` - Same source-type relaxation for ROW_LOCAL chunked runs
- `TemplateV2RuntimeRegistry.java` - Policy passthrough to CSV/JSON factories
- `EffectiveExecutionPolicy.java` - `DEFAULT_FILE_SOURCE_CHUNK_SIZE` and `fileSourceChunkSize()`
- `CsvSourceFactory.java` / `JsonSourceFactory.java` - Delegate chunk size to `policy.fileSourceChunkSize()`
- `StreamingPipelineTests.java` - CSV streaming, multi-source rejection, non-chunked error tests
- `ChunkedPipelineTests.java` - CSV chunked batch test

## Decisions Made

- Keep global JDBC `sourceChunkSize` default at 5000; file sources map unset to 1000 via `fileSourceChunkSize()`
- Pipeline chunk loop uses `resolveSourceChunkSize()` so metrics align with factory chunk sizing (D-03)
- No auto-promotion from IN_MEMORY — explicit CHUNKED/STREAMING required (D-01)

## Deviations from Plan

- Added `resolveSourceChunkSize()` in pipelines (not listed in plan) so unset-policy CSV runs use 1000-row chunks in the loop, not JDBC default 5000

## Issues Encountered

None

## Self-Check: PASSED

- `StreamingPipelineTests`: 6 tests, 0 failures
- `ChunkedPipelineTests`: 2 tests, 0 failures
- JDBC streaming test `streamsQuerySourceToJdbcSinkInBatches` unchanged (passes)

## Next Phase Readiness

- Plan 08-04 can add per-chunk CSV/JSON sink flush and streaming file writers

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
