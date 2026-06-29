---
status: testing
phase: 08-rw-streaming-upsert
source: [08-VERIFICATION.md]
started: 2026-06-29T10:10:00Z
updated: 2026-06-29T10:10:00Z
---

## Current Test

number: 1
name: Full Phase 8 Podman Playwright UAT
expected: |
  Run `.\scripts\verify-phase8-uat-rw-streaming-upsert.ps1` (without `-SkipPlaywright`) on a host with Podman/Docker.
  All 7 `rw-streaming-upsert.spec.ts` scenarios pass; Job center UI shows sink metrics and actionable errors.
awaiting: user response

## Tests

### 1. Full Phase 8 Podman Playwright UAT
expected: All 7 Playwright scenarios pass against containerized service on :9876
result: [pending]

### 2. Operator smoke on real PG/MySQL datasources
expected: Second upsert run updates rows in place; run report shows rowsUpserted > 0
result: [pending]

### 3. REQUIREMENTS.md checkbox update
expected: RW-01..RW-04 marked Complete in `.planning/REQUIREMENTS.md` after sign-off
result: [pending]

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps
