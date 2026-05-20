# JDBC Chunked Execution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver bounded, chunked JDBC query → transform → sink execution for Template V2, wired to `ExecutionPolicyVO`, supporting single-table exports (Pattern S) and broadcast fact+dimension joins (Pattern B).

**Architecture:** Introduce `EffectiveExecutionPolicy` + `RunMetrics` in `data-generator-calcite`, split `TemplateV2Runner` into `InMemoryPipeline` and `ChunkedPipeline`, add `ChunkedQueryRowSource` with JDBC `fetchSize`, classify SQL shapes before run, and extend sinks with `writeBatch`. Service layer validates `CHUNKED` + unsafe shapes.

**Tech Stack:** Java 25, Maven, Spring `NamedParameterJdbcTemplate`, Calcite SQL validation (`CalciteSqlValidator`), JUnit 5, H2 integration tests.

**Spec:** `docs/superpowers/specs/2026-05-19-jdbc-chunked-execution-design.md`

---

## File map

| File | Responsibility |
|------|----------------|
| `data-generator-common/.../ExecutionPolicyVO.java` | Add optional `broadcastMaxRows` |
| `data-generator-calcite/.../runtime/EffectiveExecutionPolicy.java` | Resolve defaults + template overrides |
| `data-generator-calcite/.../runtime/RunMetrics.java` | Counters and warnings |
| `data-generator-calcite/.../runtime/ScaleLimitExceededException.java` | Fail-fast diagnostics |
| `data-generator-calcite/.../runtime/ExecutionShape.java` | Enum: ROW_LOCAL, BROADCAST_JOIN, MATERIALIZATION_REQUIRED |
| `data-generator-calcite/.../sql/ExecutionShapeClassifier.java` | Inspect SQL AST / validated plan |
| `data-generator-calcite/.../source/ChunkedRowSource.java` | Chunk read contract |
| `data-generator-calcite/.../source/ChunkedQueryRowSource.java` | JDBC cursor chunk reader |
| `data-generator-calcite/.../join/BroadcastJoinSnapshot.java` | Materialized dimension side |
| `data-generator-calcite/.../join/BroadcastJoinExecutor.java` | Probe fact chunk against snapshot |
| `data-generator-calcite/.../runtime/InMemoryPipeline.java` | Extracted current runner logic + limits |
| `data-generator-calcite/.../runtime/ChunkedPipeline.java` | Chunk loop |
| `data-generator-calcite/.../runtime/TemplateV2Runner.java` | Dispatch by policy + shape |
| `data-generator-calcite/.../runtime/TemplateV2RunResult.java` | Add metrics fields |
| `data-generator-calcite/.../RowSink.java` | Default `writeBatch` |
| `data-generator-calcite/.../sink/JdbcRowSinkAdapter.java` | Batch slices |
| `data-generator-calcite/.../sink/KafkaRowSinkAdapter.java` | Batch + flush |
| `data-generator-calcite/.../source/QuerySourceFactory.java` | Create chunked vs in-memory source |
| `data-generator-service/.../TemplateV2Validator.java` | CHUNKED + shape rules |
| Tests under `data-generator-calcite/src/test/...` | Unit + H2 integration |

All new `.java` files: copyright block + class Javadoc + public API Javadoc per `.cursor/rules/java-copyright-class-javadoc.mdc`.

**Build command (repeat per task):**

```powershell
cd D:\Work\99_Code\data-generator
.\mvnw-jdk25.ps1 -pl data-generator-calcite,data-generator-service -am test -Dtest=<TestClass>
```

---

## Phase 1 — Bounded in-memory safety

### Task 1: EffectiveExecutionPolicy

**Files:**
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/EffectiveExecutionPolicy.java`
- Create: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/EffectiveExecutionPolicyTests.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void defaultsToInMemoryWithRepositoryDefaults() {
    EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(null);
    Assertions.assertEquals("IN_MEMORY", policy.mode());
    Assertions.assertTrue(policy.maxRowsInMemory() > 0);
    Assertions.assertTrue(policy.sourceChunkSize() > 0);
    Assertions.assertTrue(policy.sinkBatchSize() > 0);
}

@Test
void overlaysTemplatePolicy() {
    ExecutionPolicyVO vo = new ExecutionPolicyVO();
    vo.setMode("CHUNKED");
    vo.setMaxRowsInMemory(1000);
    vo.setSourceChunkSize(200);
    vo.setSinkBatchSize(50);
    EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(vo);
    Assertions.assertEquals("CHUNKED", policy.mode());
    Assertions.assertEquals(1000, policy.maxRowsInMemory());
    Assertions.assertEquals(200, policy.sourceChunkSize());
    Assertions.assertEquals(50, policy.sinkBatchSize());
}
```

- [ ] **Step 2: Run test — expect FAIL**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=EffectiveExecutionPolicyTests
```

Expected: compilation failure — class not found.

- [ ] **Step 3: Implement**

```java
public final class EffectiveExecutionPolicy {
    private static final int DEFAULT_MAX_ROWS = 500_000;
    private static final int DEFAULT_CHUNK = 5_000;
    private static final int DEFAULT_SINK_BATCH = 1_000;

    private final String mode;
    private final int maxRowsInMemory;
    private final int sourceChunkSize;
    private final int sinkBatchSize;
    private final int previewRowLimit;
    private final boolean failOnLimitExceeded;
    private final int broadcastMaxRows;

    public static EffectiveExecutionPolicy resolve(ExecutionPolicyVO templatePolicy) {
        String mode = "IN_MEMORY";
        int maxRows = DEFAULT_MAX_ROWS;
        int chunk = DEFAULT_CHUNK;
        int sinkBatch = DEFAULT_SINK_BATCH;
        int preview = 100;
        boolean fail = true;
        int broadcastMax = Math.min(50_000, maxRows / 10);
        if (templatePolicy != null) {
            if (templatePolicy.getMode() != null && !templatePolicy.getMode().isBlank()) {
                mode = templatePolicy.getMode().trim().toUpperCase(Locale.ROOT);
            }
            if (templatePolicy.getMaxRowsInMemory() != null) {
                maxRows = templatePolicy.getMaxRowsInMemory();
            }
            if (templatePolicy.getSourceChunkSize() != null) {
                chunk = templatePolicy.getSourceChunkSize();
            }
            if (templatePolicy.getSinkBatchSize() != null) {
                sinkBatch = templatePolicy.getSinkBatchSize();
            }
            if (templatePolicy.getPreviewRowLimit() != null) {
                preview = templatePolicy.getPreviewRowLimit();
            }
            if (templatePolicy.getFailOnLimitExceeded() != null) {
                fail = templatePolicy.getFailOnLimitExceeded();
            }
        }
        return new EffectiveExecutionPolicy(mode, maxRows, chunk, sinkBatch, preview, fail, broadcastMax);
    }
    // constructor + getters only
}
```

- [ ] **Step 4: Run test — expect PASS**

- [ ] **Step 5: Commit**

```bash
git add data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/EffectiveExecutionPolicy.java \
        data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/EffectiveExecutionPolicyTests.java
git commit -m "feat(calcite): add effective execution policy resolver"
```

---

### Task 2: ScaleLimitExceededException + RunMetrics

**Files:**
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/ScaleLimitExceededException.java`
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/RunMetrics.java`
- Create: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ScaleLimitExceededExceptionTests.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void messageIncludesPolicyFieldAndStage() {
    ScaleLimitExceededException ex = new ScaleLimitExceededException(
            "maxRowsInMemory", 100, 101, "SOURCE_READ", "orders");
    Assertions.assertTrue(ex.getMessage().contains("maxRowsInMemory"));
    Assertions.assertTrue(ex.getMessage().contains("SOURCE_READ"));
    Assertions.assertTrue(ex.getMessage().contains("orders"));
}
```

- [ ] **Step 2: Run — expect FAIL**

- [ ] **Step 3: Implement exception**

```java
public class ScaleLimitExceededException extends IllegalStateException {
    public ScaleLimitExceededException(String policyField, long limit, long actual,
                                       String stage, String sourceName) {
        super("Execution policy limit exceeded: field=" + policyField
                + ", limit=" + limit + ", actual=" + actual
                + ", stage=" + stage + ", source=" + sourceName);
    }
}
```

`RunMetrics`: mutable counters `rowsRead`, `chunksProcessed`, `Map<String, Long> rowsReadPerSource`, `executionMode`, `List<String> warnings`; methods `addRead(String source, int count)`, `incrementChunks()`.

- [ ] **Step 4: Run — expect PASS**

- [ ] **Step 5: Commit** — `feat(calcite): add scale limit exception and run metrics`

---

### Task 3: Enforce maxRowsInMemory in InMemoryPipeline

**Files:**
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/InMemoryPipeline.java`
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2Runner.java`
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2RunResult.java`
- Modify: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerTests.java`

- [ ] **Step 1: Write failing test** (use existing iterator + SQL template pattern from `TemplateV2RunnerTests`; set `executionPolicy.maxRowsInMemory=2` on a template that reads 5 constant rows)

```java
@Test
void failsWhenInMemoryRowLimitExceeded() {
    TemplateV2VO template = /* constant iterator source producing 5 rows, single sql transform, console sink */;
    ExecutionPolicyVO policy = new ExecutionPolicyVO();
    policy.setMode("IN_MEMORY");
    policy.setMaxRowsInMemory(2);
    policy.setFailOnLimitExceeded(true);
    template.setExecutionPolicy(policy);
    ScaleLimitExceededException ex = Assertions.assertThrows(
            ScaleLimitExceededException.class,
            () -> runner.run(template));
    Assertions.assertTrue(ex.getMessage().contains("maxRowsInMemory"));
}
```

- [ ] **Step 2: Run — expect FAIL**

- [ ] **Step 3: Refactor**

1. Move body of `TemplateV2Runner.run` into `InMemoryPipeline.run(template, policy, registry)`.
2. After each source is added to context, count `source.rows().size()` and accumulate; if total > `policy.maxRowsInMemory()`, throw `ScaleLimitExceededException`.
3. `TemplateV2Runner.run`: `policy = EffectiveExecutionPolicy.resolve(template.getExecutionPolicy())`; if `"IN_MEMORY".equals(policy.mode())` delegate to `InMemoryPipeline`; else throw `UnsupportedOperationException` until Task 6.
4. Extend `TemplateV2RunResult` with `RunMetrics metrics` field; populate `executionMode` in pipeline.

- [ ] **Step 4: Run `TemplateV2RunnerTests` — expect PASS**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=TemplateV2RunnerTests
```

- [ ] **Step 5: Commit** — `feat(calcite): enforce in-memory row limits via execution policy`

---

## Phase 2a — Row-local chunked pipeline

### Task 4: ChunkedRowSource + ChunkedQueryRowSource

**Files:**
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/ChunkedRowSource.java`
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/ChunkedQueryRowSource.java`
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/QuerySourceFactory.java`
- Create: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/ChunkedQueryRowSourceTests.java`

- [ ] **Step 1: Write failing H2 integration test**

Use embedded H2 datasource in test (follow pattern from existing JDBC tests if present, else `JdbcEmbeddedTestSupport` inline):

```java
@Test
void readsMoreRowsThanOneChunkWithoutLoadingAllAtOnce() {
    // insert 12_000 rows into H2 table t(id, name)
    QuerySourceVO source = new QuerySourceVO();
    source.setDataSourceId("test");
    source.setSql("select id, name from t");
    ChunkedQueryRowSource rowSource = new ChunkedQueryRowSource("t", source, jdbcTemplate, 5000);
  int total = 0;
  while (rowSource.hasNextChunk()) {
    List<Row> chunk = rowSource.nextChunk(5000);
    total += chunk.size();
    Assertions.assertTrue(chunk.size() <= 5000);
  }
  Assertions.assertEquals(12_000, total);
  Assertions.assertEquals(12_000, rowSource.rowsReadSoFar());
}
```

- [ ] **Step 2: Run — expect FAIL**

- [ ] **Step 3: Implement ChunkedQueryRowSource**

Key implementation notes:

- Use `jdbcTemplate.getJdbcOperations().query(con -> { PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); ps.setFetchSize(chunkSize); return ps; }, params, rowCallback)`.
- Do **not** store all rows in a field; only current chunk buffer.
- `rows()` for interface compatibility: return empty list or throw `UnsupportedOperationException` with message "use chunked API" (document in Javadoc).
- Reuse `normalizeKeys`, `inferSchema` helpers from `QueryRowSource` (extract package-private utility `QueryRowSourceSupport` if needed to avoid duplication).

`QuerySourceFactory.create(...)`: if policy mode is `CHUNKED` and source is `QuerySourceVO`, return `ChunkedQueryRowSource`; else existing `QueryRowSource`.

- [ ] **Step 4: Run ChunkedQueryRowSourceTests — PASS**

- [ ] **Step 5: Commit** — `feat(calcite): add chunked jdbc query row source`

---

### Task 5: ExecutionShapeClassifier (ROW_LOCAL)

**Files:**
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/ExecutionShape.java`
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/ExecutionShapeClassifier.java`
- Create: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sql/ExecutionShapeClassifierTests.java`

- [ ] **Step 1: Write failing tests**

```java
@Test
void singleTableSelectIsRowLocal() {
    ExecutionShape shape = ExecutionShapeClassifier.classify(
            "SELECT id, name FROM orders WHERE status = 'OPEN'");
    Assertions.assertEquals(ExecutionShape.ROW_LOCAL, shape);
}

@Test
void groupByIsMaterializationRequired() {
    ExecutionShape shape = ExecutionShapeClassifier.classify(
            "SELECT status, COUNT(*) FROM orders GROUP BY status");
    Assertions.assertEquals(ExecutionShape.MATERIALIZATION_REQUIRED, shape);
}

@Test
void innerJoinIsMaterializationRequiredInPhase2aClassifier() {
    // Until broadcast metadata wired in Task 8, two-table join without broadcast hint => MATERIALIZATION_REQUIRED
    ExecutionShape shape = ExecutionShapeClassifier.classify(
            "SELECT o.id, c.name FROM orders o INNER JOIN customers c ON o.customer_id = c.id");
    Assertions.assertEquals(ExecutionShape.MATERIALIZATION_REQUIRED, shape);
}
```

- [ ] **Step 2: Run — FAIL**

- [ ] **Step 3: Implement**

Parse SQL via existing `CalciteSqlValidator` / `SqlParser` path; walk `SqlNode`:

- `GROUP BY`, `DISTINCT`, `ORDER BY` (without fetch) → `MATERIALIZATION_REQUIRED`
- Multi-join or join with two `QuerySourceVO` names without broadcast metadata → `MATERIALIZATION_REQUIRED` (phase 2a)
- Single `FROM` identifier → `ROW_LOCAL`

Expose `classify(String sql)` and `classify(TemplateV2VO template)` (uses first `SqlTransformVO`).

- [ ] **Step 4: PASS**

- [ ] **Step 5: Commit** — `feat(calcite): add execution shape classifier`

---

### Task 6: ChunkedPipeline (ROW_LOCAL) + sink writeBatch

**Files:**
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/ChunkedPipeline.java`
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/RowSink.java`
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcRowSinkAdapter.java`
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/KafkaRowSinkAdapter.java`
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2Runner.java`
- Create: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineTests.java`

- [ ] **Step 1: Write failing integration test**

H2: 10k rows in source table → SQL `SELECT id, name FROM t` → JDBC sink to `target` table.

```java
@Test
void chunkedModeWritesAllRowsInBatches() {
    template.getExecutionPolicy().setMode("CHUNKED");
    template.getExecutionPolicy().setSourceChunkSize(2000);
    template.getExecutionPolicy().setSinkBatchSize(500);
    TemplateV2RunResult result = runner.run(template);
    Assertions.assertEquals(10_000, result.getMetrics().getRowsRead());
    Assertions.assertEquals(10_000, countRowsInTargetTable());
    Assertions.assertEquals("CHUNKED", result.getMetrics().getExecutionMode());
}
```

- [ ] **Step 2: Run — FAIL**

- [ ] **Step 3: Implement ChunkedPipeline**

Pseudocode:

```java
public TemplateV2RunResult run(TemplateV2VO template, EffectiveExecutionPolicy policy,
                               TemplateV2RuntimeRegistry registry) {
    ExecutionShape shape = ExecutionShapeClassifier.classify(template);
    if (shape != ExecutionShape.ROW_LOCAL) {
        throw new IllegalStateException("CHUNKED mode requires ROW_LOCAL shape, got " + shape);
    }
    RunMetrics metrics = new RunMetrics("CHUNKED");
    // build ChunkedQueryRowSource for sole QuerySourceVO; error if multiple non-chunked sources
    CalciteExecutionContext context = new CalciteExecutionContext();
    while (chunkedSource.hasNextChunk()) {
        List<Row> chunk = chunkedSource.nextChunk(policy.sourceChunkSize());
        metrics.addRead(chunkedSource.name(), chunk.size());
        checkLimit(metrics.getRowsRead(), policy.maxRowsInMemory(), "SOURCE_READ", chunkedSource.name());
        TransformResult out = registry.applyTransform(template.getTransformers().get(0), context.withChunk(chunk));
        for (WriteStageVO sink : template.getSinks()) {
            for (WriterVO writer : sink.getWriters()) {
                registry.createSink(writer).writeBatch(out.schema(), out.rows(), policy.sinkBatchSize());
            }
        }
        metrics.incrementChunks();
    }
    return new TemplateV2RunResult(null, List.of(), metrics);
}
```

`RowSink` default:

```java
default void writeBatch(RowSchema schema, List<Row> rows, int batchSize) {
    if (rows == null || rows.isEmpty()) return;
    for (int i = 0; i < rows.size(); i += batchSize) {
        write(schema, rows.subList(i, Math.min(i + batchSize, rows.size())));
    }
}
```

Override `JdbcRowSinkAdapter.write` to delegate to `writeBatch` with `Integer.MAX_VALUE` or implement slice loop once in `writeBatch` only.

`TemplateV2Runner`: if `CHUNKED` → `ChunkedPipeline.run`.

Add `CalciteExecutionContext.withChunk(...)` helper or rebuild context per chunk with table `input`.

- [ ] **Step 4: PASS ChunkedPipelineTests + TemplateV2RunnerTests**

- [ ] **Step 5: Commit** — `feat(calcite): add row-local chunked execution pipeline`

---

### Task 7: Service validation for CHUNKED + shape

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java`
- Modify: `data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2SupportTests.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void rejectsChunkedModeWithGroupBySql() {
    TemplateV2VO template = /* valid except sql with GROUP BY */;
    template.getExecutionPolicy().setMode("CHUNKED");
    IllegalArgumentException ex = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> TemplateV2Validator.validate(template));
    Assertions.assertTrue(ex.getMessage().contains("CHUNKED"));
    Assertions.assertTrue(ex.getMessage().contains("GROUP BY") || ex.getMessage().contains("MATERIALIZATION"));
}
```

- [ ] **Step 2: Run — FAIL**

- [ ] **Step 3: Implement**

In `validateExecutionPolicy` or new `validateChunkedCompatibility(TemplateV2VO)`:

- If mode is `CHUNKED`, call `ExecutionShapeClassifier.classify(template)`.
- If `MATERIALIZATION_REQUIRED`, throw `IllegalArgumentException` with SQL feature hint.
- If `STREAMING`, throw unsupported.

- [ ] **Step 4: PASS TemplateV2SupportTests**

- [ ] **Step 5: Commit** — `feat(service): validate chunked execution shape at template save`

---

## Phase 2b — Broadcast join (A3 Pattern B)

### Task 8: broadcastMaxRows on ExecutionPolicyVO

**Files:**
- Modify: `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/ExecutionPolicyVO.java`
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/EffectiveExecutionPolicy.java`
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java`

- [ ] **Step 1: Test** — resolve uses template `broadcastMaxRows` when set
- [ ] **Step 2: FAIL**
- [ ] **Step 3: Add field + validation (positive integer)**
- [ ] **Step 4: PASS**
- [ ] **Step 5: Commit** — `feat(core): add broadcastMaxRows to execution policy`

---

### Task 9: ExecutionShapeClassifier BROADCAST_JOIN

**Files:**
- Modify: `ExecutionShapeClassifier.java`
- Modify: `ExecutionShapeClassifierTests.java`
- Modify: `TemplateV2Validator.java`

- [ ] **Step 1: Tests**

```java
@Test
void classifiesFactPlusSmallDimensionAsBroadcastJoin() {
    TemplateV2VO template = /* sources: fact (Query, no maxRows), dim (Query, maxRows=100) */;
    template.getTransformers().get(0).setSql(
            "SELECT f.id, d.name FROM fact f LEFT JOIN dim d ON f.dim_id = d.id");
    Assertions.assertEquals(ExecutionShape.BROADCAST_JOIN,
            ExecutionShapeClassifier.classify(template));
}
```

Classifier rules:

- Exactly one `INNER` or `LEFT` join.
- Two source names in FROM/JOIN.
- Mark broadcast side: `QuerySourceVO` with `maxRows` set and ≤ `broadcastMaxRows`, or iterator/constant source.
- Other side must be `QuerySourceVO` without low maxRows (fact).

- [ ] **Step 2–5: Implement, PASS, commit** — `feat(calcite): classify broadcast join execution shape`

---

### Task 10: BroadcastJoinSnapshot + ChunkedPipeline join path

**Files:**
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/join/BroadcastJoinSnapshot.java`
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/join/BroadcastJoinExecutor.java`
- Modify: `ChunkedPipeline.java`
- Create: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/join/BroadcastJoinExecutorTests.java`

- [ ] **Step 1: Test** — H2: dim 100 rows, fact 8_000 rows, LEFT JOIN, CHUNKED, assert 8_000 output rows and dim materialize row count ≤ broadcastMaxRows

- [ ] **Step 2: FAIL**

- [ ] **Step 3: Implement**

1. `BroadcastJoinSnapshot.materialize(RowSource dim, int broadcastMaxRows)` — if `dim.rows().size() > broadcastMaxRows` throw `ScaleLimitExceededException` stage `BROADCAST_MATERIALIZE`.
2. Build join key index from parsed `ON` clause equi-keys.
3. `BroadcastJoinExecutor.join(factChunk, snapshot, joinType)` → list of rows.
4. `ChunkedPipeline`: if `BROADCAST_JOIN`, materialize dim once, loop fact chunks, join, then SQL projection if needed (may apply `CalciteRowTransformer` on joined chunk via `input` table).

- [ ] **Step 4: PASS**

- [ ] **Step 5: Commit** — `feat(calcite): support broadcast join in chunked pipeline`

---

### Task 11: Reject two large query sources under CHUNKED

**Files:**
- Modify: `TemplateV2Validator.java`
- Modify: `ExecutionShapeClassifier.java`

- [ ] **Step 1: Test** — two `QuerySourceVO` without maxRows + JOIN + CHUNKED → validation error

- [ ] **Step 2–5: Implement check, PASS, commit**

---

## Phase 3 — Hardening and docs

### Task 12: Elasticsearch sink writeBatch

**Files:**
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/ElasticsearchRowSinkAdapter.java`
- Modify: existing ES sink tests

- [ ] **Step 1–5:** Slice bulk requests by `sinkBatchSize`; test with mock client if live ES unavailable.

---

### Task 13: STREAMING mode rejection

**Files:**
- Modify: `TemplateV2Runner.java`
- Modify: `TemplateV2RunnerTests.java`

- [ ] **Test:** `executionPolicy.mode=STREAMING` → `IllegalArgumentException` message mentions not implemented.

---

### Task 14: Documentation and status

**Files:**
- Modify: `docs/calcite-implementation-status.md`
- Modify: `docs/calcite-v1-parity-scorecard.md` (row: large JDBC export)
- Create: `docs/template-v2-jdbc-chunked-execution-guide.md` (operator: MySQL cursor URL flags, policy YAML, Pattern S vs B)

- [ ] Update implementation status with phases delivered
- [ ] Add MySQL `useCursorFetch` note
- [ ] Commit — `docs: document jdbc chunked execution`

---

### Task 15: Full module verification

- [ ] **Run full targeted tests**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-calcite,data-generator-service -am test
```

Expected: BUILD SUCCESS

- [ ] **Commit any fixups** if needed

---

## Plan self-review vs spec

| Spec requirement | Task |
|------------------|------|
| Wire executionPolicy to runner | 1, 3, 6 |
| maxRowsInMemory fail-fast | 2, 3, 6 |
| ChunkedQueryRowSource + fetchSize | 4 |
| ROW_LOCAL CHUNKED pipeline | 5, 6 |
| JDBC/Kafka sink batching | 6, 12 |
| BROADCAST_JOIN (A3) | 8, 9, 10, 11 |
| CHUNKED rejects GROUP BY / materialization | 5, 7 |
| Run metrics | 2, 3, 6 |
| STREAMING deferred | 13 |
| Docs / scorecard | 14 |

No TBD steps. Type names consistent: `EffectiveExecutionPolicy`, `ExecutionShape`, `ScaleLimitExceededException`.

---

## Suggested commit sequence (Conventional Commits)

1. `feat(calcite): add effective execution policy resolver`
2. `feat(calcite): add scale limit exception and run metrics`
3. `feat(calcite): enforce in-memory row limits via execution policy`
4. `feat(calcite): add chunked jdbc query row source`
5. `feat(calcite): add execution shape classifier`
6. `feat(calcite): add row-local chunked execution pipeline`
7. `feat(service): validate chunked execution shape at template save`
8. `feat(core): add broadcastMaxRows to execution policy`
9. `feat(calcite): classify broadcast join execution shape`
10. `feat(calcite): support broadcast join in chunked pipeline`
11. `feat(calcite): reject oversized dual-query chunked joins`
12. `feat(calcite): batch elasticsearch sink writes`
13. `feat(calcite): reject streaming execution mode until implemented`
14. `docs: document jdbc chunked execution`

Each commit footer per `.cursor/rules/git-commit-conventional-ai.mdc` when AI-assisted.
