# Phase 13: Dameng Live Path + Nyquist Hygiene - Context

**Gathered:** 2026-07-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Document a reproducible Dameng **opt-in** live IT green path and backfill Nyquist/`VALIDATION` hygiene for lagging v2.0 phases **07, 07.1, and 08** — without promoting Dameng live into the P0 merge gate.

Deliver: (1) wire `ChunkedPipelineDamengUpsertIT` to real external Dameng JDBC so it can **PASS** when enabled and configured; (2) maintainer recipe in JDBC sink docs + verify script + AGENTS.md pointer; (3) VALIDATION backfill + milestone-audit Nyquist table sync for 07/07.1/08 only.

This phase does **not** add Dameng to P0/`verify-harness`, implement Testcontainers Dameng, refresh Phase 12 VALIDATION, merge dual JDBC resolvers, or rewrite product tests for Nyquist theater.

</domain>

<decisions>
## Implementation Decisions

### Dameng proof depth
- **D-01:** When a Dameng host is available, wire real JDBC so `ChunkedPipelineDamengUpsertIT` can **PASS** (not docs-only against today's placeholder abort). Default CI remains skipped via `DamengTestSupport` (`-Ddm.it=true` / `DG_DM_IT=true`).
- **D-02:** If the enable flag is on but Dameng is unreachable / misconfigured → **hard FAIL** (no `Assumptions.abort` / soft skip disguised as green).
- **D-03:** PASS evidence = **chunked upsert idempotency** (same PK re-run; row count/content correct).
- **D-04:** **Must** reuse `UpsertParitySupport.assertUpsertIdempotent` (same helper as PG/MySQL upsert ITs).
- **D-05:** If no Dameng host is available, done criteria remain honest: documented enable path + MERGE-unit default CI bar (no fake live green) — per ROADMAP SC4 / DIAL-01.

### Dameng runtime target
- **D-06:** Runtime = **external JDBC URL/env only** for this phase.
- **D-07:** Connection env: `DG_DM_JDBC_URL`, `DG_DM_USER`, `DG_DM_PASSWORD` (aligned with `DG_DM_IT` / `-Ddm.it=true`). Exact property aliases (if any) are planner discretion if needed for Surefire; prefer env-first as decided.
- **D-08:** Credentials via **plaintext env** for opt-in live tests; docs must warn never commit secrets.
- **D-09:** **Testcontainers / licensed Dameng image is out of scope** this phase (deferred).

### Nyquist phase set
- **D-10:** Strict **DIAL-02** only: phases **07, 07.1, 08**. Do **not** refresh Phase 12 `12-VALIDATION.md` in this phase.
- **D-11:** Honest green = map **existing** linked tests from VERIFICATION/SUMMARY into VALIDATION, then set `nyquist_compliant: true`. **No** new product tests written only to satisfy Nyquist.
- **D-12:** Write/update VALIDATION **in-place** under `.planning/milestones/v2.0-phases/` (not copies under active `.planning/phases/`).
- **D-13:** After VALIDATION backfill, **sync** the Nyquist table in `.planning/milestones/v2.0-MILESTONE-AUDIT.md`.

### Recipe packaging
- **D-14:** Primary recipe lives in **`docs/template-v2-jdbc-sink-guide.md`** as a dedicated Dameng live IT section (flag, `DG_DM_*`, Maven command, expected PASS/FAIL semantics).
- **D-15:** Add `scripts/verify-*-dameng*.ps1` (name at planner discretion; match existing `verify-phase*-uat-*.ps1` style) wrapping env checks + Maven slice for `ChunkedPipelineDamengUpsertIT`.
- **D-16:** Script exits **non-zero** with usage when flag off or JDBC URL missing — must not look like successful UAT.
- **D-17:** Add an opt-in Dameng live command line to **`AGENTS.md`** Commands section pointing at the doc section / script.

### Claude's Discretion
- Exact `verify-*.ps1` filename within the Dameng/phase13 naming convention
- Whether `-Ddm.it` system property mirrors are also accepted alongside `DG_DM_*` for URL/user/password (env remains canonical)
- VALIDATION table row density / status checkbox style — match phase 9 VALIDATION patterns
- Minor javadoc updates on `DamengTestSupport` / IT class as part of wiring

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — DIAL-01, DIAL-02 (DIAL-03 explicitly deferred)
- `.planning/ROADMAP.md` — Phase 13 goal and success criteria
- `.planning/PROJECT.md` — Dameng opt-in tech debt; P0 not inflated
- `.planning/research/FEATURES.md` — Dameng green path + Nyquist hygiene framing
- `.planning/research/PITFALLS.md` — Pitfall: Dameng into P0; Pitfall: Nyquist rewrite theater
- `.planning/research/STACK.md` — Example Maven opt-in Dameng slice
- `.planning/milestones/v2.0-MILESTONE-AUDIT.md` — Nyquist table (07 PARTIAL; 07.1/08 MISSING) + Dameng tech debt

### Nyquist backfill targets (archive)
- `.planning/milestones/v2.0-phases/07-datasource-governance-hot-reload/07-VALIDATION.md` — exists; `nyquist_compliant: false`
- `.planning/milestones/v2.0-phases/07.1-close-gap-ds-03-jdbc-snapshot-routing-on-execute-path/` — missing VALIDATION; use VERIFICATION/SUMMARYs
- `.planning/milestones/v2.0-phases/08-rw-streaming-upsert/` — missing VALIDATION; use VERIFICATION/SUMMARYs / UAT
- `.planning/milestones/v2.0-phases/09-jdbc-dialect-expansion/09-VALIDATION.md` — pattern reference for compliant VALIDATION shape

### Dameng live IT code & docs
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/DamengTestSupport.java` — enable gate
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java` — placeholder to replace
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresUpsertTests.java` — `UpsertParitySupport` usage precedent
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineMySqlUpsertTests.java` — same helper precedent
- `docs/template-v2-jdbc-sink-guide.md` — primary recipe home (extend Dameng section)
- `AGENTS.md` — Commands section for opt-in verify pointer

### Prior phase decisions
- `.planning/phases/12-http-execute-path-proof/12-CONTEXT.md` — Dameng deferred to Phase 13; HTTP proof complete

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DamengTestSupport.damengItEnabled()` — existing `@EnabledIf` gate; keep as opt-in switch
- `UpsertParitySupport.assertUpsertIdempotent` — required evidence helper for live IT
- `JdbcSinkSqlBuilderTests` / Dameng MERGE unit tests — remain default CI proof (unchanged role)
- Phase 9/11 verify PowerShell scripts — pattern for new Dameng verify script
- Phase 9 `09-VALIDATION.md` — template for 07.1/08 VALIDATION creation and 07 refresh

### Established Patterns
- Opt-in live IT via `@EnabledIf` + env/system property (not Docker-gated for Dameng this phase)
- Dialect upsert parity via shared support class; engine-specific connection only
- Embedded-first default CI; licensed/external engines stay opt-in
- Nyquist hygiene = documentation of existing tests, not feature rewrites

### Integration Points
- Live IT stays in `data-generator-calcite` test sources (replace placeholder body)
- Docs touch `docs/template-v2-jdbc-sink-guide.md` + `AGENTS.md` + new `scripts/verify-*.ps1`
- Nyquist files only under archived `milestones/v2.0-phases/` + audit markdown

</code_context>

<specifics>
## Specific Ideas

- Hard-FAIL when flag on but host down is intentional: operators who think they enabled live must not get a silent abort-as-skip.
- Script missing-config non-zero exit mirrors that honesty for the UAT wrapper.
- Phase 12 VALIDATION refresh was considered and explicitly left out of DIAL-02 scope.

</specifics>

<deferred>
## Deferred Ideas

- Dameng Testcontainers / GenericContainer licensed-image path — future phase when image policy allows
- Phase 12 `12-VALIDATION.md` / `nyquist_compliant` refresh — not DIAL-02; separate hygiene if needed
- Promoting Dameng live IT to P0 merge gate — DIAL-03, explicitly out of v2.1
- Dual JDBC resolver consolidation — Phase 14 docs only / later RES-02

</deferred>

---

*Phase: 13-Dameng Live Path + Nyquist Hygiene*
*Context gathered: 2026-07-26*
