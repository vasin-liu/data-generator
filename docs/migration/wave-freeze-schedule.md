# Wave freeze schedule (proposal)

Proposed V1 authoring freeze dates for the retirement program. **Finalize dates after R0 staging** completes on the target environment.

## R0 staging evidence

| Template id | Scenario family | Classification | Compare report |
|-------------|-----------------|----------------|----------------|
| _pending_ | synthetic | _run staging workflow_ | _docs/migration/reports/_ |
| _pending_ | multi_source / JDBC | _run staging workflow_ | _docs/migration/reports/_ |

**Commands:** see `docs/migration/staging-runbook.md` and `scripts/migration-staging.ps1 -Action workflow`.

## Proposed freeze waves

| Wave | Scenario families | Proposed freeze (draft) | Gate |
|------|-------------------|-------------------------|------|
| **W1** | `synthetic` | T+14 days after R0 sign-off | P3 `synthetic` family sign-off complete |
| **W2** | `multi_source`, JDBC export | T+28 days after R0 | P3 `multi_source` sign-off + CHUNKED staging checklist |
| **W3** | `orchestration_legacy` | No freeze — compatibility-only bucket | Document only; no V2 promote |

_T+14 / T+28 are placeholders until product owner sets calendar from R0 results._

## Policy after each wave

- **W1:** No new V1 templates in `synthetic` family; existing V1 synthetic runs allowed until promoted or exempted.
- **W2:** No new V1 templates for JDBC / multi-source families.
- **Global (P4):** Set `pci.data.generator.v1-execution.enabled=false` when retirement-readiness P3/P4 metrics met.

## Owners

| Role | Owner |
|------|-------|
| Engineering | _team_ |
| Business sign-off | _product owner_ |

## Rollback

- Re-enable V1: `pci.data.generator.v1-execution.enabled=true`
- Revert promoted templates via DB restore or retained V1 yaml on `TemplatePO` (promote keeps V1 content per existing semantics)

## References

- `docs/superpowers/specs/2026-05-21-v1-retirement-alignment-design.md`
- `docs/migration/retirement-readiness.md`
