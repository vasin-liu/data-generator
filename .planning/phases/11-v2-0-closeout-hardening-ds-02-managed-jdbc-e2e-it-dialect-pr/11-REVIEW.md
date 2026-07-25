---
phase: 11-v2-0-closeout-hardening-ds-02-managed-jdbc-e2e-it-dialect-pr
date: 2026-07-25
reviewed: 2026-07-25T01:26:00Z
depth: standard
files_reviewed: 3
files_reviewed_list:
  - data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogSinkE2eIT.java
  - data-generator-console-web/e2e/specs/jdbc-dialect-preset.spec.ts
  - scripts/verify-phase11-uat-closeout-hardening.ps1
findings:
  critical: 0
  warning: 1
  info: 2
  total: 3
status: issues
advisory: true
---

# Phase 11: Code Review Report

**Reviewed:** 2026-07-25T01:26:00Z
**Depth:** standard
**Files Reviewed:** 3
**Status:** issues

## Summary

Advisory standard review of Phase 11 closeout hardening (plans 11-01..11-03): managed JDBC catalog sink E2E IT, Kingbase/postgresql16 Playwright preset→save coverage, and the supplementary UAT script. Docs (`AGENTS.md`, `.planning/v2.0-MILESTONE-AUDIT.md`) were light-touched for consistency with claimed scope — no doc defects reported.

Overall the phase work matches CONTEXT decisions (D-01–D-20): dedicated IT, unbound runner, plain INSERT, no Test Connection, evidence-pack UAT (not P0), reused `e2e:phase9-jdbc-dialect`. No security issues (test-only H2/`e2e_test_only` credentials; no secrets in logs). One test-lifecycle reliability warning; two info-level tighten-ups.

## Critical Issues

_None._

## Warnings

### WR-01: Named H2 mem DB table not dropped before CREATE

**File:** `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogSinkE2eIT.java:53-54,73-78,89-95`
**Issue:** `@BeforeEach` deletes the catalog row and removes the routing entry, but the named in-memory DB (`jdbc:h2:mem:managed-jdbc-catalog-sink-e2e;…DB_CLOSE_DELAY=-1`) retains `managed_e2e_sink` across pool teardown. A same-JVM re-run (IDE re-run, surefire retry, added test methods) fails on bare `create table` because the table already exists — flaky lifecycle hygiene for an otherwise solid DS-02 proof.
**Fix:** Drop (or recreate) the sink table before DDL, e.g.:

```java
@BeforeEach
void resetDatasourceRow() {
    dataSourceConfigRepository.findById(DS_NAME).ifPresent(dataSourceConfigRepository::delete);
    if (dynamicRoutingDataSource.getDataSources().containsKey(DS_NAME)) {
        dynamicRoutingDataSource.removeDataSource(DS_NAME);
    }
}

// inside the test, after save, before create:
try {
    DynamicDataSourceContextHolder.push(DS_NAME);
    namedParameterJdbcTemplate.getJdbcTemplate().execute("drop table if exists " + TABLE);
    namedParameterJdbcTemplate.getJdbcTemplate().execute(
            "create table " + TABLE + " (id int primary key, label varchar(64))");
} finally {
    DynamicDataSourceContextHolder.clear();
}
```

## Info

### IN-01: Soft `rowsWritten` assertion can never fail if metrics is null

**File:** `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogSinkE2eIT.java:107-109`
**Issue:** `COUNT(*)` is correctly the primary DS-02 proof, but wrapping `getRowsWritten()` in `if (result.getMetrics() != null)` means a null-metrics success path skips the secondary check. Current pipelines normally return non-null `RunMetrics`, so this is overly soft.
**Fix:** Prefer `assertThat(result.getMetrics()).isNotNull()` then assert `getRowsWritten()`, or drop the metrics check entirely if COUNT-only is intentional.

### IN-02: UAT script exit 0 does not prove Kingbase upsert IT executed

**File:** `scripts/verify-phase11-uat-closeout-hardening.ps1:54-76`
**Issue:** `ChunkedPipelineKingbaseDialectTests` is `@EnabledIf(DockerTestSupport#dockerAvailable)`. Without Docker, Surefire skips the class and the script still reports SUCCESS — so piece 3 of the documented evidence pack can vanish silently. Acceptable for CI-friendly supplementary UAT (matches phase9), but weak if operators treat `-SkipPlaywright` green as full RW-06 pack proof.
**Fix:** Optionally parse Surefire XML for that class (`tests > 0` and `skipped == 0`), or print an explicit warning when the Kingbase class was skipped.

## Docs (light touch)

- `AGENTS.md` — phase11 UAT entry and supplementary-not-merge-gate wording align with D-17/D-18; no issues.
- `.planning/v2.0-MILESTONE-AUDIT.md` — flows #1/#8 OK with accepted limits; overall `tech_debt` retained per D-20; no issues.

## Review Summary

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 0     | pass   |
| WARNING  | 1     | warn   |
| INFO     | 2     | note   |
| TOTAL    | 3     | —      |

**Verdict:** WARNING — 1 test-reliability issue (WR-01) should be fixed before relying on IDE/retry re-runs; not a production security/correctness blocker. Advisory only — no source fixes applied.

---

_Reviewed: 2026-07-25T01:26:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
_Mode: advisory (no fixes applied)_
