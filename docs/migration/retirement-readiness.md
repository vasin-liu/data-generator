# V1 retirement readiness checklist

Honest gates for retiring V1 template execution in favor of Template V2 / Calcite. Inventory ids reference `docs/migration/scenario-inventory.yaml`.

## P1 — Technical

- [x] Migration scenario inventory maintained (`scenario-inventory.yaml`, ≥10 tracked templates)
- [x] Dual-run compare API (`POST /template/migration/compare/{templateId}`) with markdown reports under `docs/migration/reports/`
- [x] Classification rules (EXACT / ADAPTED / APPROXIMATE / BLOCKED / COMPATIBILITY_ONLY)
- [x] Unified draft migration (`POST /template/migration/draft/{templateId}`) — query-source + iterator
- [x] Promote workflow (`POST /template/migration/promote/{templateId}`)
- [x] CHUNKED execution policy suggested on single-source JDBC migrate (`executionPolicy.mode: CHUNKED`)
- [ ] Calcite explain / plan diff for operators (partial — compare sample match only)
- [ ] Full JDBC chunked parity on MySQL/Postgres production datasources

## P2 — Operational (partial)

- [x] Compare reports linked from inventory (`lastCompareReportPath`)
- [x] Runbook for failed dual-run (`BLOCKED`) remediation — `docs/migration/blocked-dual-run-runbook.md`
- [x] Batch / scheduled dual-run on DB catalog — `POST /template/migration/compare/batch`, optional `pci.data.generator.migration.batch-compare.scheduled-enabled`
- [ ] Operator UI for inventory + compare (REST only today)

## P3 — Business

- [ ] Sign-off per scenario family: `synthetic` (Wave 1)
- [ ] Sign-off per scenario family: `multi_source` (Wave 2)
- [ ] Compatibility-only templates documented and accepted (`docs/migration/compatibility-only-templates.md`)
  - [ ] `regression-v1-with-pause` — PAUSE retained on V1
- [ ] Production templates promoted with `migrationClass` EXACT or ADAPTED

## Evidence samples

| Inventory id | Report |
|--------------|--------|
| regression-v1-constant-five-rows | `docs/migration/reports/sample-regression-v1-constant-five-rows.md` |
| regression-v1-iterator-simple | `docs/migration/reports/sample-regression-v1-iterator-simple.md` |
| regression-v1-query-lookup | `docs/migration/reports/sample-regression-v1-query-lookup.md` |

When all P1 items and scenario-family P3 items are checked, V1 execution may be deprecated per team policy.
