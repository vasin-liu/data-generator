# Phase 8: RW Streaming & Upsert - Context

**Gathered:** 2026-06-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver **RW-01**, **RW-02**, **RW-03**, and **RW-04**: streaming/chunked CSV and JSON source and sink I/O so large files do not fully materialize in heap; JDBC sink upsert/merge for PostgreSQL and MySQL with dialect-correct SQL; run reports with per-sink row counts and actionable errors surfaced in JSON and Job center UI.

**In scope:** Explicit `CHUNKED` / `STREAMING` execution for CSV/JSON sources and sinks; NDJSON + JSON array streaming reads; UTF-8 CSV with optional BOM; per-chunk transform-through-SQL and per-chunk sink flush; `sink.options.upsert` + `upsertKeys` for PG/MySQL; extended `RunReportVO` sink metrics; Console form hints and publish validation; Playwright E2E (5+ scenarios); embedded ITs including `-Xmx256m` OOM proof and Testcontainers PG/MySQL upsert; operator docs (jdbc-sink guide update + new streaming CSV/JSON guide).

**Out of scope (later phases):** Excel streaming; ClickHouse / Dameng / Kingbase / HighGo upsert (Phase 9); mid-run progress UI; auto mode selection for large files (must be explicit); harness P0 matrix rows for RW paths (Phase 10); new file encodings beyond UTF-8; cross-chunk streaming SQL semantics.

**Depends on:** Phase 6 connection resolution stable; Phase 7 governance/hot-reload does not block this work.

</domain>

<decisions>
## Implementation Decisions

### Execution Mode (RW-01 / RW-02)
- **D-01:** Large CSV/JSON pipelines require **explicit** `executionPolicy.mode: CHUNKED` or `STREAMING` — no automatic mode promotion; `IN_MEMORY` behavior for small datasets stays unchanged.
- **D-02:** **Both `CHUNKED` and `STREAMING` are first-class** for CSV/JSON source and sink paths (operators choose per template; document trade-offs in streaming guide).
- **D-03:** Default chunk size **1000 rows** when `sourceChunkSize` / sink batch size not specified (align with existing JDBC streaming defaults where applicable).
- **D-04:** Transforms run **batch-through-SQL per chunk** — SQL evaluates within each read chunk; **no cross-chunk streaming SQL** (joins/aggregates requiring full materialization remain `IN_MEMORY`-only).
- **D-05:** **IN_MEMORY + large file:** emit **warning only** at draft save / publish when estimated or declared file size exceeds fixture bar — **no hard block** (zero-break compat for existing templates).
- **D-06:** Documented **OOM fixture bar:** **10 MB file / ~100k rows** must complete without OOM when using `CHUNKED` or `STREAMING`; prove with IT at **`-Xmx256m`**.
- **D-07:** Extend existing **`scenario-e-streaming-jdbc.yaml` / `V2ScenarioTemplateIT`** pattern for CSV/JSON streaming scenarios (do not invent parallel harness style).

### CSV/JSON Shape (RW-01 / RW-02)
- **D-08:** **JSON sources** support **NDJSON (line-delimited)** and **JSON array** via streaming element parser (detect or configure format — planner/researcher picks minimal YAML knob).
- **D-09:** **CSV sources** support **UTF-8 with optional BOM** only — no new encodings in Phase 8.
- **D-10:** **CSV/JSON sinks** flush **per chunk** to disk (streaming writer mode); path semantics unchanged from current file sinks.
- **D-11:** **Excel** sources/sinks remain **out of scope** for streaming in Phase 8.

### JDBC Upsert (RW-03)
- **D-12:** Upsert configured via sink YAML: `options.upsert: true` and `options.upsertKeys: [col1, col2, ...]`.
- **D-13:** **PostgreSQL and MySQL only** in Phase 8 — dialect-specific `ON CONFLICT` / `ON DUPLICATE KEY UPDATE` (or equivalent) SQL generation.
- **D-14:** **Missing or invalid `upsertKeys`** (unknown columns, empty list when `upsert: true`) → **fail-fast at publish and at run** (same severity as governance blocks).
- **D-15:** Idempotent re-run: second run **updates existing keys** instead of inserting duplicates (verified by IT + Playwright).

### Run Report (RW-04)
- **D-16:** Per sink, report **`rowsRead`**, **`rowsWritten`**, **`rowsUpserted`**, **`rowsSkipped`**, and **actionable errors** (structured, not opaque stack traces only).
- **D-17:** Errors appear in **`RunReportVO` JSON** and **Job center UI** detail view.
- **D-18:** **No mid-run progress** streaming to UI in Phase 8 — **final summary only** after terminal job state.

### Console UX
- **D-19:** Template editor adds **form hints** for `executionPolicy.mode` (when to use IN_MEMORY vs CHUNKED vs STREAMING) and **publish-time validation** for `upsert: true` requiring non-empty `upsertKeys`.
- **D-20:** **IN_MEMORY + large-file warning** surfaced in Console at draft/publish (toast or inline warning — match existing validator warning patterns).

### Backward Compatibility
- **D-21:** **Zero behavior change** for existing small-file **`IN_MEMORY`** CSV/JSON templates — no migration required.
- **D-22:** Warn-only boundary for large files on `IN_MEMORY` — operators opt in to `CHUNKED`/`STREAMING` explicitly.

### Verification
- **D-23:** **Playwright E2E required** (not embedded-only) — minimum **5 scenarios:**
  1. Large CSV **`CHUNKED`** run → Job **SUCCESS**
  2. **PostgreSQL upsert** idempotent re-run (no duplicate rows)
  3. **Failed job** shows **actionable errors** in Job center report
  4. **JSON NDJSON** streaming run success
  5. **MySQL upsert** idempotent re-run
  6. **`IN_MEMORY` + large file** → **warn toast** at publish (or equivalent UI signal)
- **D-24:** **OOM proof:** Maven IT with **`-Xmx256m`** + **10 MB** fixture under `CHUNKED`/`STREAMING`.
- **D-25:** **Upsert ITs:** H2 smoke (basic path) + **Testcontainers PostgreSQL and MySQL** for dialect-correct upsert SQL.

### Operator Documentation
- **D-26:** **Update** `docs/template-v2-jdbc-sink-guide.md` with upsert options and PG/MySQL examples.
- **D-27:** **Add** operator guide for **streaming CSV/JSON** (extend or companion to `docs/template-v2-streaming-execution-guide.md`).
- **D-28:** **Update `AGENTS.md`** with Phase 8 verify script entry when UAT script lands (planner adds `verify-phase8-uat-*.ps1` pattern).

### Claude's Discretion
- JSON format detection vs explicit `format: ndjson | array` YAML field (as long as D-08 holds).
- Whether CSV/JSON streaming reuses `StreamingPipeline` vs `ChunkedPipeline` internally (as long as D-01–D-04 hold).
- Exact `RunReportVO` field names if backward-compatible aliases needed for existing console types.
- Playwright spec file naming and Podman fixture layout (mirror Phase 6/7 UAT scripts).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & Roadmap
- `.planning/ROADMAP.md` — Phase 8 goal, success criteria, RW-01..RW-04 mapping
- `.planning/REQUIREMENTS.md` — RW-01, RW-02, RW-03, RW-04 full requirement text
- `.planning/PROJECT.md` — v2.0 milestone, reader/writer gap closure scope
- `.planning/phases/06-datasource-platform-core/06-CONTEXT.md` — connection catalog baseline
- `.planning/phases/07-datasource-governance-hot-reload/07-CONTEXT.md` — deferred streaming/upsert to Phase 8

### Gap & Status Docs
- `.planning/codebase/CONCERNS.md` — CSV/JSON streaming gaps, JDBC upsert absence
- `docs/calcite-implementation-status.md` — engine ceilings, streaming limitations
- `docs/superpowers/specs/2026-06-07-v1-to-v2-native-gap-matrix.md` — Postgres/MySQL upsert **Partial** status

### Execution Policy & Scenarios
- `docs/template-v2-streaming-execution-guide.md` — STREAMING vs CHUNKED vs IN_MEMORY (extend for CSV/JSON)
- `docs/template-v2-jdbc-chunked-execution-guide.md` — CHUNKED JDBC patterns
- `docs/template-v2-jdbc-sink-guide.md` — JDBC sink options (update for upsert)
- `data-generator-service/src/main/resources/template/v2-scenarios/scenario-e-streaming-jdbc.yaml` — STREAMING reference template
- `docs/template-v2-scenario-template-catalog.md` — GF-E / CV-02 catalog entry

### Existing Implementation (extend, do not rewrite)
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/StreamingPipeline.java` — JDBC streaming runtime
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/ChunkedPipeline.java` — chunked execution
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/StreamingPipelineTests.java` — streaming tests baseline
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerTests.java` — CSV/JSON source tests (`readsCsvSource*`, `readsJsonSource*`)
- `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java` — execution policy + publish validation hooks
- `data-generator-service/src/main/java/org/gensokyo/data/task/RunReportCollector.java` — run report aggregation
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/RunReportVO.java` — report model (extend sink metrics)
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/ExecutionPolicyVO.java` — mode/chunk fields
- `data-generator-service/src/test/java/org/gensokyo/data/template/V2ScenarioTemplateIT.java` — scenario IT harness

### Console & E2E
- `data-generator-console-web/src/api/types.ts` — `RunReport` TypeScript mirror
- `data-generator-console-web/e2e/helpers/template-run.ts` — template run helpers
- `data-generator-console-web/e2e/specs/datasource-v2-template-run.spec.ts` — existing run/report assertions
- `docs/testing-embedded-components.md` — embedded-first + Testcontainers patterns
- `scripts/verify-phase6-uat-*.ps1`, `scripts/verify-phase7-uat-datasource-governance.ps1` — UAT script patterns to mirror

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `StreamingPipeline` / `ChunkedPipeline` — JDBC streaming already shipped; Phase 8 wires CSV/JSON row sources/sinks into same execution-policy dispatch.
- `TemplateV2Validator` — already validates `IN_MEMORY` / `CHUNKED` / `STREAMING` and emits CHUNKED/STREAMING warnings; extend for upsertKeys and large-file IN_MEMORY warn.
- `TemplateV2RunnerTests` — CSV/JSON with injected parsers; pattern for streaming adapter tests.
- `RunReportCollector` + `RunReportVO` — existing `rowsWritten`; extend with upsert/skipped/read counts and structured sink errors.
- `V2ScenarioTemplateIT` + `scenario-e-streaming-jdbc.yaml` — extend scenario catalog for CSV/JSON streaming and upsert fixtures.
- Phase 6/7 Playwright helpers (`e2e/helpers/api.ts`, `template-run.ts`) — reuse for Job center report assertions.

### Established Patterns
- **Explicit execution policy** — operators set `mode`; engine does not silently upgrade (Phase 8 adds CSV/JSON to CHUNKED/STREAMING eligibility).
- **Publish-time validation** — warnings vs hard blocks via `TemplateV2Validator` (large-file warn vs upsertKeys fail-fast).
- **Embedded-first IT + Podman Playwright UAT** — Phase 8 follows Phase 7 bar with dedicated spec + `verify-phase8-uat-*.ps1`.
- **Per-chunk JDBC batch flush** — `sinkBatchSize` in execution policy; CSV/JSON sinks mirror per-chunk disk flush.

### Integration Points
- CSV/JSON `RowSource` / `RowSink` factories in `data-generator-calcite` — add streaming iterators/writers behind existing VO types.
- JDBC sink factory / dialect layer — upsert SQL generation for PG/MySQL only.
- `TaskController` / `DistributedJobLeaseRunner` — unchanged orchestration; consume extended `RunReportCollector` output.
- Console template editor — execution policy hints, upsert key validation, IN_MEMORY large-file warning.
- Job center UI — display new sink metrics and error list from `RunReport`.

</code_context>

<specifics>
## Specific Ideas

- **Discussion language:** Chinese with user; **downstream artifacts in English** (same as Phase 6/7).
- **Fixture bar:** 10 MB / 100k rows is the documented operator expectation and CI proof target.
- **Scenario lineage:** Build on `scenario-e-streaming-jdbc` / `V2ScenarioTemplateIT` rather than a parallel test style.
- **Playwright bar:** Strictly **more than Phase 6** — five-plus scenarios covering RW-01..RW-04 including failure and warn paths.

</specifics>

<deferred>
## Deferred Ideas

- **ClickHouse / Dameng / Kingbase / HighGo upsert** — Phase 9 (RW-05/RW-06 dialect expansion).
- **Excel streaming** — not in Phase 8 scope.
- **Mid-run progress / live row counters in Job center** — future UX phase.
- **Auto-promote IN_MEMORY → CHUNKED** based on file size — rejected; explicit operator choice only.
- **Cross-chunk SQL** (streaming joins/aggregates) — engine ceiling; remains IN_MEMORY-only.
- **Harness P0 matrix rows for RW-01..04** — Phase 10 (TEST-07/08).
- **Additional file encodings** (GBK, Latin-1, etc.) — defer until operator demand.

</deferred>

---

*Phase: 8-RW Streaming & Upsert*
*Context gathered: 2026-06-27*
