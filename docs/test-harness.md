# Test harness

The unified harness entry point runs matrix-linked Maven tests on JDK 25 and writes a machine-readable coverage summary. Playwright console smoke is opt-in.

## Quick start

```powershell
# Embedded fast path (default): linked Maven tests only
.\scripts\verify-harness.ps1
```

Output: `target/test-matrix-summary.json`

## Flags

| Flag | Purpose |
|------|---------|
| `-IncludeE2e` | Run Playwright specs referenced in matrix `linked_tests` |
| `-UsePodman` | With `-IncludeE2e`, run via `scripts/e2e-podman.ps1` (default) |
| `-UseLocalService` | With `-IncludeE2e`, run against a local service base URL |
| `-MatrixFile` | Override matrix path (default `.planning/test-matrix.yaml`) |

Exit code **1** when a linked Maven test fails or when the **P0 regression gate** fails (see below). `pending` P1/P2 rows never fail the harness on their own.

## Priority tiers (COV-01)

Each matrix row carries a `tier` field (`P0`, `P1`, or `P2`):

| Tier | Meaning |
|------|---------|
| **P0** | Must be 100% green to merge — enforced by the P0 regression gate in CI |
| **P1** | Core-adjacent coverage tracked in the summary; non-blocking this phase |
| **P2** | Best-effort backlog rows tracked in the summary; non-blocking |

**COV-01 completion target:** P0 must be 100% green to merge; P1/P2 are tracked with no hard percentage this phase.

**P0 rows (7):** `calcite-scenario-v2`, `udf-sql`, `udf-script`, `udf-java-plugin`, `transform-json`, `transform-mask`, `transform-lookup`.

## Matrix maintenance

1. Edit `.planning/test-matrix.yaml` (source of truth).
2. Regenerate the human-readable doc:

```powershell
.\scripts\generate-test-matrix-doc.ps1
```

3. Optional draft seeder from codebase maps:

```powershell
.\scripts\generate-test-matrix-draft.ps1 -Force
```

## Fixture authoring

Reusable Template V2 scenarios live in `data-generator-test-fixtures`:

- `FixtureTemplates.load("reader-jdbc-basic")` — classpath YAML
- `H2Seed.apply(dataSource, sql)` — in-memory H2 seed scripts under `fixtures/sql/`

All JDBC URLs must use `jdbc:h2:mem:` (no production credentials).

## Reading `target/test-matrix-summary.json`

| Row status | Meaning |
|------------|---------|
| `covered` | All linked tests passed |
| `partial` | Some linked tests passed |
| `pending` | No linked tests or none executed / all failed without coverage claim |
| `skipped-conditional` | Linked tests skipped (conditional gates) |

Fields: `generatedAt`, optional `gitCommit`, `totals{covered,partial,pending,skipped}`, `rows[{id,status,linkedResults[]}]`, and `p0{total,green,pass,rows[{id,status,green}]}`.

The `p0` block is the machine-readable P0 rollup consumed by the merge gate:

| Field | Meaning |
|-------|---------|
| `p0.total` | Count of rows with `tier: P0` |
| `p0.green` | Count of P0 rows whose computed status is `covered` |
| `p0.pass` | `true` only when every P0 row is green (`status == covered`) |
| `p0.rows[]` | Per-P0-row `{id, status, green}` detail |
