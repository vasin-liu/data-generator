# 05-01 Summary — P0/P1/P2 tiers + P0 rollup + scenario gap

**Status:** Complete  
**Requirements:** COV-01, COV-02

## Delivered

- Added additive `tier` field (P0/P1/P2) to all `.planning/test-matrix.yaml` rows; exactly seven P0 rows per D-03.
- Extended `New-TestMatrixSummary` to emit `p0{total,green,pass,rows[]}` in `target/test-matrix-summary.json`.
- Closed `calcite-scenario-v2` P0 gap: `V2ScenarioTemplateIT` green; row status updated to `covered`.
- Documented tiers, COV-01 target, and P0 rollup in `docs/test-harness.md`.
- Added `tier` column to `scripts/generate-test-matrix-doc.ps1` output.

## Verification

- `V2ScenarioTemplateIT` passes on JDK 25.
- P0 parser check: 7 P0 rows, all rows have `tier`.
