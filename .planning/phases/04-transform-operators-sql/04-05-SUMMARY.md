# Plan 04-05 Summary — Docs + Harness Fixtures

**Requirements:** XFORM-04, XFORM-06 (D-02, D-03, D-04, D-12, D-13)
**Wave:** 3
**Commit:** `3e7c580`
**Status:** Complete — all tests green (3/3)

## Accomplishments

Documented the new operators and proved them end-to-end in the embedded test harness.

- **`docs/transform-operators.md`** (XFORM-04): operator reference for `json`/`mask`/`lookup` with the `type` discriminator, every config field (type + required/optional + meaning, matching the 04-01 VOs), and a YAML example each. A **Schema & version notes** section states the change is additive with no breaking template-version bump (D-13). An **Internal SQL functions** note documents `V2_JSON_EXTRACT(json, path)` as internal-only / uncataloged under the `V2_` prefix (D-12). Cross-links the `/api/console/transforms` catalog from 04-03.
- **Three embedded sample templates** under `data-generator-test-fixtures/src/main/resources/fixtures/templates/` (`transform-json`, `transform-mask`, `transform-lookup`) using `inline_rows` sources only — no credentials.
- **Three `Fixture*ExampleTests`** mirroring `FixtureTransformSqlExampleTests`: each builds a `TemplateV2VO` with inline-rows sources + the operator under test + console sink, runs it through `TemplateV2Runner` + a hand-built `TemplateV2RuntimeRegistry`, and asserts operator-specific output:
  - JSON: flattened `addr.city` / `addr.zip` columns exist.
  - Mask: each column masked per strategy AND raw values absent (PII-safe regression).
  - Lookup: each row gains the projected `dept_name` by key.
- **Three matrix rows** in `.planning/test-matrix.yaml` (`transform-json`/`transform-mask`/`transform-lookup`, status `covered`) with `linked_tests` populated (XFORM-06), mirroring the `transform-sql-basic` row shape.

## Design notes

- Fixtures use `InlineRowsSourceVO` rather than H2 seeding (the SQL analog used H2) because the new operators are row-local and need no SQL datasource — this keeps the tests fully in-memory and dependency-free. The lookup test declares two inline sources (`input` + `ref`); both are loaded into the execution context before the (single) lookup transform runs, so the reference table is visible.
- Tests build templates programmatically (mirroring the SQL example) and assert the YAML fixture loads, keeping matrix linkage explicit without a YAML-parse dependency.

## Files

Created: `docs/transform-operators.md`, three `transform-*.yaml` fixtures, three `FixtureTransform*ExampleTests.java`
Modified: `.planning/test-matrix.yaml`

## Verification

`.\mvnw-jdk25.ps1 -pl data-generator-test-fixtures -am test -Dtest=FixtureTransformJsonExampleTests,FixtureTransformMaskExampleTests,FixtureTransformLookupExampleTests` → **BUILD SUCCESS**, Tests run: 3, Failures: 0, Errors: 0.
