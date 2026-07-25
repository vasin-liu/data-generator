---
phase: 08-rw-streaming-upsert
plan: 06
subsystem: api
tags: [run-report, sink-metrics, console, rw-04]

requires: [08-04, 08-05]
provides:
  - StageMetricVO rowsRead rowsUpserted rowsSkipped optional fields
  - SinkWriteExecutor rowsRead and JDBC rowsSkipped metric wiring
  - RunReportCollector extended sink mapping and actionable sink failures
  - JobDetailPage sink table columns for extended metrics
affects: [08-08, 08-09, 08-11]

tech-stack:
  added: []
  patterns:
    - "SinkWriteMetric rowsRead per chunk batch; rowsSkipped from JdbcSinkWriteStats (D-16, W-03)"
    - "StageMetricVO compact ctor null-to-zero for backward-compatible JSON (D-16)"
    - "RunReportCollector.parseSinkFailure actionable sink key + type + target (D-17)"
    - "Final summary only — no mid-run progress API (D-18)"

key-files:
  created:
    - data-generator-common/data-generator-core/src/test/java/org/gensokyo/data/model/v2/StageMetricVOTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/SinkWriteMetricTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/SinkWriteExecutorMetricsTests.java
  modified:
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/StageMetricVO.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/SinkWriteMetric.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/SinkWriteExecutor.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/RunMetrics.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkWriteStats.java
    - data-generator-service/src/main/java/org/gensokyo/data/task/RunReportCollector.java
    - data-generator-console-web/src/api/types.ts
    - data-generator-console-web/src/app/pages/JobDetailPage.tsx
    - data-generator-service/src/test/java/org/gensokyo/data/task/RunReportCollectorTests.java
    - data-generator-service/src/test/java/org/gensokyo/data/task/RunReportCollectorFailureTests.java

key-decisions:
  - "rowsRead counts rows accepted into each sink write batch, not global source total (D-16)"
  - "rowsSkipped from JDBC null upsert-key filter; distinct from rowsFailed (W-03)"
  - "i18n column labels landed in 08-07; 08-06 wires JobDetailPage columns only"

patterns-established:
  - "Extended sink metrics flow: SinkWriteMetric → RunReportCollector.buildSinkMetrics → console StageMetric"

requirements-completed: [RW-04]

duration: 40min
completed: 2026-06-29
---

# Phase 08 Plan 06 Summary

**Per-sink run report metrics (rowsRead, rowsUpserted, rowsSkipped) and actionable sink errors in JSON and Job center UI**

## Performance

- **Duration:** 40 min
- **Tasks:** 2
- **Files modified:** 14

## Accomplishments

- Extended `StageMetricVO` with optional `rowsRead`, `rowsUpserted`, `rowsSkipped` and null-to-zero compact ctor
- Extended `SinkWriteMetric` and `RunMetrics` recorders; `SinkWriteExecutor` wires rowsRead per batch and merges JDBC skip/upsert stats
- `JdbcBulkWriteExecutor` counts null upsert-key rows as `rowsSkipped` separate from failures
- `RunReportCollector.buildSinkMetrics()` maps all five sink dimensions; `collectFailure()` surfaces actionable sink errors with sink key, type, and target
- Console `StageMetric` type and `JobDetailPage` sink columns for extended metrics
- Unit tests: `StageMetricVOTests`, `SinkWriteMetricTests`, `SinkWriteExecutorMetricsTests`, extended `RunReportCollectorTests` and `RunReportCollectorFailureTests`

## Task Commits

1. **Sink metrics model, executor wiring, collector, console** - feat(08-06)
2. **Plan summary** - docs(08-06)

## Decisions Made

- CONTINUE_ON_ERROR records full batch `rowsOk` when JDBC write succeeds; `rowsSkipped` carries filtered rows
- No WebSocket/SSE progress — report populated only on terminal job state (D-18)
- i18n keys for new columns already present from plan 08-07

## Deviations from Plan

- `RunReportVO` unchanged — additive fields live on `StageMetricVO` only (backward compatible)
- i18n en/zh-CN keys committed in 08-07; 08-06 uses existing keys in JobDetailPage

## Issues Encountered

- Initial `SinkWriteExecutorMetricsTests` used a no-op skip sink without JDBC stats; rewritten to H2 upsert null-key fixture
- Maven `-pl` with comma-separated modules requires quoted arguments on PowerShell

## Self-Check: PASSED

- `StageMetricVOTests`, `SinkWriteMetricTests`, `SinkWriteExecutorMetricsTests`: 0 failures
- `RunReportCollectorTests`, `RunReportCollectorFailureTests`: 0 failures
- `npm run verify:unit` in console-web: exit 0
- Legacy `StageMetricVO` JSON deserializes with zero extended counters
- Sink failure report includes `sink[0].writer[0]` actionable message

## Next Phase Readiness

- Plan 08-09 can assert `rowsUpserted` in Testcontainers upsert scenario ITs
- Plan 08-11 E2E can assert Job center columns for upsert and partial-sink fixtures

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
