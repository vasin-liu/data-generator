# V1 retirement readiness checklist

Honest gates for retiring V1 template execution in favor of Template V2 / Calcite. Inventory ids reference `docs/migration/scenario-inventory.yaml`.

**Merge vs retirement:** `feature-4.0` may merge when P1/P2 + CI simulation are green. Full P3 production promote and P4 cutover are **M2** (staging required). See `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md`.

| Milestone | Staging | Production promote checkbox | `v1-execution.enabled=false` |
|-----------|---------|----------------------------|-------------------------------|
| **M1** (pre-staging) | Not required | Leave **unchecked** — use CI substitutes in spec | No |
| **M2** (staging-ready) | refresh / batch compare / real promote | Check when `db-{id}` cohort done | Staging trial then prod |

Staging prep: `docs/migration/staging-readiness-checklist.md` → M2 ops: `docs/migration/M2-production-catalog-handoff.md`

Promote requires business sign-off when a `db-{id}` inventory row exists (enforced in `MigrationPromoteService`). Staging script: `workflow-promote` action.

### M1 evidence substitutes (no staging)

| Staging intent | M1 substitute |
|----------------|---------------|
| Dual-run + report | `StagingSimulatedPromoteWorkflowTests`, `BuiltinClasspathTemplateMigrationWorkflowTests` |
| Cohort sign-off | `MigrationWaveCohortSignoffTests` + `scenario-inventory.yaml` |
| End-to-end promote | `StagingSimulatedPromoteWorkflowTests` + `migration-staging.ps1 -Action workflow-promote` (M2) |
| SCRIPT → SpEL draft | `V1ScriptToSpelDraftConverter`; JDBC compare uses SQL+SpEL (`BuiltinClasspathTemplateMigrationWorkflowTests` parking/11 H2); `docs/migration/script-spel-draft-migration.md` |
| W3 orchestration census | `BuiltinTemplateMigrationCensusTest` + `docs/migration/reports/builtin-orchestration-census.md`; policy S1 in `docs/migration/orchestration-retirement-boundary.md` |

## P1 — Technical

- [x] Migration scenario inventory maintained (`scenario-inventory.yaml`, ≥10 tracked templates)
- [x] Dual-run compare API (`POST /template/migration/compare/{templateId}`) with markdown reports under `docs/migration/reports/`
- [x] Classification rules (EXACT / ADAPTED / APPROXIMATE / BLOCKED / COMPATIBILITY_ONLY)
- [x] Unified draft migration (`POST /template/migration/draft/{templateId}`) — query-source + iterator
- [x] Promote workflow (`POST /template/migration/promote/{templateId}`)
- [x] CHUNKED execution policy suggested on single-source JDBC migrate (`executionPolicy.mode: CHUNKED`)
- [x] Calcite explain / plan diff for operators (bounded — compare reports include execution shape, validation, V1/V2 SQL diff notes; not full control-plane explain)
- [x] Full JDBC chunked parity on MySQL/Postgres — `ChunkedPipelineMySqlContainerTests` (useCursorFetch), `ChunkedPipelinePostgresContainerTests` (Testcontainers; skip when Docker unavailable)

## P2 — Operational

- [x] Compare reports linked from inventory (`lastCompareReportPath`)
- [x] Runbook for failed dual-run (`BLOCKED`) remediation — `docs/migration/blocked-dual-run-runbook.md`
- [x] Batch / scheduled dual-run on DB catalog — `POST /template/migration/compare/batch`, optional `pci.data.generator.migration.batch-compare.scheduled-enabled`
- [x] Operator summary API — `GET /template/migration/summary` + `scripts/migration-staging.ps1` + `docs/migration/staging-runbook.md`
- [x] Template V2 control-plane validate / explain / preview (`POST|GET /template/v2/*`)
- [x] First non-SQL transformer: `SpelTransformVO` + `SpelTransformFactory` (row-level SpEL)
- [x] SCRIPT → SpEL migration draft bridge (`V1ScriptToSpelDraftConverter`, SQL + SpEL draft chain)
- [x] Operator console UI (Vaadin 25) — `docs/operator-console-usage.md`; spec `docs/superpowers/specs/2026-05-23-operator-console-design.md`
  - [x] P0 shell + nav + V1 execution banner
  - [x] P1 V2 template editor (forms + YAML advanced) + archived templates
  - [x] P2 `datasource_config` persistence + datasource views
  - [x] P3 `task_execution` history + job center
  - [x] P4 migration tab + `/migration` dashboard (promote disabled for `COMPATIBILITY_ONLY` per S1)
  - [ ] P1.5 extra source types in forms; Vaadin security roles; editor Review→Run shortcut (see usage guide gaps)

## P3 — Business

- [x] Sign-off per scenario family: `synthetic` (Wave 1) — cohort in `scenario-inventory.yaml`; automated `MigrationWaveCohortSignoffTests` + builtin `demo/28` dual-run
- [x] Sign-off per scenario family: `multi_source` (Wave 2) — cohort in `scenario-inventory.yaml`; automated `MigrationWaveCohortSignoffTests`
- [x] Builtin `parking/11` H2 dual-run (SQL+SpEL) — analyzer family `synthetic` (Wave 1 inventory row); JDBC-shaped R0 evidence
- [x] Compatibility-only templates documented and accepted (`docs/migration/compatibility-only-templates.md`)
  - [x] `regression-v1-with-pause` — PAUSE retained on V1; excluded from promote/sign-off cohort
- [ ] Production templates promoted with `migrationClass` EXACT or ADAPTED (**M2 only** — staging DB catalog; M1 uses CI `StagingSimulatedPromoteWorkflowTests` as substitute, not this checkbox)

## Evidence samples

| Inventory id | Report |
|--------------|--------|
| regression-v1-constant-five-rows | `docs/migration/reports/sample-regression-v1-constant-five-rows.md` |
| regression-v1-iterator-simple | `docs/migration/reports/sample-regression-v1-iterator-simple.md` |
| regression-v1-query-lookup | `docs/migration/reports/sample-regression-v1-query-lookup.md` |
| builtin-demo-28 (see `wave-freeze-schedule.md`) | CI temp report via `BuiltinClasspathTemplateMigrationWorkflowTests` |
| builtin-tocc-parking-11 (H2-adapted) | CI temp report via `BuiltinClasspathTemplateMigrationWorkflowTests` |
| staging-sim-synthetic / staging-sim-jdbc | `StagingSimulatedPromoteWorkflowTests` (refresh → compare → signoff → promote); JDBC sim uses builtin `parking/11` H2-adapted with SQL+SpEL compare |
| builtin-orchestration-census | `docs/migration/reports/builtin-orchestration-census.md` (2/59 COMPATIBILITY_ONLY; PAUSE/LOG) |

## P4 — Runtime cutover

- [x] Config flag `pci.data.generator.v1-execution.enabled` gates `TaskController` V1 runs (default `true`)
- [ ] Staging/prod set `v1-execution.enabled=false` after P3 sign-off and wave-freeze dates (`docs/migration/wave-freeze-schedule.md`)

### P4 cutover checklist (operators)

1. Confirm P3: `GET /template/migration/signoff-status` shows `familySignoffComplete=true` for `synthetic` and `multi_source`.
2. Confirm wave-freeze calendar in `docs/migration/wave-freeze-schedule.md` (W1/W2 dates signed by product owner).
3. Set `pci.data.generator.v1-execution.enabled: false` in target environment `application.yaml`.
4. Smoke: V2 `POST /template/v2/preview/{id}` on promoted templates; V1 `POST /task/run` returns disabled message.
5. Keep compatibility-only templates on V1 via explicit exemption list (`docs/migration/compatibility-only-templates.md`).

When all P1 items, scenario-family P3 items, and P4 cutover are checked, V1 execution may be deprecated per team policy.
