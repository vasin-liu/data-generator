---
status: complete
phase: 13-dameng-live-path-nyquist-hygiene
source:
  - 13-VERIFICATION.md
started: 2026-07-28T21:50:00+08:00
updated: 2026-07-29T09:50:00+08:00
---

## Current Test

[testing complete]

## Tests

### 1. Run Dameng live IT against a real reachable Dameng host
expected: With DG_DM_IT=true and valid DG_DM_* credentials, verify-phase13-uat-dameng-live.ps1 prints SUCCESS for chunked upsert idempotency and Maven BUILD SUCCESS with ChunkedPipelineDamengUpsertIT green.
result: issue
reported: "Configured Dameng host reachable; ChunkedPipelineDamengUpsertIT failed with AssertionFailedError: second run should record rowsUpserted > 0; dialect=dameng (Surefire Failures: 1). Wrapper threw Dameng live IT failed with exit code 1."
severity: major

## Summary

total: 1
passed: 0
issues: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

```yaml
- truth: "Configured Dameng live IT passes chunked upsert idempotency including rowsUpserted > 0 on second MERGE run"
  status: failed
  reason: "User reported: Dameng host connected but Surefire failure — second run should record rowsUpserted > 0; dialect=dameng expected true but was false (UpsertParitySupport.java:106). First-run insert and count-stability assertions appear to have passed before the metrics check."
  severity: major
  test: 1
  artifacts: []
  missing: []
```
