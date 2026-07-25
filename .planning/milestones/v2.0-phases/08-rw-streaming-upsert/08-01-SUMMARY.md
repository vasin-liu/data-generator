---
phase: 08-rw-streaming-upsert
plan: 01
subsystem: api
tags: [csv, streaming, chunked, calcite, utf-8-bom]

requires: []
provides:
  - ChunkedCsvRowSource implementing ChunkedRowSource
  - Policy-aware CsvSourceFactory.create(name, source, policy)
  - DefaultCsvParser UTF-8 BOM strip and parseLine hook
affects: [08-02, 08-03, 08-04, 08-08]

tech-stack:
  added: []
  patterns:
    - "CsvSourceFactory mirrors QuerySourceFactory usesChunkedRead gate"
    - "Chunked CSV default chunk size 1000 when policy sourceChunkSize unset (D-03)"

key-files:
  created:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/ChunkedCsvRowSource.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/ChunkedCsvRowSourceTests.java
  modified:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/CsvSourceFactory.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/parser/DefaultCsvParser.java

key-decisions:
  - "CSV chunked default chunk size 1000 when EffectiveExecutionPolicy still has global 5000 JDBC default"
  - "Chunked path enforces UTF-8 only; IN_MEMORY CsvRowSource charset behavior unchanged"

patterns-established:
  - "Chunked file sources: rows() empty, hasNextChunk/nextChunk/rowsReadSoFar API"

requirements-completed: [RW-01]

duration: 25min
completed: 2026-06-29
---

# Phase 08 Plan 01 Summary

**Incremental CSV reads via ChunkedCsvRowSource with UTF-8 BOM handling and policy-aware CsvSourceFactory dispatch**

## Performance

- **Duration:** 25 min
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added `ChunkedCsvRowSource` for line-by-line CSV reads without full materialization
- Extended `CsvSourceFactory` with `create(..., EffectiveExecutionPolicy)` CHUNKED/STREAMING dispatch
- Extended `DefaultCsvParser` with BOM strip and `parseLine` for streaming use
- Added `ChunkedCsvRowSourceTests` covering 5000-row chunked read, BOM fixture, and factory dispatch

## Task Commits

1. **Chunked source + BOM parser + tests** - `52bab8c` (feat)

## Files Created/Modified

- `ChunkedCsvRowSource.java` - Chunked CSV reader implementing ChunkedRowSource
- `CsvSourceFactory.java` - Policy-aware factory overload
- `DefaultCsvParser.java` - BOM strip and parseLine API
- `ChunkedCsvRowSourceTests.java` - Unit tests

## Decisions Made

- Map unset policy `sourceChunkSize` (5000 global default) to CSV default 1000 per D-03
- Reject non-UTF-8 charset on chunked path only

## Deviations from Plan

None - plan executed as written.

## Issues Encountered

None

## Self-Check: PASSED

- ChunkedCsvRowSourceTests pass
- TemplateV2RunnerTests readsCsvSource* pass

## Next Phase Readiness

- Plan 08-02 can mirror ChunkedCsvRowSource/ChunkedJsonRowSource pattern for JSON

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
