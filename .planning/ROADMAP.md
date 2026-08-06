# Roadmap: data-generator

## Milestones

- ✅ **v1.0 UDF, Transform & Test Harness** — Phases 1-5 (shipped 2026-06-23)
- ✅ **v2.0 Reader/Writer & Datasource Platform** — Phases 6-11 (+07.1) (shipped 2026-07-25)
- ✅ **v2.1 Hardening & Weak-Spot Closure** — Phases 12-17 (shipped 2026-07-29)
- ✅ **v2.2 V2 Geo Synthetic Source** — Phases 18-20 (shipped 2026-07-31)
- **v2.3 Geo Assets & Map Preview** — Phases 21-23 (planning)

## Phases

<details>
<summary>✅ v1.0 UDF, Transform & Test Harness (Phases 1-5) — SHIPPED 2026-06-23</summary>

- [x] Phase 1: Test Harness Foundation (3/3 plans) — completed 2026-06-17
- [x] Phase 2: UDF Platform Core (3/3 plans) — completed 2026-06-18
- [x] Phase 3: UDF Console & Template Binding (5/5 plans) — completed 2026-06-18
- [x] Phase 4: Transform Operators & SQL (5/5 plans) — completed 2026-06-22
- [x] Phase 5: Coverage Ramp & CI Gates (2/2 plans) — completed 2026-06-23

Full archive: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

</details>

<details>
<summary>✅ v2.0 Reader/Writer & Datasource Platform (Phases 6-11) — SHIPPED 2026-07-25</summary>

- [x] Phase 6: Datasource Platform Core (5/5 plans)
- [x] Phase 7: Datasource Governance & Hot-Reload (5/5 plans)
- [x] Phase 07.1: Close gap DS-03 JDBC snapshot routing (3/3 plans) — INSERTED — completed 2026-07-24
- [x] Phase 8: RW Streaming & Upsert (12/12 plans)
- [x] Phase 9: JDBC Dialect Expansion (5/5 plans) — completed 2026-07-21
- [x] Phase 10: Harness Coverage & CI Gates (3/3 plans)
- [x] Phase 11: v2.0 closeout hardening (3/3 plans) — completed 2026-07-25

Full archive: [milestones/v2.0-ROADMAP.md](milestones/v2.0-ROADMAP.md)

</details>

<details>
<summary>✅ v2.1 Hardening & Weak-Spot Closure (Phases 12-17) — SHIPPED 2026-07-29</summary>

- [x] Phase 12: HTTP Execute-Path Proof (2/2 plans) — completed 2026-07-25
- [x] Phase 13: Dameng Live Path + Nyquist Hygiene (5/5 plans) — completed 2026-07-28
- [x] Phase 14: Resolver Ownership Docs (2/2 plans) — completed 2026-07-29
- [x] Phase 15: Multi-JVM Worker E2E (3/3 plans) — completed 2026-07-29
- [x] Phase 16: RBAC Enable Path (3/3 plans) — completed 2026-07-29
- [x] Phase 17: P1 Harness Expansion + Closeout (3/3 plans) — completed 2026-07-29

Full archive: [milestones/v2.1-ROADMAP.md](milestones/v2.1-ROADMAP.md)

</details>

<details>
<summary>✅ v2.2 V2 Geo Synthetic Source (Phases 18-20) — SHIPPED 2026-07-31</summary>

- [x] Phase 18: Geo Generator Modes (3/3 plans) — completed 2026-07-30
- [x] Phase 19: V2 Geo Synthetic Source (3/3 plans) — completed 2026-07-30
- [x] Phase 20: Pipeline Proof + Docs + P1 (3/3 plans) — completed 2026-07-30

Full archive: [milestones/v2.2-ROADMAP.md](milestones/v2.2-ROADMAP.md)

</details>

## Milestone v2.3: Geo Assets & Map Preview

**Status:** Planning  
**Phases:** 21–23  
**Requirements:** GEO-05, GEO-07, GEO-08, GEO-09, GEO-10, GEO-11, GEO-12, GEO-13, GOV-01, DOC-01, TEST-11  
**Research:** [.planning/research/SUMMARY.md](research/SUMMARY.md)

### Overview

Close the operator gap left by v2.2: durable hosted GeoJSON assets in the metadata DB (asset-id references), runtime `asset:{id}` resolution on the execute path, and equal-depth console map preview for uploaded assets and `geo_synthetic` configuration. Path/`classpath:` fallback preserved; P0 merge gate frozen at 15.

### Phase list

- [x] **Phase 21: Geo Asset Registry + Runtime Resolution** — Upload/list/get/delete API, validation + size gates, audit, `GeoAssetResolver` wired into geojson + geo_synthetic; template binding; pipeline IT with asset-id (GEO-05, GEO-08, GEO-09, GEO-10, GEO-11, GOV-01) (3/3 plans)
- [ ] **Phase 22: Console Map + geo_synthetic Editor** — Geo assets page, MapLibre preview (asset layer + synthetic overlays), asset picker, `geo_synthetic` in template editor (GEO-07, GEO-12, GEO-13)
- [ ] **Phase 23: Docs + Harness Closeout** — Asset-id YAML docs, map preview usage, optional P1 `geo-assets` row; P0 frozen (DOC-01, TEST-11)

### Phase Details

### Phase 21: Geo Asset Registry + Runtime Resolution

**Goal**: Operators can upload, browse, and safely delete GeoJSON assets; templates bind via asset-id; runs resolve `asset:{id}` on the execute path — not console-only.

**Depends on**: v2.2 complete (Phase 20)

**Requirements**: GEO-05, GEO-08, GEO-09, GEO-10, GEO-11, GOV-01

**Success Criteria** (observable):

1. Operator uploads GeoJSON via console API; platform validates geometry, enforces max bytes and feature count, persists body in metadata DB, and returns a stable asset-id
2. Operator lists assets (metadata without full body) and fetches a single asset's GeoJSON by asset-id, including derived bbox and featureCount captured at ingest
3. Operator attempting to delete an asset still referenced by a stored template receives 409 with usage hints instead of orphaning runs
4. Operator binds `geo_synthetic` (boundary/network) and `geojson` sources via asset-id; path and `classpath:` locations remain valid
5. Template V2 runs resolve `asset:{id}` through a shared `GeoAssetResolver` on the execute path (coordinator and worker share metadata DB)
6. Upload and delete emit audit events; when console RBAC is enabled, geo asset endpoints respect the existing enable flag (default off)

**Plans**: 3/3 complete (21-01, 21-02, 21-03) — ready for verify / Phase 22 plan

### Phase 22: Console Map + geo_synthetic Editor

**Goal**: Equal-depth console UX — browse uploaded assets on a map, preview `geo_synthetic` config, and edit `geo_synthetic` sources in the template editor (closes v2.2 YAML-only gap).

**Depends on**: Phase 21

**Requirements**: GEO-07, GEO-12, GEO-13

**Success Criteria** (observable):

1. Operator opens a console geo-assets view and sees the selected uploaded asset rendered on a map
2. Operator previews a `geo_synthetic` source config on the map (boundary/network overlay and/or BBOX/CIRCLE guides)
3. Preview UX documents seed behavior so operators do not mistake preview sampling for full run output
4. Console template editor supports `geo_synthetic` as an editable source kind with an asset picker
5. Map asset layers use the same resolution spine as runtime (server GeoJSON for assets; client Turf overlays for BBOX/CIRCLE guides)

**Plans**: 2/4 plans executed

Plans:
**Wave 1**

- [x] 22-01-PLAN.md — Preview APIs (path/classpath + capped synthetic) on Phase 21 resolve spine
- [x] 22-02-PLAN.md — ApiRequestError 409 usages + MapLibre/Turf + lazy GeoMapPreview

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 22-03-PLAN.md — Geo assets page (list/map/upload/delete Modal) + nav/i18n

**Wave 3** *(blocked on Wave 2 completion)*

- [ ] 22-04-PLAN.md — geo_synthetic editor, asset picker, hybrid preview honesty, E2E smoke

### Phase 23: Docs + Harness Closeout

**Goal**: Operator documentation for asset-id binding and map preview; optional P1 harness linkage without P0 gate inflation.

**Depends on**: Phase 22

**Requirements**: DOC-01, TEST-11

**Success Criteria** (observable):

1. Maintainers and operators have docs for asset-id YAML examples, path vs asset-id, map preview usage, and upload size limits
2. Feature matrix may link a P1 `geo-assets` (or equivalent) row to real tests when stable
3. `verify-harness.ps1` P0 set remains 15 rows — no P0 promotion of geo-assets
4. Geo-assets verification slice (Maven IT + console build) is green in harness summary

**Plans**: 0 plans

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 21. Geo Asset Registry + Runtime Resolution | v2.3 | 3/3 | Complete    | 2026-08-04 |
| 22. Console Map + geo_synthetic Editor | v2.3 | 2/4 | In Progress|  |
| 23. Docs + Harness Closeout | v2.3 | 0/? | Not started | — |

| Milestone | Phases | Status | Shipped |
|-----------|--------|--------|---------|
| v2.3 Geo Assets & Map Preview | 21–23 (0/? plans) | Planning | — |

**Next:** `/gsd-execute-phase 21`
