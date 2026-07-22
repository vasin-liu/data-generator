# Phase 10: Harness Coverage & CI Gates - Pattern Map

**Mapped:** 2026-07-22
**Files analyzed:** 7
**Analogs found:** 6 / 7

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `.planning/test-matrix.yaml` | config | batch | Existing P0 rows (`transform-json`, `udf-sql`, `calcite-scenario-v2`) | exact |
| `scripts/verify-harness.ps1` | utility | batch | Self (current harness; no logic change expected per D-14) | exact |
| `scripts/lib/test-matrix-summary.ps1` | utility | batch | Self (P0 rollup already implemented) | exact |
| `.github/workflows/harness-verify.yml` | config | batch | Self (reference only — **do not modify** per D-14) | exact |
| `docs/test-harness.md` | documentation | request-response | Self (current P0 inventory section) | exact |
| `AGENTS.md` | documentation | request-response | Self (`### Merge criteria (P0 regression gate)`) | exact |
| `scripts/generate-test-matrix-doc.ps1` | utility | batch | Self (optional; not a success criterion per D-16) | partial |

## Pattern Assignments

### `.planning/test-matrix.yaml` (config, batch)

**Analog:** Existing P0 rows in `.planning/test-matrix.yaml` — `transform-json`, `udf-script`, `calcite-scenario-v2`

**Schema / column contract** (lines 1–19):

```yaml
schema:
  description: >
    Capability-first feature matrix for the data-generator platform. This file is the
    single source of truth (D-02) consumed by scripts/verify-harness.ps1 and
    scripts/generate-test-matrix-doc.ps1.
  columns:
    id: Stable row identifier referenced by harness and downstream plans.
    capability: Human-readable capability name (capability-first, D-01).
    adapter: Adapter or integration kind (jdbc, kafka, console-api, etc.).
    test_types: Test categories for this row (unit, integration, e2e).
    owner_module: Owning Maven module (D-04).
    status: covered | partial | pending | skipped-conditional (D-05).
    linked_tests: Maven simple class name or Playwright e2e/specs/<file>.spec.ts#<title>.
    notes: Free-form operator notes.
```

**Single-test P0 row pattern** (lines 224–232 — copy shape for dialect/streaming rows):

```yaml
  - id: transform-json
    capability: transform-json
    adapter: calcite-json
    test_types: [integration]
    owner_module: data-generator-test-fixtures
    status: covered
    tier: P0
    linked_tests: [FixtureTransformJsonExampleTests]
    notes: Built-in json operator (parse + flatten), D-02.
```

**Multi-test P0 row pattern** (lines 486–497 — copy for upsert / dialect rows with several classes):

```yaml
  - id: udf-script
    capability: udf-script
    adapter: javascript
    test_types: [unit]
    owner_module: data-generator-calcite
    status: covered
    tier: P0
    linked_tests:
      - GraalJsScriptUdfExecutorTests
      - UdfPublishServiceTests
      - UdfConsoleTemplateBindingE2ETests
    notes: GraalJS callable script UDF executor with sandbox, timeout, and schema-gate (UDF-03).
```

**Partial P1 row with calcite owner** (lines 284–292 — adapter/module precedent for RW paths):

```yaml
  - id: calcite-pipeline-chunked
    capability: calcite-pipeline-chunked
    adapter: chunked
    test_types: [integration]
    owner_module: data-generator-calcite
    status: partial
    tier: P1
    linked_tests: [ChunkedPipelineTests]
    notes: Chunked V2 pipeline against embedded H2.
```

**Recommended 8 new P0 rows** (planner discretion on exact `id` strings; counts locked by D-09–D-11):

| Row id (suggested) | capability | adapter | owner_module | linked_tests (Phase 8/9 only) | Evidence bar |
|--------------------|------------|---------|--------------|-------------------------------|--------------|
| `v2-streaming-csv` | v2-streaming-csv | csv | data-generator-calcite | `CsvJsonStreamingSinkTests`, `StreamingPipelineTests` | Per-chunk CSV sink + streaming pipeline |
| `v2-streaming-json` | v2-streaming-json | json | data-generator-calcite | `CsvJsonStreamingSinkTests`, `StreamingPipelineTests` | NDJSON/array streaming sink + pipeline |
| `v2-jdbc-upsert-pg-mysql` | v2-jdbc-upsert | jdbc | data-generator-calcite | `ChunkedPipelinePostgresUpsertTests`, `ChunkedPipelineMySqlUpsertTests`, `JdbcUpsertSmokeTests` | PG/MySQL Testcontainers idempotency (D-02) |
| `v2-dialect-dameng` | v2-dialect-dameng | jdbc | data-generator-calcite | `JdbcSinkSqlBuilderTests` | MERGE SQL unit only (D-05); **do not** link `ChunkedPipelineDamengUpsertIT` (D-08) |
| `v2-dialect-kingbase` | v2-dialect-kingbase | jdbc | data-generator-calcite | `ChunkedPipelineKingbaseDialectTests`, `JdbcSinkSqlBuilderTests` | PG-proxy IT + `buildsKingbaseUsesOnConflictPath` (D-06) |
| `v2-dialect-highgo` | v2-dialect-highgo | jdbc | data-generator-calcite | `ChunkedPipelineKingbaseDialectTests`, `JdbcSinkSqlBuilderTests` | PG-proxy IT + `buildsHighgoUsesOnConflictPath` (D-06) |
| `v2-dialect-postgres` | v2-dialect-postgres | jdbc | data-generator-calcite | `ChunkedPipelinePostgresUpsertTests` | Testcontainers upsert IT (D-07) |
| `v2-dialect-clickhouse` | v2-dialect-clickhouse | jdbc | data-generator-calcite | `ClickHouseInsertBulkWriterIntegrationTests`, `JdbcSinkSqlBuilderTests` | Insert-bulk IT + upsert reject unit (D-07) |

**Notes field patterns:**
- Mention Phase 8/9 requirement ids (TEST-07, RW-01, RW-03, RW-05) and evidence bar (e.g. “PG-proxy fulfills Kingbase/HighGo without licensed images”).
- Optional mention of gated IT: `ChunkedPipelineDamengUpsertIT` / `-Ddm.it=true` in `notes` only — never in `linked_tests`.

**Set `status: covered` in YAML** only when linked tests are expected green; harness recomputes status from Surefire — initial YAML `status` should align with expected outcome so local doc generation stays consistent.

---

### `scripts/verify-harness.ps1` (utility, batch)

**Analog:** Current script (no Phase 10 code change expected — D-14)

**Matrix ingestion + Maven slice** (lines 29–44):

```powershell
$rows = Get-MatrixRows $matrixPath
$mavenByModule = @{}
$allMavenClasses = New-Object System.Collections.Generic.List[string]

foreach ($row in $rows) {
    $links = @($row.linked_tests)
    $mavenLinks = $links | Where-Object { $_ -and $_ -notmatch 'e2e/specs/' }
    if ($mavenLinks.Count -eq 0) { continue }
    $module = $row.owner_module
    if (-not $module) { $module = 'data-generator-service' }
    if (-not $mavenByModule.ContainsKey($module)) { $mavenByModule[$module] = New-Object System.Collections.Generic.List[string] }
    foreach ($cls in $mavenLinks) {
        if ($mavenByModule[$module] -notcontains $cls) { [void]$mavenByModule[$module].Add($cls) }
        if ($allMavenClasses -notcontains $cls) { [void]$allMavenClasses.Add($cls) }
    }
}
```

**P0 gate readout** (lines 75–90):

```powershell
$summary = New-TestMatrixSummary -MatrixFile $matrixPath -RepoRoot $RepoRoot
Write-Host "Summary written to target/test-matrix-summary.json (rows=$($summary.rows.Count))" -ForegroundColor Green

$p0Pass = $true
if ($summary.p0) {
    $p0Pass = [bool]$summary.p0.pass
    if ($p0Pass) {
        Write-Host "P0 regression gate passed ($($summary.p0.green)/$($summary.p0.total) green)" -ForegroundColor Green
    } else {
        $failedIds = @($summary.p0.rows | Where-Object { -not $_.green } | ForEach-Object { $_.id })
        Write-Host "P0 regression gate FAILED: $($failedIds -join ', ') not green" -ForegroundColor Red
    }
}
```

**Exit gate** (lines 116–118):

```powershell
if ($mavenExit -ne 0 -or $linkedFailed -or -not $p0Pass) {
    Write-Host "Harness failed: mavenExit=$mavenExit linkedFailed=$linkedFailed p0Pass=$p0Pass" -ForegroundColor Red
    exit 1
}
```

**Planner action:** Adding `tier: P0` rows with green linked tests automatically expands CI gate — verify with `.\scripts\verify-harness.ps1` after matrix edit; do not rewrite script unless deduplication or module routing breaks (unlikely: all new rows use `data-generator-calcite`).

**Phase 8 OOM caveat:** `CsvJsonStreamingOomIT` runs in a **separate** `-Xmx256m` Maven invocation in `verify-phase8-uat-rw-streaming-upsert.ps1` (lines 67–72) — do **not** add to harness `linked_tests` unless harness gains a dedicated low-heap profile (out of scope D-12).

---

### `scripts/lib/test-matrix-summary.ps1` (utility, batch)

**Analog:** Self — P0 rollup already complete; no edit expected

**Per-row status computation** (lines 64–76):

```powershell
            if ($failed -gt 0 -and $passed -eq 0) {
                $status = 'pending'
            } elseif ($skipped -gt 0 -and $passed -eq 0 -and $failed -eq 0) {
                $status = 'skipped-conditional'
            } elseif ($passed -eq $mavenLinks.Count) {
                $status = 'covered'
            } elseif ($passed -gt 0) {
                $status = 'partial'
            } elseif ($missing -eq $mavenLinks.Count) {
                $status = 'pending'
            } else {
                $status = 'pending'
            }
```

**P0 block emission** (lines 82–104):

```powershell
        if ($rowTier -eq 'P0') {
            $green = ($status -eq 'covered')
            $p0Rows.Add(@{ id = $row.id; status = $status; green = $green })
        }
    }

    $p0Pass = ($p0Rows.Count -gt 0) -and (@($p0Rows | Where-Object { -not $_.green }).Count -eq 0)
    $p0GreenCount = @($p0Rows | Where-Object { $_.green }).Count
    # ...
        p0          = @{
            total = $p0Rows.Count
            green = $p0GreenCount
            pass  = $p0Pass
            rows  = $p0Rows
        }
```

**Docker-skipped tests:** `@EnabledIf("...DockerTestSupport#dockerAvailable")` classes skipped entirely yield `skipped-conditional` when all links skip — CI (ubuntu-latest + Docker) should run Testcontainers ITs green. Do not link DM IT that is env-gated.

---

### `.github/workflows/harness-verify.yml` (config, batch — reference only)

**Analog:** Self — **no modification** (D-14)

**CI entry** (lines 36–37):

```yaml
      - name: Harness verification + P0 regression gate
        run: pwsh -NoProfile -File ./scripts/verify-harness.ps1
```

**Artifact upload** (lines 39–45):

```yaml
      - name: Upload test matrix summary
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-matrix-summary
          path: target/test-matrix-summary.json
          if-no-files-found: error
```

Gate expands when matrix P0 rows increase — workflow unchanged.

---

### `docs/test-harness.md` (documentation, request-response)

**Analog:** Self — update P0 inventory list and counts

**Quick start pattern** (lines 5–12 — keep unchanged):

```markdown
## Quick start

```powershell
# Embedded fast path (default): linked Maven tests only
.\scripts\verify-harness.ps1
```

Output: `target/test-matrix-summary.json`
```

**Tier table pattern** (lines 25–35 — keep unchanged):

```markdown
## Priority tiers (COV-01)

Each matrix row carries a `tier` field (`P0`, `P1`, or `P2`):

| Tier | Meaning |
|------|---------|
| **P0** | Must be 100% green to merge — enforced by the P0 regression gate in CI |
| **P1** | Core-adjacent coverage tracked in the summary; non-blocking this phase |
| **P2** | Best-effort backlog rows tracked in the summary; non-blocking |
```

**P0 inventory line to replace** (line 37):

```markdown
**P0 rows (7):** `calcite-scenario-v2`, `udf-sql`, `udf-script`, `udf-java-plugin`, `transform-json`, `transform-mask`, `transform-lookup`.
```

**Replace with expanded inventory (15 rows):** existing 7 + 8 new (`v2-streaming-csv`, `v2-streaming-json`, `v2-jdbc-upsert-pg-mysql`, five dialect rows). Document evidence bars briefly (DM MERGE unit, KB/HG PG-proxy, CK upsert reject). Reference `docs/testing-embedded-components.md` for Testcontainers norms.

**p0 block docs** (lines 74–81 — keep structure, note `p0.total` becomes 15):

```markdown
| `p0.total` | Count of rows with `tier: P0` |
| `p0.green` | Count of P0 rows whose computed status is `covered` |
| `p0.pass` | `true` only when every P0 row is green (`status == covered`) |
| `p0.rows[]` | Per-P0-row `{id, status, green}` detail |
```

---

### `AGENTS.md` (documentation, request-response)

**Analog:** Self — `### Merge criteria (P0 regression gate)` (lines 159–161)

**Verify command registry entry** (lines 76–77 — already present, keep):

```markdown
# Test harness: matrix-linked Maven slice + coverage summary (see docs/test-harness.md)
.\scripts\verify-harness.ps1
```

**Merge criteria paragraph to extend** (lines 159–161):

```markdown
### Merge criteria (P0 regression gate)

Pull requests are **blocked when any P0 matrix row is not green**. The gate is enforced by `scripts/verify-harness.ps1` (reads `p0.pass` from `target/test-matrix-summary.json`) via the **Harness verify** workflow (`.github/workflows/harness-verify.yml`) on `pull_request`. P1/P2 row failures are tracked in the summary but do **not** block merge this phase. The P0 set is defined in `.planning/test-matrix.yaml` (`tier: P0`); see `docs/test-harness.md` for tier semantics and the COV-01 completion target.
```

**Add:** Enumerate or point to full 15-row P0 set (streaming CSV/JSON, JDBC upsert, five dialects). Do not duplicate Phase 8/9 UAT scripts as merge gates (D-16).

---

### `scripts/generate-test-matrix-doc.ps1` (utility, batch — optional)

**Analog:** Self (partial — only if local human matrix doc drift matters)

**Table header pattern** (line 86):

```powershell
| id | capability | adapter | test_types | owner_module | status | tier | linked_tests |
```

Run after matrix edit if operators use generated doc; not a Phase 10 success criterion (D-16).

---

## Linked Test Class Patterns (reference for matrix rows — do not modify)

These Phase 8/9 classes are the **only** new-test sources per D-12. Excerpts show what “green” means.

### Streaming — `CsvJsonStreamingSinkTests` (unit, file-I/O)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/CsvJsonStreamingSinkTests.java`

**Core per-chunk flush** (lines 38–52):

```java
    @Test
    void csvStreamingFiveChunksProduces5000DataRows(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("out.csv");
        CsvRowSinkAdapter sink = streamingCsvSink(csv);
        RowSchema schema = idNameSchema();

        for (int chunk = 0; chunk < CHUNK_COUNT; chunk++) {
            sink.write(schema, chunkRows(chunk * CHUNK_SIZE, CHUNK_SIZE));
        }
        sink.finish();

        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        Assertions.assertEquals(TOTAL_ROWS + 1, lines.size());
        Assertions.assertEquals("id,name", lines.getFirst());
    }
```

### Streaming — `StreamingPipelineTests` (integration, streaming)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/StreamingPipelineTests.java`

**Module:** `data-generator-calcite`; tests `StreamingPipeline` + `TemplateV2Runner` streaming mode for CSV/JSON sources and sinks.

### Upsert — `ChunkedPipelinePostgresUpsertTests` (integration, batch)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresUpsertTests.java`

**Testcontainers + parity helper** (lines 21–40):

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

**Sibling:** `ChunkedPipelineMySqlUpsertTests` — same pattern with MySQL container.

### Dialect SQL — `JdbcSinkSqlBuilderTests` (unit, transform)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcSinkSqlBuilderTests.java`

**Dameng MERGE (D-05 evidence)** (lines 144–156):

```java
    @Test
    void buildsDamengMergeInto() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of(
                "dialect", "dameng",
                "upsert", true,
                "upsertKeys", List.of("id")));
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount"));
        Assertions.assertTrue(sql.toLowerCase(Locale.ROOT).contains("merge into"));
        Assertions.assertTrue(sql.contains("t.id = s.id"));
        Assertions.assertTrue(sql.contains("when matched"));
        Assertions.assertTrue(sql.contains("when not matched"));
    }
```

**Kingbase / HighGo ON CONFLICT mapping** (lines 121–141):

```java
    @Test
    void buildsKingbaseUsesOnConflictPath() {
        JdbcWriterVO writer = writer("orders_out");
        writer.setOptions(Map.of(
                "dialect", "kingbase",
                "upsert", true,
                "upsertKeys", List.of("id")));
        String sql = JdbcSinkSqlBuilder.buildSql(writer, List.of("id", "amount"));
        Assertions.assertTrue(sql.contains("on conflict (id) do update set"));
        Assertions.assertTrue(sql.contains("amount = excluded.amount"));
    }
```

**ClickHouse upsert reject** (lines 107–118):

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

### Dialect IT — `ChunkedPipelineKingbaseDialectTests` (integration, batch)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineKingbaseDialectTests.java`

**PG-proxy for kingbase + highgo dialect keys** (lines 40–59):

```java
    @Test
    void chunkedUpsertKingbaseDialectIsIdempotent() {
        UpsertParitySupport.assertUpsertIdempotent(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword(),
                "org.postgresql.Driver",
                "kingbase");
    }

    @Test
    void chunkedUpsertHighgoDialectIsIdempotent() {
        UpsertParitySupport.assertUpsertIdempotent(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword(),
                "org.postgresql.Driver",
                "highgo");
    }
```

### ClickHouse — `ClickHouseInsertBulkWriterIntegrationTests` (integration, batch)

**Analog:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/ClickHouseInsertBulkWriterIntegrationTests.java`

**Testcontainers disabledWithoutDocker** (lines 33–38):

```java
@Testcontainers(disabledWithoutDocker = true)
class ClickHouseInsertBulkWriterIntegrationTests {

    @Container
    private static final ClickHouseContainer CLICKHOUSE = new ClickHouseContainer(
            DockerImageName.parse("clickhouse/clickhouse-server:24.8"));
```

### UAT script test-list pattern (for linked_tests validation, not harness code)

**Phase 8 analog:** `scripts/verify-phase8-uat-rw-streaming-upsert.ps1` (lines 45–52):

```powershell
$testList = @(
    'V2ScenarioTemplateIT',
    'StreamingPipelineTests',
    'ChunkedPipelineTests',
    'JdbcSinkSqlBuilderTests',
    'ChunkedPipelinePostgresUpsertTests',
    'ChunkedPipelineMySqlUpsertTests',
    'TemplateV2ValidatorTests'
) -join ','
```

**Phase 9 analog:** `scripts/verify-phase9-uat-jdbc-dialect.ps1` (lines 44–48):

```powershell
$testList = @(
    'JdbcSinkSqlBuilderTests',
    'ChunkedPipelinePostgresUpsertTests',
    'ClickHouseInsertBulkWriterIntegrationTests',
    'ChunkedPipelineKingbaseDialectTests',
    ...
)
```

Use these lists to cross-check `linked_tests` class names — harness uses **simple class names** only (no package prefix).

---

## Shared Patterns

### Matrix row registration
**Source:** `.planning/test-matrix.yaml` P0 rows (`transform-json`, `udf-script`)
**Apply to:** All 8 new P0 rows
- Always set `tier: P0`, `owner_module: data-generator-calcite` for RW/dialect paths
- `linked_tests` = Maven simple names; multiple classes require **all** to pass for `covered`
- `notes` documents evidence bar and explicit exclusions (e.g. gated DM IT)

### Harness auto-expansion (no CI YAML edit)
**Source:** `scripts/verify-harness.ps1` + `scripts/lib/test-matrix-summary.ps1`
**Apply to:** Matrix + verify workflow
- New P0 rows automatically join Maven `-Dtest=` union and `p0.total` count
- `p0.pass=false` when any P0 row status ≠ `covered`
- CI: `.github/workflows/harness-verify.yml` unchanged (D-14)

### Documentation sync
**Source:** `docs/test-harness.md` lines 37–37 + `AGENTS.md` lines 159–161
**Apply to:** Both doc files (D-15)
- Replace “P0 rows (7)” with full 15-row inventory
- Keep `.\scripts\verify-harness.ps1` as canonical verify command
- Document strict gate: pending P1/P2 never block merge alone

### Phase 8/9 evidence bars (TEST-07)
**Source:** `.planning/phases/10-harness-coverage-ci-gates/10-CONTEXT.md` D-05–D-08
**Apply to:** Dialect row `linked_tests` and `notes`
- Dameng: unit MERGE only — not `ChunkedPipelineDamengUpsertIT`
- Kingbase/HighGo: PG-proxy IT + SQL builder unit
- PostgreSQL/ClickHouse: Testcontainers IT (+ CK reject unit)
- No new fixtures; no Playwright in P0 `linked_tests`

### Embedded-first / Docker gates
**Source:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresUpsertTests.java`
**Apply to:** Rows linking Testcontainers ITs
- `@EnabledIf("...DockerTestSupport#dockerAvailable")` — CI must have Docker (GitHub ubuntu-latest does)
- Skipped-all-links → `skipped-conditional` → P0 not green

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| *(none)* | — | — | All Phase 10 deliverables extend existing harness/matrix/doc patterns |

---

## Metadata

**Analog search scope:** `.planning/test-matrix.yaml`, `scripts/verify-harness.ps1`, `scripts/lib/test-matrix-summary.ps1`, `.github/workflows/harness-verify.yml`, `docs/test-harness.md`, `AGENTS.md`, Phase 8/9 UAT scripts, `data-generator-calcite/src/test/java/**/{CsvJsonStreamingSinkTests,StreamingPipelineTests,ChunkedPipeline*UpsertTests,ChunkedPipelineKingbaseDialectTests,JdbcSinkSqlBuilderTests,ClickHouseInsertBulkWriterIntegrationTests}.java`
**Files scanned:** 18
**Pattern extraction date:** 2026-07-22
