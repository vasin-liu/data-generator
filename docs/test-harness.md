# Test harness

The unified harness entry point runs matrix-linked Maven tests on JDK 25 and writes a machine-readable coverage summary. Playwright console smoke is opt-in.

## Quick start

```powershell
# Embedded fast path (default): linked Maven tests only
.\scripts\verify-harness.ps1
```

Output: `target/test-matrix-summary.json`

## Flags

| Flag | Purpose |
|------|---------|
| `-IncludeE2e` | Run Playwright specs referenced in matrix `linked_tests` |
| `-UsePodman` | With `-IncludeE2e`, run via `scripts/e2e-podman.ps1` (default) |
| `-UseLocalService` | With `-IncludeE2e`, run against a local service base URL |
| `-MatrixFile` | Override matrix path (default `.planning/test-matrix.yaml`) |

Exit code **1** when a linked Maven test fails or when the **P0 regression gate** fails (see below). `pending` P1/P2 rows never fail the harness on their own.

## Priority tiers (COV-01)

Each matrix row carries a `tier` field (`P0`, `P1`, or `P2`):

| Tier | Meaning |
|------|---------|
| **P0** | Must be 100% green to merge — enforced by the P0 regression gate in CI |
| **P1** | Core-adjacent coverage tracked in the summary; non-blocking this phase |
| **P2** | Best-effort backlog rows tracked in the summary; non-blocking |

**COV-01 completion target:** P0 must be 100% green to merge; P1/P2 are tracked with no hard percentage this phase.

**P0 rows (15):** `calcite-scenario-v2`, `udf-sql`, `udf-script`, `udf-java-plugin`, `transform-json`, `transform-mask`, `transform-lookup`, `v2-streaming-csv`, `v2-streaming-json`, `v2-jdbc-upsert-pg-mysql`, `v2-dialect-dameng`, `v2-dialect-kingbase`, `v2-dialect-highgo`, `v2-dialect-postgres`, `v2-dialect-clickhouse`.

### Phase 10 RW/dialect P0 evidence

Phase 10 expanded the merge gate with Phase 8 streaming/upsert and Phase 9 dialect rows. Evidence bars (see also [testing-embedded-components.md](testing-embedded-components.md) for Testcontainers norms):

| Row id | Evidence bar |
|--------|--------------|
| `v2-dialect-dameng` | **MERGE SQL unit** (`JdbcSinkSqlBuilderTests`, e.g. `buildsDamengMergeInto`). Optional `ChunkedPipelineDamengUpsertIT` via `-Ddm.it=true` is **not** in the default gate. |
| `v2-dialect-kingbase`, `v2-dialect-highgo` | **PostgreSQL Testcontainers proxy IT** (`ChunkedPipelineKingbaseDialectTests`) plus dialect-key SQL builder units (`buildsKingbaseUsesOnConflictPath`, `buildsHighgoUsesOnConflictPath`). No licensed Kingbase/HighGo images required. |
| `v2-dialect-postgres` | **PostgreSQL Testcontainers upsert IT** (`ChunkedPipelinePostgresUpsertTests`). |
| `v2-dialect-clickhouse` | **ClickHouse Testcontainers insert-bulk IT** (`ClickHouseInsertBulkWriterIntegrationTests`) plus SQL builder unit rejecting upsert (`clickhouseUpsertIsUnsupported`). |
| `v2-streaming-csv`, `v2-streaming-json` | Streaming sink units and `StreamingPipelineTests` (low-heap `CsvJsonStreamingOomIT` excluded from linked tests). |
| `v2-jdbc-upsert-pg-mysql` | PG/MySQL Testcontainers upsert ITs plus `JdbcUpsertSmokeTests`. |

### Phase 17 / v2.1 P1 evidence

Phase 17 wired finished v2.1 proof paths as **P1 non-blocking** matrix rows. `scripts/verify-harness.ps1` remains the sole **P0 merge gate** (15 rows). P1 row failures or `skipped-conditional` statuses do not block merge unless a linked Maven test fails hard (exit code 1).

| Row id | Capability | Evidence bar | Supplementary |
|--------|------------|--------------|---------------|
| `exec-http-managed-catalog` | HTTP `/task/run` + managed JDBC catalog | `ManagedJdbcCatalogHttpExecuteIT` — publish gate, SUCCESS poll, H2 sink COUNT(*) | — |
| `exec-http-postgres-dialect` | HTTP spine + PG dialect upsert | `ManagedJdbcCatalogHttpPostgresUpsertIT` — Testcontainers PG ON CONFLICT; **skipped-conditional** without Docker (non-blocking) | — |
| `dist-multi-jvm-worker` | Multi-JVM coordinator → worker | `scripts/verify-multi-jvm-worker.ps1` — host two-JVM, dual SUCCESS; **script-primary** (`linked_tests: []`) | [staging-distributed-deployment.md](staging-distributed-deployment.md) |
| `rbac-enable-path` | Header RBAC when enabled | `ConsoleAuthorizationIntegrationIT`, `ConsoleSecurityDefaultOffIT`, `ConsoleAuthorizationFilterTest` | [verify-rbac-enable.ps1](../scripts/verify-rbac-enable.ps1), [staging-console-rbac.md](staging-console-rbac.md) |

These rows are **not merge blockers**. The CI **Harness verify** workflow (`.github/workflows/harness-verify.yml`) gates on `p0.pass` only.

### Phase 23 / v2.3 P1 evidence

Phase 23 links geo asset registry + console preview coverage as a **P1 non-blocking** matrix row. `scripts/verify-harness.ps1` P0 set stays **15 rows** — `geo-assets` is not promoted to the merge gate.

| Row id | Capability | Evidence bar | Supplementary |
|--------|------------|--------------|---------------|
| `geo-assets` | Metadata DB geo assets + console preview + asset-id execute path | `ConsoleGeoAssetControllerIT`, `ConsoleGeoAssetPreviewIT`, `GeoAssetServiceTests`, `GeoAssetReferenceScannerTests`, `TemplateV2RunnerGeoAssetSourceTests` — Maven IT/unit only | — |

Playwright/Podman map smoke from Phase 22 is optional operator evidence, not a matrix requirement for this row.

## Matrix maintenance

1. Edit `.planning/test-matrix.yaml` (source of truth).
2. Regenerate the human-readable doc:

```powershell
.\scripts\generate-test-matrix-doc.ps1
```

3. Optional draft seeder from codebase maps:

```powershell
.\scripts\generate-test-matrix-draft.ps1 -Force
```

## Fixture authoring

Reusable Template V2 scenarios live in `data-generator-test-fixtures`:

- `FixtureTemplates.load("reader-jdbc-basic")` — classpath YAML
- `H2Seed.apply(dataSource, sql)` — in-memory H2 seed scripts under `fixtures/sql/`

All JDBC URLs must use `jdbc:h2:mem:` (no production credentials).

## Reading `target/test-matrix-summary.json`

| Row status | Meaning |
|------------|---------|
| `covered` | All linked tests passed |
| `partial` | Some linked tests passed |
| `pending` | No linked tests or none executed / all failed without coverage claim |
| `skipped-conditional` | Linked tests skipped (conditional gates) |

Fields: `generatedAt`, optional `gitCommit`, `totals{covered,partial,pending,skipped}`, `rows[{id,status,linkedResults[]}]`, and `p0{total,green,pass,rows[{id,status,green}]}`.

The `p0` block is the machine-readable P0 rollup consumed by the merge gate:

| Field | Meaning |
|-------|---------|
| `p0.total` | Count of rows with `tier: P0` (15 after Phase 10 expansion) |
| `p0.green` | Count of P0 rows whose computed status is `covered` |
| `p0.pass` | `true` only when every P0 row is green (`status == covered`) |
| `p0.rows[]` | Per-P0-row `{id, status, green}` detail |
