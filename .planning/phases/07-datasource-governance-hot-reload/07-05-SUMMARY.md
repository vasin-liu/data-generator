---
phase: 07-datasource-governance-hot-reload
plan: 05
subsystem: e2e
tags: [playwright, playwright-cli, uat, datasource, governance, hot-reload, audit]

requires:
  - phase: 07-datasource-governance-hot-reload
    provides: Console UX, REST governance flags, hot-reload runtime (07-01..07-04)
provides:
  - datasource-governance.spec.ts with 10 Phase 7 operator scenarios (D-27)
  - e2e/helpers/datasource-governance.ts API + isolation helpers
  - playwright-cli HEALTHY/DEGRADED snapshot automation (D-28)
  - verify-phase7-uat-datasource-governance.ps1 and verify-phase7-uat-hot-reload.ps1
  - npm scripts e2e:phase7-governance and e2e:cli:phase7-governance
affects: [phase-7-complete, harness-matrix-future]

tech-stack:
  added: []
  patterns:
    - "Phase 7 UAT Maven slice: DatasourceGovernanceIT + audit + catalog test + HotReloadTests + ConnectionSnapshotIT"
    - "Podman UAT uses e2e,staging profile + DG_E2E_GOVERNANCE_STAGING for governance-block scenario"
    - "Hot-reload E2E uses manual workflow pause + /task/executions API snapshot proxy (D-10)"

key-files:
  created:
    - data-generator-console-web/e2e/specs/datasource-governance.spec.ts
    - data-generator-console-web/e2e/helpers/datasource-governance.ts
    - data-generator-console-web/e2e/cli/run-datasource-governance-cli.ps1
    - data-generator-console-web/e2e/snapshots/governance/.gitkeep
    - scripts/verify-phase7-uat-datasource-governance.ps1
    - scripts/verify-phase7-uat-hot-reload.ps1
  modified:
    - data-generator-console-web/e2e/helpers/api.ts
    - data-generator-console-web/package.json
    - .planning/ROADMAP.md
    - AGENTS.md

key-decisions:
  - "In-flight isolation asserted via legacy task execution summary unchanged after datasource save (no public snapshot JSON API)"
  - "Governance gate/connectivity-block UI tests skip when profile flags off; staging Podman sets DG_E2E_GOVERNANCE_STAGING"

patterns-established:
  - "Phase 7 verify scripts mirror Phase 6 UAT layout with -SkipPlaywright Maven gate"
  - "playwright-cli snapshots land under e2e/snapshots/governance/"

requirements-completed: [DS-03, DS-04, DS-05]

duration: 45min
completed: 2026-06-27
---

# Phase 07 Plan 05 Summary

**Playwright E2E, playwright-cli snapshots, and Phase 7 UAT verify scripts for datasource governance & hot-reload (D-27, D-28)**

## Accomplishments

- Added `datasource-governance.spec.ts` with 10 scenarios: CRUD + unified test (JDBC/Kafka/ES), connectivity gate, driver preset round-trip, hot-reload isolation, DEGRADED UI, governance publish block (staging), audit deep-link, template run regression, HEALTHY list screenshot
- Extended `e2e/helpers/datasource-governance.ts` with `forceReloadFailure`, `waitForExecutionRunning`, `getConnectionSnapshot`, `listAuditEvents`, and template builders
- Added `run-datasource-governance-cli.ps1` for HEALTHY list + DEGRADED detail playwright-cli snapshots
- Added `verify-phase7-uat-datasource-governance.ps1` and `verify-phase7-uat-hot-reload.ps1` with `-SkipPlaywright` Maven gates
- Updated ROADMAP Phase 7 plans/waves; npm `e2e:phase7-governance` + `e2e:cli:phase7-governance`

## Verification

```powershell
powershell -NoProfile -File scripts/verify-phase7-uat-datasource-governance.ps1 -SkipPlaywright
cd data-generator-console-web && npm run verify:unit
```

Full UAT (Podman + Playwright + playwright-cli):

```powershell
powershell -NoProfile -File scripts/verify-phase7-uat-datasource-governance.ps1
```
