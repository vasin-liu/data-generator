# Phase 11: v2.0 closeout hardening — DS-02 managed JDBC E2E IT + dialect preset/upsert depth - Context

**Gathered:** 2026-07-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Close the two PARTIAL E2E proof gaps from `.planning/v2.0-MILESTONE-AUDIT.md` (flows #1 and #8) so v2.0 can archive with traced evidence — **no new product capabilities**.

**In scope:**
1. **DS-02 proof depth** — A dedicated Maven `@SpringBootTest` that creates a **managed** JDBC datasource via `DataSourceConfigService.save`, runs a V2 template whose **JDBC sink** references that managed `dataSourceId` (no inline `dataSource` on sink), and asserts sink table row counts via `COUNT(*)` on the managed connection.
2. **RW-05 / RW-06 E2E depth** — Evidence pack for a non-PostgreSQL dialect journey: Playwright `kingbase8` preset → save; existing Maven connectivity actionable-failure coverage; reuse `ChunkedPipelineKingbaseDialectTests` for dialect-correct upsert (PG-proxy). Keep `postgresql16` Playwright. Narrative + new `scripts/verify-phase11-uat-closeout-hardening.ps1` (`-SkipPlaywright`).
3. **Audit/docs closeout** — Update `AGENTS.md` with the phase11 UAT command; surgically update audit flow #1/#8 disposition after evidence lands (ROADMAP SC3).

**Out of scope:**
- Dual JDBC resolver consolidation (Phase 07.1 ownership split stays)
- Changing P0 / `verify-harness.ps1` / `test-matrix.yaml` merge gate
- Dameng code/IT changes or mandatory live Dameng in default CI
- New upsert IT classes; HTTP `/api/.../run` or `/task/run` as the managed-E2E primary path
- Single-JVM “preset → test → upsert” orchestration IT or brittle Playwright Test Connection / live KB upsert
- Promoting Playwright or managed-catalog IT into P0 `linked_tests`

**Depends on:** Phase 10 complete; Phases 6/9 implementations already in tree.

</domain>

<decisions>
## Implementation Decisions

### Managed E2E proof surface
- **D-01:** New dedicated `@SpringBootTest` (do not extend `V2ScenarioTemplateIT`; not Playwright-primary).
- **D-02:** Create managed DS via `DataSourceConfigService.save`.
- **D-03:** Managed `dataSourceId` on **JDBC sink only**; source may be inline/fixture.
- **D-04:** Keep connectivity-before-save **off** (`application-phase7-test.yaml` defaults).

### Managed E2E run path
- **D-05:** In-process `TemplateV2Runner` (not HTTP `/api/templates/.../run` or `/task/run`).
- **D-06:** Assert via `COUNT(*)` on the sink table through the managed DS (same spirit as `V2ScenarioTemplateIT.countRows`).
- **D-07:** Template must use managed `dataSourceId` only on sink — **no inline `dataSource` block**.
- **D-08:** Plain **INSERT** (not upsert) for the managed E2E IT.

### Dialect depth target
- **D-09:** Primary non-PG proof = **Kingbase/HighGo** (PG-proxy + dialect keys), not Dameng live / not ClickHouse.
- **D-10:** Expand Playwright to **`kingbase8`** (beyond `postgresql16`).
- **D-11:** Connectivity = existing Maven (`ConnectionCatalogTestTests` kingbase failure path); Playwright **does not** click Test Connection.
- **D-12:** Dameng code/IT **unchanged** this phase.

### Dialect journey shape
- **D-13:** Evidence pack + verification narrative (not a single-JVM chain).
- **D-14:** New `scripts/verify-phase11-uat-closeout-hardening.ps1` with `-SkipPlaywright`.
- **D-15:** Keep `postgresql16` Playwright **and** add `kingbase8`.
- **D-16:** No new upsert IT — reuse `ChunkedPipelineKingbaseDialectTests`.

### Harness / CI / docs
- **D-17:** Verification/UAT only — **do not** change P0 / `verify-harness.ps1` gate (align Phase 10: no Phase 6–7 into P0; Playwright not P0).
- **D-18:** Update **`AGENTS.md`** with phase11 command as supplementary UAT; after execute, update **`v2.0-MILESTONE-AUDIT.md`** flow disposition per ROADMAP SC3.
- **D-19:** Reuse npm script **`e2e:phase9-jdbc-dialect`** (same `jdbc-dialect-preset.spec.ts`) — no new npm entry.
- **D-20:** **Surgical** update of audit flows #1 and #8 in `v2.0-MILESTONE-AUDIT.md` during Phase 11 closeout (evidence pointers + disposition). Optional full `/gsd-audit-milestone` before complete-milestone remains a later operator choice; overall audit `status` may stay `tech_debt` for remaining Dameng/Nyquist items.

### Claude's Discretion
- Exact IT class/package name and fixture table DDL, as long as D-01–D-08 hold.
- Whether Playwright uses parameterized `test.each` vs a second `test(...)` for `kingbase8`.
- Exact Maven surefire includes for the phase11 verify script slice.
- Wording of verification narrative linking the three dialect evidence pieces.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & roadmap
- `.planning/ROADMAP.md` — Phase 11 goal, success criteria (SC1–SC3), DS-02 / RW-05 / RW-06 mapping
- `.planning/REQUIREMENTS.md` — DS-02, RW-05, RW-06 full text
- `.planning/v2.0-MILESTONE-AUDIT.md` — flows #1 and #8 PARTIAL dispositions to close (SC3 / D-18 / D-20)
- `.planning/PROJECT.md` — v2.0 quality / embedded-first posture

### Prior phase decisions
- `.planning/phases/06-datasource-platform-core/06-CONTEXT.md` — managed catalog / DS-02 intent
- `.planning/phases/07-datasource-governance-hot-reload/07-CONTEXT.md` — connectivity gate / audit patterns
- `.planning/phases/07.1-close-gap-ds-03-jdbc-snapshot-routing-on-execute-path/07.1-CONTEXT.md` — JDBC snap routing; dual-resolver ownership stays
- `.planning/phases/09-jdbc-dialect-expansion/09-CONTEXT.md` — KB/HG PG-proxy evidence; Playwright PG-only baseline (D-12 historically); presets
- `.planning/phases/10-harness-coverage-ci-gates/10-CONTEXT.md` — D-13 no Phase 6–7 into P0; Playwright not P0

### Implementation anchors
- `data-generator-service/.../DataSourceConfigService.java` — `save` write path for managed DS
- `data-generator-service/.../V2ScenarioTemplateIT.java` — inline registration contrast; `countRows` pattern
- `data-generator-service/src/test/resources/application-phase7-test.yaml` — connectivity-before-save off
- `ChunkedPipelineKingbaseDialectTests` — reuse for dialect upsert evidence (D-16)
- `ConnectionCatalogTestTests` — kingbase actionable failure without secrets (D-11)
- `data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts` — expand beyond `postgresql16`
- `data-generator-console-web/package.json` — `e2e:phase9-jdbc-dialect` (D-19)
- `scripts/verify-phase9-uat-jdbc-dialect.ps1` — pattern for phase11 UAT script
- `AGENTS.md` — supplementary UAT command registry (D-18)
- `docs/testing-embedded-components.md` — embedded-first norms

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DataSourceConfigService.save` — production-aligned managed DS create/register.
- `TemplateV2Runner` — in-process run without HTTP/job queue.
- `V2ScenarioTemplateIT.countRows` — pattern for table `COUNT(*)` assertions (do not extend the class).
- `ChunkedPipelineKingbaseDialectTests` — kingbase/highgo PG-proxy upsert coverage.
- `ConnectionCatalogTestTests.jdbcDraftTest_kingbaseDriverFailureIsActionableWithoutSecrets` — connectivity evidence without secrets.
- `jdbc-dialect-preset.spec.ts` + `e2e:phase9-jdbc-dialect` — extend for `kingbase8`.
- Phase 8/9 UAT script shape — model for `verify-phase11-uat-closeout-hardening.ps1`.

### Established Patterns
- Phase 6–7 proof depth is **supplementary UAT**, not P0 matrix (Phase 10 D-13).
- Kingbase/HighGo harness evidence = PG-proxy + dialect-key mapping (Phase 9/10), not licensed images in default CI.
- Dual JDBC resolvers remain split post-07.1 — out of Phase 11 scope.

### Integration Points
- New managed JDBC E2E IT in `data-generator-service` test tree → included in phase11 verify Maven slice.
- Playwright spec edit under `e2e/specs/jdbc-dialect-preset.spec.ts` → invoked via existing npm script from phase11 verify (optional Playwright).
- Closeout: `AGENTS.md` + surgical `v2.0-MILESTONE-AUDIT.md` flow #1/#8 updates + `11-VERIFICATION.md`.

</code_context>

<specifics>
## Specific Ideas

- User scoped Phase 11 after audit option **B** (cleanup phase) with scope **3** (both DS-02 managed E2E IT **and** dialect preset/upsert depth).
- Discuss language: **中文** for operator-facing discuss; paths/identifiers stay English in artifacts.
- Audit overall `status: tech_debt` may remain after flow #1/#8 move toward OK if Dameng/Nyquist debt stays listed.

</specifics>

<deferred>
## Deferred Ideas

- Dual JDBC resolver consolidation.
- Folding managed-catalog / Phase 6–7 rows into P0 / `verify-harness.ps1`.
- Dameng live IT as default-CI or Phase 11 deliverable.
- ClickHouse as the primary non-PG preset journey.
- HTTP console/task run path as the canonical managed E2E proof.
- Full `/gsd-audit-milestone` rewrite inside Phase 11 (optional later; D-20 is surgical).
- Nyquist / VALIDATION.md hygiene for Phases 7/8/07.1.

</deferred>

---

*Phase: 11-v2-0-closeout-hardening-ds-02-managed-jdbc-e2e-it-dialect-pr*
*Context gathered: 2026-07-24*
