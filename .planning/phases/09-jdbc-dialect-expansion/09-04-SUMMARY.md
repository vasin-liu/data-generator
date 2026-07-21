---
phase: 09-jdbc-dialect-expansion
plan: 04
subsystem: testing
tags: [jdbc, playwright, uat, dialect, presets, maven]

requires:
  - phase: 09-jdbc-dialect-expansion
    plan: 01
    provides: JdbcSinkSqlBuilder dialect matrix and validator publish gates
  - phase: 09-jdbc-dialect-expansion
    plan: 02
    provides: JdbcDriverPresetCatalog and connectivity contracts
  - phase: 09-jdbc-dialect-expansion
    plan: 03
    provides: Embedded dialect IT classes for Maven slice
provides:
  - scripts/verify-phase9-uat-jdbc-dialect.ps1 Maven dialect gate with -SkipPlaywright
  - Playwright jdbc-dialect-preset.spec.ts preset auto-fill and save path
  - npm e2e:phase9-jdbc-dialect script for Podman UAT
affects: [09-05]

tech-stack:
  added: []
  patterns:
    - "Phase 9 UAT mirrors Phase 8: Maven slice first, optional Podman Playwright (D-16)"
    - "One Playwright preset path only — postgresql16 select/auto-fill/save (D-12)"

key-files:
  created:
    - scripts/verify-phase9-uat-jdbc-dialect.ps1
    - data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts
  modified:
    - data-generator-console-web/package.json

key-decisions:
  - "PostgreSQL 16 preset chosen for Playwright path (one engine, server catalog authoritative)"
  - "Maven slice lists 09-01..09-03 test classes via -Dtest comma list on data-generator-service -am"

patterns-established:
  - "verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright is CI merge-friendly dialect gate"
  - "E2E fetches /api/datasources/driver-presets then asserts UI auto-fill before save"

requirements-completed: [RW-05, RW-06]

coverage:
  - id: D1
    description: Phase 9 UAT verify script runs Maven dialect IT slice with -SkipPlaywright exit 0
    requirement: RW-06
    verification:
      - kind: integration
        ref: scripts/verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright
        status: pass
    human_judgment: false
  - id: D2
    description: Playwright preset select auto-fills driver/URL and saves datasource via POST /api/datasources
    requirement: RW-05
    verification:
      - kind: e2e
        ref: data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts#select preset auto-fills driver and URL then saves datasource
        status: pass
      - kind: automated_ui
        ref: npm run e2e:phase9-jdbc-dialect -- --list
        status: pass
    human_judgment: false
  - id: D3
    description: Console-web tsc and production build pass before E2E registration
    requirement: RW-06
    verification:
      - kind: unit
        ref: npx tsc -p tsconfig.json --noEmit && npm run build
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-07-21
status: complete
---

# Phase 9 Plan 04: UAT Verify Script and Playwright Preset E2E Summary

**Phase 9 JDBC dialect UAT entry point with Maven embedded gate and one Playwright preset auto-fill save path**

## Performance

- **Duration:** ~45 min
- **Started:** 2026-07-21T12:50:00Z
- **Completed:** 2026-07-21T13:35:00Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- Added `scripts/verify-phase9-uat-jdbc-dialect.ps1` running eight-class Maven dialect slice with `-SkipPlaywright` CI gate (D-16)
- Created `jdbc-dialect-preset.spec.ts` covering postgresql16 preset select, driver/URL auto-fill from API catalog, and save (D-12)
- Registered `e2e:phase9-jdbc-dialect` npm script wired into verify script Playwright branch

## Task Commits

Each task was committed atomically:

1. **Task 1: Create verify-phase9-uat-jdbc-dialect.ps1 Maven slice** - `e3c7161` (chore)
2. **Task 2: Playwright preset E2E and npm script wiring** - `9f9481d` (feat)

**Plan metadata:** pending (docs: complete plan)

## Files Created/Modified

- `scripts/verify-phase9-uat-jdbc-dialect.ps1` - Phase 9 UAT Maven + optional Podman Playwright
- `data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts` - One preset workflow E2E
- `data-generator-console-web/package.json` - `e2e:phase9-jdbc-dialect` script entry

## Decisions Made

- Used `postgresql16` preset for Playwright (plan allowed dm8 or postgresql16; PG preset has stable i18n label)
- Synthetic E2E password `e2e_test_only`; unreachable JDBC host acceptable for save-only path (T-09-11)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Nested `powershell -File` verify invocation can fail when host `JAVA_HOME` points to JDK 8; direct script call or clearing `JAVA_HOME` allows `mvnw-jdk25.ps1` path (same as other phase verify scripts)

## User Setup Required

None for `-SkipPlaywright` Maven gate. Full Podman Playwright UAT requires Podman and JDK 25 via `mvnw-jdk25.ps1`.

## Next Phase Readiness

- Plan 09-05 can document operator guide updates and AGENTS.md verify script entry (D-17, D-18)
- Harness P0 matrix expansion remains Phase 10

## Self-Check: PASSED

- FOUND: scripts/verify-phase9-uat-jdbc-dialect.ps1
- FOUND: data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts
- FOUND: .planning/phases/09-jdbc-dialect-expansion/09-04-SUMMARY.md
- FOUND: e3c7161, 9f9481d

---
*Phase: 09-jdbc-dialect-expansion*
*Completed: 2026-07-21*
