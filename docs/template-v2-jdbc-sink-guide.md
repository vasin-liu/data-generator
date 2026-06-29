# Template V2 JDBC sink guide

## Overview

V2 JDBC sinks use `JdbcRowSinkAdapter` with a generic `INSERT` statement by default. Dialect-specific upsert behavior is opt-in via writer `options`.

Phase 8 adds **`options.upsertKeys`** (YAML array) for PostgreSQL `ON CONFLICT` and MySQL `ON DUPLICATE KEY UPDATE`. Legacy `conflictColumns` (comma-separated string) remains supported for PostgreSQL only.

## Writer options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `dialect` | string | `generic` | `generic`, `postgres`, `mysql`, or `clickhouse` |
| `bulkMode` | string | `jdbc_batch` | `jdbc_batch`, `postgres_copy` (alias `copy`), or `clickhouse_insert` (alias `clickhouse`) |
| `upsert` | boolean | `false` | Enable dialect-specific duplicate handling |
| `upsertKeys` | string[] | — | **Required** when `upsert: true` (Phase 8). Conflict key columns |
| `conflictColumns` | string | — | Legacy PostgreSQL-only comma-separated keys; prefer `upsertKeys` |

When `upsert: true` and `upsertKeys` is missing, empty, or references unknown columns, publish and run **fail fast** (same severity as governance blocks).

## Examples

### Generic insert (all databases)

```yaml
sink:
  writers:
    - type: JDBC
      target: orders_out
      template: id:value, amount:amount
```

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

Idempotent re-run: a second execution **updates** existing keys instead of inserting duplicates (D-15). Verified by `ChunkedPipelinePostgresUpsertTests` and scenario **GF-G** (`scenario-g-upsert-pg.yaml`).

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

### ClickHouse insert (dedup via table engine)

ClickHouse has no `ON CONFLICT` or `INSERT IGNORE`. With `dialect: clickhouse` and `upsert: true`, the runtime rejects upsert in Phase 8 — use `ReplacingMergeTree` / `CollapsingMergeTree` (or application-level keys) for duplicate handling. **Dialect upsert expansion** (Dameng, Kingbase, HighGo, ClickHouse) is planned for **Phase 9**.

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

- **Upsert dialects (Phase 8):** `postgres` and `mysql` only. Dameng, Kingbase, HighGo, and ClickHouse upsert SQL → Phase 9.
- **Bulk modes and upsert**: `postgres_copy` and `clickhouse_insert` reject `upsert: true`; use `jdbc_batch` when duplicate handling is required.
- **ClickHouse FORMAT CSV**: not implemented; `clickhouse_insert` uses JDBC multi-value `INSERT` only.
- **Generic dialect + upsert**: `upsert` is ignored unless `postgres` or `mysql` is set.
- **Parallel sinks**: optional `sinkExecutionPolicy.parallelSinks` runs writers concurrently when more than one writer is configured; targets must be independent (no shared connection assumptions).

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
- Verification: `.\scripts\verify-phase8-uat-rw-streaming-upsert.ps1 -SkipPlaywright`
