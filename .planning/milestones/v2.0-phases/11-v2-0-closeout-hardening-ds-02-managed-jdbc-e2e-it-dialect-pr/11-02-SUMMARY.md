---
phase: 11-v2-0-closeout-hardening-ds-02-managed-jdbc-e2e-it-dialect-pr
plan: 02
subsystem: testing
tags: [jdbc, kingbase, playwright, uat-script, rw-05, rw-06, dialect]

requires:
  - phase: 11-v2-0-closeout-hardening-ds-02-managed-jdbc-e2e-it-dialect-pr
    provides: ManagedJdbcCatalogSinkE2eIT for phase11 Maven -Dtest slice
  - phase: 09-jdbc-dialect-expansion
    provides: e2e:phase9-jdbc-dialect npm script and jdbc-dialect-preset.spec.ts baseline
provides:
  - Playwright kingbase8 + postgresql16 preset→save coverage in jdbc-dialect-preset.spec.ts
  - scripts/verify-phase11-uat-closeout-hardening.ps1 with -SkipPlaywright evidence pack
affects:
  - 11-03 AGENTS.md + audit flow #8 disposition
  - v2.0-MILESTONE-AUDIT flow #8

tech-stack:
  added: []
  patterns:
    - Three-piece Kingbase evidence pack (Playwright preset, Maven connectivity, PG-proxy upsert IT)
    - Supplementary UAT script clones phase9 shape; reuses e2e:phase9-jdbc-dialect (no new npm entry)

key-files:
  created:
    - scripts/verify-phase11-uat-closeout-hardening.ps1
  modified:
    - data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts

key-decisions:
  - "D-09–D-19 honored: Kingbase primary, no Dameng edits, no Test Connection, no new upsert IT, no new npm script, no P0/harness edits, -SkipPlaywright after Maven"

patterns-established:
  - "Phase closeout dialect UAT: Maven slice first, optional Podman Playwright via existing phase9 npm script"

requirements-completed: [RW-05, RW-06]

coverage:
  - id: D1
    description: Playwright covers kingbase8 and postgresql16 preset select→form fill→POST /api/datasources success without Test Connection
    requirement: RW-05
    verification:
      - kind: e2e
        ref: data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts#select kingbase8 auto-fills driver and URL then saves datasource
        status: pass
      - kind: other
        ref: npm run e2e:phase9-jdbc-dialect -- --list (2 tests; postgresql16 + kingbase8)
        status: pass
      - kind: other
        ref: npx tsc -p tsconfig.json --noEmit && npm run build
        status: pass
    human_judgment: false
  - id: D2
    description: verify-phase11-uat-closeout-hardening.ps1 Maven evidence pack green with -SkipPlaywright (managed IT + connectivity + Kingbase upsert + preset catalog)
    requirement: RW-06
    verification:
      - kind: other
        ref: .\scripts\verify-phase11-uat-closeout-hardening.ps1 -SkipPlaywright
        status: pass
      - kind: integration
        ref: data-generator-service/.../ConnectionCatalogTestTests + ChunkedPipelineKingbaseDialectTests + ManagedJdbcCatalogSinkE2eIT
        status: pass
    human_judgment: false

duration: 11 min
completed: 2026-07-25
status: complete
---

# Phase 11 Plan 02: Kingbase evidence pack (Playwright + UAT script) Summary

**Playwright `kingbase8` (+ `postgresql16`) preset→save path and `verify-phase11-uat-closeout-hardening.ps1` three-piece Kingbase evidence pack with `-SkipPlaywright` Maven green**

## Performance

- **Duration:** 11 min
- **Started:** 2026-07-25T00:51:08Z
- **Completed:** 2026-07-25T01:02:26Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Expanded `jdbc-dialect-preset.spec.ts` for `kingbase8` (`/Kingbase 8|金仓 8/i`) while retaining `postgresql16`; asserts `com.kingbase8.Driver`; no Test Connection click
- Added `scripts/verify-phase11-uat-closeout-hardening.ps1` documenting Playwright + `ConnectionCatalogTestTests` + `ChunkedPipelineKingbaseDialectTests` evidence pack; Maven `-Dtest` includes Wave 1 `ManagedJdbcCatalogSinkE2eIT` and `JdbcDriverPresetCatalogTests`
- Reused `npm run e2e:phase9-jdbc-dialect` only (D-19); P0 / `verify-harness.ps1` / `test-matrix.yaml` untouched (D-17)

## Task Commits

Each task was committed atomically:

1. **Task 1: Expand jdbc-dialect-preset.spec.ts for kingbase8 (keep postgresql16)** - `bf73401` (test)
2. **Task 2: Create verify-phase11-uat-closeout-hardening.ps1 evidence-pack UAT** - `99dcc6f` (test)

**Plan metadata:** (this commit)

## Files Created/Modified

- `data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts` - parameterized postgresql16 + kingbase8 preset→save E2E
- `scripts/verify-phase11-uat-closeout-hardening.ps1` - Phase 11 supplementary UAT (Maven + optional Podman Playwright)

## Decisions Made

- Followed CONTEXT D-09–D-19: Kingbase/HighGo primary non-PG proof; evidence pack not single-JVM chain; no Dameng code changes; no new upsert IT; no new npm script; `-SkipPlaywright` early exit after Maven

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required. Full Playwright branch needs Podman; `-SkipPlaywright` is CI-friendly. `ChunkedPipelineKingbaseDialectTests` requires Docker when not skipped by `@EnabledIf`.

## Next Phase Readiness

- Ready for Wave 3 (`11-03`) AGENTS.md + surgical audit flow #1/#8 closeout
- ROADMAP SC2 satisfied under CONTEXT override (evidence pack, Kingbase primary)

## Self-Check: PASSED

- `[ -f ]` jdbc-dialect-preset.spec.ts and verify-phase11-uat-closeout-hardening.ps1 exist
- `git log --grep=11-02` includes `bf73401`, `99dcc6f`
- Acceptance: both preset ids covered; Kingbase label regex; no Test Connection; no e2e:phase11 npm script; tsc+build+`--list` exit 0; UAT `-SkipPlaywright` BUILD SUCCESS (Tests run: 21 service + 2 calcite Kingbase, Failures: 0); no test-matrix/verify-harness edits

---
*Phase: 11-v2-0-closeout-hardening-ds-02-managed-jdbc-e2e-it-dialect-pr*
*Completed: 2026-07-25*
