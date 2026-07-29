---
phase: 13-dameng-live-path-nyquist-hygiene
review_scope: "plans 13-01 and 13-02 (Dameng live IT wiring + docs/script)"
status: clean
reviewer: code-reviewer subagent
date: 2026-07-28
files_reviewed:
  - data-generator-calcite/pom.xml
  - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java
  - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/DamengTestSupport.java
  - scripts/verify-phase13-uat-dameng-live.ps1
  - docs/template-v2-jdbc-sink-guide.md (Dameng live IT section)
  - AGENTS.md (Commands entry)
findings:
  critical: 0
  high: 0
  medium: 0
  low: 0
  info: 2
---

# Phase 13 Code Review — Dameng live path (plans 13-01, 13-02)

## Scope

Reviewed the product/source and script changes from plans 13-01 (live JDBC wiring) and 13-02
(docs + AGENTS.md entry) against the review focus: credential leakage, fail-closed semantics,
soft-skip-disguised-as-green, P0 gate contamination, and Java Javadoc/copyright conventions.
`.planning` VALIDATION/SUMMARY/PLAN prose was read for context only, not reviewed as product code.

## Verification performed

- Confirmed `com.dameng:dm-jdbc` has no explicit `<version>` in `data-generator-calcite/pom.xml`
  (inherits `${dm.version}` = `1.8` from the root BOM) — no version drift risk.
- Confirmed `UpsertParitySupport` (the shared helper the new IT delegates to) is byte-for-byte
  unchanged — no risk of regressing the PostgreSQL/MySQL/Kingbase/HighGo upsert tests that share it.
- Confirmed via `.planning/test-matrix.yaml` (`v2-dialect-dameng` row) that `ChunkedPipelineDamengUpsertIT`
  is explicitly **not** in `linked_tests` — the P0 bar for Dameng stays on the unit-level
  `JdbcSinkSqlBuilderTests`. Confirmed `scripts/verify-harness.ps1` only ever runs matrix-linked
  classes, and neither it nor `.github/workflows/harness-verify.yml` reference the new script or class.
  No P0 gate contamination.
- Confirmed the test class name ends in `...UpsertIT` (Failsafe convention), which does **not** match
  Surefire's default include globs (`**/*Test.java`, `**/*Tests.java`, `**/*TestCase.java`), and no
  `maven-failsafe-plugin` execution is bound to any lifecycle phase in this repo. So the class is
  invisible to a plain `mvn test`/`mvn verify` and can only run via the explicit
  `-Dtest=ChunkedPipelineDamengUpsertIT` selector the wrapper script uses. This is a second,
  independent layer of protection against accidental default-CI execution, on top of the
  `@EnabledIf` gate.
- Confirmed `-Dsurefire.failIfNoSpecifiedTests=false` combined with explicit `-Dtest=...` is the
  same established pattern used by ~15 other `verify-phase*`/`verify-ai-*` scripts in this repo — not
  a novel soft-pass risk.
- Confirmed neither `ChunkedPipelineDamengUpsertIT.requireEnv` nor
  `scripts/verify-phase13-uat-dameng-live.ps1` ever print `DG_DM_JDBC_URL`/`DG_DM_USER`/`DG_DM_PASSWORD`
  values — only variable names, in both the hard-fail exception message and the script's
  missing-variable warning list. `Invoke-RepoMaven` streams Maven's own stdout/stderr but no
  credential value is ever passed as a `-D` system property (env-only design), so it cannot appear in
  process argument lists or Maven's own log output.
- Confirmed fail-closed semantics end-to-end:
  - `DamengTestSupport.damengItEnabled()` — opt-in only via exact `true` (case-insensitive) on
    property or env; absent/anything else → disabled (test skipped, not silently passed).
  - `requireEnv` — flag on + missing/blank var → `IllegalStateException`, hard build failure, never a
    skip. This is the specific "misconfigured opt-in run must never report as green" invariant called
    out in both the class Javadoc and the docs.
  - `verify-phase13-uat-dameng-live.ps1` — checks all four required vars *before* invoking Maven and
    `exit 1`s with `$ErrorActionPreference = 'Stop'` already set if any are missing; only calls Maven
    once preconditions are met, and propagates a non-zero Maven exit via `throw`.
- Confirmed Javadoc/copyright compliance: both touched `.java` files have the exact repo copyright
  block, type-level Javadoc with `@author`/`@since`, and Javadoc on the public API surface
  (`DamengTestSupport.damengItEnabled()`); the test class and its lone `@Test` method are
  package-private, so the "all public methods" rule doesn't strictly apply, but Javadoc was added
  anyway for clarity.
- Confirmed the docs anchor link `#dameng-live-it-opt-in-dial-01` matches the GitHub-flavored-markdown
  slug generated from the new `#### Dameng live IT (opt-in, DIAL-01)` heading.
- Confirmed `AGENTS.md` diff is exactly the two intended lines (comment + command) inserted in the
  existing Commands section; the P0 merge-criteria / 15-row-gate prose elsewhere in the file is
  untouched.

## Findings

No CRITICAL, HIGH, or MEDIUM issues found.

### [INFO] Shared `UpsertParitySupport` error path could theoretically echo driver-supplied text on failure

**Files:** `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/UpsertParitySupport.java` (unmodified, out of scope), exercised by `ChunkedPipelineDamengUpsertIT`

`withDialectSqlHint` folds `cause.getMessage()` from the underlying JDBC/Spring exception into a new
assertion/exception message, which Surefire will print on failure. For the existing PostgreSQL/MySQL/
Kingbase/HighGo callers this is low-risk (ephemeral Testcontainers credentials). For Dameng this path
now carries a **real, operator-supplied `DG_DM_PASSWORD`** value into the connection attempt for the
first time. Most JDBC drivers (including `dm.jdbc.driver.DmDriver`, per the URL format used —
password is passed via `DriverManagerDataSource`, not embedded in the URL) do not echo the password in
connection-failure exception text, and this is a pre-existing shared file plans 13-01/13-02 explicitly
declared unmodified — so this is not attributable to the diff under review. Flagging only as a residual
note: if a future Dameng driver upgrade ever changes that behavior, a failed live IT run could leak the
password into CI/Surefire logs. No action required now.

### [INFO] `requireEnv`'s hard-fail message references the docs guide by relative path

`ChunkedPipelineDamengUpsertIT.requireEnv` points readers at
`docs/template-v2-jdbc-sink-guide.md`, and the class Javadoc does the same. This is a repo-root-relative
path (correct when running from the repo root, which is how `mvnw`/the wrapper script always invoke
Maven). No fix needed; noting only because it's the kind of string that would silently rot if the doc
were ever renamed/moved — no test currently guards the reference.

## Summary

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 0     | pass   |
| HIGH     | 0     | pass   |
| MEDIUM   | 0     | pass   |
| LOW      | 0     | pass   |
| INFO     | 2     | note   |

**Verdict: CLEAN.** Credential handling is hard-fail-only and never prints values; the live IT is
excluded from the P0 gate through two independent mechanisms (matrix `linked_tests` omission and
Surefire naming-convention invisibility) in addition to the `@EnabledIf` gate; the wrapper script is
fail-closed before any Maven invocation; and both touched Java files satisfy the repository's
copyright/Javadoc conventions.
