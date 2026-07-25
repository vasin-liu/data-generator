---
phase: 08-rw-streaming-upsert
plan: 05
subsystem: api
tags: [jdbc, upsert, postgres, mysql, calcite, sink-metrics]

requires: [08-03]
provides:
  - JdbcSinkSqlBuilder upsertKeys PG ON CONFLICT DO UPDATE and MySQL ON DUPLICATE KEY UPDATE
  - WriterOptionResolver upsertKeysOption with legacy conflictColumns fallback
  - JdbcBulkWriteExecutor upsert batch path with rowsUpserted metrics
  - SinkWriteMetric.rowsUpserted and RunMetrics.recordSinkRowsUpserted
affects: [08-09, 08-11]

tech-stack:
  added: []
  patterns:
    - "options.upsertKeys YAML array standard (D-12)"
    - "Fail-fast upsertKeys validation at SQL build and first JDBC batch (D-14)"
    - "MySQL updateCount==2 upsert heuristic; PG counts successful upsert rows (D-15)"

key-files:
  created:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkWriteStats.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcUpsertSmokeTests.java
  modified:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilder.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcRowSinkAdapter.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/WriterOptionResolver.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/SinkWriteMetric.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/RunMetrics.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/SinkWriteExecutor.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java

key-decisions:
  - "Standardize on options.upsertKeys list; legacy conflictColumns comma-string maps to upsertKeys for PG only"
  - "ClickHouse and generic dialects reject upsert=true (Phase 9 deferred)"
  - "H2 smoke uses MODE=MySQL for ON DUPLICATE KEY UPDATE; PG dialect proof deferred to 08-09 Testcontainers"

patterns-established:
  - "JdbcSinkWriteStats per sink job; SinkWriteExecutor records rowsUpserted after JDBC writes"

requirements-completed: [RW-03]

duration: 50min
completed: 2026-06-29
---

# Phase 08 Plan 05 Summary

**Dialect-correct JDBC upsert SQL for PostgreSQL and MySQL with upsertKeys validation and rowsUpserted batch metrics**

## Performance

- **Duration:** 50 min
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments

- `JdbcSinkSqlBuilder` generates PG `ON CONFLICT (keys) DO UPDATE SET …` and MySQL `ON DUPLICATE KEY UPDATE …` from `options.upsertKeys`
- `WriterOptionResolver.upsertKeysOption()` resolves YAML array; legacy `conflictColumns` accepted for PG
- Fail-fast `IllegalArgumentException` when `upsert=true` with empty or unknown `upsertKeys` (D-14)
- `JdbcBulkWriteExecutor` validates keys before execute, counts `rowsUpserted` via driver batch update heuristic
- `SinkWriteMetric.rowsUpserted` + `RunMetrics.recordSinkRowsUpserted` wired through `SinkWriteExecutor`
- `JdbcSinkSqlBuilderTests` (7) and `JdbcUpsertSmokeTests` (5) H2 MySQL-mode smoke + re-run upsert counter proof

## Task Commits

1. **Upsert SQL builder, executor metrics, tests** - `ff03cef` (feat)

## Files Created/Modified

- `JdbcSinkSqlBuilder.java` - upsertKeys PG/MySQL SQL generation and validation
- `JdbcBulkWriteExecutor.java` - upsert batch path, update-count heuristic, postgres_copy guard preserved
- `WriterOptionResolver.java` - `upsertKeysOption()` with legacy `conflictColumns` fallback
- `JdbcSinkWriteStats.java` - per-job upsert counter holder
- `JdbcRowSinkAdapter.java` - passes write stats to bulk executor
- `SinkWriteMetric.java` / `RunMetrics.java` - `rowsUpserted` field and recorder
- `SinkWriteExecutor.java` - attaches stats collector and records upsert metrics
- `JdbcSinkSqlBuilderTests.java` - upsertKeys dialect matrix
- `JdbcUpsertSmokeTests.java` - H2 validation, re-run idempotency, COPY+upsert guard

## Decisions Made

- PG upsert uses `EXCLUDED.col`; MySQL uses `VALUES(col)` per project SQL style
- Non-PG/MySQL dialects throw on `upsert=true` instead of silent plain insert (ClickHouse changed)
- PostgreSQL upsert metric heuristic counts all successful batch rows (insert or update); MySQL uses `updateCount==2`

## Deviations from Plan

- `SinkWriteExecutor` already contained 08-04 `SinkWriteSession` changes in working tree; 08-05 upsert metrics integrated into that version
- Added `JdbcSinkWriteStats` helper (not listed in plan) to thread counters from executor to `SinkWriteExecutor` without changing `RowSink` interface

## Issues Encountered

- Maven `-am` requires `-Dsurefire.failIfNoSpecifiedTests=false` when filtering test classes
- H2 smoke tests required `writer.setDataSourceId` to avoid NPE in `DynamicDataSourceContextHolder.push`

## Self-Check: PASSED

- `JdbcSinkSqlBuilderTests` + `JdbcUpsertSmokeTests`: 12 tests, 0 failures
- `data-generator-calcite` full module suite: 0 failures
- PG upsert SQL contains `ON CONFLICT (id) DO UPDATE SET`
- MySQL upsert SQL contains `ON DUPLICATE KEY UPDATE`
- Empty upsertKeys throws message containing `upsertKeys` and `upsert`
- `bulkMode postgres_copy` + `upsert=true` throws unchanged error message
- Re-run smoke asserts `rowsUpserted > 0` on duplicate-key update

## Next Phase Readiness

- Plan 08-09 can add Testcontainers PG/MySQL idempotent re-run ITs reading `rowsUpserted` from run metrics
- Plan 08-11 can extend `RunReportCollector` and console for `rowsUpserted` display

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
