---
phase: 13-dameng-live-path-nyquist-hygiene
verified: 2026-07-29T10:30:00+08:00
status: passed
score: 16/16 truths verified
behavior_unverified: 0
overrides_applied: 0
optional_maintainer_confirmation:
  - test: "Optional live Dameng IT re-run against a reachable host (ROADMAP SC4 / D-05)"
    expected: "`.\\scripts\\verify-phase13-uat-dameng-live.ps1` (with `DG_DM_IT=true` + valid `DG_DM_*`) prints `[SUCCESS] Dameng live IT passed (chunked upsert idempotency).`"
    note: "Not blocking — UAT gap (rowsUpserted>0) closed by plan 13-05 unit-level metric fix; CI-safe proof via JdbcUpsertSmokeTests (9 tests, 0 failures)"
---

# Phase 13: Dameng Live Path + Nyquist Hygiene Verification Report

**Phase Goal:** Document a reproducible Dameng opt-in live IT green path and backfill Nyquist/VALIDATION hygiene for lagging v2.0 phases — without promoting Dameng live into the P0 merge gate.
**Verified:** 2026-07-29T10:30:00+08:00
**Status:** passed
**Re-verification:** Yes — post plan 13-05 gap closure (Dameng rowsUpserted metric fix)

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `ChunkedPipelineDamengUpsertIT` connects to a real external Dameng JDBC endpoint and reuses `UpsertParitySupport.assertUpsertIdempotent(..., "dm.jdbc.driver.DmDriver", "dameng")` so it **can** PASS when a host is configured (D-01, D-03, D-04, D-06, D-07) | ✓ VERIFIED (code-level) | Read `ChunkedPipelineDamengUpsertIT.java:41-49` — delegates to the unmodified shared helper with correct driver class and dialect key, env-only connection (`DG_DM_JDBC_URL`/`DG_DM_USER`/`DG_DM_PASSWORD`). `UpsertParitySupport.java` confirmed byte-identical role to the PostgreSQL/MySQL/Kingbase/HighGo call sites. Live host PASS itself is a separate human-verification item below (D-05: honest without a host) |
| 2 | Flag-on-but-misconfigured run is a hard build **FAILURE**, never a skip (D-02) | ✓ VERIFIED | Directly re-ran: `DG_DM_IT=true`, `DG_DM_JDBC_URL` unset → `mvn ... -Dtest=ChunkedPipelineDamengUpsertIT test` exited 1 with `Tests run: 1, Failures: 0, Errors: 1, Skipped: 0` and `IllegalStateException: ... required environment variable DG_DM_JDBC_URL is missing or blank`. No credential value in the message |
| 3 | Default CI without the flag still skips the IT via the class-level gate, unchanged (D-01, D-05) | ✓ VERIFIED | Directly re-ran with `DG_DM_IT`/`DG_DM_JDBC_URL` unset → `mvn ... -Dtest=ChunkedPipelineDamengUpsertIT test` exited 0 (BUILD SUCCESS, IT skipped by `@EnabledIf` on `DamengTestSupport#damengItEnabled`) |
| 4 | `dm-jdbc` resolvable at test scope in `data-generator-calcite` (no explicit version, inherits root BOM) | ✓ VERIFIED | `data-generator-calcite/pom.xml:191-195` — `com.dameng:dm-jdbc`, `<scope>test</scope>`, no `<version>`. `mvnw-jdk25.ps1 -pl data-generator-calcite -am test-compile` → BUILD SUCCESS |
| 5 | `DamengTestSupport` Javadoc documents the full opt-in contract; gate logic (`damengItEnabled()`) frozen | ✓ VERIFIED | `DamengTestSupport.java:8-46` documents flag forms, three env vars, hard-fail semantics, unchanged MERGE-unit CI bar; `damengItEnabled()` signature/logic unchanged from pre-phase behavior (flag-only check) |
| 6 | UAT wrapper fails closed with usage (names only) when unconfigured, never a false success (D-16) | ✓ VERIFIED | Directly re-ran `powershell -File scripts/verify-phase13-uat-dameng-live.ps1` with no env set → exit 1, printed all four required variable names, no URL/password value, no success banner |
| 7 | Maintainer recipe in `docs/template-v2-jdbc-sink-guide.md` covers flag forms, `DG_DM_*` vars, wrapper + direct Maven commands, PASS/FAIL/skip semantics, host DDL prerequisites, never-commit-secrets warning, and the unchanged MERGE-unit merge bar (D-14, D-05, D-08) | ✓ VERIFIED | `docs/template-v2-jdbc-sink-guide.md:135-180` — dedicated "Dameng live IT (opt-in, DIAL-01)" subsection covers every listed element; no realistic credential value present |
| 8 | `AGENTS.md` Commands section points at the opt-in Dameng wrapper (D-17) | ✓ VERIFIED | `AGENTS.md:99-100` — comment + `.\scripts\verify-phase13-uat-dameng-live.ps1` line in the Commands fenced block |
| 9 | Stale Dameng MERGE unit-test cross-reference corrected to a real class/method | ✓ VERIFIED | `rg "JdbcSinkSqlBuilderDamengMergeTests" docs/` → no matches; `docs/template-v2-jdbc-sink-guide.md:133` cites `JdbcSinkSqlBuilderTests.buildsDamengMergeInto`, and the default MERGE-unit bar (`JdbcSinkSqlBuilderTests`) re-run green (`mvn ... -Dtest=JdbcSinkSqlBuilderTests test` → BUILD SUCCESS) |
| 10 | P0/harness/test-matrix untouched; Dameng live IT not promoted to the merge gate (D-05, DIAL-03 deferred) | ✓ VERIFIED | `rg "verify-phase13\|dameng" .planning/test-matrix.yaml scripts/verify-harness.ps1` — no `verify-phase13` reference in either file; the pre-existing `v2-dialect-dameng` row explicitly notes `ChunkedPipelineDamengUpsertIT ... is not linked` |
| 11 | `07-VALIDATION.md` refreshed in place, `nyquist_compliant: true`, Per-Task Verification Map cites only pre-existing green tests (D-11) | ✓ VERIFIED | File carries `nyquist_compliant: true`, 9 map rows each citing a test class/command traceable to `07-VERIFICATION.md` or a `07-0N-SUMMARY.md`; backfill provenance note present; commit `914ec75` touches only this file |
| 12 | `07.1-VALIDATION.md` created for the first time, `nyquist_compliant: true`, from `07.1-VERIFICATION.md`/SUMMARYs only (D-11) | ✓ VERIFIED | New file with `nyquist_compliant: true`, 6 map rows honestly labeled (1 real IT + code-trace/compile rows), Manual-Only Verifications carries the two human-judgment footnotes verbatim; commit `ebc7502` touches only this file |
| 13 | `08-VALIDATION.md` created, `nyquist_compliant: true`, grouped (not 58-row) map, accepted limits carried forward (D-11) | ✓ VERIFIED | New file with `nyquist_compliant: true`, 12 plan-task rows grouping the 58 `08-VERIFICATION.md` truths, each citing a real test class; Manual-Only Verifications carries the Playwright PG-on-H2 skip and the `CsvJsonStreamingOomIT` logging observation; commit `2605ad1` touches only this file |
| 14 | `v2.0-MILESTONE-AUDIT.md` Nyquist table + frontmatter synced to COMPLIANT for 7/07.1/8, tech-debt entries annotated not deleted (D-13) | ✓ VERIFIED | Table rows for 7/07.1/8 read COMPLIANT; frontmatter `compliant_phases: ["06","07","07.1","08","09","10"]`, `partial_phases: []`, `missing_phases: []`, `overall: compliant`; both stale tech-debt bullets prefixed `CLOSED (Phase 13, DIAL-02)` with original text preserved; commit `9ef2eee` touches only this file |
| 15 | Phase 12 validation state and the P0 gate remain untouched throughout (D-10 scope boundary) | ✓ VERIFIED | `.planning/phases/12-http-execute-path-proof/12-VALIDATION.md` still reads `nyquist_compliant: false`; `.planning/test-matrix.yaml` and `scripts/verify-harness.ps1` carry no phase-13/verify-phase13 additions |
| 16 | Dameng MERGE upsert metrics count dm-jdbc 1.8 zero batch updateCounts as successful upsert rows (13-05 gap closure) | ✓ VERIFIED | `JdbcBulkWriteExecutor.java:118-121` — Dameng early-return branch returns `1` for any non-negative `updateCount`; `JdbcUpsertSmokeTests` covers zero counts, SUCCESS_NO_INFO, positive, negative, and postgres/kingbase/highgo/mysql regressions (9 tests, 0 failures, BUILD SUCCESS) |

**Score:** 16/16 truths verified (UAT gap closed by 13-05; live host re-run optional per ROADMAP SC4 / D-05)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `data-generator-calcite/pom.xml` | test-scope `com.dameng:dm-jdbc`, no explicit version | ✓ VERIFIED | Lines 191-195; inherits `${dm.version}` from root BOM |
| `ChunkedPipelineDamengUpsertIT.java` | Real external-JDBC live IT, hard-fail-on-misconfig | ✓ VERIFIED | 66 lines; no `Assumptions` import; delegates to `UpsertParitySupport`; PCI copyright + full Javadoc present |
| `DamengTestSupport.java` | Opt-in gate with full env-contract Javadoc | ✓ VERIFIED | 47 lines; `damengItEnabled()` unchanged; Javadoc documents flag + 3 env vars + hard-fail + MERGE-unit bar |
| `scripts/verify-phase13-uat-dameng-live.ps1` | Opt-in UAT wrapper, fail-closed | ✓ VERIFIED | 45 lines; dot-sources `lib/repo-maven.ps1`; `Invoke-RepoMaven` call; fail-closed precheck confirmed by direct run |
| `docs/template-v2-jdbc-sink-guide.md` | Dameng live IT recipe section | ✓ VERIFIED | Dedicated subsection at lines 135-180 with all required elements |
| `AGENTS.md` | Commands-section entry | ✓ VERIFIED | Lines 99-100 |
| `.planning/milestones/v2.0-phases/07-.../07-VALIDATION.md` | Refreshed, `nyquist_compliant: true` | ✓ VERIFIED | In-place edit, provenance note present |
| `.planning/milestones/v2.0-phases/07.1-.../07.1-VALIDATION.md` | New, `nyquist_compliant: true` | ✓ VERIFIED | New file, 90 lines |
| `.planning/milestones/v2.0-phases/08-.../08-VALIDATION.md` | New, `nyquist_compliant: true` | ✓ VERIFIED | New file, 101 lines, grouped map |
| `.planning/milestones/v2.0-MILESTONE-AUDIT.md` | Nyquist table/frontmatter synced | ✓ VERIFIED | COMPLIANT rows, empty partial/missing lists, annotated tech debt |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `ChunkedPipelineDamengUpsertIT` | `UpsertParitySupport.assertUpsertIdempotent` | Direct call with `dm.jdbc.driver.DmDriver` / `dameng` | ✓ WIRED | Confirmed at `ChunkedPipelineDamengUpsertIT.java:43-48`; `UpsertParitySupport.java` unmodified (byte-identical shape to Postgres/MySQL sibling ITs) |
| `ChunkedPipelineDamengUpsertIT` | `DamengTestSupport#damengItEnabled` | Class-level `@EnabledIf` | ✓ WIRED | `@EnabledIf("org.gensokyo.data.calcite.support.DamengTestSupport#damengItEnabled")` at class declaration; behaviorally confirmed (flag-off → skip observed) |
| `data-generator-calcite/pom.xml` | root `pom.xml` dependencyManagement | `com.dameng:dm-jdbc` with no version | ✓ WIRED | `test-compile` succeeded, proving BOM resolution |
| `scripts/verify-phase13-uat-dameng-live.ps1` | `scripts/lib/repo-maven.ps1` | Dot-sourced `Invoke-RepoMaven` | ✓ WIRED | Confirmed in script body and via direct execution |
| `scripts/verify-phase13-uat-dameng-live.ps1` | `ChunkedPipelineDamengUpsertIT` | `-Dtest=` selector in `Invoke-RepoMaven` call | ✓ WIRED | Script line 38 |
| `AGENTS.md` | `scripts/verify-phase13-uat-dameng-live.ps1` | Commands section reference | ✓ WIRED | Confirmed present |
| `07-VALIDATION.md` / `07.1-VALIDATION.md` / `08-VALIDATION.md` | corresponding `*-VERIFICATION.md` / `*-SUMMARY.md` | Per-Task Verification Map row citations | ✓ WIRED | Every test class named in the three maps traced back to its source VERIFICATION/SUMMARY document |
| `v2.0-MILESTONE-AUDIT.md` | the three backfilled VALIDATION files | Nyquist Compliance table + `nyquist:` frontmatter | ✓ WIRED | Table and frontmatter agree, and both are backed by the actual `nyquist_compliant: true` flags in the three files |

### Behavioral Verification

| Check | Result | Detail |
|-------|--------|--------|
| `mvnw-jdk25.ps1 -pl data-generator-calcite -am test-compile` | ✓ PASS | BUILD SUCCESS |
| Flag off, `-Dtest=ChunkedPipelineDamengUpsertIT test` | ✓ PASS | Exit 0, IT skipped, default CI bar unaffected (D-01/D-05) |
| Flag on (`DG_DM_IT=true`), `DG_DM_JDBC_URL` unset, `-Dtest=ChunkedPipelineDamengUpsertIT test` | ✓ PASS (expected FAILURE observed) | Exit 1, `Tests run: 1, Errors: 1, Skipped: 0`, `IllegalStateException` naming only the missing variable — proves D-02 |
| `-Dtest=JdbcSinkSqlBuilderTests test` (default MERGE-unit merge bar) | ✓ PASS | Exit 0, BUILD SUCCESS — confirms the merge bar is unaffected by this phase |
| `scripts/verify-phase13-uat-dameng-live.ps1` unconfigured | ✓ PASS (expected exit 1 observed) | Exit 1, usage naming all four vars, no credential values |
| Full-tree scope guard: `verify-phase13`/`dameng` absent from `test-matrix.yaml`/`verify-harness.ps1` beyond the pre-existing unlinked `v2-dialect-dameng` row | ✓ PASS | Confirmed via `rg` |
| `-Dtest=JdbcUpsertSmokeTests test` (13-05 Dameng metric + dialect regressions) | ✓ PASS | BUILD SUCCESS, Tests run: 9, Failures: 0 — closes UAT gap on `rowsUpserted > 0` without live JDBC |

**Live host PASS path:** Optional maintainer confirmation only (ROADMAP SC4 / D-05). The UAT failure mode (`rowsUpserted > 0` false despite correct MERGE persistence) was a metrics bug in `upsertCountAsRows`, now fixed and proven by `JdbcUpsertSmokeTests`; full end-to-end live re-run is not a blocking item.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| DIAL-01 | 13-01, 13-02, 13-05 | Dameng live IT documented opt-in green path; default CI stays MERGE-unit; rowsUpserted metric gap closed | ✓ SATISFIED | Truths 1-10, 16 above; optional live re-run per D-05 |
| DIAL-02 | 13-03, 13-04 | Nyquist/VALIDATION hygiene backfilled for 07, 07.1, 08 | ✓ SATISFIED | Truths 11-14 above |

No orphaned requirements: `.planning/REQUIREMENTS.md` maps only DIAL-01 and DIAL-02 to Phase 13, and both are claimed by plans. DIAL-03 (P0 promotion) is correctly listed as deferred in REQUIREMENTS.md "Deferred" section and is not claimed by any Phase 13 plan.

### Anti-Patterns Found

None. Scanned all files modified by this phase (`ChunkedPipelineDamengUpsertIT.java`, `DamengTestSupport.java`, `data-generator-calcite/pom.xml`, `scripts/verify-phase13-uat-dameng-live.ps1`, `docs/template-v2-jdbc-sink-guide.md`, `AGENTS.md`, the three backfilled `*-VALIDATION.md` files, `v2.0-MILESTONE-AUDIT.md`) for `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/placeholder text — no matches. `git show --stat` on all 8 phase-13 task commits (`e6a32f9`, `b0c6c41`, `01c9819`, `fc47a4b`, `914ec75`, `ebc7502`, `2605ad1`, `9ef2eee`) confirms each touches only its declared files, with none straying into `data-generator-*` product code beyond the two declared DIAL-01 code files.

### Test Quality Audit

| Test File | Linked Req | Active | Skipped | Circular | Assertion Level | Verdict |
|-----------|-----------|--------|---------|----------|----------------|---------|
| `ChunkedPipelineDamengUpsertIT.java` | DIAL-01 | Yes (class-level `@EnabledIf`, not `@Disabled`) | Conditionally, by design (opt-in) | No — helper called live, not comparing against self-generated fixtures | Behavioral (`UpsertParitySupport.assertUpsertIdempotent` performs value-level row-count/content assertions) | Sound — the conditional skip is the documented, intentional opt-in gate, not a hidden disabled-test evasion; negative path (misconfigured) was directly re-run and confirmed to hard-fail rather than skip |
| `JdbcUpsertSmokeTests.java` | DIAL-01 (13-05) | Yes | No | No — tests `countUpsertedRows` directly with synthetic batch counts | Unit (asserts counter logic for dameng zero counts and dialect regressions) | Sound — closes the UAT-reported `rowsUpserted > 0` failure mode without requiring live dm-jdbc |

**Disabled tests on requirements:** 0 — the class-level `@EnabledIf` gate is an intentional, documented opt-in switch (D-01), not a disabled/skipped test masking a broken requirement; its off-state behavior was directly verified as a clean skip, and its on-but-misconfigured state was directly verified as a hard failure.
**Circular patterns detected:** 0 — `UpsertParitySupport` assertions compare live database row counts/values against fixture-seeded expectations, not against values generated by the system under test.
**Insufficient assertions:** 0.

### Optional Maintainer Confirmation (non-blocking)

#### 1. Live Dameng IT end-to-end re-run (ROADMAP SC4 / D-05)

**Test:** Configure `DG_DM_IT=true`, `DG_DM_JDBC_URL`, `DG_DM_USER`, `DG_DM_PASSWORD` against a real, reachable Dameng instance, then run `.\scripts\verify-phase13-uat-dameng-live.ps1`.
**Expected:** BUILD SUCCESS; `[SUCCESS] Dameng live IT passed (chunked upsert idempotency).`; `ChunkedPipelineDamengUpsertIT` green including `rowsUpserted > 0` on second MERGE run.
**Status:** Optional — not required for phase pass. UAT test 1 originally failed on `rowsUpserted > 0`; plan 13-05 fixed `JdbcBulkWriteExecutor.upsertCountAsRows` for dm-jdbc zero batch counts and added `JdbcUpsertSmokeTests` (9 tests, 0 failures). The failure mode was a metrics bug, not incorrect MERGE persistence.

### Gaps Summary

No gaps. All 16 observable truths verified. The UAT-reported gap (Dameng live IT `rowsUpserted > 0` on second MERGE run) was closed by plan 13-05 with CI-safe unit proof. Live end-to-end re-run remains optional maintainer confirmation per ROADMAP SC4 / D-05, not a blocking item. No P0/harness/test-matrix file was touched; Phase 12 validation state unchanged.

---

_Verified: 2026-07-29T10:30:00+08:00_
_Re-verified after: plan 13-05 (gap closure)_
_Verifier: Claude (gsd-verifier)_
