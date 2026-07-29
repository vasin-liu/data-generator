# Roadmap: data-generator

## Milestones

- ✅ **v1.0 UDF, Transform & Test Harness** — Phases 1-5 (shipped 2026-06-23)
- ✅ **v2.0 Reader/Writer & Datasource Platform** — Phases 6-11 (+07.1) (shipped 2026-07-25)
- 📋 **v2.1 Hardening & Weak-Spot Closure** — Phases 12-17 (in planning)

## Phases

<details>
<summary>✅ v1.0 UDF, Transform & Test Harness (Phases 1-5) — SHIPPED 2026-06-23</summary>

- [x] Phase 1: Test Harness Foundation (3/3 plans) — completed 2026-06-17
- [x] Phase 2: UDF Platform Core (3/3 plans) — completed 2026-06-18
- [x] Phase 3: UDF Console & Template Binding (5/5 plans) — completed 2026-06-18
- [x] Phase 4: Transform Operators & SQL (5/5 plans) — completed 2026-06-22
- [x] Phase 5: Coverage Ramp & CI Gates (2/2 plans) — completed 2026-06-23

Full archive: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

</details>

<details>
<summary>✅ v2.0 Reader/Writer & Datasource Platform (Phases 6-11) — SHIPPED 2026-07-25</summary>

- [x] Phase 6: Datasource Platform Core (5/5 plans)
- [x] Phase 7: Datasource Governance & Hot-Reload (5/5 plans)
- [x] Phase 07.1: Close gap DS-03 JDBC snapshot routing (3/3 plans) — INSERTED — completed 2026-07-24
- [x] Phase 8: RW Streaming & Upsert (12/12 plans)
- [x] Phase 9: JDBC Dialect Expansion (5/5 plans) — completed 2026-07-21
- [x] Phase 10: Harness Coverage & CI Gates (3/3 plans)
- [x] Phase 11: v2.0 closeout hardening (3/3 plans) — completed 2026-07-25

Full archive: [milestones/v2.0-ROADMAP.md](milestones/v2.0-ROADMAP.md)

</details>

## Milestone v2.1: Hardening & Weak-Spot Closure

**Status:** Ready for closeout — all phases 12–17 complete; run `/gsd-verify-work` before milestone archive
**Phases:** 12–17
**Requirements:** EXEC-01, EXEC-02, DIAL-01, DIAL-02, RES-01, DIST-01, SEC-01, TEST-09

### Overview

Close the highest-value proof and reliability gaps left after v2.0 — HTTP execute-path evidence, Dameng/Nyquist hygiene, resolver ownership docs, one multi-JVM worker path, RBAC enable-path (default-off), and focused P1 harness rows. No new major feature lane; P0 merge gate unchanged.

### Phase list

- [x] **Phase 12: HTTP Execute-Path Proof** — Managed catalog (+ dialect) via HTTP `/task/run` (EXEC-01, EXEC-02) — 2 plans (completed 2026-07-25)
- [x] **Phase 13: Dameng Live Path + Nyquist Hygiene** — Opt-in Dameng green path; VALIDATION backfill (DIAL-01, DIAL-02) (completed 2026-07-28)
- [x] **Phase 14: Resolver Ownership Docs** — Catalog vs execute-path ownership + inventory (RES-01) (completed 2026-07-29)
- [x] **Phase 15: Multi-JVM Worker E2E** — Coordinator → worker lease → SUCCESS (DIST-01) (completed 2026-07-29)
- [x] **Phase 16: RBAC Enable Path** — Testable header RBAC; default remains off (SEC-01) (completed 2026-07-29)
- [x] **Phase 17: P1 Harness Expansion + Closeout** — Focused P1 rows; keep P0 gate (TEST-09) (completed 2026-07-29)

### Phase Details

### Phase 12: HTTP Execute-Path Proof

**Goal**: Prove managed JDBC catalog (and at least one CI-friendly dialect path) through the real HTTP execute spine — not in-process `TemplateV2Runner` alone.

**Depends on**: v2.0 complete (Phases 6–11)

**Requirements**: EXEC-01, EXEC-02

**Success Criteria** (what must be TRUE):

1. MockMvc or HTTP IT enqueues a V2 template that uses a **managed** JDBC `dataSourceId` via `/task/run` or console `/api/templates/{id}/run` and the run reaches SUCCESS with sink row evidence
2. At least one CI-friendly dialect path (PostgreSQL and/or Kingbase-proxy patterns already in-repo) is proven on that same HTTP execute spine with dialect-aware write/upsert evidence
3. The IT does **not** count as done if it only invokes `TemplateV2Runner` in-process without HTTP enqueue + job/sink assertion
4. Evidence packaging makes the HTTP→managed-id→rows path obvious to maintainers reviewing the test

**Plans**: 2/2 plans complete

Plans:
**Wave 1**

- [x] 12-01-PLAN.md — EXEC-01 ManagedJdbcCatalogHttpExecuteIT (H2, MockMvc `/task/run`)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 12-02-PLAN.md — EXEC-02 ManagedJdbcCatalogHttpPostgresUpsertIT (Testcontainers PG + ON CONFLICT)

### Phase 13: Dameng Live Path + Nyquist Hygiene

**Goal**: Document a reproducible Dameng opt-in live IT green path and backfill Nyquist/VALIDATION hygiene for lagging v2.0 phases — without promoting Dameng live into the P0 merge gate.

**Depends on**: v2.0 Phase 9/11 dialect evidence (can proceed in parallel with Phase 12)

**Requirements**: DIAL-01, DIAL-02

**Success Criteria** (what must be TRUE):

1. Maintainers can follow a documented recipe (`-Ddm.it=true` / `DG_DM_IT=true`, host/image, expected PASS) for Dameng live IT
2. Default CI / merge bar remains MERGE-unit based; Dameng live IT is explicitly not a P0 merge requirement
3. Nyquist/`nyquist_compliant` / VALIDATION status is accurate for phases 07, 07.1, and 08 (honest green or documented gap closed)
4. If no Dameng host is available, done criteria are honest: documented enable path + MERGE unit remains the merge bar (no fake live green)

**Plans**: 5/5 plans complete

Plans:
**Wave 1**

- [x] 13-01-PLAN.md — Wire ChunkedPipelineDamengUpsertIT to real external Dameng JDBC with hard-fail on misconfig (DIAL-01)
- [x] 13-03-PLAN.md — Backfill 07 and 07.1 VALIDATION from existing verification evidence (DIAL-02)
- [x] 13-05-PLAN.md — **Gap closure:** Dameng `rowsUpserted` metric fix for dm-jdbc MERGE zero batch counts (DIAL-01 UAT)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 13-02-PLAN.md — Opt-in UAT wrapper script, JDBC sink guide recipe, AGENTS.md command entry (DIAL-01)
- [x] 13-04-PLAN.md — Backfill 08 VALIDATION and sync the v2.0 milestone audit Nyquist table (DIAL-02)

### Phase 14: Resolver Ownership Docs

**Goal**: Give maintainers a clear ownership model and call-site inventory for the dual JDBC resolvers — without merging them.

**Depends on**: Phase 12 preferred (inventory reflects real HTTP run-path callers); can start after research if needed

**Requirements**: RES-01

**Success Criteria** (what must be TRUE):

1. An ownership document exists describing `JdbcCatalogResolver` (catalog-side) vs `DefaultRuntimeJdbcEndpointResolver` (V2 execute-path)
2. A call-site inventory lists where each resolver is used (HTTP/run path, catalog/admin, tests)
3. No code merge of the two resolvers lands in this milestone
4. Docs state the deferred consolidation path (RES-02) without implementing it

**Plans**: 2/2 plans complete

Plans:
**Wave 1**

- [x] 14-01-PLAN.md — RES-01 ownership doc + rg-derived call-site inventory (`docs/jdbc-resolver-ownership.md`)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 14-02-PLAN.md — AGENTS.md pointer + optional governance-doc cross-link + optional Javadoc `@see`

### Phase 15: Multi-JVM Worker E2E

**Goal**: Prove one multi-JVM happy path: coordinator enqueues, worker JVM leases/executes, run reaches SUCCESS — with harness linkage and a runnable recipe.

**Depends on**: Phase 12 (stable HTTP/execute spine confidence)

**Requirements**: DIST-01

**Success Criteria** (what must be TRUE):

1. A second JVM (`DataGeneratorWorkerApplication` or equivalent worker profile) leases and executes a coordinator-enqueued job to SUCCESS
2. A harness-linked row exists for this path (P1 acceptable)
3. A runnable script or documented recipe reproduces the path locally
4. Scope stays one happy path — not full staging AC-1..AC-7

**Plans**: 3/3 plans complete

Plans:
**Wave 1**

- [x] 15-01-PLAN.md — verify-multi-jvm-worker.ps1 + host JVM spawn + dual SUCCESS poll (DIST-01 core)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 15-02-PLAN.md — P1 matrix row dist-multi-jvm-worker + harness summary linkage (D-03, D-04)

**Wave 3** *(blocked on Wave 1–2 completion)*

- [x] 15-03-PLAN.md — staging-distributed-deployment.md DIST-01 subsection + AGENTS.md pointer (D-09)

### Phase 16: RBAC Enable Path

**Goal**: Operators can enable header RBAC via a documented staging/e2e path and verify authorization in IT/E2E, while local/default stays off.

**Depends on**: None hard (can follow Phase 15 to reduce e2e churn)

**Requirements**: SEC-01

**Success Criteria** (what must be TRUE):

1. Staging/e2e documentation shows how to enable `data.generator.console-security.*` header RBAC
2. IT and/or E2E proves authorization behavior when RBAC is enabled (deny/allow observable)
3. `data.generator.console-security.enabled` remains **false** by default in base application config
4. Enabling RBAC does not break default local/dev or default e2e profiles unless those profiles explicitly opt in

**Plans**: 1/3 plans complete

Plans:
**Wave 1**

- [x] 16-01-PLAN.md — Default-off regression + profile contract IT + RBAC-on HTTP IT strengthen (SEC-01 core proof)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 16-02-PLAN.md — docs/staging-console-rbac.md + operator-console cross-link + AGENTS.md pointer (D-01, D-02, D-09)

**Wave 3** *(blocked on Wave 1–2 completion)*

- [x] 16-03-PLAN.md — verify-rbac-enable.ps1 optional Playwright e2e-rbac leg + doc packaging notes (D-06, D-07, D-08)

### Phase 17: P1 Harness Expansion + Closeout

**Goal**: Wire v2.1 proof paths into the feature matrix as focused P1 rows, keep P0/`verify-harness.ps1` semantics unchanged, and close the milestone docs.

**Depends on**: Phases 12–16 (rows link finished proofs)

**Requirements**: TEST-09

**Success Criteria** (what must be TRUE):

1. [x] Feature matrix adds focused **P1** rows covering HTTP managed-catalog execute, multi-JVM worker, and RBAC enable paths
2. [x] P0 set size/membership and `scripts/verify-harness.ps1` merge-gate semantics remain unchanged
3. [x] AGENTS/docs note how to run or interpret the new P1 rows without treating them as merge blockers
4. [x] Milestone roadmap/state reflect closeout readiness (or explicit remaining gaps) after P1 wiring

**Plans**: 3/3 complete

**Wave 1** *(matrix P1 rows — no P0 changes)*

- [x] 17-01-PLAN.md — add exec-http-managed-catalog, exec-http-postgres-dialect, rbac-enable-path; verify dist-multi-jvm-worker (D-01–D-06)

**Wave 2** *(doc regen + harness verify)*

- [x] 17-02-PLAN.md — regenerate test-feature-matrix.md; assert p0.total=15, p0.pass=true (D-09, D-12)

**Wave 3** *(docs + milestone closeout)*

- [x] 17-03-PLAN.md — test-harness.md Phase 17 subsection, AGENTS.md, REQUIREMENTS/ROADMAP/MILESTONES/STATE (D-13–D-16)

### Requirement coverage

| Requirement | Phase |
|-------------|-------|
| EXEC-01 | 12 |
| EXEC-02 | 12 |
| DIAL-01 | 13 |
| DIAL-02 | 13 |
| RES-01 | 14 |
| DIST-01 | 15 |
| SEC-01 | 16 |
| TEST-09 | 17 |

**Coverage:** 8/8 requirements mapped · 0 unmapped

### Explicitly not in this roadmap

- ORCH-01 / ORCH-02 (orchestration)
- RW-07 (net-new connectors)
- RES-02 / SEC-02 / DIST-02 / DIAL-03 / TEST-V2 (deferred beyond v2.1)

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
| ----- | --------- | -------------- | ------ | --------- |
| 1. Test Harness Foundation | v1.0 | 3/3 | Complete | 2026-06-17 |
| 2. UDF Platform Core | v1.0 | 3/3 | Complete | 2026-06-18 |
| 3. UDF Console & Binding | v1.0 | 5/5 | Complete | 2026-06-18 |
| 4. Transform Operators & SQL | v1.0 | 5/5 | Complete | 2026-06-22 |
| 5. Coverage Ramp & CI Gates | v1.0 | 2/2 | Complete | 2026-06-23 |
| 6. Datasource Platform Core | v2.0 | 5/5 | Complete | 2026-07 |
| 7. Datasource Governance & Hot-Reload | v2.0 | 5/5 | Complete | 2026-07 |
| 07.1 DS-03 JDBC snap routing | v2.0 | 3/3 | Complete | 2026-07-24 |
| 8. RW Streaming & Upsert | v2.0 | 12/12 | Complete | 2026-07 |
| 9. JDBC Dialect Expansion | v2.0 | 5/5 | Complete | 2026-07-21 |
| 10. Harness Coverage & CI Gates | v2.0 | 3/3 | Complete | 2026-07 |
| 11. Closeout hardening | v2.0 | 3/3 | Complete | 2026-07-25 |
| 12. HTTP Execute-Path Proof | v2.1 | 2/2 | Complete    | 2026-07-25 |
| 13. Dameng Live Path + Nyquist Hygiene | v2.1 | 5/5 | Complete    | 2026-07-28 |
| 14. Resolver Ownership Docs | v2.1 | 2/2 | Complete    | 2026-07-29 |
| 15. Multi-JVM Worker E2E | v2.1 | 3/3 | Complete    | 2026-07-29 |
| 16. RBAC Enable Path | v2.1 | 3/3 | Complete    | 2026-07-29 |
| 17. P1 Harness Expansion + Closeout | v2.1 | 3/3 | Complete    | 2026-07-29 |

---
*Roadmap updated: 2026-07-29 — v2.1 Phase 17 complete; ready for closeout verification*
