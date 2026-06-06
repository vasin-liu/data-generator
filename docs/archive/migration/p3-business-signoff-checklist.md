# P3 business sign-off checklist

Use this checklist with the migration REST APIs and `docs/migration/scenario-inventory.yaml` to close **P3 — Business** gates in `docs/migration/retirement-readiness.md`.

## APIs

| Step | API |
|------|-----|
| Family rollup | `GET /template/migration/signoff-status` |
| Work queue | `GET /template/migration/backlog?filter=pending_signoff` |
| Record approval | `POST /template/migration/inventory/{inventoryId}/signoff` |
| Promote (after sign-off) | `POST /template/migration/promote/{templateId}` |

PowerShell:

```powershell
.\scripts\migration-staging.ps1 -Action signoff-status
.\scripts\migration-staging.ps1 -Action backlog -Filter pending_signoff
.\scripts\migration-staging.ps1 -Action signoff -InventoryId regression-v1-constant-five-rows -ApprovedBy "owner@example.com"
```

## Wave 1 — `synthetic` scenario family

| Inventory id | migrationClass | Compare report | Business sign-off | Notes |
|--------------|----------------|----------------|-------------------|-------|
| regression-v1-constant-five-rows | EXACT | sample report | [ ] | Number iterator baseline |
| regression-v1-iterator-simple | ADAPTED | sample report | [ ] | SCRIPT field — review ADAPTED boundary |
| wave1-synthetic-number-1to5 | EXACT | linked | [ ] | Alias of number family |
| wave1-synthetic-iterator-draft | ADAPTED | linked | [ ] | Iterator draft path |

**Family gate:** `GET /migration/signoff-status` → `synthetic.familySignoffComplete == true` when every **ready** row has `businessSignoffApproved: true` in inventory YAML.

## Wave 2 — `multi_source` scenario family

| Inventory id | migrationClass | Compare report | Business sign-off | Notes |
|--------------|----------------|----------------|-------------------|-------|
| regression-v1-query-lookup | ADAPTED | sample report | [ ] | JDBC field reader |
| wave2-jdbc-single-reader | ADAPTED | linked | [ ] | |
| wave2-jdbc-chunked-policy | ADAPTED | linked | [ ] | CHUNKED policy on migrate |

## Compatibility-only (documented, not promoted)

| Inventory id | Action |
|--------------|--------|
| regression-v1-with-pause | Remain on V1 — see `docs/migration/compatibility-only-templates.md` |

Record acceptance in inventory `notes` and check retirement-readiness P3 sub-item.

## Production DB templates (`db-{id}`)

1. `POST /template/migration/inventory/refresh`
2. `POST /template/migration/compare/batch` (or per-id compare on staging)
3. For each `filter=ready` row: business sign-off → staging promote → production promote per change window
4. Set `migrationClass` to EXACT or ADAPTED only after dual-run evidence

## Sign-off record in inventory YAML

After `POST .../signoff`, the row includes:

```yaml
businessSignoffApproved: true
businessSignoffBy: owner@example.com
businessSignoffAt: 2026-05-20T12:00:00Z
```

Commit the updated `scenario-inventory.yaml` with the sign-off audit trail.
