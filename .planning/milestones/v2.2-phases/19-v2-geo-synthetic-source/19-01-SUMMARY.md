---
phase: 19-v2-geo-synthetic-source
plan: 01
subsystem: api
tags: [java, template-v2, geo, source-vo, jackson]

requires: []
provides:
  - GeoSyntheticSourceVO with GEO_SYNTHETIC @JsonSubType and geo_synthetic runtime type
  - GeoSyntheticSourceOutputVO independent output knobs per D-09
  - GeoSyntheticSampleVO nested sample block per D-01
affects: [19-v2-geo-synthetic-source-02, 19-v2-geo-synthetic-source-03]

tech-stack:
  added: []
  patterns:
    - "@AutoService(SourceVO.class) + @JsonSubType polymorphic V2 source registration"
    - "Parallel output VO type mirroring GeoJsonSourceOutputVO without coupling"

key-files:
  created:
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoSyntheticSourceOutputVO.java
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoSyntheticSampleVO.java
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoSyntheticSourceVO.java
  modified: []

key-decisions:
  - "GeoSyntheticSourceOutputVO is independent from GeoJsonSourceOutputVO per locked D-09"
  - "seed field initializes to 0L so omitted YAML is deterministic per D-03"
  - "bbox and center remain List<Double> arrays on VO per D-01; mapper expands in Plan 19-02"

patterns-established:
  - "geo_synthetic follows GeoJsonSourceVO registration: @AutoService + @JsonSubType + constructor setType"

requirements-completed: [GEO-01]

coverage:
  - id: D1
    description: "GeoSyntheticSourceOutputVO with format/columnNames/includeProperties independent of GeoJsonSourceOutputVO"
    requirement: GEO-01
    verification:
      - kind: other
        ref: "mvnw-jdk25.ps1 -pl data-generator-common/data-generator-core -am -DskipTests compile"
        status: pass
    human_judgment: false
  - id: D2
    description: "GeoSyntheticSampleVO nested sample block (strategy, spacingMeters) without iterator deps"
    requirement: GEO-01
    verification:
      - kind: other
        ref: "mvnw-jdk25.ps1 -pl data-generator-common/data-generator-core -am -DskipTests compile"
        status: pass
    human_judgment: false
  - id: D3
    description: "GeoSyntheticSourceVO registered as GEO_SYNTHETIC with full YAML field set and seed default 0L"
    requirement: GEO-01
    verification:
      - kind: other
        ref: "mvnw-jdk25.ps1 -pl data-generator-common/data-generator-core -am -DskipTests compile"
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-07-30
status: complete
---

# Phase 19 Plan 01: GeoSynthetic V2 VO Types Summary

**Template V2 `geo_synthetic` configuration VOs in data-generator-core with AutoService registration, D-01 YAML arrays, and D-03 seed default**

## Performance

- **Duration:** 12 min
- **Started:** 2026-07-30T08:08:00Z
- **Completed:** 2026-07-30T08:20:00Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- Added `GeoSyntheticSourceOutputVO` mirroring geojson output knobs as an independent type (D-09)
- Added `GeoSyntheticSampleVO` for nested `sample` block without iterator module imports
- Added `GeoSyntheticSourceVO` extending `SourceVO` with `@JsonSubType("GEO_SYNTHETIC")`, `setType("geo_synthetic")`, full field set, and `seed = 0L` default

## Task Commits

Each task was committed atomically:

1. **Task 1: Create GeoSyntheticSourceOutputVO** - `acd4863` (feat)
2. **Task 2: Create GeoSyntheticSampleVO** - `1b9c568` (feat)
3. **Task 3: Create GeoSyntheticSourceVO** - `459972a` (feat)

## Files Created/Modified

- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoSyntheticSourceOutputVO.java` - Output format knobs for synthetic geo source
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoSyntheticSampleVO.java` - LINE_SAMPLE nested sample config
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoSyntheticSourceVO.java` - Primary V2 source VO for `type: geo_synthetic`

## Decisions Made

None - followed plan and locked CONTEXT decisions D-01, D-02, D-03, D-09 as specified.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Initial `mvn -pl data-generator-core compile` failed on unresolved sibling SNAPSHOT artifacts; resolved by compiling with `-am` reactor flag (standard monorepo pattern).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 19-02 can implement `GeoSyntheticRequestMapper` (VO → `GeoGenerationRequest`)
- Plan 19-03 can add Factory/RowSource and CoreConfig bean registration
- `GeoJsonSourceVO` unchanged (GEO-03 regression prep satisfied)

## Self-Check: PASSED

- FOUND: GeoSyntheticSourceOutputVO.java
- FOUND: GeoSyntheticSampleVO.java
- FOUND: GeoSyntheticSourceVO.java
- FOUND: commit acd4863
- FOUND: commit 1b9c568
- FOUND: commit 459972a

---
*Phase: 19-v2-geo-synthetic-source*
*Completed: 2026-07-30*
