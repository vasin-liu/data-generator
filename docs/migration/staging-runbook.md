# Staging migration runbook

> **Milestone:** **M2 only** — not required to merge `feature-4.0`.  
> **M1 substitute:** CI tests and `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md` evidence table.  
> **Prerequisite:** Complete `docs/migration/staging-readiness-checklist.md` before first staging sweep.

Operator checklist for validating one production V1 template on a staging `data-generator-service` instance before promote.

**Base URL:** `http://<host>:9876/template` (adjust port and context path).

**Prerequisites**

- Service running with access to the same metadata DB and JDBC datasources as staging/production targets.
- For large JDBC exports: datasource URL includes MySQL `useCursorFetch=true` when the source is MySQL (see `docs/template-v2-jdbc-chunked-execution-guide.md`).
- Inventory file writable: `docs/migration/scenario-inventory.yaml` (or path configured in `MigrationInventoryService`).

## 1. Catalog health

```bash
curl -s "http://localhost:9876/template/migration/summary"
curl -s "http://localhost:9876/template/migration/inventory"
```

Review `readyToPromote`, `blocked`, and `compatibilityOnly` counts before sweeping the catalog.

Refresh DB templates into inventory:

```bash
curl -s -X POST "http://localhost:9876/template/migration/inventory/refresh"
```

Optional catalog dual-run (cap 50 templates):

```bash
curl -s -X POST "http://localhost:9876/template/migration/compare/batch" \
  -H "Content-Type: application/json" \
  -d "{\"refreshInventoryFirst\":true,\"maxTemplates\":50}"
```

## 2. Single-template workflow

Replace `{id}` with the persisted template id.

```bash
# Analyze blockers and recommended path
curl -s "http://localhost:9876/template/migration/analyze/{id}"

# Build V2 draft (not persisted)
curl -s -X POST "http://localhost:9876/template/migration/draft/{id}"

# Dual-run compare — writes report + updates inventory
curl -s -X POST "http://localhost:9876/template/migration/compare/{id}" \
  -H "Content-Type: application/json" \
  -d "{\"sampleSize\":500}"

# After human review of classification + planExplain + report markdown
curl -s -X POST "http://localhost:9876/template/migration/promote/{id}"
```

## 3. Acceptance gates

| Classification | Promote? |
|----------------|----------|
| `EXACT` | Yes, after spot-check report |
| `ADAPTED` / `APPROXIMATE` | Yes with documented notes in inventory |
| `BLOCKED` | No — follow `docs/migration/blocked-dual-run-runbook.md` |
| `COMPATIBILITY_ONLY` | No — remain on V1 |

## 4. PowerShell helper

From repo root (Windows):

```powershell
.\scripts\migration-staging.ps1 -BaseUrl "http://localhost:9876/template" -TemplateId 42 -Action workflow
```

Actions: `summary`, `refresh`, `analyze`, `draft`, `compare`, `promote`, `signoff`, `workflow` (analyze → draft → compare), `workflow-promote` (through signoff + promote when classification allows).

## 5. Sign-off record

After staging, update `docs/migration/scenario-inventory.yaml` notes for the template id and capture business sign-off in `docs/migration/retirement-readiness.md` (P3).
