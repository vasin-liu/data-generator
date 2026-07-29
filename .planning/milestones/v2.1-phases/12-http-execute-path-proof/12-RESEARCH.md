# Phase 12 Research: HTTP Execute-Path Proof

**Researched:** 2026-07-25  
**Domain:** Brownfield Java/Maven/Spring Boot Template V2 — MockMvc HTTP `/task/run` + managed JDBC catalog (+ PostgreSQL Testcontainers upsert)  
**Confidence:** HIGH  
**CodeGraph:** not indexed at repo root this session — structural claims verified via Read/Grep [ASSUMED tooling gap; see Project Constraints]

---

## User Constraints

Locked decisions from `12-CONTEXT.md` (verbatim for planners/implementers):

- **D-01:** Prove via **`POST /task/run/{id}`** (not console `/api/templates/{id}/run` as the primary evidence endpoint).
- **D-02:** IT must **publish** the template before calling `/task/run` (real publish gate path).
- **D-03:** Seed template via **service/repository + publish**, then MockMvc `POST /task/run` (not full HTTP create→publish→run chain).
- **D-04:** Parse **`instanceId=`** from the `R.ok` message using the existing regex pattern (`TemplateEditorRunSupport` / `RunReportPersistenceTests`).
- **D-05:** Poll **`TaskExecution` repository/service** until a terminal status (not MockMvc job API, not fixed sleep).
- **D-06:** Success requires **`SUCCESS` status + managed-pool `COUNT(*)`** (same evidence bar as Phase 11 sink proof).
- **D-07:** Poll with a **~30–60s** timeout; **fail immediately** on `FAILED` / `CANCELLED`.
- **D-08:** Use a **separate IT** from the EXEC-01 H2 managed-catalog HTTP proof (do not serialize both in one fragile test).
- **D-09:** Dialect engine = **Testcontainers PostgreSQL**.
- **D-10:** Dialect IT uses **managed `dataSourceId` + dialect upsert (ON CONFLICT)** on the HTTP execute spine.
- **D-11:** Phase 12 proof is **unbound managed `dataSourceId` via HTTP** only. Do **not** require asserting `snap:{instanceId}:…` in this phase. Existing `JdbcSnapshotExecutePathIT` covers snap routing; ownership docs are Phase 14.

**Deferred (out of Phase 12 DoD):**

- Console `/api/templates/{id}/run` as additional HTTP evidence — optional later / not required for Phase 12 DoD
- Asserting `snap:{instanceId}:` on the HTTP path — Phase 14 docs + existing snap ITs; optional future hardening
- Dameng live IT — Phase 13
- Multi-JVM worker — Phase 15
- RBAC enable path — Phase 16
- P1 harness rows — Phase 17

**Claude's Discretion (from CONTEXT):**

- Exact poll interval / backoff within the 30–60s budget
- Exact IT class naming and package placement under `data-generator-service` tests
- Whether EXEC-01 uses the same H2 URL/table naming style as Phase 11 (prefer reuse for maintainability)
- How publish is invoked (lifecycle service vs `/api/templates/{id}/publish` MockMvc) as long as D-02 holds

---

## Summary

Phase 12 closes the v2.0 accepted limit where managed-catalog → rows proof stopped at in-process `TemplateV2Runner` (`ManagedJdbcCatalogSinkE2eIT`). The gap is **not** catalog save or JDBC sink capability — it is **HTTP enqueue + async job completion + sink evidence** on the production task spine.

### Primary recommendation

Ship **two** `@SpringBootTest` ITs in `data-generator-service` (do **not** rename or “upgrade” Phase 11 IT):

1. **EXEC-01 (H2, always-on CI):** Seed managed DS via `DataSourceConfigService.save` → persist V2 YAML template → `TemplateLifecycleService.publish` with **`require-published-for-task-run=true`** → MockMvc `POST /task/run/{id}` → parse `instanceId=` → poll `TaskExecutionService` to `SUCCESS` (fail fast on `FAILED`/`CANCELLED`) → `COUNT(*)` on managed pool.
2. **EXEC-02 (PostgreSQL Testcontainers, Docker-gated):** Same HTTP spine; managed DS URL points at PG container; sink options `dialect=postgres`, `upsert=true`, `upsertKeys=[id]`; assert SUCCESS + row/idempotency evidence (ON CONFLICT path). Gate with existing Docker-availability pattern (`DockerTestSupport` style / `@EnabledIf` + `@Testcontainers`).

Reuse poll + `instanceId=` parse from `RunReportPersistenceTests` / `TemplateEditorRunSupport`; reuse managed-sink template shape + COUNT helper from `ManagedJdbcCatalogSinkE2eIT`; reuse PG container + upsert options shape from `ChunkedPipelinePostgresUpsertTests` / `UpsertParitySupport` (shrink row count for HTTP IT).

**Do not** call `templateV2Runner.run(...)` in these new classes as the primary execution path. [VERIFIED: codebase]

---

## Architectural Responsibility Map

| Concern | Owner (reuse) | Phase 12 role |
|---------|---------------|---------------|
| HTTP enqueue | `TaskController.postRunById` → `runByIdInternal(..., requirePublished=true)` | MockMvc target `POST /task/run/{templateId}` [VERIFIED: codebase] |
| Publish gate | `TemplateLifecycleService.publish` + `requirePublishedForTaskRun` | Call publish before run; enable governance flag in IT props [VERIFIED: codebase] |
| Async execution | `TaskController.runV2` queues `TaskExecutionService` then `executor.submit(runV2Tracked)` | HTTP returns immediately; IT must poll [VERIFIED: codebase] |
| Run binding | `runV2Tracked` calls `captureConnectionSnapshot` + `WorkflowRunContext.bind` | Production path **does** bind snap; Phase 12 **must not** assert snap keys (D-11) [VERIFIED: codebase] |
| JDBC resolve | `DefaultRuntimeJdbcEndpointResolver` | Managed id → catalog resolve (snap key when bound); no resolver code changes [VERIFIED: codebase] |
| Managed catalog write | `DataSourceConfigService.save` | Same seed path as Phase 11 [VERIFIED: codebase] |
| Sink row proof | `COUNT(*)` via `DynamicDataSourceContextHolder` + `NamedParameterJdbcTemplate` | Same evidence bar as Phase 11 (D-06) [VERIFIED: codebase] |
| Snap routing proof | `JdbcSnapshotExecutePathIT` | Keep as separate regression; not Phase 12 DoD [VERIFIED: codebase] |
| Dialect ON CONFLICT | `JdbcSinkSqlBuilder` + `UpsertParitySupport` / `ChunkedPipelinePostgresUpsertTests` | Pattern source for EXEC-02 options + container [VERIFIED: codebase] |

Call path (conceptual):

```
MockMvc POST /task/run/{id}
  → TaskController.postRunById
  → requirePublishedForTaskRun
  → queueExecution + executor.submit(runV2Tracked)
  → captureConnectionSnapshot + WorkflowRunContext.bind
  → TemplateV2Runner.run (production bean)
  → markSuccess / markFailed
```

---

## Standard Stack

No new production libraries. Expected test stack (already in monorepo):

| Piece | Where already used |
|-------|--------------------|
| Java 25 / Spring Boot 4 / `@SpringBootTest` | `ManagedJdbcCatalogSinkE2eIT`, `RunReportPersistenceTests` |
| `classpath:/application-phase7-test.yaml` | Same ITs; override governance props in `@SpringBootTest(properties=...)` |
| MockMvc `webAppContextSetup` | `ConsoleWebEndpointIT`, `ConsoleAuthorizationIntegrationIT` |
| `TaskExecutionService.getByInstanceId` poll loop | `RunReportPersistenceTests.awaitSuccess` |
| `Pattern.compile("instanceId=(\\d+)")` | `TemplateEditorRunSupport`, `RunReportPersistenceTests` |
| Testcontainers `PostgreSQLContainer` `postgres:16-alpine` | `ChunkedPipelinePostgresUpsertTests` |
| Docker gate | `DockerTestSupport#dockerAvailable` + `@EnabledIf` (calcite) |
| AssertJ / JUnit 5 | Existing service ITs |

**Service POM note:** `data-generator-service` does **not** currently declare Testcontainers test deps (only calcite does, version `1.20.6`). EXEC-02 under service tests will need **test-scoped** `org.testcontainers:postgresql` + `junit-jupiter` (same version as calcite) — existing monorepo packages, not a net-new dependency family. [VERIFIED: codebase]

---

## Package Legitimacy Audit

**N/A for new product packages.** Optional planner work: add existing Testcontainers artifacts as **test** dependencies on `data-generator-service` to host EXEC-02. Do not introduce new Maven groups or npm packages.

---

## Architecture Patterns

### Pattern A — Seed + publish + MockMvc enqueue (locked)

1. `dataSourceConfigService.save(name, url, user, pass, null, driver, null, null)`
2. DDL on managed pool (`CREATE TABLE` / prefer `DROP IF EXISTS` for re-run safety)
3. Persist `TemplatePO` with V2 YAML whose JDBC writer has **only** `dataSourceId` (no inline `dataSource`)
4. `templateLifecycleService.publish(id)` with IT property `data.generator.governance.require-published-for-task-run=true`
5. `mockMvc.perform(post("/task/run/{id}"))` → assert HTTP 200 + `$.success=true`
6. Parse `instanceId=` from `$.message` (or full body string) via existing regex
7. Poll `taskExecutionService.getByInstanceId` until `SUCCESS` (immediate fail on `FAILED`/`CANCELLED`)
8. `COUNT(*)` on managed pool ≥ expected rows

Prefer lifecycle `publish(...)` over MockMvc publish (D-03 discretion) — matches `TemplatePublishUdfValidationTests`. [VERIFIED: codebase]

### Pattern B — Async poll (reuse, extend timeout)

`RunReportPersistenceTests` polls 50 × 200ms ≈ **10s** and breaks early on opposite terminal status. Phase 12 must expand to **~30–60s** (D-07) and treat `CANCELLED` like `FAILED` (immediate fail). Prefer short sleep (100–250ms) or light backoff — **not** a single fixed `Thread.sleep(N)` without status checks. [VERIFIED: codebase] [CITED: CONTEXT D-07]

Playwright `waitForJobSuccess` uses ~90s against job API — Java IT should use TaskExecution poll, not console job API (D-05). [VERIFIED: codebase]

### Pattern C — Managed sink template (keep vs change)

**Keep from Phase 11 IT:**

- Managed `dataSourceId` only; assert writer inline DS is null
- Inline/small seed rows + SQL transform + JDBC writer target table
- `COUNT(*)` evidence bar
- Unique DS name / table / H2 mem URL per class (avoid same-JVM re-run flake)

**Change for HTTP:**

- Persist YAML into `TemplatePO` (text block or serializer) — Phase 11 never persisted a template
- Publish gate + MockMvc `/task/run` instead of `templateV2Runner.run`
- Poll execution status; do not rely on `TemplateV2RunResult` return value
- Class Javadoc must say **HTTP `/task/run` spine** — never claim Phase 11 is HTTP

**Do not copy Phase 11 comment “Keep WorkflowRunContext unbound”** into the HTTP IT: production `runV2Tracked` **binds** context. D-11 means: do not assert snap keys; still assert managed-id template + SUCCESS + COUNT. [VERIFIED: codebase] [CITED: PITFALLS Pitfall 2]

### Pattern D — Publish gate reality

| Source | `require-published-for-task-run` |
|--------|----------------------------------|
| `application-phase7-test.yaml` | `false` [VERIFIED: codebase] |
| Java default (`DataGeneratorProperties`) | `true` [VERIFIED: codebase] |
| Staging | `true` [VERIFIED: codebase] |

`RunReportPersistenceTests` calls `taskController.runById` **without** publish and passes only because phase7-test disables the gate — **do not copy that shortcut** for Phase 12 (would violate D-02 spirit). Override to `true` in the new IT properties (see `TemplateLifecycleServiceTests`, `ConsoleAuthorizationIntegrationIT`). [VERIFIED: codebase]

### Pattern E — EXEC-02 dialect packaging

- Separate class (D-08), Docker-gated
- `PostgreSQLContainer<>("postgres:16-alpine")` as in `ChunkedPipelinePostgresUpsertTests`
- Managed catalog URL/user/pass/driver from container getters
- Writer options: `dialect=postgres`, `upsert=true`, `upsertKeys=["id"]` (ON CONFLICT path)
- Prefer **small** row counts (e.g. 2–10), not `UpsertParitySupport.ROW_COUNT=500`, for HTTP latency
- Evidence: SUCCESS + COUNT(*); optional second HTTP run for idempotent count (stronger, still in-scope for D-10)
- Kingbase-proxy dialect key is **optional** — CONTEXT locks PG engine (D-09); Kingbase PG-proxy already proven in-process in Phase 9/11

### Don't Hand-Roll

| Need | Reuse |
|------|-------|
| instanceId parse | `instanceId=(\\d+)` from `TemplateEditorRunSupport` / `RunReportPersistenceTests` |
| Status poll | Adapt `awaitSuccess` — do not invent MockMvc job polling |
| Managed DS seed + COUNT | `ManagedJdbcCatalogSinkE2eIT` helpers |
| PG upsert options / container image | `ChunkedPipelinePostgresUpsertTests` + `UpsertParitySupport.upsertTemplate` options map |
| MockMvc WAC setup | `MockMvcBuilders.webAppContextSetup(webApplicationContext).build()` |
| Docker availability | Copy `DockerTestSupport` into service test sources **or** duplicate the tiny `DockerClientFactory` check — do not invent a new gate API |

### Common Pitfalls (Phase 12–specific)

1. **Labeling in-process as HTTP** — new IT must contain MockMvc (or equivalent) `POST /task/run/{id}`; keep Phase 11 unchanged. [CITED: PITFALLS Pitfall 1]
2. **Wrong snap assumptions** — do not require `snap:` asserts (D-11); do not pretend HTTP leaves context unbound. [CITED: PITFALLS Pitfall 2] [CITED: CONTEXT D-11]
3. **Skipping publish** — phase7-test defaults make unpublished runs succeed; Phase 12 must enable gate + publish. [VERIFIED: codebase]
4. **Fixed sleep only** — status poll with terminal fail-fast (D-05/D-07). [CITED: CONTEXT]
5. **Same-JVM H2 flake** — unique mem DB names + `DROP IF EXISTS` / cleanup. [CITED: PITFALLS Pitfall 10]
6. **Inline `dataSource` sneak-back** — assert managed id only on proven sink. [CITED: PITFALLS Integration Gotchas]
7. **CapturingTemplateV2Runner** — `TaskControllerApiTests` / V1 retired tests replace the real runner; EXEC proofs must use the **real** `TemplateV2Runner` bean (no `@Primary` capture override). [VERIFIED: codebase]
8. **Promoting EXEC ITs to P0** — Phase 17 owns P1 matrix wiring; do not expand P0 here. [CITED: PITFALLS Pitfall 8]
9. **COUNT(*) after snap bind** — sink writes via snap-registered pool pointing at the **same JDBC URL** as the managed catalog entry; COUNT on logical managed id against that URL remains valid post-unbind. Prefer counting after SUCCESS (context cleared). Do not fail the IT if snap pool name differs. [ASSUMED: same-URL pool sharing; matches H2 mem + PG container semantics]

---

## Code Examples

### 1. HTTP enqueue return shape [VERIFIED: codebase]

`TaskController` (`POST /task/run/{templateId}`):

```java
@PostMapping("/run/{templateId}")
public R<String> postRunById(@NotNull @PathVariable Long templateId) {
    return runById(templateId);
}
// ...
return R.ok(String.format("Template '%s' started. templateId=%s, instanceId=%s",
        runtime.name(), runtime.id(), runtime.instanceId()));
```

`R.ok(String message)` puts the string in **`message`** (data null). MockMvc should read `$.message` (or parse full JSON). `TemplateEditorRunSupport` falls back: `data != null ? data : message`.

### 2. Existing poll + parse [VERIFIED: codebase]

From `RunReportPersistenceTests`:

```java
private static final Pattern INSTANCE_ID_PATTERN = Pattern.compile("instanceId=(\\d+)");

// start via controller today — Phase 12 replaces this call with MockMvc POST /task/run/{id}
R<String> start = taskController.runById(entity.getId());
Long instanceId = extractInstanceId(start.getMessage());
TaskExecutionSummary summary = awaitSuccess(instanceId);
```

Extend loop budget to 30–60s; add `CANCELLED` immediate failure.

### 3. Production async path binds snap [VERIFIED: codebase]

From `TaskController.runV2Tracked`:

```java
taskExecutionService.markRunning(instanceId);
taskExecutionService.captureConnectionSnapshot(instanceId, template, connectionCatalog);
WorkflowRunContext.bind(instanceId, control);
try {
    TemplateV2RunResult result = templateV2Runner.run(template);
    // ...
    taskExecutionService.markSuccess(instanceId, rowCount, metricsJson, reportJson);
} finally {
    // unbind ...
}
```

### 4. Phase 11 baseline to keep (regression only) [VERIFIED: codebase]

`ManagedJdbcCatalogSinkE2eIT`: `save` → unbound `templateV2Runner.run` → `COUNT(*)`. **Do not** modify this class to call HTTP.

### 5. MockMvc WAC precedent [VERIFIED: codebase]

```java
mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
mockMvc.perform(post("/task/run/{id}", templateId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
```

### 6. Publish gate enable + lifecycle publish [VERIFIED: codebase]

```java
@SpringBootTest(
    classes = DataGeneratorApplication.class,
    properties = {
        "spring.config.location=classpath:/application-phase7-test.yaml",
        "data.generator.governance.require-published-for-task-run=true"
    })
// ...
templateRepository.saveAndFlush(entity);
templateLifecycleService.publish(entity.getId());
```

### 7. PG upsert options shape [VERIFIED: codebase]

From `UpsertParitySupport.upsertTemplate`:

```java
writer.setOptions(new LinkedHashMap<>(Map.of(
        "dialect", dialect,      // "postgres" for EXEC-02
        "upsert", true,
        "upsertKeys", List.of("id"))));
```

Container:

```java
new PostgreSQLContainer<>("postgres:16-alpine")
    .withDatabaseName("...")
    .withUsername("test")
    .withPassword("test");
```

### 8. Suggested IT packaging (discretion) [ASSUMED naming]

| Req | Suggested class | Package |
|-----|-----------------|---------|
| EXEC-01 | `ManagedJdbcCatalogHttpExecuteIT` | `org.gensokyo.data.datasource.catalog` (beside Phase 11) |
| EXEC-02 | `ManagedJdbcCatalogHttpPostgresUpsertIT` | same package |

Javadoc first line must mention `POST /task/run` + managed `dataSourceId` + SUCCESS/COUNT so maintainers see the evidence path without reading the whole method.

---

## Validation Architecture

### Test plan (Nyquist)

| ID | Test to write | Req | Command / evidence |
|----|---------------|-----|-------------------|
| T1 | `ManagedJdbcCatalogHttpExecuteIT` (name discretionary) | EXEC-01 | MockMvc `POST /task/run/{id}`; `SUCCESS`; managed `COUNT(*)`; no direct `TemplateV2Runner` invoke |
| T2 | Assert publish required | EXEC-01 / D-02 | IT props `require-published-for-task-run=true`; draft run would fail; published run succeeds |
| T3 | Assert sink is managed-id-only | EXEC-01 | Template YAML / PO sink: `dataSourceId` set, no inline DS |
| T4 | `ManagedJdbcCatalogHttpPostgresUpsertIT` | EXEC-02 | Docker-gated; managed DS → PG; upsert options; HTTP spine; SUCCESS + COUNT (and/or second-run idempotent count) |
| T5 | Regression: Phase 11 IT unchanged | — | Existing `ManagedJdbcCatalogSinkE2eIT` still green |

### Suggested Maven commands

```powershell
# EXEC-01 focused (H2, no Docker required)
.\mvnw-jdk25.ps1 -pl data-generator-service -am -Dtest=ManagedJdbcCatalogHttpExecuteIT -Dskip.console.frontend=true test

# EXEC-02 focused (needs Docker)
.\mvnw-jdk25.ps1 -pl data-generator-service -am -Dtest=ManagedJdbcCatalogHttpPostgresUpsertIT -Dskip.console.frontend=true test

# Both new ITs + Phase 11 regression
.\mvnw-jdk25.ps1 -pl data-generator-service -am "-Dtest=ManagedJdbcCatalog*IT" -Dskip.console.frontend=true test
```

[ASSUMED: exact class names match planner discretion]

### Evidence packaging (maintainer-obvious)

Each new IT class Javadoc should state, in order:

1. Entry: `POST /task/run/{id}` via MockMvc  
2. Catalog: managed `dataSourceId` from `DataSourceConfigService.save`  
3. Gate: publish before run  
4. Async: poll `TaskExecutionService` → `SUCCESS`  
5. Rows: managed-pool `COUNT(*)`  
6. Explicit non-claim: not in-process-only; snap key assertion deferred (D-11)

Method names should encode the path, e.g. `httpTaskRun_managedCatalogSink_reachesSuccessWithCountableRows`.

### Out of validation scope (Phase 12)

- Console `/api/templates/{id}/run`  
- `snap:` string asserts  
- Dameng / multi-JVM / RBAC / harness P1 rows  

---

## Project Constraints

From `.cursor/rules/` and project agent docs (apply to Phase 12 plans/ITs):

### Behavioral / surgical change

- Karpathy guidelines: minimum code that solves the problem; no speculative abstractions; touch only what the phase requires (`karpathy-guidelines.mdc`).
- Do not “improve” adjacent code; keep Phase 11 IT as regression.

### Java documentation

- Every new `.java` file: copyright block → package → imports → type Javadoc (`@author` / `@version` / `@since`) → public method Javadoc → inline `//` for non-obvious steps (`java-copyright-class-javadoc.mdc`).

### Git commits (when committing later)

- Conventional Commits + footer `AI-Assisted-by` / `Co-authored-by` (`git-commit-conventional-ai.mdc`).

### CodeGraph

- Prefer CodeGraph for structural lookups when `.codegraph/` exists (`codegraph.mdc`). **This research session:** index absent at repo root — used Read/Grep instead; initializing CodeGraph is optional tooling, not Phase 12 DoD (`PITFALLS` Pitfall 11). [VERIFIED: MCP returned not initialized]

### Build / test

- JDK 25 via `mvnw-jdk25.ps1` / `.mvn/settings-jdk25.xml`.
- Prefer embedded-first tests; Testcontainers PG for dialect IT (`docs/testing-embedded-components.md` / AGENTS.md).
- Use `-Dskip.console.frontend=true` for focused service IT runs.
- P0 harness / `verify-harness.ps1` unchanged this phase (P1 wiring is Phase 17).

### Console verify rule

- `console-verify.mdc` applies when editing console paths — **not** expected for Phase 12 (service ITs only).

---

## Research Focus Answers

1. **MockMvc + TaskController + poll:** Closest in-repo poll/parse is `RunReportPersistenceTests` (direct `TaskController`, not MockMvc). Closest MockMvc WAC is `ConsoleAuthorizationIntegrationIT` / `ConsoleWebEndpointIT`. Compose: WAC MockMvc `POST /task/run/{id}` + parse/poll from RunReport tests with 30–60s budget. Direct `taskController.postRunById` alone is insufficient for D-03 MockMvc requirement. [VERIFIED: codebase]

2. **ManagedJdbcCatalogSinkE2eIT keep vs change:** Keep managed save, template shape, COUNT, unique names. Change: persist+publish+MockMvc+poll; drop in-process runner as primary; drop “unbound context” framing for HTTP class docs. [VERIFIED: codebase]

3. **Publish gate:** `TemplateLifecycleService.requirePublishedForTaskRun` no-ops when property false; `publish(id)` validates + sets `PUBLISHED`. Phase 12 IT must set property true and call publish (lifecycle preferred). [VERIFIED: codebase]

4. **PG + ON CONFLICT precedents:** `ChunkedPipelinePostgresUpsertTests` + `UpsertParitySupport` — in-process only today; lift container + options onto HTTP spine with managed catalog id and small fixtures. [VERIFIED: codebase]

5. **Pitfalls:** In-process mislabel; snap over/under-assert; flaky sleeps; unpublished runs under phase7 defaults; capturing runner overrides; H2 re-run DDL. [CITED: PITFALLS + CONTEXT]

6. **Validation Architecture:** See section above (required for Nyquist).

---

## Open Questions for Planner (non-blocking)

1. Exact Surefire display names / whether EXEC-02 should also assert `rowsUpserted` from persisted report JSON vs COUNT-only.
2. Whether to vendor a tiny `DockerTestSupport` copy under service tests vs shared test-jar (calcite does not currently export one).
3. Optional: assert draft `POST /task/run` returns failure when gate enabled (strengthens D-02) — not required if publish+success path is clear.

---

## Sources

| Source | Use |
|--------|-----|
| `12-CONTEXT.md` | Locked D-01..D-11 |
| `.planning/REQUIREMENTS.md` | EXEC-01, EXEC-02 |
| `.planning/ROADMAP.md` | Phase 12 success criteria |
| `.planning/research/SUMMARY.md` / `PITFALLS.md` | HTTP-first; pitfalls 1–2, 8, 10 |
| `.planning/milestones/v2.0-MILESTONE-AUDIT.md` | Accepted in-process limit for flow #1 |
| Canonical Java files listed in CONTEXT | Patterns verified by Read/Grep |

**Overall confidence:** HIGH for planning. Remaining risk is operational (Docker availability for EXEC-02; poll timeout under loaded CI) — mitigate with Docker gate + 30–60s budget.

---
*Phase 12 research — HTTP Execute-Path Proof*  
*Output only: this file (no PLAN.md)*
