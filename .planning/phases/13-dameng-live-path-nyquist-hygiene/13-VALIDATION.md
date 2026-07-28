---
phase: 13
slug: dameng-live-path-nyquist-hygiene
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-28
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire) — `data-generator-calcite` module; no new framework |
| **Config file** | none — plain JUnit IT (not `@SpringBootTest`) |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=JdbcSinkSqlBuilderTests -Dsurefire.failIfNoSpecifiedTests=false -q` |
| **Full suite command** | `powershell -NoProfile -File scripts/verify-phase13-uat-dameng-live.ps1` (requires `DG_DM_IT=true` + `DG_DM_*`; exits 1 with usage otherwise per D-16) |
| **Estimated runtime** | ~30–90s unit; live IT only when Dameng host configured |

---

## Sampling Rate

- **After every task commit:** Quick run (`JdbcSinkSqlBuilderTests`) for DIAL-01 code tasks; `rg -n` doc-checks for DIAL-02
- **After every plan wave:** `scripts/verify-phase13-uat-dameng-live.ps1` (graceful non-zero without host = D-16 negative-path OK) + Nyquist frontmatter grep on 07/07.1/08 VALIDATION files
- **Before `/gsd-verify-work`:** `data-generator-calcite` module tests green; **no** `verify-harness.ps1` / P0 changes
- **Max feedback latency:** 90 seconds (unit); live IT host-dependent

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 13-01-01 | 01 | 1 | DIAL-01 | T-13-01 | Never log/print `DG_DM_PASSWORD` value on hard-FAIL | unit | `-Dtest=JdbcSinkSqlBuilderTests` | ✅ | ⬜ pending |
| 13-01-02 | 01 | 1 | DIAL-01 | T-13-02 | Hard FAIL when flag on + misconfigured (no soft skip) | IT | `-Dtest=ChunkedPipelineDamengUpsertIT` with flag on, URL unset | ❌ W0 | ⬜ pending |
| 13-01-03 | 01 | 1 | DIAL-01 | — | Upsert idempotency via `UpsertParitySupport` when host configured | IT | same + real `DG_DM_*` | ❌ W0 | ⬜ pending |
| 13-01-04 | 01 | 1 | DIAL-01 | — | UAT wrapper exits non-zero when config missing | script | `scripts/verify-phase13-uat-dameng-live.ps1` (no env) | ❌ W0 | ⬜ pending |
| 13-02-01 | 02 | 2 | DIAL-02 | — | 07/07.1/08 VALIDATION `nyquist_compliant: true` from existing evidence | docs | `rg -n "nyquist_compliant: true" .../07-VALIDATION.md .../07.1-VALIDATION.md .../08-VALIDATION.md` | ❌ W0 | ⬜ pending |
| 13-02-02 | 02 | 2 | DIAL-02 | — | Milestone audit Nyquist table synced (12 untouched) | docs | `rg -n "COMPLIANT" .planning/milestones/v2.0-MILESTONE-AUDIT.md` | ✅ (file) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Plan/task IDs provisional — planner may rename; keep requirement mapping.*

---

## Wave 0 Requirements

- [ ] `data-generator-calcite/pom.xml` — add `com.dameng:dm-jdbc` test-scope dependency
- [ ] `ChunkedPipelineDamengUpsertIT.java` — replace placeholder body (reuse `UpsertParitySupport`)
- [ ] `scripts/verify-phase13-uat-dameng-live.ps1` — new UAT wrapper (D-15/D-16)
- [ ] `.planning/milestones/v2.0-phases/07.1-.../07.1-VALIDATION.md` — create from VERIFICATION evidence
- [ ] `.planning/milestones/v2.0-phases/08-rw-streaming-upsert/08-VALIDATION.md` — create from VERIFICATION/UAT evidence

*Existing:* `07-VALIDATION.md` and milestone audit need in-place edits only. MERGE unit (`JdbcSinkSqlBuilderTests`) already covers default CI bar.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Live Dameng IT PASS on real host | DIAL-01 / D-05 | No Dameng host in default CI/sandbox | Follow recipe in `docs/template-v2-jdbc-sink-guide.md`; set `DG_DM_IT=true` + `DG_DM_*`; expect BUILD SUCCESS + upsert idempotency |

*Default merge bar remains MERGE-unit; live PASS is opt-in when host available.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s (unit path)
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
