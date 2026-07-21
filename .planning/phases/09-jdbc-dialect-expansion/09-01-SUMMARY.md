---
phase: 09-jdbc-dialect-expansion
plan: 01
subsystem: database
tags: [jdbc, upsert, dameng, kingbase, highgo, clickhouse, calcite, validator]

requires:
  - phase: 08-rw-streaming-upsert
    provides: options.upsert + upsertKeys contract, PG/MySQL upsert SQL, publish fail-fast baseline
provides:
  - Phase 9 dialect upsert matrix in JdbcSinkSqlBuilder (MERGE, ON CONFLICT, fail-fast)
  - Publish-time dialect+upsert validation in TemplateV2Validator
  - Upsert metrics for kingbase/highgo/dameng in JdbcBulkWriteExecutor
affects: [09-02, 09-03, 09-05]

tech-stack:
  added: []
  patterns:
    - "Dual fail-fast: JdbcSinkSqlBuilder run-time + TemplateV2Validator publish-time dialect rules"
    - "Kingbase/HighGo map to PostgreSQL ON CONFLICT SQL path"
    - "Dameng MERGE INTO with upsertKeys ON clause"

key-files:
  created: []
  modified:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilder.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java
    - data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java
    - data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2ValidatorTests.java

key-decisions:
  - "Kingbase and highgo delegate unchanged to appendPostgresUpsert (D-01, D-06)"
  - "Dameng upsert uses MERGE INTO with SELECT :col FROM dual USING subquery (D-02)"
  - "generic and clickhouse upsert=true fail at publish and run with dialect in message (D-03, D-08)"

patterns-established:
  - "validateJdbcUpsertDialect mirrors JdbcSinkSqlBuilder unsupportedUpsertDialect messages with writerPath prefix"
  - "isPostgresStyleUpsertDialect groups postgres/kingbase/highgo/dameng for batch update-count metrics"

requirements-completed: [RW-05]

coverage:
  - id: D1
    description: JdbcSinkSqlBuilder generates dialect-correct upsert SQL for kingbase, highgo, dameng and rejects clickhouse/generic
    requirement: RW-05
    verification:
      - kind: unit
        ref: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java
        status: pass
    human_judgment: false
  - id: D2
    description: TemplateV2Validator publish gate blocks clickhouse/generic upsert and allows dameng with valid keys
    requirement: RW-05
    verification:
      - kind: unit
        ref: data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2ValidatorTests.java
        status: pass
    human_judgment: false
  - id: D3
    description: countUpsertedRows kingbase/highgo/dameng integration proof via UpsertParitySupport rowsUpserted
    requirement: RW-05
    verification: []
    human_judgment: true
    rationale: Metric loop closed in plan 09-03 ChunkedPipelineKingbaseDialectTests per plan manual verify note

duration: 45min
completed: 2026-07-21
status: complete
---

# Phase 9 Plan 01: JDBC Dialect Upsert Matrix Summary

**V2 JDBC sink upsert SQL and publish validation for Dameng MERGE, Kingbase/HighGo ON CONFLICT, and clickhouse/generic fail-fast**

## Performance

- **Duration:** ~45 min
- **Started:** 2026-07-21T11:30:00Z
- **Completed:** 2026-07-21T12:15:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Extended `JdbcSinkSqlBuilder` with kingbase/highgo ON CONFLICT delegation, Dameng MERGE INTO, and tightened generic/unknown upsert rejection
- Added publish-time `validateJdbcUpsertDialect` in `TemplateV2Validator` mirroring run-time dialect rules (D-04)
- Extended `JdbcBulkWriteExecutor.countUpsertedRows` with explicit postgres-style dialect grouping for kingbase, highgo, and dameng
- Unit test matrix covers all five Phase 9 dialect upsert outcomes plus validator publish rejections

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend JdbcSinkSqlBuilder for five-engine dialect upsert matrix** - `162c6a5` (test RED), `ef69a34` (feat GREEN)
2. **Task 2: Wire runtime upsert metrics and publish-time dialect validation** - `6ee784b` (feat)

**Plan metadata:** pending (docs commit)

## Files Created/Modified

- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilder.java` - Phase 9 dialect switch, Dameng MERGE, fail-fast messages
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java` - Postgres-style upsert metrics for new dialects
- `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java` - Publish dialect+upsert gate
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java` - Kingbase, HighGo, Dameng, generic tests
- `data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2ValidatorTests.java` - Publish rejection and dameng positive tests

## Decisions Made

- Dameng MERGE uses `USING (SELECT :col AS col FROM dual)` to preserve named-parameter batch binding
- Unsupported upsert message simplified to `JDBC sink upsert=true is not supported for dialect {name}` for dual-layer alignment
- Existing validator tests updated to set explicit `postgres` dialect where upsert validation must reach column cross-check (D-08)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed WriterOptionResolver.stringOption misuse in validator**
- **Found during:** Task 2 (validateJdbcUpsertDialect)
- **Issue:** Third parameter is `Row`, not default string — compilation error
- **Fix:** Use `stringOption(writer, "dialect", null)` and default blank to `generic` like JdbcSinkSqlBuilder.resolveDialect
- **Files modified:** `TemplateV2Validator.java`
- **Committed in:** `6ee784b`

**2. [Rule 1 - Bug] Updated existing validator tests for D-08 generic+upsert fail-fast**
- **Found during:** Task 2 test run
- **Issue:** Tests using upsert without explicit dialect now fail at publish before intended assertion
- **Fix:** Set `dialect: postgres` on tests targeting column cross-check or opaque-transform warnings
- **Files modified:** `TemplateV2ValidatorTests.java`
- **Committed in:** `6ee784b`

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Required for D-08 correctness and compilation; no scope creep.

## Issues Encountered

None beyond auto-fixed compilation and test ordering issues.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 09-02 can proceed with console preset/connectivity work
- Plan 09-03 should run UpsertParitySupport ITs to close countUpsertedRows integration proof for kingbase/highgo
- Operator docs (09-05) should document new dialect keys and MERGE/ON CONFLICT behavior

## Self-Check: PASSED

- FOUND: data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilder.java
- FOUND: data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java
- FOUND: data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java
- FOUND: .planning/phases/09-jdbc-dialect-expansion/09-01-SUMMARY.md
- FOUND: 162c6a5, ef69a34, 6ee784b

---
*Phase: 09-jdbc-dialect-expansion*
*Completed: 2026-07-21*
