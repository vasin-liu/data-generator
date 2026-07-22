---
phase: 10-harness-coverage-ci-gates
plan: 01
subsystem: testing
tags: [test-matrix, harness, p0-gate, calcite, streaming, jdbc-upsert, dialect]

requires:
  - phase: 08-rw-streaming-upsert
    provides: Phase 8 streaming and PG/MySQL upsert Maven IT classes
  - phase: 09-jdbc-dialect-expansion
    provides: Phase 9 dialect SQL builder and Testcontainers evidence classes
provides:
  - Eight new P0 rows in test-matrix.yaml (15 total P0 inventory)
  - Linked Phase 8/9 test classes for harness auto-expansion
affects:
  - 10-02 harness verification
  - 10-03 documentation sync (AGENTS.md, docs/test-harness.md)

tech-stack:
  added: []
  patterns:
    - "P0 row registration with multi-class linked_tests evidence bars"
    - "Dameng covered via MERGE unit only; gated DM IT notes-only"

key-files:
  created: []
  modified:
    - .planning/test-matrix.yaml

key-decisions:
  - "Streaming CSV and JSON are separate P0 rows (D-09)"
  - "Dameng P0 links JdbcSinkSqlBuilderTests only; ChunkedPipelineDamengUpsertIT excluded from linked_tests (D-08)"
  - "Kingbase/HighGo share PG-proxy IT plus dialect-key SQL builder units (D-06)"

patterns-established:
  - "Phase 10 P0 rows use data-generator-calcite owner_module and Phase 8/9 linked_tests only"

requirements-completed: [TEST-07]

coverage:
  - id: D1
    description: P0 streaming CSV matrix row with linked Phase 8 tests
    requirement: TEST-07
    verification:
      - kind: automated_ui
        ref: "rg v2-streaming-csv .planning/test-matrix.yaml"
        status: pass
    human_judgment: false
  - id: D2
    description: P0 streaming JSON matrix row with linked Phase 8 tests
    requirement: TEST-07
    verification:
      - kind: automated_ui
        ref: "rg v2-streaming-json .planning/test-matrix.yaml"
        status: pass
    human_judgment: false
  - id: D3
    description: P0 JDBC upsert row linking PG/MySQL Phase 8 evidence
    requirement: TEST-07
    verification:
      - kind: automated_ui
        ref: "rg v2-jdbc-upsert-pg-mysql .planning/test-matrix.yaml"
        status: pass
    human_judgment: false
  - id: D4
    description: Five P0 dialect rows with evidence-bar linked_tests
    requirement: TEST-07
    verification:
      - kind: automated_ui
        ref: "rg -c 'tier: P0' .planning/test-matrix.yaml => 15"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-07-22
status: complete
---

# Phase 10 Plan 01: Register P0 Matrix Rows Summary

**Eight new P0 harness rows for streaming CSV/JSON, JDBC upsert, and five JDBC dialects with Phase 8/9 linked tests**

## Performance

- **Duration:** 8 min
- **Started:** 2026-07-22T05:33:00Z
- **Completed:** 2026-07-22T05:41:00Z
- **Tasks:** 3
- **Files modified:** 1

## Accomplishments

- Registered `v2-streaming-csv` and `v2-streaming-json` as independent merge-blocking P0 rows
- Registered `v2-jdbc-upsert-pg-mysql` linking three Phase 8 upsert test classes
- Registered five dialect P0 rows (Dameng, Kingbase, HighGo, PostgreSQL, ClickHouse) with evidence-bar linked_tests
- Expanded P0 inventory from 7 to 15 rows; no Phase 6–7 datasource rows added

## Task Commits

Each task was committed atomically:

1. **Task 1: Add P0 streaming CSV and JSON matrix rows** - `a7360b6` (chore)
2. **Task 2: Add P0 JDBC upsert matrix row (PG + MySQL)** - `5991815` (chore)
3. **Task 3: Add five P0 dialect matrix rows with evidence-bar linked_tests** - `30191d0` (chore)

## Files Created/Modified

- `.planning/test-matrix.yaml` — Added 8 P0 rows with Phase 8/9 linked_tests and evidence notes

## Decisions Made

- Followed D-08: `ChunkedPipelineDamengUpsertIT` mentioned in Dameng row notes only, never in linked_tests
- Followed D-12: excluded `CsvJsonStreamingOomIT` and Playwright from all new linked_tests
- Kept harness script and workflow unchanged per D-14 (deferred to plan 02 verification)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02 can run `.\scripts\verify-harness.ps1` to validate `p0.pass` with expanded 15-row set
- Plan 03 should update `docs/test-harness.md` and `AGENTS.md` P0 inventory per D-15

## Self-Check: PASSED

- FOUND: `.planning/test-matrix.yaml`
- FOUND: `.planning/phases/10-harness-coverage-ci-gates/10-01-SUMMARY.md`
- FOUND commit: `a7360b6`
- FOUND commit: `5991815`
- FOUND commit: `30191d0`
- P0 tier count: 15
- ChunkedPipelineDamengUpsertIT absent from linked_tests

---
*Phase: 10-harness-coverage-ci-gates*
*Completed: 2026-07-22*
