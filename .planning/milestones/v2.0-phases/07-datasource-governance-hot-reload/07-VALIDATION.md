---
phase: 7
slug: datasource-governance-hot-reload
status: draft
nyquist_compliant: false
wave_0_complete: true
created: 2026-06-27
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire/Failsafe) + Playwright 1.49 |
| **Config file** | `data-generator-service/src/test/resources/application-phase7-test.yaml` |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=ExecutionConnectionSnapshotTests,ConnectionCatalogImplTests -q` |
| **Full suite command** | `.\mvnw-jdk25.ps1 -pl data-generator-service,data-generator-datasource -am test -q` |
| **E2E command** | `.\scripts\verify-phase7-uat-datasource-governance.ps1` (Wave 4+) |
| **Estimated runtime** | ~90s quick · ~8min full service slice · ~15min Podman E2E |

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
| 07-01-01 | 01 | 1 | DS-03/04 | T-07-01 | Catalog list payloads secret-free | unit | `mvnw -pl data-generator-datasource-api test` | ✅ | ✅ green |
| 07-01-02 | 01 | 1 | DS-03 | T-07-02 | Snapshot JSON schema round-trip | unit | `mvnw -Dtest=ExecutionConnectionSnapshotTests test` | ✅ | ✅ green |
| 07-02-01 | 02 | 2 | DS-03 | T-07-03 | Snapshot at RUNNING; in-flight isolation | IT | `mvnw -Dtest=ConnectionSnapshotIT test` | ❌ W2 | ⬜ pending |
| 07-02-02 | 02 | 2 | DS-03 | T-07-04 | DEGRADED retains last-known-good | IT | `mvnw -Dtest=HotReloadTests test` | ❌ W2 | ⬜ pending |
| 07-03-01 | 03 | 3 | DS-04 | T-07-05 | Governance blocks inline prod refs at publish/run | IT | `mvnw -Dtest=DatasourceGovernanceIT test` | ❌ W3 | ⬜ pending |
| 07-03-02 | 03 | 3 | DS-04/05 | T-07-06 | Catalog.test JDBC/Kafka/ES | IT | `mvnw -Dtest=ConnectionCatalogTestTests test` | ❌ W3 | ⬜ pending |
| 07-03-03 | 03 | 3 | DS-05 | T-07-07 | Audit events summary-only | IT | `mvnw -Dtest=ConsoleAuditControllerTest test` | ✅ | ⬜ pending |
| 07-04-01 | 04 | 4 | DS-04 | T-07-08 | Console test gate + HEALTHY/DEGRADED | unit/e2e | console-web build + manual | ❌ W4 | ⬜ pending |
| 07-05-01 | 05 | 4 | DS-03/04/05 | T-07-09 | Full governance E2E path | e2e | `verify-phase7-uat-datasource-governance.ps1` | ❌ W4 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers Phase 7 baseline:

- [x] `application-phase7-test.yaml` — IT profile with governance placeholders
- [x] `ExecutionConnectionSnapshotTests` — snapshot schema unit tests
- [x] Phase 6 Playwright helpers (`e2e/helpers/api.ts`, messaging, template-run)
- [x] Embedded Kafka/ES patterns from `data-generator-calcite` test support

Wave 2+ adds new IT classes listed above during execution.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Distributed worker snapshot-only resolve | DS-03 | Multi-JVM coordination | Run two-worker fixture; verify worker reads `connection_snapshot_json` only |
| playwright-cli snapshot regression | DS-03/04 | CLI harness optional in CI | Run `e2e/cli/` commands against Podman container; compare snapshot hashes |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers Phase 6 + 07-01 baseline
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s (quick slice)
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
