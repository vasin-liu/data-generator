# Phase 13: Dameng Live Path + Nyquist Hygiene - Research

**Researched:** 2026-07-28
**Domain:** Opt-in live JDBC integration testing (Dameng) + GSD Nyquist/VALIDATION documentation hygiene backfill
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Dameng proof depth
- **D-01:** When a Dameng host is available, wire real JDBC so `ChunkedPipelineDamengUpsertIT` can **PASS** (not docs-only against today's placeholder abort). Default CI remains skipped via `DamengTestSupport` (`-Ddm.it=true` / `DG_DM_IT=true`).
- **D-02:** If the enable flag is on but Dameng is unreachable / misconfigured → **hard FAIL** (no `Assumptions.abort` / soft skip disguised as green).
- **D-03:** PASS evidence = **chunked upsert idempotency** (same PK re-run; row count/content correct).
- **D-04:** **Must** reuse `UpsertParitySupport.assertUpsertIdempotent` (same helper as PG/MySQL upsert ITs).
- **D-05:** If no Dameng host is available, done criteria remain honest: documented enable path + MERGE-unit default CI bar (no fake live green) — per ROADMAP SC4 / DIAL-01.

#### Dameng runtime target
- **D-06:** Runtime = **external JDBC URL/env only** for this phase.
- **D-07:** Connection env: `DG_DM_JDBC_URL`, `DG_DM_USER`, `DG_DM_PASSWORD` (aligned with `DG_DM_IT` / `-Ddm.it=true`). Exact property aliases (if any) are planner discretion if needed for Surefire; prefer env-first as decided.
- **D-08:** Credentials via **plaintext env** for opt-in live tests; docs must warn never commit secrets.
- **D-09:** **Testcontainers / licensed Dameng image is out of scope** this phase (deferred).

#### Nyquist phase set
- **D-10:** Strict **DIAL-02** only: phases **07, 07.1, 08**. Do **not** refresh Phase 12 `12-VALIDATION.md` in this phase.
- **D-11:** Honest green = map **existing** linked tests from VERIFICATION/SUMMARY into VALIDATION, then set `nyquist_compliant: true`. **No** new product tests written only to satisfy Nyquist.
- **D-12:** Write/update VALIDATION **in-place** under `.planning/milestones/v2.0-phases/` (not copies under active `.planning/phases/`).
- **D-13:** After VALIDATION backfill, **sync** the Nyquist table in `.planning/milestones/v2.0-MILESTONE-AUDIT.md`.

#### Recipe packaging
- **D-14:** Primary recipe lives in **`docs/template-v2-jdbc-sink-guide.md`** as a dedicated Dameng live IT section (flag, `DG_DM_*`, Maven command, expected PASS/FAIL semantics).
- **D-15:** Add `scripts/verify-*-dameng*.ps1` (name at planner discretion; match existing `verify-phase*-uat-*.ps1` style) wrapping env checks + Maven slice for `ChunkedPipelineDamengUpsertIT`.
- **D-16:** Script exits **non-zero** with usage when flag off or JDBC URL missing — must not look like successful UAT.
- **D-17:** Add an opt-in Dameng live command line to **`AGENTS.md`** Commands section pointing at the doc section / script.

### Claude's Discretion
- Exact `verify-*.ps1` filename within the Dameng/phase13 naming convention
- Whether `-Ddm.it` system property mirrors are also accepted alongside `DG_DM_*` for URL/user/password (env remains canonical)
- VALIDATION table row density / status checkbox style — match phase 9 VALIDATION patterns
- Minor javadoc updates on `DamengTestSupport` / IT class as part of wiring

### Deferred Ideas (OUT OF SCOPE)
- Dameng Testcontainers / GenericContainer licensed-image path — future phase when image policy allows
- Phase 12 `12-VALIDATION.md` / `nyquist_compliant` refresh — not DIAL-02; separate hygiene if needed
- Promoting Dameng live IT to P0 merge gate — DIAL-03, explicitly out of v2.1
- Dual JDBC resolver consolidation — Phase 14 docs only / later RES-02
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DIAL-01 | Dameng live IT has a documented opt-in green path (`-Ddm.it=true` / `DG_DM_IT=true`, host/image, expected PASS); default CI remains MERGE-unit based — live IT is not a P0 merge requirement | `DamengTestSupport.damengItEnabled()` gate already exists (env + system property); `ChunkedPipelineDamengUpsertIT` placeholder replacement pattern below reuses `UpsertParitySupport.assertUpsertIdempotent` exactly like PG/MySQL; `dm-jdbc` 1.8 driver already root-BOM-managed (`dm.jdbc.driver.DmDriver`, `jdbc:dm://host:port?schema=...`); recipe packaging pattern from `docs/template-v2-jdbc-sink-guide.md` §Dameng + `scripts/verify-phase9-uat-jdbc-dialect.ps1` style + `AGENTS.md` Commands section; `.planning/test-matrix.yaml` `v2-dialect-dameng` row confirms unit-only P0 stays untouched |
| DIAL-02 | Nyquist/VALIDATION hygiene backfilled for lagging phases 07, 07.1, and 08 (docs/`nyquist_compliant` status accurate) | `09-VALIDATION.md` is the compliant-shape template; `07-VALIDATION.md` exists with `nyquist_compliant: false` (needs Per-Task map filled from `07-VERIFICATION.md` Behavioral Verification + Test Quality Audit tables); 07.1 and 08 have no VALIDATION.md — build from `07.1-VERIFICATION.md` (7 truths, 1 test file) and `08-VERIFICATION.md` (58 truths, Maven slice command in "Automated Test Results"); `v2.0-MILESTONE-AUDIT.md` `## Nyquist Compliance` table is the sync target after backfill |
</phase_requirements>

## Summary

This phase has two independent, additive workstreams — no new libraries, no new architecture, pure "close a documented gap honestly" work.

**Workstream A (DIAL-01, code):** `ChunkedPipelineDamengUpsertIT` is currently a one-method placeholder that calls `Assumptions.abort(...)` even when the opt-in flag (`DamengTestSupport.damengItEnabled()`) is on — meaning today "enabling" the live IT produces a skip that looks green in Surefire's summary. The fix is mechanical: replace the placeholder body with a real JDBC connection built from `DG_DM_JDBC_URL` / `DG_DM_USER` / `DG_DM_PASSWORD`, then delegate to `UpsertParitySupport.assertUpsertIdempotent(jdbcUrl, user, password, "dm.jdbc.driver.DmDriver", "dameng")` — the exact same helper `ChunkedPipelinePostgresUpsertTests` and `ChunkedPipelineMySqlUpsertTests` already call. `JdbcBulkWriteExecutor` already treats `"dameng"` as a PostgreSQL-style upsert dialect for `rowsUpserted` accounting, so the shared helper's assertions apply unchanged. The critical behavior change required by D-02 is: when `damengItEnabled()` is true but the JDBC env vars are missing/unreachable, the test must **fail** (throw), not abort/skip — the opposite of the existing `OllamaAiRuntimeBridgeLiveIT` precedent, which is the wrong pattern to copy here. `dm-jdbc` (version `1.8`, root-pom-managed) is not currently a test dependency of `data-generator-calcite` and must be added at test scope so the IT can load `dm.jdbc.driver.DmDriver` — no version needs to be specified since it inherits `${dm.version}` from the parent POM's `dependencyManagement`.

**Workstream B (DIAL-02, docs-only):** Phases 07, 07.1, and 08 already have complete `VERIFICATION.md` reports (and 07 already has a `VALIDATION.md` shell). Nyquist backfill is a transcription exercise: read each phase's `VERIFICATION.md` "Behavioral Verification" / "Automated Test Results" / "Test Quality Audit" tables (which already list the concrete test classes and green run counts) and populate a `## Per-Task Verification Map` following the exact shape of the compliant `09-VALIDATION.md`. No new test is written; `nyquist_compliant: true` is set once the map is filled and the sign-off checklist genuinely holds. After all three are done, `.planning/milestones/v2.0-MILESTONE-AUDIT.md`'s `## Nyquist Compliance` table (and its YAML `nyquist:` frontmatter block) is edited to move 07 from PARTIAL→COMPLIANT and 07.1/08 from MISSING→COMPLIANT.

**Primary recommendation:** Do the DIAL-01 wiring and DIAL-02 docs backfill as two independent plan waves (no shared files); keep every change additive-only (new test-scope dependency, replaced placeholder test body, new doc section, new script, backfilled VALIDATION files) and touch zero P0/harness files.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Dameng live JDBC connectivity (opt-in IT) | Domain / execution layer (`data-generator-calcite` test sources) | — | `ChunkedPipelineDamengUpsertIT` exercises `TemplateV2Runner` + `JdbcSinkFactory` directly, same tier as the PG/MySQL sibling ITs; no service/HTTP layer involved |
| Enable-flag gating (`-Ddm.it` / `DG_DM_IT`) | Infrastructure / test-support layer (`DamengTestSupport`) | — | Existing `@EnabledIf`-driven static gate class; unchanged responsibility, only consumer behavior (hard-fail vs abort) changes inside the IT |
| Env/URL parsing + hard-fail-on-misconfig | Domain / execution layer (new logic inside the IT class) | — | Small, test-local concern; does not belong in `DamengTestSupport` (which only answers "is opt-in on?", not "is it configured correctly?") |
| Recipe documentation | Presentation (docs) | — | `docs/template-v2-jdbc-sink-guide.md` is the existing home for all dialect operator guidance (Kingbase/HighGo/PG/MySQL sections already there) |
| UAT wrapper script | Infrastructure / tooling (`scripts/`) | — | Mirrors existing `verify-phase*-uat-*.ps1` pattern; wraps `Invoke-RepoMaven` from `scripts/lib/repo-maven.ps1` |
| Nyquist VALIDATION backfill | Planning/process docs (`.planning/milestones/v2.0-phases/`) | — | Pure documentation; no runtime code path |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `dm-jdbc` | 1.8 (`${dm.version}` in root `pom.xml`, unchanged) | Dameng JDBC driver for the live IT connection | Already the sole approved Dameng driver in this repo (`data-generator-service`, `data-generator-writer-database` both depend on it); adding it to `data-generator-calcite` at `test` scope is the only pom change needed |
| JUnit 5 (`junit-jupiter`) | existing (Boot BOM) | Test framework for the IT | Already used by every sibling upsert IT in the same package |
| Spring `NamedParameterJdbcTemplate` / `DriverManagerDataSource` | existing (`spring-jdbc`, already a `data-generator-calcite` dependency) | JDBC access inside `UpsertParitySupport` | Already wired; no new dependency |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| PowerShell (`scripts/lib/repo-maven.ps1` `Invoke-RepoMaven`) | existing | Cross-platform Maven wrapper used by every `verify-*.ps1` script | Reuse verbatim for the new Dameng verify script — do not hand-roll a new Maven invocation helper |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| External JDBC URL/env-only (D-06) | Testcontainers `GenericContainer` with a licensed Dameng image | Explicitly deferred (D-09) — no public Dameng Testcontainers module exists and a licensed image cannot be pulled in default CI |
| Hard-fail on misconfiguration (D-02) | `Assumptions.assumeTrue(...)` soft-skip (the `OllamaAiRuntimeBridgeLiveIT` pattern already in-tree) | Soft-skip is the anti-pattern this phase is explicitly correcting — Ollama's own precedent must NOT be copied here |

**Installation:**
```xml
<!-- data-generator-calcite/pom.xml — add alongside the other test-scope JDBC drivers -->
<dependency>
    <groupId>com.dameng</groupId>
    <artifactId>dm-jdbc</artifactId>
    <scope>test</scope>
</dependency>
```

**Version verification:** `dm-jdbc` version is fixed at `1.8` via root `pom.xml` line 61 (`<dm.version>1.8</dm.version>`) and dependencyManagement (lines 225–229). No version override needed in `data-generator-calcite/pom.xml` — omit `<version>` so it inherits from the managed BOM, matching how `data-generator-writer-database/pom.xml` (test/provided scope precedent) already declares it. [VERIFIED: root pom.xml + data-generator-writer-database/pom.xml]

## Package Legitimacy Audit

**No new packages.** `dm-jdbc` is already an approved, root-BOM-managed dependency used elsewhere in the reactor (`data-generator-service`, `data-generator-writer-database`); this phase only adds an existing managed dependency to a new module's test scope. Per the task framing, Package Legitimacy Audit is **none**.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|--------------|---------|-------------|
| `com.dameng:dm-jdbc` | already in root BOM (internal Nexus / vendor) | pre-existing in repo since prior phases | n/a (vendor JDBC driver, not a public registry package) | vendor-distributed, no public GitHub | OK (pre-approved) | Approved — reused, not newly introduced |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
Operator sets env vars                     scripts/verify-*-dameng*.ps1
  DG_DM_IT=true                                    │
  DG_DM_JDBC_URL=jdbc:dm://host:5236?schema=X       ├─ checks DG_DM_IT + DG_DM_JDBC_URL set
  DG_DM_USER=...                                    │  (exit non-zero + usage if missing — D-16)
  DG_DM_PASSWORD=...                                │
        │                                           └─ invokes Invoke-RepoMaven
        │                                                 -pl data-generator-calcite -am
        ▼                                                 -Dtest=ChunkedPipelineDamengUpsertIT
DamengTestSupport.damengItEnabled()                       -Ddm.it=true (mirrors env)
  reads -Ddm.it / DG_DM_IT                                        │
        │ true                                                    ▼
        ▼                                          ChunkedPipelineDamengUpsertIT
ChunkedPipelineDamengUpsertIT.chunkedUpsertDamengMergeIsIdempotent()
        │
        ├─ reads DG_DM_JDBC_URL / DG_DM_USER / DG_DM_PASSWORD
        │     missing/blank → throw IllegalStateException (hard FAIL, D-02)
        │
        ▼
UpsertParitySupport.assertUpsertIdempotent(url, user, pass, "dm.jdbc.driver.DmDriver", "dameng")
        │
        ├─ seeds upsert_source_t (500 rows)
        ├─ runs TemplateV2Runner (CHUNKED policy) → MERGE INTO upsert_target_t   [1st run: INSERT all]
        ├─ mutates source names
        ├─ re-runs TemplateV2Runner                                              [2nd run: MERGE upsert]
        └─ asserts: row count unchanged, updated values present, rowsUpserted > 0
```

### Recommended Project Structure
No new directories. Touched paths only:
```
data-generator-calcite/
├── pom.xml                                              # + test-scope dm-jdbc dependency
└── src/test/java/org/gensokyo/data/calcite/
    ├── runtime/ChunkedPipelineDamengUpsertIT.java       # replace placeholder body
    └── support/DamengTestSupport.java                   # optional: add env accessor javadoc only (discretion)
docs/
└── template-v2-jdbc-sink-guide.md                       # extend existing Dameng section (D-14)
scripts/
└── verify-phaseXX-uat-dameng-live.ps1                   # new (D-15) — name at planner discretion
AGENTS.md                                                # + one Commands-section line (D-17)
.planning/milestones/v2.0-phases/
├── 07-datasource-governance-hot-reload/07-VALIDATION.md              # refresh in place (D-12)
├── 07.1-close-gap-ds-03-jdbc-snapshot-routing-on-execute-path/07.1-VALIDATION.md  # new
└── 08-rw-streaming-upsert/08-VALIDATION.md                           # new
.planning/milestones/v2.0-MILESTONE-AUDIT.md             # sync Nyquist table + frontmatter (D-13)
```

### Pattern 1: Env-driven opt-in live IT with hard-fail-on-misconfigured (D-01/D-02)
**What:** When the enable flag is on, read connection env vars; if any required var is missing/blank, throw immediately (fail the test) instead of skipping.
**When to use:** Any opt-in live-infrastructure IT where "I turned this on" must never silently report green.
**Example:**
```java
// Source: pattern derived from DamengTestSupport (existing) + UpsertParitySupport (existing)
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.support.UpsertParitySupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Live Dameng MERGE upsert integration test (D-01..D-05).
 *
 * <p>Enable with {@code -Ddm.it=true} or {@code DG_DM_IT=true} plus {@code DG_DM_JDBC_URL},
 * {@code DG_DM_USER}, {@code DG_DM_PASSWORD}. When enabled but misconfigured/unreachable this
 * test fails (no soft skip) — operators who opt in must see an honest result (D-02).
 *
 * @author Gensokyo
 * @since 2026-07-28
 */
@EnabledIf("org.gensokyo.data.calcite.support.DamengTestSupport#damengItEnabled")
class ChunkedPipelineDamengUpsertIT {

    @Test
    void chunkedUpsertDamengMergeIsIdempotent() {
        String jdbcUrl = requireEnv("DG_DM_JDBC_URL");
        String user = requireEnv("DG_DM_USER");
        String password = requireEnv("DG_DM_PASSWORD");

        UpsertParitySupport.assertUpsertIdempotent(
                jdbcUrl, user, password, "dm.jdbc.driver.DmDriver", "dameng");
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            // Hard FAIL per D-02 — never Assumptions.abort here; opt-in must mean honest PASS/FAIL.
            throw new IllegalStateException(
                    "Dameng live IT enabled (-Ddm.it=true/DG_DM_IT=true) but " + name
                            + " is not set; see docs/template-v2-jdbc-sink-guide.md Dameng live IT section");
        }
        return value;
    }
}
```

### Pattern 2: Non-zero-exit UAT wrapper for missing opt-in config (D-16)
**What:** A `verify-*.ps1` script that checks required env vars up front and exits non-zero with a usage message rather than silently skipping the Maven step.
**When to use:** Any opt-in/live UAT script layered over an `@EnabledIf`-gated test, so a missing-config run cannot be mistaken for a passed UAT.
**Example:**
```powershell
# Source: pattern derived from scripts/verify-phase9-uat-jdbc-dialect.ps1 + scripts/lib/repo-maven.ps1
param()

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'lib/repo-maven.ps1')

if ($env:DG_DM_IT -ne 'true' -or [string]::IsNullOrWhiteSpace($env:DG_DM_JDBC_URL)) {
    Write-Host "Usage: set DG_DM_IT=true, DG_DM_JDBC_URL, DG_DM_USER, DG_DM_PASSWORD then re-run." -ForegroundColor Yellow
    Write-Host "See docs/template-v2-jdbc-sink-guide.md Dameng live IT section." -ForegroundColor Yellow
    exit 1   # non-zero — must not look like a passed UAT (D-16)
}

$code = Invoke-RepoMaven -RepoRoot $RepoRoot `
    -pl data-generator-calcite -am `
    '-Dtest=ChunkedPipelineDamengUpsertIT' `
    '-Dsurefire.failIfNoSpecifiedTests=false' `
    test
if ($code -ne 0) { throw "Dameng live IT failed with exit code $code" }

Write-Host "[SUCCESS] Dameng live IT passed." -ForegroundColor Green
```

### Anti-Patterns to Avoid
- **Soft-skip on unreachable live infra when the flag is on:** `OllamaAiRuntimeBridgeLiveIT.assumeOllamaAvailable()` uses `Assumptions.assumeTrue(false, ...)` when the port is closed, so the test always reports as "skipped" rather than "failed" even if the operator explicitly meant to run it. D-02 explicitly forbids this shape for Dameng — do not port this helper pattern over.
- **Docs claiming "live IT green" when the class is still a placeholder:** PITFALLS.md Pitfall 3 and the "Looks Done But Isn't" checklist both call out claiming "Dameng live" without real wiring — the phase is done only when the class can actually connect and assert idempotency.
- **Nyquist rewrite theater:** PITFALLS.md Pitfall 9 — do not reopen Phase 8 streaming/upsert implementation or write new product tests "for Nyquist"; VALIDATION backfill must cite only tests that already exist and are already green per the VERIFICATION reports.
- **P0 inflation:** Do not add `ChunkedPipelineDamengUpsertIT` (or the new verify script) to `.planning/test-matrix.yaml`'s `v2-dialect-dameng` row or to `scripts/verify-harness.ps1`. That row's `notes` field already explicitly says the optional IT "is not linked" — keep it that way (DIAL-03 is deferred).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Chunked upsert idempotency assertions (seed → run → mutate → re-run → assert) | A new Dameng-specific test scenario/template builder | `UpsertParitySupport.assertUpsertIdempotent(url, user, pass, driverClass, dialect)` | D-04 mandates reuse; the helper already parameterizes JDBC URL/driver/dialect and already treats `"dameng"` as a PostgreSQL-style upsert-count dialect via `JdbcBulkWriteExecutor.isPostgresStyleUpsertDialect` |
| Maven invocation / JDK25 wrapper selection in the new verify script | A bespoke `mvn`/`mvnw` shell-out | `scripts/lib/repo-maven.ps1` `Invoke-RepoMaven` (dot-sourced) | Every existing `verify-*.ps1` script already handles JDK25 fallback, settings.xml, and cross-platform mvnw selection here — duplicating it risks drift |
| VALIDATION.md structure/sections | A new ad hoc validation doc format | Copy the exact section shape of `09-VALIDATION.md` (frontmatter, Test Infrastructure, Sampling Rate, Per-Task Verification Map, Wave 0 Requirements, Manual-Only Verifications, Validation Sign-Off) | D-11 discretion note says "match phase 9 VALIDATION patterns"; consistency lets `/gsd-audit-milestone` parse all phases uniformly |

**Key insight:** Every piece of this phase already has a working sibling in the codebase (PG/MySQL upsert ITs, dialect docs sections, verify scripts, and one compliant VALIDATION.md). The work is substitution and transcription, not invention — any solution that introduces new abstractions (a Dameng-specific test base class, a new PowerShell helper library, a new VALIDATION template) is over-engineering for this phase's scope.

## Common Pitfalls

### Pitfall 1: Soft-skip disguised as "live IT green"
**What goes wrong:** The IT uses `Assumptions.assumeTrue`/`Assumptions.abort` when the JDBC connection fails, so a misconfigured or host-down run reports as SKIPPED in the Surefire summary — indistinguishable from "not run" but easy to misreport as "passed" in docs/UAT logs.
**Why it happens:** The existing `OllamaAiRuntimeBridgeLiveIT` in the same repo uses exactly this shape for its own opt-in live check, so it's the closest in-tree precedent to copy from muscle memory.
**How to avoid:** Read `DG_DM_JDBC_URL`/`DG_DM_USER`/`DG_DM_PASSWORD` and throw `IllegalStateException` (or let the JDBC `SQLException` propagate) when the flag is on but config/connection fails — never wrap in `Assumptions`. Only the outer `@EnabledIf("...damengItEnabled")` class-level gate is allowed to skip, and only when the flag itself is off.
**Warning signs:** Surefire XML shows `<skipped>` instead of `<failure>` for `ChunkedPipelineDamengUpsertIT` when `DG_DM_IT=true` is set but the URL is wrong.

### Pitfall 2: P0/harness inflation
**What goes wrong:** Someone adds `ChunkedPipelineDamengUpsertIT` (or the new verify script) to `.planning/test-matrix.yaml`'s `v2-dialect-dameng` row's `linked_tests`, or wires it into `verify-harness.ps1`, making the P0 merge gate depend on a licensed external database.
**Why it happens:** "We finally made it pass" naturally invites "let's make it required" — but DIAL-03 (promoting Dameng live to P0) is explicitly deferred in `.planning/REQUIREMENTS.md` Future Requirements and PITFALLS.md Pitfall 3/8 name this exact temptation.
**How to avoid:** Leave `.planning/test-matrix.yaml` completely untouched in this phase; the `v2-dialect-dameng` row's existing note ("Optional ChunkedPipelineDamengUpsertIT gated by -Ddm.it=true is not linked") stays true after this phase.
**Warning signs:** Diff touches `.planning/test-matrix.yaml`, `scripts/verify-harness.ps1`, or `.github/workflows/harness-verify.yml`.

### Pitfall 3: Nyquist backfill turning into feature rework
**What goes wrong:** While writing 07.1/08's `VALIDATION.md`, an agent notices a "gap" (e.g. Phase 8 Playwright PG-upsert scenario skipped on H2 per `08-UAT.md`) and tries to "fix" it with new test code, ballooning the phase.
**Why it happens:** VERIFICATION reports for 07/07.1/08 already document a couple of accepted limits (e.g. `08-VERIFICATION.md` "Playwright D-23 #2 PG upsert skipped on H2 e2e (W-01); covered by Testcontainers"); it's tempting to "complete" these while touching the docs anyway.
**How to avoid:** VALIDATION backfill only **transcribes** already-green tests already recorded in each phase's `VERIFICATION.md`/`SUMMARY.md`/`UAT.md`; accepted limits get copied into `Manual-Only Verifications` verbatim (as `09-VALIDATION.md` already does for Dameng/Kingbase), not resolved.
**Warning signs:** Plan tasks for 07/07.1/08 touch `.java` files under `data-generator-calcite`/`data-generator-service` `src/main` or `src/test` — DIAL-02 tasks should touch only `.planning/milestones/v2.0-phases/**` and `.planning/milestones/v2.0-MILESTONE-AUDIT.md`.

### Pitfall 4: Wrong module for the new test-scope dependency
**What goes wrong:** `dm-jdbc` gets added to `data-generator-service/pom.xml` (already has it, non-test scope) instead of `data-generator-calcite/pom.xml`, so the IT class fails to compile/load the driver at test time.
**Why it happens:** `data-generator-service` is the module that ships the driver for the packaged app; it's easy to assume "the driver dependency" lives there.
**How to avoid:** `ChunkedPipelineDamengUpsertIT` lives under `data-generator-calcite/src/test/java`, which currently has **no** `dm-jdbc` dependency at all (verified via pom inspection) — the new `<dependency>` block belongs in `data-generator-calcite/pom.xml`'s `<dependencies>` at `<scope>test</scope>`, alongside the existing `mysql-connector-j`/`postgresql`/`clickhouse-jdbc` test dependencies.
**Warning signs:** `ClassNotFoundException: dm.jdbc.driver.DmDriver` when running the IT with the flag on.

### Pitfall 5: Doc drift on the existing test-class name reference
**What goes wrong:** `docs/template-v2-jdbc-sink-guide.md` line 133 already says "Verified by `JdbcSinkSqlBuilderDamengMergeTests`" — but no such class exists; the real unit test is `JdbcSinkSqlBuilderTests.buildsDamengMergeInto()`. If the new Dameng live-IT doc section is added without noticing this, the doc's internal cross-reference stays wrong.
**Why it happens:** The class was likely renamed/consolidated after the doc was written and the doc wasn't updated.
**How to avoid:** When extending the Dameng docs section (D-14), correct the class-name reference to `JdbcSinkSqlBuilderTests` (method `buildsDamengMergeInto`) while adding the new live-IT subsection — small, in-scope surgical fix since it's the same doc section being touched anyway.
**Warning signs:** `rg -n "JdbcSinkSqlBuilderDamengMergeTests" docs/` still matches after the phase.

## Runtime State Inventory

> Not applicable — this is not a rename/refactor/migration phase. No stored data, live service config, OS-registered state, secrets, or build artifacts carry names that change in this phase. All work is additive (new test dependency, replaced placeholder method body, new docs section, new script, new VALIDATION files).

## Code Examples

### Reference: how PG/MySQL upsert ITs already call the shared helper
```java
// Source: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresUpsertTests.java
@Test
void chunkedUpsertReRunIsIdempotent() {
    UpsertParitySupport.assertUpsertIdempotent(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword(),
            "org.postgresql.Driver",
            "postgres");
}
```

### Reference: dialect key mapping already treats Dameng as PostgreSQL-style upsert accounting
```java
// Source: data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java (lines 133-138)
private static boolean isPostgresStyleUpsertDialect(String dialect) {
    return switch (dialect) {
        case "postgres", "postgresql", "kingbase", "highgo", "dameng" -> true;
        default -> false;
    };
}
```

### Reference: Dameng driver class + JDBC URL shape already documented in the preset catalog
```java
// Source: data-generator-service/src/main/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalog.java (line 28-31)
preset(
        "datasources.driver.dm8",
        "dm.jdbc.driver.DmDriver",
        List.of(),
        "jdbc:dm://localhost:5236?schema=YOUR_SCHEMA")
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|---------------|--------|
| `ChunkedPipelineDamengUpsertIT` = placeholder `Assumptions.abort(...)` regardless of flag | Real JDBC wiring, opt-in PASS, hard-FAIL on misconfig | This phase (13) | Operators with a real DM host finally get a genuine green signal; operators without one see documented enable-path only (D-05) |
| `07-VALIDATION.md` exists with `nyquist_compliant: false`; 07.1/08 have no VALIDATION.md | All three backfilled to `nyquist_compliant: true` from existing VERIFICATION evidence | This phase (13) | `v2.0-MILESTONE-AUDIT.md` Nyquist table moves from `partial` overall to fully compliant for 06–10 (Phase 12 stays out of scope per D-10) |

**Deprecated/outdated:** none — no library or framework version changes in this phase.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|----------------|
| A1 | `-Ddm.it` system property mirrors for `DG_DM_JDBC_URL`/`DG_DM_USER`/`DG_DM_PASSWORD` are not required (env-only is sufficient) since CONTEXT.md explicitly marks this as Claude's Discretion and D-07 says "prefer env-first" | Standard Stack / Pattern 1 | LOW — planner can add `-Ddm.url` etc. as an alias later without breaking the env-first contract; explicitly flagged as discretion, not a locked decision |
| A2 | No Dameng host is reachable in this development/CI sandbox (`DG_DM_JDBC_URL` unset, confirmed via env probe) so the wired IT cannot be exercised against a real live PASS during this research/planning cycle | Environment Availability | LOW — consistent with D-05's explicit "if no host available, done criteria remain honest" bar; the planner should not block phase completion on obtaining a live host |

**If this table is empty:** N/A — two low-risk assumptions logged above, both explicitly anticipated by CONTEXT.md decisions.

## Open Questions (RESOLVED)

1. **Exact verify script filename** — RESOLVED
   - What we know: D-15 says "match existing `verify-phase*-uat-*.ps1` style", name at planner discretion; this phase is numbered 13 but addresses DIAL-01 (no single "uat" theme like prior numbered phases)
   - What's unclear: Whether to name it `verify-phase13-uat-dameng-live.ps1` (phase-numbered, consistent with 06-11) or `verify-dameng-live-it.ps1` (capability-named, since this phase also does unrelated Nyquist docs work)
   - Recommendation: Planner picks `verify-phase13-uat-dameng-live.ps1` for consistency with the unbroken `verify-phase{N}-uat-*.ps1` naming convention used by every prior numbered phase (06, 07, 08, 09, 11)

2. **`-Ddm.it` Surefire property mirrors for URL/user/password** — RESOLVED
   - What we know: `DamengTestSupport` already reads both `-Ddm.it` and `DG_DM_IT` for the boolean flag; D-07 explicitly defers the question of whether URL/user/password need Surefire `-D` mirrors to the planner
   - What's unclear: Whether any CI/local workflow needs `-D` system-property overrides (harder to pass via PowerShell without quoting pain) versus env-only being sufficient for this phase's opt-in local/CI use
   - Recommendation: Env-only (`DG_DM_JDBC_URL`/`DG_DM_USER`/`DG_DM_PASSWORD`) is sufficient — matches D-06 "external JDBC URL/env only" framing and avoids `-D` quoting issues already flagged in `AGENTS.md` ("Quote `-Dsurefire.argLine` / `-Ddm.it=true` on PowerShell")

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 | Building/running the IT | ✓ | OpenJDK 25.0.1 | — |
| Maven wrapper (`mvnw-jdk25.ps1`) | Running Maven slices | ✓ | present at repo root | — |
| `dm-jdbc` 1.8 driver artifact | Compiling/loading `ChunkedPipelineDamengUpsertIT` | ✓ (already resolvable via internal Nexus / local repo — used by `data-generator-service` today) | 1.8 (`${dm.version}`) | — |
| Real Dameng JDBC host (`DG_DM_JDBC_URL` reachable) | Exercising a genuine live PASS | ✗ (unset in this sandbox) | — | Documented enable path only (D-05) — phase done criteria do not require a live host in this environment |
| Podman/Docker | Not required — D-09 defers Testcontainers/licensed-image Dameng | n/a | — | — |

**Missing dependencies with no fallback:**
- None — the only "missing" dependency (a reachable Dameng host) has an explicit documented fallback per D-05.

**Missing dependencies with fallback:**
- Real Dameng host: fallback is "documented enable path + MERGE-unit default CI bar" (D-05); this is the expected/accepted phase outcome when no host is available, not a blocker.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Maven Surefire) — `data-generator-calcite` module; no new framework |
| Config file | none — no `application-*.yaml` needed (plain JUnit IT, not `@SpringBootTest`) |
| Quick run command | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=JdbcSinkSqlBuilderTests -Dsurefire.failIfNoSpecifiedTests=false -q` (unit MERGE SQL proof, always runs, no flag needed) |
| Full suite command | `powershell -NoProfile -File scripts/verify-phase13-uat-dameng-live.ps1` (requires `DG_DM_IT=true` + `DG_DM_JDBC_URL`/`DG_DM_USER`/`DG_DM_PASSWORD` set; exits 1 with usage otherwise per D-16) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DIAL-01 | MERGE SQL unit proof always green in default CI (unchanged) | unit | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=JdbcSinkSqlBuilderTests -Dsurefire.failIfNoSpecifiedTests=false -q` | ✅ |
| DIAL-01 | Opt-in live IT hard-fails when flag on + misconfigured | IT (negative case) | `$env:DG_DM_IT='true'; .\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=ChunkedPipelineDamengUpsertIT -Dsurefire.failIfNoSpecifiedTests=false` (with `DG_DM_JDBC_URL` unset — expect BUILD FAILURE, not BUILD SUCCESS-with-skip) | ❌ Wave 0 (placeholder body must be replaced first) |
| DIAL-01 | Opt-in live IT PASSes chunked upsert idempotency when a real host is configured | IT | same command with real `DG_DM_JDBC_URL`/`DG_DM_USER`/`DG_DM_PASSWORD` set | ❌ Wave 0 — cannot be exercised in this sandbox (no host); documented enable path is the honest deliverable per D-05 |
| DIAL-01 | UAT wrapper exits non-zero when config missing | script | `powershell -NoProfile -File scripts/verify-phase13-uat-dameng-live.ps1` with no env set — expect exit code 1 | ❌ Wave 0 |
| DIAL-02 | 07/07.1/08 VALIDATION.md exist with `nyquist_compliant: true` derived from existing VERIFICATION evidence | docs check | `rg -n "nyquist_compliant: true" .planning/milestones/v2.0-phases/07-datasource-governance-hot-reload/07-VALIDATION.md .planning/milestones/v2.0-phases/07.1-close-gap-ds-03-jdbc-snapshot-routing-on-execute-path/07.1-VALIDATION.md .planning/milestones/v2.0-phases/08-rw-streaming-upsert/08-VALIDATION.md` | ❌ Wave 0 (07.1/08 files don't exist yet; 07 currently says `false`) |
| DIAL-02 | Milestone audit Nyquist table synced | docs check | `rg -n "COMPLIANT" .planning/milestones/v2.0-MILESTONE-AUDIT.md` (expect 07/07.1/08 rows now COMPLIANT, Phase 12 untouched — D-10) | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** Quick run command (`JdbcSinkSqlBuilderTests`, ~unit-speed) for DIAL-01 code tasks; `rg -n` doc-check commands for DIAL-02 doc tasks
- **Per wave merge:** `powershell -NoProfile -File scripts/verify-phase13-uat-dameng-live.ps1` (expect graceful non-zero exit without a host — this itself is the "PASS" signal for D-16's negative-path requirement) plus a Nyquist frontmatter grep across all three backfilled files
- **Phase gate:** Full existing `data-generator-calcite` module test suite green (`.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -q`, excluding the still-gated Dameng/PG/MySQL/Kingbase Docker-dependent ITs which already skip without Docker) before `/gsd-verify-work`; no `verify-harness.ps1` P0 changes expected/allowed

### Wave 0 Gaps
- [ ] `data-generator-calcite/pom.xml` — add `com.dameng:dm-jdbc` test-scope dependency (prerequisite for the IT to compile)
- [ ] `ChunkedPipelineDamengUpsertIT.java` — replace placeholder body (currently the only gap; no shared fixtures needed beyond existing `UpsertParitySupport`)
- [ ] `scripts/verify-phase13-uat-dameng-live.ps1` — new script (Wave 0 gap; no existing Dameng-specific verify script exists)
- [ ] `.planning/milestones/v2.0-phases/07.1-close-gap-ds-03-jdbc-snapshot-routing-on-execute-path/07.1-VALIDATION.md` — new file (missing entirely today)
- [ ] `.planning/milestones/v2.0-phases/08-rw-streaming-upsert/08-VALIDATION.md` — new file (missing entirely today)

*(07-VALIDATION.md and the milestone audit file already exist and only need in-place edits — not a "gap" in the sense of missing infrastructure.)*

## Security Domain

> `security_enforcement` not explicitly disabled in `.planning/config.json`; however this phase touches no authentication, session, access-control, or cryptography surfaces — it is test-infrastructure wiring (opt-in JDBC IT) and documentation. The one security-relevant concern (credential handling) is called out explicitly by CONTEXT D-08 and covered below instead of the standard ASVS table.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | not touched — opt-in test infra only |
| V3 Session Management | no | not touched |
| V4 Access Control | no | not touched |
| V5 Input Validation | no | env vars are read by a test class, not attacker-controlled input |
| V6 Cryptography | no | not touched |

### Known Threat Patterns for this phase

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Committing real Dameng credentials into `DG_DM_PASSWORD` env docs/scripts/CI config | Information Disclosure | D-08: plaintext env is acceptable **only** for local/opt-in developer use; docs must explicitly warn "never commit secrets" (mirrors existing repo pattern of no `.env` files in git per AGENTS.md); do not add a sample `.env.dameng` file with a real-looking password |
| Live IT error message leaking JDBC URL/credentials in Surefire output on hard-FAIL | Information Disclosure | Existing `UpsertParitySupport.withDialectSqlHint` already avoids echoing credentials; the new `requireEnv` failure message should name the missing env var, not its value — never log/print the actual `DG_DM_PASSWORD` value |

## Sources

### Primary (HIGH confidence)
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/DamengTestSupport.java` — existing opt-in gate, confirmed unchanged in this phase's Wave 0
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelineDamengUpsertIT.java` — placeholder to replace
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/UpsertParitySupport.java` — reused helper, full source read
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ChunkedPipelinePostgresUpsertTests.java` / `ChunkedPipelineMySqlUpsertTests.java` — sibling IT precedent
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java` / `JdbcSinkSqlBuilder.java` — confirmed `"dameng"` dialect handling
- `data-generator-service/src/main/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalog.java` — confirmed driver class + JDBC URL shape
- `data-generator-service/src/test/java/org/gensokyo/data/ai/runtime/OllamaAiRuntimeBridgeLiveIT.java` — confirmed soft-skip anti-pattern precedent (to avoid)
- root `pom.xml` (lines 61, 225-229) + `data-generator-writer-database/pom.xml` — confirmed `dm-jdbc` version management and existing scoping precedent
- `data-generator-calcite/pom.xml` — confirmed no existing `dm-jdbc` dependency
- `scripts/verify-phase9-uat-jdbc-dialect.ps1`, `scripts/verify-ai-p2.ps1`, `scripts/lib/repo-maven.ps1` — verify-script pattern precedents
- `.planning/milestones/v2.0-phases/07-datasource-governance-hot-reload/07-VALIDATION.md`, `.planning/milestones/v2.0-phases/09-jdbc-dialect-expansion/09-VALIDATION.md` — VALIDATION shape precedents (partial vs compliant)
- `.planning/milestones/v2.0-phases/07-datasource-governance-hot-reload/07-VERIFICATION.md`, `07.1-.../07.1-VERIFICATION.md`, `08-rw-streaming-upsert/08-VERIFICATION.md` — source evidence for VALIDATION backfill transcription
- `.planning/milestones/v2.0-MILESTONE-AUDIT.md` — Nyquist table sync target
- `.planning/test-matrix.yaml` (`v2-dialect-dameng` row) — confirmed P0 stays unit-only, IT explicitly not linked
- `docs/template-v2-jdbc-sink-guide.md` — existing Dameng docs section to extend; confirmed stale class-name reference (Pitfall 5)
- `.planning/research/PITFALLS.md`, `STACK.md`, `FEATURES.md` — v2.1 Dameng/Nyquist framing (Pitfalls 3, 8, 9; Stack "If Dameng live IT" pattern)
- `.planning/REQUIREMENTS.md`, `.planning/ROADMAP.md` (Phase 13 section), `.planning/STATE.md`, `.planning/PROJECT.md` — requirement/roadmap/state context
- Environment probe (this session): JDK 25.0.1 confirmed, `mvnw-jdk25.ps1` present, `DG_DM_JDBC_URL`/`DG_DM_IT` confirmed unset in sandbox

### Secondary (MEDIUM confidence)
- none required — all claims verified directly against codebase/config in this session

### Tertiary (LOW confidence)
- none

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new libraries; `dm-jdbc` version/scope confirmed directly from root pom and sibling module
- Architecture: HIGH — every pattern has a direct, read-verified sibling implementation in the same package
- Pitfalls: HIGH — each pitfall is grounded in either a confirmed in-tree anti-pattern (`OllamaAiRuntimeBridgeLiveIT`) or an explicit CONTEXT/PITFALLS.md decision

**Research date:** 2026-07-28
**Valid until:** 30 days (stable brownfield codebase; no fast-moving external dependency in this phase)
