# Phase 7: Datasource Governance & Hot-Reload - Context

**Gathered:** 2026-06-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver **DS-03**, **DS-04**, and **DS-05**: snapshot-based hot-reload so in-flight runs are isolated from connection changes; governance policy for managed vs inline connections and secret refs; unified connectivity test before save/publish; audit visibility for datasource lifecycle and reload events.

**In scope:** Run-start connection param snapshots (JDBC/Kafka/ES); save-triggered hot-reload with DEGRADED fallback; `ConnectionCatalog.test()` for all kinds; governance flags via Spring profiles; audit event expansion; Console DEGRADED UX; fix known Datasources UI bugs and template run button; strict Playwright + playwright-cli regression (`datasource-governance.spec.ts`).

**Out of scope (later phases):** Streaming CSV/JSON and JDBC upsert (Phase 8); dialect expansion (Phase 9); harness matrix P0 expansion (Phase 10); unified `connectionRef` template field; merged single Connections console page; net-new connector families.

</domain>

<decisions>
## Implementation Decisions

### Run-Start Snapshot (DS-03)
- **D-01:** Snapshot **connection parameters only** (URL, username, secretRef, cluster name, kind-specific config) — not resolved runtime handles (DataSource, KafkaTemplate, ES Client instances).
- **D-02:** Capture snapshot when the **Worker enters RUNNING** (not at QUEUED); Coordinator does not pre-write snapshots.
- **D-03:** Snapshot **JDBC, Kafka, and Elasticsearch** connections referenced by the template (all three kinds in Phase 7).
- **D-04:** Persist snapshot JSON on **`task_execution`** (alongside existing lineage fields) **and** cache in-process until execution reaches a terminal state; **DB rows retained permanently** for audit/traceability.
- **D-05:** **Inline template connection blocks** are included in the run snapshot (same param-freeze semantics as managed refs).
- **D-06:** Each snapshotted connection records **`BOOTSTRAP` vs `MANAGED` source** tag to avoid ambiguity after reload.
- **D-07:** In **distributed mode**, the **Worker** writes the snapshot at RUNNING; Workers resolve connections from the **execution-row snapshot JSON only** (no live Catalog access on the worker path).
- **D-08:** Use **Catalog entry `version` / `updatedAt`** — snapshot pins the version at RUNNING to avoid half-updated state during concurrent reload.

### Hot-Reload (DS-03)
- **D-09:** **Save triggers immediate hot-reload** — update DB, refresh Catalog, and update runtime registries/pools for new runs (same for JDBC, Kafka, ES).
- **D-10:** **Never modify connections held by in-flight runs** — isolation via per-execution snapshot + execution-scoped resolve cache.
- **D-11:** On reload failure: **retain new DB config**, mark connection **`DEGRADED`**, and serve **last known good runtime config** for new runs until connectivity is restored.
- **D-12:** Emit **`DATASOURCE_RELOAD` audit** on every reload attempt (success or failure path per D-22).

### Governance Policy (DS-04)
- **D-13:** When governance is enabled, **production templates must use managed catalog references** — inline `dataSource` / cluster blocks are rejected at publish/run (in addition to existing plaintext-secret rules).
- **D-14:** Governance switches are **profile-based**: **staging and production ON**, **dev OFF** (`data.generator.governance.*` aligned with `application-staging.yaml` patterns).
- **D-15:** **BOOTSTRAP reference policy is profile-split**: dev/staging may reference yaml-seeded BOOTSTRAP entries; **production allows MANAGED refs only**.
- **D-16:** **Draft save surfaces warnings**; **publish and task run block** governance violations.
- **D-17:** **Grandfather existing published inline templates** — enforcement applies to **new publishes and material template changes**, not retroactive blocking of unchanged published templates.

### Connectivity Test (DS-04)
- **D-18:** Expose unified **`ConnectionCatalog.test(kind, name | payload)`** as the single connectivity entry point; service layers delegate to Catalog/adapters.
- **D-19:** Connectivity test must **pass before datasource save** and **before template publish** when governance requires it (configurable per profile).
- **D-20:** Implement connectivity tests for **JDBC, Kafka, and Elasticsearch** in Phase 7 (not JDBC-only).
- **D-21:** **Fix known Console bugs** as part of Phase 7: new JDBC test invalid, common driver selection lost after save, and **template run button invalid** (same Playwright regression batch).

### Audit (DS-05)
- **D-22:** Record full datasource audit event set: **`DATASOURCE_CREATE`**, **`UPDATE`**, **`DELETE`**, **`RELOAD`**, **`DEGRADED`**, **`CONNECTIVITY_FAIL`**, **`GOVERNANCE_BLOCK`**.
- **D-23:** Audit payload is **summary-only** (connection name, kind, action, actor) — **no passwords, secret values, or full URLs** in the feed.
- **D-24:** **Every hot-reload** (including save-triggered) writes an audit entry.
- **D-25:** Console Audit page adds **Datasource category filter** and **deep-link from Datasources page** to filtered audit feed.

### Console UX — DEGRADED State
- **D-26:** Catalog/datasource list shows **`HEALTHY` / `DEGRADED` badge**, last reload timestamp, and **detail view** with failure reason and reference to last known good config.

### Verification / E2E
- **D-27:** Add **`datasource-governance.spec.ts`** with full CRUD, connectivity gate, reload, DEGRADED display, governance block, and audit visibility coverage.
- **D-28:** Use **playwright-cli** snapshots/commands alongside existing Playwright harness for stricter, reproducible console regression (extend Phase 6 e2e helpers, do not replace embedded-first Java ITs).

### Claude's Discretion
- Exact snapshot JSON schema and `task_execution` column naming (as long as D-01–D-08 hold).
- DEGRADED “last known good” storage mechanism (Catalog generation counter vs embedded copy in catalog row).
- Kafka/ES connectivity test implementation details (admin API vs produce/ping) as long as actionable failure messages are returned.
- Grandfather detection logic for “material template change” vs metadata-only edits.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & Roadmap
- `.planning/ROADMAP.md` — Phase 7 goal, success criteria, DS-03/DS-04/DS-05 mapping
- `.planning/REQUIREMENTS.md` — DS-03, DS-04, DS-05 full requirement text
- `.planning/PROJECT.md` — v2.0 milestone, datasource governance scope
- `.planning/phases/06-datasource-platform-core/06-CONTEXT.md` — Phase 6 decisions (Catalog API, live lookup deferred here, deferred items)

### Codebase Maps
- `.planning/codebase/INTEGRATIONS.md` — governance flags, secret registry, audit API, datasource console paths
- `.planning/codebase/ARCHITECTURE.md` — TaskExecutionService, AuditService, layer boundaries
- `.planning/codebase/TESTING.md` — embedded-first and Playwright patterns

### Existing Implementation (extend, do not rewrite)
- `data-generator-datasource/data-generator-datasource-api/` — `ConnectionCatalog` interfaces (add test + version semantics)
- `data-generator-service/src/main/java/org/gensokyo/data/datasource/DataSourceConfigService.java` — JDBC CRUD, testConnection, audit hooks
- `data-generator-service/src/main/java/org/gensokyo/data/messaging/MessagingClusterConfigService.java` — Kafka/ES CRUD and registration
- `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateGovernanceSupport.java` — plaintext secret checks (extend for managed-only policy)
- `data-generator-service/src/main/java/org/gensokyo/data/config/DataGeneratorProperties.java` — `governance.*` nested config
- `data-generator-service/src/main/java/org/gensokyo/data/audit/AuditService.java` — audit record model
- `data-generator-service/src/main/java/org/gensokyo/data/task/TaskExecutionService.java` — execution row lineage snapshots
- `data-generator-service/src/main/java/org/gensokyo/data/task/RunLineageSupport.java` — snapshot JSON patterns on `task_execution`
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2RuntimeServices.java` — runtime connection access at pipeline start
- `data-generator-console-web/src/app/pages/DatasourcesPage.tsx` — datasource UI, test/save flows (known bugs)
- `data-generator-console-web/e2e/specs/datasource-*.spec.ts` — Phase 6 UAT specs (extend for governance)
- `data-generator-console-web/e2e/helpers/` — api, messaging, template-run helpers

### Testing & Verification
- `docs/testing-embedded-components.md` — embedded-first patterns
- `scripts/verify-phase6-uat-*.ps1` — Phase 6 UAT script patterns (model Phase 7 UAT scripts)
- `data-generator-service/src/test/resources/application-phase7-test.yaml` — Spring test slice profile

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DataSourceConfigService.testConnection()` / `testConnectionByName()` — JDBC connectivity baseline; extend via Catalog and fix console integration bugs.
- `TemplateGovernanceSupport.collectSecretViolations()` — publish-time governance hook; extend for managed-only and BOOTSTRAP rules.
- `AuditService.record()` — already used for `DATASOURCE_CREATE` / `UPDATE`; extend event types.
- `TaskExecutionService` + `RunLineageSupport` — precedent for persisting snapshot JSON on execution rows.
- Phase 6 `ConnectionCatalogImpl` + adapter modules — hot-reload target surface for JDBC/Kafka/ES.
- `DatasourcesPage.tsx` — test/save UI, catalog table, driver presets (fix driver-loss and test-invalid bugs).
- Phase 6 Playwright specs and `e2e/helpers/api.ts` — extend for governance matrix.

### Established Patterns
- **Profile-based governance** — `application-staging.yaml` enables RBAC and publish governance; Phase 7 aligns datasource policy with same profile split.
- **Console APIs return `R<T>`**; violations throw `IllegalArgumentException` → `ConsoleApiAdvice`.
- **Save → immediate runtime register** — JDBC already calls `registerToRuntime()` on save; Phase 7 generalizes with Catalog version + DEGRADED semantics.
- **Embedded-first Java ITs + Playwright Podman E2E** — Phase 7 adds strict playwright-cli layer without replacing Maven slices.

### Integration Points
- `CoreConfig` — wire Catalog test API, snapshot resolver into `TemplateV2RuntimeServices`.
- `TemplateLifecycleService` / publish path — connectivity + managed-only governance gates.
- `ConsoleDataSourceController` + messaging console APIs — delegate test/save to Catalog; surface DEGRADED status.
- Console Audit page — filter by datasource event types; link from Datasources page.
- Distributed worker execution path — read snapshot JSON from `task_execution` at RUNNING.

</code_context>

<specifics>
## Specific Ideas

- **Discussion language:** Chinese with user; **downstream artifacts in English** (same as Phase 6).
- **Known UI bugs (must fix in Phase 7 Playwright batch):**
  - New JDBC datasource **connectivity test invalid**
  - **Common driver selection lost** after save/create
  - **Template run button invalid** (include in same regression wave)
- **Testing bar:** Stricter **Playwright + playwright-cli** coverage than Phase 6 UAT — full CRUD/governance/reload/audit path in dedicated spec file.
- **Operator expectation:** DEGRADED connections remain visible with actionable detail — not silent fallback.

</specifics>

<deferred>
## Deferred Ideas

- **Unified template `connectionRef` field** — still deferred; would require template migration (from Phase 6).
- **Single merged Console Connections page** — UI consolidation beyond Phase 7 core.
- **Streaming/upsert run reports** — Phase 8 (RW-04).
- **Harness P0 matrix rows for DS-03..05** — Phase 10 (TEST-07/08); Phase 7 ships Playwright UAT scripts analogous to Phase 6 `verify-phase6-uat-*.ps1`.

</deferred>

---

*Phase: 7-Datasource Governance & Hot-Reload*
*Context gathered: 2026-06-26*
