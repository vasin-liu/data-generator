# Merge Request: feature-4.0

## Summary

This branch delivers coordinated capabilities for Template V2 on `feature-4.0`:

1. **JDBC chunked execution** — row-local `CHUNKED` pipeline for large JDBC read → DB/Kafka/ES export, with execution-shape classification, broadcast-join support, scale limits, and service-side validation at template save.
2. **V1 migration workbench** — analyze / draft / compare / promote APIs, scenario inventory (DB + regression fixtures), dual-run classification, sign-off, and markdown reports under `docs/migration/reports/`.
3. **V1 retirement capabilities** — control plane validate/explain/preview, `SpelTransformVO` row-level SpEL, **SCRIPT → SpEL migration draft** (`V1ScriptToSpelDraftConverter`, JDBC **compare** uses same SQL+SpEL chain), `pci.data.generator.v1-execution.enabled` gate, CI regression over built-in templates.
4. **Geospatial (Phases 1, 2B, 2C, 2D)** — synthetic `GEO` iterator, `GEOJSON`/`POSTGIS` V2 sources, Calcite SQL over geo rows, and `V2_GEO_*` in-memory functions. See `docs/geospatial-overview.md`.

## V1 retirement (merge vs M2)

This MR **delivers retirement capabilities**; it does **not** complete production retirement.

| Milestone | In this MR? | Evidence |
|-----------|-------------|----------|
| **M1** (no staging) | Yes | CI: `BuiltinClasspathTemplate*`, `MigrationWaveCohortSignoffTests`, `StagingSimulatedPromoteWorkflowTests`; spec: `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md` |
| **M2** (staging) | No | Real `db-{id}` promote, batch compare, `v1-execution.enabled=false` — post-merge ops (`docs/migration/staging-readiness-checklist.md`) |

**Do not merge-block on:** production promote checkbox, wave-freeze calendar dates, staging runbook execution.

## Why

- Roadmap priority **D (big data volume)** → sub-scenario **A (large JDBC export)** required streaming/chunking without loading full result sets in memory.
- Product gap **A (V1 migration + dual-run acceptance)** needed an operator path before retiring V1 templates.
- Constraint **A (V1 retire/freeze)** — wave-freeze and runtime disable deferred until staging (M2).

## Notable changes

| Area | Highlights |
|------|------------|
| `data-generator-calcite` | `ChunkedPipeline`, `ChunkedQueryRowSource`, `ExecutionShapeClassifier`, `SpelTransformFactory`, geo row sources |
| `data-generator-service` | `org.gensokyo.data.template.migration.*`, control plane `/template/v2/*`, migration REST |
| `data-generator-geo` | GeoJSON I/O, JTS predicates/buffer, synthetic generator |
| Docs | `docs/template-v2-jdbc-chunked-execution-guide.md`, `docs/migration/*`, `docs/geospatial-overview.md` |

## API (migration)

| Method | Path |
|--------|------|
| GET | `/template/migration/analyze/{id}` |
| POST | `/template/migration/draft/{id}` |
| POST | `/template/migration/compare/{id}` |
| POST | `/template/migration/promote/{id}` |
| GET | `/template/migration/inventory` |
| GET | `/template/migration/summary` |
| POST | `/template/migration/inventory/refresh` |
| POST | `/template/migration/compare/batch` |
| GET | `/template/migration/backlog` |
| GET | `/template/migration/signoff-status` |
| POST | `/template/migration/inventory/{inventoryId}/signoff` |

## API (control plane)

| Method | Path |
|--------|------|
| POST | `/template/v2/validate` |
| GET | `/template/v2/explain/{id}` |
| POST | `/template/v2/preview/{id}` |

| Config | `pci.data.generator.v1-execution.enabled` (default `true`; gates V1 task run when `false`) |

See `docs/migration/workbench-usage.md`.

## Embedded integration tests (calcite)

| Component | Tests |
|-----------|--------|
| H2 / JDBC | `ChunkedPipelineTests`, `ChunkedQueryRowSourceTests`, `TemplateControllerMigrationCompareTests`, `application-phase7-test.yaml` |
| MySQL / PostgreSQL CHUNKED | `ChunkedPipelineMySqlContainerTests`, `ChunkedPipelinePostgresContainerTests` (Testcontainers, requires Docker) |
| Kafka | `EmbeddedKafkaTestSupport`, `KafkaRowSinkAdapterEmbeddedTests`, `TemplateV2RunnerKafkaEmbeddedTests` |
| Elasticsearch | `EmbeddedElasticsearchHttpSupport`, `ElasticsearchRowSinkAdapterHttpEmbeddedTests`, `TemplateV2RunnerElasticsearchHttpEmbeddedTests` |
| Migration dual-run | `PipelineTemplateRunExecutor` + H2; `StagingSimulatedPromoteWorkflowTests` |
| Built-in templates | `BuiltinClasspathTemplateRegressionTests`, `BuiltinClasspathTemplateMigrationWorkflowTests` |

See `docs/testing-embedded-components.md`.

## Test plan

- [x] Full reactor test (2026-05-22): `.\mvnw-jdk25.ps1 test` — **BUILD SUCCESS**, 43/43 modules, ~5m 11s, 0 failures (incl. JDBC compare SpEL + `dependsOn` / lowercase row keys)
- [x] Full reactor test (2026-05-22 earlier): `.\mvnw-jdk25.ps1 test` — **BUILD SUCCESS**, 43/43 modules, ~4m 30s, 0 failures (incl. SCRIPT→SpEL draft + compare fixes)
- [x] Full reactor test (2026-05-21): `.\mvnw-jdk25.ps1 test` — **BUILD SUCCESS**, 43/43 modules, ~4m 12s, 0 failures
- [x] Full reactor test (2026-05-20): `.\mvnw-jdk25.ps1 test` — **BUILD SUCCESS**, 41/41 modules, ~6m 24s, 0 failures
- [x] Retirement M1 CI slice (2026-05-21): BuiltinClasspath*, MigrationWaveCohort*, StagingSimulatedPromote*, control plane, v1 flag, promote — **BUILD SUCCESS**, 25 tests, 0 failures
- [x] Staging unsigned promote guard (2026-05-21): `StagingSimulatedPromoteWorkflowTests#stagingWorkflowRejectsPromoteWithoutBusinessSignoffAfterCompare` — **BUILD SUCCESS**
- [x] Sample compare reports present under `docs/migration/reports/` (`sample-regression-v1-*.md`, classifications e.g. EXACT)
- [x] Retirement M1: CI simulated promote + cohort sign-off (deferred-ops spec)
- [ ] **M2** On staging: pick one production V1 JDBC export template → draft → compare → review classification
- [ ] **M2** MySQL: confirm datasource supports cursor fetch per `docs/template-v2-jdbc-chunked-execution-guide.md`
- [ ] **M2** Staging trial: `v1-execution.enabled=false` after cohort promote
- [ ] Do **not** promote `COMPATIBILITY_ONLY` templates (pause, shared, legacy script) — see `docs/migration/compatibility-only-templates.md`

## Geospatial test plan

- [x] `data-generator-geo`, `iterator-geo`, calcite geo tests (runner, row sources, SQL functions)
- [ ] **M2** Staging: one `GEOJSON` fixture template + one `POSTGIS` JDBC template with `CHUNKED` export
- [ ] **M2** Confirm PostGIS extension and `ST_*` projections on target warehouse

Deferred geo: streaming GeoJSON, Shapefile/GeoPackage, Calcite-native `ST_*`, survey-grade buffers — see `docs/geospatial-overview.md`.

## Out of scope (post-merge / M2)

- Production-wide `db-{id}` promote and P4 `v1-execution.enabled=false` (requires staging — see `docs/migration/staging-readiness-checklist.md`)
- Vaadin operator UI for migration inventory
- Wave freeze calendar dates (product owner, after staging R0)

## Reviewer checklist

- [ ] **Scope:** M1 retirement evidence is CI-only; do not require staging promote or P4 flag flip to merge.
- [ ] **Tests:** `.\mvnw-jdk25.ps1 test` green (or reviewer re-ran 2026-05-22 full reactor SUCCESS).
- [ ] **Migration:** Promote rejects `COMPATIBILITY_ONLY` / `BLOCKED` and unsigned `db-{id}` inventory rows.
- [ ] **Config:** Default `pci.data.generator.v1-execution.enabled=true` — no accidental V1 disable in shipped yaml.
- [ ] **Docs:** `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md` read for M2 follow-up.
- [ ] **Geo / CHUNKED:** Spot-check only if your domain uses geo or large JDBC export (see geospatial + chunked guides).

**Create MR (GitLab):** `feature-4.0` → `master`  
http://172.25.21.141/gensokyo/data-generator/-/merge_requests/new?merge_request[source_branch]=feature-4.0&merge_request[target_branch]=master

## Commits

`feature-4.0` includes JDBC chunked execution, migration workbench, geospatial phases, V1 retirement program (control plane, SpEL, SCRIPT→SpEL draft, **JDBC dual-run compare with SpEL**, v1 flag, CI simulation, promote sign-off guard), and deferred-ops documentation. **`master`** at `b93bc3f` merges this line (2026-05-22). See `docs/migration/script-spel-draft-migration.md`.
