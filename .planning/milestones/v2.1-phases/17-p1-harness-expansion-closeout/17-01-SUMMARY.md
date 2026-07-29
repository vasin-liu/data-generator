---
phase: 17-p1-harness-expansion-closeout
plan: 01
subsystem: testing
tags: [test-matrix, harness, P1, TEST-09, EXEC-01, EXEC-02, SEC-01, DIST-01]

requires:
  - phase: 12-http-execute-path-proof
    provides: ManagedJdbcCatalogHttpExecuteIT, ManagedJdbcCatalogHttpPostgresUpsertIT
  - phase: 15-multi-jvm-worker-e2e
    provides: dist-multi-jvm-worker row (script-primary)
  - phase: 16-rbac-enable-path
    provides: ConsoleAuthorizationIntegrationIT stack
provides:
  - Three new P1 matrix rows (exec-http-managed-catalog, exec-http-postgres-dialect, rbac-enable-path)
  - Verified dist-multi-jvm-worker row unchanged (no duplicate)
affects: [17-02 doc regen, 17-03 milestone closeout]

tech-stack:
  added: []
  patterns: [Maven-linked P1 rows for HTTP/RBAC proofs; script-primary DIST row retained]

key-files:
  created: []
  modified:
    - .planning/test-matrix.yaml

key-decisions:
  - "HTTP rows link Phase 12 IT classes only (D-04); no P0 promotion (D-08)"
  - "RBAC row links three Maven classes; verify-rbac-enable.ps1 cited as supplementary (D-05)"
  - "dist-multi-jvm-worker left script-primary with empty linked_tests (D-06)"

patterns-established:
  - "v2.1 proof paths wired as P1-only; P0 merge gate frozen at 15 rows"

requirements-completed: [TEST-09]

coverage:
  - id: D1
    description: exec-http-managed-catalog P1 row with ManagedJdbcCatalogHttpExecuteIT linkage
    requirement: TEST-09
    verification:
      - kind: integration
        ref: "ManagedJdbcCatalogHttpExecuteIT"
        status: pass
    human_judgment: false
  - id: D2
    description: exec-http-postgres-dialect P1 row with ManagedJdbcCatalogHttpPostgresUpsertIT linkage
    requirement: TEST-09
    verification:
      - kind: integration
        ref: "ManagedJdbcCatalogHttpPostgresUpsertIT"
        status: pass
    human_judgment: false
  - id: D3
    description: rbac-enable-path P1 row with ConsoleAuthorization IT/unit stack
    requirement: TEST-09
    verification:
      - kind: integration
        ref: "ConsoleAuthorizationIntegrationIT"
        status: pass
      - kind: integration
        ref: "ConsoleSecurityDefaultOffIT"
        status: pass
      - kind: unit
        ref: "ConsoleAuthorizationFilterTest"
        status: pass
    human_judgment: false
  - id: D4
    description: dist-multi-jvm-worker row verified present, P1, script-primary, no duplicate
    requirement: TEST-09
    verification:
      - kind: other
        ref: "scripts/verify-multi-jvm-worker.ps1 (notes citation; linked_tests empty)"
        status: pass
    human_judgment: false

duration: 10min
completed: 2026-07-29
status: complete
---

# Phase 17: P1 Harness Expansion — Plan 01 Summary

**Three v2.1 proof paths wired into test-matrix.yaml as P1 rows; 15-row P0 merge gate unchanged.**

## Performance

- **Duration:** ~10 min
- **Tasks:** 3/3
- **Files modified:** 1 (`.planning/test-matrix.yaml`)

## Accomplishments

- Added `exec-http-managed-catalog` (EXEC-01) → `ManagedJdbcCatalogHttpExecuteIT`
- Added `exec-http-postgres-dialect` (EXEC-02) → `ManagedJdbcCatalogHttpPostgresUpsertIT`
- Added `rbac-enable-path` (SEC-01) → three Console security IT/unit classes
- Verified existing `dist-multi-jvm-worker` (DIST-01): P1, `linked_tests: []`, notes cite `verify-multi-jvm-worker.ps1`; no duplicate row

## P0 / P1 Invariants

| Tier | Count | Notes |
|------|-------|-------|
| P0 | **15** | Unchanged ids: calcite-scenario-v2, udf-*, transform-*, v2-streaming-*, v2-jdbc-upsert-pg-mysql, v2-dialect-* |
| P1 | **12** | Was 9; +3 new (exec-http-managed-catalog, exec-http-postgres-dialect, rbac-enable-path) |

- `scripts/verify-harness.ps1` — not modified (gate semantics preserved)

## Decisions Made

None — followed plan 17-01 as specified.

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None

## Next Phase Readiness

- Plan 17-02: regenerate `docs/test-feature-matrix.md`, run harness smoke
- Plan 17-03: update `docs/test-harness.md`, AGENTS.md, milestone state

---
*Phase: 17-p1-harness-expansion-closeout*
*Completed: 2026-07-29*
