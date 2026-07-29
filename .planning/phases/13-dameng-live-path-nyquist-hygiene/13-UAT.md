---
status: complete
phase: 13-dameng-live-path-nyquist-hygiene
source:
  - 13-VERIFICATION.md
started: 2026-07-28T21:50:00+08:00
updated: 2026-07-29T10:30:00+08:00
---

## Current Test

[testing complete]

## Tests

### 1. Run Dameng live IT against a real reachable Dameng host
expected: With DG_DM_IT=true and valid DG_DM_* credentials, verify-phase13-uat-dameng-live.ps1 prints SUCCESS for chunked upsert idempotency and Maven BUILD SUCCESS with ChunkedPipelineDamengUpsertIT green.
result: issue (historical — resolved via plan 13-05)
reported: "Configured Dameng host reachable; ChunkedPipelineDamengUpsertIT failed with AssertionFailedError: second run should record rowsUpserted > 0; dialect=dameng (Surefire Failures: 1). Wrapper threw Dameng live IT failed with exit code 1."
resolved: "Plan 13-05 fixed JdbcBulkWriteExecutor.upsertCountAsRows for dm-jdbc updateCount==0; JdbcUpsertSmokeTests (9 tests, 0 failures) provides CI-safe proof. Live re-run optional for maintainer confirmation (ROADMAP SC4 / D-05)."
severity: major

## Summary

total: 1
passed: 0
issues: 0
resolved: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

```yaml
- truth: "Configured Dameng live IT passes chunked upsert idempotency including rowsUpserted > 0 on second MERGE run"
  status: resolved
  resolved_by: "13-05"
  reason: "User reported: Dameng host connected but Surefire failure — second run should record rowsUpserted > 0; dialect=dameng expected true but was false (UpsertParitySupport.java:106). First-run insert and count-stability assertions appear to have passed before the metrics check."
  resolution: "Plan 13-05 added Dameng branch in JdbcBulkWriteExecutor.upsertCountAsRows (non-negative updateCount → 1 upsert row) and JdbcUpsertSmokeTests unit coverage. CI-safe proof; optional live re-run for maintainer confirmation per ROADMAP SC4 / D-05."
  severity: major
  test: 1
  root_cause: "dm-jdbc 1.8 returns batch updateCount==0 for successful MERGE WHEN MATCHED UPDATE; JdbcBulkWriteExecutor.upsertCountAsRows only counted Dameng rows when updateCount>0 (or SUCCESS_NO_INFO), so rowsUpserted stayed 0 despite data correctly upserting. See .planning/debug/dameng-rows-upserted-metric.md"
  artifacts:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/JdbcUpsertSmokeTests.java
  missing: []
```
