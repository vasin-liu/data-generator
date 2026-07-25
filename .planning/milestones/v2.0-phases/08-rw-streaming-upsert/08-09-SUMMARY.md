---
phase: 08-rw-streaming-upsert
plan: 09
subsystem: test
tags: [oom, testcontainers, upsert, streaming, d-24, d-25]

requires: [08-08]
provides:
  - CsvJsonStreamingOomIT 256MB heap proof for CSV CHUNKED/STREAMING and NDJSON STREAMING
  - ChunkedPipelinePostgresUpsertTests Testcontainers ON CONFLICT idempotency
  - ChunkedPipelineMySqlUpsertTests Testcontainers ON DUPLICATE KEY idempotency
  - UpsertParitySupport shared assertUpsertIdempotent helper
  - calcite pom surefire.argLine override for OOM JVM args
affects: [08-11, 08-12]

tech-stack:
  added: []
  patterns:
    - "@Tag(oom) + CLI -Dsurefire.argLine=-Xmx256m for D-24 heap proof"
    - "@EnabledIf DockerTestSupport#dockerAvailable for PG/MySQL upsert ITs (D-25)"
    - "UpsertParitySupport: two-run idempotency + rowsUpserted > 0 on second run (D-15)"

key-files:
  created:
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/CsvJsonStreamingOomIT.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresUpsertTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineMySqlUpsertTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/UpsertParitySupport.java
  modified:
    - data-generator-calcite/pom.xml
    - docs/testing-embedded-components.md

key-decisions:
  - "Generate ≥10 MB fixtures in @BeforeAll with padded payload columns (100k rows)"
  - "Peak memory cap: sourceChunkSize * 2 safety factor"
  - "Shared UpsertParitySupport instead of inline duplication across PG/MySQL tests"
  - "application-phase7-test.yaml unchanged — calcite module tests are Spring-free"

patterns-established:
  - "OOM IT documents PowerShell-quoted -Dsurefire.argLine for Windows Maven"
  - "Dialect SQL hint on upsert failure mentions ON CONFLICT vs ON DUPLICATE KEY"

requirements-completed: [RW-01, RW-02, RW-03]

duration: 55min
completed: 2026-06-29
---

# Phase 08 Plan 09 Summary

**OOM proof IT at -Xmx256m and Testcontainers PostgreSQL/MySQL upsert idempotency**

## Performance

- **Duration:** 55 min
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- `CsvJsonStreamingOomIT`: generates ~100k-row CSV and NDJSON fixtures (≥10 MB each) in `@BeforeAll`
- OOM bar tests: `chunkedCsvCompletesUnder256mHeap`, `streamingCsvCompletesUnder256mHeap`, `streamingNdjsonCompletesUnder256mHeap`
- Asserts SUCCESS, row counts, `peakRowsInMemory <= chunkSize * 2`, expected chunk count
- `data-generator-calcite/pom.xml`: `surefire.argLine` property for CLI `-Xmx256m` override (D-24)
- `UpsertParitySupport.assertUpsertIdempotent`: seeds source, runs CHUNKED upsert twice, asserts stable row count, updated non-key values, `rowsUpserted > 0` on second run
- `ChunkedPipelinePostgresUpsertTests` (postgres:16-alpine) and `ChunkedPipelineMySqlUpsertTests` (mysql:8.0) gated with `@EnabledIf` Docker
- `docs/testing-embedded-components.md`: OOM proof command and upsert IT catalog entries

## Task Commits

1. **OOM IT, upsert Testcontainers ITs, UpsertParitySupport, surefire argLine** — feat(08-09) commit
2. **Plan 09 summary and testing doc** — docs(08-09) commit

## Files Created/Modified

- `CsvJsonStreamingOomIT.java` — D-06/D-24 OOM proof at 256 MB heap
- `UpsertParitySupport.java` — shared PG/MySQL upsert idempotency scenario
- `ChunkedPipelinePostgresUpsertTests.java` — Testcontainers PG upsert IT
- `ChunkedPipelineMySqlUpsertTests.java` — Testcontainers MySQL upsert IT
- `data-generator-calcite/pom.xml` — surefire `argLine` override hook
- `docs/testing-embedded-components.md` — OOM and upsert IT documentation

## Decisions Made

- Fixtures padded with 90–100 char payload fields to exceed 10 MB without inflating row count beyond 100k
- IN_MEMORY OOM on same fixture documented in Javadoc only — not a CI gate (per plan)
- H2 upsert smoke remains in `JdbcUpsertSmokeTests` from 08-05; dialect proof delegated to Testcontainers here

## Deviations from Plan

- `application-phase7-test.yaml` not modified — calcite runtime ITs do not require Spring Boot test profile
- Used `UpsertParitySupport` (not `assertUpsertIdempotent` on `ChunkedJdbcParitySupport`) to keep parity vs upsert concerns separate

## Issues Encountered

- Maven `-am` reactor slice requires `-Dsurefire.failIfNoSpecifiedTests=false` when targeting single IT classes
- OOM IT console sink produces large stdout; use `-q` and expect long runtime (~50s) at 100k rows

## Self-Check: PASSED

- `CsvJsonStreamingOomIT` with `-Dsurefire.argLine=-Xmx256m`: 3 tests, 0 failures (~49s)
- `ChunkedPipelinePostgresUpsertTests,ChunkedPipelineMySqlUpsertTests`: 2 tests, 0 failures with Docker (~3 min)
- CSV fixture ≥10 MB; NDJSON fixture ≥10 MB verified in `@BeforeAll`
- Second upsert run: row count stable; `rowsUpserted > 0`; updated `name` column reflects second-run source
- Tests skip gracefully when Docker unavailable (`@EnabledIf`)

## Next Phase Readiness

- Plan 08-11/08-12 can reference documented OOM Maven command in verify scripts
- Playwright upsert scenarios (D-23) can mirror `UpsertParitySupport` two-run pattern

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
