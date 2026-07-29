---
phase: 16
slug: rbac-enable-path
status: pending
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-29
---

# Phase 16 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Research skipped (`research_enabled=false`); checks are Maven grep/script-oriented for the SEC-01 enable-path.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (`@SpringBootTest`, filter unit tests) + PowerShell verify script + optional Playwright (Podman `e2e-rbac`) |
| **Config files** | `application.yaml` (no block → default off), `application-phase7-test.yaml`, `application-staging.yaml`, `application-e2e.yaml`, `application-e2e-rbac.yaml` |
| **Quick run command** | `rg -n "ConsoleSecurityDefaultOff|verify-rbac-enable" data-generator-service/src/test scripts/ docs/staging-console-rbac.md AGENTS.md` |
| **Full suite command** | `powershell -NoProfile -File scripts/verify-rbac-enable.ps1 -SkipPlaywright` |
| **Estimated runtime** | ~30–90s Maven slice; +3–8 min with Playwright/Podman (wave 3, optional) |

---

## Sampling Rate

- **After every task commit:** Quick grep/static checks from Per-Task Verification Map
- **After Wave 1 (plan 16-01):** Maven RBAC slice green via `verify-rbac-enable.ps1 -SkipPlaywright`
- **After Wave 2 (plan 16-02):** Doc + AGENTS + operator-console cross-link grep bundle
- **After Wave 3 (plan 16-03):** Optional Playwright leg smoke (Podman required); `-SkipPlaywright` still exits 0 after Maven
- **Before `/gsd-verify-work`:** Full Maven verify green; profile contract grep; P0 gate unchanged
- **Max feedback latency:** 90 seconds (Maven-only path)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 16-01-01 | 01 | 1 | SEC-01 | Pitfall 4 | Default-off: `enabled=false` on base + phase7-test without RBAC override | junit | `rg -n "ConsoleSecurityDefaultOff" data-generator-service/src/test`; Maven `-Dtest=ConsoleSecurityDefaultOffIT` | ❌ W0 | ⬜ pending |
| 16-01-02 | 01 | 1 | SEC-01 | — | Base `application.yaml` has no `console-security.enabled: true` | static | `rg "console-security" data-generator-service/src/main/resources/application.yaml`; expect no match | ✅ | ⬜ pending |
| 16-01-03 | 01 | 1 | SEC-01 | — | Canonical RBAC-on IT proves deny paths (+ optional allow GET) | junit | `mvnw-jdk25.ps1 -pl data-generator-service -Dtest=ConsoleAuthorizationIntegrationIT test` | ✅ IT | ⬜ pending |
| 16-01-04 | 01 | 1 | SEC-01 | — | Filter + UDF unit tests remain green | junit | `mvnw-jdk25.ps1 -pl data-generator-service -Dtest=ConsoleAuthorizationFilterTest,ConsoleUdfAuthorizationFilterTest test` | ✅ | ⬜ pending |
| 16-01-05 | 01 | 1 | SEC-01 | — | Profile contract: e2e/distributed overlays stay RBAC-off | static | `rg -A2 "console-security:" data-generator-service/src/main/resources/application-e2e.yaml application-e2e-distributed.yaml application-distributed-staging.yaml \| Select-String "enabled: false"` | ✅ | ⬜ pending |
| 16-01-06 | 01 | 1 | SEC-01 | — | verify-rbac-enable.ps1 Maven slice exits 0 | script | `powershell -NoProfile -File scripts/verify-rbac-enable.ps1 -SkipPlaywright` | ❌ W0 | ⬜ pending |
| 16-02-01 | 02 | 2 | SEC-01 | — | Operator doc covers keys, headers, roles, profiles, verify one-liner | docs | `rg -n "console-security|X-Console-Role|verify-rbac-enable|e2e-rbac" docs/staging-console-rbac.md` | ❌ W0 | ⬜ pending |
| 16-02-02 | 02 | 2 | SEC-01 | — | AGENTS.md Commands pointer (Phase 13–15 pattern) | docs | `rg -n "verify-rbac-enable|staging-console-rbac" AGENTS.md` | ✅ | ⬜ pending |
| 16-02-03 | 02 | 2 | SEC-01 | — | operator-console-usage cross-link; no duplicate full recipe | docs | `rg -n "staging-console-rbac" docs/operator-console-usage.md`; `rg -c "verify-rbac-enable" docs/operator-console-usage.md` → 0 or 1 link only | ✅ | ⬜ pending |
| 16-02-04 | 02 | 2 | SEC-01 | Pitfall 4 | Profile contract table documents D-09 states | docs | `rg -n "application-e2e.yaml|application-staging.yaml|enabled: false|enabled: true" docs/staging-console-rbac.md` | ❌ W0 | ⬜ pending |
| 16-03-01 | 03 | 3 | SEC-01 | — | verify script optional Playwright leg uses `e2e-rbac` + `DG_E2E_RBAC=true` | script | `rg -n "e2e-rbac|DG_E2E_RBAC|rbac\.console\.spec|SkipPlaywright" scripts/verify-rbac-enable.ps1` | ❌ W0 | ⬜ pending |
| 16-03-02 | 03 | 3 | SEC-01 | — | Doc distinguishes DG_E2E_RBAC vs DG_E2E_GOVERNANCE_STAGING | docs | `rg -n "DG_E2E_RBAC|DG_E2E_GOVERNANCE_STAGING" docs/staging-console-rbac.md` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Map synced to 3-plan / 3-wave breakdown at `/gsd-plan-phase 16` (research skipped).*

---

## Wave 0 Requirements

- [ ] `data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleSecurityDefaultOffIT.java` — default-off regression (D-04)
- [ ] `scripts/verify-rbac-enable.ps1` — SEC-01 operator one-liner (Maven + optional Playwright)
- [ ] `docs/staging-console-rbac.md` — focused enable runbook (D-01)
- [ ] `AGENTS.md` — Commands/docs pointer
- [ ] `docs/operator-console-usage.md` — cross-link only (D-02)

*Existing:* `ConsoleAuthorizationIntegrationIT`, filter unit tests, Playwright `rbac.*.spec.ts`, `e2e-podman.ps1` RBAC block — reuse, do not replace.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Staging profile smoke on host | SEC-01 | Port 8080 / tarball layout variance | Start with `--spring.profiles.active=staging`; curl `/api/templates/scenarios` without header → 403; with `X-Console-Role: VIEWER` → 200 |
| Full Playwright RBAC leg | D-06 | Podman + Node required | Run `.\scripts\verify-rbac-enable.ps1` without `-SkipPlaywright`; confirm `rbac.console.spec.ts` + `rbac.ui.spec.ts` pass |
| Default e2e unchanged | D-09 | Profile sanity | Confirm `application-e2e.yaml` still has `console-security.enabled: false` after phase |

*Default merge bar remains 15-row P0; SEC-01 verify script is non-blocking (D-08, D-10).*

---

## Scope Guard Checks (D-10)

Run after phase execution to confirm non-goals held:

```powershell
# No P1/P0 matrix row for RBAC in Phase 16
rg -n "rbac-enable" .planning/test-matrix.yaml  # expect no match OR only comment referencing Phase 17

# verify-harness merge gate untouched
git diff scripts/verify-harness.ps1  # expect empty

# No default-on in base config
rg "console-security" data-generator-service/src/main/resources/application.yaml  # expect no match

# distributed-staging stays RBAC-off (Phase 15 compatibility)
rg -A2 "console-security:" data-generator-service/src/main/resources/application-distributed-staging.yaml | Select-String "enabled: false"
```

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s (Maven quick path)
- [ ] `nyquist_compliant: true` set in frontmatter after `/gsd-verify-work`

**Approval:** pending
