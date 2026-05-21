# Staging readiness checklist (M2 unlock)

Complete before first staging migration sweep. Does **not** block `feature-4.0` merge.

## Environment

| Item | Owner | Done |
|------|-------|------|
| Staging `data-generator-service` URL | | [ ] |
| Metadata DB reachable (same schema as prod templates) | | [ ] |
| `docs/migration/scenario-inventory.yaml` writable path configured | | [ ] |

## Data sources

| dataSourceId (staging) | Matches prod? | Smoke query OK? |
|------------------------|---------------|-----------------|
| | | [ ] |

## Catalog

| Metric | Value |
|--------|-------|
| Approximate V1 template count in DB | |
| Templates marked COMPATIBILITY_ONLY (expected) | |
| Target batch compare cap (default 50) | |

## Execution (M2 — after checklist complete)

1. `scripts/migration-staging.ps1 -Action refresh`
2. `scripts/migration-staging.ps1 -Action batch-compare`
3. Per-template promote only when classification ∈ {EXACT, ADAPTED, APPROXIMATE}
4. `GET /template/migration/signoff-status` → both families complete
5. Staging trial: `pci.data.generator.v1-execution.enabled: false`

See `docs/migration/staging-runbook.md` and `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md`.
