---
status: diagnosed
phase: 13-dameng-live-path-nyquist-hygiene
slug: dameng-rows-upserted-metric
created: 2026-07-29
---

# DEBUG: Dameng live IT fails on rowsUpserted metric

## Symptoms

- Live Dameng host reachable (`jdbc:dm://172.25.22.2:30236`); IT ran ~192s
- `ChunkedPipelineDamengUpsertIT.chunkedUpsertDamengMergeIsIdempotent` → Surefire **Failures: 1**
- Assertion: `second run should record rowsUpserted > 0; dialect=dameng` (`UpsertParitySupport.java:106`)
- Preceding assertions passed: first-run insert count (500), second-run count unchanged, `name == "u0"` (data MERGE succeeded)

## Hypotheses

1. **Dameng JDBC returns `updateCount == 0` for successful `MERGE … WHEN MATCHED THEN UPDATE`** — metrics treat `> 0` only → **CONFIRMED (code-level)**
2. Driver returns `EXECUTE_FAILED` / negative without throwing — unlikely (data updated)
3. Metrics not plumbed for Dameng dialect key — **REJECTED** (`isPostgresStyleUpsertDialect` includes `"dameng"`)
4. Second run skipped all rows via null upsert-key filter — **REJECTED** (name updated in target)

## Investigation

`JdbcBulkWriteExecutor.writeJdbcBatch` calls `jdbcTemplate.batchUpdate` then:

```java
long upserted = countUpsertedRows(updateCounts, dialect);
writeStats.addRowsUpserted(upserted);
```

For Dameng (`isPostgresStyleUpsertDialect`):

```java
return updateCount > 0 ? 1 : 0;  // SUCCESS_NO_INFO (-2) → 1; 0 → 0
```

Comment at line 121 claims "Dameng MERGE report successful row ops as updateCount > 0" — **this assumption is false on dm-jdbc 1.8 against the live host**: MATCHED UPDATE path persists data but batch counts are `0`, so `rowsUpserted` stays 0 while business assertions pass.

MySQL uses `updateCount == 2` for updates; Postgres returns `1`. Dameng MERGE matched-update returns `0` (driver quirk), which is neither `SUCCESS_NO_INFO` nor `> 0`.

## Root Cause

**Dameng JDBC (`dm-jdbc` 1.8) reports batch `updateCount == 0` for successful MERGE WHEN MATCHED UPDATE.**  
`JdbcBulkWriteExecutor.upsertCountAsRows` only counts Dameng rows when `updateCount > 0` (or `SUCCESS_NO_INFO`), so the second upsert run correctly writes rows but records `rowsUpserted == 0`, failing `UpsertParitySupport`'s metric assertion. The Phase 13 IT wiring is fine; the dialect metric interpretation for Dameng is wrong.

## Suggested Fix

1. In `JdbcBulkWriteExecutor.upsertCountAsRows`, for dialect `dameng`: treat `updateCount == 0` as one successful upsert row (same effective treatment as `SUCCESS_NO_INFO`), with an inline comment documenting the dm-jdbc MERGE quirk.
2. Add a focused unit test on `countUpsertedRows` with Dameng dialect and `{0, 0, SUCCESS_NO_INFO, 1}` inputs proving zero counts still contribute.
3. Re-run `.\scripts\verify-phase13-uat-dameng-live.ps1` against the same host to confirm green (credentials stay in env only).

**Do not** weaken the data assertions in `UpsertParitySupport`; fix the metric counter.
