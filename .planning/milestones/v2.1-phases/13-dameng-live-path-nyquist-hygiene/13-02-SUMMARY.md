---
phase: 13-dameng-live-path-nyquist-hygiene
plan: 02
subsystem: testing
tags: [dameng, uat, documentation, jdbc, opt-in]

requires:
  - phase: 13-dameng-live-path-nyquist-hygiene
    provides: ChunkedPipelineDamengUpsertIT wired to real external JDBC with hard-fail-on-misconfig (13-01)
provides:
  - Opt-in Dameng live IT UAT wrapper that fails closed when unconfigured
  - Documented Dameng live IT recipe with PASS/FAIL semantics and never-commit-secrets warning
  - AGENTS.md Commands entry pointing at the wrapper script
affects: [dial-01-complete, dameng-live-it, maintainer-repro]

tech-stack:
  added: []
  patterns: ["fail-closed UAT wrapper precheck before Invoke-RepoMaven (phase verify-*-uat style)"]

key-files:
  created:
    - scripts/verify-phase13-uat-dameng-live.ps1
  modified:
    - docs/template-v2-jdbc-sink-guide.md
    - AGENTS.md

key-decisions:
  - "Script named verify-phase13-uat-dameng-live.ps1 to match verify-phase{N}-uat-* convention (RESEARCH Open Question 1)"
  - "Unconfigured runs exit 1 with variable names only — never password or full JDBC URL (T-13-01, D-16)"
  - "Docs claim enable path + expected semantics only; no observed live PASS asserted here (D-05, T-13-09)"

requirements-completed: [DIAL-01]

coverage:
  - id: D1
    description: "scripts/verify-phase13-uat-dameng-live.ps1 fails closed when DG_DM_IT/URL/USER/PASSWORD missing, then drives ChunkedPipelineDamengUpsertIT via Invoke-RepoMaven"
    requirement: "DIAL-01"
    verification:
      - kind: other
        ref: "powershell -NoProfile -File scripts/verify-phase13-uat-dameng-live.ps1 (env unset) -> exit 1 + usage"
        status: pass
    human_judgment: false
  - id: D2
    description: "JDBC sink guide Dameng live IT recipe covers flag forms, DG_DM_* vars, wrapper + Maven commands, PASS/FAIL semantics, host prerequisites, never-commit warning, MERGE-unit merge bar"
    requirement: "DIAL-01"
    verification:
      - kind: other
        ref: "rg DG_DM_JDBC_URL|buildsDamengMergeInto docs/template-v2-jdbc-sink-guide.md"
        status: pass
    human_judgment: false
  - id: D3
    description: "AGENTS.md Commands section lists verify-phase13-uat-dameng-live.ps1 with pointer to the recipe"
    requirement: "DIAL-01"
    verification:
      - kind: other
        ref: "rg verify-phase13-uat-dameng-live AGENTS.md"
        status: pass
    human_judgment: false
  - id: D4
    description: "Stale JdbcSinkSqlBuilderDamengMergeTests reference corrected to JdbcSinkSqlBuilderTests.buildsDamengMergeInto; P0 gate files untouched"
    requirement: "DIAL-01"
    verification:
      - kind: other
        ref: "rg JdbcSinkSqlBuilderDamengMergeTests docs/ -> no matches; verify-phase13 absent from test-matrix/harness"
        status: pass
    human_judgment: false

duration: ~45min
completed: 2026-07-28
status: complete
---

# Phase 13: Plan 02 Summary

**Dameng opt-in live path is now reproducible from docs alone: fail-closed UAT wrapper, JDBC sink recipe, and AGENTS.md pointer complete DIAL-01.**

## Performance

- **Duration:** ~45 min (tasks committed earlier; SUMMARY closed out after interrupted executor)
- **Completed:** 2026-07-28
- **Tasks:** 2/2
- **Files modified:** 3

## Accomplishments

- Added `scripts/verify-phase13-uat-dameng-live.ps1` — fail-closed precheck on `DG_DM_IT` + three connection env vars, then `Invoke-RepoMaven` slice for `ChunkedPipelineDamengUpsertIT`
- Extended `docs/template-v2-jdbc-sink-guide.md` with Dameng live IT recipe (flag forms, env vars, commands, PASS/FAIL, host prerequisites, never-commit secrets, MERGE-unit merge bar)
- Corrected stale Dameng MERGE unit-test cross-reference to `JdbcSinkSqlBuilderTests.buildsDamengMergeInto`
- Added AGENTS.md Commands entry for the opt-in Dameng live run

## Task Commits

1. `01c9819` — feat(13-02): add opt-in Dameng live IT UAT wrapper script
2. `fc47a4b` — docs(13-02): document the Dameng live IT recipe and AGENTS.md entry

## Self-Check: PASSED

- Unconfigured wrapper exits 1 with usage listing all four env vars (no password/URL values)
- Docs contain `DG_DM_JDBC_URL` and `buildsDamengMergeInto`; AGENTS.md references the wrapper
- P0 gate (`test-matrix.yaml`, `verify-harness.ps1`) untouched
- DIAL-01 marked complete (code from 13-01 + packaging from this plan)
