---
phase: 19-v2-geo-synthetic-source
plan: 02
subsystem: api
tags: [java, template-v2, geo, mapper, calcite]

requires:
  - phase: 19-v2-geo-synthetic-source-01
    provides: GeoSyntheticSourceVO with bbox/center arrays and nested sample/output blocks
provides:
  - GeoSyntheticRequestMapper VO-to-GeoGenerationRequest translation with array expansion
  - Unit tests for four modes and invalid-config source-scoped errors
affects: [19-v2-geo-synthetic-source-03]

tech-stack:
  added: []
  patterns:
    - "Dedicated calcite mapper expands YAML bbox/center arrays to flat GeoGenerationRequest fields (D-04)"
    - "Source-scoped IllegalArgumentException wrapping validate() failures (D-07)"

key-files:
  created:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapper.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapperTests.java
  modified: []

key-decisions:
  - "LINE_SAMPLE tests use BY_SPACING_METERS enum (design doc EVEN shorthand maps to spacing strategy at YAML layer in Phase 20 docs)"
  - "GEO-01 and GEO-03 remain pending until Plan 19-03 Factory/RowSource and regression evidence"

patterns-established:
  - "GeoSyntheticRequestMapper.toRequest(sourceName, vo) validates before returning; no GeoJSON reads in mapper (GEO-03/D-08)"

requirements-completed: []

coverage:
  - id: D1
    description: "GeoSyntheticRequestMapper expands bbox/center arrays and maps scalar fields per D-04"
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapperTests.java#bbox_expandsBboxArrayToFlatFields"
        status: pass
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapperTests.java#circle_expandsCenterArrayAndRadius"
        status: pass
    human_judgment: false
  - id: D2
    description: "Blank boundaryPath/networkPath and validate() failures surface source name + field per D-06/D-07"
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapperTests.java#blankBoundaryPath_throwsWithSourceNameAndField"
        status: pass
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapperTests.java#circleZeroRadius_surfacesValidateErrorThroughMapper"
        status: pass
    human_judgment: false
  - id: D3
    description: "Four-mode mapping happy paths (BOUNDARY_POINTS, LINE_SAMPLE, BBOX, CIRCLE) pass validate()"
    verification:
      - kind: other
        ref: "mvnw-jdk25.ps1 -pl data-generator-calcite -am -Dtest=GeoSyntheticRequestMapperTests -Dsurefire.failIfNoSpecifiedTests=false test"
        status: pass
    human_judgment: false

duration: 22min
completed: 2026-07-30
status: complete
---

# Phase 19 Plan 02: GeoSyntheticRequestMapper Summary

**Dedicated calcite mapper translating GeoSyntheticSourceVO YAML arrays into validated GeoGenerationRequest flat fields with source-scoped errors**

## Performance

- **Duration:** 22 min
- **Started:** 2026-07-30T08:32:00Z
- **Completed:** 2026-07-30T08:54:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added `GeoSyntheticRequestMapper.toRequest(sourceName, vo)` expanding `bbox`/`center` lists and mapping sample/output blocks per D-04
- Enforced blank `boundaryPath`/`networkPath` guards and wrapped `validate()` failures with source name per D-06/D-07
- Added seven JUnit tests covering all four modes plus invalid bbox, blank boundary, and zero radius cases per D-13

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement GeoSyntheticRequestMapper** - `e3f5c8e` (feat)
2. **Task 2: Add GeoSyntheticRequestMapperTests** - `a17ffcc` (test)

## Files Created/Modified

- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapper.java` - VO to GeoGenerationRequest mapper with array expansion and validation
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapperTests.java` - Four-mode and invalid-config unit coverage

## Decisions Made

- Used `BY_SPACING_METERS` in LINE_SAMPLE test (actual enum value; design doc "EVEN" is operator-facing shorthand deferred to Phase 20 docs)
- Left GEO-01/GEO-03 requirement checkboxes pending per orchestrator instruction — Factory/RowSource lands in Plan 19-03

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- `mvn -pl data-generator-calcite` compile/test requires `-am` reactor flag for sibling SNAPSHOT artifacts (same pattern as Plan 19-01)
- PowerShell requires quoted `-D` properties for Surefire test filter when using `-am`

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 19-03 can wire `GeoSyntheticSourceFactory`, `GeoSyntheticRowSource`, and `CoreConfig` bean using this mapper
- `GeoIteratorRequestMapper` unchanged (D-05 satisfied)
- GEO-03 full regression evidence deferred to Plan 19-03 RowSource tests

## Self-Check: PASSED

- FOUND: GeoSyntheticRequestMapper.java
- FOUND: GeoSyntheticRequestMapperTests.java
- FOUND: commit e3f5c8e
- FOUND: commit a17ffcc

---
*Phase: 19-v2-geo-synthetic-source*
*Completed: 2026-07-30*
