# Phase 10: Harness Coverage & CI Gates - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver **TEST-07** and **TEST-08**: expand `.planning/test-matrix.yaml` with P0/P1-capable rows for streaming CSV/JSON, JDBC upsert, and each target dialect (Dameng, Kingbase, HighGo, PostgreSQL, ClickHouse); ensure `scripts/verify-harness.ps1` emits `target/test-matrix-summary.json` reflecting covered vs pending for those rows; expand the existing P0 merge gate so regressions on those paths fail CI; document the expanded P0 set and verify command in `AGENTS.md` (and sync `docs/test-harness.md`).

**In scope:** New matrix rows (strict TEST-07 list); link `linked_tests` to existing Phase 8/9 Maven test classes; mark rows `covered` when the agreed evidence bar is met; rely on existing `harness-verify.yml` + `p0.pass` (no workflow YAML rewrite); update operator/dev docs for the new P0 inventory.

**Out of scope:** New product features; Phase 6–7 datasource matrix expansion; requiring licensed DM/Kingbase/HighGo images in default CI; mandatory new fixture suites or Playwright-as-P0; exhaustive 100% matrix coverage; retiring/rewriting unrelated historical pending rows; merging phase8/9 UAT scripts into harness-verify.

**Depends on:** Phases 6–9 complete (features exist); Phase 8 streaming/upsert tests and Phase 9 dialect tests already in tree.

</domain>

<decisions>
## Implementation Decisions

### P0 merge-blocking set (TEST-08)
- **D-01:** Streaming CSV and streaming JSON are both **P0** (merge-blocking).
- **D-02:** JDBC upsert (Phase 8 PG/MySQL path) is **P0**.
- **D-03:** All five target dialects — Dameng, Kingbase, HighGo, PostgreSQL, ClickHouse — are **P0**.
- **D-04:** Gate is **strict**: any new Phase-10 P0 row whose computed status is not `covered` makes `p0.pass=false` and blocks merge (same semantics as today’s harness).

### Dialect / capability “covered” bar (TEST-07 evidence)
- **D-05:** Dameng `covered` = Dameng **MERGE SQL unit tests green** (align Phase 9 D-13/D-14); optional real DM IT remains default-skipped and is **not** required for covered.
- **D-06:** Kingbase and HighGo `covered` = **PostgreSQL Testcontainers proxy IT + dialect-key mapping unit coverage** (align Phase 9 D-15), e.g. `ChunkedPipelineKingbaseDialectTests` / `UpsertParitySupport` assertions.
- **D-07:** PostgreSQL and ClickHouse `covered` = **Testcontainers integration tests green** (upsert / insert-bulk / CK upsert-reject as applicable).
- **D-08:** Optional gated DM IT (`ChunkedPipelineDamengUpsertIT` / `-Ddm.it=true`) must **not** appear in P0 `linked_tests`; may be mentioned in `notes` only.

### Matrix row shape
- **D-09:** Streaming → **two independent P0 rows** (CSV and JSON separate).
- **D-10:** JDBC upsert → **one P0 row** that may link both PG and MySQL upsert test classes.
- **D-11:** Dialects → **five independent P0 rows** (one per engine), for per-dialect traceability.
- **D-12:** `linked_tests` **reuse existing Phase 8/9 test classes only**; no mandatory new fixtures or Playwright P0 links this phase. Add new tests only if a required row cannot be proven with current classes.

### Expansion scope & docs / CI
- **D-13:** Scope is **strict TEST-07** — do **not** fold Phase 6–7 datasource catalog/governance rows into this phase’s matrix expansion.
- **D-14:** Do **not** change `.github/workflows/harness-verify.yml`; the gate expands automatically when new `tier: P0` rows are added and linked tests run green.
- **D-15:** Update **both** `AGENTS.md` and `docs/test-harness.md` (current “P0 rows (7)” inventory must be replaced with the expanded set + `.\scripts\verify-harness.ps1`).
- **D-16:** No drive-by obligations: do not require regenerating human matrix docs, retiring old pending writer/reader rows, or dual-running phase UAT scripts inside harness-verify.

### Claude's Discretion
- Exact matrix `id` / `capability` naming strings (e.g. `v2-streaming-csv` vs similar), as long as D-09–D-11 row counts and P0 tiers hold.
- Which specific Phase 8/9 class names land in each row’s `linked_tests` list (planner/researcher map from tree).
- Whether to touch `scripts/generate-test-matrix-doc.ps1` output only if needed for local consistency — not a success criterion (D-16).
- P1 companion rows (if any) for non-blocking tracking — optional; not required by locked decisions.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & roadmap
- `.planning/ROADMAP.md` — Phase 10 goal, success criteria, TEST-07/TEST-08 mapping
- `.planning/REQUIREMENTS.md` — TEST-07, TEST-08 full text; out-of-scope exhaustive matrix
- `.planning/PROJECT.md` — v2.0 harness / quality posture
- `.planning/phases/08-rw-streaming-upsert/08-CONTEXT.md` — deferred harness P0 for RW streaming/upsert to Phase 10
- `.planning/phases/09-jdbc-dialect-expansion/09-CONTEXT.md` — deferred dialect matrix/CI; D-13–D-15 evidence bars for DM/KB/HG

### Harness & matrix (source of truth)
- `.planning/test-matrix.yaml` — capability matrix schema, tiers, linked_tests
- `scripts/verify-harness.ps1` — Maven linked slice + `p0.pass` gate
- `scripts/lib/test-matrix-summary.ps1` — summary / P0 rollup helpers (if present)
- `.github/workflows/harness-verify.yml` — CI entry (do not rewrite per D-14)
- `docs/test-harness.md` — operator-facing harness docs (update per D-15)
- `AGENTS.md` — verify command registry (update per D-15)
- `docs/testing-embedded-components.md` — embedded-first / Testcontainers norms

### Existing tests to link (illustrative — confirm at plan time)
- Streaming: `CsvJsonStreamingSinkTests`, `StreamingPipelineTests`, `CsvJsonStreamingOomIT` (as applicable)
- Upsert: `ChunkedPipelinePostgresUpsertTests`, `ChunkedPipelineMySqlUpsertTests`, `JdbcUpsertSmokeTests`, `UpsertParitySupport`
- Dialects: `JdbcSinkSqlBuilderTests`, `ChunkedPipelineKingbaseDialectTests`, `ClickHouseInsertBulkWriterIntegrationTests`; optional DM IT **not** linked (D-08)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `verify-harness.ps1` + `test-matrix.yaml` + `harness-verify.yml` — already enforce P0; Phase 10 is registration + docs, not a new gate framework.
- Phase 8/9 Maven IT/unit classes listed above — primary `linked_tests` candidates.
- `docs/test-harness.md` — documents current 7 P0 rows; must be updated after expansion.

### Established Patterns
- Matrix row fields: `id`, `capability`, `adapter`, `test_types`, `owner_module`, `status`, `tier`, `linked_tests`, `notes`.
- P0 green = computed `covered` after linked Surefire run; pending/partial P1/P2 do not fail merge alone.
- Phase 9: KB/HG PG-proxy + DM MERGE unit as intentional success evidence — matrix must encode that (D-05/D-06), not invent new CI images.

### Integration Points
- Edit `.planning/test-matrix.yaml` → run `.\scripts\verify-harness.ps1` → confirm `target/test-matrix-summary.json` `p0.pass` and new row statuses.
- Doc sync: `AGENTS.md`, `docs/test-harness.md`.
- ROADMAP Phase 10 success criteria checklist at verify-work time.

</code_context>

<specifics>
## Specific Ideas

- Expected new P0 row count shape: **2** (streaming) + **1** (upsert) + **5** (dialects) = **8 new P0 rows**, plus existing 7 → inventory docs must list the full expanded set.
- User chose the strictest dialect P0 policy; evidence bars intentionally keep default CI green without proprietary DB images.

</specifics>

<deferred>
## Deferred Ideas

- Folding Phase 6–7 datasource catalog/governance rows into the matrix / P0 set.
- Changing `harness-verify.yml` for extra services, timeouts, or phase UAT script aggregation.
- Promoting Playwright E2E into P0 `linked_tests`.
- Requiring real Dameng/Kingbase/HighGo engines in default CI.
- Exhaustive cleanup of historical `pending` reader/writer matrix rows.
- Regenerating human-readable matrix docs as a mandatory deliverable.

None raised as new product capabilities outside harness/CI.

</deferred>

---

*Phase: 10-Harness Coverage & CI Gates*
*Context gathered: 2026-07-22*
