---
phase: 18-geo-generator-modes
plan: 01
subsystem: geo
tags: [java, jts, wgs84, validation, bbox, circle]

requires: []
provides:
  - GeoGenerationMode BBOX and CIRCLE enum constants
  - GeoGenerationRequest flat WGS84 bbox/circle fields with validateBbox/validateCircle
  - GeoGenerationRequestValidationTests proving fail-fast field-named errors
affects: [18-02, 18-03, 19-geo-synthetic-v2-source]

tech-stack:
  added: []
  patterns:
    - "Mode-specific validate() branches with IllegalArgumentException field names (D-11)"
    - "Explicit seed on request; 0 valid, no wall-clock fallback (D-06)"

key-files:
  created:
    - data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoGenerationRequestValidationTests.java
  modified:
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoGenerationMode.java
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoGenerationRequest.java
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java

key-decisions:
  - "Flat bboxMinLon/bboxMaxLon/centerLon fields for Phase 19 VO mapping"
  - "GeoSyntheticGenerator switch stub throws UnsupportedOperationException until Plan 18-02 wires sampling"

patterns-established:
  - "BBOX/CIRCLE validation in GeoGenerationRequest.validate() before any sampling runs"

requirements-completed: [GEO-02]

coverage:
  - id: D1
    description: "GeoGenerationMode exposes BBOX and CIRCLE alongside boundary/line modes"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java"
        status: pass
    human_judgment: false
  - id: D2
    description: "GeoGenerationRequest carries bbox/circle geometry fields mappable to Phase 19 VO"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoGenerationRequestValidationTests.java#bboxHappyPathAcceptsValidConfig"
        status: pass
    human_judgment: false
  - id: D3
    description: "Illegal BBOX/CIRCLE config fails validate() with IllegalArgumentException naming the field"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoGenerationRequestValidationTests.java"
        status: pass
    human_judgment: false
  - id: D4
    description: "seed is always taken from request.getSeed() with 0 valid (no wall-clock fallback)"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoGenerationRequestValidationTests.java#seedZeroIsValidForBbox"
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-07-30
status: complete
---

# Phase 18 Plan 01: Geo Generator Contracts Summary

**BBOX and CIRCLE modes added to geo generator contracts with strict WGS84 validation and 15 unit tests before sampling dispatch**

## Performance

- **Duration:** 12 min
- **Started:** 2026-07-30T12:01:00Z
- **Completed:** 2026-07-30T12:13:00Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Extended `GeoGenerationMode` with `BBOX` and `CIRCLE` appended after existing modes
- Added flat WGS84 fields (`bboxMinLon`…`bboxMaxLat`, `centerLon`/`centerLat`/`radiusMeters`) to `GeoGenerationRequest`
- Implemented `validateBbox()` and `validateCircle()` per D-03, D-05, D-06, D-11 with field-named `IllegalArgumentException` messages
- Added `GeoGenerationRequestValidationTests` (15 cases) covering happy paths, degenerate bbox, invalid circle radius/bounds, and seed=0

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend GeoGenerationMode with BBOX and CIRCLE** - `59b3248` (feat)
2. **Task 2: Add bbox and circle fields to GeoGenerationRequest** - `9194481` (feat)
3. **Task 3: Implement BBOX and CIRCLE validation rules** - `2938299` (test)

**Plan metadata:** pending (docs commit)

## Files Created/Modified

- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoGenerationMode.java` - Four-mode enum with updated Javadoc
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoGenerationRequest.java` - Bbox/circle fields, validate branches, seed Javadoc
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java` - Exhaustive switch stub for new modes
- `data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoGenerationRequestValidationTests.java` - Validation unit tests

## Decisions Made

- Used flat field names on `GeoGenerationRequest` for clean Phase 19 VO mapping (per plan discretion)
- Documented explicit seed requirement on request; validation accepts seed=0 without special casing

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added GeoSyntheticGenerator switch arms for BBOX/CIRCLE**
- **Found during:** Task 1 (Extend GeoGenerationMode)
- **Issue:** Java exhaustive switch on enum fails to compile when new constants are added; plan said not to touch generator yet
- **Fix:** Added `case BBOX, CIRCLE -> throw new UnsupportedOperationException(...)` so existing tests compile and pass; sampling deferred to Plan 18-02
- **Files modified:** `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java`
- **Verification:** `GeoSyntheticGeneratorTests` green (31/31 module tests)
- **Committed in:** `59b3248`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Minimal compile-time stub only; no sampling behavior added. Required for Task 1 verification.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 18-02 can implement `BboxPointGenerator`, `CirclePointGenerator`, and wire `GeoSyntheticGenerator` dispatch
- Request validation contract is stable for Phase 19 VO mapping

## Self-Check: PASSED

- FOUND: data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoGenerationMode.java
- FOUND: data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoGenerationRequest.java
- FOUND: data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoGenerationRequestValidationTests.java
- FOUND: commit 59b3248
- FOUND: commit 9194481
- FOUND: commit 2938299

---
*Phase: 18-geo-generator-modes*
*Completed: 2026-07-30*
