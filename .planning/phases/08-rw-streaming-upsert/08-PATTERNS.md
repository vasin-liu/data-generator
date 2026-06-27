# Phase 8 — RW Streaming & Upsert: Pattern Mapping

**Phase:** 08-rw-streaming-upsert  
**Generated:** 2026-06-27  
**Source:** `08-CONTEXT.md`, `ROADMAP.md` (Phase 8), canonical refs, codebase survey

---

## Overview

Phase 8 extends **existing** JDBC streaming/chunked execution paths to **CSV/JSON** sources and sinks, adds **PostgreSQL/MySQL JDBC upsert** via sink options, and surfaces **per-sink run-report metrics** in JSON and the Job center UI. The work is **additive and backward-compatible**: `IN_MEMORY` small-file templates behave unchanged; large-file `IN_MEMORY` gets **warn-only** guidance; `CHUNKED`/`STREAMING` require **explicit** operator choice.

### Architectural spine

```
TemplateV2Runner
  └─ EffectiveExecutionPolicy.resolve()
       ├─ IN_MEMORY  → InMemoryPipeline        (CsvRowSource.rows() / JsonRowSource.rows() — unchanged)
       ├─ CHUNKED    → ChunkedPipeline        (requires ChunkedRowSource — today JDBC only)
       └─ STREAMING  → StreamingPipeline      (requires ChunkedRowSource — today JDBC only)
            │
            ├─ per-chunk: registry.applyTransform(sql per chunk)
            └─ per-chunk: SinkWriteExecutor.writeSinks(batch flush)
```

**Phase 8 goal:** CSV/JSON factories produce `ChunkedRowSource` implementations when policy mode is `CHUNKED` or `STREAMING`; file sinks flush **per chunk**; JDBC sink generates dialect upsert SQL from `options.upsert` + `options.upsertKeys`; `RunReportCollector` maps extended `SinkWriteMetric` / `StageMetricVO` fields to console.

### Key decisions → pattern hooks

| Decision | Pattern to follow |
|----------|-------------------|
| D-01/D-02 Explicit CHUNKED/STREAMING | Mirror `QuerySourceFactory.usesChunkedRead()` — no auto-promotion |
| D-03 Default chunk 1000 | Align with `EffectiveExecutionPolicy.DEFAULT_SINK_BATCH_SIZE` (1000); note `DEFAULT_SOURCE_CHUNK_SIZE` is 5000 today — Phase 8 may add CSV/JSON-specific default or document override |
| D-04 Per-chunk SQL only | Reuse `StreamingPipeline`/`ChunkedPipeline` chunk loop; reject cross-chunk shapes via `ExecutionShapeClassifier` |
| D-12..D-14 upsertKeys fail-fast | Mirror `TemplateV2Validator.validateGovernance()` throw pattern + publish validation API |
| D-16..D-18 Run report | Extend `SinkWriteMetric` + `StageMetricVO`; map in `RunReportCollector.buildSinkMetrics()` |
| D-23 Playwright 5+ scenarios | Extend Phase 6 `createPublishRunFromScenario` + Phase 7 governance spec structure |
| D-24 OOM proof | New IT with `-Xmx256m` + 10 MB fixture under CHUNKED/STREAMING |
| D-25 Upsert ITs | H2 smoke + `ChunkedPipelinePostgresContainerTests` Testcontainers pattern |

---

## File Map

Legend: **C** create, **M** modify, **role** = primary concern.

### Runtime (data-generator-calcite)

| File | Role | Action | Closest analog |
|------|------|--------|----------------|
| `runtime/StreamingPipeline.java` | runtime | M | Self — relax `soleQuerySource()` / `validateV1Scope()` for single CSV/JSON source |
| `runtime/ChunkedPipeline.java` | runtime | M | Self — same source-type relaxation for ROW_LOCAL CSV/JSON |
| `runtime/TemplateV2Runner.java` | runtime | — | Dispatch unchanged; pipelines gain CSV/JSON eligibility |
| `runtime/EffectiveExecutionPolicy.java` | runtime | M? | Self — confirm chunk defaults (D-03) |
| `runtime/RunMetrics.java` | runtime | M | Self — add upsert/skipped counters if not on `SinkWriteMetric` |
| `runtime/SinkWriteMetric.java` | runtime | M | Self — add `rowsUpserted`, `rowsSkipped` |
| `runtime/SinkWriteExecutor.java` | runtime | M | Self — per-chunk file sink flush, upsert metric recording |
| `source/ChunkedRowSource.java` | runtime | — | Interface — CSV/JSON must implement |
| `source/ChunkedQueryRowSource.java` | runtime | C (pattern) | **Analog for new `ChunkedCsvRowSource` / `ChunkedJsonRowSource`** |
| `source/CsvRowSource.java` | runtime | M | Keep for IN_MEMORY; delegate or split chunked impl |
| `source/JsonRowSource.java` | runtime | M | Same |
| `source/CsvSourceFactory.java` | runtime | M | **`QuerySourceFactory`** — policy-aware `create(name, source, policy)` |
| `source/JsonSourceFactory.java` | runtime | M | Same |
| `source/QuerySourceFactory.java` | runtime | — | Reference implementation for chunked dispatch |
| `parser/DefaultCsvParser.java` | runtime | M | Add BOM strip (UTF-8); streaming line iterator hook |
| `parser/DefaultJsonParser.java` | runtime | M | Add NDJSON line mode + streaming array parser (D-08) |
| `sink/CsvRowSinkAdapter.java` | runtime | M | Per-chunk append flush (today rewrites whole file per `write()`) |
| `sink/JsonRowSinkAdapter.java` | runtime | M | NDJSON append mode; ARRAY first-chunk vs subsequent-chunk |
| `sink/JdbcSinkSqlBuilder.java` | runtime | M | Extend upsert: `upsertKeys`, PG `ON CONFLICT DO UPDATE`, MySQL `ON DUPLICATE KEY UPDATE` |
| `sink/JdbcBulkWriteExecutor.java` | runtime | M | Upsert + bulk mode interaction (COPY rejects upsert today) |
| `runtime/TemplateV2RuntimeRegistry.java` | runtime | M | Pass `policy` to CSV/JSON factories like `QuerySourceFactory` |

### Model (data-generator-core)

| File | Role | Action | Closest analog |
|------|------|--------|----------------|
| `model/v2/RunReportVO.java` | model | M | Self — backward-compatible ctor pattern for new fields |
| `model/v2/StageMetricVO.java` | model | M | Self — add optional `rowsRead`, `rowsUpserted`, `rowsSkipped` |
| `model/v2/ExecutionPolicyVO.java` | model | — | Existing mode/chunk fields sufficient |
| `model/v2/CsvSourceVO.java` | model | M? | Optional `format` / size hint for validator |
| `model/v2/JsonSourceVO.java` | model | M? | Optional `format: ndjson \| array` (D-08 discretion) |

### Service (data-generator-service)

| File | Role | Action | Closest analog |
|------|------|--------|----------------|
| `template/TemplateV2Validator.java` | runtime | M | `collectWarnings()` + `validateGovernance()` patterns |
| `template/TemplateV2ControlPlaneService.java` | runtime | M | Merges validator warnings into validate/publish API |
| `task/RunReportCollector.java` | runtime | M | `buildSinkMetrics()` + `collectFailure()` |
| `resources/template/v2-scenarios/scenario-*-streaming-csv.yaml` | test fixture | C | `scenario-e-streaming-jdbc.yaml` |
| `resources/template/v2-scenarios/scenario-*-upsert-pg.yaml` | test fixture | C | `scenario-d-chunked-jdbc.yaml` + upsert options |
| `test/.../V2ScenarioTemplateIT.java` | test | M | Self — add scenarios to `scenarioResources()` |

### Console (data-generator-console-web)

| File | Role | Action | Closest analog |
|------|------|--------|----------------|
| `src/api/types.ts` | console | M | Extend `StageMetric`, `RunReport` mirrors |
| `src/app/pages/JobDetailPage.tsx` | console | M | Existing `sinkStageColumns` + `rowsOk`/`rowsFailed` |
| `src/app/editor/ReviewPanel.tsx` | console | M | `message.warning(validation.warnings.join(...))` on save |
| `src/i18n/locales/en.json`, `zh-CN.json` | console | M | Job report column labels, execution policy hints |
| Editor form hints (execution policy, upsert keys) | console | M | Existing template editor field components |

### Tests

| File | Role | Action | Closest analog |
|------|------|--------|----------------|
| `calcite/runtime/StreamingPipelineTests.java` | test | M | JDBC streaming baseline |
| `calcite/runtime/ChunkedPipelineTests.java` | test | M | JDBC chunked baseline |
| `calcite/runtime/ChunkedPipelinePostgresContainerTests.java` | test | C/M | Upsert idempotency PG |
| `calcite/runtime/ChunkedPipelineMySqlContainerTests.java` | test | C/M | Upsert idempotency MySQL |
| `calcite/TemplateV2RunnerTests.java` | test | M | `readsCsvSource*`, `readsJsonSource*` |
| `calcite/sink/JdbcSinkSqlBuilderTests.java` | test | M | Existing upsert dialect tests |
| `calcite/runtime/CsvJsonStreamingOomIT.java` (name TBD) | test | C | `-Xmx256m` + 10 MB fixture |
| `service/template/V2ScenarioTemplateIT.java` | test | M | Scenario harness |

### E2E & scripts

| File | Role | Action | Closest analog |
|------|------|--------|----------------|
| `console-web/e2e/specs/rw-streaming-upsert.spec.ts` | test | C | `datasource-v2-template-run.spec.ts`, `datasource-governance.spec.ts` |
| `console-web/e2e/helpers/template-run.ts` | test | M | Job report assertion types |
| `scripts/verify-phase8-uat-rw-streaming-upsert.ps1` | script | C | `verify-phase7-uat-datasource-governance.ps1` |

### Docs

| File | Role | Action | Closest analog |
|------|------|--------|----------------|
| `docs/template-v2-jdbc-sink-guide.md` | docs | M | Existing JDBC sink options |
| `docs/template-v2-streaming-csv-json-guide.md` (or extend streaming guide) | docs | C | `docs/template-v2-streaming-execution-guide.md` |
| `AGENTS.md` | docs | M | Phase 7 verify script entry pattern |

---

## Pattern Catalog

### 1. Execution policy dispatch (unchanged entry point)

`TemplateV2Runner` selects pipeline by resolved mode — Phase 8 does **not** add new modes:

```86:95:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2Runner.java
        if ("STREAMING".equals(policy.mode())) {
            return new StreamingPipeline(this::createSink).run(template, policy, registry);
        }
        if ("IN_MEMORY".equals(policy.mode())) {
            return new InMemoryPipeline(this::createSink).run(template, policy, registry);
        }
        if ("CHUNKED".equals(policy.mode())) {
            return new ChunkedPipeline(this::createSink).run(template, policy, registry);
        }
        throw new UnsupportedOperationException("Execution mode not yet supported: " + policy.mode());
```

**Phase 8 pattern:** CSV/JSON sources participate in CHUNKED/STREAMING by implementing `ChunkedRowSource`, not by changing this dispatch.

---

### 2. Chunked source factory gate (copy for CSV/JSON)

`QuerySourceFactory` is the template for policy-aware source creation:

```43:57:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/QuerySourceFactory.java
    public RowSource create(String name, SourceVO source, EffectiveExecutionPolicy policy) {
        QuerySourceVO querySource = (QuerySourceVO) source;
        // ... datasource resolution ...
        if (policy != null && usesChunkedRead(policy.mode())) {
            return new ChunkedQueryRowSource(name, querySource, jdbcTemplate, policy.sourceChunkSize());
        }
        return new QueryRowSource(name, querySource, jdbcTemplate);
    }

    private static boolean usesChunkedRead(String mode) {
        return "CHUNKED".equals(mode) || "STREAMING".equals(mode);
    }
```

**Apply to:** `CsvSourceFactory`, `JsonSourceFactory` — add `create(name, source, policy)` overload; register policy passthrough in `TemplateV2RuntimeRegistry.createSource()` (today only `QuerySourceFactory` / `PostGisQuerySourceFactory` receive policy).

**Chunked read contract:**

```19:43:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/ChunkedRowSource.java
public interface ChunkedRowSource extends RowSource {
    boolean hasNextChunk();
    List<Row> nextChunk(int maxRows);
    long rowsReadSoFar();
}
```

**Reference chunked JDBC impl:** `ChunkedQueryRowSource` — forward-only cursor, `rows()` intentionally empty, cumulative `rowsReadSoFar()`.

---

### 3. Per-chunk pipeline loop (CSV/JSON must plug in here)

Both pipelines share the same chunk → transform → sink flush shape:

```94:120:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/StreamingPipeline.java
        while (chunked.hasNextChunk()) {
            var chunk = chunked.nextChunk(chunkSize);
            if (chunk.isEmpty()) {
                continue;
            }
            metrics.incrementChunks();
            metrics.addRead(sourceName, chunk.size());
            // ... limit checks ...
            CalciteExecutionContext context = new CalciteExecutionContext()
                    .addTable(sourceName, chunkSchema, chunk);
            CalciteRowTransformer.TransformResult current =
                    registry.applyTransform(transformer, context);
            metrics.recordPeakRowsInMemory(current.rows().size());
            writeSinks(registry, template, current, metrics, sinkBatchSize);
        }
```

**Phase 8 constraints (D-04):** SQL evaluates **within each chunk only** — same as JDBC streaming v1. Cross-chunk joins/aggregates remain `IN_MEMORY`-only via `ExecutionShapeClassifier` + `validateChunkedCompatibility()`.

**Streaming vs CHUNKED for CSV/JSON:**

| Mode | Pipeline | Typical use (D-02) |
|------|----------|-------------------|
| `CHUNKED` | `ChunkedPipeline` | Large file export; may support broadcast join later (JDBC today) |
| `STREAMING` | `StreamingPipeline` | Strict single-source + `peakRowsInMemory` tracking |

**Required pipeline change:** `StreamingPipeline.soleQuerySource()` currently rejects non-`QuerySourceVO`:

```153:160:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/StreamingPipeline.java
    private static Map.Entry<String, QuerySourceVO> soleQuerySource(TemplateV2VO template) {
        Map.Entry<String, SourceVO> entry = template.getSources().entrySet().iterator().next();
        if (!(entry.getValue() instanceof QuerySourceVO querySource)) {
            throw new IllegalArgumentException(
                    "STREAMING mode v1 requires a QuerySourceVO source, got "
                            + entry.getValue().getClass().getSimpleName());
        }
        return Map.entry(entry.getKey(), querySource);
    }
```

Generalize to `SourceVO` + `ChunkedRowSource` check (mirrors lines 83–87).

---

### 4. IN_MEMORY CSV/JSON (unchanged baseline — D-21)

Current implementations fully materialize — **keep for small files**:

```56:72:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/CsvRowSource.java
    @Override
    public List<Row> rows() {
        List<List<String>> records = records();
        // ... materialize all rows ...
        return rows;
    }
```

```101:105:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/CsvRowSource.java
    private List<List<String>> records() {
        return csvParser.parse(source, readLines()).stream()
                .filter(record -> !record.stream().allMatch(String::isBlank))
                .toList();
    }
```

```55:60:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/JsonRowSource.java
    @Override
    public List<Row> rows() {
        long limit = source.getMaxRows() == null ? Long.MAX_VALUE : source.getMaxRows();
        return records().stream()
                .limit(limit)
                .map(values -> new Row(new LinkedHashMap<>(values)))
                .toList();
    }
```

**Phase 8:** New chunked sources read incrementally (line-by-line CSV/NDJSON; streaming JSON array parser) — do **not** break existing `rows()` paths used by `InMemoryPipeline`.

---

### 5. JSON sink modes (extend for streaming append)

`JsonRowSinkAdapter` already distinguishes ARRAY vs NDJSON — Phase 8 adds **per-chunk append** for streaming runs:

```29:51:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JsonRowSinkAdapter.java
    @Override
    public void write(RowSchema schema, List<Row> rows) {
        // ... today writes entire content in one Files.writeString ...
    }

    private String content(List<Row> rows, String mode) {
        return switch (mode) {
            case ARRAY_MODE -> jsonArray(rows);
            case NDJSON_MODE -> ndjson(rows);
            default -> throw new IllegalArgumentException("Unsupported JSON sink mode [" + mode + "]");
        };
    }
```

**Pattern:** First chunk creates file (ARRAY: `[`; NDJSON: first line); subsequent chunks append (`StandardOpenOption.APPEND`), mirroring `CsvRowSinkAdapter` append branch:

```53:57:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/CsvRowSinkAdapter.java
            if (append) {
                Files.write(path, lines, charset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.write(path, lines, charset, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
```

---

### 6. JDBC upsert SQL (extend existing builder)

Partial upsert exists today with `conflictColumns` (PG) and `insert ignore` (MySQL). Phase 8 standardizes on **`options.upsertKeys`** (D-12) and **update-on-conflict** (D-13, D-15):

```34:68:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilder.java
    static String buildSql(JdbcWriterVO writer, List<String> targetColumns) {
        String baseInsert = "insert into " + table + " (" + columns + ") values (" + values + ")";
        if (!WriterOptionResolver.booleanOption(writer, "upsert")) {
            return baseInsert;
        }
        return appendUpsertClause(writer, table, columns, values, baseInsert);
    }

    private static String appendPostgresUpsert(JdbcWriterVO writer, String baseInsert) {
        String conflictColumns = WriterOptionResolver.stringOption(writer, "conflictColumns", null);
        if (!StringUtils.hasText(conflictColumns)) {
            throw new IllegalArgumentException(
                    "JDBC sink upsert with dialect postgres requires options.conflictColumns");
        }
        return baseInsert + " on conflict (" + conflictColumns.trim() + ") do nothing";
    }
```

**Phase 8 target YAML:**

```yaml
sink:
  writers:
    - type: jdbc
      target: orders_out
      options:
        dialect: postgres   # or mysql
        upsert: true
        upsertKeys: [id]
```

**Test pattern to extend:**

```32:63:data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java
    @Test
    void buildsPostgresUpsertWhenDialectAndConflictColumnsAreSet() { ... }

    @Test
    void buildsMysqlInsertIgnoreWhenDialectIsMysql() { ... }
```

**Bulk mode guard (keep):**

```79:80:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java
        if (WriterOptionResolver.booleanOption(writer, "upsert")) {
            throw new IllegalArgumentException("JDBC sink bulkMode postgres_copy does not support upsert=true");
```

---

### 7. Run report model and aggregation (RW-04)

**Model — per-stage counters:**

```22:28:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/StageMetricVO.java
public record StageMetricVO(
        String name,
        Long rowsProcessed,
        Long durationMs,
        String errorSample,
        Long rowsOk,
        Long rowsFailed) implements Serializable {
```

**Phase 8 extension pattern:** Add optional fields (`rowsRead`, `rowsUpserted`, `rowsSkipped`) with backward-compatible record compact ctor — mirror `RunReportVO` null-normalization:

```38:44:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/RunReportVO.java
    public RunReportVO {
        if (aiCalls == null) {
            aiCalls = List.of();
        }
        if (transformErrors == null) {
            transformErrors = List.of();
        }
    }
```

**Runtime sink metrics today:**

```14:44:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/SinkWriteMetric.java
public final class SinkWriteMetric {
    private long rowsOk;
    private long rowsFailed;
    private String lastErrorSample;
    // Phase 8: rowsUpserted, rowsSkipped
}
```

**Collector mapping:**

```226:237:data-generator-service/src/main/java/org/gensokyo/data/task/RunReportCollector.java
    private static List<StageMetricVO> buildSinkMetrics(TemplateV2VO template, RunMetrics metrics, long outputRows) {
        for (Map.Entry<String, SinkWriteMetric> entry : metrics.getSinkMetrics().entrySet()) {
            SinkWriteMetric sinkMetric = entry.getValue();
            long processed = sinkMetric.getRowsOk() + sinkMetric.getRowsFailed();
            sinks.add(new StageMetricVO(
                    entry.getKey(),
                    processed,
                    null,
                    sinkMetric.getLastErrorSample(),
                    sinkMetric.getRowsOk(),
                    sinkMetric.getRowsFailed()));
        }
```

**Failure path (actionable errors — D-17):** `collectFailure()` already builds `TransformErrorVO` for terminal failures; extend for sink-level structured errors where applicable.

**Console mirror:**

```41:48:data-generator-console-web/src/api/types.ts
export interface StageMetric {
  name: string;
  rowsProcessed: number | null;
  durationMs: number | null;
  errorSample: string | null;
  rowsOk?: number | null;
  rowsFailed?: number | null;
}
```

**Job center UI — sink table already shows partial-success columns:**

```125:149:data-generator-console-web/src/app/pages/JobDetailPage.tsx
  const sinkStageColumns: ColumnsType<StageMetric> = useMemo(
    () => [
      { title: t('jobDetail.report.col.name'), dataIndex: 'name' },
      { title: t('jobDetail.report.col.rowsOk'), dataIndex: 'rowsOk', ... },
      { title: t('jobDetail.report.col.rowsFailed'), dataIndex: 'rowsFailed', ... },
      { title: t('jobDetail.report.col.error'), dataIndex: 'errorSample', ... },
    ],
    [t],
  );
```

Add columns for `rowsUpserted` / `rowsSkipped` following the same pattern.

---

### 8. Publish validation and warnings (D-05, D-14, D-19, D-20)

**Hard blocks** — throw in `TemplateV2Validator.validate()` / governance:

```391:423:data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java
    private static void validateExecutionPolicy(TemplateV2VO template) {
        // mode must be IN_MEMORY | CHUNKED | STREAMING
        if ("CHUNKED".equals(mode)) {
            validateChunkedCompatibility(template);
        }
    }
```

**Phase 8 add:** When `sink.options.upsert: true`, require non-empty valid `upsertKeys` — same severity as governance blocks (D-14).

**Warn-only** — `collectWarnings()`:

```153:170:data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java
    public static List<String> collectWarnings(TemplateV2VO template) {
        List<String> warnings = new ArrayList<>();
        appendMaxTotalRowsWarnings(template, warnings);
        return warnings;
    }

    private static void appendMaxTotalRowsWarnings(...) {
        if (("CHUNKED".equals(mode) || "STREAMING".equals(mode)) && policy.getMaxTotalRows() == null) {
            warnings.add("CHUNKED/STREAMING mode without maxTotalRows — consider setting a row cap...");
        }
    }
```

**Phase 8 add:** `IN_MEMORY` + large CSV/JSON file (≥10 MB or declared row estimate) → warning string (D-05, D-06).

**Console surfacing** — warnings on save/publish via toast:

```137:140:data-generator-console-web/src/app/editor/ReviewPanel.tsx
        const validation = await validateDraft(draft, id);
        if (validation.warnings.length > 0) {
          message.warning(validation.warnings.join(' · '));
        }
```

Wire publish mutation similarly for D-20 warn toast on large-file `IN_MEMORY`.

---

### 9. Scenario YAML harness (D-07)

**Reference streaming JDBC scenario:**

```1:35:data-generator-service/src/main/resources/template/v2-scenarios/scenario-e-streaming-jdbc.yaml
name: scenario-e-streaming-jdbc
executionPolicy:
  mode: STREAMING
  sourceChunkSize: 40
  sinkBatchSize: 40
  maxTotalRows: 160
sources:
  ledger:
    type: query
    sql: SELECT id, label FROM gf_ledger ORDER BY id
transform:
  type: sql
  sql: SELECT id, label FROM ledger
sink:
  writers:
    - type: jdbc
      target: gf_ledger_export
```

**IN_MEMORY CSV baseline (unchanged):**

```1:23:data-generator-service/src/main/resources/template/v2-scenarios/scenario-c-csv-export.yaml
name: scenario-c-csv-export
sources:
  incoming:
    type: csv
    path: template/v2-scenarios/fixtures/orders.csv
transform:
  type: sql
  sql: SELECT order_id, UPPER(customer) AS customer, amount FROM incoming
```

**Phase 8 new scenarios (planned):**

| Scenario | Mode | Validates |
|----------|------|-----------|
| `scenario-f-streaming-csv` (name TBD) | CHUNKED or STREAMING | Large CSV source → sink |
| `scenario-f-streaming-ndjson` | STREAMING | NDJSON source |
| `scenario-g-upsert-pg` | CHUNKED | PG upsert idempotent re-run |
| `scenario-g-upsert-mysql` | CHUNKED | MySQL upsert idempotent re-run |

**IT harness extension:**

```69:98:data-generator-service/src/test/java/org/gensokyo/data/template/V2ScenarioTemplateIT.java
    static Stream<String> scenarioResources() {
        return Stream.of(
                "scenario-a-synthetic.yaml",
                // ...
                "scenario-e-streaming-jdbc.yaml",
                // Phase 8: add new scenario YAMLs
        ).map(name -> SCENARIO_ROOT + name);
    }

    @ParameterizedTest
    @MethodSource("scenarioResources")
    void validatesAndRunsGreenfieldScenario(String resourcePath) throws Exception {
        TemplateV2VO template = loadNormalizedTemplate(resourcePath);
        prepareScenarioFixtures(template);
        TemplateV2Validator.validate(template);
        TemplateV2RunResult result = templateV2Runner.run(template);
        assertScenarioOutcome(resourcePath, template, result);
    }
```

**Streaming assertion pattern:**

```185:191:data-generator-service/src/test/java/org/gensokyo/data/template/V2ScenarioTemplateIT.java
            case "scenario-e-streaming-jdbc" -> {
                assertThat(result.getMetrics().getExecutionMode()).isEqualTo("STREAMING");
                assertThat(result.getMetrics().getPeakRowsInMemory()).isGreaterThan(0);
                assertThat(result.getMetrics().getRowsWritten()).isEqualTo(120L);
            }
```

---

### 10. Effective execution policy defaults

```22:23:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/EffectiveExecutionPolicy.java
    private static final int DEFAULT_SOURCE_CHUNK_SIZE = 5_000;
    private static final int DEFAULT_SINK_BATCH_SIZE = 1_000;
```

D-03 specifies **1000** default chunk when unset — `sinkBatchSize` already defaults to 1000; confirm whether CSV/JSON streaming should override `sourceChunkSize` from 5000 → 1000 or document explicit YAML. Scenario templates use explicit smaller chunks (40–50) for fast IT.

---

## Test Patterns

### Unit / module tests (data-generator-calcite)

| Pattern | Reference | Phase 8 use |
|---------|-----------|-------------|
| Streaming JDBC integration | `StreamingPipelineTests.streamsQuerySourceToJdbcSinkInBatches()` | Clone for CSV/JSON source → JDBC/console sink |
| Chunk size / peak memory | Assert `peakRowsInMemory <= chunkSize`, `chunksProcessed == rows/chunkSize` | OOM regression guard |
| Injected parser | `TemplateV2RunnerTests.readsCsvSourceThroughInjectedParser()` | Chunked parser without large fixtures |
| SQL builder dialect matrix | `JdbcSinkSqlBuilderTests` | Add `upsertKeys` PG UPDATE + MySQL DUPLICATE KEY |
| Shape rejection | `StreamingPipelineTests.rejectsMultipleSources()` | Ensure CSV/JSON streaming keeps v1 scope rules |

**Streaming JDBC test excerpt:**

```38:61:data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/StreamingPipelineTests.java
    @Test
    void streamsQuerySourceToJdbcSinkInBatches() {
        TemplateV2VO template = streamingJdbcTemplate(jdbcTemplate);
        TemplateV2RunResult result = new TemplateV2Runner(streamingRegistry(jdbcTemplate)).run(template);
        Assertions.assertTrue(result.getRows().isEmpty());
        Assertions.assertEquals("STREAMING", result.getMetrics().getExecutionMode());
        Assertions.assertTrue(result.getMetrics().getPeakRowsInMemory() <= 100);
        Assertions.assertEquals(ROW_COUNT / 100, result.getMetrics().getChunksProcessed());
    }
```

### Service IT (V2ScenarioTemplateIT)

- **Spring context:** `@SpringBootTest(classes = DataGeneratorApplication.class, properties = "spring.config.location=classpath:/application-phase7-test.yaml")`
- **Fixture prep:** `prepareScenarioFixtures()` switch on `template.getName()` — materialize large CSV to temp path, seed PG/MySQL via Testcontainers JDBC URLs
- **Partial sink errors:** `scenario-e-partial-sink` asserts per-writer `SinkWriteMetric` keys `sink[0].writer[0]`

### OOM proof IT (D-24) — new

```xml
<!-- surefire argLine example pattern -->
<argLine>-Xmx256m</argLine>
```

- Generate or ship **10 MB** CSV/NDJSON fixture (~100k rows)
- Run with `executionPolicy.mode: CHUNKED` or `STREAMING`
- Assert SUCCESS + expected row counts; JVM must not OOM
- Contrast: same fixture under `IN_MEMORY` may OOM (optional negative test, not CI gate)

### Testcontainers upsert (D-25)

Mirror existing chunked parity tests:

```22:40:data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresContainerTests.java
@EnabledIf("org.gensokyo.data.calcite.support.DockerTestSupport#dockerAvailable")
@Testcontainers
class ChunkedPipelinePostgresContainerTests {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void chunkedModeWritesAllRows() {
        ChunkedJdbcParitySupport.assertChunkedExportParity(...);
    }
}
```

**Phase 8 add:** `assertUpsertIdempotent()` — run template twice, assert row count unchanged, column values updated.

### Maven verify script slice (Phase 8)

Mirror Phase 7 governance script structure:

```42:61:scripts/verify-phase7-uat-datasource-governance.ps1
Write-Step "Maven slice — DatasourceGovernanceIT, ..."
$testList = @(
    'DatasourceGovernanceIT',
    ...
) -join ','
$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl data-generator-service -am `
    "-Dtest=$testList" `
    test
```

**Phase 8 `-SkipPlaywright` slice:** `V2ScenarioTemplateIT`, `StreamingPipelineTests`, `JdbcSinkSqlBuilderTests`, OOM IT, Postgres/MySQL upsert container tests.

---

## E2E Patterns

### Playwright scenario matrix (D-23)

Minimum 6 scenarios (5+ required):

| # | Scenario | API vs UI | Assertion focus |
|---|----------|-----------|-----------------|
| 1 | Large CSV CHUNKED | API | `waitForJobSuccess`, `report.executionMode`, row counts |
| 2 | PG upsert re-run | API | Two runs, same row count, updated values |
| 3 | Failed job report | API/UI | `report.sinks[].errorSample` or `transformErrors` visible |
| 4 | NDJSON STREAMING | API | SUCCESS + metrics |
| 5 | MySQL upsert re-run | API | Idempotency |
| 6 | IN_MEMORY large file warn | UI | Publish/save shows warning toast |

### Helper patterns

**Publish-run from catalog (Phase 6):**

```41:79:data-generator-console-web/e2e/helpers/template-run.ts
export async function createPublishRunFromScenario(
  request: APIRequestContext,
  scenarioId: string,
): Promise<string> {
  const { body: scaffoldBody } = await apiGetWithRole(
    request, `/api/templates/scenarios/${scenarioId}/scaffold`);
  // save → publish → run
  return instanceId;
}
```

**Job success polling:**

```89:100:data-generator-console-web/e2e/helpers/template-run.ts
export async function waitForJobSuccess(
  request: APIRequestContext,
  instanceId: string,
  timeoutMs = 90_000,
): Promise<JobDetailPayload> {
  await expect.poll(async () => {
    const { body } = await fetchJobDetail(request, instanceId);
    return unwrapApiData<JobDetailPayload>(body)?.execution?.status;
  }, { timeout: timeoutMs }).toBe('SUCCESS');
```

**Partial sink report assertion (reuse for RW-04):**

```32:42:data-generator-console-web/e2e/specs/datasource-v2-template-run.spec.ts
  test('GF-EP API: inline JDBC sink ... partial sink run succeeds', async ({ request }) => {
    const detail = await waitForJobSuccess(request, instanceId);
    const sinks = detail.execution?.report?.sinks ?? [];
    const failingWriter = sinks.find((row) => row.name === 'sink[0].writer[0]');
    expect(Number(failingWriter?.rowsFailed)).toBe(3);
    expect(Number(okWriter?.rowsOk)).toBe(3);
  });
```

### UAT script orchestration (Phase 6/7 → Phase 8)

```1:9:scripts/verify-phase6-uat-v2-template-run.ps1
# Phase 6 UAT — V2 template run: Podman + Playwright + Maven IT.
param(
    [switch]$SkipBuild,
    [switch]$SkipMavenIt,
    [switch]$KeepContainer,
    ...
)
```

**Phase 8 script (`verify-phase8-uat-rw-streaming-upsert.ps1`):**

1. Maven slice (`-SkipPlaywright`) — IT classes listed above  
2. Optional `-SkipBuild` / Podman image build (same as Phase 7)  
3. Playwright: `npx playwright test e2e/specs/rw-streaming-upsert.spec.ts`  
4. Health wait on `:9876/healthz`  
5. Register new scenario IDs in template catalog for E2E scaffold paths  

### Phase 7 governance spec as structural template

`datasource-governance.spec.ts` demonstrates:

- API-first tests with `test.setTimeout(120_000)`  
- Mixed API + UI flows in one describe block  
- Helper module separation (`datasource-governance.ts` vs `template-run.ts`)  
- Cleanup of created datasources/templates via API  

Phase 8 should follow the same **describe → API tests → UI smoke** layout without inventing parallel harness infrastructure.

---

## Cross-cutting checklist for implementers

1. **Extend, don't fork** — new CSV/JSON chunked types plug into existing pipelines and `V2ScenarioTemplateIT`.
2. **Explicit modes only** — no file-size auto-promotion (D-01).
3. **Fail-fast upsertKeys** at validate + run (D-14).
4. **Warn-only large IN_MEMORY** via `collectWarnings` + console toast (D-05, D-20).
5. **Backward-compatible report fields** — optional new `StageMetricVO` components (D-16 discretion).
6. **Document fixture bar** — 10 MB / ~100k rows in operator guide + OOM IT (D-06, D-27).
7. **PG/MySQL upsert only** — other dialects deferred to Phase 9 (D-13).

---

*Pattern mapping for Phase 8 — downstream planners and executors should treat this document as the code-pattern companion to `08-CONTEXT.md`.*
