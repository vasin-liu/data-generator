# M2 handoff — production catalog census (post–path B)

Use when staging environment is ready. Path **B** (builtin classpath) is complete; this document is the **M2** counterpart for `db-{id}` templates.

## Prerequisites

- [ ] `docs/migration/staging-readiness-checklist.md` complete
- [ ] Team aligned on W3 policy **S1** ([`orchestration-retirement-boundary.md`](orchestration-retirement-boundary.md))

## Step 1 — Refresh inventory

```bash
curl -s -X POST "http://<staging-host>:9876/template/migration/inventory/refresh"
```

Or: `.\scripts\migration-staging.ps1 -Action refresh`

## Step 2 — Batch analyze / compare (capped)

```bash
curl -s -X POST "http://<staging-host>:9876/template/migration/compare/batch" \
  -H "Content-Type: application/json" \
  -d "{\"refreshInventoryFirst\":true,\"maxTemplates\":50}"
```

Record reports under `docs/migration/reports/` (or staging path configured in `MigrationReportWriter`).

## Step 3 — Compare to builtin baseline

| Builtin census (2026-05-22) | Expect in production |
|-----------------------------|----------------------|
| 59 classpath templates | DB count TBD |
| 2 `COMPATIBILITY_ONLY` (3%) | Likely low if PAUSE/LOG rare; investigate outliers |
| 43 `recommendedPath: spel` | Majority SCRIPT→SpEL migratable |

Export summary from `GET /template/migration/summary` and append to a new file:

`docs/migration/reports/production-migration-census.md` (create on first M2 run).

## Step 4 — Sign-off and promote

Only rows with:

- `migrationClass` ∈ {EXACT, ADAPTED, APPROXIMATE}
- `businessSignoffApproved: true`
- **Not** `COMPATIBILITY_ONLY` / `BLOCKED`

Per template: `staging-runbook.md` → sign-off → promote.

## Step 5 — P4 trial (staging only)

After W1/W2 family sign-off on **production** cohort:

1. `data.generator.v1-execution.enabled: false` on staging
2. Smoke V2 preview; confirm V1 run rejected
3. Document date in `wave-freeze-schedule.md`

## Do not promote (W3)

Templates with PAUSE / LOG / SHARED / JAVASCRIPT — same rules as builtin `demo/18`, `demo/27`. See [`compatibility-only-templates.md`](compatibility-only-templates.md).

## References

- [`staging-runbook.md`](staging-runbook.md)
- [`p3-business-signoff-checklist.md`](p3-business-signoff-checklist.md)
- [`reports/builtin-orchestration-census.md`](reports/builtin-orchestration-census.md)
