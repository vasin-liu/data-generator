---
phase: 20-pipeline-proof-docs-p1
plan: 01
subsystem: testing
tags: [calcite, geo_synthetic, TemplateV2Runner, junit, pipeline-proof]

requires:
  - phase: 19-v2-geo-synthetic-source
    provides: GeoSyntheticSourceFactory, GeoSyntheticRowSource, GeoSyntheticSourceVO
provides:
  - TemplateV2RunnerGeoSyntheticSourceTests four-mode pipeline IT
  - GEO-02 pipeline evidence (Phase 19 D-14 deferral closed)
affects:
  - 20-03-PLAN.md
  - test-matrix geo-synthetic linked_tests

tech-stack:
  added: []
  patterns:
    - "Dedicated TemplateV2Runner test class with geoSyntheticRegistry() per D-01"
    - "Passthrough SQL select lon, lat → console sink per D-03"

key-files:
  created:
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerGeoSyntheticSourceTests.java
  modified: []

key-decisions:
  - "Dedicated TemplateV2RunnerGeoSyntheticSourceTests class (not extending TemplateV2RunnerGeoSourceTests) per D-01"
  - "Four explicit @Test methods with counts aligned to GeoSyntheticRowSourceTests fixtures per D-02/D-06"
  - "Passthrough SQL only — no new ST_* or V2_GEO_* surface per D-03"

patterns-established:
  - "geo_synthetic pipeline proof mirrors geoJsonRegistry pattern with GeoSyntheticSourceFactory"

requirements-completed: [GEO-02]

coverage:
  - id: D1
    description: "Dedicated TemplateV2RunnerGeoSyntheticSourceTests with geoSyntheticRegistry() helpers"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-calcite/.../TemplateV2RunnerGeoSyntheticSourceTests.java (compile)"
        status: pass
    human_judgment: false
  - id: D2
    description: "BOUNDARY_POINTS pipeline returns 6 rows with lon/lat through SQL transform and console sink"
    requirement: GEO-02
    verification:
      - kind: integration
        ref: "TemplateV2RunnerGeoSyntheticSourceTests#boundaryPoints_pipelineRun_returnsExpectedRowCount"
        status: pass
    human_judgment: false
  - id: D3
    description: "LINE_SAMPLE pipeline returns non-empty rows via BY_SPACING_METERS fixture"
    requirement: GEO-02
    verification:
      - kind: integration
        ref: "TemplateV2RunnerGeoSyntheticSourceTests#lineSample_pipelineRun_returnsNonEmptyRows"
        status: pass
    human_judgment: false
  - id: D4
    description: "BBOX pipeline returns configured count (5 rows) with passthrough SQL"
    requirement: GEO-02
    verification:
      - kind: integration
        ref: "TemplateV2RunnerGeoSyntheticSourceTests#bbox_pipelineRun_returnsExpectedRowCount"
        status: pass
    human_judgment: false
  - id: D5
    description: "CIRCLE pipeline returns configured count (4 rows) with passthrough SQL"
    requirement: GEO-02
    verification:
      - kind: integration
        ref: "TemplateV2RunnerGeoSyntheticSourceTests#circle_pipelineRun_returnsExpectedRowCount"
        status: pass
    human_judgment: false
  - id: D6
    description: "geojson and RowSource regression tests remain green (GEO-03 unchanged)"
    requirement: GEO-02
    verification:
      - kind: integration
        ref: "TemplateV2RunnerGeoSourceTests + GeoSyntheticRowSourceTests (16 tests)"
        status: pass
    human_judgment: false

duration: 24min
completed: 2026-07-30
status: complete
---

# Phase 20 Plan 01: Geo Synthetic Pipeline IT Summary

**Four-mode TemplateV2Runner pipeline proof for geo_synthetic with passthrough SQL and console sink — closes Phase 19 D-14 deferral**

## Performance

- **Duration:** 24 min
- **Started:** 2026-07-30T11:34:00Z
- **Completed:** 2026-07-30T11:58:00Z
- **Tasks:** 3
- **Files modified:** 1

## Accomplishments

- Added `TemplateV2RunnerGeoSyntheticSourceTests` with `geoSyntheticRegistry()` registering `GeoSyntheticSourceFactory`, `SqlTransformFactory`, and `ConsoleSinkFactory` per D-01
- Four explicit `@Test` methods cover BOUNDARY_POINTS (6 rows), LINE_SAMPLE (non-empty), BBOX (5 rows), and CIRCLE (4 rows) per D-02
- Pipeline shape `geo_synthetic → select lon, lat → console sink` with no new SQL UDFs per D-03
- Calcite-only in-process runner — no Spring Boot IT or Playwright per D-04
- GEO-02 pipeline evidence satisfied; REQUIREMENTS.md GEO-02 was already Complete (generator + pipeline halves now both evidenced)

## Task Commits

Each task was committed atomically:

1. **Task 1: Scaffold TemplateV2RunnerGeoSyntheticSourceTests and registry helper** - `eff8e16` (test)
2. **Task 2: Pipeline tests for BOUNDARY_POINTS and LINE_SAMPLE** - `a09d6a9` (test)
3. **Task 3: Pipeline tests for BBOX and CIRCLE** - `87ce002` (test)

**Plan metadata:** pending (docs commit follows)

## Files Created/Modified

- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerGeoSyntheticSourceTests.java` — End-to-end pipeline proof for all four geo_synthetic modes (146 lines)

## Decisions Made

- Used classpath fixtures from `GeoSyntheticRowSourceTests` (南沙区边界.geojson, 南沙区道路路网.geojson) for boundary/line modes
- BBOX count 5 and CIRCLE count 4 aligned with Phase 19 RowSource unit tests (D-06 heuristic)
- Left `TemplateV2RunnerGeoSourceTests` unchanged per plan scope

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 20-03 can now link `TemplateV2RunnerGeoSyntheticSourceTests` in test-matrix `geo-synthetic` row (TEST-10)
- GEO-04 and TEST-10 remain for plans 02/03 respectively — not marked complete by this plan

## Self-Check: PASSED

- FOUND: data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerGeoSyntheticSourceTests.java
- FOUND: eff8e16
- FOUND: a09d6a9
- FOUND: 87ce002

---
*Phase: 20-pipeline-proof-docs-p1*
*Completed: 2026-07-30*
