---
status: testing
phase: 13-dameng-live-path-nyquist-hygiene
source:
  - 13-VERIFICATION.md
started: 2026-07-28T21:50:00+08:00
updated: 2026-07-28T21:50:00+08:00
---

## Current Test

number: 1
name: Run Dameng live IT against a real reachable Dameng host
expected: |
  With DG_DM_IT=true and valid DG_DM_JDBC_URL / DG_DM_USER / DG_DM_PASSWORD,
  .\scripts\verify-phase13-uat-dameng-live.ps1 prints SUCCESS for chunked upsert
  idempotency and Maven BUILD SUCCESS with ChunkedPipelineDamengUpsertIT green.
awaiting: user response

## Tests

### 1. Run Dameng live IT against a real reachable Dameng host
expected: |
  Configure DG_DM_IT=true, DG_DM_JDBC_URL, DG_DM_USER, DG_DM_PASSWORD against a
  Dameng instance with DDL rights for upsert_source_t / upsert_target_t, then run
  .\scripts\verify-phase13-uat-dameng-live.ps1. Expect SUCCESS banner and green IT.
  (If no host is available, ROADMAP Success Criterion 4 / CONTEXT D-05 accepts the
  documented enable path + MERGE-unit merge bar as the honest completion bar —
  skip this item with that reason.)
result: [pending]

## Summary

total: 1
passed: 0
issues: 0
pending: 1
skipped: 0
blocked: 0

## Gaps
