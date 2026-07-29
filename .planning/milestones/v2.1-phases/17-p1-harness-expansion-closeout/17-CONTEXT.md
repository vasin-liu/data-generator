# Phase 17: P1 Harness Expansion + Closeout - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning
**Mode:** --auto (recommended defaults selected in one pass)

<domain>
## Phase Boundary

Wire **finished v2.1 proof paths** into `.planning/test-matrix.yaml` as **focused P1 rows**, keep the **15-row P0 merge gate** and `scripts/verify-harness.ps1` semantics **unchanged**, and **close out v2.1 milestone docs** (TEST-09).

This phase delivers harness visibility and operator documentation for proofs already shipped in Phases 12–16. It does **not** add new product features, promote v2.1 proofs to P0, change harness gate logic, expand the matrix to exhaustive console/UI coverage, or re-open implementation work in Phases 12–16 unless a wiring bug is discovered.

</domain>

<decisions>
## Implementation Decisions

### P1 row inventory (TEST-09)
- **D-01:** Add **two new P1 rows** for proofs not yet in the matrix:
  - `exec-http-managed-catalog` — EXEC-01 HTTP `/task/run` + managed JDBC catalog + H2 sink evidence
  - `rbac-enable-path` — SEC-01 header RBAC enable when `console-security.enabled=true`
- **D-02:** Add **one new P1 row** for EXEC-02:
  - `exec-http-postgres-dialect` — HTTP spine + managed catalog + PostgreSQL Testcontainers upsert (separate from EXEC-01 row per Phase 12 D-08)
- **D-03:** **Retain and verify** existing Phase 15 row `dist-multi-jvm-worker` (DIST-01) — already P1 with script-primary linkage; Phase 17 confirms it satisfies TEST-09 multi-JVM requirement without duplicating the row. Update notes/status only if harness summary drifts.

### Row linkage strategy
- **D-04:** **HTTP rows link Maven IT classes** (embedded-first, CI-friendly default path):
  - `exec-http-managed-catalog` → `ManagedJdbcCatalogHttpExecuteIT`
  - `exec-http-postgres-dialect` → `ManagedJdbcCatalogHttpPostgresUpsertIT`
- **D-05:** **RBAC row links Maven IT/unit classes** as primary harness evidence:
  - `ConsoleAuthorizationIntegrationIT`, `ConsoleSecurityDefaultOffIT`, `ConsoleAuthorizationFilterTest`
  - Notes cite `scripts/verify-rbac-enable.ps1` as supplementary operator UAT (Maven slice + optional Playwright); script is **not** wired into `verify-harness.ps1` Maven aggregation
- **D-06:** **`dist-multi-jvm-worker` stays script-primary** — `linked_tests: []`, `status: covered`, notes cite `scripts/verify-multi-jvm-worker.ps1` (Phase 15 D-03 pattern). Do not link single-JVM distributed ITs as substitute for multi-JVM proof.

### P0 gate invariants (non-negotiable)
- **D-07:** **Do not change** P0 row count (15), P0 row ids/membership, or `scripts/verify-harness.ps1` exit-code / `p0.pass` semantics. Any diff to harness gate logic requires explicit out-of-scope escalation — not TEST-09.
- **D-08:** All new rows **`tier: P1`** only. No v2.1 proof row may become P0 without a future milestone decision (Pitfall 8).
- **D-09:** After wiring, run `scripts/verify-harness.ps1` and assert `p0.total == 15`, `p0.pass == true`, and no P0 row ids changed.

### Docker-gated / skipped-conditional handling
- **D-10:** `ManagedJdbcCatalogHttpPostgresUpsertIT` may be `@EnabledIf(DockerTestSupport#dockerAvailable)` — when skipped, row status may be `skipped-conditional` in summary; this is **acceptable and non-blocking** for P1 (Phase 12 review). Document in row notes; do **not** fail merge gate or require Docker for default harness path.
- **D-11:** Do **not** add Surefire XML parsing to `verify-harness.ps1` in this phase unless research finds a one-line fix — optional hardening is Claude's discretion, not a done gate.

### Docs & milestone closeout packaging
- **D-12:** Regenerate **`docs/test-feature-matrix.md`** via `scripts/generate-test-matrix-doc.ps1` after matrix edits (Phase 15 pattern).
- **D-13:** Extend **`docs/test-harness.md`** with a **Phase 17 / v2.1 P1 evidence** subsection listing the three capability areas and row ids (`exec-http-managed-catalog`, `exec-http-postgres-dialect`, `dist-multi-jvm-worker`, `rbac-enable-path`) with evidence bars and explicit “non-blocking” reminder.
- **D-14:** Update **`AGENTS.md`** harness section: P1 row count after expansion, supplementary verify scripts (`verify-multi-jvm-worker.ps1`, `verify-rbac-enable.ps1`), reaffirm `verify-harness.ps1` as sole P0 merge gate.
- **D-15:** Closeout state updates when TEST-09 is green:
  - `.planning/REQUIREMENTS.md` — mark TEST-09 complete
  - `.planning/ROADMAP.md` — Phase 17 progress + v2.1 phase table
  - `.planning/MILESTONES.md` — v2.1 status → ready for closeout / shipped (after verification)
  - `.planning/STATE.md` — session + milestone position
- **D-16:** **Do not** run full v2.1 milestone archive (`milestones/v2.1-*`) unless planner confirms team wants archive in same phase — minimum is REQUIREMENTS/ROADMAP/MILESTONES/STATE accuracy.

### Explicit non-goals (scope lock)
- **D-17:** **Out of scope for TEST-09 done criteria:** P0 promotion of HTTP/multi-JVM/RBAC rows, new matrix rows for Dameng live (DIAL-01), resolver docs (RES-01), Nyquist backfill, exhaustive console/UI matrix (TEST-V2), combined multi-JVM+RBAC row, new product features (ORCH/RW-07), changes to Phase 12–16 IT implementations except broken linkage, editing `.github/workflows/harness-verify.yml` unless P0 set accidentally changes.

### Claude's Discretion
- Exact row `status` values (`covered` vs `partial` vs `skipped-conditional`) after first green harness run
- Whether to add `ConsoleUdfAuthorizationFilterTest` to RBAC row linked_tests
- Minor notes text in matrix rows and doc tables
- Plan wave split (matrix wiring vs docs vs milestone state)
- Optional RETROSPECTIVE.md one-liner for v2.1 harness closeout

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — TEST-09 (active); EXEC-01/02, DIST-01, SEC-01 (proof sources)
- `.planning/ROADMAP.md` — Phase 17 goal and success criteria
- `.planning/MILESTONES.md` — v2.1 scope and closeout target
- `.planning/PROJECT.md` — harness-first quality bar; P0 gate semantics
- `.planning/STATE.md` — current phase position
- `.planning/research/SUMMARY.md` — Phase 17 rationale; P1 expansion, avoid P0 inflation
- `.planning/research/PITFALLS.md` — Pitfall 8 (P0 promotion); Pitfall 7 (scope creep on matrix)

### Prior phase decisions (proof artifacts to wire)
- `.planning/phases/12-http-execute-path-proof/12-CONTEXT.md` — HTTP `/task/run` spine; separate EXEC-01/02 ITs; defer P1 wiring
- `.planning/phases/15-multi-jvm-worker-e2e/15-CONTEXT.md` — `dist-multi-jvm-worker` P1 row pattern; script-primary
- `.planning/phases/16-rbac-enable-path/16-CONTEXT.md` — RBAC proof deferred to Phase 17 matrix row

### Harness source of truth
- `.planning/test-matrix.yaml` — matrix SSOT (add rows here)
- `docs/test-harness.md` — P0 vs P1 tiers; verify-harness behavior; COV-01
- `docs/test-feature-matrix.md` — generated human-readable matrix
- `scripts/verify-harness.ps1` — merge gate entry (do not change gate semantics)
- `scripts/lib/test-matrix-summary.ps1` — summary JSON builder; P0 rollup
- `scripts/generate-test-matrix-doc.ps1` — regen docs after matrix edit
- `AGENTS.md` — verify-script catalog; merge criteria

### Proof implementations (link targets)
- `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpExecuteIT.java` — EXEC-01
- `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpPostgresUpsertIT.java` — EXEC-02
- `data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleAuthorizationIntegrationIT.java` — RBAC-on HTTP
- `data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleSecurityDefaultOffIT.java` — default-off guard
- `data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleAuthorizationFilterTest.java` — filter unit
- `scripts/verify-multi-jvm-worker.ps1` — DIST-01 primary proof (row already exists)
- `scripts/verify-rbac-enable.ps1` — SEC-01 operator UAT (supplementary)

### CI / gate
- `.github/workflows/harness-verify.yml` — P0 gate in CI (read-only unless accidental breakage)
- `.planning/test-matrix.yaml` P0 rows (15) — must remain unchanged

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **Phase 15 already added `dist-multi-jvm-worker`** — P1, `status: covered`, empty `linked_tests`, notes reference verify script
- **HTTP execute ITs exist** from Phase 12 — ready for matrix linkage without new tests
- **RBAC IT stack exists** from Phase 16 — `verify-rbac-enable.ps1` already documents non-P0 status
- **`verify-harness.ps1`** aggregates Maven `linked_tests` across all rows; rows with empty links contribute to summary totals but not Surefire slice
- **`New-TestMatrixSummary`** computes `p0.pass` only from `tier: P0` rows with `status == covered`

### Established Patterns
- P1 rows for new proofs; P0 gate frozen at 15 (Phase 10 expansion; v2.1 explicit non-inflation)
- Script-primary rows when proof requires multi-JVM or long-running host processes (Phase 15)
- Maven-linked rows when proof is `@SpringBootTest` / embedded IT (majority of matrix)
- Doc regen: edit yaml → `generate-test-matrix-doc.ps1` → update `test-harness.md` + AGENTS.md
- Supplementary UAT scripts (`verify-*-uat-*.ps1`, phase verify scripts) explicitly **not** merge gates

### Integration Points
- Matrix row `linked_tests` → Surefire `-Dtest=` list in `verify-harness.ps1`
- `target/test-matrix-summary.json` → CI harness-verify reads `p0.pass`
- Row `tier: P1` → failures tracked but exit code driven by P0 + linked Maven failures on **any** tier

</code_context>

<specifics>
## Specific Ideas

- TEST-09 names three capability areas; `dist-multi-jvm-worker` already satisfies multi-JVM — Phase 17 adds HTTP + RBAC rows and validates the existing DIST row
- Phase 12 review noted Docker-skipped EXEC-02 IT as acceptable P1 `skipped-conditional` — document, do not block
- Phase 16 explicitly deferred P1 RBAC row — this phase completes that deferred item
- v2.1 closeout should mirror v2.0 pattern: REQUIREMENTS complete, MILESTONES status update, optional archive later

</specifics>

<deferred>
## Deferred Ideas

- **P0 promotion** of HTTP execute, multi-JVM, or RBAC rows — future milestone if flake-free and team agrees
- **Surefire skip detection** in harness for Docker-gated ITs — optional hardening beyond D-11
- **Full v2.1 milestone archive** (`milestones/v2.1-ROADMAP.md`, phase folders) — may follow closeout verification
- **Matrix rows for Dameng live, resolver docs, Nyquist** — not TEST-09 scope
- **Exhaustive console/UI matrix (TEST-V2)** — deferred requirement
- **Combined multi-JVM + RBAC proof row** — optional future hardening

</deferred>

---
*Phase: 17-p1-harness-expansion-closeout*
*Discussed: 2026-07-29 (--auto)*
