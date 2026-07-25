---
phase: 09-jdbc-dialect-expansion
plan: 02
subsystem: database
tags: [jdbc, console, presets, connectivity, dameng, kingbase, highgo, postgresql, clickhouse]

requires:
  - phase: 09-jdbc-dialect-expansion
    plan: 01
    provides: JdbcSinkSqlBuilder dialect matrix and publish validation baseline
provides:
  - Five-engine JDBC driver preset catalog tests and driver-presets API contract
  - Console fallback preset sync for DM/KB/HG/CK/PG
  - ConnectionConnectivityService secret-safe failure summaries (D-11)
affects: [09-03, 09-04, 09-05]

tech-stack:
  added: []
  patterns:
    - "Frontend JDBC_DRIVER_PRESETS_FALLBACK mirrors JdbcDriverPresetCatalog fields"
    - "sanitizeOperatorMessage redacts passwords and JDBC URL userinfo before console display"

key-files:
  created: []
  modified:
    - data-generator-service/src/test/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalogTests.java
    - data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleDataSourceControllerTest.java
    - data-generator-console-web/src/app/datasources/jdbcDriverPresets.ts
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionConnectivityService.java
    - data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogTestTests.java

key-decisions:
  - "Fallback presets mark DM/KB/HG bundled:true to match jdbc-bundled distribution policy"
  - "Proprietary driver failures prefix actionable driverClassName hint in operator message"

patterns-established:
  - "ConnectionCatalogTestTests proprietary driver helper asserts no s3cr3t or userinfo in failure message"

requirements-completed: [RW-06]

coverage:
  - id: D1
    description: GET /api/datasources/driver-presets returns complete five-engine presets with bundled flags
    requirement: RW-06
    verification:
      - kind: unit
        ref: data-generator-service/src/test/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalogTests.java
        status: pass
      - kind: unit
        ref: data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleDataSourceControllerTest.java#driverPresets_returnsCatalog
        status: pass
    human_judgment: false
  - id: D2
    description: Console fallback presets aligned with server catalog; tsc and production build green
    requirement: RW-06
    verification:
      - kind: unit
        ref: data-generator-console-web npx tsc -p tsconfig.json --noEmit
        status: pass
      - kind: unit
        ref: data-generator-console-web npm run build
        status: pass
    human_judgment: false
  - id: D3
    description: DM/KB/HG connectivity failures actionable without password or JDBC userinfo leakage
    requirement: RW-06
    verification:
      - kind: integration
        ref: data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogTestTests.java
        status: pass
    human_judgment: false

duration: 50min
completed: 2026-07-21
status: complete
---

# Phase 9 Plan 02: Console JDBC Presets and Connectivity Summary

**Five-engine driver preset catalog tests, console fallback sync, and secret-safe connectivity failure messages for DM/KB/HG/PostgreSQL/ClickHouse**

## Performance

- **Duration:** ~50 min
- **Started:** 2026-07-21T12:08:00Z
- **Completed:** 2026-07-21T12:22:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Extended `JdbcDriverPresetCatalogTests` and `ConsoleDataSourceControllerTest` for all five Phase 9 JDBC engine groups (D-09, D-10)
- Synced `jdbcDriverPresets.ts` fallback with server catalog including bundled flags for proprietary drivers
- Hardened `ConnectionConnectivityService` to redact passwords and JDBC URL userinfo in operator-facing messages (D-11)
- Added DM/KB/HG connectivity contract tests proving actionable failures without secret leakage

## Task Commits

Each task was committed atomically:

1. **Task 1: Complete JdbcDriverPresetCatalog and bundled driver flags** - `8a4f65c` (test)
2. **Task 2: Sync console fallback presets and connectivity secret hygiene** - `e4eb601` (feat)

**Plan metadata:** `730f21f` (docs: complete plan)

## Files Created/Modified

- `data-generator-service/src/test/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalogTests.java` - HighGo/DM/CK/PG preset and bundle assertions
- `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleDataSourceControllerTest.java` - Extended driver-presets JSON contract
- `data-generator-console-web/src/app/datasources/jdbcDriverPresets.ts` - Fallback sync with bundled DM/KB/HG and CK/PG entries
- `data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionConnectivityService.java` - sanitizeOperatorMessage and driver hints
- `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogTestTests.java` - Proprietary driver secret hygiene tests

## Decisions Made

- Fallback presets use `bundled: true` for DM/KB/HG to match server bundle policy when API is unavailable
- Proprietary driver failure messages include `[driverClassName]` prefix for operator actionability without echoing secrets

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Lenient Mockito stub for governance in controller test**
- **Found during:** Task 1 verification (focused `-Dtest` run)
- **Issue:** `driverPresets_returnsCatalog` triggered UnnecessaryStubbing on `properties.getGovernance()` from setUp
- **Fix:** Wrapped governance stub with `lenient()` in `ConsoleDataSourceControllerTest.setUp`
- **Files modified:** `ConsoleDataSourceControllerTest.java`
- **Committed in:** `8a4f65c`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Required for focused test slice to pass; no scope creep.

## Issues Encountered

None beyond auto-fixed Mockito strictness in focused test run.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 09-03 can proceed with UpsertParitySupport ITs for kingbase/highgo dialect keys
- Plan 09-04 can add Playwright preset selection E2E via verify-phase9 script
- Operator docs (09-05) can reference preset catalog and connectivity hygiene behavior

## Self-Check: PASSED

- FOUND: data-generator-service/src/test/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalogTests.java
- FOUND: data-generator-console-web/src/app/datasources/jdbcDriverPresets.ts
- FOUND: data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionConnectivityService.java
- FOUND: .planning/phases/09-jdbc-dialect-expansion/09-02-SUMMARY.md
- FOUND: 8a4f65c, e4eb601, 730f21f

---
*Phase: 09-jdbc-dialect-expansion*
*Completed: 2026-07-21*
