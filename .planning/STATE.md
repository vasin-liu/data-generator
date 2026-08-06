---
gsd_state_version: 1.0
milestone: v2.3
milestone_name: Geo Assets & Map Preview
current_phase: 22
current_phase_name: Console Map + geo_synthetic Editor
status: executing
stopped_at: Completed 22-03-PLAN.md
last_updated: "2026-08-06T15:32:29.132Z"
last_activity: 2026-08-06
last_activity_desc: Completed 22-01-PLAN.md (geo preview APIs)
progress:
  total_phases: 3
  completed_phases: 1
  total_plans: 7
  completed_plans: 6
  percent: 33
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-07-31)

**Core value:** Operators can define, extend, and trust data-generation pipelines through uploadable UDFs, richer transforms, and an automated test harness.
**Current focus:** Phase 22 — Console Map + geo_synthetic Editor

## Current Position

Phase: 22 — Console Map + geo_synthetic Editor
Plan: 4 of 04
Status: Executing (22-01 complete; --no-transition)
Last activity: 2026-08-06 — Completed 22-01-PLAN.md (geo preview APIs)

## Performance Metrics

**Velocity (v1.0):**

- Phases: 5
- Plans: 18
- Commits (milestone range): 46
- Timeline: 2026-06-17 → 2026-06-23

**Velocity (v2.0):**

- Phases: 7 (6–11 + 07.1)
- Plans: 36
- Tasks: 66
- Commits: 130 (`v1.0` → HEAD)
- Files: 331 changed (+33,397 / −482)
- Timeline: 2026-06-23 → 2026-07-25 (31 days)

**Velocity (v2.1):**

- Phases: 6 (12–17)
- Plans: 18 (Phase 12: 2, 13: 5, 14: 2, 15: 3, 16: 3, 17: 3)
- Status: All phases complete — archived via `/gsd-complete-milestone`

**Velocity (v2.2):**

- Phases: 3 (18–20)
- Plans: 9
- Tasks: 26
- Commits: 71 (`v2.1` → HEAD)
- Files: 129 changed (+6,569 / −57)
- Timeline: 2026-07-30 → 2026-07-31 (2 days)
- Status: All phases complete — archived via `/gsd-complete-milestone`

**Velocity (v2.3 in progress):**

- Phases: 3 (21–23)
- Plans completed: 4 (Phase 21: 3, Phase 22: 1)
- Status: Phase 22 executing (22-01 complete)
- Phase 22 P01: 45min | 2 tasks | 9 files

## Accumulated Context

### Decisions

Full log in PROJECT.md Key Decisions. Highlights:

- v1 quality-first harness + UDF + transforms — **shipped**
- v2 datasource platform + RW streaming/upsert + dialects — **shipped**
- P0 gate expanded 7 → 15 rows — **shipped**
- Dual JDBC resolvers ownership split — **deferred consolidation** (v2.1: docs/inventory only — RES-01)
- Dameng live IT opt-in — **accepted tech debt** (v2.1: document green path — DIAL-01)
- Orchestration deferred beyond v2.0 — **still deferred** (not in v2.1)
- v2.1 breadth hardening over new features — **complete**
- v2.1 RBAC stays default-off with testable enable path — **complete** (SEC-01)
- v2.1 phase map 12–17 from research SUMMARY — **roadmap locked**
- [Phase 12]: HTTP EXEC-01 via MockMvc /task/run + publish gate + COUNT(*); Phase 11 IT untouched — Close v2.0 in-process-only limit without renaming Phase 11 proof
- [Phase 12]: HTTP snap pools require passwordSecretRef for non-empty managed JDBC passwords (EXEC-02 IT) — ConnectionSnapshotSupport only snapshots passwordSecretRef; plaintext save leaves snap: pools without credentials
- [Phase 13]: Phase 13 P03: Nyquist backfill for 07/07.1 transcribes existing green tests only — 07-VALIDATION.md and 07.1-VALIDATION.md set nyquist_compliant: true by citing test classes/commands already recorded green in VERIFICATION/SUMMARY reports (D-11); no new tests written, no Phase 07/07.1 implementation reopened, Phase 12 validation state untouched (D-10)
- [Phase 13]: Phase 13 P04: Backfilled 08-VALIDATION.md (12 grouped plan-task rows from 58 already-green truths) and synced v2.0-MILESTONE-AUDIT.md Nyquist table/frontmatter to COMPLIANT for 07/07.1/08 — DIAL-02 requires accurate nyquist_compliant status across 07/07.1/08; grouping by plan task (not 1:1 with the 58 VERIFICATION truths) keeps the map readable while staying honest transcription-only (D-11); milestone audit must agree with the backfilled files or the exercise is invisible to maintainers (D-13). Phase 12 validation and P0 gate untouched (D-10). DIAL-02 now complete; Phase 13 fully done.
- [Phase 17]: TEST-09 P1 rows wired (exec-http-managed-catalog, exec-http-postgres-dialect, rbac-enable-path, dist-multi-jvm-worker); P0 gate frozen at 15 rows; verify-harness.ps1 sole merge gate unchanged
- [Phase 18]: Copied BoundaryPointGenerator retry pattern for BboxPointGenerator per D-10
- [Phase 19-v2-geo-synthetic-source]: GeoSynthetic V2 VO types in core with independent output VO and seed default 0L — Locked D-01/D-03/D-09; enables Plan 19-02 mapper and Plan 19-03 RowSource
- [Phase 19-v2-geo-synthetic-source]: GeoSyntheticRequestMapper in calcite expands VO arrays and wraps validate() with source-scoped errors — D-04/D-07 bridge for Plan 19-03 Factory/RowSource; V1 GeoIteratorRequestMapper untouched per D-05
- [Phase 19-v2-geo-synthetic-source]: GeoSynthetic Factory/RowSource + CoreConfig bean complete; GEO-01/GEO-03 Phase 19 criteria met
- [Phase 20-pipeline-proof-docs-p1]: Dedicated geo-synthetic-v2-source.md for YAML/modes/output; overview remains landing page per D-06 — GEO-04 complete
- [Phase 20-pipeline-proof-docs-p1]: Dedicated TemplateV2RunnerGeoSyntheticSourceTests with four mode-specific pipeline tests per D-01/D-02 — GEO-02 pipeline evidence complete
- [Phase 20-pipeline-proof-docs-p1]: geo-synthetic harness row P1 covered with inline linked_tests; P0 frozen at 15 per D-10 — TEST-10 complete
- [Phase 21]: Geo asset registry CLOB + console REST upload/list/get — GEO-05/GEO-08
- [Phase 21]: asset:{uuid} execute-path spine via GeoAssetResolver in geo module — GEO-10/GEO-11
- [Phase 21]: Hard delete with template reference scan → 409 + GEO_ASSET_DELETE audit — GEO-09/GOV-01
- [Phase 22]: Reject maxCount > 500 with IAE naming the cap (no silent clamp)
- [Phase 22]: preview/location raw geo+json; synthetic R envelope; same Phase 21 resolve spine
- [Phase 22]: ApiRequestError preserves status+data for 409 usages; location preview uses raw geo+json fetch
- [Phase 22]: GeoMapPreview MapLibre via react-map-gl/maplibre with OSM + honesty Alert; CSS only in map module
- [Phase ?]: 22-03: contentType column only when API exposes it; size omitted (D-02)
- [Phase ?]: 22-03: 409 delete shows usages Modal.info; no client force-delete
- [Phase ?]: 22-03: geoAssets.* + nav only; source.geoSynthetic.* deferred to Plan 04

### Pending Todos

None.

### Blockers/Concerns

- Boot 4 / internal Kafka-ES starter compatibility (see `.planning/codebase/CONCERNS.md`)
- Console RBAC default-off for external deployments
- Accepted v2.0 tech debt (Dameng CI, dual resolvers) — addressed in v2.1; not P0 inflation
- Accepted v2.2 tech debt: BboxPointGenerator retry duplication, inline linked_tests for matrix doc — see `milestones/v2.2-MILESTONE-AUDIT.md`

## Deferred Items

Items carried or acknowledged at milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Product | Template-level orchestration (ORCH) | Later version | 2026-06-17 |
| Product | Net-new connectors (Redis, S3, HTTP) | Later version | 2026-06-23 |
| Process | Milestone audit skipped at v1.0 close | Accepted | 2026-06-23 |
| tech_debt | Dameng live CI opt-in only | Documented via DIAL-01 (v2.1) | 2026-07-25 |
| tech_debt | Nyquist hygiene phases 07/08/07.1 | Closed via DIAL-02 (Phase 13, 2026-07-28) | 2026-07-25 |
| tech_debt | Dual JDBC resolver consolidation | Docs-only RES-01; merge = RES-02 later | 2026-07-25 |
| Process | v2.1 milestone archive tree | Deferred per D-16 | 2026-07-29 |
| Product | GeoJSON asset upload (GEO-05) | Done in Phase 21 | 2026-07-31 |
| Product | Polygon synthesis (GEO-06) | Deferred past v2.3 | 2026-07-31 |
| Product | Console map preview (GEO-07) | In scope v2.3 Phase 22 | 2026-07-31 |
| Product | Common-data CRUD (DATA-01) | Next milestone candidate | 2026-07-31 |
| Phase 22 P02 | 17min | 2 tasks | 6 files |
| Phase 22 P03 | 12min | 2 tasks | 5 files |

## Session Continuity

**Resume file:** None

Last session: 2026-08-06T15:32:29.117Z
Stopped at: Completed 22-03-PLAN.md
Next: Continue Phase 22 with `/gsd-execute-phase 22` (plan 22-02) — parent flagged `--no-transition`

## Operator Next Steps

- Execute remaining Phase 22 plans (22-02 MapLibre client, 22-03 assets page, 22-04 editor)
