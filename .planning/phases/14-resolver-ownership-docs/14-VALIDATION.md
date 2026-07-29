---
phase: 14
slug: resolver-ownership-docs
status: draft
nyquist_compliant: false
wave_0_complete: true
created: 2026-07-29
---

# Phase 14 — Validation Strategy

> Per-phase validation contract for a **docs-only** phase (RES-01). No product tests are added; verification is `rg`/manual doc review.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | none new — doc greps + optional compile if Javadoc cross-link touched |
| **Config file** | none |
| **Quick run command** | `rg -n "JdbcCatalogResolver|DefaultRuntimeJdbcEndpointResolver|RuntimeJdbcEndpointResolver|NoopRuntimeJdbcEndpointResolver" docs/jdbc-resolver-ownership.md` |
| **Full suite command** | same greps + AGENTS pointer grep + P0 scope guard (see Per-Task Verification Map) |
| **Estimated runtime** | &lt;10s |

---

## Sampling Rate

- **After every task commit:** `rg -n` acceptance greps on the edited doc paths
- **After every plan wave:** Re-run inventory spot-check — each table row's path must still exist on disk (`Test-Path` or `rg -l`)
- **Before `/gsd-verify-work`:** Confirm `.planning/test-matrix.yaml`, `scripts/verify-harness.ps1`, and `.github/workflows/**` unchanged
- **Max feedback latency:** 10 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 14-01-01 | 01 | 1 | RES-01 | T-14-01 | Inventory rows cite real paths from fresh `rg`; no fabricated production `JdbcCatalogResolver` callers | docs | `rg -n "Execute-path production|Catalog-side|Tests and stubs" docs/jdbc-resolver-ownership.md` | ❌ W0 | ⬜ pending |
| 14-01-02 | 01 | 1 | RES-01 | T-14-02, T-14-03 | Ownership narrative covers snap:, coexistence, RES-02 deferral, non-goals; no merge/implement language | docs | `rg -n "snap:|RES-02|Non-goals|coexist" docs/jdbc-resolver-ownership.md` | ❌ W0 | ⬜ pending |
| 14-02-01 | 02 | 2 | RES-01 | T-14-04 | AGENTS.md pointer matches Phase 13 comment+path pattern; P0 gate untouched | docs | `rg -n "jdbc-resolver-ownership" AGENTS.md` | ❌ W0 | ⬜ pending |
| 14-02-02 | 02 | 2 | RES-01 | T-14-05 | Optional cross-link and Javadoc `@see` only; no behavior/wiring edits beyond doc pointer | docs | `git diff --name-only HEAD -- .planning/test-matrix.yaml scripts/verify-harness.ps1` → empty | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Map synced to the 2-plan / 2-wave breakdown created at `/gsd-plan-phase 14`.*

---

## Wave 0 Requirements

- [x] Canonical resolver classes exist on `main` (`JdbcCatalogResolver`, `DefaultRuntimeJdbcEndpointResolver`, `RuntimeJdbcEndpointResolver`, `NoopRuntimeJdbcEndpointResolver`)
- [x] Phase 12 HTTP execute-path IT (`ManagedJdbcCatalogHttpExecuteIT`) and DS-03 IT (`JdbcSnapshotExecutePathIT`) exist as inventory evidence sources

*No new Wave 0 artifacts required — docs-only phase.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Inventory accuracy vs `main` | RES-01 / D-04 | Doc tables cannot be fully proven by grep alone | After writing tables, run `rg -l JdbcCatalogResolver --glob '*.java'` and confirm production caller count matches the doc's honest statement (expect: class + unit test only) |
| Ownership narrative clarity | RES-01 / D-06 | Qualitative maintainer readability | Read ownership section; confirm a new contributor knows which resolver to extend for execute-path vs catalog-module work without opening both implementations |
| HTTP run-path story | RES-01 / D-05 | Narrative cross-references Phase 12 proof | Confirm doc names `/task/run` spine and cites `ManagedJdbcCatalogHttpExecuteIT` as evidence, not as a third resolver |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Manual-Only coverage
- [ ] `nyquist_compliant: true` set only after both waves complete and manual inventory spot-check passes
- [ ] P0 / test-matrix / verify-harness confirmed untouched
- [ ] No product behavior or Spring wiring changes landed
