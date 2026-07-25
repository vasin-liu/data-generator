---
phase: 9
slug: jdbc-dialect-expansion
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-21
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Generated without RESEARCH.md (`workflow.research: false`); map derived from PLAN.md verify blocks.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire) + Playwright 1.49 |
| **Config file** | `data-generator-service/src/test/resources/application-phase7-test.yaml` (service ITs); calcite module Testcontainers for PG/CK |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=JdbcSinkSqlBuilderTests -Dsurefire.failIfNoSpecifiedTests=false -q` |
| **Full suite command** | `.\scripts\verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright` |
| **E2E command** | `.\scripts\verify-phase9-uat-jdbc-dialect.ps1` (Playwright enabled) |
| **Estimated runtime** | ~60s quick · ~8–12min Maven dialect slice · ~15min Podman E2E |

---

## Sampling Rate

- **After every task commit:** Run the task's `<automated>` verify command
- **After every plan wave:** Run full suite command (`verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright`) once Wave 3+ artifacts exist; Waves 1–2 use module-scoped `-Dtest=` lists from plans
- **Before `/gsd-verify-work`:** Full suite + optional Playwright green
- **Max feedback latency:** 120 seconds (quick slice)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 09-01-01 | 01 | 1 | RW-05 | T-09-01 | Dialect upsert SQL fail-fast; no silent ignore | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=JdbcSinkSqlBuilderTests -Dsurefire.failIfNoSpecifiedTests=false -q` | ✅ | ⬜ pending |
| 09-01-02 | 01 | 1 | RW-05 | T-09-02 | Publish+run dual fail-fast for CK/generic upsert | unit | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=TemplateV2ValidatorTests,JdbcSinkSqlBuilderTests -Dsurefire.failIfNoSpecifiedTests=false -q` | ✅ | ⬜ pending |
| 09-02-01 | 02 | 1 | RW-06 | T-09-03 | Presets complete; bundled drivers; no secrets in catalog DTO | unit | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=JdbcDriverPresetCatalogTests,ConsoleDataSourceControllerTest -Dsurefire.failIfNoSpecifiedTests=false -q` | ✅ | ⬜ pending |
| 09-02-02 | 02 | 1 | RW-06 | T-09-04 | Connectivity summaries omit password/full JDBC URL | unit + frontend build | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=ConnectionCatalogTestTests -Dsurefire.failIfNoSpecifiedTests=false -q` then `cd data-generator-console-web; npm run build` | ✅ / ❌ W1 | ⬜ pending |
| 09-03-01 | 03 | 2 | RW-05 | T-09-05 | PG/KB/HG upsert path (PG-proxy for KB/HG) | IT | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=ChunkedPipelineKingbaseDialectTests,ChunkedPipelinePostgresUpsertTests -Dsurefire.failIfNoSpecifiedTests=false -q` | ❌ W2 | ⬜ pending |
| 09-03-02 | 03 | 2 | RW-05 | T-09-06 | CK insert/bulk; CK upsert reject; optional DM IT gated | IT/unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=ClickHouseInsertBulkWriterIntegrationTests,JdbcSinkSqlBuilderTests -Dsurefire.failIfNoSpecifiedTests=false -q` | ✅ / ❌ W2 | ⬜ pending |
| 09-04-01 | 04 | 3 | RW-05/06 | T-09-07 | UAT Maven slice mirrors 09-01..09-03 tests | script | `powershell -NoProfile -File scripts/verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright` | ❌ W3 | ⬜ pending |
| 09-04-02 | 04 | 3 | RW-06 | T-09-08 | One Playwright preset→form→save path | e2e | `cd data-generator-console-web; npm run build; npm run e2e:phase9-jdbc-dialect -- --list` (full run via UAT script without `-SkipPlaywright`) | ❌ W3 | ⬜ pending |
| 09-05-01 | 05 | 4 | RW-05 | T-09-09 | Operator docs list dialect limits (no silent failures) | docs | `rg -n "dameng|kingbase|highgo" docs/template-v2-jdbc-sink-guide.md` | ✅ | ⬜ pending |
| 09-05-02 | 05 | 4 | RW-06 | — | AGENTS.md + ROADMAP list verify script | docs | `rg -n "verify-phase9-uat-jdbc-dialect" AGENTS.md .planning/ROADMAP.md` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers Phase 9 baseline — no greenfield harness install:

- [x] `JdbcSinkSqlBuilderTests` — dialect SQL unit matrix (extend in 09-01)
- [x] `TemplateV2ValidatorTests` — publish validation (extend in 09-01)
- [x] `JdbcDriverPresetCatalog` / `ConnectionCatalogTestTests` — presets + connectivity (extend in 09-02)
- [x] `UpsertParitySupport` / PG & ClickHouse Testcontainers ITs from Phase 8 (extend in 09-03)
- [x] Phase 8 UAT script pattern `scripts/verify-phase8-uat-rw-streaming-upsert.ps1` (mirror in 09-04)
- [x] Console-web Playwright + `e2e/helpers` from prior phases

Wave 2+ adds Kingbase dialect IT class and Phase 9 UAT/Playwright assets during execution.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Live Dameng MERGE against real DM image | RW-05 | Licensed image; gated IT skipped by default (D-14) | Enable with documented flag (e.g. `-Ddm.it=true`); run optional DM IT when host/image available |
| Live Kingbase/HighGo engines | RW-05 | PG-proxy + dialect-mapping tests fulfill success criterion (D-15) | Optional manual smoke against real KB/HG if available; not required for phase close |

*All other phase behaviors have automated verification.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers Phase 8 + existing unit/IT baseline
- [x] No watch-mode flags
- [x] Feedback latency < 120s (quick slice)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending (pre-execution draft 2026-07-21)
