# Phase 9: JDBC Dialect Expansion - Pattern Map

**Mapped:** 2026-07-21
**Files analyzed:** 22
**Analogs found:** 21 / 22

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilder.java` | utility | batch / transform | Same file (Phase 8 PG/MySQL upsert switch) | exact |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java` | utility | batch / streaming | Same file (Phase 8 upsert metrics + CK bulk reject) | exact |
| `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java` | middleware | request-response (publish gate) | Same file (`validateJdbcUpsertOptions`) | exact |
| `data-generator-service/src/main/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalog.java` | config | CRUD (catalog) | Same file (existing DM/KB/HG/CK/PG presets) | exact |
| `data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionConnectivityService.java` | service | request-response | Same file (`summarizeJdbcFailure`, `sanitizeMessage`) | exact |
| `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleDataSourceController.java` | controller | request-response | Same file (`driverPresets`, `testConnectionUnified`) | exact |
| `data-generator-console-web/src/app/datasources/DriverPresetFields.tsx` | component | CRUD | Same file (preset picker + bundled hint) | exact |
| `data-generator-console-web/src/app/datasources/jdbcDriverPresets.ts` | config | CRUD | Same file (`JDBC_DRIVER_GROUP_KEYS`, fallback presets) | exact |
| `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java` | test | batch | Same file (`clickhouseUpsertIsUnsupported`, PG/MySQL cases) | exact |
| `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/UpsertParitySupport.java` | test | batch | Same file (PG/MySQL idempotency harness) | exact |
| `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresUpsertTests.java` | test | batch | Same file (Testcontainers PG) | exact |
| `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/ClickHouseInsertBulkWriterIntegrationTests.java` | test | batch | Same file (Testcontainers CK insert) | exact |
| `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineKingbaseDialectTests.java` *(new)* | test | batch | `ChunkedPipelinePostgresUpsertTests.java` + dialect-key unit tests | role-match |
| `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java` *(new, optional)* | test | batch | `ChunkedPipelinePostgresUpsertTests.java` with `@EnabledIf` gate | role-match |
| `data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2ValidatorTests.java` | test | request-response | Same file (`upsertMissingKeysThrows`) | exact |
| `data-generator-service/src/test/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalogTests.java` | test | CRUD | Same file (`resolveDriverClassCandidates_*`) | exact |
| `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleDataSourceControllerTest.java` | test | request-response | Same file (`driverPresets_returnsCatalog`) | exact |
| `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogTestTests.java` | test | request-response | Same file (`jdbcDraftTest_failureHasActionableMessageWithoutSecrets`) | exact |
| `data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts` *(new)* | test | request-response | `e2e/specs/datasource-managed-crud.spec.ts` (select preset → save) | role-match |
| `scripts/verify-phase9-uat-jdbc-dialect.ps1` *(new)* | config | batch | `scripts/verify-phase8-uat-rw-streaming-upsert.ps1` | exact |
| `docs/template-v2-jdbc-sink-guide.md` | config | file-I/O | Same file (Phase 8 dialect/upsert sections) | exact |
| `AGENTS.md` | config | file-I/O | Same file (Phase 8 verify script entry) | exact |

## Pattern Assignments

### `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilder.java` (utility, batch / transform)

**Analog:** `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilder.java`

**Imports pattern** (lines 6-14):

```java
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
```

**Core dialect switch + upsert SQL pattern** (lines 60-98):

```java
private static String appendUpsertClause(
        JdbcWriterVO writer,
        String table,
        String columns,
        String values,
        String baseInsert,
        List<String> targetColumns) {
    String dialect = resolveDialect(writer);
    return switch (dialect) {
        case "postgres", "postgresql" -> appendPostgresUpsert(writer, baseInsert, targetColumns);
        case "mysql" -> appendMysqlUpsert(writer, table, columns, values, targetColumns);
        case "clickhouse", "click_house" -> throw unsupportedUpsertDialect("clickhouse");
        default -> throw unsupportedUpsertDialect(dialect);
    };
}

private static String appendPostgresUpsert(
        JdbcWriterVO writer,
        String baseInsert,
        List<String> targetColumns) {
    List<String> upsertKeys = requireUpsertKeys(writer, targetColumns);
    String conflict = String.join(", ", upsertKeys);
    List<String> updateColumns = targetColumns.stream()
            .filter(column -> !upsertKeys.contains(column))
            .toList();
    if (updateColumns.isEmpty()) {
        return baseInsert + " on conflict (" + conflict + ") do nothing";
    }
    String updates = updateColumns.stream()
            .map(column -> column + " = excluded." + column)
            .collect(Collectors.joining(", "));
    return baseInsert + " on conflict (" + conflict + ") do update set " + updates;
}
```

**Fail-fast upsert key validation** (lines 131-146):

```java
private static List<String> requireUpsertKeys(JdbcWriterVO writer, List<String> targetColumns) {
    List<String> upsertKeys = WriterOptionResolver.upsertKeysOption(writer);
    String writerName = writer.getTarget() == null ? "jdbc" : writer.getTarget();
    if (upsertKeys.isEmpty()) {
        throw new IllegalArgumentException(
                "JDBC sink writer '" + writerName + "' upsert=true requires non-empty options.upsertKeys; "
                        + "known columns: " + targetColumns);
    }
    for (String key : upsertKeys) {
        if (!targetColumns.contains(key)) {
            throw new IllegalArgumentException(
                    "JDBC sink writer '" + writerName + "' upsert key '" + key + "' is not a known column; "
                            + "known columns: " + targetColumns);
        }
    }
    return upsertKeys;
}
```

**Phase 9 extension notes:** Add `case "kingbase", "highgo" -> appendPostgresUpsert(...)`, `case "dameng" -> appendDamengMerge(...)`, tighten `default`/`generic` to fail when `upsert=true` (D-08). Keep `resolveDialect` as source of truth (D-07); do not compare against JDBC URL.

---

### `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java` (utility, batch / streaming)

**Analog:** `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java`

**Core JDBC batch write + upsert metrics** (lines 65-87):

```java
private static void writeJdbcBatch(
        NamedParameterJdbcTemplate jdbcTemplate,
        JdbcWriterVO writer,
        List<JdbcSinkColumnMappings.ColumnMapping> mappings,
        List<Row> rows,
        JdbcSinkWriteStats writeStats) {
    List<String> targetColumns = mappings.stream().map(JdbcSinkColumnMappings.ColumnMapping::target).toList();
    // Fail-fast upsert key validation before JDBC execute (D-14).
    JdbcSinkSqlBuilder.validateUpsertKeys(writer, targetColumns);
    String sql = JdbcSinkSqlBuilder.buildSql(writer, targetColumns);
    List<Row> writableRows = filterWritableRows(writer, mappings, rows, writeStats);
    if (writableRows.isEmpty()) {
        return;
    }
    Map<String, ?>[] batch = writableRows.stream()
            .map(row -> JdbcSinkColumnMappings.toSqlParams(row, mappings))
            .toArray(Map[]::new);
    int[] updateCounts = jdbcTemplate.batchUpdate(sql, batch);
    if (writeStats != null && WriterOptionResolver.booleanOption(writer, "upsert")) {
        String dialect = resolveDialect(writer);
        long upserted = countUpsertedRows(updateCounts, dialect);
        writeStats.addRowsUpserted(upserted);
    }
}
```

**ClickHouse bulk upsert reject (already present — keep aligned with publish gate)** (lines 213-226):

```java
private static void writeClickHouseInsert(
        NamedParameterJdbcTemplate jdbcTemplate,
        JdbcWriterVO writer,
        List<JdbcSinkColumnMappings.ColumnMapping> mappings,
        List<Row> rows) {
    if (WriterOptionResolver.booleanOption(writer, "upsert")) {
        throw new IllegalArgumentException("JDBC sink bulkMode clickhouse_insert does not support upsert=true");
    }
    String dialect = WriterOptionResolver.stringOption(writer, "dialect", null);
    if (StringUtils.hasText(dialect)) {
        String normalized = dialect.trim().toLowerCase(Locale.ROOT);
        if (!"clickhouse".equals(normalized) && !"click_house".equals(normalized)) {
            throw new IllegalArgumentException("JDBC sink bulkMode clickhouse_insert requires dialect clickhouse");
        }
    }
    // ...
}
```

**Upsert row-count interpretation** (lines 110-121): extend `countUpsertedRows` so `kingbase`/`highgo`/`dameng` follow the PostgreSQL branch (count `updateCount > 0`); keep MySQL `== 2` rule.

---

### `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java` (middleware, request-response)

**Analog:** `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java`

**Publish-time JDBC upsert validation** (lines 424-477):

```java
/**
 * Validates JDBC sink upsert options: non-empty {@code upsertKeys} when {@code upsert: true} and,
 * for simple SQL transforms, publish-time column cross-check (D-14).
 */
private static void validateJdbcUpsertOptions(TemplateV2VO template) {
    // ... infer sqlOutputColumns from primary SQL transform ...
    for (int sinkIndex = 0; sinkIndex < template.getSinks().size(); sinkIndex++) {
        WriteStageVO sink = template.getSinks().get(sinkIndex);
        // ...
        for (int writerIndex = 0; writerIndex < sink.getWriters().size(); writerIndex++) {
            WriterVO writer = sink.getWriters().get(writerIndex);
            if (!isJdbcWriter(writer) || !WriterOptionResolver.booleanOption(writer, "upsert")) {
                continue;
            }
            String writerPath = "sink[" + sinkIndex + "].writer[" + writerIndex + "]";
            List<String> upsertKeys = WriterOptionResolver.upsertKeysOption(writer);
            if (upsertKeys.isEmpty()) {
                throw new IllegalArgumentException(writerPath
                        + ": JDBC sink upsert=true requires non-empty options.upsertKeys");
            }
            // ... blank key + column cross-check ...
        }
    }
}
```

**Phase 9 extension notes:** After upsertKeys checks, read `WriterOptionResolver.stringOption(writer, "dialect", "generic")` and fail publish for: `clickhouse`+upsert, `generic`+upsert, unknown dialect+upsert. Mirror `JdbcSinkSqlBuilder.unsupportedUpsertDialect` messages. Draft warnings via `appendOpaqueUpsertKeyWarnings` unchanged.

---

### `data-generator-service/src/main/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalog.java` (config, CRUD)

**Analog:** `data-generator-service/src/main/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalog.java`

**Preset registration pattern** (lines 23-55):

```java
private static final List<JdbcDriverPreset> PRESETS = List.of(
        preset(
                "dm8",
                "dm",
                "dm",
                "datasources.driver.dm8",
                "dm.jdbc.driver.DmDriver",
                List.of(),
                "jdbc:dm://localhost:5236?schema=YOUR_SCHEMA"),
        preset(
                "kingbase8",
                "kingbase",
                "kingbase8",
                "datasources.driver.kingbase8",
                "com.kingbase8.Driver",
                List.of("com.kingbase.Driver"),
                "jdbc:kingbase8://localhost:54321/YOUR_DATABASE"),
        preset(
                "highgo",
                "highgo",
                "highgo",
                "datasources.driver.highgo",
                "com.highgo.jdbc.Driver",
                List.of(),
                "jdbc:highgo://localhost:5866/highgo"),
        // clickhouse*, postgresql* entries follow same shape
);
```

**URL matching for preset resolution** (lines 273-282):

```java
private static boolean matchesUrl(JdbcDriverPreset preset, String urlLower) {
    return switch (preset.groupKey()) {
        case "dm" -> urlLower.contains(":dm:");
        case "kingbase" -> urlLower.contains(":kingbase");
        case "highgo" -> urlLower.contains(":highgo:");
        case "clickhouse" -> urlLower.contains(":clickhouse:") || urlLower.contains(":ch:");
        case "postgresql" -> urlLower.contains(":postgresql:");
        case "mysql" -> urlLower.contains(":mysql:");
        default -> false;
    };
}
```

**Bundled flag surfacing:** `JdbcDriverPresetDto.from(preset, registry)` sets `bundled` from `BundledJdbcDriverRegistry.hasBundle(preset.bundleKey())` — verify DM/KB/HG bundle keys match `jdbc-bundled/` layout (D-10).

---

### `data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionConnectivityService.java` (service, request-response)

**Analog:** `data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionConnectivityService.java`

**JDBC test with sanitized failure** (lines 107-127):

```java
private ConnectionTestResult testJdbc(
        String url,
        String username,
        String password,
        String driverClassName,
        String driverJarPath) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("driverClassName", driverClassName);
    try {
        JdbcDriverLoadResult loaded = driverSupport.ensureDriverLoaded(driverClassName, url, driverJarPath);
        try (Connection connection = driverSupport.openConnection(url, username, password, loaded)) {
            if (!connection.isValid(5)) {
                return ConnectionTestResult.fail("JDBC connection invalid — verify URL and credentials", details);
            }
        }
        return ConnectionTestResult.ok("JDBC connection OK", details);
    } catch (DataGeneratorException ex) {
        return ConnectionTestResult.fail("JDBC connectivity test failed: " + sanitizeMessage(ex.getMessage()), details);
    } catch (Exception ex) {
        return ConnectionTestResult.fail(summarizeJdbcFailure(ex), details);
    }
}
```

**Actionable summary + secret hygiene** (lines 213-234):

```java
private static String summarizeJdbcFailure(Exception ex) {
    String message = ex.getMessage();
    if (message == null || message.isBlank()) {
        return "JDBC connection failed — verify URL, driver, and credentials";
    }
    String lower = message.toLowerCase();
    if (lower.contains("password") || lower.contains("access denied") || lower.contains("authentication")) {
        return "JDBC authentication failed — verify username and password";
    }
    if (lower.contains("unknown host") || lower.contains("connection refused") || lower.contains("timeout")) {
        return "JDBC host unreachable — verify URL and network access";
    }
    return "JDBC connectivity test failed: " + sanitizeMessage(message);
}

private static String sanitizeMessage(String message) {
    if (message == null) {
        return "";
    }
    // Strip userinfo from JDBC URLs that may appear in driver error messages (D-23).
    return message.replaceAll("jdbc:[^:@/]+://[^@/]+@", "jdbc://[redacted]@");
}
```

**Phase 9 extension notes:** Optionally add driver-class hints in `details` for DM/KB/HG failures; never echo password or full JDBC URL in `message` (D-11). Contract tests in `ConnectionCatalogTestTests` already assert no password leakage.

---

### `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleDataSourceController.java` (controller, request-response)

**Analog:** `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleDataSourceController.java`

**Driver preset API** (lines 132-137):

```java
@GetMapping("/driver-presets")
public R<List<JdbcDriverPresetDto>> driverPresets() {
    List<JdbcDriverPresetDto> presets = JdbcDriverPresetCatalog.all().stream()
            .map(p -> JdbcDriverPresetDto.from(p, bundledJdbcDriverRegistry))
            .toList();
    return R.ok(presets);
}
```

**Unified connectivity test** (lines 184-194):

```java
@PostMapping("/connections/test")
public R<String> testConnectionUnified(@RequestBody ConnectionTestRequestDto request) {
    if (request == null || request.kind() == null || request.kind().isBlank()) {
        return R.fail("kind is required");
    }
    ConnectionKind kind = ConnectionKind.valueOf(request.kind().trim().toUpperCase());
    ConnectionTestRequest catalogRequest = buildCatalogTestRequest(kind, request);
    ConnectionTestResult result = connectionCatalog.test(catalogRequest);
    if (!result.success()) {
        return R.fail(result.message());
    }
    return R.ok(result.message());
}
```

**Pattern:** Controller stays thin; preset correctness lives in `JdbcDriverPresetCatalog` + tests. Return `R<T>` envelope via `ConsoleApiAdvice` on exceptions.

---

### `data-generator-console-web/src/app/datasources/DriverPresetFields.tsx` (component, CRUD)

**Analog:** `data-generator-console-web/src/app/datasources/DriverPresetFields.tsx`

**Imports + preset apply pattern** (lines 1-11, 77-93):

```typescript
import { Form, Input, Select, Typography } from 'antd';
import {
  JDBC_DRIVER_GROUP_KEYS,
  findJdbcDriverPreset,
  guessPresetId,
  type JdbcDriverPreset,
} from './jdbcDriverPresets';

const applyPreset = (preset: JdbcDriverPreset, updateUrl: boolean) => {
  const currentUrl = form.getFieldValue('url');
  form.setFieldsValue({
    driverClassName: preset.driverClassName,
    url: updateUrl || !currentUrl?.trim() ? preset.urlTemplate : currentUrl,
  });
};

const onPresetChange = (id: string) => {
  setPresetId(id);
  onPresetIdChange?.(id);
  form.setFieldValue('driverPresetId', id);
  const preset = findJdbcDriverPreset(presets, id);
  if (preset) {
    applyPreset(preset, true);
  }
};
```

**Bundled driver hint** (lines 95-96):

```typescript
const selectedPreset = presetId ? findJdbcDriverPreset(presets, presetId) : undefined;
const bundledSelected = selectedPreset?.bundled ?? false;
```

**Phase 9 extension notes:** Align `jdbcDriverPresets.ts` fallback `bundled: true` for DM/KB/HG when server registry confirms bundle; no new capability-hint UI (D-09 out of scope for hints).

---

### `data-generator-console-web/src/app/datasources/jdbcDriverPresets.ts` (config, CRUD)

**Analog:** `data-generator-console-web/src/app/datasources/jdbcDriverPresets.ts`

**Group key ordering (matches server catalog)** (lines 17-24):

```typescript
export const JDBC_DRIVER_GROUP_KEYS = [
  'dm',
  'kingbase',
  'highgo',
  'clickhouse',
  'postgresql',
  'mysql',
] as const;
```

**Fallback preset shape** (lines 27-36):

```typescript
export const JDBC_DRIVER_PRESETS_FALLBACK: JdbcDriverPreset[] = [
  {
    id: 'dm8',
    labelKey: 'datasources.driver.dm8',
    groupKey: 'dm',
    bundleKey: 'dm',
    driverClassName: 'dm.jdbc.driver.DmDriver',
    alternateDriverClassNames: [],
    urlTemplate: 'jdbc:dm://localhost:5236?schema=YOUR_SCHEMA',
    bundled: false,
  },
  // ...
];
```

**Pattern:** Keep fallback URLs/driver classes in sync with `JdbcDriverPresetCatalog`; server API is authoritative at runtime.

---

### `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java` (test, batch)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java`

**Postgres upsert unit test** (lines 59-68):

```java
@Test
void buildsPostgresUpsertWithUpsertKeysAndUpdateClause() {
    JdbcWriterVO writer = writer("orders_out");
    writer.setOptions(Map.of(
            "dialect", "postgres",
            "upsert", true,
            "upsertKeys", List.of("id")));
    String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount"));
    Assertions.assertTrue(sql.contains("on conflict (id) do update set"));
    Assertions.assertTrue(sql.contains("amount = excluded.amount"));
}
```

**ClickHouse upsert reject** (lines 106-117):

```java
@Test
void clickhouseUpsertIsUnsupported() {
    JdbcWriterVO writer = writer("orders_out");
    writer.setOptions(Map.of(
            "dialect", "clickhouse",
            "upsert", true,
            "upsertKeys", List.of("id")));
    IllegalArgumentException ex = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount")));
    Assertions.assertTrue(ex.getMessage().contains("clickhouse"));
}
```

**Phase 9 new tests to mirror:** `buildsKingbaseUsesOnConflictPath`, `buildsHighgoUsesOnConflictPath`, `buildsDamengMergeInto`, `genericDialectUpsertFailsFast`.

---

### `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/UpsertParitySupport.java` (test, batch)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/UpsertParitySupport.java`

**Shared idempotency harness** (lines 55-101):

```java
public static void assertUpsertIdempotent(
        String jdbcUrl, String username, String password, String driverClassName, String dialect) {
    // ... create tables, seed 500 rows ...
    TemplateV2VO template = upsertTemplate(dialect);
    TemplateV2RuntimeRegistry registry = registry(jdbcTemplate);

    TemplateV2RunResult firstRun = new TemplateV2Runner(registry).run(template);
    long countAfterFirst = countRows(jdbcTemplate, "upsert_target_t");
    Assertions.assertEquals(ROW_COUNT, countAfterFirst, "first run should insert all rows");

    updateSourceNames(jdbcTemplate, "u");
    TemplateV2RunResult secondRun = new TemplateV2Runner(registry).run(template);

    long countAfterSecond = countRows(jdbcTemplate, "upsert_target_t");
    Assertions.assertEquals(countAfterFirst, countAfterSecond,
            "second upsert run must not increase row count (D-15); dialect=" + dialect);
    // ... rowsUpserted metric assertion ...
}
```

**Template options contract** (lines 112-118):

```java
writer.setOptions(new LinkedHashMap<>(Map.of(
        "dialect", dialect,
        "upsert", true,
        "upsertKeys", List.of("id"))));
```

**Phase 9 extension notes:** Parameterize `withDialectSqlHint` for `kingbase`/`highgo` → `ON CONFLICT`, `dameng` → `MERGE`. Reuse PG container with `dialect=kingbase|highgo` for proxy ITs (D-13, D-15).

---

### `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresUpsertTests.java` (test, batch)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresUpsertTests.java`

**Testcontainers + UpsertParitySupport** (lines 21-40):

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

    @Test
    void chunkedUpsertReRunIsIdempotent() {
        UpsertParitySupport.assertUpsertIdempotent(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword(),
                "org.postgresql.Driver",
                "postgres");
    }
}
```

**New file pattern (`ChunkedPipelineKingbaseDialectTests`):** Same container; call `assertUpsertIdempotent(..., "kingbase")` and `(..., "highgo")`; add companion unit test asserting SQL builder maps both to `ON CONFLICT`.

---

### `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/ClickHouseInsertBulkWriterIntegrationTests.java` (test, batch)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/ClickHouseInsertBulkWriterIntegrationTests.java`

**Testcontainers ClickHouse insert** (lines 33-38, 76-84):

```java
@Testcontainers(disabledWithoutDocker = true)
class ClickHouseInsertBulkWriterIntegrationTests {

    @Container
    private static final ClickHouseContainer CLICKHOUSE = new ClickHouseContainer(
            DockerImageName.parse("clickhouse/clickhouse-server:24.8"));

    @Test
    void writesRowsUsingClickHouseInsertBulkMode() {
        // ...
        writer.setOptions(Map.of("bulkMode", "clickhouse_insert", "dialect", "clickhouse"));
```

**Phase 9:** Add contract test `clickhouseUpsertRejectedAtRuntime` calling `JdbcSinkSqlBuilder.buildSql` or full sink with `upsert: true` (complements existing unit test).

---

### `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java` *(new, optional)* (test, batch)

**Analog:** `ChunkedPipelinePostgresUpsertTests.java` + `@EnabledIf` gating pattern from Docker tests

**Optional IT gate pattern:**

```java
@EnabledIf("org.gensokyo.data.calcite.support.DamengTestSupport#damengItEnabled")
@Testcontainers
class ChunkedPipelineDamengUpsertIT {
    // Real DM container when -Ddm.it=true or env flag (D-14)
}
```

**Primary proof:** `JdbcSinkSqlBuilderTests` MERGE SQL generation without live DM (D-13).

---

### `data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2ValidatorTests.java` (test, request-response)

**Analog:** `data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2ValidatorTests.java`

**Publish fail-fast test** (lines 36-47):

```java
@Test
void upsertMissingKeysThrows() {
    TemplateV2VO template = baseTemplate(
            sql("SELECT id, label FROM input"),
            jdbcUpsertWriter(true, List.of()));

    IllegalArgumentException ex = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> TemplateV2Validator.validate(template));

    Assertions.assertTrue(ex.getMessage().contains("sink[0].writer[0]"));
    Assertions.assertTrue(ex.getMessage().contains("upsertKeys"));
}
```

**Helper for JDBC upsert writer** (lines 166-170):

```java
private static WriteStageVO jdbcUpsertWriter(boolean upsert, List<String> upsertKeys) {
    JdbcWriterVO writer = new JdbcWriterVO();
    writer.getOptions().put("upsert", upsert);
    writer.getOptions().put("upsertKeys", upsertKeys);
```

**Phase 9 new tests:** `clickhouseUpsertRejectedAtPublish`, `genericDialectUpsertRejectedAtPublish`, optional `damengUpsertAllowedWhenKeysPresent`.

---

### `data-generator-service/src/test/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalogTests.java` (test, CRUD)

**Analog:** `data-generator-service/src/test/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalogTests.java`

**Preset resolution tests** (lines 22-46):

```java
@Test
void resolveDriverClassCandidates_includesKingbase9Alternates() {
    List<String> candidates = JdbcDriverPresetCatalog.resolveDriverClassCandidates(
            "com.kingbase9.Driver", "jdbc:kingbase9://localhost:54321/db");
    assertThat(candidates)
            .containsExactly(
                    "com.kingbase9.Driver",
                    "com.kingbase8.Driver",
                    "com.kingbase.Driver");
}

@Test
void resolveDriverClassCandidates_matchesByJdbcUrlWhenClassUnknown() {
    List<String> candidates = JdbcDriverPresetCatalog.resolveDriverClassCandidates(
            "com.example.CustomDriver", "jdbc:dm://host:5236");
    assertThat(candidates).contains("com.example.CustomDriver", "dm.jdbc.driver.DmDriver");
}
```

**Phase 9:** Add assertions for HighGo URL template, ClickHouse bundled key, PostgreSQL preset completeness.

---

### `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleDataSourceControllerTest.java` (test, request-response)

**Analog:** `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleDataSourceControllerTest.java`

**Driver preset JSON contract** (lines 117-124):

```java
@Test
void driverPresets_returnsCatalog() throws Exception {
    when(bundledJdbcDriverRegistry.hasBundle(anyString())).thenReturn(true);
    mockMvc.perform(get("/api/datasources/driver-presets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[?(@.id == 'dm8')].driverClassName").value("dm.jdbc.driver.DmDriver"))
            .andExpect(jsonPath("$.data[?(@.id == 'dm8')].bundled").value(true));
}
```

**Phase 9:** Extend jsonPath checks for `kingbase8`, `highgo`, `clickhouse24`, `postgresql16` URL templates and `bundled: true`.

---

### `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogTestTests.java` (test, request-response)

**Analog:** `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogTestTests.java`

**Secret-safe failure contract** (lines 76-98):

```java
@Test
void jdbcDraftTest_failureHasActionableMessageWithoutSecrets() {
    // ...
    payload = Map.of(
            "url", "jdbc:h2:tcp://127.0.0.1:59999/nonexistent;DB_CLOSE_DELAY=-1",
            "username", "sa",
            "password", "s3cr3t",
            "driverClassName", "org.h2.Driver");
    result = connectionCatalog.test(ConnectionTestRequest.forDraft(ConnectionKind.JDBC, payload));

    Assertions.assertFalse(result.success());
    Assertions.assertFalse(result.message().isBlank());
    Assertions.assertFalse(result.message().contains("s3cr3t"));
}
```

**Phase 9:** Add draft tests with DM/KB/HG driver classes against unreachable hosts; assert actionable message + no password/URL userinfo echo.

---

### `data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts` *(new)* (test, request-response)

**Analog:** `data-generator-console-web/e2e/specs/datasource-managed-crud.spec.ts`

**UI save flow with preset selection** (lines 62-82 of analog):

```typescript
test('UI save and test connection on persisted row', async ({ page }) => {
  const name = `e2e-crud-ui-${Date.now()}`;
  await gotoConsoleHome(page);
  await navigateViaTopNav(page, TestIds.nav.datasources);
  await page.getByTestId(TestIds.actions.datasourcesNew).click();

  const dialog = page.getByRole('dialog');
  await dialog.getByRole('textbox', { name: /名称|Name/i }).fill(name);
  // ... fill url, username, password, driver class ...
  const [saveResp] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/api/datasources') && response.request().method() === 'POST',
    ),
    dialog.locator('button[type="submit"]').click(),
  ]);
  expect((await saveResp.json()).success).toBe(true);
});
```

**Phase 9 pattern:** Select one preset from grouped dropdown (e.g. `dm8` or `postgresql16`), assert URL/driver auto-fill, save with H2 or mock — one path only (D-12). Wire npm script `e2e:phase9-jdbc-dialect` like Phase 8.

---

### `scripts/verify-phase9-uat-jdbc-dialect.ps1` *(new)* (config, batch)

**Analog:** `scripts/verify-phase8-uat-rw-streaming-upsert.ps1`

**Script skeleton** (lines 1-65 of analog):

```powershell
# Phase 9 UAT — JDBC dialect expansion: Maven IT slice + optional Podman Playwright.
param(
    [switch]$SkipBuild,
    [switch]$SkipPlaywright,
    [switch]$KeepContainer,
    [string]$ImageTag = 'dg-phase9-jdbc-dialect-uat:local',
    [string]$ContainerName = 'dg-phase9-jdbc-dialect-uat',
    [int]$HostPort = 9876,
    [string]$SpringProfiles = 'e2e'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')

Write-Step "Maven slice — JdbcSinkSqlBuilderTests, UpsertParitySupport ITs, validator, preset catalog"
$testList = @(
    'JdbcSinkSqlBuilderTests',
    'ChunkedPipelinePostgresUpsertTests',
    'ClickHouseInsertBulkWriterIntegrationTests',
    'ChunkedPipelineKingbaseDialectTests',
    'TemplateV2ValidatorTests',
    'JdbcDriverPresetCatalogTests',
    'ConsoleDataSourceControllerTest',
    'ConnectionCatalogTestTests'
) -join ','
$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl data-generator-service -am `
    "-Dtest=$testList" `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
```

**Pattern:** `-SkipPlaywright` exits 0 after Maven; optional Podman + `npm run e2e:phase9-jdbc-dialect` (mirror Phase 7/8).

---

### `docs/template-v2-jdbc-sink-guide.md` (config, file-I/O)

**Analog:** `docs/template-v2-jdbc-sink-guide.md`

**Dialect option table + fail-fast note** (lines 11-19):

```markdown
| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `dialect` | string | `generic` | `generic`, `postgres`, `mysql`, or `clickhouse` |
| `upsert` | boolean | `false` | Enable dialect-specific duplicate handling |
| `upsertKeys` | string[] | — | **Required** when `upsert: true` (Phase 8). Conflict key columns |

When `upsert: true` and `upsertKeys` is missing, empty, or references unknown columns, publish and run **fail fast** (same severity as governance blocks).
```

**ClickHouse deferral text to replace** (lines 99-112):

```markdown
### ClickHouse insert (dedup via table engine)

ClickHouse has no `ON CONFLICT` or `INSERT IGNORE`. With `dialect: clickhouse` and `upsert: true`, the runtime rejects upsert in Phase 8 — use `ReplacingMergeTree` / `CollapsingMergeTree` (or application-level keys) for duplicate handling. **Dialect upsert expansion** (Dameng, Kingbase, HighGo, ClickHouse) is planned for **Phase 9**.
```

**Phase 9:** Add sections for `dameng` (MERGE), `kingbase`/`highgo` (ON CONFLICT via PG path), explicit `dialect` requirement (D-05), `generic`+upsert fail-fast (D-08), Kingbase/HighGo PG-proxy test note (D-15), update Limitations list (lines 172-178).

---

### `AGENTS.md` (config, file-I/O)

**Analog:** `AGENTS.md` Phase 8 verify entry (existing pattern):

```bash
# Phase 8 RW streaming CSV/JSON + JDBC upsert UAT (Maven IT slice + optional Podman Playwright)
.\scripts\verify-phase8-uat-rw-streaming-upsert.ps1 -SkipPlaywright
```

**Phase 9 entry to add:**

```bash
# Phase 9 JDBC dialect expansion UAT (Maven dialect slice + optional Podman Playwright)
.\scripts\verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright
```

---

## Shared Patterns

### Dual fail-fast (publish + run)

**Source:** `JdbcSinkSqlBuilder.java` (run) + `TemplateV2Validator.java` (publish)

**Apply to:** All dialect+upsert validation in Phase 9

```java
// Run-time (JdbcSinkSqlBuilder)
case "clickhouse", "click_house" -> throw unsupportedUpsertDialect("clickhouse");
default -> throw unsupportedUpsertDialect(dialect);

// Publish-time (TemplateV2Validator.validateJdbcUpsertOptions)
if (upsertKeys.isEmpty()) {
    throw new IllegalArgumentException(writerPath
            + ": JDBC sink upsert=true requires non-empty options.upsertKeys");
}
```

Mirror the same dialect rules in both layers; draft save may warn via `collectWarnings`, publish/run block.

### Writer options resolution

**Source:** `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/WriterOptionResolver.java`

**Apply to:** All JDBC sink code and validator tests

```java
WriterOptionResolver.booleanOption(writer, "upsert")
WriterOptionResolver.upsertKeysOption(writer)
WriterOptionResolver.stringOption(writer, "dialect", null)
```

YAML contract unchanged: `options.upsert` + `options.upsertKeys` + explicit `options.dialect`.

### Connectivity test secret hygiene

**Source:** `ConnectionConnectivityService.java` + `ConnectionCatalogTestTests.java`

**Apply to:** Console connectivity for DM/KB/HG/CK/PG presets

```java
return ConnectionTestResult.fail(summarizeJdbcFailure(ex), details);
// details may include driverClassName; message must not contain password or full JDBC URL userinfo
```

### Testcontainers embedded-first

**Source:** `docs/testing-embedded-components.md` + `ChunkedPipelinePostgresUpsertTests.java` + `ClickHouseInsertBulkWriterIntegrationTests.java`

**Apply to:** PG/CK integration tests; PG-as-proxy for Kingbase/HighGo

```java
@EnabledIf("org.gensokyo.data.calcite.support.DockerTestSupport#dockerAvailable")
@Testcontainers
class ChunkedPipelinePostgresUpsertTests {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
```

### Console REST envelope

**Source:** `ConsoleDataSourceController.java` + `ConsoleApiAdvice`

**Apply to:** Preset and connectivity API tests

```java
return R.ok(presets);
return R.fail(result.message());
```

### UAT verify script structure

**Source:** `scripts/verify-phase8-uat-rw-streaming-upsert.ps1`

**Apply to:** `verify-phase9-uat-jdbc-dialect.ps1`

- `Invoke-RepoMaven` with `-Dtest=` comma list
- `-SkipPlaywright` early success exit
- Optional Podman build/run + Playwright env vars (`DG_E2E_BASE_URL`, `DG_E2E_API_URL`)

### Java file documentation (project rule)

**Source:** `.cursor/rules/java-copyright-class-javadoc.mdc`

**Apply to:** Any new `.java` test or production files

- Copyright block above `package`
- Class Javadoc with `@author` / `@since`
- Public method Javadoc with `@param` / `@return` / `@throws`
- Inline `//` for non-obvious control flow

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `appendDamengMerge` implementation details | utility | batch | No existing MERGE INTO SQL builder in repo; derive from `appendPostgresUpsert` structure + Dameng MERGE semantics (D-02 discretion) |

Planner should use Phase 8 `JdbcSinkSqlBuilder` switch/case pattern and operator docs for MERGE shape; optional real DM IT gated by env flag (D-14).

## Metadata

**Analog search scope:** `data-generator-calcite/` (sink, runtime tests), `data-generator-service/` (validator, datasource catalog, console API), `data-generator-console-web/` (preset UI, e2e), `scripts/`, `docs/`
**Files scanned:** ~45
**Pattern extraction date:** 2026-07-21
