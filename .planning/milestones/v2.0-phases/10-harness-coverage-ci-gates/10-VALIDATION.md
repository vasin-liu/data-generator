---
phase: 10
slug: harness-coverage-ci-gates
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-22
---

# Phase 10 — Validation Strategy

> Per-phase validation contract. Research skipped (`workflow.research: false`); map derived from PLAN.md verify blocks.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Matrix harness (PowerShell) + JUnit 5 via Maven Surefire |
| **Config file** | `.planning/test-matrix.yaml` |
| **Quick run command** | `rg -n "v2-streaming-csv|v2-dialect-dameng|tier: P0" .planning/test-matrix.yaml` |
| **Full suite command** | `.\scripts\verify-harness.ps1` |
| **Estimated runtime** | ~10s quick · ~10–20min full harness (Testcontainers) |

---

## Sampling Rate

- **After every task commit:** Run that task's `<automated>` verify
- **After Wave 1:** Matrix `rg` checks for all 8 new ids + P0 count
- **After Wave 2:** Full `verify-harness.ps1` + summary JSON assertions + doc `rg`
- **Before `/gsd-verify-work`:** `verify-harness.ps1` green with `p0.pass=true`, `p0.total=15`
- **Max feedback latency:** 120s for matrix/doc checks; harness may exceed for containers

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | TEST-07 | — | Streaming rows registered P0 | docs/rg | `rg -n "v2-streaming-csv\|v2-streaming-json" .planning/test-matrix.yaml` | ✅ | ⬜ pending |
| 10-01-02 | 01 | 1 | TEST-07 | — | Upsert row registered P0 | docs/rg | `rg -n "v2-jdbc-upsert-pg-mysql" .planning/test-matrix.yaml` | ✅ | ⬜ pending |
| 10-01-03 | 01 | 1 | TEST-07 | — | Five dialect P0 rows; no DM IT link | docs/rg | `rg -c "tier: P0" .planning/test-matrix.yaml` | ✅ | ⬜ pending |
| 10-02-01 | 02 | 2 | TEST-08 | T-10-01 | Harness exit 0 / p0.pass | script | `.\scripts\verify-harness.ps1` | ✅ | ⬜ pending |
| 10-02-02 | 02 | 2 | TEST-08 | T-10-01 | Summary lists 8 new ids green; p0.total=15 | script | PowerShell summary assertion (see 10-02-PLAN) | ✅ | ⬜ pending |
| 10-02-03 | 02 | 2 | TEST-08 | — | harness-verify.yml still calls verify-harness.ps1 | docs/rg | `rg -n "verify-harness.ps1" .github/workflows/harness-verify.yml` | ✅ | ⬜ pending |
| 10-03-01 | 03 | 2 | TEST-07/08 | — | test-harness.md 15-row inventory | docs/rg | `rg -n "P0 rows \(15\)\|v2-streaming-csv\|v2-dialect-dameng" docs/test-harness.md` | ✅ | ⬜ pending |
| 10-03-02 | 03 | 2 | TEST-08 | — | AGENTS.md merge criteria + command | docs/rg | `rg -n "v2-streaming-csv\|v2-dialect-clickhouse\|verify-harness.ps1" AGENTS.md` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers Phase 10 — no new test framework:

- [x] `.planning/test-matrix.yaml` schema + existing 7 P0 rows
- [x] `scripts/verify-harness.ps1` + `scripts/lib/test-matrix-summary.ps1`
- [x] `.github/workflows/harness-verify.yml`
- [x] Phase 8/9 Maven test classes for `linked_tests`

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| GitHub Actions PR red on forced P0 failure | TEST-08 | Requires remote CI / intentional breakage | Optional: open PR that breaks a linked test and confirm harness-verify fails |

*All other phase behaviors have automated verification.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify
- [x] Sampling continuity satisfied
- [x] Wave 0 covers existing harness
- [x] No watch-mode flags
- [x] `nyquist_compliant: true`

**Approval:** pending (pre-execution draft 2026-07-22)
