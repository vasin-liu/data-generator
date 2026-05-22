# Compatibility-only V1 templates

Templates in this category should **remain on V1** (or use a future V2 orchestration layer) until pause, shared state, logging side effects, or JavaScript script stages have an explicit V2 design.

**Census (staging-free):** [`reports/builtin-orchestration-census.md`](reports/builtin-orchestration-census.md) — 2/59 builtins `COMPATIBILITY_ONLY` (2026-05-22). **Boundary summary:** [`orchestration-retirement-boundary.md`](orchestration-retirement-boundary.md).

## Inventory entries (compatibility-only)

| Inventory id | Name | Scenario family | Blockers |
|--------------|------|-----------------|----------|
| regression-v1-with-pause | regression-with-pause | orchestration_legacy | PAUSE orchestration on iterator |
| regression-v1-iterator-simple | regression-iterator-simple | synthetic | Field SCRIPT not in SQL (review; not blocked for draft) |

> **Note:** `regression-v1-iterator-simple` is **ADAPTED** for Wave 1 draft migration but retains SCRIPT field logic on V1 until rewritten. It is listed here because operators often treat SCRIPT-heavy templates as compatibility-only during dual-run review.

**SpEL-migratable SCRIPT (B 族):** plain / `SPEL` field scripts are **not** compatibility-only; they should receive `SpelTransformVO` in V2 drafts per `docs/superpowers/specs/2026-05-21-script-spel-draft-migration-design.md`.

## Signals (from `V1TemplateMigrationAnalyzer`)

- JavaScript (`JAVASCRIPT`) script stages
- `PAUSE`, `SHARED`, or `LOG` orchestration stages
- Recommended path: `compatibility_only`

## Actions

1. Do **not** promote to production V2 without explicit sign-off.
2. Record `migrationClass: COMPATIBILITY_ONLY` in `docs/migration/scenario-inventory.yaml`.
3. Track in retirement readiness P3 gates until orchestration parity exists or templates are rewritten.

See also: `docs/migration/retirement-readiness.md`, `docs/calcite-v1-parity-scorecard.md`.
