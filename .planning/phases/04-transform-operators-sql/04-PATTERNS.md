# Phase 4: Transform Operators & SQL - Pattern Map
**Mapped:** 2026-06-22
**Files analyzed:** 22
**Analogs found:** 21/22

> READ-ONLY analysis. Every excerpt below is copied verbatim from an existing source file with
> file path + line numbers so the planner/implementer can mirror established patterns. New built-in
> operators plug into the **exact** `TransformVO` subtype + `V2TransformFactory` + `@AutoService`/`@JsonSubType`
> registration path used by `sql`/`js`/`spel` today — no runtime-dispatch changes needed (CONTEXT D-13 additive).

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `model/v2/JsonTransformVO.java` (new) | model/VO | transform | `model/v2/SqlTransformVO.java`, `JsTransformVO.java` | Strong |
| `model/v2/MaskTransformVO.java` (new) | model/VO | transform | `model/v2/SpelTransformVO.java` (+ `SpelColumnMapping`) | Strong |
| `model/v2/LookupTransformVO.java` (new) | model/VO | transform | `model/v2/SqlTransformVO.java`, `SpelTransformVO.java` | Strong |
| `model/v2/MaskRuleVO.java` (new, list element) | model/VO | transform | `model/v2/SpelColumnMapping.java` | Strong |
| `calcite/transform/JsonTransformFactory.java` (new) | factory/runtime | transform | `calcite/sql/SpelTransformFactory.java`, `transform/JsTransformFactory.java` | Strong |
| `calcite/transform/MaskTransformFactory.java` (new) | factory/runtime | transform | `calcite/sql/SpelTransformFactory.java` | Strong |
| `calcite/transform/LookupTransformFactory.java` (new) | factory/runtime | transform | `calcite/sql/SpelTransformFactory.java` (+ multi-table `context.getData()`) | Strong |
| `calcite/sql/TemplateV2JsonSqlFunctions.java` (new, internal) | runtime (SQL fn) | transform | `calcite/sql/` Faker/Geo fn classes via `TemplateV2SqlFunctionRegistry.builtIn()` | Medium |
| `calcite/sql/TemplateV2SqlFunctionRegistry.java` (modify) | config/registrar | transform | self (`builtIn()` list, lines 32-341) | Strong |
| `calcite/plugin/DefaultTemplateV2RuntimePlugin.java` (modify) | config/registrar | transform | self (lines 28-54) | Strong |
| `api/console/ConsoleTransformCatalogController.java` (new) | controller | request-response | `api/console/ConsoleUdfController.java` | Strong |
| `api/console/dto/TransformCatalogEntryView.java` (new) | dto | request-response | `api/console/dto/UdfVersionView.java`, `UdfGroupView.java` | Strong |
| `<catalog source>` e.g. `udf/TransformCatalogSource.java` (new) | runtime/service | request-response | `udf/DefaultRegistrySqlFunctionSource.java` | Strong |
| `task/RunReportCollector.java` (modify) | runtime/service | transform | self (lines 53-213) | Strong |
| `model/v2/TransformErrorVO.java` (new) | model/VO | transform | `model/v2/StageMetricVO.java`, `AiCallMetricVO.java` | Strong |
| `model/v2/RunReportVO.java` (modify) | model/VO | transform | self (record + compact ctor, lines 24-41) | Strong |
| `api/console/dto/JobExecutionDetail.java` (modify, surfacing) | dto | request-response | self (lines 19-23) | Strong |
| `.planning/test-matrix.yaml` (modify) | test (matrix) | n/a | self (rows 195-211) | Strong |
| `fixtures/templates/transform-{json,mask,lookup}.yaml` (new) | test (sample) | transform | `fixtures/templates/transform-sql-basic.yaml` | Strong |
| `testfixtures/FixtureTransform{Json,Mask,Lookup}ExampleTests.java` (new) | test | transform | `testfixtures/FixtureTransformSqlExampleTests.java` | Strong |
| `calcite/.../{Json,Mask,Lookup}TransformFactoryTests.java` (new) | test | transform | `calcite/sql/SpelTransformFactoryTests.java`, `transform/JsTransformFactoryTests.java` | Strong |
| `api/console/ConsoleTransformCatalogControllerTest.java` (new) | test | request-response | `api/console/ConsoleUdfControllerTest.java` | Strong |

---

## Pattern Assignments

### `model/v2/JsonTransformVO.java` (model/VO, transform)
**Analog:** `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/SqlTransformVO.java`

**imports/copyright + subtype declaration** (`SqlTransformVO.java` lines 1-19): note copyright block is REQUIRED per java-copyright rule (present on `JsTransformVO`, absent on `SqlTransformVO` — follow `JsTransformVO`):

```1:19:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/SqlTransformVO.java
package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

@Getter
@Setter
@AutoService(TransformVO.class)
@JsonSubType("SQL")
public class SqlTransformVO extends TransformVO {
    public SqlTransformVO() {
        setType("sql");
    }

    private String dialect;
    private String sql;
}
```

**Copyright + Javadoc convention to apply** (mirror `JsTransformVO.java` lines 1-40 — copyright block, type Javadoc with `@author`/`@since`, ctor sets `type`, field-level Javadoc, public constants):

```1:45:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/JsTransformVO.java
/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

/**
 * Row-local JavaScript transform executed in a sandboxed GraalJS context.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@AutoService(TransformVO.class)
@JsonSubType("JS")
public class JsTransformVO extends TransformVO {
    ...
    public JsTransformVO() {
        setType("js");
    }
    /** Inline JavaScript body evaluated once per input row ... */
    private String script;
```

**Registration is automatic:** `@AutoService(TransformVO.class)` + `@JsonSubType("JSON")` means `JsonSubtypeRegistry.loadSubtypes(TransformVO.class)` (already called in `TemplateObjectMapperFactory`, lines 70) discovers the subtype with no central-file edit. `@JsonSubType` value yields BOTH the uppercase ID and its lowercase alias (`JsonSubtypeRegistry.namedTypes`, lines 57-67), so YAML `type: json` resolves. Base `TransformVO` discriminates by `type` (`TransformVO.java` lines 11-12). Suggested fields per CONTEXT D-02: `sourceColumn`, `targetColumn`, `flatten` (boolean), `separator` (default `.`).

---

### `model/v2/MaskTransformVO.java` (model/VO, transform)
**Analog:** `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/SpelTransformVO.java` (list-of-rules shape) + `SpelColumnMapping.java` (list element).

**list-of-rules VO pattern** (`SpelTransformVO.java` lines 22-39):

```22:39:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/SpelTransformVO.java
@Getter
@Setter
@AutoService(TransformVO.class)
@JsonSubType("SPEL")
public class SpelTransformVO extends TransformVO {

    /**
     * Creates a transform with {@code type} set to {@code spel}.
     */
    public SpelTransformVO() {
        setType("spel");
    }

    /**
     * Output columns to add or replace; each mapping is evaluated once per input row.
     */
    private List<SpelColumnMapping> columns = new ArrayList<>();
}
```

**list-element VO pattern** for `MaskRuleVO` (`SpelColumnMapping.java` lines 12-30 — `@Data`, `Serializable`, field Javadoc). Per CONTEXT D-03 each rule carries `column` + `strategy` (one of `email`/`phone`/`credit-card`/`generic-fixed`):

```12:30:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/SpelColumnMapping.java
/**
 * One output column produced by a row-local SpEL expression in {@link SpelTransformVO}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@Data
public class SpelColumnMapping implements Serializable {

    /**
     * Output column name to add or replace on each row.
     */
    private String name;

    /**
     * SpEL expression evaluated per row, e.g. {@code "#row['id'] + '-x'"}.
     */
    private String expression;
}
```

---

### `model/v2/LookupTransformVO.java` (model/VO, transform)
**Analog:** `SqlTransformVO.java` (flat scalar fields). Per CONTEXT D-04 fields reference a named in-template source: e.g. `source` (named source key), `leftKey`, `rightKey`, `columns` (projected lookup columns). Same `@AutoService(TransformVO.class)` + `@JsonSubType("LOOKUP")` + `setType("lookup")` skeleton as shown above.

---

### `calcite/transform/JsonTransformFactory.java` (factory/runtime, transform)
**Analog:** `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/SpelTransformFactory.java` — row-local per-row transform that reads table `input`, merges schema, returns `TransformResult`.

**factory contract** (`V2TransformFactory.java` lines 8-12):

```8:12:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/V2TransformFactory.java
public interface V2TransformFactory {
    boolean supports(TransformVO transform);

    CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context);
}
```

**supports + apply + schema-merge core pattern** (`SpelTransformFactory.java` lines 36-120). Mirror: read `input` table, validate presence, build output schema (merge input cols + new cols via `ColumnDef`), iterate rows producing `new Row(values)`:

```36:106:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/SpelTransformFactory.java
public class SpelTransformFactory implements V2TransformFactory {

    private static final String INPUT_TABLE = "input";
    ...
    @Override
    public boolean supports(TransformVO transform) {
        return transform instanceof SpelTransformVO;
    }
    ...
    @Override
    public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
        SpelTransformVO spelTransform = (SpelTransformVO) transform;
        RowSchema inputSchema = context.getSchemas().get(INPUT_TABLE);
        List<Row> inputRows = context.getData().get(INPUT_TABLE);
        if (inputSchema == null || inputRows == null) {
            throw new IllegalArgumentException("SpEL transform requires table '" + INPUT_TABLE + "' in execution context");
        }

        List<ParsedMapping> mappings = parseMappings(spelTransform.getColumns());
        RowSchema outputSchema = mergeSchema(inputSchema, mappings);
        List<Row> outputRows = new ArrayList<>(inputRows.size());
        for (Row inputRow : inputRows) {
            outputRows.add(applyMappings(inputRow, mappings));
        }
        return new CalciteRowTransformer.TransformResult(outputSchema, outputRows);
    }
    ...
    private static RowSchema mergeSchema(RowSchema inputSchema, List<ParsedMapping> mappings) {
        Map<String, ColumnDef> columns = new LinkedHashMap<>();
        if (inputSchema.getColumns() != null) {
            for (ColumnDef column : inputSchema.getColumns()) {
                columns.put(column.getName().toLowerCase(Locale.ROOT), column);
            }
        }
        for (ParsedMapping mapping : mappings) {
            String name = mapping.name();
            columns.put(name.toLowerCase(Locale.ROOT), new ColumnDef(name, "ANY", true));
        }
        RowSchema schema = new RowSchema();
        schema.setColumns(List.copyOf(columns.values()));
        return schema;
    }
```

**error handling:** throw `IllegalArgumentException` for bad config / missing input (client error → 400 / fail-fast per CONTEXT D-10); the runtime wraps any `RuntimeException` with step context (see `TemplateV2RuntimeRegistry.applyTransform` below). JSON parse failure should carry the offending column + raw value to satisfy D-08.

**copyright + Javadoc:** `JsTransformFactory.java`/`SpelTransformFactory.java` both carry the copyright block + type Javadoc + `@Override` method Javadoc (lines 1-40 of `JsTransformFactory.java`). `SqlTransformFactory.java` does NOT (older file) — follow the documented ones.

---

### `calcite/transform/MaskTransformFactory.java` (factory/runtime, transform)
**Analog:** `SpelTransformFactory.java` (same per-row + per-column-rule shape as above). Strategies (`email`/`phone`/`credit-card`/`generic-fixed`, CONTEXT D-03) selected by name — model as a `switch` over strategy name (mirror `DefaultRegistrySqlFunctionSource.returnTypeInference` switch, lines 75-83, for the idiom). Masks replace the column value in place; output schema == input schema (no new columns), so `mergeSchema` simplifies to returning `inputSchema`.

---

### `calcite/transform/LookupTransformFactory.java` (factory/runtime, transform)
**Analog:** `SpelTransformFactory.java` for the per-row loop; the join needs a SECOND table from the execution context. `CalciteExecutionContext` exposes ALL named tables via `context.getData()` / `context.getSchemas()` (see lines 67-68 of `SpelTransformFactory`). The lookup source name (CONTEXT D-04) keys into that map — `context.getData().get(lookupSourceName)` — build a key→row index once, then enrich each input row. Missing/duplicate keys surface as the D-08 failure contract (throw `IllegalArgumentException`/`IllegalStateException` with key + source). Cross-check: `FixtureTransformSqlExampleTests` shows multiple sources land as named tables (`template.setSources(Map.of("orders", source))`, line 72).

---

### `calcite/sql/TemplateV2JsonSqlFunctions.java` (runtime SQL fn, internal-only — CONTEXT D-11/D-12)
**Analog:** the Faker/Geo function helper classes referenced from `TemplateV2SqlFunctionRegistry.builtIn()` (e.g. `TemplateV2GeoSqlFunctions`, `TemplateV2FakerFunctions`). A `TemplateV2SqlFunction` is a record bundling name + return-type inference + operand checker + evaluator:

```8:29:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/TemplateV2SqlFunction.java
public record TemplateV2SqlFunction(String name,
                                    SqlReturnTypeInference returnTypeInference,
                                    SqlOperandTypeChecker operandTypeChecker,
                                    TemplateV2SqlFunctionEvaluator evaluator) {
    public TemplateV2SqlFunction {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("SQL function name must not be blank");
        }
        ...
    }

    public SqlFunction toSqlFunction() {
        return SqlBasicFunction.create(name, returnTypeInference, operandTypeChecker);
    }
}
```

**registration into builtIn()** (`TemplateV2SqlFunctionRegistry.builtIn()` lists every built-in; geo entries lines 262-340 are the closest analog for adding new `V2_*`-prefixed internal functions). NAME PREFIX must avoid the UDF `sqlName` namespace (CONTEXT D-12) — use the established `V2_` prefix, e.g. `V2_JSON_EXTRACT`:

```262:272:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/TemplateV2SqlFunctionRegistry.java
                new TemplateV2SqlFunction("V2_GEO_DISTANCE_METERS", ReturnTypes.DOUBLE_NULLABLE,
                        OperandTypes.family(
                                List.of(
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC,
                                        SqlTypeFamily.NUMERIC
                                ),
                                count -> count == 4
                        ),
                        TemplateV2GeoSqlFunctions::distanceMeters),
```

> Note: per CONTEXT D-11 add a scalar function ONLY if `json` genuinely needs SQL-path extraction internally; the operator may instead parse in pure Java (preferred simpler path per CLAUDE.md §2). Mark this file Medium-match / optional.

---

### `calcite/plugin/DefaultTemplateV2RuntimePlugin.java` (config/registrar, transform) — MODIFY
**Analog:** self. Built-in factories are listed in `transformFactories(...)` and capabilities in `descriptor()`. Add the three new factories + `transform(...)` capabilities:

```28:54:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/plugin/DefaultTemplateV2RuntimePlugin.java
                .capability(TemplateV2PluginCapability.transform("sql"))
                .capability(TemplateV2PluginCapability.transform("spel"))
                .capability(TemplateV2PluginCapability.transform("js"))
                ...
    @Override
    public List<V2TransformFactory> transformFactories(TemplateV2SqlFunctionRegistry sqlFunctionRegistry) {
        return List.of(new SqlTransformFactory(sqlFunctionRegistry), new SpelTransformFactory(), new JsTransformFactory());
    }
```

The factory-dispatch loop is unchanged — `TemplateV2RuntimeRegistry.applyTransform` iterates `transformFactories` and calls the first whose `supports(...)` matches, wrapping `RuntimeException` in a step-located `IllegalStateException` (the seed of D-08 error surfacing):

```64:75:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2RuntimeRegistry.java
    public CalciteRowTransformer.TransformResult applyTransform(TransformVO transform, CalciteExecutionContext context) {
        for (V2TransformFactory factory : transformFactories) {
            if (factory.supports(transform)) {
                try {
                    return factory.apply(transform, context);
                } catch (RuntimeException e) {
                    throw runtimeFailure("transform", transform.getType(), transform.getClass(), factory.getClass(), e);
                }
            }
        }
        throw new UnsupportedOperationException("Unsupported V2 transformer in current runner: " + transform.getClass().getSimpleName());
    }
```

---

### `api/console/ConsoleTransformCatalogController.java` (controller, request-response)
**Analog:** `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleUdfController.java` — `@RestController` + `/api/console/...` + `@RequiredArgsConstructor` final-field injection + `R<T>` envelope + GET listing + structured errors bubble to `ConsoleApiAdvice` (no controller try/catch).

```50:58:data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleUdfController.java
@RestController
@RequestMapping("/api/console/udfs")
@RequiredArgsConstructor
public class ConsoleUdfController {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final UdfRegistryService udfRegistryService;
    private final UdfPublishService udfPublishService;
```

**list endpoint returning `R<List<...>>`** (CONTEXT D-05 endpoint e.g. `/api/console/transforms`, D-06 unified built-in + published UDFs):

```187:207:data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleUdfController.java
    @GetMapping
    public R<List<UdfGroupView>> list(@RequestParam(required = false) String type) {
        Optional<UdfType> typeFilter = (type == null || type.isBlank())
                ? Optional.empty()
                : Optional.of(UdfType.fromValue(type));
        return R.ok(groupByUdfId(udfRegistryService.list(typeFilter)));
    }

    @GetMapping("/{udfId}")
    public R<UdfGroupView> history(@PathVariable String udfId) {
        ...
        return R.ok(UdfGroupView.of(udfId, records));
    }
```

---

### `api/console/dto/TransformCatalogEntryView.java` (dto, request-response)
**Analog:** `api/console/dto/UdfVersionView.java` — a `record` with copyright + Javadoc on every `@param`, a static `from(...)` projector, and NEVER exposing payload bytes. CONTEXT D-07 entry metadata: `type` name, description, parameter schema (fields + types), usage example, plus a `kind`/`source` tag (built-in vs UDF, D-06).

```31:58:data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/UdfVersionView.java
public record UdfVersionView(
        String udfId,
        String version,
        String type,
        String state,
        Instant registeredAt,
        Instant publishedAt,
        Instant deprecatedAt,
        Map<String, String> metadata) {

    /**
     * Maps a registry record to its console view, dropping the payload bytes (D-14).
     *
     * @param record source registry record
     * @return per-version view
     */
    public static UdfVersionView from(UdfRecord record) {
        return new UdfVersionView(
                record.udfId(),
                ...
                record.metadata());
    }
}
```

For grouping/derived helpers (e.g. group by kind), see `UdfGroupView.of(...)` (`UdfGroupView.java` lines 35-43).

---

### `udf/TransformCatalogSource.java` (runtime/service, request-response) — catalog data source
**Analog:** `data-generator-service/src/main/java/org/gensokyo/data/udf/DefaultRegistrySqlFunctionSource.java` — iterates the UDF registry filtering to PUBLISHED entries; the catalog folds these into the unified list (CONTEXT D-06). Built-in operator descriptors can be authored (a static list) or derived from registered `TransformVO` subtypes.

```50:63:data-generator-service/src/main/java/org/gensokyo/data/udf/DefaultRegistrySqlFunctionSource.java
    @Override
    public List<TemplateV2SqlFunction> publishedSqlFunctions() {
        List<TemplateV2SqlFunction> functions = new ArrayList<>();
        for (UdfRecord record : registry.list(Optional.empty())) {
            if (record.state() != UdfLifecycleState.PUBLISHED) {
                continue;
            }
            if (record.type() != UdfType.SQL && record.type() != UdfType.SCRIPT) {
                continue;
            }
            functions.add(toSqlFunction(record));
        }
        return functions;
    }
```

Wire the bean in `CoreConfig` with `@Bean @ConditionalOnMissingBean(...)` injecting `UdfRegistry` (mirror `registrySqlFunctionSource` bean, `CoreConfig.java` lines 365-370).

---

### `task/RunReportCollector.java` (runtime/service, transform) — MODIFY for XFORM-05
**Analog:** self. The collector already projects `TransformVO` step names with `transformers[index]`-style paths — extend it to attach structured error entries (CONTEXT D-08 step-path + root-cause; D-09 reuse one shape across report + job detail).

**existing step-path naming** (reuse for the error step locator):

```194:202:data-generator-service/src/main/java/org/gensokyo/data/task/RunReportCollector.java
    private static String transformName(TransformVO transformer, int index) {
        if (transformer.getName() != null && !transformer.getName().isBlank()) {
            return transformer.getName();
        }
        if (transformer.getType() != null && !transformer.getType().isBlank()) {
            return transformer.getType() + "[" + index + "]";
        }
        return "transform[" + index + "]";
    }
```

**existing error-sample aggregation to extend** (currently sink-only + warnings; add transform/UDF structured errors):

```204:213:data-generator-service/src/main/java/org/gensokyo/data/task/RunReportCollector.java
    private static List<String> collectErrorSamples(RunMetrics metrics) {
        List<String> samples = new ArrayList<>();
        for (SinkWriteMetric sinkMetric : metrics.getSinkMetrics().values()) {
            if (sinkMetric.getLastErrorSample() != null) {
                samples.add(sinkMetric.getLastErrorSample());
            }
        }
        samples.addAll(metrics.getWarnings());
        return samples;
    }
```

The runtime side already produces a located message in `TemplateV2RuntimeRegistry.runtimeFailure(...)` (`...for type [<type>] and model [<class>]`, lines 100-108) — capture that as the root-cause string for the new error entry.

---

### `model/v2/TransformErrorVO.java` (model/VO, transform) — structured error shape (D-08/D-09)
**Analog:** `model/v2/StageMetricVO.java` — a `Serializable` record with copyright + `@param` Javadoc per component. Suggested components: `step` (e.g. `transformers[2]`), `operatorType`, `operatorName`, `message`, nullable `row`/`column` locators.

```10:29:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/StageMetricVO.java
/**
 * Per-stage counters for a Template V2 run report.
 *
 * @param name           stage identifier (source name, transform name, or sink key)
 * ...
 */
public record StageMetricVO(
        String name,
        Long rowsProcessed,
        Long durationMs,
        String errorSample,
        Long rowsOk,
        Long rowsFailed) implements Serializable {
}
```

---

### `model/v2/RunReportVO.java` (model/VO, transform) — MODIFY additively (CONTEXT D-13)
**Analog:** self. Add a `List<TransformErrorVO> transformErrors` (or similar) component and normalize nulls in the compact constructor so older persisted reports still deserialize (back-compat pattern already used for `aiCalls`):

```24:41:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/RunReportVO.java
public record RunReportVO(
        List<StageMetricVO> sources,
        List<StageMetricVO> transformers,
        List<StageMetricVO> sinks,
        String executionMode,
        Long durationMs,
        List<String> errorSamples,
        List<AiCallMetricVO> aiCalls) implements Serializable {

    /**
     * Normalizes nullable collections for backward-compatible report deserialization.
     */
    public RunReportVO {
        if (aiCalls == null) {
            aiCalls = List.of();
        }
    }
}
```

---

### `api/console/dto/JobExecutionDetail.java` (dto, request-response) — surfacing in console job detail (D-09)
**Analog:** self — a `record` aggregating execution + optional metrics. The transform/UDF errors reach console job detail through the persisted run report (the report VO above is serialized into the execution row); confirm whether a dedicated component must be added here or whether the existing report JSON carried on `TaskExecutionSummary` is enough.

```19:23:data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/JobExecutionDetail.java
public record JobExecutionDetail(
        TaskExecutionSummary execution,
        DistributedJobView distributedJob,
        PartitionRunMetrics partitionMetrics) {
}
```

---

### Harness rows + sample templates + tests (XFORM-06)

**matrix rows** — `.planning/test-matrix.yaml` (add `transform-json`, `transform-mask`, `transform-lookup` mirroring rows 195-211):

```195:211:.planning/test-matrix.yaml
  - id: transform-sql-basic
    capability: transform-sql
    adapter: calcite-sql
    test_types: [integration]
    owner_module: data-generator-test-fixtures
    status: partial
    linked_tests: [FixtureTransformSqlExampleTests]
    notes: Embedded SQL transform example from data-generator-test-fixtures.

  - id: calcite-transform-js
    capability: calcite-transform-js
    adapter: javascript
    test_types: [unit]
    owner_module: data-generator-calcite
    status: pending
    linked_tests: []
    notes: GraalJS row transform factory.
```

**sample template + embedded fixture test** — `FixtureTransformSqlExampleTests.java` (CONTEXT embedded-first): build a `TemplateV2VO` with named sources, transformers, console sink; run via `new TemplateV2Runner(registry)`; assert output rows. Sample YAML lives at `data-generator-test-fixtures/src/main/resources/fixtures/templates/transform-sql-basic.yaml` (+ matching `fixtures/sql/*.sql` seed):

```54:83:data-generator-test-fixtures/src/test/java/org/gensokyo/data/testfixtures/FixtureTransformSqlExampleTests.java
    private static TemplateV2VO transformTemplate() {
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId("ignored");
        source.setSql("SELECT id, amount FROM fixture_orders ORDER BY id");

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT COUNT(*) AS order_count, SUM(amount) AS total_amount FROM orders");
        ...
        template.setSources(Map.of("orders", source));
        template.setTransformers(List.of(transform));
        template.setSinks(List.of(sink));
        return template;
    }

    private static TemplateV2RuntimeRegistry transformRegistry(NamedParameterJdbcTemplate jdbcTemplate) {
        return new TemplateV2RuntimeRegistry(
                List.of(new QuerySourceFactory(jdbcTemplate)),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
```

**factory unit tests** — `SpelTransformFactoryTests.java` / `JsTransformFactoryTests.java` (in `data-generator-calcite/src/test/...`); build a `CalciteExecutionContext` with table `input`, call `apply`, assert output rows/schema.

**controller test** — `ConsoleUdfControllerTest.java` (mirror for the catalog GET endpoint, `R<T>` assertions).

---

## Shared Patterns

### Java copyright + Javadoc convention — **Source:** `data-generator-calcite/.../transform/JsTransformFactory.java` lines 1-40 — **Apply to:** ALL new `.java` files
Order = copyright block → `package` → imports → type Javadoc (`@author`, `@since`) → body; Javadoc on every public method/ctor with `@param`/`@return`/`@throws`; `//` inline only for non-obvious steps (per `.cursor/rules/java-copyright-class-javadoc.mdc`). NOTE: some older analogs (`SqlTransformVO`, `SqlTransformFactory`, `V2TransformFactory`, `TemplateV2RuntimeRegistry`) PREDATE the rule and lack the block — do NOT copy that omission; follow the documented files.

```1:5:data-generator-calcite/src/main/java/org/gensokyo/data/calcite/transform/JsTransformFactory.java
/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
```

### AutoService + JsonSubType subtype registration — **Source:** `JsTransformVO.java` lines 21-22, `JsonSubtypeRegistry.java` lines 57-67, `TemplateObjectMapperFactory.java` line 70 — **Apply to:** all new `*TransformVO`
Annotate each new VO `@AutoService(TransformVO.class)` + `@JsonSubType("JSON"|"MASK"|"LOOKUP")`. ServiceLoader discovery is already wired (`TemplateObjectMapperFactory.registerTemplateSubtypes` loads `TransformVO` subtypes), so NO edit to `TemplateModelSubtypeRegistrar` (that path is only for PF4J plugin classloaders, not in-tree subtypes). The annotation value auto-generates both uppercase ID + lowercase alias:

```57:67:data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/json/JsonSubtypeRegistry.java
    private static NamedType[] namedTypes(Class<?> subtype) {
        JsonSubType annotation = subtype.getAnnotation(JsonSubType.class);
        if (annotation == null || blank(annotation.value())) {
            return new NamedType[]{new NamedType(subtype)};
        }
        String typeId = annotation.value();
        return new NamedType[]{
                new NamedType(subtype, typeId),
                new NamedType(subtype, typeId.toLowerCase())
        };
    }
```

### `R<T>` envelope + ConsoleApiAdvice error handling — **Source:** `ConsoleUdfController.java` lines 187-193, `ConsoleApiAdvice.java` lines 33-49 — **Apply to:** catalog controller
Controllers return `R.ok(...)`; throw `IllegalArgumentException` for client errors; central `@RestControllerAdvice(basePackages="org.gensokyo.data.api.console")` maps it to HTTP 400 `R.fail`. No controller-side try/catch.

```33:49:data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleApiAdvice.java
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> badRequest(IllegalArgumentException ex) {
        return R.fail(ex.getMessage());
    }
    ...
    @ExceptionHandler(UdfRegistryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<UdfErrorPayload> udfRegistryError(UdfRegistryException ex) {
        return R.fail(ex.getMessage(), new UdfErrorPayload(ex.code(), ex.errors()));
    }
```

### Embedded-first fixture test linkage — **Source:** `FixtureTransformSqlExampleTests.java` lines 34-83 — **Apply to:** all new operator E2E rows
H2 datasource via `FixtureTestSupport.h2DataSource(...)` + `H2Seed.apply(...)`; assert the matrix row id is referenced; build the registry directly with only the needed factories; run via `TemplateV2Runner`. `linked_tests` in `test-matrix.yaml` must name the new `Fixture*ExampleTests` class.

### DTO record projector — **Source:** `UdfVersionView.java` lines 32-58, `StageMetricVO.java`, `RunReportVO.java` — **Apply to:** catalog DTO + error VO
All wire/report types are immutable `record`s implementing `Serializable` (model/v2) with static `from(...)`/`of(...)` factories and full `@param` Javadoc; never expose code/payload bytes.

---

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `calcite/sql/TemplateV2JsonSqlFunctions.java` (internal JSON scalar fn) | runtime (SQL fn) | transform | No JSON-path scalar function exists today; closest idiom is `TemplateV2GeoSqlFunctions`/Faker fns registered in `TemplateV2SqlFunctionRegistry.builtIn()`, but the JSON evaluator body has no direct precedent. Marked Medium and OPTIONAL — per CONTEXT D-11 only add if the `json` operator genuinely needs it; pure-Java parsing inside `JsonTransformFactory` is the simpler default (CLAUDE.md §2). |

---

## Metadata
**Analog search scope:** `data-generator-calcite` (transform/sql/runtime/plugin/udf packages), `data-generator-common/data-generator-core` (model/v2, json), `data-generator-service` (api.console + dto, task, config, udf), `data-generator-test-fixtures` (fixtures + example tests), `.planning/test-matrix.yaml`.
**Files scanned (read in full):** 22 — `V2TransformFactory`, `TransformVO`, `JsTransformFactory`, `SqlTransformFactory`, `SpelTransformFactory`, `JsTransformVO`, `SqlTransformVO`, `SpelTransformVO`, `SpelColumnMapping`, `JsonSubtypeRegistry`, `TemplateModelSubtypeRegistrar`, `TemplateObjectMapperFactory`, `ConsoleUdfController`, `UdfVersionView`, `UdfGroupView`, `DefaultRegistrySqlFunctionSource`, `RunReportCollector`, `RunReportVO`, `StageMetricVO`, `ConsoleApiAdvice`, `TemplateV2SqlFunctionRegistry`, `TemplateV2SqlFunction`, `TemplateV2RuntimeRegistry`, `TemplateV2RuntimeRegistryFactory`, `DefaultTemplateV2RuntimePlugin`, `JobExecutionDetail`, `FixtureTransformSqlExampleTests` (+ `CoreConfig` wiring grep, `test-matrix.yaml` rows).
**Date:** 2026-06-22
