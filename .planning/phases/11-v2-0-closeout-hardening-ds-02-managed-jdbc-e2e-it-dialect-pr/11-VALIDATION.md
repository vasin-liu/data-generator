---
phase: 11
slug: v2-0-closeout-hardening-ds-02-managed-jdbc-e2e-it-dialect-pr
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-24
planned: 2026-07-25
plans:
  - 11-01 (wave 1) — ManagedJdbcCatalogSinkE2eIT → T-11-01
  - 11-02 (wave 2) — Playwright kingbase8 + verify-phase11 script → T-11-02..T-11-05
  - 11-03 (wave 3) — AGENTS + audit flows #1/#8 → T-11-06..T-11-07
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire) + Playwright 1.49 + PowerShell UAT scripts |
| **Config file** | `data-generator-service/src/test/resources/application-phase7-test.yaml` |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-service -am "-Dtest=ManagedJdbcCatalogSinkE2eIT" -Dsurefire.failIfNoSpecifiedTests=false test` |
| **Full suite command** | `.\scripts\verify-phase11-uat-closeout-hardening.ps1 -SkipPlaywright` |
| **Estimated runtime** | ~120–300 seconds (Maven slice); Playwright optional / longer |

---

## Sampling Rate

- **After every task commit:** Run the task's automated command from the map below
- **After every plan wave:** Run `.\scripts\verify-phase11-uat-closeout-hardening.ps1 -SkipPlaywright` (once script exists)
- **Before `/gsd-verify-work`:** Full suite must be green (Maven + docs rg; Playwright when Podman available)
- **Max feedback latency:** 300 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| T-11-01 | 01 | 1 | DS-02 | — | Managed sink uses catalog id only (no secrets in assert path) | maven | `.\mvnw-jdk25.ps1 -pl data-generator-service -am "-Dtest=ManagedJdbcCatalogSinkE2eIT" -Dsurefire.failIfNoSpecifiedTests=false test` | ❌ W0 | ⬜ pending |
| T-11-02 | 02 | 2 | RW-05 | — | Kingbase connectivity failure actionable without secrets | maven | `.\mvnw-jdk25.ps1 -pl data-generator-service -am "-Dtest=ConnectionCatalogTestTests" -Dsurefire.failIfNoSpecifiedTests=false test` | ✅ | ⬜ pending |
| T-11-03 | 02 | 2 | RW-06 | — | Dialect-correct upsert via existing Kingbase IT | maven | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am "-Dtest=ChunkedPipelineKingbaseDialectTests" -Dsurefire.failIfNoSpecifiedTests=false test` | ✅ | ⬜ pending |
| T-11-04 | 02 | 2 | RW-05, RW-06 | — | Phase 11 UAT script exit 0 with -SkipPlaywright | script | `.\scripts\verify-phase11-uat-closeout-hardening.ps1 -SkipPlaywright` | ❌ W0 | ⬜ pending |
| T-11-05 | 02 | 2 | RW-05 | — | Playwright postgresql16 + kingbase8 preset save | playwright | `npm run e2e:phase9-jdbc-dialect` (or script without -SkipPlaywright) | ✅ (spec) / ❌ kingbase8 | ⬜ pending |
| T-11-06 | 03 | 3 | SC3 / D-18 | — | AGENTS.md lists phase11 UAT command | docs | `rg -n "verify-phase11-uat-closeout-hardening" AGENTS.md` | ❌ W0 | ⬜ pending |
| T-11-07 | 03 | 3 | SC3 / D-20 | — | Audit flows #1/#8 disposition updated | docs | PowerShell `rg` evidence chain; residual PARTIAL wording exits 1 (11-03 Task 2 `<automated>`) | ✅ (file) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `ManagedJdbcCatalogSinkE2eIT` (or planner-chosen name) — new managed JDBC sink E2E IT
- [ ] `scripts/verify-phase11-uat-closeout-hardening.ps1` — phase UAT script with `-SkipPlaywright`
- [ ] Playwright `kingbase8` coverage in `jdbc-dialect-preset.spec.ts`

*Existing infrastructure covers ConnectionCatalogTestTests, ChunkedPipelineKingbaseDialectTests, and `e2e:phase9-jdbc-dialect` npm script.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Full Playwright against Podman console | RW-05 | Optional when Podman unavailable in default CI | Run verify script without `-SkipPlaywright` when Podman local stack is up |

*All core SC1–SC3 proofs have automated verification; Playwright full path is optional UAT.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 300s
- [x] `nyquist_compliant: true` set in frontmatter
- [x] 11-03 Task 2 docs verify is gating (positive `rg` chain; residual PARTIAL flow wording fails)

**Approval:** approved 2026-07-25 (revision 1 — Task 2 verify Nyquist-gated)
