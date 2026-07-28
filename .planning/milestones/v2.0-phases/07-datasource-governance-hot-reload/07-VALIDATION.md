---
phase: 7
slug: datasource-governance-hot-reload
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-06-27
backfilled: 2026-07-28 (Phase 13, DIAL-02)
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
>
> **Backfill note (Phase 13, DIAL-02, 2026-07-28):** Phase 7 shipped and passed goal verification on 2026-07-24 (`07-VERIFICATION.md`, status: passed, 4/4 truths). This document was originally a pre-execution draft with an unfilled Per-Task Verification Map (`nyquist_compliant: false`). It has been retroactively filled by transcribing test classes and Maven commands that already exist and were already recorded green in `07-VERIFICATION.md` and the `07-01-SUMMARY.md`..`07-05-SUMMARY.md` Verification sections. No new tests were written and no Phase 7 implementation was reopened (D-11).

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire/Failsafe) + Playwright 1.49 |
| **Config file** | `data-generator-service/src/test/resources/application-phase7-test.yaml` |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=ExecutionConnectionSnapshotTests,ConnectionCatalogImplTests -Dsurefire.failIfNoSpecifiedTests=false -q` |
| **Full suite command** | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=ConnectionCatalogTestTests,DatasourceGovernanceIT,DatasourceAuditTests,ConsoleAuditControllerTest,HotReloadTests,DataSourceConfigServiceTests -Dsurefire.failIfNoSpecifiedTests=false -q` (07-03-SUMMARY, 22 tests PASS) |
| **E2E command** | `powershell -NoProfile -File scripts/verify-phase7-uat-datasource-governance.ps1 -SkipPlaywright` |
| **Estimated runtime** | ~90s quick (2 test classes) · full governance UAT Maven slice recorded at 23 tests green in ~run-time not separately timed (07-VERIFICATION Behavioral Verification) · Podman Playwright E2E skipped by default (`-SkipPlaywright`) |

---

## Sampling Rate

- **After every task commit:** Run quick run command for touched module
- **After every plan wave:** Run full suite command for service + datasource modules
- **Before `/gsd-verify-work`:** Full suite + Phase 7 UAT scripts green
- **Max feedback latency:** 120 seconds (quick slice)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 07-01-01 | 01 | 1 | DS-03/04 | T-07-01 | Catalog list payloads secret-free | unit | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=ConnectionCatalogImplTests,ConnectionCatalogBootstrapTests -Dsurefire.failIfNoSpecifiedTests=false -q` (07-01-SUMMARY.md Task 1: "ConnectionCatalogImplTests / ConnectionCatalogBootstrapTests: PASS") | ✅ | ✅ green |
| 07-01-02 | 01 | 1 | DS-03 | T-07-02 | Snapshot JSON schema round-trip | unit | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=ExecutionConnectionSnapshotTests -Dsurefire.failIfNoSpecifiedTests=false -q` (07-01-SUMMARY.md Task 2: "ExecutionConnectionSnapshotTests: PASS (2 tests)") | ✅ | ✅ green |
| 07-02-01 | 02 | 2 | DS-03 | T-07-03 | Snapshot at RUNNING; in-flight isolation | IT | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=ConnectionSnapshotSupportTests,ConnectionSnapshotIT,HotReloadTests,ExecutionConnectionSnapshotTests -Dsurefire.failIfNoSpecifiedTests=false -q` (07-02-SUMMARY.md: "Result: PASS (9 tests)"); `07-VERIFICATION.md` Truth 1 also cites `ConnectionSnapshotIT` capture at RUNNING | ✅ | ✅ green |
| 07-02-02 | 02 | 2 | DS-03 | T-07-04 | DEGRADED retains last-known-good | IT | Same combined run as 07-02-01 (`HotReloadTests` includes `reloadFailureMarksDegradedAndServesLastKnownGoodForNewRun` per `07-VERIFICATION.md` Truth 1) | ✅ | ✅ green |
| 07-03-01 | 03 | 3 | DS-04 | T-07-05 | Governance blocks inline prod refs at publish/run | IT | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=ConnectionCatalogTestTests,DatasourceGovernanceIT,DatasourceAuditTests,ConsoleAuditControllerTest,HotReloadTests,DataSourceConfigServiceTests -Dsurefire.failIfNoSpecifiedTests=false -q` (07-03-SUMMARY.md: "Result: PASS (22 tests)"; `DatasourceGovernanceIT` = 5 tests per `07-VERIFICATION.md` Behavioral Verification) | ✅ | ✅ green |
| 07-03-02 | 03 | 3 | DS-04/05 | T-07-06 | Catalog.test JDBC/Kafka/ES | IT | Same combined run as 07-03-01 (`ConnectionCatalogTestTests` = 10 tests per `07-VERIFICATION.md` Behavioral Verification) | ✅ | ✅ green |
| 07-03-03 | 03 | 3 | DS-05 | T-07-07 | Audit events summary-only | IT | Same combined run as 07-03-01 (`DatasourceAuditTests` = 3 tests per `07-VERIFICATION.md` Behavioral Verification) | ✅ | ✅ green |
| 07-04-01 | 04 | 4 | DS-04 | T-07-08 | Console test gate + HEALTHY/DEGRADED | unit/e2e | `cd data-generator-console-web && npm run build` (07-04-SUMMARY.md: "Result: PASS (tsc + vite build)") | ✅ | ✅ green |
| 07-05-01 | 05 | 4 | DS-03/04/05 | T-07-09 | Full governance E2E path | e2e | `powershell -NoProfile -File scripts/verify-phase7-uat-datasource-governance.ps1 -SkipPlaywright` (`07-VERIFICATION.md` Behavioral Verification: "Tests run: 23, Failures: 0; BUILD SUCCESS" — DatasourceGovernanceIT 5, DatasourceAuditTests 3, ConnectionCatalogTestTests 10, HotReloadTests 3, ConnectionSnapshotIT 2) | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*07-01 Automated Commands construct the standard `-Dtest=` Maven invocation (the same pattern already used verbatim by 07-02/07-03 rows below) around the exact test class names `07-01-SUMMARY.md` records as passing; no test was invented or re-run to produce this row (D-11).*

---

## Wave 0 Requirements

Existing infrastructure covers Phase 7 baseline:

- [x] `application-phase7-test.yaml` — IT profile with governance placeholders
- [x] `ExecutionConnectionSnapshotTests` — snapshot schema unit tests
- [x] Phase 6 Playwright helpers (`e2e/helpers/api.ts`, messaging, template-run)
- [x] Embedded Kafka/ES patterns from `data-generator-calcite` test support

Wave 2+ shipped the new IT classes listed above during execution (`ConnectionSnapshotIT`, `HotReloadTests`, `DatasourceGovernanceIT`, `ConnectionCatalogTestTests`, `ConsoleAuditControllerTest`) — all present and green per `07-VERIFICATION.md`.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Distributed worker snapshot-only resolve | DS-03 | Multi-JVM coordination | Run two-worker fixture; verify worker reads `connection_snapshot_json` only |
| playwright-cli snapshot regression | DS-03/04 | CLI harness optional in CI | Run `e2e/cli/` commands against Podman container; compare snapshot hashes |
| `datasource-governance.spec.ts` conditional `test.skip` (lines 113, 257) when governance profile flags off | DS-04 | Profile-gated E2E scenario; Java ITs (`DatasourceGovernanceIT`) cover the same gates by default | Set `DG_E2E_GOVERNANCE_STAGING` and run against staging Podman profile to exercise the Playwright path (per `07-VERIFICATION.md` Anti-Patterns Found, ℹ️ Info) |
| JDBC DELETE audit assertion | DS-05 | Shares `auditService.record` path with CREATE/UPDATE but has no dedicated Maven assertion; covered by E2E + wiring instead | Not treated as a goal gap per `07-VERIFICATION.md` Test Quality Audit — accepted as-is, not resolved in this backfill (D-11) |

*All other phase behaviors have automated verification (0 human-verification items recorded — `07-VERIFICATION.md` "Human Verification Required: None").*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers Phase 6 + 07-01 baseline
- [x] No watch-mode flags
- [x] Feedback latency < 120s (quick slice)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** Backfilled 2026-07-28 (Phase 13, DIAL-02) from `07-VERIFICATION.md` (status: passed, 2026-07-24) and `07-01-SUMMARY.md`..`07-05-SUMMARY.md` Verification sections — retrospective, not pre-execution.
