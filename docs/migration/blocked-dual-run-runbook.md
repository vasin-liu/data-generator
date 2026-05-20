# BLOCKED dual-run remediation runbook

When `POST /template/migration/compare/{id}` or batch compare classifies a template as **BLOCKED** (`sampleMatchRate < 0.95` or row-count mismatch with blockers), use this checklist before promote.

## 1. Read the report

Open `lastCompareReportPath` from `GET /template/migration/inventory` (or the API response `reportPath`).

Note:

- V1 vs V2 row counts
- Sample match rate
- Warnings and analyzer blockers (pause, script, unsupported source)

## 2. Classify the failure mode

| Symptom | Likely cause | Action |
|---------|----------------|--------|
| V2 row count = 0, V1 > 0 | CHUNKED compare without in-memory samples, or missing JDBC datasource | Re-run compare with in-memory policy (service forces IN_MEMORY for compare); verify datasource id exists |
| Match rate low, counts equal | Column naming (`iterator.id` vs `id`), type coercion | Inspect sample rows in report; adjust V2 SQL projection or compare `keyColumns` in request body |
| Blockers in warnings | PAUSE, legacy script, unsupported iterator | See `compatibility-only-templates.md`; do not promote — mark COMPATIBILITY_ONLY |
| V2 draft has no sources | Draft not built | `POST /template/migration/draft/{id}` then compare again |
| JDBC timeout / connection | Staging datasource down | Fix datasource registry; retry on staging only |

## 3. Fix and re-compare

```bash
# Refresh inventory from DB catalog (adds new db-* rows)
curl -s -X POST "http://localhost:8080/template/migration/inventory/refresh"

# Rebuild draft if V2 side changed
curl -s -X POST "http://localhost:8080/template/migration/draft/{id}"

# Single template
curl -s -X POST "http://localhost:8080/template/migration/compare/{id}"

# Catalog sweep (cap 50 db templates by default)
curl -s -X POST "http://localhost:8080/template/migration/compare/batch" \
  -H "Content-Type: application/json" \
  -d '{"refreshInventoryFirst":true,"maxTemplates":50}'
```

## 4. When to accept anyway

Only with explicit review:

- **ADAPTED** or **APPROXIMATE** — document the semantic delta in inventory `notes`
- Never promote **COMPATIBILITY_ONLY** or **BLOCKED** without engineering sign-off

## 5. Nightly catalog (optional)

Enable in `application.yaml` only on environments that can run dual-load safely:

```yaml
pci:
  data:
    generator:
      migration:
        batch-compare:
          scheduled-enabled: true
          cron: "0 0 2 * * *"
          max-templates: 100
```

Default is `scheduled-enabled: false`.
