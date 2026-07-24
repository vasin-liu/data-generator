# Phase 11 Research: v2.0 closeout hardening — DS-02 managed JDBC E2E IT + dialect preset/upsert depth

**Researched:** 2026-07-25  
**Phase:** 11 — v2-0-closeout-hardening-ds-02-managed-jdbc-e2e-it-dialect-pr  
**Status:** Ready for planning  
**CodeGraph:** not initialized at repo root (used Read/Grep/Glob) [VERIFIED: no `.codegraph/` under project root]

---

## User Constraints

### Implementation Decisions

#### Managed E2E proof surface
- **D-01:** New dedicated `@SpringBootTest` (do not extend `V2ScenarioTemplateIT`; not Playwright-primary).
- **D-02:** Create managed DS via `DataSourceConfigService.save`.
- **D-03:** Managed `dataSourceId` on **JDBC sink only**; source may be inline/fixture.
- **D-04:** Keep connectivity-before-save **off** (`application-phase7-test.yaml` defaults).

#### Managed E2E run path
- **D-05:** In-process `TemplateV2Runner` (not HTTP `/api/templates/.../run` or `/task/run`).
- **D-06:** Assert via `COUNT(*)` on the sink table through the managed DS (same spirit as `V2ScenarioTemplateIT.countRows`).
- **D-07:** Template must use managed `dataSourceId` only on sink — **no inline `dataSource` block**.
- **D-08:** Plain **INSERT** (not upsert) for the managed E2E IT.

#### Dialect depth target
- **D-09:** Primary non-PG proof = **Kingbase/HighGo** (PG-proxy + dialect keys), not Dameng live / not ClickHouse.
- **D-10:** Expand Playwright to **`kingbase8`** (beyond `postgresql16`).
- **D-11:** Connectivity = existing Maven (`ConnectionCatalogTestTests` kingbase failure path); Playwright **does not** click Test Connection.
- **D-12:** Dameng code/IT **unchanged** this phase.

#### Dialect journey shape
- **D-13:** Evidence pack + verification narrative (not a single-JVM chain).
- **D-14:** New `scripts/verify-phase11-uat-closeout-hardening.ps1` with `-SkipPlaywright`.
- **D-15:** Keep `postgresql16` Playwright **and** add `kingbase8`.
- **D-16:** No new upsert IT — reuse `ChunkedPipelineKingbaseDialectTests`.

#### Harness / CI / docs
- **D-17:** Verification/UAT only — **do not** change P0 / `verify-harness.ps1` gate (align Phase 10: no Phase 6–7 into P0; Playwright not P0).
- **D-18:** Update **`AGENTS.md`** with phase11 command as supplementary UAT; after execute, update **`v2.0-MILESTONE-AUDIT.md`** flow disposition per ROADMAP SC3.
- **D-19:** Reuse npm script **`e2e:phase9-jdbc-dialect`** (same `jdbc-dialect-preset.spec.ts`) — no new npm entry.
- **D-20:** **Surgical** update of audit flows #1 and #8 in `v2.0-MILESTONE-AUDIT.md` during Phase 11 closeout (evidence pointers + disposition). Optional full `/gsd-audit-milestone` before complete-milestone remains a later operator choice; overall audit `status` may stay `tech_debt` for remaining Dameng/Nyquist items.

### Claude's Discretion
- Exact IT class/package name and fixture table DDL, as long as D-01–D-08 hold.
- Whether Playwright uses parameterized `test.each` vs a second `test(...)` for `kingbase8`.
- Exact Maven surefire includes for the phase11 verify script slice.
- Wording of verification narrative linking the three dialect evidence pieces.

### Deferred Ideas
- Dual JDBC resolver consolidation.
- Folding managed-catalog / Phase 6–7 rows into P0 / `verify-harness.ps1`.
- Dameng live IT as default-CI or Phase 11 deliverable.
- ClickHouse as the primary non-PG preset journey.
- HTTP console/task run path as the canonical managed E2E proof.
- Full `/gsd-audit-milestone` rewrite inside Phase 11 (optional later; D-20 is surgical).
- Nyquist / VALIDATION.md hygiene for Phases 7/8/07.1.

---

## Project Constraints (from .cursor/rules/)

| Rule | Implication for Phase 11 |
|------|--------------------------|
| `karpathy-guidelines.mdc` | Minimum code; no speculative abstractions; surgical diffs only for proof/docs/UAT. [CITED: `.cursor/rules/karpathy-guidelines.mdc`] |
| `java-copyright-class-javadoc.mdc` | New IT class needs PCI copyright, type Javadoc (`@author` / `@since`), Javadoc on public members, `//` only for non-obvious steps. [CITED: `.cursor/rules/java-copyright-class-javadoc.mdc`] |
| `git-commit-conventional-ai.mdc` | Conventional commits + `AI-Assisted-by` / `Co-authored-by` footers when committing. [CITED: `.cursor/rules/git-commit-conventional-ai.mdc`] |
| `console-verify.mdc` | Editing `jdbc-dialect-preset.spec.ts` or console E2E triggers console verification expectations; phase11 UAT script with Playwright path satisfies operator UAT; `-SkipPlaywright` is CI-merge-friendly. Prefer `verify-phase11-…` for this phase’s dialect preset change rather than inventing a second pipeline. [CITED: `.cursor/rules/console-verify.mdc`] |
| `codegraph.mdc` | Prefer CodeGraph when indexed; **repo root has no `.codegraph/`** — research used native tools. [VERIFIED: codebase] |
| GSD workflow (CLAUDE.md) | Edits go through GSD phase plans; Phase 11 plans must stay inside D-01–D-20 fences. [CITED: `CLAUDE.md` GSD workflow] |
| AGENTS.md / JDK 25 | Builds via `.\mvnw-jdk25.ps1` or `.\mvnw.cmd -s .mvn\settings-jdk25.xml`; do not change global JAVA_HOME. [CITED: `AGENTS.md`] |
| Embedded-first | Managed E2E uses H2 mem URL via catalog save — no shared staging JDBC. [CITED: `docs/testing-embedded-components.md`] |

**Author for new Java:** `Gensokyo` / `liuweixing@pcitech.com` from `git config`. [VERIFIED: shell `git config`]

---

## Standard Stack / Approach (prescriptive)

Planner should treat Phase 11 as **three deliverable tracks** (no product features):

### Track A — DS-02 managed JDBC sink E2E IT (SC1 / flow #1)

1. **New** `@SpringBootTest` class in `data-generator-service` (recommended package: `org.gensokyo.data.datasource.catalog`, alongside `JdbcSnapshotExecutePathIT`). Suggested name: `ManagedJdbcCatalogSinkE2eIT` (discretion).
2. Bootstrap: `@SpringBootTest(classes = DataGeneratorApplication.class, properties = "spring.config.location=classpath:/application-phase7-test.yaml")` — same as `V2ScenarioTemplateIT` / `JdbcSnapshotExecutePathIT`. [VERIFIED: codebase]
3. `@BeforeEach`: delete prior catalog row + remove from `DynamicRoutingDataSource` (copy cleanup from `JdbcSnapshotExecutePathIT.resetDatasourceRow`). [CITED: `JdbcSnapshotExecutePathIT.java:82-88`]
4. Create managed DS: `dataSourceConfigService.save(name, h2MemUrl, "sa", "", null, "org.h2.Driver", null, null)`. [CITED: `DataSourceConfigService.java:84-147`] — `save` already calls `connectionCatalog.reload` → `HotReloadCoordinator.reloadJdbc` → `registerToRuntime`. [CITED: `DataSourceConfigService.java:138-139`, `HotReloadCoordinator.java:169-174`]
5. Build `TemplateV2VO` in Java (not YAML scenario file):
   - Source: `inline_rows` (or CSV) — **no** managed id required (D-03).
   - Sink: `JdbcWriterVO` with **only** `setDataSourceId(managedName)` + `target` table; **no** `setDataSource(...)` (D-07).
   - Writer mode: plain INSERT (omit upsert options) (D-08).
6. DDL on managed pool: `DynamicDataSourceContextHolder.push(managedName)` + `NamedParameterJdbcTemplate`/`JdbcTemplate.execute("create table …")`.
7. Run: `templateV2Runner.run(template)` (D-05). **Do not** call `TaskExecutionService.queueExecution` / `WorkflowRunContext.bind` — that would switch resolve to `snap:` keys (DS-03 path). [CITED: `DefaultRuntimeJdbcEndpointResolver.java:95-110`]
8. Assert: `COUNT(*)` on managed id (copy `countRows` spirit from `V2ScenarioTemplateIT`) + optional `result.getMetrics().getRowsWritten()`. [CITED: `V2ScenarioTemplateIT.java:318-327`]

### Track B — RW-05/RW-06 dialect evidence pack (SC2 / flow #8)

| Piece | Artifact | Action |
|-------|----------|--------|
| Preset UI | `jdbc-dialect-preset.spec.ts` | Keep `postgresql16`; add `kingbase8` (test.each or second test) (D-10, D-15, D-19) |
| npm | `e2e:phase9-jdbc-dialect` | **Reuse only** — no new script (D-19) [CITED: `package.json`] |
| Connectivity | `ConnectionCatalogTestTests.jdbcDraftTest_kingbaseDriverFailureIsActionableWithoutSecrets` | Include in Maven slice; do **not** add Playwright Test Connection (D-11) |
| Upsert | `ChunkedPipelineKingbaseDialectTests` | Reuse as-is (D-16); already P0-linked for kingbase/highgo [CITED: `.planning/test-matrix.yaml`] |
| UAT entry | `scripts/verify-phase11-uat-closeout-hardening.ps1` | Clone phase9 script shape; `-SkipPlaywright` (D-14) |

**Recommended Maven `-Dtest=` slice (RESOLVED by 11-02 — include preset catalog tests):**

```
ManagedJdbcCatalogSinkE2eIT,ConnectionCatalogTestTests,ChunkedPipelineKingbaseDialectTests,JdbcDriverPresetCatalogTests
```

Use `-pl data-generator-service -am` + `-Dsurefire.failIfNoSpecifiedTests=false` exactly like phase9. [CITED: `scripts/verify-phase9-uat-jdbc-dialect.ps1:44-61`]

**Evidence narrative (for VERIFICATION / audit):** three complementary proofs, not one JVM:

1. Playwright: operator selects `kingbase8` preset → form fill → POST `/api/datasources` success.  
2. Maven: kingbase draft connectivity failure is actionable and secret-free.  
3. Maven/Testcontainers: `dialect=kingbase|highgo` upsert idempotency via PG proxy.

### Track C — Docs / audit closeout (SC3)

1. `AGENTS.md`: add `.\scripts\verify-phase11-uat-closeout-hardening.ps1 -SkipPlaywright` next to phase 8/9 commands; mention supplementary (not P0). (D-18)
2. `v2.0-MILESTONE-AUDIT.md`: surgical edits to flow #1 and #8 rows + matching `tech_debt` bullets; overall `status` may remain `tech_debt` if Dameng/Nyquist remain. (D-20)
3. Do **not** edit `.planning/test-matrix.yaml` or `verify-harness.ps1`. (D-17)

### ROADMAP wording vs CONTEXT (planner note)

ROADMAP SC1 says sink/**source** managed id; ROADMAP SC2 reads like a single preset→connectivity→upsert chain. **CONTEXT overrides:** sink-only managed id (D-03); evidence pack without Playwright Test Connection (D-11, D-13). Document accepted limits in audit if needed.

---

## Architecture Patterns (with file:line anchors)

### Managed catalog write → runtime pool

```
DataSourceConfigService.save
  → repository.saveAndFlush
  → connectionCatalog.reload(name, JDBC)
       → HotReloadCoordinator.reloadJdbc
            → dataSourceConfigService.registerToRuntime(row)
                 → DynamicRoutingDataSource.addDataSource(name, DruidDataSource)
```

[CITED: `DataSourceConfigService.java:84-147`, `ConnectionCatalogImpl.java:113-121`, `HotReloadCoordinator.java:169-174`, `DataSourceConfigService.java:251-268`]

### Managed vs inline resolve on execute path

`DefaultRuntimeJdbcEndpointResolver`:

- Non-blank `dataSourceId` → `catalog.resolve` → `registerIfAbsent` → return `connectionName()` (logical name when no `WorkflowRunContext`; `snap:{instanceId}:{name}` when bound). [CITED: `DefaultRuntimeJdbcEndpointResolver.java:56-110`]
- Blank id + inline `dataSource` → `ensureInlineDataSource` (this is what `V2ScenarioTemplateIT` / scenario YAML use — **contrast**, not the Phase 11 proof). [CITED: `DefaultRuntimeJdbcEndpointResolver.java:128-139`]
- Dual resolver: `JdbcCatalogResolver` (catalog-side) stays separate — **out of scope**. [CITED: class Javadoc lines 38-43]

### Sink write uses resolver return value

`JdbcRowSinkAdapter.writeBatch` pushes `resolveSinkDataSourceId(writer)` onto `DynamicDataSourceContextHolder`. [CITED: `JdbcRowSinkAdapter.java:137-149`]

### Connectivity-before-save gate

`save` only calls `connectivityTestGate.requireRecentSuccess` when `properties.getGovernance().isRequireConnectivityTestBeforeSave()`. Phase7 test yaml sets `require-connectivity-test-before-save: false`. [CITED: `DataSourceConfigService.java:133-137`, `application-phase7-test.yaml:38`]

### Dialect evidence already in tree

| Concern | Location |
|---------|----------|
| Kingbase/HighGo upsert PG-proxy | `data-generator-calcite/.../ChunkedPipelineKingbaseDialectTests.java` |
| Upsert helper | `UpsertParitySupport.assertUpsertIdempotent(..., "kingbase"|"highgo")` |
| Kingbase actionable connectivity | `ConnectionCatalogTestTests.java:109-113` |
| Preset id `kingbase8` | `JdbcDriverPresetCatalog.java:32-39`; console fallback `jdbcDriverPresets.ts:38-47`; i18n `Kingbase 8` / `金仓 8` |
| Playwright PG-only today | `jdbc-dialect-preset.spec.ts:12` (`PRESET_ID = 'postgresql16'`) |
| P0 already links KB/HG upsert | `test-matrix.yaml` `v2-dialect-kingbase` / `v2-dialect-highgo` → `ChunkedPipelineKingbaseDialectTests` |

### Audit PARTIAL flows to close

| Flow | Current disposition | Phase 11 target |
|------|---------------------|-----------------|
| #1 Managed JDBC DS → template run → rows | PARTIAL (resolve OK; dedicated managed→task→rows IT missing) | OK via Track A (in-process runner, not HTTP task) [CITED: `v2.0-MILESTONE-AUDIT.md:119`] |
| #8 Dialect preset + upsert (≥1 dialect) | PARTIAL (PG preset E2E only; pieces exist) | OK via Track B evidence pack; document Dameng still unit-only as accepted limit [CITED: `v2.0-MILESTONE-AUDIT.md:126`] |

---

## Don't Hand-Roll

| Need | Reuse |
|------|-------|
| Managed DS persistence + runtime register | `DataSourceConfigService.save` — do not call `registerToRuntime` alone in the IT |
| Spring Boot test profile | `classpath:/application-phase7-test.yaml` |
| Catalog cleanup pattern | `JdbcSnapshotExecutePathIT.resetDatasourceRow` |
| `COUNT(*)` assertion | Copy private helper pattern from `V2ScenarioTemplateIT.countRows` (do **not** extend that class — D-01) |
| In-process V2 run | Autowire `TemplateV2Runner` |
| Phase11 UAT script | Clone `scripts/verify-phase9-uat-jdbc-dialect.ps1` (params, `Invoke-RepoMaven`, Podman Playwright block, `-SkipPlaywright`) |
| Playwright helpers | Existing `apiGetWithRole`, `gotoConsoleHome`, `navigateViaTopNav`, `TestIds` |
| Kingbase upsert | Existing `ChunkedPipelineKingbaseDialectTests` — no new upsert IT |
| npm E2E entry | `e2e:phase9-jdbc-dialect` only |
| Preset metadata | Server `JdbcDriverPresetCatalog` + client i18n labels (`金仓 8` / `Kingbase 8`) |

---

## Common Pitfalls

1. **Inline vs managed confusion** — `V2ScenarioTemplateIT` registers inline endpoints via `resolve*DataSourceId` then mutates VO ids; scenario YAML embeds `dataSource:` blocks. Phase 11 IT must leave sink with **managed id only** and no inline block, or the audit gap remains. [VERIFIED: `scenario-d-chunked-jdbc.yaml` uses inline `dataSource:`]

2. **Extending `V2ScenarioTemplateIT`** — Forbidden (D-01). Scenario suite is parameterized YAML + inline registration contrast.

3. **Connectivity-before-save ON** — If a plan accidentally overrides governance to `true`, `save` fails without a prior gate success fingerprint. Keep phase7-test defaults (D-04).

4. **Binding `WorkflowRunContext` / queuing a task** — Turns managed resolve into `snap:` keys; COUNT via logical name may miss the pool. Keep pure `TemplateV2Runner.run` unbound (D-05).

5. **HTTP run as primary proof** — Explicitly deferred. Audit text historically said “TaskController/console job”; CONTEXT accepts in-process runner as the closing proof — update audit wording surgically to match.

6. **Playwright Test Connection / live Kingbase upsert** — Out of scope; brittle and secret-prone. Connectivity proof is Maven failure path without secrets (D-11).

7. **Playwright secrets in assertions** — Mirror phase9: use disposable `e2e_user` / `e2e_test_only`; never assert password fields in API responses. Kingbase label click must use `/Kingbase 8|金仓 8/i` (not PostgreSQL regex).

8. **New npm script or P0 matrix row** — Violates D-17/D-19. Phase11 script is supplementary UAT only.

9. **Dual-resolver “cleanup”** — Tempting while reading `DefaultRuntimeJdbcEndpointResolver` Javadoc; deferred.

10. **`@Transactional` on the E2E IT method** — `JdbcSnapshotExecutePathIT` uses it for resolver-only checks; a full write/`COUNT(*)` across a separate H2 mem pool is safer **without** test-class transaction wrapping the runner (follow `V2ScenarioTemplateIT`). [ASSUMED: transaction boundaries could hide commits on metadata DS; keep runner path non-transactional like scenario IT]

11. **Surefire module mismatch** — `ChunkedPipelineKingbaseDialectTests` lives in `data-generator-calcite`; must use `-pl data-generator-service -am` so the class is on the reactor, plus `failIfNoSpecifiedTests=false`.

12. **Docker for KB dialect tests** — Class is `@EnabledIf(DockerTestSupport#dockerAvailable)`; CI/UAT hosts without Docker skip — phase9 already accepts this; narrative should note Docker requirement for upsert piece.

---

## Code Examples / Analog Excerpts

### Managed save + cleanup (`JdbcSnapshotExecutePathIT`)

```java
// CITED: data-generator-service/.../JdbcSnapshotExecutePathIT.java
dataSourceConfigService.save(DS_NAME, url, "sa", "", null, "org.h2.Driver", null, null);

// cleanup pattern
dataSourceConfigRepository.findById(DS_NAME).ifPresent(dataSourceConfigRepository::delete);
if (dynamicRoutingDataSource.getDataSources().containsKey(DS_NAME)) {
    dynamicRoutingDataSource.removeDataSource(DS_NAME);
}
```

### COUNT(*) pattern (`V2ScenarioTemplateIT`)

```java
// CITED: V2ScenarioTemplateIT.java:318-327
private long countRows(String dataSourceId, String table) {
    try {
        DynamicDataSourceContextHolder.push(dataSourceId);
        Long count = namedParameterJdbcTemplate.getJdbcTemplate()
                .queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0L : count;
    } finally {
        DynamicDataSourceContextHolder.clear();
    }
}
```

### Managed id on sink-only (shape for new IT)

```java
// Prescriptive sketch — planner/implementer fills DDL/row counts
JdbcWriterVO writer = new JdbcWriterVO();
writer.setDataSourceId(managedName); // managed catalog key from save()
writer.setTarget("managed_e2e_sink");
// do NOT writer.setDataSource(...);
```

### Phase9 UAT Maven slice pattern

```powershell
# CITED: scripts/verify-phase9-uat-jdbc-dialect.ps1:44-61
$testList = @('JdbcSinkSqlBuilderTests', ... 'ConnectionCatalogTestTests') -join ','
Invoke-RepoMaven -RepoRoot $RepoRoot -pl data-generator-service -am `
    "-Dtest=$testList" '-Dsurefire.failIfNoSpecifiedTests=false' test
```

### Playwright preset today (extend for `kingbase8`)

```typescript
// CITED: jdbc-dialect-preset.spec.ts:12-38
const PRESET_ID = 'postgresql16';
// ...
.getByText(/PostgreSQL 16\.x/i)
```

For `kingbase8`: preset id `kingbase8`, UI text `/Kingbase 8|金仓 8/i`, assert `preset.driverClassName === 'com.kingbase8.Driver'` and URL template match from API. [CITED: `JdbcDriverPresetCatalog.java:32-39`, `en.json` / `zh-CN.json`]

### Kingbase connectivity without secrets

```java
// CITED: ConnectionCatalogTestTests.java:109-113
void jdbcDraftTest_kingbaseDriverFailureIsActionableWithoutSecrets() {
    assertProprietaryDriverFailureWithoutSecrets(
            "jdbc:kingbase8://127.0.0.1:59999/YOUR_DATABASE",
            "com.kingbase8.Driver");
}
```

### Governance defaults (keep)

```yaml
# CITED: application-phase7-test.yaml:34-39
governance:
  require-connectivity-test-before-save: false
  require-connectivity-test-before-publish: false
```

---

## Validation Architecture

Nyquist-style sampling for Phase 11: every success criterion maps to an automated command; no unmeasured “done”.

### What to prove

| SC / Decision | Signal | How |
|---------------|--------|-----|
| SC1 / D-01–D-08 | Managed catalog id → V2 run → sink rows | New `@SpringBootTest` IT; assert `COUNT(*)` ≥ expected |
| SC2 / D-09–D-16 | Non-PG preset + connectivity + dialect upsert | Playwright `kingbase8` + existing Maven classes + narrative |
| SC3 / D-18–D-20 | Audit + AGENTS updated | File content checks after execute |
| D-17 | P0 untouched | `git diff` must not include `test-matrix.yaml` / `verify-harness.ps1` |

### Wave commands (planner should embed in PLAN tasks)

| Wave | Command | Expect |
|------|---------|--------|
| A — managed IT | `.\mvnw-jdk25.ps1 -pl data-generator-service -am "-Dtest=ManagedJdbcCatalogSinkE2eIT" -Dsurefire.failIfNoSpecifiedTests=false test` | BUILD SUCCESS; COUNT assert green |
| B — dialect Maven | `.\scripts\verify-phase11-uat-closeout-hardening.ps1 -SkipPlaywright` | Exit 0; includes ConnectionCatalog + Kingbase dialect tests |
| B — Playwright (optional full UAT) | same script **without** `-SkipPlaywright` (Podman) **or** `npm run e2e:phase9-jdbc-dialect` against running console | Both `postgresql16` and `kingbase8` save paths pass |
| C — docs | `rg -n "verify-phase11-uat-closeout-hardening" AGENTS.md` + audit flow #1/#8 no longer `PARTIAL` (or documented accepted limit) | Matches D-18/D-20 |
| Regression fence | Do **not** require `verify-harness.ps1` for phase close; optional smoke only | P0 unchanged |

### Recommended `11-VALIDATION.md` rows (for later validate-phase)

| ID | Requirement | Type | Command |
|----|-------------|------|---------|
| T-11-01 | DS-02 managed sink E2E | maven | `-Dtest=ManagedJdbcCatalogSinkE2eIT` |
| T-11-02 | KB connectivity actionable | maven | `-Dtest=ConnectionCatalogTestTests` |
| T-11-03 | KB/HG upsert reuse | maven | `-Dtest=ChunkedPipelineKingbaseDialectTests` |
| T-11-04 | Phase11 UAT script | script | `verify-phase11-uat-closeout-hardening.ps1 -SkipPlaywright` |
| T-11-05 | Preset E2E PG+KB | playwright | `e2e:phase9-jdbc-dialect` |
| T-11-06 | AGENTS lists phase11 | docs | `rg verify-phase11-uat-closeout-hardening AGENTS.md` |
| T-11-07 | Audit flows #1/#8 | docs | surgical disposition update |

### Sampling rate

- After Track A lands: run T-11-01 once.  
- After Track B script + Playwright edit: run T-11-04 (and T-11-05 when Podman available).  
- After Track C: rg checks only.  
- Full phase gate: T-11-04 + T-11-01 + docs rg.

---

## Open Questions (RESOLVED)

None blocking planning — CONTEXT locks resolve prior ambiguities. Discretion items **resolved by plans 11-01 / 11-02**:

1. **IT class name / package / table DDL** — **RESOLVED (11-01):** `ManagedJdbcCatalogSinkE2eIT` in `org.gensokyo.data.datasource.catalog`; `DS_NAME = managed-jdbc-catalog-sink-e2e-ds`; `TABLE = managed_e2e_sink`; H2 mem URL `jdbc:h2:mem:managed-jdbc-catalog-sink-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1` (within D-01–D-08).
2. **Playwright shape for `kingbase8`** — **RESOLVED (11-02):** either `test.each` **or** two `test(...)` blocks is OK; both `postgresql16` and `kingbase8` must be covered (D-10, D-15).
3. **`JdbcDriverPresetCatalogTests` in Maven slice** — **RESOLVED (11-02):** **include** it in `verify-phase11-uat-closeout-hardening.ps1` `-Dtest=` list (optional-but-recommended → required for this phase’s narrative completeness).

**Resolved via codebase (do not re-litigate):**

- Managed save path registers runtime pool via reload (`HotReloadCoordinator.reloadJdbc`). [VERIFIED]  
- `kingbase8` preset id and UI labels already exist server + client. [VERIFIED]  
- Upsert IT already exists and is P0-linked — reuse only. [VERIFIED]  
- Connectivity-before-save is off in phase7-test yaml. [VERIFIED]  
- ROADMAP SC1/SC2 phrasing is broader than CONTEXT; CONTEXT wins. [CITED: CONTEXT vs ROADMAP]

---

## RESEARCH COMPLETE

**Path:** `.planning/phases/11-v2-0-closeout-hardening-ds-02-managed-jdbc-e2e-it-dialect-pr/11-RESEARCH.md`
