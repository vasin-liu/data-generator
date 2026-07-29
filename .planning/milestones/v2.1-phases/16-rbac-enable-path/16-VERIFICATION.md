---
phase: 16-rbac-enable-path
status: passed
verified: 2026-07-29
score: 12/12
requirement: SEC-01
---

# Phase 16 Verification

## Goal

Documented, testable header-RBAC enable path; IT/E2E prove deny/allow when enabled; base/local defaults stay off.

## Must-haves

| # | Truth | Result |
|---|-------|--------|
| 1 | Default-off IT (`ConsoleSecurityDefaultOffIT`) | pass |
| 2 | Base application.yaml has no `console-security.enabled: true` | pass |
| 3 | `ConsoleAuthorizationIntegrationIT` canonical RBAC-on proof (+ VIEWER allow) | pass |
| 4 | Filter unit tests green | pass |
| 5 | `scripts/verify-rbac-enable.ps1 -SkipPlaywright` exits 0 | pass |
| 6 | `docs/staging-console-rbac.md` + AGENTS pointer | pass |
| 7 | Cross-link from `operator-console-usage.md` | pass |
| 8 | Profile contract D-09 preserved | pass |
| 9 | Optional Playwright leg wired (Podman/`e2e-rbac`); not sole gate | pass (wiring; full E2E not required for closeout) |
| 10 | `DG_E2E_RBAC` vs `DG_E2E_GOVERNANCE_STAGING` documented | pass |
| 11 | No P1 matrix / OAuth / default-on | pass |
| 12 | SEC-01 packaged for operators | pass |

## Evidence

- `.\scripts\verify-rbac-enable.ps1 -SkipPlaywright` → BUILD SUCCESS twice (Wave 1 + Wave 3)
- Deliverables: default-off IT, verify script, staging-console-rbac.md, AGENTS, optional Playwright wiring

## Gaps

None blocking. Full Podman Playwright RBAC leg remains optional operator drill.

## Next

`phase.complete 16` → Phase 17 (P1 Harness Expansion + Closeout)
