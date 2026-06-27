# Phase 7 Research: Datasource Governance & Hot-Reload

**Researched:** 2026-06-27  
**Phase:** 07-datasource-governance-hot-reload  
**Requirements:** DS-03, DS-04, DS-05  
**Status:** Ready for planning (Wave 1 complete; Waves 2–5 pending)

---

## Executive Summary

- **Phase 6 is the runtime baseline.** `ConnectionCatalog` resolves JDBC/Kafka/ES via `ConnectionCatalogImpl`; `TemplateV2RuntimeServices` delegates to live catalog on every run. Phase 7 adds isolation without changing template YAML shape (Phase 6 D-03).
- **Wave 1 (07-01) is done.** API contracts (`test()`, `reload()`, health/version on `CatalogEntry`), snapshot types (`ExecutionConnectionSnapshot`, `SnapshottedConnectionRef`), `TaskExecutionPO.connectionSnapshotJson`, governance placeholders in `application-phase7-test.yaml`, and `ExecutionConnectionSnapshotTests` are in place.
- **Waves 2–5 are not implemented.** `ConnectionCatalogImpl.test()` and `reload()` throw `UnsupportedOperationException`; no `ConnectionSnapshotSupport`, hot-reload coordinator, governance flags in Java, or console/E2E Phase 7 deliverables exist yet.
- **Core design:** Capture param-only snapshot when execution enters `RUNNING` (not `QUEUED`); persist to `task_execution.connection_snapshot_json`; wrap catalog resolve for active `WorkflowRunContext.instanceId()` so in-flight runs never see post-save pools.
- **Hot-reload path:** Replace direct `registerToRuntime()` on save with `catalog.reload(name, kind)`; on failure retain DB row, mark `DEGRADED`, serve last-known-good runtime handles for new runs (D-11).
- **Governance extends Phase B.** `TemplateGovernanceSupport` already blocks plaintext secrets; Phase 7 adds managed-only refs, BOOTSTRAP policy split, connectivity-before-save/publish gates, and grandfather logic for unchanged published templates (D-13–D-17).
- **Audit expansion is incremental.** `AuditService.record()` and `DATASOURCE_CREATE`/`UPDATE` exist; Phase 7 adds `RELOAD`, `DEGRADED`, `CONNECTIVITY_FAIL`, `GOVERNANCE_BLOCK`, `DELETE` with summary-only payloads (fix existing `url` in create/update detail — D-23).
- **Run entry points to wire:** `TaskController.runV2Tracked()` (coordinator-local) and `DistributedJobLeaseRunner.runLease()` (worker). Both call `taskExecutionService.markRunning(instanceId)` then `templateV2Runner.run(template)` with live catalog today.
- **Console gaps:** `CatalogConnectionSummaryDto` exposes only `name/kind/source`; no health badges, unified Kafka/ES test endpoints, audit deep-link, or D-21 bug fixes. Phase 6 Playwright + `playwright-cli` patterns in `scripts/verify-phase6-uat-*.ps1` are the E2E template for Phase 7.
- **Recommended execution:** Follow existing 07-01..07-05 plan waves — backend snapshot/reload (07-02), connectivity/governance/audit (07-03), console UX (07-04), strict E2E gate (07-05).

---

## Current State Analysis

### Phase 6 provides (complete)

| Capability | Location | Notes |
|------------|----------|-------|
| Unified catalog resolve | `ConnectionCatalog`, `ConnectionCatalogImpl` | Live lookup; `resolve(name, kind)`, `listAll()` merge BOOTSTRAP + MANAGED |
| JDBC/Kafka/ES adapters | `data-generator-datasource-{jdbc,kafka,elasticsearch}/` | `JdbcCatalogResolver`, relocated registries |
| Calcite runtime wiring | `CoreConfig.templateV2RuntimeContext()` | Injects live `ConnectionCatalog` into `TemplateV2RuntimeServices` |
| JDBC endpoint resolution | `DefaultRuntimeJdbcEndpointResolver` / `JdbcCatalogResolver` | Managed id first, inline `InlineDataSourceVO` fallback |
| Console CRUD | `DataSourceConfigService`, `MessagingClusterConfigService`, `ConsoleDataSourceController` | Save → direct pool/registry register (no reload abstraction) |
| JDBC connectivity test | `DataSourceConfigService.testConnection()` | JDBC-only; not routed through `ConnectionCatalog.test()` |
| Plaintext secret governance | `TemplateGovernanceSupport.collectSecretViolations()` | Used at publish (`TemplateLifecycleService`) and run (`TemplateV2Validator.validateGovernance`) |
| Basic datasource audit | `DataSourceConfigService.save()` | `DATASOURCE_CREATE` / `UPDATE` with `url` in detail (needs D-23 hardening) |
| Lineage on queue | `RunLineageSupport`, `TaskExecutionService.queueExecution()` | `templateVersion`, `pluginSetJson`, `datasourceConfigHash` at QUEUED — **not** connection params |

### Phase 7 Wave 1 provides (complete — 07-01-SUMMARY)

| Artifact | Status |
|----------|--------|
| `ConnectionHealthStatus`, `ConnectionTestRequest`, `ConnectionTestResult` | Done |
| Extended `CatalogEntry` (version, updatedAt, healthStatus, lastReloadAt, degradedReason) | Done |
| `ConnectionCatalog.test()`, `reload()`, `findEntry()` | Interface done; **impl stubbed** |
| `ExecutionConnectionSnapshot`, `SnapshottedConnectionRef` | Done |
| `TaskExecutionPO.connectionSnapshotJson` | Field mapped; **no writer yet** |
| `application-phase7-test.yaml` governance placeholders | YAML only — **not bound in `DataGeneratorProperties.Governance` yet** |

### Gaps vs DS-03 / DS-04 / DS-05

| Requirement | Gap |
|-------------|-----|
| **DS-03** Hot-reload + snapshot isolation | No snapshot builder, no RUNNING persistence, no snapshot-scoped catalog, `reload()` unimplemented, save still calls `registerToRuntime()` directly |
| **DS-04** Governance + connectivity gate | No managed-only / BOOTSTRAP rules, no `require-connectivity-test-before-save`, no unified Kafka/ES test, staging profile missing Phase 7 flags |
| **DS-05** Audit for reload/lifecycle | Missing event types; no reload audit; audit UI lacks datasource category filter / deep-link; detail payloads may leak URLs |

### Known UI bugs (D-21 — must fix in Phase 7)

1. **New JDBC test invalid** — Modal test uses `testDataSourceConnection()` (`POST /api/datasources/test`); likely fails when driver preset state and form fields are out of sync on create flow (`DatasourcesPage.tsx` + `DriverPresetFields.tsx`).
2. **Driver preset lost after save** — `selectedPresetId` is local state in `DatasourcesPage`; not round-tripped on edit reopen (`openEditJdbc` sets `driverClassName` only, not preset id).
3. **Template run button invalid** — Regression target for `datasource-governance.spec.ts`; Phase 6 `datasource-v2-template-run.spec.ts` covers scenario runs but not post-CRUD session invalidation.

---

## Technical Approach

### 1. Run-start snapshot (DS-03, D-01–D-08)

**When:** On transition to `RUNNING`, not at `queueExecution()` (D-02). Coordinator paths:

- `TaskController.runV2Tracked()` — line ~287: `markRunning` then `WorkflowRunContext.bind` then `templateV2Runner.run`
- `DistributedJobLeaseRunner.runLease()` — line ~79: same order inside worker JVM (D-07)

**What to capture:** `ConnectionSnapshotSupport.buildSnapshot(TemplateV2VO template, ConnectionCatalog catalog)`:

- Walk `template.getSources()` for `QuerySourceVO` / `PostGisQuerySourceVO` (`dataSourceId` + inline `dataSource`)
- Walk `template.getSinks()` → `JdbcWriterVO`, Kafka writers (`writer.getDataSourceId()` as cluster key per `KafkaSinkFactory`), ES writers
- For each ref: `SnapshottedConnectionRef` with `name`, `kind`, `source` (BOOTSTRAP/MANAGED from `catalog.findEntry`), `catalogVersion`, `catalogUpdatedAt`, param-only `configParams` (no resolved handles, no plaintext secrets — see `ExecutionConnectionSnapshotTests`)

**Persistence (D-04):**

- Serialize to `task_execution.connection_snapshot_json` inside `markRunning` (extend signature or companion service method with template + catalog)
- In-process `ConcurrentHashMap<Long, ExecutionConnectionSnapshot>` keyed by `instanceId`; evict on terminal status (`markSuccess` / `markFailed` / `markCancelled`); DB row retained permanently

**Version pinning (D-08):** Use `CatalogEntry.version()` (today derived from `updatedAt.toEpochMilli()` in `ConnectionCatalogImpl.entry()`) at snapshot time to detect half-updated reloads.

### 2. Snapshot-scoped resolve (DS-03, D-07, D-10)

**Pattern:** `ExecutionSnapshotConnectionCatalog` decorator wrapping `ConnectionCatalogImpl`:

- If `WorkflowRunContext.instanceId()` is set → resolve from in-memory cache or deserialized `connectionSnapshotJson` (worker path: **DB only**, no live catalog — D-07)
- Else → delegate to live catalog (new runs, console test, preview)

**Calcite integration:** Either:

- Replace bean in `CoreConfig` with wrapper that reads `WorkflowRunContext`, or
- Build per-run `TemplateV2RuntimeServices` with snapshot catalog in `TaskController` / `DistributedJobLeaseRunner` before `templateV2Runner.run` (plan 07-02 prefers decorator + existing bean wiring)

**Inline blocks (D-05):** Snapshot includes inline param maps; snapshot resolver materializes ephemeral inline pools from frozen params (similar to `DefaultRuntimeJdbcEndpointResolver.ensureInlineDataSource`) without reading live catalog.

### 3. Hot-reload (DS-03, D-09–D-12)

**Trigger:** After successful DB commit in:

- `DataSourceConfigService.save()` — today calls `registerToRuntime(saved)` directly
- `MessagingClusterConfigService.saveKafka()` / `saveElasticsearch()` — today calls `registerKafka` / `registerElasticsearch`

**Replace with:** `connectionCatalog.reload(name, kind)` implemented in `HotReloadCoordinator` + `ConnectionCatalogImpl`:

1. Load desired config from DB
2. Attempt pool/registry refresh (JDBC: `DynamicRoutingDataSource`; Kafka/ES: adapter registries)
3. **Success:** bump version, `HEALTHY`, update `lastReloadAt`
4. **Failure (D-11):** keep DB row, set `DEGRADED` + `degradedReason`, retain last-known-good handles in a generation map for new resolves
5. Emit `DATASOURCE_RELOAD` audit (success/failure) — wired in 07-03

**In-flight isolation (D-10):** Runs with snapshot never call live reload paths; decorator ignores catalog version bumps after RUNNING.

### 4. Governance policy (DS-04, D-13–D-17)

**Extend `DataGeneratorProperties.Governance`** (YAML keys already in `application-phase7-test.yaml`):

| Property | Dev (phase7-test) | Staging/prod |
|----------|-------------------|--------------|
| `require-managed-connections` | `false` | `true` (prod templates) |
| `allow-bootstrap-references` | `true` | `false` (prod only MANAGED) |
| `require-connectivity-test-before-save` | `false` | configurable ON |
| `reject-plaintext-passwords-in-templates` | `true` | `true` (existing) |

**Enforcement layers:**

- **Draft save:** warnings via extended `TemplateGovernanceSupport.collectWarnings()` or new `DatasourceGovernanceSupport.collectViolations()` — non-blocking in dev (D-16)
- **Publish / run:** hard fail in `TemplateLifecycleService.publish()` and `TaskController.runV2` / console run APIs (D-16)
- **Grandfather (D-17):** If template status is PUBLISHED and `contentMd5` unchanged since last publish, skip new managed-only rules; enforce on material content change or new publish — reuse `RunLineageSupport.templateVersion()` / `TemplatePO.contentMd5`

**Managed-only detection:** Flag templates using inline `dataSource` blocks or non-catalog Kafka/ES inline config when `requireManagedConnections=true`.

### 5. Unified connectivity test (DS-04, D-18–D-20)

**API:** `ConnectionCatalog.test(ConnectionTestRequest)` — existing record with `forExisting(kind, name)` and `forDraft(kind, payload)`.

**Implementation (07-03):**

- **JDBC:** Delegate to `DataSourceConfigService.testConnection()` logic (Druid + `JdbcDriverLoadResult`)
- **Kafka:** Admin client or producer ping against `bootstrapServers` from draft or persisted `MessagingClusterConfigPO`
- **Elasticsearch:** HTTP ping to cluster `uris` (reuse patterns from `MessagingClusterConfigService` client build)

**Service gates (D-19):** When `requireConnectivityTestBeforeSave`, `DataSourceConfigService.save()` and messaging saves require prior successful `catalog.test()` (session token or last-test timestamp — planner discretion per D-28).

**Console:** Route `POST /api/datasources/test` and new Kafka/ES test endpoints through catalog; support draft payload for new JDBC row (D-21).

### 6. Audit (DS-05, D-22–D-25)

**Event set:**

| Action | Trigger |
|--------|---------|
| `DATASOURCE_CREATE` / `UPDATE` / `DELETE` | CRUD (extend delete audit on `remove()`) |
| `DATASOURCE_RELOAD` | Every hot-reload attempt (D-24) |
| `DATASOURCE_DEGRADED` | Reload failure transition |
| `DATASOURCE_CONNECTIVITY_FAIL` | Failed `catalog.test()` when governance requires |
| `GOVERNANCE_BLOCK` | Publish/run blocked by datasource policy |

**Payload (D-23):** `{ name, kind, action, actor }` summary — **remove full URLs** from current create/update detail in `DataSourceConfigService` (line ~122 puts `url`). `AuditDetailSanitizer` already redacts `password`/`secret` fragments.

**Console:** Extend `AuditPage` `ACTION_OPTIONS` and resource type filter; deep-link `?resourceType=DATASOURCE&resourceId={name}` from Datasources page (D-25).

---

## File-Level Change Map

### Create (new)

| File | Rationale |
|------|-----------|
| `data-generator-service/.../task/ConnectionSnapshotSupport.java` | Extract template refs; build `ExecutionConnectionSnapshot` (07-02) |
| `data-generator-service/.../datasource/catalog/HotReloadCoordinator.java` | DEGRADED state machine, last-known-good generation (07-02) |
| `data-generator-service/.../datasource/catalog/ExecutionSnapshotConnectionCatalog.java` | Snapshot-scoped resolve decorator (07-02) |
| `data-generator-service/.../template/DatasourceGovernanceSupport.java` | Managed-only, BOOTSTRAP, grandfather checks (07-03) — or extend `TemplateGovernanceSupport` |
| `data-generator-service/.../audit/DatasourceAuditActions.java` | Action constant enum for consistency (07-03) |
| `data-generator-datasource-jdbc/.../JdbcConnectionTester.java` | Catalog.test JDBC delegate (07-03) |
| `data-generator-datasource-kafka/.../KafkaConnectionTester.java` | Catalog.test Kafka delegate (07-03) |
| `data-generator-datasource-elasticsearch/.../ElasticsearchConnectionTester.java` | Catalog.test ES delegate (07-03) |
| `data-generator-service/src/test/.../ConnectionSnapshotSupportTests.java` | RUNNING capture unit tests (07-02) |
| `data-generator-service/src/test/.../HotReloadTests.java` | Isolation + DEGRADED scenarios (07-02) |
| `data-generator-service/src/test/.../ConnectionSnapshotIT.java` | Spring slice end-to-end snapshot (07-02) |
| `data-generator-service/src/test/.../DatasourceGovernanceIT.java` | Profile matrix dev/staging/prod (07-03) |
| `data-generator-console-web/e2e/specs/datasource-governance.spec.ts` | Phase 7 UAT (07-05) |
| `data-generator-console-web/e2e/helpers/datasource-governance.ts` | API helpers for reload isolation (07-05) |
| `data-generator-console-web/e2e/cli/run-datasource-governance-cli.ps1` | playwright-cli regression (07-05) |
| `scripts/verify-phase7-uat-datasource-governance.ps1` | Podman + Playwright gate (07-05) |
| `scripts/verify-phase7-uat-hot-reload.ps1` | Hot-reload isolation UAT (07-05) |

### Modify (existing)

| File | Change |
|------|--------|
| `ConnectionCatalogImpl.java` | Implement `test()`, `reload()`; health/version state; delegate to HotReloadCoordinator |
| `DataSourceConfigService.java` | Save → `catalog.reload()`; test/save governance gates; audit payload fix; `DATASOURCE_DELETE` on remove |
| `MessagingClusterConfigService.java` | Save → `catalog.reload()`; audit hooks; optional test-before-save |
| `TaskExecutionService.java` | `markRunning` persists snapshot; terminal methods evict cache |
| `TaskController.java` | Pass template to snapshot capture; governance at run; optional snapshot-scoped runtime |
| `DistributedJobLeaseRunner.java` | Worker snapshot write at RUNNING; snapshot-only resolve (D-07) |
| `CoreConfig.java` | Register `ExecutionSnapshotConnectionCatalog` wrapper bean |
| `DataGeneratorProperties.Governance` | Bind new YAML flags |
| `application-staging.yaml` | Enable Phase 7 governance flags for acceptance |
| `TemplateGovernanceSupport.java` / `TemplateLifecycleService.java` | Managed-only + grandfather + connectivity at publish |
| `TemplateV2Validator.java` | Wire datasource governance warnings vs errors |
| `CatalogConnectionSummaryDto.java` | Add healthStatus, lastReloadAt, degradedReason, version |
| `ConsoleDataSourceController.java` | Unified test endpoint(s); list health fields |
| `DatasourcesPage.tsx` | Badges, test gate UX, driver preset fix, audit deep-link |
| `AuditPage.tsx` | Datasource category filter + query-param deep-link |
| `TaskExecutionPO` / DB schema | Ensure `connection_snapshot_json` column exists in H2 init schema (verify migration — 07-01 notes migration but column may need `schema.sql` update) |

---

## Integration Points

### CoreConfig

```276:296:data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java
    public TemplateV2RuntimeContext templateV2RuntimeContext(...,
                                                             ConnectionCatalog connectionCatalog,
                                                             ...) {
        ...
        return new TemplateV2RuntimeContext(
                runtimeJdbcEndpointResolver,
                new TemplateV2RuntimeServices(
                        namedParameterJdbcTemplate,
                        connectionCatalog,
                        aiRuntimeBridgeProvider.getIfAvailable()
                ),
                ...
        );
    }
```

Phase 7 should inject a **wrapper** `ConnectionCatalog` that consults `WorkflowRunContext.instanceId()` before delegating to `ConnectionCatalogImpl`.

### Worker / distributed mode (D-07)

- Coordinator: `TaskController.runV2` → `distributedJobService.enqueue` — snapshot **not** written at queue time
- Worker: `DistributedJobLeaseRunner.runLease` → `markRunning` → must write snapshot and resolve **only** from `task_execution.connection_snapshot_json`
- Worker JVM may not have identical live catalog state; snapshot JSON is source of truth

### Publish / run gates

| Gate | Location | Phase 7 addition |
|------|----------|----------------|
| Plaintext secrets | `TemplateLifecycleService.publish()`, `TaskController.runV2` | Existing |
| Published-only run | `TemplateLifecycleService.requirePublishedForTaskRun()` | Existing (staging) |
| Managed connections | **New** — publish + run | `DatasourceGovernanceSupport` |
| Connectivity test | **New** — save + publish (profile) | `catalog.test()` |
| Grandfather | **New** — publish only when content changed | Compare `contentMd5` |

### Calcite pipeline

- `TemplateV2RuntimeServices.kafkaTemplate(cluster)` / `elasticsearchClient(cluster)` → `connectionCatalog.resolve` — must hit snapshot decorator during active run
- `JdbcCatalogResolver` / `RuntimeJdbcEndpointResolver` — inline + managed paths must respect snapshot for JDBC keys frozen at RUNNING

---

## Testing Strategy

### Java IT slices (embedded-first)

| Test class | Validates | Profile / infra |
|------------|-----------|-----------------|
| `ExecutionConnectionSnapshotTests` | JSON round-trip, no secrets | Unit (exists) |
| `ConnectionSnapshotSupportTests` | Ref extraction JDBC/Kafka/ES + inline | Unit |
| `ConnectionSnapshotIT` | Snapshot written on RUNNING, absent at QUEUED | `@SpringBootTest` + `application-phase7-test.yaml`, H2 |
| `HotReloadTests` | In-flight isolation, new run picks reload, DEGRADED fallback | H2 + embedded Kafka/ES stubs |
| `ConnectionCatalogTestTests` | Unified test for all kinds | Adapter unit + service IT |
| `DatasourceGovernanceIT` | dev OFF vs staging ON matrix | `@TestPropertySource` overrides |
| `PhaseBGovernanceTests` | Extend for managed-only cases | Unit |
| Phase 6 regression | `V2ScenarioTemplateIT`, `ConnectionCatalogImplTests`, `ConsoleDataSourceControllerTest` | Must stay green |

**Embedded components** (per `docs/testing-embedded-components.md`):

- H2 metadata: `jdbc:h2:mem:data-generator-phase7`
- Kafka: `EmbeddedKafkaTestSupport` / `EmbeddedKafkaKraftBroker`
- ES: `EmbeddedElasticsearchHttpSupport` HTTP stub
- No production credentials in tests

### Playwright E2E (Phase 6 patterns → Phase 7)

**Existing specs to extend:**

- `datasource-managed-crud.spec.ts` — CRUD baseline
- `datasource-catalog-overview.spec.ts` — BOOTSTRAP/MANAGED badges
- `datasource-v2-template-run.spec.ts` — run regression (D-21)
- `datasource-messaging-sink-resolution.spec.ts` — Kafka/ES resolve

**New:** `datasource-governance.spec.ts` (D-27) covering CRUD + test gate, hot-reload isolation (API assertion), DEGRADED UI, governance block, audit visibility.

**Verify scripts:** Mirror `scripts/verify-phase6-uat-managed-crud.ps1`:

1. Podman build + health wait
2. `npx playwright test e2e/specs/datasource-governance.spec.ts`
3. `e2e/cli/run-datasource-governance-cli.ps1` via `npx playwright-cli` (D-28)

### playwright-cli patterns (from Phase 6)

From `e2e/cli/run-managed-crud-cli.ps1`:

- Session: `npx playwright-cli -s=dg-phase7-governance open {url}`
- Assertions: `npx playwright-cli eval '() => ...'` expecting `Result true`
- REST setup via `Invoke-RestMethod` against `DG_E2E_API_URL`
- Screenshots for DEGRADED detail states (07-UI-SPEC)

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Concurrency / half-updated reload** | New run sees mixed old pool + new config | Pin `catalogVersion` at RUNNING (D-08); reload compares version before swap |
| **markRunning without template** | Empty snapshot | Extend run path to pass `TemplateV2VO` into snapshot service (both `TaskController` and `DistributedJobLeaseRunner`) |
| **Worker without live catalog** | Resolve failure | Worker reads snapshot JSON only (D-07); IT with worker profile |
| **DEGRADED last-known-good storage** | Memory loss on restart | Persist generation counter + optional embedded good-config copy in catalog row (planner discretion D-11) |
| **Grandfather false positives** | Blocks legacy templates | Only enforce on content hash change vs last published `contentMd5` (D-17) |
| **Audit URL leakage** | D-23 violation | Remove `url` from audit detail maps; sanitize host-only summary |
| **schema.sql / migration drift** | H2 IT fails on missing column | Verify `connection_snapshot_json` in DB init used by phase7-test profile |
| **Kafka sink cluster field** | Missed snapshot refs | `KafkaSinkFactory` uses `writer.getDataSourceId()` as cluster name — include in extractor |
| **WorkflowRunContext bind order** | Snapshot decorator misses instanceId | Capture snapshot in `markRunning` before `templateV2Runner.run`; decorator uses same instanceId |
| **Phase 6 regression** | DS-01/02 break | Run `ConnectionCatalogImplTests`, `V2ScenarioTemplateIT` in every wave verify |

---

## Open Questions

**Settled in CONTEXT (do not re-open):** D-01–D-28 including snapshot at RUNNING, param-only freeze, worker snapshot-only resolve, save-triggered reload, DEGRADED fallback, profile-split governance, grandfather unchanged published templates, unified `ConnectionCatalog.test()`, full audit event set, console DEGRADED UX, `datasource-governance.spec.ts` + playwright-cli.

**Remaining planner discretion (from CONTEXT "Claude's Discretion"):**

1. **Exact snapshot JSON schema naming** — field names in `configParams` per kind (must satisfy D-01, D-05; follow `ExecutionConnectionSnapshotTests` patterns).
2. **Last-known-good storage** — in-memory generation map vs auxiliary DB column on datasource/messaging POs.
3. **Kafka/ES test implementation** — admin API vs producer ping vs HTTP HEAD (must return actionable messages per D-20).
4. **Grandfather "material change"** — strict `contentMd5` diff vs semantic diff ignoring whitespace/comments.
5. **Connectivity gate session model** — how console proves "test passed before save" (client-side flag vs server-side short-lived token).
6. **`schema.sql` location** — 07-01 claims migration added; confirm H2 init script includes `connection_snapshot_json` or rely on JPA ddl-auto for tests.

---

## Validation Architecture

Nyquist mapping for Phase 7 requirements — test types, config, and embedded components.

| Dimension | DS-03 Hot-reload & snapshot | DS-04 Governance & connectivity | DS-05 Audit |
|-----------|----------------------------|--------------------------------|-------------|
| **Unit** | `ConnectionSnapshotSupportTests`, `ExecutionConnectionSnapshotTests`, adapter `*ConnectionTester` tests | `DatasourceGovernanceSupportTests`, extended `PhaseBGovernanceTests` | Audit action constant / sanitizer tests |
| **Service IT** | `HotReloadTests`, `ConnectionSnapshotIT` | `DatasourceGovernanceIT`, `ConnectionCatalogTestTests` | Audit filter IT on `ConsoleAuditController` |
| **Calcite IT** | Snapshot-scoped run via `InMemoryCatalog` + decorator | N/A (governance at service boundary) | N/A |
| **Spring profile** | `application-phase7-test.yaml` (governance OFF) | Override: `require-managed-connections=true`, `allow-bootstrap-references=false` | Same + audit list filters |
| **Staging profile** | Podman E2E with live reload | `application-staging.yaml` + governance ON | Audit deep-link in E2E |
| **Embedded JDBC** | H2 inline + managed datasources | Connectivity test against H2 URL | N/A |
| **Embedded Kafka** | `EmbeddedKafkaKraftBroker` reload + run isolation | Kafka `catalog.test()` draft payload | N/A |
| **Embedded ES** | HTTP stub cluster reload | ES `catalog.test()` against stub | N/A |
| **Playwright** | API helper: start run → save DS → assert execution unchanged | Publish inline template blocked in staging fixture | Navigate audit filter after CRUD/reload |
| **playwright-cli** | Snapshot list + DEGRADED detail screenshots | Governance error toast eval | Audit page category eval |
| **Verify scripts** | `verify-phase7-uat-hot-reload.ps1` | `verify-phase7-uat-datasource-governance.ps1` | Included in governance script |
| **Regression gate** | Phase 6 specs + `V2ScenarioTemplateIT` | `ConsoleDataSourceControllerTest` | `ConsoleAuditControllerTest` |
| **Harness matrix** | Deferred Phase 10 (TEST-07/08) | Deferred Phase 10 | Deferred Phase 10 |

**Config file roles:**

- `application-phase7-test.yaml` — default IT slice; governance OFF for isolated snapshot/reload tests
- `application-staging.yaml` — extend with `require-managed-connections`, `require-connectivity-test-before-save`, `allow-bootstrap-references`
- `application-e2e-rbac.yaml` — reference for RBAC + governance combined E2E

---

## Recommended Plan Breakdown

Aligns with existing `.planning/phases/07-datasource-governance-hot-reload/07-0N-PLAN.md` artifacts.

### Wave 1 — Catalog API & snapshot schema ✅ (07-01 complete)

- API types, `TaskExecutionPO` column, governance YAML placeholders, `ExecutionConnectionSnapshotTests`

### Wave 2 — Run-start snapshot, hot-reload, DEGRADED (07-02)

**Plans:** Single execute plan 07-02 (2 tasks)

1. `ConnectionSnapshotSupport` + `TaskExecutionService` RUNNING hook + distributed worker parity
2. `HotReloadCoordinator`, `ExecutionSnapshotConnectionCatalog`, save-path `reload()`, `HotReloadTests` / `ConnectionSnapshotIT`

**Verify:** `.\mvnw-jdk25.ps1 -pl data-generator-service,data-generator-calcite -am test -Dtest=HotReloadTests,ConnectionSnapshotIT -q`

### Wave 3 — Connectivity, governance, audit backend (07-03)

**Plans:** Single execute plan 07-03 (2 tasks)

1. Adapter `test()` implementations + `ConnectionCatalogImpl.test()`
2. `DatasourceGovernanceSupport`, property binding, publish/run/save gates, full audit event wiring

**Verify:** `DatasourceGovernanceIT`, `ConnectionCatalogTestTests`, staging profile IT

### Wave 4 — Console UX & bug fixes (07-04)

**Plans:** Single execute plan 07-04

- Health badges, unified test UX, driver preset fix, audit deep-link, template run regression fix

**Verify:** `.\scripts\verify-console-unit.ps1 -IncludeWebBuild` + manual/console build

### Wave 4 (parallel after 07-04) — E2E & UAT scripts (07-05)

**Plans:** Single execute plan 07-05

- `datasource-governance.spec.ts`, playwright-cli script, `verify-phase7-uat-*.ps1`

**Verify:** `.\scripts\verify-phase7-uat-datasource-governance.ps1` (full Podman pipeline)

---

## Key Code References

**Live catalog (Phase 6 — to wrap in Phase 7):**

```97:110:data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogImpl.java
    @Override
    public ConnectionTestResult test(ConnectionTestRequest request) {
        throw new UnsupportedOperationException("ConnectionCatalog.test is implemented in Phase 7 Wave 2");
    }

    @Override
    public CatalogEntry reload(String name, ConnectionKind kind) {
        throw new UnsupportedOperationException("ConnectionCatalog.reload is implemented in Phase 7 Wave 2");
    }
```

**Run path (snapshot hook point):**

```282:292:data-generator-service/src/main/java/org/gensokyo/data/controller/TaskController.java
    private void runV2Tracked(TemplateV2VO template, Long instanceId) {
        ...
        taskExecutionService.markRunning(instanceId);
        WorkflowRunContext.bind(instanceId, control);
        ...
        TemplateV2RunResult result = templateV2Runner.run(template);
```

**Governance today (plaintext only):**

```42:66:data-generator-service/src/main/java/org/gensokyo/data/template/TemplateGovernanceSupport.java
    public static List<String> collectSecretViolations(TemplateV2VO template, boolean rejectPlaintextPasswords) {
        ...
    }
```

**Thread-local run identity (snapshot decorator key):**

```45:48:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/WorkflowRunContext.java
    public static Long instanceId() {
        Binding binding = CURRENT.get();
        return binding == null ? null : binding.instanceId();
    }
```

---

*Research complete for Phase 7 planning. Downstream `/gsd-plan-phase 7` should treat 07-01 as done and schedule 07-02 onward.*
