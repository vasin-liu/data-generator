---
phase: 08-rw-streaming-upsert
plan: 02
subsystem: api
tags: [json, streaming, chunked, ndjson, calcite]

requires: [08-01]
provides:
  - ChunkedJsonRowSource implementing ChunkedRowSource
  - Policy-aware JsonSourceFactory.create(name, source, policy)
  - DefaultJsonParser streaming NDJSON line and JSON array element helpers
affects: [08-03, 08-04, 08-08]

tech-stack:
  added: []
  patterns:
    - "JsonSourceFactory mirrors CsvSourceFactory usesChunkedRead gate"
    - "Chunked JSON default chunk size 1000 when policy sourceChunkSize unset (D-03)"
    - "Format detection: explicit format field or first non-whitespace byte"

key-files:
  created:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/ChunkedJsonRowSource.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/ChunkedJsonRowSourceTests.java
    - data-generator-calcite/src/test/resources/fixtures/orders.ndjson
  modified:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/JsonSourceFactory.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/parser/DefaultJsonParser.java
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/JsonSourceVO.java

key-decisions:
  - "JSON chunked default chunk size 1000 when EffectiveExecutionPolicy still has global 5000 JDBC default"
  - "Chunked path rejects root selector; IN_MEMORY JsonRowSource root behavior unchanged (D-21)"
  - "Array streaming uses Jackson TokenStreamFactory with FAIL_ON_TRAILING_TOKENS disabled for element reads"

patterns-established:
  - "Chunked JSON sources: rows() empty, hasNextChunk/nextChunk/rowsReadSoFar API"
  - "NDJSON malformed line errors include 1-based line number"

requirements-completed: [RW-01]

duration: 45min
completed: 2026-06-29
---

# Phase 08 Plan 02 Summary

**Incremental JSON reads via ChunkedJsonRowSource with NDJSON/array streaming and policy-aware JsonSourceFactory dispatch**

## Performance

- **Duration:** 45 min
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added `ChunkedJsonRowSource` for NDJSON line-by-line and top-level JSON array streaming reads
- Extended `JsonSourceFactory` with `create(..., EffectiveExecutionPolicy)` CHUNKED/STREAMING dispatch
- Extended `DefaultJsonParser` with `parseNdjsonLine` and `openArrayElementIterator` streaming helpers
- Added optional `JsonSourceVO.format` (`ndjson` | `array`) with auto-detect fallback (D-08)
- Added `ChunkedJsonRowSourceTests` covering 10k NDJSON/array rows, fixture parse, malformed line errors, maxRows cap, and factory dispatch

## Task Commits

1. **Chunked JSON source + streaming parser + tests** - `fe33bb0` (feat)

## Files Created/Modified

- `ChunkedJsonRowSource.java` - Chunked JSON reader implementing ChunkedRowSource
- `JsonSourceFactory.java` - Policy-aware factory overload
- `DefaultJsonParser.java` - NDJSON line parse and streaming array element iterator
- `JsonSourceVO.java` - Optional `format` field
- `ChunkedJsonRowSourceTests.java` - Unit tests
- `fixtures/orders.ndjson` - Sample NDJSON fixture

## Decisions Made

- Map unset policy `sourceChunkSize` (5000 global default) to JSON default 1000 per D-03
- Reject `root` selector on chunked path; operators use flattened files or IN_MEMORY for nested JSON
- Use Jackson 3 `TokenStreamFactory` streaming with per-element `ObjectReader` for compact arrays

## Deviations from Plan

- `TemplateV2RuntimeRegistry` policy passthrough deferred to plan 08-03 (as documented in 08-03-PLAN.md)
- Format YAML/JSON round-trip verified via getter/setter on `JsonSourceVO`; polymorphic codec round-trip left to existing template binding tests

## Issues Encountered

- Jackson 3 `JsonParser` import conflict with project `JsonParser` interface — resolved with fully qualified Jackson types
- Compact JSON arrays required `FAIL_ON_TRAILING_TOKENS` disabled on array element `ObjectReader`

## Self-Check: PASSED

- ChunkedJsonRowSourceTests: 8 tests, 0 failures
- data-generator-calcite full suite: 275 tests, 0 failures
- TemplateV2RunnerTests `readsJsonSource*` unchanged (IN_MEMORY path)

## Next Phase Readiness

- Plan 08-03 can wire `TemplateV2RuntimeRegistry` policy to `JsonSourceFactory` and relax pipeline source-type gates

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
