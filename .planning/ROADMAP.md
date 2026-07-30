# Roadmap: data-generator

## Milestones

- ✅ **v1.0 UDF, Transform & Test Harness** — Phases 1-5 (shipped 2026-06-23)
- ✅ **v2.0 Reader/Writer & Datasource Platform** — Phases 6-11 (+07.1) (shipped 2026-07-25)
- ✅ **v2.1 Hardening & Weak-Spot Closure** — Phases 12-17 (shipped 2026-07-29)
- 🚧 **v2.2 V2 Geo Synthetic Source** — Phases 18-20 (in planning)

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

## Milestone v2.2: V2 Geo Synthetic Source

**Status:** Planning  
**Phases:** 18–20  
**Requirements:** GEO-01, GEO-02, GEO-03, GEO-04, TEST-10  
**Spec:** [docs/superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md](../docs/superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md)

### Overview

Add first-class Template V2 `geo_synthetic` source for point synthesis (boundary, line sample, bbox, circle). Path-referenced GeoJSON only; keep `geojson` read-only; P1 harness link; P0 gate frozen.

### Phase list

- [x] **Phase 18: Geo Generator Modes** — Extend `GeoGenerationMode` / generator for BBOX + CIRCLE; harden validation (GEO-02 foundation) (completed 2026-07-30)
- [x] **Phase 19: V2 Geo Synthetic Source** — `GeoSyntheticSourceVO` + Factory + RowSource + CoreConfig; path assets; `geojson` untouched (GEO-01, GEO-03) (completed 2026-07-30)
- [x] **Phase 20: Pipeline Proof + Docs + P1** — TemplateV2Runner IT for four modes; docs; matrix P1 for `geo-synthetic` (GEO-02 closeout, GEO-04, TEST-10) (completed 2026-07-30)

### Phase Details

### Phase 18: Geo Generator Modes

**Goal**: Extend `data-generator-geo` so BBOX and CIRCLE modes generate seeded, in-domain points with the same validation rigor as existing boundary/line modes.

**Depends on**: v2.1 complete

**Requirements**: GEO-02 (generator half)

**Success Criteria**:

1. `GeoGenerationMode` includes `BBOX` and `CIRCLE`
2. Unit tests prove in-domain points, seed reproducibility, and illegal config failures
3. Existing `BOUNDARY_POINTS` / `LINE_SAMPLE` tests remain green
4. CIRCLE uses area-uniform polar sampling + Haversine check per design spec

**Plans**: 3/3 plans complete

Plans:
**Wave 1**

- [x] 18-01-PLAN.md — Extend GeoGenerationMode + request validation for BBOX/CIRCLE

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 18-02-PLAN.md — BboxPointGenerator + BBOX dispatch + integration tests

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 18-03-PLAN.md — CirclePointGenerator + CIRCLE dispatch + full module regression

### Phase 19: V2 Geo Synthetic Source

**Goal**: Expose synthesis as Template V2 `type: geo_synthetic` (Approach A) — distinct from read-only `geojson`.

**Depends on**: Phase 18

**Requirements**: GEO-01, GEO-03

**Success Criteria**:

1. `GeoSyntheticSourceVO` + `GeoSyntheticSourceFactory` + `GeoSyntheticRowSource` registered like `GeoJsonSourceFactory`
2. All four modes configurable via YAML and produce `Row`/`RowSchema`
3. Paths resolve only via `GeoResourceResolver`; no upload API
4. `type: geojson` behavior unchanged (regression green)
5. Invalid config throws `IllegalArgumentException` with source name + field

**Plans**: 3/3 plans complete

Plans:
**Wave 1**

- [x] 19-01-PLAN.md — GeoSynthetic V2 VO types (output, sample, source config)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 19-02-PLAN.md — GeoSyntheticRequestMapper + mapping unit tests

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 19-03-PLAN.md — Factory, RowSource, CoreConfig bean, integration tests + geojson regression

### Phase 20: Pipeline Proof + Docs + P1

**Goal**: Prove end-to-end V2 pipeline, document the source split, and link harness P1 without touching P0.

**Depends on**: Phase 19

**Requirements**: GEO-02 (pipeline evidence), GEO-04, TEST-10

**Success Criteria**:

1. Template V2 IT runs `geo_synthetic` → transform → sink for each mode (or shared fixture covering all four)
2. Docs distinguish `geo_synthetic` vs `geojson` and include a minimal YAML example
3. `.planning/test-matrix.yaml` `geo-synthetic` is P1 with linked tests; `p0.total` remains 15
4. V1 geo iterator not required for the happy path (docs may note V2 preference)

**Plans**: 3/3 plans complete

Plans:
**Wave 1** *(parallel)*

- [x] 20-01-PLAN.md — TemplateV2RunnerGeoSyntheticSourceTests four-mode pipeline IT (GEO-02)
- [x] 20-02-PLAN.md — geospatial docs: geo_synthetic vs geojson, YAML example, output formats (GEO-04)

**Wave 2** *(blocked on 20-01 for linked_tests accuracy)*

- [x] 20-03-PLAN.md — test-matrix P1 promotion + doc sync; P0 frozen at 15 (TEST-10)

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 18. Geo Generator Modes | v2.2 | 3/3 | Complete    | 2026-07-30 |
| 19. V2 Geo Synthetic Source | v2.2 | 3/3 | Complete    | 2026-07-30 |
| 20. Pipeline Proof + Docs + P1 | v2.2 | 3/3 | Complete    | 2026-07-30 |

**Next:** `/gsd-verify-work` (v2.2 milestone)
