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

Exit code **1** only when a linked Maven test fails. `pending` rows never fail the harness.

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

Fields: `generatedAt`, optional `gitCommit`, `totals{covered,partial,pending,skipped}`, `rows[{id,status,linkedResults[]}]`.
