---
phase: 13-dameng-live-path-nyquist-hygiene
plan: 01
subsystem: testing
tags: [dameng, jdbc, upsert, integration-test, junit5]

requires:
  - phase: 09-jdbc-dialect-expansion
    provides: UpsertParitySupport shared upsert-idempotency helper, Dameng dialect key precedent
provides:
  - Real external-JDBC ChunkedPipelineDamengUpsertIT that can PASS against a configured Dameng host
  - Hard-fail-on-misconfiguration behavior with observed Surefire evidence (no soft-skip disguised as green)
  - DamengTestSupport Javadoc documenting the full opt-in env contract
affects: [13-02-dameng-live-path-docs-script, dameng-live-it, nyquist-hygiene]

tech-stack:
  added: [com.dameng:dm-jdbc (test scope, data-generator-calcite)]
  patterns: ["env-only opt-in live IT wiring via shared UpsertParitySupport helper (same pattern as Postgres/MySQL/Kingbase/HighGo)"]

key-files:
  created: []
  modified:
    - data-generator-calcite/pom.xml
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/DamengTestSupport.java

key-decisions:
  - "Connection config is env-only (DG_DM_JDBC_URL/DG_DM_USER/DG_DM_PASSWORD); no -D system-property mirrors, per RESEARCH Open Question 2 resolution"
  - "Env-reading helper throws IllegalStateException naming only the missing variable, never its value, so a real DG_DM_PASSWORD cannot reach Surefire output (T-13-01)"
  - "UpsertParitySupport left completely unmodified to avoid regressing the Postgres/MySQL/Kingbase/HighGo ITs that share it (D-04, T-13-06)"

requirements-completed: [DIAL-01]

coverage:
  - id: D1
    description: "data-generator-calcite gains a test-scope com.dameng:dm-jdbc dependency (version inherited from root BOM) so dm.jdbc.driver.DmDriver loads at runtime"
    requirement: "DIAL-01"
    verification:
      - kind: unit
        ref: ".\\mvnw-jdk25.ps1 -pl data-generator-calcite -am test-compile"
        status: pass
    human_judgment: false
  - id: D2
    description: "ChunkedPipelineDamengUpsertIT is rewired to real external JDBC: reads DG_DM_JDBC_URL/DG_DM_USER/DG_DM_PASSWORD and delegates to UpsertParitySupport.assertUpsertIdempotent with dm.jdbc.driver.DmDriver and dialect key dameng"
    requirement: "DIAL-01"
    verification:
      - kind: integration
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java#chunkedUpsertDamengMergeIsIdempotent"
        status: unknown
    human_judgment: true
    rationale: "No reachable Dameng host exists in this environment (RESEARCH Environment Availability), so the PASS path against a real host cannot be exercised here; D-05 accepts this as honest given the documented enable path and unchanged MERGE-unit default CI bar."
  - id: D3
    description: "Flag-on-but-misconfigured run (DG_DM_IT=true, DG_DM_JDBC_URL unset) fails the Maven build with a genuine Surefire failure result, not a skip"
    requirement: "DIAL-01"
    verification:
      - kind: integration
        ref: "mvnw-jdk25.ps1 -pl data-generator-calcite -am -Dtest=ChunkedPipelineDamengUpsertIT test (DG_DM_IT=true, DG_DM_JDBC_URL unset) -> BUILD FAILURE, surefire-reports errors=1 skipped=0"
        status: pass
    human_judgment: false
  - id: D4
    description: "Flag-off run still skips ChunkedPipelineDamengUpsertIT cleanly via the class-level DamengTestSupport gate, keeping default CI unchanged"
    requirement: "DIAL-01"
    verification:
      - kind: integration
        ref: "mvnw-jdk25.ps1 -pl data-generator-calcite -am -Dtest=ChunkedPipelineDamengUpsertIT test (flag unset) -> BUILD SUCCESS, Skipped: 1"
        status: pass
    human_judgment: false
  - id: D5
    description: "DamengTestSupport Javadoc documents the complete opt-in contract (flag + three env vars + hard-fail-on-misconfig + unchanged MERGE-unit default CI bar), with damengItEnabled() logic frozen"
    requirement: "DIAL-01"
    verification:
      - kind: unit
        ref: "rg -n \"DG_DM_JDBC_URL\" DamengTestSupport.java; rg -n \"public static boolean damengItEnabled\" DamengTestSupport.java"
        status: pass
    human_judgment: false

duration: 41min
completed: 2026-07-28
status: complete
---

# Phase 13 Plan 01: Dameng Live JDBC Wiring + Hard-Fail Gate Summary

**Rewired the opt-in `ChunkedPipelineDamengUpsertIT` from a permanent `Assumptions.abort` placeholder to real env-driven JDBC wiring against `UpsertParitySupport`, with observed evidence that a misconfigured opt-in run now fails the build instead of masquerading as a skip.**

## Performance

- **Duration:** 41 min
- **Started:** 2026-07-28T09:02:00Z
- **Completed:** 2026-07-28T09:43:50Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- Added test-scope `com.dameng:dm-jdbc` to `data-generator-calcite/pom.xml` (version inherited from root BOM `${dm.version}`)
- Replaced the `ChunkedPipelineDamengUpsertIT` placeholder body with real logic that reads `DG_DM_JDBC_URL`/`DG_DM_USER`/`DG_DM_PASSWORD` and delegates to `UpsertParitySupport.assertUpsertIdempotent(..., "dm.jdbc.driver.DmDriver", "dameng")`
- Removed the soft-skip `Assumptions.abort` escape hatch and its import entirely; a missing/blank required env var now throws `IllegalStateException` naming only the variable (never its value)
- Proved via live Maven runs that flag-on + missing URL yields `BUILD FAILURE` with a genuine Surefire `<error>` (errors=1, skipped=0), while flag-off still yields `BUILD SUCCESS` with the IT skipped
- Refreshed `DamengTestSupport` Javadoc to document the complete opt-in contract (flag, three env vars, hard-fail semantics, unchanged MERGE-unit CI bar) without touching `damengItEnabled()` logic

## Task Commits

Each task was committed atomically:

1. **Task 1: Add test-scope dm-jdbc and wire the Dameng live IT to real JDBC** - `e6a32f9` (feat)
2. **Task 2: Prove flag-on-misconfigured fails the build, and document the env contract on the gate** - `b0c6c41` (docs)

_Task 2 required no code change to the IT or `DamengTestSupport` behavior beyond Javadoc, so its evidence-gathering step and doc update landed in a single `docs(13-01)` commit._

## Files Created/Modified
- `data-generator-calcite/pom.xml` - Added test-scope `com.dameng:dm-jdbc` dependency (no explicit version)
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java` - Real env-driven JDBC IT delegating to `UpsertParitySupport`, hard-fail helper, refreshed Javadoc
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/DamengTestSupport.java` - Javadoc-only refresh documenting the full opt-in env contract

## Decisions Made
- Env-only connection config (no `-D` system-property mirrors for URL/user/password) per RESEARCH Open Question 2 resolution — avoids PowerShell `-D` quoting problems
- Failure message names only the missing variable, never its value, to prevent credential leakage into Surefire/CI logs (T-13-01)
- `UpsertParitySupport` left byte-for-byte unmodified since it is shared with the PostgreSQL, MySQL, Kingbase, and HighGo upsert ITs (D-04, T-13-06)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. No reachable Dameng host exists in this environment, which is expected and documented in RESEARCH Environment Availability; the plan's own success criteria treat this as honest (D-05) rather than a blocker, since the negative-path (misconfigured) and skip-path (flag-off) behaviors were both directly observed with real Maven/Surefire evidence.

## User Setup Required
None - no external service configuration required by this plan. (A maintainer recipe for actually running the live IT against a real Dameng host is deferred to plan 13-02.)

## Next Phase Readiness
- DIAL-01 code half is complete: the IT is real, env-driven, hard-fails on misconfiguration, and reuses `UpsertParitySupport`
- Ready for `13-02` to add the opt-in UAT wrapper script, JDBC sink guide recipe section, and `AGENTS.md` pointer
- No P0/harness/test-matrix files were touched; default CI behavior is unchanged

## Self-Check: PASSED

---
*Phase: 13-dameng-live-path-nyquist-hygiene*
*Completed: 2026-07-28*
