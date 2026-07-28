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
| 13-01-01 | 01 | 1 | DIAL-01 | T-13-01, T-13-SC | Env-read failure names the missing variable, never its value; pre-approved BOM dependency only | build | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test-compile` | ❌ W0 | ⬜ pending |
| 13-01-02 | 01 | 1 | DIAL-01 | T-13-02 | Hard FAIL when flag on + misconfigured (Surefire failure, not skip) | IT (inverted) | `-Dtest=ChunkedPipelineDamengUpsertIT` with `DG_DM_IT=true`, URL unset → expect non-zero exit | ❌ W0 | ⬜ pending |
| 13-02-01 | 02 | 2 | DIAL-01 | T-13-08, T-13-01 | Wrapper exits 1 with usage when unconfigured; echoes variable names only | script (inverted) | `powershell -NoProfile -File scripts/verify-phase13-uat-dameng-live.ps1` with no env → expect exit 1 | ❌ W0 | ⬜ pending |
| 13-02-02 | 02 | 2 | DIAL-01 | T-13-03, T-13-04, T-13-09 | Placeholder credentials + never-commit warning; P0 gate untouched | docs | `rg -n "DG_DM_JDBC_URL\|buildsDamengMergeInto" docs/template-v2-jdbc-sink-guide.md` + `rg -n "verify-phase13-uat-dameng-live" AGENTS.md` | ❌ W0 | ⬜ pending |
| 13-03-01 | 03 | 1 | DIAL-02 | T-13-05, T-13-10, T-13-11 | 07 compliance claimed only from existing green evidence; Phase 12 untouched | docs | `rg -n "nyquist_compliant: true" .../07-VALIDATION.md` | ✅ (file) | ⬜ pending |
| 13-03-02 | 03 | 1 | DIAL-02 | T-13-05, T-13-10 | 07.1 VALIDATION created from its own VERIFICATION record | docs | `rg -n "nyquist_compliant: true" .../07.1-VALIDATION.md` | ❌ W0 | ⬜ pending |
| 13-04-01 | 04 | 2 | DIAL-02 | T-13-05, T-13-10 | 08 map grouped by task; accepted limits carried, not resolved | docs | `rg -n "nyquist_compliant: true" .../08-VALIDATION.md` | ❌ W0 | ⬜ pending |
| 13-04-02 | 04 | 2 | DIAL-02 | T-13-13, T-13-04, T-13-11 | Audit claims bound to real flags; P0 and Phase 12 untouched | docs | `rg -n "COMPLIANT\|partial_phases: \[\]\|missing_phases: \[\]" .planning/milestones/v2.0-MILESTONE-AUDIT.md` | ✅ (file) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Map synced to the 4-plan / 2-wave breakdown created at `/gsd-plan-phase 13`.*

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
