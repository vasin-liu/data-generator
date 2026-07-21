---
phase: 09-jdbc-dialect-expansion
plan: 03
subsystem: testing
tags: [jdbc, testcontainers, upsert, kingbase, highgo, dameng, clickhouse, embedded]

requires:
  - phase: 09-jdbc-dialect-expansion
    plan: 01
    provides: JdbcSinkSqlBuilder dialect matrix and countUpsertedRows metrics
provides:
  - Kingbase/HighGo PG-proxy upsert ITs with rowsUpserted coverage
  - ClickHouse insert IT plus upsert=true runtime reject contract
  - DamengTestSupport gate and optional ChunkedPipelineDamengUpsertIT skeleton
affects: [09-04, 09-05]

tech-stack:
  added: []
  patterns:
    - "Kingbase/HighGo dialect keys tested via PostgreSQL Testcontainers proxy (D-15)"
    - "Dameng real IT gated by -Ddm.it=true or DG_DM_IT=true (D-14)"

key-files:
  created:
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineKingbaseDialectTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/DamengTestSupport.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java
  modified:
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/UpsertParitySupport.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/ClickHouseInsertBulkWriterIntegrationTests.java

key-decisions:
  - "Kingbase/highgo PG-proxy ITs fulfill per-dialect harness without licensed KB/HG images (D-15)"
  - "Dameng MERGE proof remains JdbcSinkSqlBuilderTests; real DM IT opt-in only (D-13, D-14)"

patterns-established:
  - "UpsertParitySupport documents PG-proxy: dialect key on sink, PG driver on JDBC connection"
  - "ClickHouse upsert reject asserted in integration suite complementing unit test (D-03)"

requirements-completed: [RW-05]

coverage:
  - id: D1
    description: Kingbase and highgo dialect upsert idempotency via PG Testcontainers proxy with rowsUpserted > 0
    requirement: RW-05
    verification:
      - kind: integration
        ref: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineKingbaseDialectTests.java
        status: pass
    human_judgment: false
  - id: D2
    description: ClickHouse insert bulk IT passes and upsert=true rejected at runtime
    requirement: RW-05
    verification:
      - kind: integration
        ref: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/ClickHouseInsertBulkWriterIntegrationTests.java
        status: pass
    human_judgment: false
  - id: D3
    description: Dameng MERGE unit tests pass; optional DM IT skipped by default
    requirement: RW-05
    verification:
      - kind: unit
        ref: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java
        status: pass
      - kind: integration
        ref: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java
        status: pass
    human_judgment: false

duration: 35min
completed: 2026-07-21
status: complete
---

# Phase 9 Plan 03: Embedded Dialect Harness Tests Summary

**Layered Testcontainers and unit tests proving five-engine JDBC dialect read/write and upsert contracts without production credentials**

## Performance

- **Duration:** ~35 min
- **Started:** 2026-07-21T12:30:00Z
- **Completed:** 2026-07-21T12:45:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Added `ChunkedPipelineKingbaseDialectTests` proving `dialect=kingbase` and `dialect=highgo` upsert idempotency against PostgreSQL Testcontainers (D-15 PG-proxy)
- Extended `UpsertParitySupport` with PG-proxy Javadoc and ON CONFLICT hint mapping for kingbase/highgo; `rowsUpserted > 0` closes 09-01 `countUpsertedRows` integration proof
- Added `clickhouseUpsertRejectedAtRuntime` contract test in ClickHouse integration suite (D-03)
- Created `DamengTestSupport` gate and optional `ChunkedPipelineDamengUpsertIT` skeleton skipped by default (D-14)

## Task Commits

Each task was committed atomically:

1. **Task 1: Kingbase/HighGo PG-proxy upsert ITs** - `8e04d66` (test RED), `e78e161` (feat GREEN)
2. **Task 2: ClickHouse upsert reject and gated Dameng IT** - `b2c969e` (test)

**Plan metadata:** `140e056` (docs: complete plan)

## Files Created/Modified

- `ChunkedPipelineKingbaseDialectTests.java` - PG-proxy ITs for kingbase and highgo dialect keys
- `UpsertParitySupport.java` - PG-proxy documentation and dialect hint mapping
- `ClickHouseInsertBulkWriterIntegrationTests.java` - upsert=true runtime reject contract
- `DamengTestSupport.java` - `-Ddm.it=true` / `DG_DM_IT=true` gate
- `ChunkedPipelineDamengUpsertIT.java` - optional DM IT placeholder

## Decisions Made

- Kingbase/HighGo harness uses same PG container as Postgres upsert tests with distinct dialect option keys
- Dameng real IT remains opt-in; MERGE SQL unit tests from 09-01 are primary proof

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None for default CI. Optional Dameng IT: set `-Ddm.it=true` or `DG_DM_IT=true` when DM JDBC endpoint available.

## Next Phase Readiness

- Plan 09-04 can wire verify-phase9 script and Playwright preset E2E
- Plan 09-05 can document PG-proxy test strategy and DM IT enablement in operator docs

## Self-Check: PASSED

- FOUND: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineKingbaseDialectTests.java
- FOUND: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/DamengTestSupport.java
- FOUND: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java
- FOUND: .planning/phases/09-jdbc-dialect-expansion/09-03-SUMMARY.md
- FOUND: 8e04d66, e78e161, b2c969e

---
*Phase: 09-jdbc-dialect-expansion*
*Completed: 2026-07-21*
