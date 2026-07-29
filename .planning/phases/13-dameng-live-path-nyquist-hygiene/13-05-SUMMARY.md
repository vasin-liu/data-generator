---
phase: 13-dameng-live-path-nyquist-hygiene
plan: 05
subsystem: database
tags: [dameng, jdbc, upsert, metrics, dm-jdbc, gap-closure]

# Dependency graph
requires:
  - phase: 13-dameng-live-path-nyquist-hygiene (plans 01-04)
    provides: "Dameng live IT wiring and UAT gap diagnosis (.planning/debug/dameng-rows-upserted-metric.md)"
provides:
  - "Dameng-aware upsertCountAsRows branch treating dm-jdbc zero batch updateCount as one successful upsert row"
  - "JdbcUpsertSmokeTests unit coverage for countUpsertedRows(dameng) without live JDBC"
affects:
  - "Phase 13 UAT test 1 (ChunkedPipelineDamengUpsertIT rowsUpserted > 0) — optional live re-verify"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Dameng MERGE metrics: treat non-negative batch updateCount (including 0) as one upsert row due to dm-jdbc 1.8 driver quirk; negative counts still ignored"

key-files:
  created: []
  modified:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcUpsertSmokeTests.java

key-decisions:
  - "Dameng branch executes before postgres-style rule in upsertCountAsRows; isPostgresStyleUpsertDialect still lists dameng but is unreachable for dameng after early return"
  - "UpsertParitySupport data assertions unchanged — fix counter only per debug writeup"

patterns-established:
  - "Dialect-specific JDBC batch updateCount interpretation lives in JdbcBulkWriteExecutor.upsertCountAsRows with inline driver-quirk comments"

requirements-completed: [DIAL-01]

coverage:
  - id: D1
    description: "Dameng upsertCountAsRows treats updateCount==0 and updateCount>0 as one upsert row; SUCCESS_NO_INFO and negative counts behave correctly"
    requirement: DIAL-01
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcUpsertSmokeTests.java#damengUpsertCountTreatsZeroAsSuccessfulUpsert"
        status: pass
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcUpsertSmokeTests.java#damengUpsertCountHandlesSuccessNoInfoAndPositive"
        status: pass
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcUpsertSmokeTests.java#damengUpsertCountIgnoresNegativeBatchCounts"
        status: pass
    human_judgment: false
  - id: D2
    description: "Postgres, Kingbase, HighGo, and MySQL upsert count heuristics unchanged"
    requirement: DIAL-01
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcUpsertSmokeTests.java#postgresStyleUpsertCountStillRequiresPositive"
        status: pass
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcUpsertSmokeTests.java#mysqlUpsertCountHeuristicTreatsDuplicateUpdateAsUpsert"
        status: pass
    human_judgment: false
  - id: D3
    description: "Live Dameng UAT rowsUpserted > 0 on second MERGE run passes against reachable host"
    requirement: DIAL-01
    verification: []
    human_judgment: true
    rationale: "Requires DG_DM_IT=true and reachable dm-jdbc host; CI unit tests prove counter logic but not live driver behavior end-to-end"

duration: 7min
completed: 2026-07-29
status: complete
---

# Phase 13 Plan 05: Dameng rowsUpserted Metric Gap Closure Summary

**Dameng MERGE upsert metrics now count dm-jdbc 1.8 zero batch updateCounts as successful upsert rows, with unit tests locking postgres/mysql/kingbase/highgo regressions.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-07-29T10:17:00+08:00
- **Completed:** 2026-07-29T10:24:00+08:00
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added Dameng-specific branch in `JdbcBulkWriteExecutor.upsertCountAsRows` returning `1` for any non-negative batch `updateCount`, with inline comment documenting the dm-jdbc 1.8 MERGE WHEN MATCHED UPDATE zero-count quirk.
- Updated `countUpsertedRows` Javadoc to document the Dameng exception; removed misleading postgres-style comment that grouped Dameng with PostgreSQL.
- Added four new `@Test` methods in `JdbcUpsertSmokeTests` covering dameng zero counts, SUCCESS_NO_INFO + mixed counts, negative counts, and postgres/kingbase/highgo regression guards.

## Task Commits

Each task was committed atomically:

1. **Task 1: Dameng-aware upsertCountAsRows for zero batch counts** - `ab66629` (fix)
2. **Task 2: Unit tests for countUpsertedRows dameng and dialect regressions** - `f745dcf` (test)

**Plan metadata:** pending (docs commit)

## Files Created/Modified

- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java` - Dameng early-return branch and updated Javadoc
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcUpsertSmokeTests.java` - Four new countUpsertedRows unit tests

## Decisions Made

- Dameng branch placed after negative guard and before mysql/postgres-style rules; `isPostgresStyleUpsertDialect` membership for `"dameng"` left unchanged per plan.
- Counter fix only — `UpsertParitySupport` and live IT wiring untouched.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Maven `-Dsurefire.failIfNoSpecifiedTests=false` required quoting on PowerShell when using `-pl data-generator-calcite -am test -Dtest=JdbcUpsertSmokeTests` so upstream modules without matching tests do not fail the reactor.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- CI-safe verification complete via `JdbcUpsertSmokeTests` (9 tests, 0 failures).
- Optional maintainer live confirmation: `.\scripts\verify-phase13-uat-dameng-live.ps1` with `DG_DM_IT=true` and valid `DG_DM_*` credentials.
- Phase 13 gap-closure complete; orchestrator may re-run UAT and mark phase verification.

## Self-Check: PASSED

- `upsertCountAsRows(0, "dameng")` → 1 via `damengUpsertCountTreatsZeroAsSuccessfulUpsert` — PASS
- `upsertCountAsRows(Statement.SUCCESS_NO_INFO, "dameng")` → 1 via `damengUpsertCountHandlesSuccessNoInfoAndPositive` — PASS
- `upsertCountAsRows(1, "dameng")` → 1 via `damengUpsertCountHandlesSuccessNoInfoAndPositive` — PASS
- `upsertCountAsRows(-3, "dameng")` → 0 via `damengUpsertCountIgnoresNegativeBatchCounts` — PASS
- `upsertCountAsRows(0, "postgres")` → 0 via `postgresStyleUpsertCountStillRequiresPositive` — PASS
- `upsertCountAsRows(2, "mysql")` → 1 via existing `mysqlUpsertCountHeuristicTreatsDuplicateUpdateAsUpsert` — PASS
- `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test "-Dtest=JdbcUpsertSmokeTests" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false"` — BUILD SUCCESS, Tests run: 9, Failures: 0

---
*Phase: 13-dameng-live-path-nyquist-hygiene*
*Completed: 2026-07-29*
