---
phase: 19-v2-geo-synthetic-source
plan: 03
subsystem: api
tags: [java, template-v2, geo, calcite, rowsource, spring]

requires:
  - phase: 19-v2-geo-synthetic-source-02
    provides: GeoSyntheticRequestMapper VO-to-GeoGenerationRequest translation
provides:
  - GeoSyntheticSourceFactory + GeoSyntheticRowSource Factory→RowSource path (GEO-01)
  - CoreConfig geoSyntheticSourceFactory bean registration (D-11)
  - Four-mode RowSource integration tests + geojson regression green (GEO-03, D-13/D-15)
affects: [20-pipeline-proof-docs-p1]

tech-stack:
  added: []
  patterns:
    - "Eager GeoSyntheticRowSource constructor mirrors GeoJsonRowSource per D-12"
    - "GeoResourceResolver IAE wrapped with source name + path for D-08 parity with IOException"

key-files:
  created:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticSourceFactory.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticRowSource.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRowSourceTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticSourceFactoryTests.java
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java

key-decisions:
  - "GeoResourceResolver missing-classpath failures are IllegalArgumentException; RowSource wraps them with D-08 message format"
  - "Phase 19 GEO-01 success criteria met at Factory→RowSource path; TemplateV2Runner IT deferred Phase 20 per D-14"

patterns-established:
  - "geo_synthetic registered parallel to geojson: V2SourceFactory bean + eager RowSource materialization"

requirements-completed: [GEO-01, GEO-03]

coverage:
  - id: D1
    description: "GeoSyntheticSourceFactory supports GeoSyntheticSourceVO and creates GeoSyntheticRowSource"
    requirement: GEO-01
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticSourceFactoryTests.java"
        status: pass
    human_judgment: false
  - id: D2
    description: "GeoSyntheticRowSource four-mode row materialization with schema inference"
    requirement: GEO-01
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRowSourceTests.java"
        status: pass
    human_judgment: false
  - id: D3
    description: "CoreConfig geoSyntheticSourceFactory bean with ConditionalOnMissingBean"
    requirement: GEO-01
    verification:
      - kind: other
        ref: "mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests -Dskip.console.frontend=true compile"
        status: pass
    human_judgment: false
  - id: D4
    description: "geojson regression green; path-only IO unchanged (GEO-03)"
    requirement: GEO-03
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoJsonRowSourceTests.java"
        status: pass
      - kind: integration
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerGeoSourceTests.java"
        status: pass
    human_judgment: false
  - id: D5
    description: "Unreadable GeoJSON path fails with source name and path in message (D-08)"
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRowSourceTests.java#invalidPathFailsWithSourceAndPath"
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-07-30
status: complete
---

# Phase 19 Plan 03: GeoSynthetic Factory/RowSource Summary

**Template V2 `geo_synthetic` Factory→RowSource→rows path with CoreConfig registration and four-mode calcite integration tests**

## Performance

- **Duration:** 45 min
- **Started:** 2026-07-30T09:05:00Z
- **Completed:** 2026-07-30T09:50:00Z
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments

- Added `GeoSyntheticSourceFactory` and `GeoSyntheticRowSource` eagerly materializing rows via `GeoSyntheticGenerator.generateRows` per D-10/D-12
- Registered `geoSyntheticSourceFactory` Spring bean in `CoreConfig` mirroring `geoJsonSourceFactory` per D-11
- Added four-mode `GeoSyntheticRowSourceTests` and factory smoke tests; geojson regression suite green per D-15
- Wrapped `GeoResourceResolver` classpath-not-found `IllegalArgumentException` with source-scoped path message per D-08

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement GeoSyntheticSourceFactory and GeoSyntheticRowSource** - `2268a9e` (feat)
2. **Task 2: Register geoSyntheticSourceFactory in CoreConfig** - `ef4d89d` (feat)
3. **Task 3: Add RowSource/Factory tests and run geo regression suite** - `4b32927` (test)

## Files Created/Modified

- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticSourceFactory.java` - V2SourceFactory for `geo_synthetic`
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticRowSource.java` - Eager row/schema materialization
- `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java` - `geoSyntheticSourceFactory` bean
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRowSourceTests.java` - Four-mode + invalid path tests
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticSourceFactoryTests.java` - supports/create smoke tests

## Decisions Made

- GEO-01 Phase 19 criteria satisfied at Factory→RowSource layer; full `TemplateV2Runner` pipeline IT remains Phase 20 (D-14)
- GEO-03 marked complete: path-only resolution via existing `GeoResourceResolver`; `geojson` source and regression tests unchanged

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Wrap GeoResourceResolver IllegalArgumentException for missing classpath paths**
- **Found during:** Task 3
- **Issue:** Missing GeoJSON paths throw `IllegalArgumentException` from `GeoResourceResolver`, not `IOException`; D-08 message with source name/path was not produced
- **Fix:** Catch `IllegalArgumentException` in `GeoSyntheticRowSource` constructor and wrap with `readFailure()` when path-based mode
- **Files modified:** `GeoSyntheticRowSource.java`
- **Commit:** `4b32927`

## TDD Gate Compliance

Task 3 had `tdd="true"` but implementation landed in Task 1 per plan structure — tests passed on first run (no separate RED commit). Acceptable for split-task plan; GREEN evidence in `4b32927`.

## Issues Encountered

- PowerShell requires quoted `-pl` and `-D` Maven arguments when using comma-separated module lists
- Full reactor compile with console-web frontend is slow; use `-Dskip.console.frontend=true` for service-only compile

## User Setup Required

None.

## Next Phase Readiness

- Phase 20 can add `TemplateV2Runner` geo_synthetic pipeline IT, operator docs (GEO-04), and P1 matrix row (TEST-10)
- `GeoJsonSourceFactory` / `GeoJsonRowSource` untouched — GEO-03 regression satisfied

## Self-Check: PASSED

- FOUND: GeoSyntheticSourceFactory.java
- FOUND: GeoSyntheticRowSource.java
- FOUND: GeoSyntheticRowSourceTests.java
- FOUND: GeoSyntheticSourceFactoryTests.java
- FOUND: commit 2268a9e
- FOUND: commit ef4d89d
- FOUND: commit 4b32927

---
*Phase: 19-v2-geo-synthetic-source*
*Completed: 2026-07-30*
