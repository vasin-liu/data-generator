# Merge Request: feature-4.0

## Summary

This branch delivers two coordinated capabilities for Template V2 on `feature-4.0`:

1. **JDBC chunked execution** — row-local `CHUNKED` pipeline for large JDBC read → DB/Kafka/ES export, with execution-shape classification, broadcast-join support, scale limits, and service-side validation at template save.
2. **V1 migration workbench** — analyze / draft / compare / promote APIs, scenario inventory (DB + regression fixtures), dual-run classification, and markdown reports under `docs/migration/reports/`.

## Why

- Roadmap priority **D (big data volume)** → sub-scenario **A (large JDBC export)** required streaming/chunking without loading full result sets in memory.
- Product gap **A (V1 migration + dual-run acceptance)** needed an operator path before retiring V1 templates.

## Notable changes

| Area | Highlights |
|------|------------|
| `data-generator-calcite` | `ChunkedPipeline`, `ChunkedQueryRowSource`, `ExecutionShapeClassifier`, `EffectiveExecutionPolicy`, sink `writeBatch`, `broadcastMaxRows` |
| `data-generator-service` | `org.gensokyo.data.template.migration.*`, REST under `/template/migration/*` |
| Docs | `docs/template-v2-jdbc-chunked-execution-guide.md`, `docs/migration/*`, `docs/migration/workbench-usage.md` |

## API (migration)

| Method | Path |
|--------|------|
| GET | `/template/migration/analyze/{id}` |
| POST | `/template/migration/draft/{id}` |
| POST | `/template/migration/compare/{id}` |
| POST | `/template/migration/promote/{id}` |
| GET | `/template/migration/inventory` |
| POST | `/template/migration/inventory/refresh` |

See `docs/migration/workbench-usage.md`.

## Test plan

- [ ] `.\mvnw-jdk25.ps1 "-pl" "data-generator-service,data-generator-calcite" "-am" "test"`
- [ ] Review sample reports in `docs/migration/reports/sample-*.md`
- [ ] On staging: pick one production V1 JDBC export template → draft → compare → review classification
- [ ] MySQL: confirm datasource supports cursor fetch per `docs/template-v2-jdbc-chunked-execution-guide.md`
- [ ] Do **not** promote `COMPATIBILITY_ONLY` templates (pause, shared, legacy script) — see `docs/migration/compatibility-only-templates.md`

## Out of scope (follow-ups)

- Full explain/preview control plane (roadmap P0, deferred)
- Official non-SQL transformer (blocks Wave 3 script-heavy migrations)
- Production-wide inventory export (manual refresh via `MigrationInventoryService.refreshFromRepository`)

## Commits

25 commits on `feature-4.0` from JDBC policy resolver through migration workbench and workbench usage doc.
