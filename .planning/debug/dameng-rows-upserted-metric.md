---
status: resolved
phase: 13-dameng-live-path-nyquist-hygiene
slug: dameng-rows-upserted-metric
created: 2026-07-29
resolved_by: Phase 13-05 JdbcUpsertSmokeTests
resolved_at: 2026-07-29
---

# DEBUG: Dameng live IT fails on rowsUpserted metric

## Symptoms

- Live Dameng host reachable; IT ran ~192s
- `ChunkedPipelineDamengUpsertIT.chunkedUpsertDamengMergeIsIdempotent` → Surefire Failures: 1
- Assertion: `second run should record rowsUpserted > 0; dialect=dameng`
- Preceding assertions passed: first-run insert count, second-run count unchanged, name updated (data MERGE succeeded)

## Hypotheses

1. Dameng JDBC returns updateCount == 0 for successful MERGE WHEN MATCHED UPDATE — CONFIRMED
2. Other hypotheses rejected in Phase 13 diagnosis

## Resolution

Fixed in plan 13-05: Dameng early-return in JdbcBulkWriteExecutor.upsertCountAsRows treats non-negative updateCount (including 0) as successful upsert row. Covered by JdbcUpsertSmokeTests. Closed at v2.1 milestone complete.
