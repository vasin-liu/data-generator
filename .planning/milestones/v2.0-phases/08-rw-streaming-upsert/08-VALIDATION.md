---
phase: 8
slug: rw-streaming-upsert
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-06-29
backfilled: 2026-07-28 (Phase 13, DIAL-02)
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
>
> **Backfill note (Phase 13, DIAL-02, 2026-07-28):** This VALIDATION.md did not exist during Phase 8 execution. Phase 8 shipped and passed goal verification on 2026-07-23 (`08-VERIFICATION.md`, status: passed, 58/58 truths; human UAT closed same day per `08-UAT.md`, 3/3 passed). This document is built entirely from the existing `08-VERIFICATION.md` Goal Achievement / Requirements Coverage / Automated Test Results sections and the `08-01-SUMMARY.md`..`08-12-SUMMARY.md` Self-Check sections — no new tests were written and no Phase 8 streaming/upsert implementation was reopened (D-11). Phase 8's `08-VERIFICATION.md` carries 58 observable-truth rows across 12 plan tasks; this map groups those truths by the plan task and test class that proved them (one row per plan task, ~12 rows) rather than transcribing all 58 individually, per Phase 13 Task 1 guidance.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Maven Surefire) + Testcontainers (PostgreSQL/MySQL) + Playwright 1.49 |
| **Config file** | none dedicated — plain `@SpringBootTest`/JUnit ITs in `data-generator-calcite` and `data-generator-service`; Playwright config under `data-generator-console-web/e2e/` |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=JdbcSinkSqlBuilderTests -Dsurefire.failIfNoSpecifiedTests=false -q` (dialect SQL unit matrix; later extended by Phase 9's own quick command, confirming continuity) |
| **Full suite command** | `.\mvnw-jdk25.ps1 -pl data-generator-service -am -Dtest=V2ScenarioTemplateIT,StreamingPipelineTests,... test` (recorded verbatim, including the source document's own `...` abbreviation, from `08-VERIFICATION.md` Automated Test Results: "PASS, exit 0, ~11 min") plus `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am -Dtest=CsvJsonStreamingOomIT -Dsurefire.argLine=-Xmx256m test` (OOM proof, "PASS, exit 0") |
| **E2E command** | `.\scripts\verify-phase8-uat-rw-streaming-upsert.ps1` (Playwright enabled) — `08-UAT.md`: 6 passed / 1 skipped against `dg-phase8-rw-streaming-upsert-uat:local` (52.2s) |
| **Estimated runtime** | ~60s quick slice · ~11 min Maven Phase 8 slice · Testcontainers PG/MySQL ITs ~3 min · Podman Playwright UAT ~52s (recorded) |

---

## Sampling Rate

- **After every task commit:** Run the task's own recorded unit/IT command (see Per-Task Verification Map)
- **After every plan wave:** Re-run the wave's combined test classes; Wave 4 (08-08..08-10) adds the scenario IT + OOM/Testcontainers proof; Wave 5 (08-11/08-12) adds Playwright + UAT script
- **Before `/gsd-verify-work`:** Full suite command + Podman Playwright UAT green (already recorded 2026-07-23)
- **Max feedback latency:** 120 seconds (quick slice)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|----------|-----------|-------------------|-------------|--------|
| 08-01-01 | 01 | 1 | RW-01 | `ChunkedCsvRowSource` incremental CSV reads; UTF-8 BOM strip; policy-aware `CsvSourceFactory` CHUNKED/STREAMING dispatch | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=ChunkedCsvRowSourceTests -Dsurefire.failIfNoSpecifiedTests=false -q` (08-01-SUMMARY.md Self-Check: "ChunkedCsvRowSourceTests pass; TemplateV2RunnerTests readsCsvSource* pass") | ✅ | ✅ green |
| 08-02-01 | 02 | 1 | RW-01 | `ChunkedJsonRowSource` NDJSON/array streaming; `JsonSourceFactory` CHUNKED/STREAMING dispatch mirrors CSV | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=ChunkedJsonRowSourceTests -Dsurefire.failIfNoSpecifiedTests=false -q` (08-02-SUMMARY.md Self-Check: "8 tests, 0 failures"; full `data-generator-calcite` suite 275 tests, 0 failures) | ✅ | ✅ green |
| 08-03-01 | 03 | 1 | RW-01 | `StreamingPipeline`/`ChunkedPipeline` accept sole CSV/JSON `ChunkedRowSource`; registry passes policy to Csv/Json factories; no auto-promotion IN_MEMORY→CHUNKED | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=StreamingPipelineTests,ChunkedPipelineTests -Dsurefire.failIfNoSpecifiedTests=false -q` (08-03-SUMMARY.md Self-Check: "StreamingPipelineTests: 6 tests, 0 failures; ChunkedPipelineTests: 2 tests, 0 failures") | ✅ | ✅ green |
| 08-04-01 | 04 | 2 | RW-02 | CSV/JSON sinks flush per chunk (APPEND semantics); pipelines call `closeSinks` after final chunk; Excel sinks remain non-streaming (accepted, out of scope) | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=CsvJsonStreamingSinkTests,StreamingPipelineTests -Dsurefire.failIfNoSpecifiedTests=false -q` (08-04-SUMMARY.md Self-Check: "CsvJsonStreamingSinkTests: 5 tests, 0 failures; StreamingPipelineTests: 8 tests, 0 failures"; full `data-generator-calcite -am test` exit 0) | ✅ | ✅ green |
| 08-05-01 | 05 | 2 | RW-03 | PostgreSQL `ON CONFLICT DO UPDATE` and MySQL `ON DUPLICATE KEY UPDATE` upsert SQL from `options.upsertKeys`; invalid keys fail-fast; `rowsUpserted` incremented; `postgres_copy` rejects `upsert=true` | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=JdbcSinkSqlBuilderTests,JdbcUpsertSmokeTests -Dsurefire.failIfNoSpecifiedTests=false -q` (08-05-SUMMARY.md Self-Check: "12 tests, 0 failures"; PG SQL contains `ON CONFLICT (id) DO UPDATE SET`; MySQL SQL contains `ON DUPLICATE KEY UPDATE`) | ✅ | ✅ green |
| 08-06-01 | 06 | 3 | RW-04 | Per-sink `rowsRead`/`rowsWritten`/`rowsUpserted`/`rowsSkipped` metrics; actionable errors in `RunReportVO` JSON; final-summary-only (no mid-run progress) | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=StageMetricVOTests,SinkWriteMetricTests,SinkWriteExecutorMetricsTests -Dsurefire.failIfNoSpecifiedTests=false -q` plus `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=RunReportCollectorTests,RunReportCollectorFailureTests -Dsurefire.failIfNoSpecifiedTests=false -q` (08-06-SUMMARY.md Self-Check: "0 failures" across all five classes; `npm run verify:unit` exit 0 for Job center UI columns) | ✅ | ✅ green |
| 08-07-01 | 07 | 3 | RW-03/RW-04 | `upsert=true` requires valid `upsertKeys` at publish/run (fail-fast); IN_MEMORY + large-file warn-only at publish; console executionPolicy mode hints | unit + frontend build | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=TemplateV2ValidatorTests -Dsurefire.failIfNoSpecifiedTests=false -q` then `cd data-generator-console-web; npm run verify:unit` (08-07-SUMMARY.md Self-Check: "TemplateV2ValidatorTests: 6 tests, 0 failures"; "npm run verify:unit (console tsc): exit 0") | ✅ | ✅ green |
| 08-08-01 | 08 | 4 | RW-01/RW-02/RW-03/RW-04 | GF-F (streaming) and GF-G (upsert) scenario YAMLs execute green end-to-end via `V2ScenarioTemplateIT`; `scenario-e-partial-sink` asserts `errorSample` | IT | `.\mvnw-jdk25.ps1 -pl data-generator-service -am test "-Dtest=V2ScenarioTemplateIT" "-Dsurefire.failIfNoSpecifiedTests=false"` (08-08-SUMMARY.md Self-Check: "exit 0 (~5 min); all 12 scenario resources load, validate, and execute"; `08-VERIFICATION.md` Truth 32: "Maven slice: `V2ScenarioTemplateIT` exit 0 (2026-06-29)") | ✅ | ✅ green |
| 08-09-01 | 09 | 4 | RW-01/RW-02/RW-03 | OOM proof at `-Xmx256m` / 10 MB bar (CSV/NDJSON streaming stays under heap limit); Testcontainers PG/MySQL upsert idempotent re-run | IT | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=CsvJsonStreamingOomIT -Dsurefire.argLine=-Xmx256m -Dsurefire.failIfNoSpecifiedTests=false -q` plus `-Dtest=ChunkedPipelinePostgresUpsertTests,ChunkedPipelineMySqlUpsertTests` (08-09-SUMMARY.md Self-Check: "`CsvJsonStreamingOomIT`: 3 tests, 0 failures (~49s)"; "`ChunkedPipelinePostgresUpsertTests,ChunkedPipelineMySqlUpsertTests`: 2 tests, 0 failures with Docker (~3 min)"; Docker-gated via `@EnabledIf`) | ✅ | ✅ green |
| 08-10-01 | 10 | 4 | RW-01/RW-02/RW-03 | `StreamingPipelineTests`/`ChunkedPipelineTests` CSV/JSON batch streaming + per-chunk SQL regression; `JdbcSinkSqlBuilderTests` upsert matrix ≥6 methods; multi-source/cross-chunk shapes rejected | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=StreamingPipelineTests,ChunkedPipelineTests,JdbcSinkSqlBuilderTests -Dsurefire.failIfNoSpecifiedTests=false -q` (08-10-SUMMARY.md Self-Check: "14 tests, 0 failures"; "JdbcSinkSqlBuilderTests has 9 upsert-related methods (≥6 required)") | ✅ | ✅ green |
| 08-11-01 | 11 | 5 | RW-01/RW-02/RW-03/RW-04 | ≥6 Playwright D-23 scenarios (streaming + upsert) plus Job center UI sink-metrics smoke | e2e | `cd data-generator-console-web; npm run e2e:phase8-rw-streaming-upsert` against Podman (08-11-SUMMARY.md records the spec + package.json wiring; human-closed run per `08-UAT.md`: "6 passed, 1 skipped (52.2s)" — see Manual-Only Verifications for the one accepted skip) | ✅ | ✅ green (human UAT closed 2026-07-23) |
| 08-12-01 | 12 | 5 | RW-01/RW-02/RW-03/RW-04 | UAT wrapper script mirrors Phase 7 pattern; operator docs (JDBC sink upsert examples, streaming CSV/JSON guide); AGENTS.md verify command; ROADMAP lists 08-01..08-12 | script + docs | `powershell -NoProfile -File scripts/verify-phase8-uat-rw-streaming-upsert.ps1 -SkipPlaywright` (08-12-SUMMARY.md Self-Check: "exits 0"; "JDBC sink guide contains `upsertKeys` examples for postgres and mysql"; "AGENTS.md lists Phase 8 verify command") | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*The 12 rows above group `08-VERIFICATION.md`'s 58 observable-truth rows by the plan task and real test class that proved them (Requirements Coverage + Automated Test Results as the join), per Phase 13 Task 1 guidance — this is a grouped transcription, not a 1:1 truth-to-row map.*

---

## Wave 0 Requirements

Phase 8 built on the existing Template V2 Calcite runtime baseline — no greenfield harness install:

- [x] `EffectiveExecutionPolicy` IN_MEMORY/STREAMING/CHUNKED execution modes — existing (Phase 6/7), extended with file-source `sourceChunkSize`/`sinkBatchSize` defaults in 08-01/08-04
- [x] `RowSource`/`RowSink` abstractions and `TemplateV2RuntimeRegistry` factory dispatch — existing, extended for policy-aware CSV/JSON factories in 08-01..08-03
- [x] `JdbcBulkWriteExecutor` base insert/bulk-write path — existing, extended with upsert batch path in 08-05
- [x] `RunReportCollector` / `StageMetricVO` base run-report metrics — existing, extended with `rowsUpserted`/`rowsSkipped` in 08-06
- [x] `scenario-e-*` V2 scenario YAML pattern and `V2ScenarioTemplateIT` harness — existing (pre-Phase 8), extended with `scenario-f-*`/`scenario-g-*` in 08-08
- [x] Console-web Playwright + `e2e/helpers` from prior phases — existing, extended with `rw-streaming-upsert.spec.ts` in 08-11
- [x] `scripts/verify-phase7-uat-datasource-governance.ps1` pattern — existing, mirrored for `verify-phase8-uat-rw-streaming-upsert.ps1` in 08-12

No Wave 2+ greenfield infrastructure gap — every plan extends an existing baseline component rather than introducing a new framework.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Playwright D-23 #2 PostgreSQL upsert scenario | RW-03 | H2 e2e profile lacks `ON CONFLICT` support (W-01); accepted limit, not resolved in this backfill (D-11) | Covered instead by `ChunkedPipelinePostgresUpsertTests` Testcontainers idempotent re-run in the Maven slice (08-09). Per `08-UAT.md`: "Skipped: D-23 #2 PostgreSQL upsert — H2 e2e profile lacks ON CONFLICT (W-01); covered by `ChunkedPipelinePostgresUpsertTests` Testcontainers." To exercise the real Playwright PG path, run against a datasource profile with genuine PostgreSQL instead of the H2 e2e default |
| `CsvJsonStreamingOomIT` large-payload stdout logging | RW-01 | Recorded observation in `v2.0-MILESTONE-AUDIT.md` tech debt ("dumps large row payloads to stdout — floods UAT logs"); accepted as-is, not fixed in this backfill per D-11/RESEARCH Pitfall 3 | Not required for phase close; a future logging-hygiene pass could redact/truncate large row payloads in this IT's assertion failure output |
| Operator smoke on real PG/MySQL datasources (non-Testcontainers) | RW-03/RW-04 | `08-VERIFICATION.md` Human Verification #2 — production-like connectivity beyond embedded/Testcontainers is an operator-environment concern | Configure production-like PostgreSQL/MySQL datasources in Console; run `scenario-g-upsert-*` templates twice and confirm `rowsUpserted > 0` on the second run (already ✓ PASSED 2026-07-23 per `08-VERIFICATION.md`, carried here as the accepted manual-smoke record) |

*`08-VERIFICATION.md` "Human Verification Required" items #1–#3 were all closed 2026-07-23 (Playwright UAT, PG/MySQL smoke, REQUIREMENTS.md checkbox update); the first two are carried above as accepted-limit / operator-smoke records rather than resolved further, per D-11.*

---

## Validation Sign-Off

- [x] All tasks have an `<automated>`-equivalent verify (unit/IT/script) or Wave 0 dependency
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 fully reuses the existing Template V2 runtime baseline
- [x] No watch-mode flags
- [x] Feedback latency < 120s (quick slice)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** Backfilled 2026-07-28 (Phase 13, DIAL-02) from `08-VERIFICATION.md` (status: passed, 2026-07-23) and `08-01-SUMMARY.md`..`08-12-SUMMARY.md` Self-Check sections, plus `08-UAT.md` — retrospective, not pre-execution. This is a new file; Phase 8 had no VALIDATION.md before this backfill.
