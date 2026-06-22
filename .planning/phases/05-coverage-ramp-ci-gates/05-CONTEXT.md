# Phase 5: Coverage Ramp & CI Gates - Context

**Gathered:** 2026-06-22
**Status:** Ready for planning
**Source:** Inline context capture (operator decisions via /gsd-plan-phase)

<domain>
## Phase Boundary

Turn the test harness from "covered/pending bookkeeping" into a **graded, gated quality signal**. Concretely: (1) define explicit **P0/P1/P2 priority tiers** on the existing capability matrix with a documented COV-01 completion target; (2) drive every **P0 row to automated green** in the embedded harness; (3) **expand console API slice tests** for the UDF and transform-metadata endpoints; (4) add a **regression merge gate** that fails CI when any P0 row is not green, documented in `AGENTS.md`.

**Requirements in scope:** COV-01 (P0/P1/P2 tiers + minimum completion target), COV-02 (P0 rows green in CI), COV-03 (console API slice tests expand for UDF + transform metadata), COV-04 (merge gate blocks on P0 failure, documented).

**Builds on prior phases (do not re-decide):**
- `.planning/test-matrix.yaml` is the single source of truth (Phase 1 D-02), consumed by `scripts/verify-harness.ps1` + `scripts/lib/test-matrix-summary.ps1` → `target/test-matrix-summary.json` (TEST-04).
- Embedded-first testing (H2 / embedded Kafka / HTTP-embedded ES) — no production credentials (Phase 1, `docs/testing-embedded-components.md`).
- Console API style: `Console*Controller` returning `R<T>`, errors via `ConsoleApiAdvice` (Phases 3–4). Slice tests `ConsoleUdfControllerTest` (Phase 3) and `ConsoleTransformCatalogControllerTest` (Phase 4) already exist and are the expansion targets.
- The harness already exits 1 when a linked Maven test fails; `pending` rows never fail the harness.

**Explicitly out of scope (later / deferred):** ramping P1/P2 rows to green (only tracked this phase), new Reader/Writer adapters, datasource refactor, console-web UI work, Playwright/Podman E2E enforcement in the P0 gate (E2E stays opt-in via `-IncludeE2e`), distributed worker multi-JVM E2E.

**Depends on:** Phase 1 (harness + matrix + summary tooling), Phase 3 (`ConsoleUdfControllerTest`), Phase 4 (`ConsoleTransformCatalogControllerTest`, the three transform rows).

</domain>

<decisions>
## Implementation Decisions

### Tiering & Target (COV-01)
- **D-01:** Add an additive `tier` field to each `.planning/test-matrix.yaml` row with values `P0` | `P1` | `P2`. `Parse-MatrixRows` already captures arbitrary `key: value` pairs, so the field parses with no parser change. Document the tier semantics in the matrix `schema` block and `docs/test-harness.md`.
- **D-02:** **COV-01 completion target = P0 must be 100% green; P1/P2 are tracked (counted) but carry no hard percentage this phase.** Quality-first: the P0 set is the gate, not an aggregate percentage.
- **D-03:** **P0 set (exactly these 7 rows)** — `calcite-scenario-v2` (core V2 run), `udf-sql`, `udf-script`, `udf-java-plugin` (UDF publish, all three types), `transform-json`, `transform-mask`, `transform-lookup` (the three new operators). This is the literal ROADMAP success-criteria set.
- **D-04:** P1/P2 assignment is **Claude's discretion** but lightweight: mark core-runtime-adjacent rows that already have linked tests as `P1` (e.g. `transform-sql-basic`, `calcite-pipeline-chunked`, `console-api-templates`, `console-api-jobs`, `reader-jdbc-basic`, `writer-jdbc-basic`, plus the COV-03 console-api rows); everything else `P2`. P1/P2 are tracked only — never block merge this phase.

### P0 Gap Closure (COV-02)
- **D-05:** Of the 7 P0 rows, six are already `covered`; **`calcite-scenario-v2` is `partial`** (linked `V2ScenarioTemplateIT`). Closing the P0 gap means its linked test(s) pass and the harness computes its status as `covered`. Scope is **make the existing core-V2 scenario row green** — not broaden scenario coverage.
- **D-06:** Extend `New-TestMatrixSummary` to emit a machine-readable **P0 rollup** in `target/test-matrix-summary.json`: a `p0` block listing each P0 row with its computed status + a boolean `green` (green ⇔ summary status `covered`), plus an overall `p0Pass` boolean. This is the artifact the gate (COV-04) reads. A P0 row whose `tier: P0` but is `partial`/`pending`/`missing`/`failed` ⇒ `green=false` ⇒ `p0Pass=false`.

### Console API Slice Expansion (COV-03)
- **D-07:** Expand the two existing slice tests (no new controllers): `ConsoleUdfControllerTest` (e.g. version-history listing, publish/deprecate state transitions, unknown-id → 400) and `ConsoleTransformCatalogControllerTest` (e.g. `kind` filter BUILTIN vs UDF, invalid `kind` → 400, per-entry metadata completeness). Link these classes into the matrix so the harness executes them. The UDF/transform-metadata API rows are **P1** (tracked), not P0 — the literal P0 set (D-03) is unchanged.

### Regression Merge Gate (COV-04)
- **D-08:** **Operator-authorized CI workflow change.** Add a P0 gate to `scripts/verify-harness.ps1`: after writing the summary, if `p0Pass` is false (any P0 row not green, or a `tier: P0` row missing linked tests) the harness exits non-zero with an explicit P0 failure message. Modify `.github/workflows/harness-verify.yml` so the P0 regression gate is an explicit, named enforcement on `pull_request` (blocks merge). Document the merge criteria + gate behavior in `AGENTS.md`.
- **D-09:** Gate is **P0-only and embedded-only**: P1/P2 failures do not block; Playwright/Podman E2E stays opt-in and is not part of the merge gate. Keeps the gate fast and deterministic on `ubuntu-latest`.

### Claude's Discretion
- Exact JSON shape of the `p0` rollup block (field names) as long as it carries per-row status + `green` + an overall `p0Pass`.
- Precise additional assertions/test method names added to the two slice tests, and whether COV-03 rows are new matrix rows (`console-api-udf`, `console-api-transforms`) or `linked_tests` added to existing console-api rows.
- Wording of the `AGENTS.md` merge-criteria section and the named CI step.
- P1/P2 row assignments beyond the P0 set (D-04).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & Roadmap
- `.planning/ROADMAP.md` — Phase 5 goal, success criteria, plans 05-01/05-02
- `.planning/REQUIREMENTS.md` — COV-01..COV-04 definitions and phase mapping
- `.planning/PROJECT.md` — milestone scope; quality-first / phased coverage ramp decision

### Harness & Matrix (extend these)
- `.planning/test-matrix.yaml` — single source of truth; add `tier` field + link COV-03 tests
- `scripts/verify-harness.ps1` — harness entry; add the P0 gate after summary generation
- `scripts/lib/test-matrix-summary.ps1` — `New-TestMatrixSummary` / `Parse-MatrixRows`; add the P0 rollup
- `scripts/generate-test-matrix-doc.ps1` — human-readable matrix doc (surface tier column)
- `docs/test-harness.md` — harness docs; document tiers + COV-01 target + gate

### Console API Slice Targets (COV-03)
- `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleUdfControllerTest.java`
- `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleTransformCatalogControllerTest.java`
- `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleUdfController.java` + `ConsoleTransformCatalogController.java` + `ConsoleApiAdvice.java` — behavior under test

### P0 Gap Target (COV-02)
- `data-generator-service/src/test/java/org/gensokyo/data/template/V2ScenarioTemplateIT.java` — `calcite-scenario-v2` linked test

### CI & Governance
- `.github/workflows/harness-verify.yml` — workflow to wire the P0 gate into (operator-authorized edit, D-08)
- `AGENTS.md` — Git workflow + Boundaries sections; document merge criteria
- `.planning/codebase/CONVENTIONS.md` — Java/Javadoc + test naming conventions

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Parse-MatrixRows` already parses arbitrary lowercase `key: value` row fields → `tier` parses with no change.
- `New-TestMatrixSummary` already computes per-row status (covered/partial/pending/skipped) from Surefire reports → P0 rollup is a thin projection over existing per-row status.
- `verify-harness.ps1` already exits 1 on linked Maven failure and already runs on `pull_request` via `harness-verify.yml` → the P0 gate is additive.
- `ConsoleUdfControllerTest` / `ConsoleTransformCatalogControllerTest` — established slice-test style to extend.

### Established Patterns
- Matrix-as-source-of-truth → summary JSON → harness exit code (Phase 1).
- Console `R<T>` + `ConsoleApiAdvice` 400 mapping for invalid input (Phases 3–4).
- Embedded-first, JDK 25 via `mvnw-jdk25.ps1` / `.mvn/settings-jdk25.xml`.

### Integration Points
- `.planning/test-matrix.yaml` + `scripts/lib/test-matrix-summary.ps1` + `scripts/verify-harness.ps1` — tiers, rollup, gate.
- `data-generator-service` test sources — COV-03 slice expansion.
- `.github/workflows/harness-verify.yml` + `AGENTS.md` — CI gate + merge criteria docs.

</code_context>

<specifics>
## Specific Ideas

- `tier: P0|P1|P2` additive matrix field; P0 = the 7 literal rows (D-03).
- COV-01 target = P0 100% green, P1/P2 tracked (D-02).
- P0 rollup block in `target/test-matrix-summary.json` with `p0Pass` boolean (D-06) — the gate's input.
- P0 gate in `verify-harness.ps1` exits non-zero on `p0Pass=false`; wired explicitly into `harness-verify.yml` on PRs (D-08).
- Only P0 row needing work: `calcite-scenario-v2` → green (D-05).
- COV-03 expands the two existing slice tests; rows are P1 (D-07).

</specifics>

<deferred>
## Deferred Ideas

- Ramping P1/P2 rows to green / an aggregate completion % target (D-02 keeps P0-only this phase).
- Playwright/Podman E2E as part of the merge gate (stays opt-in, D-09).
- New console-web operator-catalog UI page (Phase 4 deferral stands).
- Distributed worker multi-JVM harness (TEST-V2-02, v2).

</deferred>

---

*Phase: 05-coverage-ramp-ci-gates*
*Context gathered: 2026-06-22 via inline capture*
