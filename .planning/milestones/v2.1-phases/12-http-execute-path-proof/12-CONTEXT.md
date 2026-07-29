# Phase 12: HTTP Execute-Path Proof - Context

**Gathered:** 2026-07-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Prove that a V2 template using a **managed** JDBC `dataSourceId` can be executed through the real HTTP task spine (`POST /task/run/{id}`), reach SUCCESS, and leave countable sink rows — closing the v2.0 accepted limit where proof stopped at in-process `TemplateV2Runner` (`ManagedJdbcCatalogSinkE2eIT`).

Also prove at least one CI-friendly dialect path (PostgreSQL via Testcontainers) on that same HTTP spine with managed-catalog + dialect upsert evidence (separate IT).

This phase does **not** deliver Dameng live CI, resolver code merge, multi-JVM worker E2E, RBAC enablement, or P1 matrix wiring (Phases 13–17).

</domain>

<decisions>
## Implementation Decisions

### HTTP entry
- **D-01:** Prove via **`POST /task/run/{id}`** (not console `/api/templates/{id}/run` as the primary evidence endpoint).
- **D-02:** IT must **publish** the template before calling `/task/run` (real publish gate path).
- **D-03:** Seed template via **service/repository + publish**, then MockMvc `POST /task/run` (not full HTTP create→publish→run chain).
- **D-04:** Parse **`instanceId=`** from the `R.ok` message using the existing regex pattern (`TemplateEditorRunSupport` / `RunReportPersistenceTests`).

### Async completion
- **D-05:** Poll **`TaskExecution` repository/service** until a terminal status (not MockMvc job API, not fixed sleep).
- **D-06:** Success requires **`SUCCESS` status + managed-pool `COUNT(*)`** (same evidence bar as Phase 11 sink proof).
- **D-07:** Poll with a **~30–60s** timeout; **fail immediately** on `FAILED` / `CANCELLED`.

### Dialect packaging (EXEC-02)
- **D-08:** Use a **separate IT** from the EXEC-01 H2 managed-catalog HTTP proof (do not serialize both in one fragile test).
- **D-09:** Dialect engine = **Testcontainers PostgreSQL**.
- **D-10:** Dialect IT uses **managed `dataSourceId` + dialect upsert (ON CONFLICT)** on the HTTP execute spine.

### snap: binding depth
- **D-11:** Phase 12 proof is **unbound managed `dataSourceId` via HTTP** only. Do **not** require asserting `snap:{instanceId}:…` in this phase. Existing `JdbcSnapshotExecutePathIT` covers snap routing; ownership docs are Phase 14.

### Claude's Discretion
- Exact poll interval / backoff within the 30–60s budget
- Exact IT class naming and package placement under `data-generator-service` tests
- Whether EXEC-01 uses the same H2 URL/table naming style as Phase 11 (prefer reuse for maintainability)
- How publish is invoked (lifecycle service vs `/api/templates/{id}/publish` MockMvc) as long as D-02 holds

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — EXEC-01, EXEC-02
- `.planning/ROADMAP.md` — Phase 12 goal and success criteria
- `.planning/research/SUMMARY.md` — HTTP proof first; pitfalls on in-process mislabeling
- `.planning/research/PITFALLS.md` — Pitfall 1 (in-process labeled as HTTP), Pitfall 2 (wrong snap assumptions)
- `.planning/milestones/v2.0-MILESTONE-AUDIT.md` — accepted Phase 11 in-process limit for flow #1

### Existing proof & execute spine
- `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogSinkE2eIT.java` — Phase 11 in-process baseline (keep as regression; do not rename as HTTP)
- `data-generator-service/src/main/java/org/gensokyo/data/controller/TaskController.java` — `POST /task/run/{id}`, `runByIdInternal`, async `runV2Tracked`
- `data-generator-service/src/test/resources/application-phase7-test.yaml` — embedded test bootstrap
- `data-generator-service/src/main/java/org/gensokyo/data/template/editor/TemplateEditorRunSupport.java` — `instanceId=` parse pattern
- `data-generator-service/src/test/java/org/gensokyo/data/task/RunReportPersistenceTests.java` — same parse pattern in tests
- `data-generator-console-web/e2e/helpers/template-run.ts` — `waitForJobSuccess` timeout precedent (~90s e2e; Java IT may use 30–60s)

### Dialect / upsert precedents
- Existing Testcontainers PostgreSQL upsert ITs in calcite/service (reuse patterns for ON CONFLICT + managed id)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ManagedJdbcCatalogSinkE2eIT`: managed DS save + sink template shape + `COUNT(*)` on managed pool
- `TaskController.postRunById` / `runById`: HTTP enqueue returning message with `instanceId=`
- `TaskExecutionService` + repository: status polling for QUEUED/RUNNING/SUCCESS/FAILED
- `DataSourceConfigService.save`: managed catalog write path
- `TemplateLifecycleService` / publish APIs: PUBLISHED gate for `/task/run`

### Established Patterns
- Embedded-first: `@SpringBootTest` + `application-phase7-test.yaml`
- Async run: HTTP returns immediately; work on executor — must poll
- Evidence bar: sink `COUNT(*)` preferred over status-only for catalog proofs
- Keep Phase 11 IT unchanged as regression

### Integration Points
- New ITs connect at MockMvc → `TaskController` → `TaskExecutionService` → `TemplateV2Runner` (via production path)
- Managed JDBC resolve still through catalog / `DefaultRuntimeJdbcEndpointResolver` on execute path
- PostgreSQL dialect IT needs Testcontainers + managed DS URL pointing at container

</code_context>

<specifics>
## Specific Ideas

- Primary HTTP evidence endpoint explicitly chosen over console run API to keep the proof focused on the classic task spine.
- EXEC-01 (H2) and EXEC-02 (PG Testcontainers) intentionally split so default CI can still run the H2 HTTP proof without always needing Docker PG if planners later gate the dialect IT — planner should still aim for both green in normal verify when Testcontainers available.

</specifics>

<deferred>
## Deferred Ideas

- Console `/api/templates/{id}/run` as additional HTTP evidence — optional later / not required for Phase 12 DoD
- Asserting `snap:{instanceId}:` on the HTTP path — Phase 14 docs + existing snap ITs; optional future hardening
- Dameng live IT — Phase 13
- Multi-JVM worker — Phase 15
- RBAC enable path — Phase 16
- P1 harness rows — Phase 17

None — discussion stayed within phase scope for deferred product features

</deferred>

---

*Phase: 12-HTTP Execute-Path Proof*
*Context gathered: 2026-07-25*
