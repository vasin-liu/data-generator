# Template V2 JDBC sink guide

## Overview

V2 JDBC sinks use `JdbcRowSinkAdapter` with a generic `INSERT` statement by default. Dialect-specific upsert and bulk behavior is opt-in via writer `options`.

Phase 8 adds **`options.upsertKeys`** (YAML array) for PostgreSQL `ON CONFLICT` and MySQL `ON DUPLICATE KEY UPDATE`. Legacy `conflictColumns` (comma-separated string) remains supported for PostgreSQL only.

Phase 9 extends dialect writers for **Dameng**, **Kingbase**, **HighGo**, **PostgreSQL**, and **ClickHouse** with explicit `options.dialect` keys, documented upsert/bulk limits, and console datasource presets (RW-05, RW-06).

## Writer options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `dialect` | string | `generic` | `generic`, `postgres`, `mysql`, `dameng`, `kingbase`, `highgo`, or `clickhouse` |
| `bulkMode` | string | `jdbc_batch` | `jdbc_batch`, `postgres_copy` (alias `copy`), or `clickhouse_insert` (alias `clickhouse`) |
| `upsert` | boolean | `false` | Enable dialect-specific duplicate handling |
| `upsertKeys` | string[] | — | **Required** when `upsert: true`. Conflict key columns |
| `conflictColumns` | string | — | Legacy PostgreSQL-only comma-separated keys; prefer `upsertKeys` |

### Explicit `dialect` (Phase 9)

For Dameng, Kingbase, HighGo, PostgreSQL, and ClickHouse sinks, set **`options.dialect`** to the matching key. The YAML value is the **source of truth for SQL generation** — the runtime does not auto-detect dialect from the JDBC URL or driver class (D-05, D-07).

Keep `options.dialect` aligned with your datasource connection (URL, driver preset). A mismatch does not hard-fail at publish or run; connectivity tests validate the connection only. Misaligned dialect + connection can produce invalid SQL at run time.

When `upsert: true` and `upsertKeys` is missing, empty, or references unknown columns, publish and run **fail fast** (same severity as governance blocks).

## Console datasource presets (RW-06)

Register JDBC datasources in the operator console using driver presets for the five Phase 9 engines. Presets supply URL templates, driver class names, and bundled-driver hints:

| Engine | Preset IDs (examples) | Sink `options.dialect` |
|--------|----------------------|------------------------|
| Dameng | `dm8` | `dameng` |
| Kingbase | `kingbase8`, `kingbase9` | `kingbase` |
| HighGo | `highgo` | `highgo` |
| PostgreSQL | `postgresql10` … `postgresql18` | `postgres` |
| ClickHouse | `clickhouse20` … `clickhouse26` | `clickhouse` |

Create or edit a datasource under **Console → Datasources**, select a preset, run the connectivity test, then reference the datasource from your template sink `dataSourceId`.

## Examples

### Generic insert (all databases)

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, amount:amount
```

**Do not** combine `dialect: generic` with `upsert: true` — publish and run **fail fast** with an explicit error (D-08). Use a dialect-specific key when duplicate handling is required.

### PostgreSQL upsert (`ON CONFLICT DO UPDATE`)

Sets non-key columns from the excluded insert row. When all columns are keys, emits `ON CONFLICT (...) DO NOTHING`.

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, tenant_id:tenant_id, amount:amount, status:status
      options:
        dialect: postgres
        upsert: true
        upsertKeys: [id, tenant_id]
```

Generated SQL shape:

```sql
INSERT INTO orders_out (id, tenant_id, amount, status) VALUES (:id, :tenant_id, :amount, :status)
ON CONFLICT (id, tenant_id) DO UPDATE SET amount = excluded.amount, status = excluded.status
```

Idempotent re-run: a second execution **updates** existing keys instead of inserting duplicates. Verified by `ChunkedPipelinePostgresUpsertTests` and scenario **GF-G** (`scenario-g-upsert-pg.yaml`).

### MySQL upsert (`ON DUPLICATE KEY UPDATE`)

Requires a primary or unique key on `upsertKeys` in the target table.

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, amount:amount, status:status
      options:
        dialect: mysql
        upsert: true
        upsertKeys: [id]
```

Generated SQL shape:

```sql
INSERT INTO orders_out (id, amount, status) VALUES (:id, :amount, :status)
ON DUPLICATE KEY UPDATE amount = VALUES(amount), status = VALUES(status)
```

Verified by `ChunkedPipelineMySqlUpsertTests` and **GF-G** (`scenario-g-upsert-mysql.yaml`).

### Dameng upsert (`MERGE INTO`)

Dameng uses **`MERGE INTO`** with the same YAML contract as Phase 8 (`upsert: true`, `upsertKeys`). Use **`dialect: dameng`** — do not rely on `generic`.

```yaml
sink:
  writers:
    - type: JDBC
      target: YOUR_SCHEMA.orders_out
      template: id:value, tenant_id:tenant_id, amount:amount, status:status
      options:
        dialect: dameng
        upsert: true
        upsertKeys: [id, tenant_id]
```

Generated SQL shape:

```sql
MERGE INTO YOUR_SCHEMA.orders_out t
USING (SELECT :id AS id, :tenant_id AS tenant_id, :amount AS amount, :status AS status FROM dual) s
ON (t.id = s.id AND t.tenant_id = s.tenant_id)
WHEN MATCHED THEN UPDATE SET t.amount = s.amount, t.status = s.status
WHEN NOT MATCHED THEN INSERT (id, tenant_id, amount, status) VALUES (s.id, s.tenant_id, s.amount, s.status)
```

Verified by `JdbcSinkSqlBuilderTests.buildsDamengMergeInto` (MERGE SQL generation). Optional real Dameng integration tests require `-Ddm.it=true` or `DG_DM_IT=true` when a DM host/image is available (skipped by default in CI).

#### Dameng live IT (opt-in, DIAL-01)

`ChunkedPipelineDamengUpsertIT` proves CHUNKED JDBC upsert idempotency (same PK re-run; row count and updated values correct) against a **real external Dameng host**. It is skipped by default and is **not** part of the P0 merge gate — the default CI and PR merge bar for Dameng MERGE SQL remains the unit test above, `JdbcSinkSqlBuilderTests.buildsDamengMergeInto`, which requires no live host.

**Enable the flag** (either form works; the wrapper script sets both):

- System property: `-Ddm.it=true`
- Environment: `DG_DM_IT=true`

**Connection environment variables** (all three required once the flag is on):

| Variable | Purpose |
|----------|---------|
| `DG_DM_JDBC_URL` | Dameng JDBC URL, e.g. `jdbc:dm://host:5236?schema=YOUR_SCHEMA` |
| `DG_DM_USER` | Database user |
| `DG_DM_PASSWORD` | Database password |

**Run it — wrapper script:**

```powershell
$env:DG_DM_IT = 'true'
$env:DG_DM_JDBC_URL = 'jdbc:dm://host:5236?schema=YOUR_SCHEMA'
$env:DG_DM_USER = 'YOUR_USER'
$env:DG_DM_PASSWORD = 'YOUR_PASSWORD'
.\scripts\verify-phase13-uat-dameng-live.ps1
```

**Run it — equivalent direct Maven command:**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-calcite -am `
  -Ddm.it=true `
  "-Dtest=ChunkedPipelineDamengUpsertIT" `
  "-Dsurefire.failIfNoSpecifiedTests=false" `
  test
```

**PASS/FAIL semantics (D-02, D-05, honest by design):**

- **Flag off** (`DG_DM_IT` unset / not `true`) — the IT is **skipped**; default CI is unaffected.
- **Flag on and fully configured against a reachable host** — the IT **PASSes**, proving chunked upsert idempotency via the shared `UpsertParitySupport.assertUpsertIdempotent` helper (the same one used by the PostgreSQL and MySQL upsert ITs).
- **Flag on but misconfigured (missing env var) or the host is unreachable** — the build **FAILS**. This test never soft-skips once the flag is on; a misconfigured opt-in run must never be mistaken for a passed UAT.

**Host prerequisites:** the target Dameng schema must grant the configured user DDL rights to `DROP`/`CREATE` two scratch tables (`upsert_source_t`, `upsert_target_t`) and tolerate on the order of hundreds of seeded/updated rows per run — the IT reuses the exact same `UpsertParitySupport` fixture as the PostgreSQL and MySQL upsert ITs. An incompatible schema or insufficient grants surfaces as a genuine test failure, not a skip.

**Never commit secrets:** `DG_DM_JDBC_URL`, `DG_DM_USER`, and `DG_DM_PASSWORD` are plaintext environment values intended only for local or opt-in maintainer runs. Never commit them to the repository or paste them into CI configuration files — the repository intentionally ships no environment file for these variables.

### Kingbase upsert (PostgreSQL `ON CONFLICT` path)

Kingbase reuses the PostgreSQL upsert SQL generator. Use the independent dialect key **`kingbase`** — operators do not need to set `postgres` for Kingbase targets (D-01, D-06).

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, tenant_id:tenant_id, amount:amount, status:status
      options:
        dialect: kingbase
        upsert: true
        upsertKeys: [id, tenant_id]
```

Generated SQL matches the PostgreSQL `ON CONFLICT` shape above. Embedded harness tests use a **PostgreSQL Testcontainers proxy** for the shared upsert path plus unit tests asserting `dialect=kingbase` mapping (D-15).

### HighGo upsert (PostgreSQL `ON CONFLICT` path)

HighGo uses the same PostgreSQL upsert path with the independent dialect key **`highgo`**.

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, tenant_id:tenant_id, amount:amount, status:status
      options:
        dialect: highgo
        upsert: true
        upsertKeys: [id, tenant_id]
```

Same SQL shape as Kingbase/PostgreSQL. Harness coverage uses PostgreSQL container proxy + dialect-mapping unit tests (D-15).

### Legacy PostgreSQL `conflictColumns` (prefer `upsertKeys`)

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, amount:amount
      options:
        dialect: postgres
        upsert: true
        conflictColumns: id
```

New templates should use `upsertKeys: [id]` instead.

### ClickHouse insert (upsert rejected)

ClickHouse has no JDBC upsert equivalent. With **`dialect: clickhouse`** and **`upsert: true`**, publish and run **fail fast** with an explicit error — the runtime does **not** silently insert-as-upsert (D-03).

Use **`upsert: false`** and handle duplicates via table engine or application logic:

- **`ReplacingMergeTree`** — background dedup by version/sort key
- **`CollapsingMergeTree`** — sign column for upsert-like semantics
- **Application-level keys** — read-modify-write outside the sink

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, amount:amount
      options:
        dialect: clickhouse
        upsert: false
```

Insert and **`bulkMode: clickhouse_insert`** remain supported. Verified by `ClickHouseInsertBulkWriterIntegrationTests` and `JdbcSinkSqlBuilderClickHouseRejectTests`.

### PostgreSQL COPY bulk load

Use `bulkMode: postgres_copy` for `COPY ... FROM STDIN` CSV streaming. Requires a PostgreSQL JDBC datasource; `upsert` is not supported on this path.

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, amount:amount
      options:
        dialect: postgres
        bulkMode: postgres_copy
```

### ClickHouse multi-row INSERT bulk load

Use `bulkMode: clickhouse_insert` to emit one `INSERT ... VALUES (...), (...)` statement per batch slice.

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, amount:amount
      options:
        dialect: clickhouse
        bulkMode: clickhouse_insert
```

Verified by `ClickHouseInsertBulkWriterIntegrationTests` (Testcontainers; requires Docker).

## Combining upsert with chunked execution

Upsert runs cleanly under `CHUNKED` execution (typical for large JDBC exports). Pair with `executionPolicy.sinkBatchSize` for batch flush sizing.

```yaml
executionPolicy:
  mode: CHUNKED
  sourceChunkSize: 1000
  sinkBatchSize: 1000
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
      options:
        dialect: postgres
        upsert: true
        upsertKeys: [id]
```

## Limitations

Unsupported capabilities **fail fast at publish and run** — they are not silently ignored.

| Dialect / combination | Supported | Unsupported (fail-fast or blocked) |
|----------------------|-----------|-----------------------------------|
| `postgres` | INSERT, upsert (`ON CONFLICT`), `postgres_copy` bulk | `upsert` + `postgres_copy` together |
| `mysql` | INSERT, upsert (`ON DUPLICATE KEY UPDATE`) | — |
| `dameng` | INSERT, upsert (`MERGE INTO`) | `postgres_copy`, `clickhouse_insert` bulk modes |
| `kingbase`, `highgo` | INSERT, upsert (PostgreSQL `ON CONFLICT` path) | `postgres_copy`, `clickhouse_insert` bulk modes |
| `clickhouse` | INSERT, `clickhouse_insert` bulk | **`upsert: true`** (explicit rejection) |
| `generic` | INSERT (`jdbc_batch` only) | **`upsert: true`** (explicit rejection; Phase 9 tightens Phase 8 ignore behavior) |

Additional notes:

- **Bulk modes and upsert**: `postgres_copy` and `clickhouse_insert` reject `upsert: true`; use `jdbc_batch` when duplicate handling is required.
- **ClickHouse FORMAT CSV**: not implemented; `clickhouse_insert` uses JDBC multi-value `INSERT` only.
- **Unknown dialect + upsert**: any dialect key other than `postgres`, `mysql`, `dameng`, `kingbase`, or `highgo` with `upsert: true` fails at publish and run.
- **Parallel sinks**: optional `sinkExecutionPolicy.parallelSinks` runs writers concurrently when more than one writer is configured; targets must be independent (no shared connection assumptions).

### Embedded test strategy (developers)

| Engine | Default CI proof | Opt-in real-engine IT |
|--------|------------------|------------------------|
| PostgreSQL, ClickHouse | Testcontainers read/write / insert-or-reject | — |
| Kingbase, HighGo | PostgreSQL container proxy + `dialect=kingbase\|highgo` mapping unit tests (D-15) | Licensed KB/HG image not required for merge gate |
| Dameng | MERGE SQL generation unit tests | `-Ddm.it=true` or `DG_DM_IT=true` when DM host available (D-14) |
| ClickHouse upsert | Contract tests proving `upsert: true` rejection | — |

Run the Phase 9 dialect gate: `.\scripts\verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright`

## Sink execution policy

```yaml
sinkExecutionPolicy:
  mode: CONTINUE_ON_ERROR
  maxRetries: 3
  retryBackoffMs: 100
  parallelSinks: false
```

Under `CONTINUE_ON_ERROR`, run reports include per-sink `rowsOk`, `rowsFailed`, `rowsUpserted`, `rowsSkipped`, and `errorSample` in the job detail UI.

## Related

- Streaming CSV/JSON: `docs/template-v2-streaming-csv-json-guide.md`
- Scenario catalog GF-G: `docs/template-v2-scenario-template-catalog.md`
- Embedded testing: `docs/testing-embedded-components.md`
- Phase 8 verification: `.\scripts\verify-phase8-uat-rw-streaming-upsert.ps1 -SkipPlaywright`
- Phase 9 verification: `.\scripts\verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright`
- Dameng live IT (opt-in): `.\scripts\verify-phase13-uat-dameng-live.ps1` — see [Dameng live IT (opt-in, DIAL-01)](#dameng-live-it-opt-in-dial-01) above
