# Phase 12: HTTP Execute-Path Proof - Pattern Map

**Mapped:** 2026-07-25  
**Files analyzed:** 4 planned (2 new ITs + optional DockerTestSupport copy + service POM test deps)  
**Analogs found:** 8 / 8 focus targets  
**CodeGraph:** not indexed at repo root — excerpts from Read/Grep

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `…/ManagedJdbcCatalogHttpExecuteIT.java` (new, name discretionary) | test (IT) | request-response (MockMvc enqueue → poll → COUNT) | Compose: `ManagedJdbcCatalogSinkE2eIT` + `RunReportPersistenceTests` + `ConsoleWebEndpointIT` / `ConsoleAuthorizationIntegrationIT` + `TemplateLifecycleService` publish | exact (compose) |
| `…/ManagedJdbcCatalogHttpPostgresUpsertIT.java` (new) | test (IT) | request-response + container | Same HTTP spine + `ChunkedPipelinePostgresUpsertTests` / `UpsertParitySupport` | exact (compose) |
| `data-generator-service/pom.xml` (test deps) | build | n/a | `data-generator-calcite/pom.xml` Testcontainers `1.20.6` block | exact |
| `…/support/DockerTestSupport.java` (optional copy under service tests) | test util | n/a | `data-generator-calcite/.../DockerTestSupport.java` | exact |
| `ManagedJdbcCatalogSinkE2eIT.java` | test (regression) | in-process | self — **do not modify** | reference-only |
| `TaskController.java` | controller | request-response | self — **do not modify** | reference-only |
| `application-phase7-test.yaml` | config | n/a | override gate in IT `@SpringBootTest(properties=…)` — **do not flip yaml default** | reference-only |

**Suggested packaging (CONTEXT discretion):** both ITs under `org.gensokyo.data.datasource.catalog` beside Phase 11.

**Reference only (do not modify for Phase 12 DoD):** `ManagedJdbcCatalogSinkE2eIT`, `TaskController`, `JdbcSnapshotExecutePathIT`, harness matrix / P0 rows, console `/api/templates/{id}/run`.

---

## Pattern Assignments

### 1. `ManagedJdbcCatalogHttpExecuteIT` (test, request-response) — EXEC-01

**Purpose:** Prove managed JDBC `dataSourceId` through `POST /task/run/{id}` → `SUCCESS` + managed-pool `COUNT(*)`.

**Compose these analogs — do not call `templateV2Runner.run` as primary path.**

#### Analog A — Managed catalog seed + COUNT (`ManagedJdbcCatalogSinkE2eIT`)

**Keep:** unique DS name / H2 mem URL / table; `DataSourceConfigService.save`; DDL on managed pool; managed-id-only writer assert; `countRows` helper.

```java
// ManagedJdbcCatalogSinkE2eIT — seed + COUNT (lines 49–54, 87–95, 137–145)
private static final String DS_NAME = "managed-jdbc-catalog-sink-e2e-ds";
private static final String H2_URL =
        "jdbc:h2:mem:managed-jdbc-catalog-sink-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1";

dataSourceConfigService.save(DS_NAME, H2_URL, "sa", "", null, "org.h2.Driver", null, null);

try {
    DynamicDataSourceContextHolder.push(DS_NAME);
    namedParameterJdbcTemplate.getJdbcTemplate().execute(
            "create table " + TABLE + " (id int primary key, label varchar(64))");
} finally {
    DynamicDataSourceContextHolder.clear();
}

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

**Managed-id-only assert (keep):**

```java
assertThat(writer.getDataSourceId()).isEqualTo(DS_NAME);
assertThat(writer.getDataSource()).isNull();
```

**Change for HTTP:**

| Phase 11 | Phase 12 EXEC-01 |
|----------|------------------|
| In-memory `TemplateV2VO` only | Persist YAML into `TemplatePO` + `saveAndFlush` |
| `templateV2Runner.run(template)` | MockMvc `POST /task/run/{id}` |
| Sync `TemplateV2RunResult` | Poll `TaskExecutionService` → `SUCCESS` |
| Javadoc: unbound in-process | Javadoc: HTTP spine; **do not** copy “Keep WorkflowRunContext unbound” (production binds snap; D-11 = no snap assert) |
| Unique names `…-sink-e2e…` | Use distinct names (e.g. `…-http-execute…`) to avoid same-JVM H2 clash |

**Template shape to lift** (VO → YAML text in `TemplatePO.contentYaml`):

```java
// ManagedJdbcCatalogSinkE2eIT.buildManagedSinkTemplate (lines 112–134)
InlineRowsSourceVO source = new InlineRowsSourceVO();
source.setRows(List.of(row("id", 1, "label", "a"), row("id", 2, "label", "b")));
SqlTransformVO transform = new SqlTransformVO();
transform.setSql("SELECT id, label FROM seed");
JdbcWriterVO writer = new JdbcWriterVO();
writer.setDataSourceId(DS_NAME);
writer.setTarget(TABLE);
// Plain INSERT — no upsert options for EXEC-01
```

YAML equivalent pattern (persist as text; align keys with V2 draft schema used by `RunReportPersistenceTests` / `TemplatePublishUdfValidationTests`):

```yaml
name: managed-jdbc-catalog-http-execute
sources:
  seed:
    type: inline
    rows:
      - { id: 1, label: a }
      - { id: 2, label: b }
transform:
  type: sql
  sql: SELECT id, label FROM seed
sink:
  writers:
    - type: jdbc
      dataSourceId: <DS_NAME>
      target: <TABLE>
```

(Exact `inline` key names must match existing draft parser — verify against `InlineRowsSourceVO` / sample YAML in service tests when implementing.)

#### Analog B — Persist template + `instanceId=` parse + poll (`RunReportPersistenceTests`)

```java
// RunReportPersistenceTests (lines 40, 66–92, 148–168)
private static final Pattern INSTANCE_ID_PATTERN = Pattern.compile("instanceId=(\\d+)");

templateRepository.saveAndFlush(entity);

R<String> start = taskController.runById(entity.getId()); // ← replace with MockMvc (D-03)
Long instanceId = extractInstanceId(start.getMessage());
TaskExecutionSummary summary = awaitSuccess(instanceId);

private static Long extractInstanceId(String message) {
    Matcher matcher = INSTANCE_ID_PATTERN.matcher(message);
    assertThat(matcher.find()).isTrue();
    return Long.valueOf(matcher.group(1));
}

private TaskExecutionSummary awaitSuccess(Long instanceId) throws InterruptedException {
    TaskExecutionSummary summary = null;
    for (int attempt = 0; attempt < 50; attempt++) {  // ← expand to ~30–60s (D-07)
        summary = taskExecutionService.getByInstanceId(instanceId);
        if (TaskExecutionStatus.SUCCESS.name().equals(summary.status())) {
            return summary;
        }
        if (TaskExecutionStatus.FAILED.name().equals(summary.status())) {
            break;  // ← also treat CANCELLED as immediate fail (D-07)
        }
        TimeUnit.MILLISECONDS.sleep(200);
    }
    assertThat(summary).isNotNull();
    assertThat(summary.status()).isEqualTo(TaskExecutionStatus.SUCCESS.name());
    return summary;
}
```

**Production parse twin** (`TemplateEditorRunSupport`):

```java
private static final Pattern INSTANCE_ID = Pattern.compile("instanceId=(\\d+)");
// R.ok puts text in message — parse message (or data fallback in editor support)
```

**Poll budget adaptation (D-07):** e.g. 150–300 attempts × 200ms ≈ 30–60s; fail immediately on `FAILED` **and** `CANCELLED`; no fixed sleep without status checks.

**Anti-pattern to avoid:** `RunReportPersistenceTests` skips publish because `application-phase7-test.yaml` sets `require-published-for-task-run: false`. Phase 12 **must** override to `true` + publish (D-02).

#### Analog C — MockMvc WAC (`ConsoleWebEndpointIT` / `ConsoleAuthorizationIntegrationIT`)

```java
// ConsoleWebEndpointIT — minimal WAC setup
mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

// ConsoleAuthorizationIntegrationIT — gate override pattern (lines 38–44)
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                "data.generator.governance.require-published-for-task-run=true"
        })
```

**HTTP enqueue target (D-01):**

```java
mockMvc.perform(post("/task/run/{id}", templateId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
// Then read $.message for instanceId= parse
```

Do **not** use console `POST /api/templates/{id}/run` as primary evidence. RBAC filter add from `ConsoleAuthorizationIntegrationIT` is **not** required (Phase 16).

#### Analog D — Publish gate (`TemplateLifecycleService` + `TemplatePublishUdfValidationTests`)

```java
// TemplateLifecycleService.publish / requirePublishedForTaskRun (lines 93–147)
public void publish(Long templateId) {
    // validate → set PUBLISHED → saveAndFlush
    entity.setStatus(TemplateLifecycleStatus.PUBLISHED.name());
    repository.saveAndFlush(entity);
}

public void requirePublishedForTaskRun(TemplatePO entity) {
    if (!properties.getGovernance().isRequirePublishedForTaskRun()) {
        return;  // phase7-test default hits this — override IT props to true
    }
    if (statusOf(entity) != TemplateLifecycleStatus.PUBLISHED) {
        throw new IllegalArgumentException(
                "Template must be PUBLISHED before task run; current status=" + statusOf(entity));
    }
}
```

**Preferred invoke (D-03 discretion):** lifecycle service, matching `TemplatePublishUdfValidationTests`:

```java
templateRepository.saveAndFlush(row);
templateLifecycleService.publish(row.getId());
```

Also mirrored by `TemplateLifecycleServiceTests` properties array with `require-published-for-task-run=true`.

#### Analog E — HTTP return + async bind (`TaskController`) — read-only reference

```java
@PostMapping("/run/{templateId}")
public R<String> postRunById(@NotNull @PathVariable Long templateId) {
    return runById(templateId);  // → runByIdInternal(..., requirePublished=true)
}

return R.ok(String.format("Template '%s' started. templateId=%s, instanceId=%s",
        runtime.name(), runtime.id(), runtime.instanceId()));

// runV2Tracked — production binds snap (do not assert snap keys; D-11)
taskExecutionService.markRunning(instanceId);
taskExecutionService.captureConnectionSnapshot(instanceId, template, connectionCatalog);
WorkflowRunContext.bind(instanceId, control);
try {
    TemplateV2RunResult result = templateV2Runner.run(template);
    taskExecutionService.markSuccess(instanceId, rowCount, metricsJson, reportJson);
} finally { /* unbind */ }
```

**Anti-pattern:** `TaskControllerApiTests` / `CapturingTemplateV2Runner` `@Primary` override — EXEC ITs must use the **real** `TemplateV2Runner` bean.

#### Suggested class skeleton (discretion)

```java
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = {
                "spring.config.location=classpath:/application-phase7-test.yaml",
                "data.generator.governance.require-published-for-task-run=true"
        })
class ManagedJdbcCatalogHttpExecuteIT {
    // @Autowired: DataSourceConfigService, TemplateRepository, TemplateLifecycleService,
    //              TaskExecutionService, NamedParameterJdbcTemplate, DynamicRoutingDataSource,
    //              WebApplicationContext
    // Method: httpTaskRun_managedCatalogSink_reachesSuccessWithCountableRows
}
```

**Javadoc must state (order):** `POST /task/run` MockMvc → managed `dataSourceId` → publish gate → poll `SUCCESS` → `COUNT(*)` → not in-process-only; snap assert deferred (D-11).

---

### 2. `ManagedJdbcCatalogHttpPostgresUpsertIT` (test, request-response) — EXEC-02

**Purpose:** Same HTTP spine; managed DS → Testcontainers PostgreSQL; dialect upsert `ON CONFLICT`; SUCCESS + COUNT (optional second-run idempotent count).

**Separate class (D-08).** Docker-gated (D-09).

#### Analog A — Container + gate (`ChunkedPipelinePostgresUpsertTests` + `DockerTestSupport`)

```java
@EnabledIf("org.gensokyo.data.calcite.support.DockerTestSupport#dockerAvailable")
@Testcontainers
class ChunkedPipelinePostgresUpsertTests {

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("upsert_parity")
            .withUsername("test")
            .withPassword("test");
}
```

```java
// DockerTestSupport
public static boolean dockerAvailable() {
    try {
        return DockerClientFactory.instance().isDockerAvailable();
    } catch (RuntimeException | LinkageError ex) {
        return false;
    }
}
```

**Service-module note:** calcite’s `DockerTestSupport` is **test-scoped** in calcite — not on service classpath. Planner options: (1) copy tiny class under `data-generator-service/src/test/java/.../support/` and point `@EnabledIf` at it; (2) duplicate the `DockerClientFactory` check inline. Do not invent a new gate API.

#### Analog B — Upsert options (`UpsertParitySupport.upsertTemplate`)

```java
writer.setOptions(new LinkedHashMap<>(Map.of(
        "dialect", dialect,      // "postgres" for EXEC-02
        "upsert", true,
        "upsertKeys", List.of("id"))));
```

**Shrink fixtures:** `UpsertParitySupport.ROW_COUNT = 500` is too heavy for HTTP IT — use 2–10 rows (CONTEXT/RESEARCH). Prefer inline/small seed like EXEC-01, not full parity support runner (that path is in-process `TemplateV2Runner` with noop resolver).

**Managed catalog URL:** `dataSourceConfigService.save(dsName, POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), null, "org.postgresql.Driver", null, null)`.

**DDL:** create target with PK on `id` so `ON CONFLICT` is valid; `DROP TABLE IF EXISTS` for re-run safety.

**Evidence (D-06 / D-10):** poll `SUCCESS` + `COUNT(*)`; optional second `POST /task/run` asserting count unchanged (stronger idempotency).

**Reuse HTTP compose from EXEC-01:** same publish + MockMvc + parse + poll patterns; only DS dialect, writer options, and Docker gate differ.

---

### 3. `data-generator-service/pom.xml` (build, test deps)

**Analog:** `data-generator-calcite/pom.xml` Testcontainers block (version `1.20.6`):

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.20.6</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.20.6</version>
    <scope>test</scope>
</dependency>
```

Service currently has runtime `postgresql` driver but **no** Testcontainers test deps — required for EXEC-02 under service tests. No new Maven groups.

---

### 4. Publish-gate property (`application-phase7-test.yaml`) — reference only

```yaml
# application-phase7-test.yaml (lines 31–35)
data:
  generator:
    governance:
      require-published-for-task-run: false
```

**Do not change the yaml default** (other ITs rely on draft runs). Override per-IT via `@SpringBootTest(properties=…)` as in `ConsoleAuthorizationIntegrationIT` / `TemplateLifecycleServiceTests`.

| Source | Value |
|--------|-------|
| `application-phase7-test.yaml` | `false` |
| Java / staging default | `true` |
| Phase 12 IT props | **must be `true`** |

---

## Don't Hand-Roll

| Need | Reuse |
|------|-------|
| Managed DS save + COUNT | `ManagedJdbcCatalogSinkE2eIT` |
| `instanceId=` regex | `instanceId=(\\d+)` from `RunReportPersistenceTests` / `TemplateEditorRunSupport` |
| Status poll | Adapt `awaitSuccess`; expand timeout; add `CANCELLED` fail-fast |
| MockMvc WAC | `MockMvcBuilders.webAppContextSetup(wac).build()` |
| Publish | `TemplateLifecycleService.publish` |
| Gate enable | IT property `data.generator.governance.require-published-for-task-run=true` |
| PG container image | `postgres:16-alpine` + `@Testcontainers` / `@Container` |
| Upsert options map | `UpsertParitySupport` dialect/upsert/upsertKeys |
| Docker gate | Copy/adapt `DockerTestSupport` |

---

## Anti-Patterns (Phase 12–specific)

1. **Labeling Phase 11 as HTTP** — leave `ManagedJdbcCatalogSinkE2eIT` unchanged; new ITs must MockMvc `POST /task/run/{id}`.
2. **Direct `taskController.runById` only** — insufficient for D-03 MockMvc requirement (may use controller in helpers, but primary evidence is MockMvc).
3. **Skipping publish** under phase7 defaults — violates D-02.
4. **Asserting `snap:{instanceId}:…`** — deferred (D-11); production still binds context.
5. **Copying “Keep WorkflowRunContext unbound”** into HTTP IT docs.
6. **`CapturingTemplateV2Runner` / `@Primary` fake runner** — real bean only.
7. **Fixed `Thread.sleep(N)` without status poll**.
8. **Serializing EXEC-01 + EXEC-02 in one class** (D-08).
9. **Promoting new ITs to P0 harness** — Phase 17.
10. **Same H2 mem DB name as Phase 11** — unique names + `DROP IF EXISTS`.

---

## Data-Flow Checklist (planner)

```
1. DataSourceConfigService.save(managed id, jdbc url…)
2. DDL on managed pool (CREATE / DROP IF EXISTS)
3. TemplatePO YAML (managed dataSourceId only) → saveAndFlush
4. templateLifecycleService.publish(id)   // gate enabled in IT props
5. MockMvc POST /task/run/{id} → $.success + $.message
6. Parse instanceId=(\\d+)
7. Poll TaskExecutionService.getByInstanceId → SUCCESS (fail on FAILED/CANCELLED; 30–60s)
8. COUNT(*) on managed pool ≥ expected
```

EXEC-02 inserts Testcontainers PG + upsert options between steps 1 and 3; same steps 4–8.

---

## Verification Commands (from RESEARCH)

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am -Dtest=ManagedJdbcCatalogHttpExecuteIT -Dskip.console.frontend=true test
.\mvnw-jdk25.ps1 -pl data-generator-service -am -Dtest=ManagedJdbcCatalogHttpPostgresUpsertIT -Dskip.console.frontend=true test
.\mvnw-jdk25.ps1 -pl data-generator-service -am "-Dtest=ManagedJdbcCatalog*IT" -Dskip.console.frontend=true test
```

---

*Phase: 12-http-execute-path-proof*  
*Pattern map only — no PLAN.md, no application source changes*
