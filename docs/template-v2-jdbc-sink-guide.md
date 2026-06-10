# Template V2 JDBC sink guide

## Overview

V2 JDBC sinks use `JdbcRowSinkAdapter` with a generic `INSERT` statement by default. Dialect-specific upsert behavior is opt-in via writer `options`.

## Writer options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `dialect` | string | `generic` | `generic`, `postgres`, or `mysql` |
| `upsert` | boolean | `false` | Enable dialect-specific duplicate handling |
| `conflictColumns` | string | — | Required for `postgres` upsert; comma-separated conflict key columns |

## Examples

### Generic insert (all databases)

```yaml
sinks:
  - writers:
      - type: JDBC
        target: orders_out
        template: id:value, amount:amount
```

### PostgreSQL upsert (ON CONFLICT DO NOTHING)

```yaml
sinks:
  - writers:
      - type: JDBC
        target: orders_out
        template: id:value, amount:amount
        options:
          dialect: postgres
          upsert: true
          conflictColumns: id
```

### MySQL insert ignore

```yaml
sinks:
  - writers:
      - type: JDBC
        target: orders_out
        template: id:value, amount:amount
        options:
          dialect: mysql
          upsert: true
```

## Limitations

- **ClickHouse / bulk loaders**: no native COPY/bulk path yet; use generic `INSERT` only.
- **Generic dialect + upsert**: `upsert` is ignored; only `postgres` and `mysql` change SQL shape.
- **ON DUPLICATE KEY UPDATE**: not generated; MySQL path uses `INSERT IGNORE` only.
- **Parallel sinks**: optional `sinkExecutionPolicy.parallelSinks` runs writers concurrently; use with independent targets.

## Sink execution policy

```yaml
sinkExecutionPolicy:
  mode: CONTINUE_ON_ERROR
  maxRetries: 3
  retryBackoffMs: 100
  parallelSinks: false
```

Under `CONTINUE_ON_ERROR`, run reports include per-sink `rowsOk`, `rowsFailed`, and `errorSample` in the job detail UI.
