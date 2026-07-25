---
phase: 12-http-execute-path-proof
date: 2026-07-25
reviewed: 2026-07-25T20:34:00Z
depth: standard
files_reviewed: 4
files_reviewed_list:
  - data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpExecuteIT.java
  - data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpPostgresUpsertIT.java
  - data-generator-service/src/test/java/org/gensokyo/data/support/DockerTestSupport.java
  - data-generator-service/pom.xml
findings:
  critical: 0
  warning: 0
  info: 3
  total: 3
status: clean
advisory: true
---

# Phase 12: Code Review Report

**Reviewed:** 2026-07-25T20:34:00Z
**Depth:** standard
**Files Reviewed:** 4
**Status:** clean

## Summary

Advisory standard review of Phase 12 HTTP execute-path proof (plans 12-01..12-02): MockMvc `POST /task/run/{id}` managed-catalog H2 sink (EXEC-01), Docker-gated PostgreSQL upsert ON CONFLICT (EXEC-02), service-local `DockerTestSupport`, and test-scoped Testcontainers 1.20.6 deps.

Evidence paths match SUMMARYs (publish gate override, `instanceId=` parse, ~50s fail-fast poll including CANCELLED, managed-pool COUNT). D-11 honored: no `snap:{instanceId}` assertions in either IT (comments explaining `passwordSecretRef` for snap materialization are documentation only). Copyright blocks and type/public-API Javadoc on scoped Java sources meet repository rules. Testcontainers local credentials (`test`/`test`, H2 empty password, secret-ref password) are acceptable for embedded/local IT use. No critical or warning defects found.

## Critical Issues

_None._

## Warnings

_None._

## Info

### IN-01: EXEC-02 suite stays green when Docker skips the class

**File:** `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpPostgresUpsertIT.java:75-76`
**Issue:** `@EnabledIf(DockerTestSupport#dockerAvailable)` correctly skips without Docker, but Surefire can still report BUILD SUCCESS with the class skipped — EXEC-02 evidence then evaporates silently (same pattern as Phase 11 Kingbase Docker gate). Acceptable for optional Docker ITs and Phase 17 harness deferral; weak if operators treat a no-Docker green as full EXEC-02 proof.
**Fix:** Optional — when promoting in Phase 17, parse Surefire XML (`tests > 0`, `skipped == 0`) or fail the UAT/harness row when the class was skipped.

### IN-02: FAILED/CANCELLED poll assertion omits run error detail

**File:** `ManagedJdbcCatalogHttpExecuteIT.java:226-234`, `ManagedJdbcCatalogHttpPostgresUpsertIT.java:277-285`
**Issue:** On terminal FAILED/CANCELLED the loop breaks then asserts status equals SUCCESS. AssertJ shows expected vs actual status but not `TaskExecutionSummary` error/report text, which slows diagnosis when SCRAM/auth or upsert SQL fails (the plaintext-password snap gap found in 12-02 would have been clearer with message-in-assert).
**Fix:** Prefer `assertThat(summary.status()).as("%s", summary).isEqualTo(SUCCESS)` or include `summary` / error fields in the assertion description (match whatever fields `TaskExecutionSummary` exposes).

### IN-03: Residual product gap — HTTP snap pools drop plaintext managed passwords

**File:** `ManagedJdbcCatalogHttpPostgresUpsertIT.java:90-94,159-169` (workaround); product path outside this phase’s source scope
**Issue:** SUMMARY documents that `ConnectionSnapshotSupport` snapshots `passwordSecretRef` only. EXEC-02 correctly registers Testcontainers password via `SecretService` + `passwordSecretRef`. Non-empty plaintext managed JDBC passwords still fail HTTP snap auth — IT-local fix only; product/docs ownership deferred.
**Fix:** Track for a later datasource/security phase (not a Phase 12 IT defect).

## Focus checks

| Check | Result |
|-------|--------|
| Bugs / correctness of EXEC-01/02 spine | Pass — publish → MockMvc enqueue → poll SUCCESS → COUNT; PG second-run idempotency |
| Security (secrets in tests) | Pass — local Testcontainers / H2 / secret-ref only |
| Flaky timing | Pass — ~50s poll @ 200ms with FAILED+CANCELLED fail-fast; DDL uses `drop table if exists` (Phase 11 WR-01 pattern fixed) |
| D-11 no `snap:{instanceId}` asserts | Pass — none in scoped ITs |
| Copyright + Javadoc on public types | Pass — PCI block on all three `.java`; `DockerTestSupport` class + `dockerAvailable` Javadoc; IT types documented |

## pom.xml (Testcontainers slice)

- `org.testcontainers:junit-jupiter` + `postgresql` **1.20.6**, `test` scope — aligns with calcite; no production classpath leak.

## Review Summary

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 0     | pass   |
| WARNING  | 0     | pass   |
| INFO     | 3     | note   |
| TOTAL    | 3     | —      |

**Verdict:** CLEAN — no blocking or warning-level defects in Phase 12 scoped sources. Info items are harness/diagnostics/product-debt notes for later phases. Advisory only — no source fixes applied.

---

_Reviewed: 2026-07-25T20:34:00Z_
_Reviewer: Cursor (gsd-code-reviewer)_
_Depth: standard_
_Mode: advisory (no fixes applied)_
