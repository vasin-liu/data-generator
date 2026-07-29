---
phase: 16-rbac-enable-path
plan: 01
subsystem: security
tags: [rbac, console-security, spring-boot-test, SEC-01]

requires: []
provides:
  - ConsoleSecurityDefaultOffIT default-off regression guard (Pitfall 4)
  - ConsoleAuthorizationIntegrationIT VIEWER GET allow-path
  - scripts/verify-rbac-enable.ps1 Maven slice with -SkipPlaywright
affects: [16-02, 16-03]

tech-stack:
  added: []
  patterns:
    - "Default-off SpringBootTest guard on application-phase7-test.yaml"
    - "Phase verify script Maven slice + deferred Playwright stub"

key-files:
  created:
    - data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleSecurityDefaultOffIT.java
    - scripts/verify-rbac-enable.ps1
  modified:
    - data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleAuthorizationIntegrationIT.java

key-decisions:
  - "Default-off IT loads phase7-test without RBAC property override"
  - "verify-rbac-enable.ps1 -SkipPlaywright is Wave 1 gate; Playwright leg deferred to 16-03"
  - "Single VIEWER GET allow-path added; full matrix stays in Playwright rbac.console.spec.ts"

requirements-completed: [SEC-01]

coverage:
  - id: D1
    description: Default-off regression — ConsoleSecurityProperties.enabled false on phase7-test
    requirement: SEC-01
    verification:
      - kind: integration
        ref: "data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleSecurityDefaultOffIT.java#consoleSecurityDisabledByDefaultOnPhase7TestProfile"
        status: pass
    human_judgment: false
  - id: D2
    description: Base application.yaml guard — no console-security.enabled true
    requirement: SEC-01
    verification:
      - kind: integration
        ref: "data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleSecurityDefaultOffIT.java#baseApplicationYamlDoesNotEnableConsoleSecurity"
        status: pass
    human_judgment: false
  - id: D3
    description: Canonical RBAC-on HTTP deny/allow IntegrationIT green
    requirement: SEC-01
    verification:
      - kind: integration
        ref: "data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleAuthorizationIntegrationIT.java"
        status: pass
    human_judgment: false
  - id: D4
    description: Filter + UDF authorization unit tests green
    requirement: SEC-01
    verification:
      - kind: unit
        ref: "ConsoleAuthorizationFilterTest,ConsoleUdfAuthorizationFilterTest"
        status: pass
    human_judgment: false
  - id: D5
    description: Operator Maven one-liner verify-rbac-enable.ps1 -SkipPlaywright
    requirement: SEC-01
    verification:
      - kind: other
        ref: "powershell -NoProfile -File scripts/verify-rbac-enable.ps1 -SkipPlaywright"
        status: pass
    human_judgment: false

duration: 40min
completed: 2026-07-29T10:03:11Z
status: complete
---

# Phase 16 Plan 01: RBAC Enable Path Backend Proof Summary

**Default-off regression guard, RBAC-on HTTP IntegrationIT with VIEWER allow-path, and Maven verify script — all green without enabling RBAC by default.**

## Performance

- **Duration:** 40 min
- **Started:** 2026-07-29T09:23:00Z
- **Completed:** 2026-07-29T10:03:11Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- `ConsoleSecurityDefaultOffIT` guards Pitfall 4: `enabled == false` on phase7-test profile and base `application.yaml` has no `console-security.enabled: true`
- `ConsoleAuthorizationIntegrationIT` retains four deny/governance tests and adds `viewerCanGetScenariosCatalogWhenSecurityEnabled`
- `scripts/verify-rbac-enable.ps1 -SkipPlaywright` runs four security test classes via `Invoke-RepoMaven` (BUILD SUCCESS, ~21 min)

## Task Commits

1. **Task 1: ConsoleSecurityDefaultOffIT** — `87751ce` (test)
2. **Task 2: IntegrationIT allow-path + Task 3: verify script** — `515748b` (test/chore)

**Plan metadata:** pending (this SUMMARY commit)

## Files Created/Modified

- `data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleSecurityDefaultOffIT.java` — default-off regression IT
- `data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleAuthorizationIntegrationIT.java` — VIEWER GET allow-path
- `scripts/verify-rbac-enable.ps1` — SEC-01 Maven operator one-liner

## Decisions Made

None — followed plan 16-01 as written (D-03 through D-05, D-08, D-09 honored; no matrix/harness edits).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## Verification

| Check | Result |
|-------|--------|
| `ConsoleSecurityDefaultOffIT` in test tree | PASS |
| `scripts/verify-rbac-enable.ps1` exists | PASS |
| `application-e2e.yaml` console-security enabled: false | PASS |
| `application-staging.yaml` console-security enabled: true (opt-in) | PASS |
| No edits to `test-matrix.yaml` / `verify-harness.ps1` | PASS |
| `verify-rbac-enable.ps1 -SkipPlaywright` | PASS — `[SUCCESS] SEC-01 RBAC enable-path Maven verification passed (-SkipPlaywright).` |

## Self-Check: PASSED

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Ready for **16-02** (docs/staging-console-rbac.md + AGENTS.md pointer). Playwright leg remains **16-03**.

---
*Phase: 16-rbac-enable-path*
*Completed: 2026-07-29*
