---
phase: 18-geo-generator-modes
plan: 03
subsystem: geo
tags: [java, jts, wgs84, circle, haversine, sampling, minDistance]

requires:
  - phase: 18-01
    provides: GeoGenerationMode.CIRCLE, circle fields on GeoGenerationRequest, validateCircle()
  - phase: 18-02
    provides: BboxPointGenerator pattern, GeoSyntheticGenerator dispatch pattern
provides:
  - CirclePointGenerator area-uniform polar sampling with Haversine acceptance
  - GeoSyntheticGenerator CIRCLE dispatch completing four-mode generator API
  - CirclePointGeneratorTests and GeoSyntheticGenerator CIRCLE integration tests
affects: [19-geo-synthetic-v2-source, 20-geo-pipeline-it]

tech-stack:
  added: []
  patterns:
    - "Area-uniform polar r=R*sqrt(u), theta=2*pi*v with Haversine gate (D-04)"
    - "Dedicated CirclePointGenerator parallel to BboxPointGenerator (D-08)"
    - "Copied minDistance retry loop with DEFAULT_MAX_RETRIES=10000 (D-10/D-11)"

key-files:
  created:
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/generate/CirclePointGenerator.java
    - data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/CirclePointGeneratorTests.java
  modified:
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java
    - data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java

key-decisions:
  - "Duplicated EARTH_RADIUS_METERS=6_371_000 for meter→degree projection (GeoHaversine constant is private)"
  - "Retry exhaustion test uses 50 m radius with 400 m minDistance (500 m radius fits two 400 m-spaced points)"
  - "Four-mode GeoSyntheticGenerator switch now fully dispatchable per D-09"

patterns-established:
  - "CirclePointGenerator static generate() API matching BboxPointGenerator/BoundaryPointGenerator"

requirements-completed: [GEO-02]

coverage:
  - id: D1
    description: "CirclePointGenerator produces count points inside Haversine radius of center"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/CirclePointGeneratorTests.java#allPointsWithinRadius"
        status: pass
    human_judgment: false
  - id: D2
    description: "Same seed and circle params produce identical coordinates across runs"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/CirclePointGeneratorTests.java#sameSeed_producesIdenticalPoints"
        status: pass
    human_judgment: false
  - id: D3
    description: "minDistanceMeters uses BoundaryPointGenerator-equivalent retry and exhaustion semantics"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/CirclePointGeneratorTests.java#minDistanceTooLarge_throwsRuntimeExceptionOnRetryExhaustion"
        status: pass
    human_judgment: false
  - id: D4
    description: "GeoSyntheticGenerator.generatePoints dispatches CIRCLE to CirclePointGenerator end-to-end"
    requirement: GEO-02
    verification:
      - kind: integration
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java#circleModeProducesRequestedCount"
        status: pass
    human_judgment: false
  - id: D5
    description: "All four GeoGenerationMode values dispatchable; boundary/line/bbox tests remain green"
    requirement: GEO-02
    verification:
      - kind: integration
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java"
        status: pass
    human_judgment: false
  - id: D6
    description: "Full data-generator-geo module test suite green (45 tests)"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: ".\\mvnw-jdk25.ps1 -pl data-generator-geo -am test"
        status: pass
    human_judgment: false

duration: 22min
completed: 2026-07-30
status: complete
---

# Phase 18 Plan 03: CirclePointGenerator Summary

**Area-uniform CIRCLE point synthesis with Haversine acceptance wired into four-mode GeoSyntheticGenerator and 45/45 geo module tests green**

## Performance

- **Duration:** 22 min
- **Started:** 2026-07-30T12:42:00Z
- **Completed:** 2026-07-30T13:04:00Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Created `CirclePointGenerator` with area-uniform polar sampling (`r = R * sqrt(u)`, `θ = 2πv`) and Haversine acceptance gate per D-04
- Implemented minDistance spacing via copied retry loop matching `BoundaryPointGenerator` per D-02, D-10, D-11
- Wired `GeoSyntheticGenerator.generatePointsInternal` CIRCLE case delegating center, radius, count, minDistance, and seed per D-09
- Added 5 unit tests and 2 integration tests; full module regression 45/45 tests pass

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: CirclePointGenerator tests** - `99533b8` (test)
2. **Task 1 GREEN: CirclePointGenerator implementation** - `3369cf0` (feat)
3. **Task 2: Wire CIRCLE dispatch in GeoSyntheticGenerator** - `8d0cef3` (feat)
4. **Task 3: CIRCLE integration tests and full module regression** - `b6c6da8` (test)

**Plan metadata:** `478ef10` (docs: complete plan)

## Files Created/Modified

- `data-generator-geo/src/main/java/org/gensokyo/data/geo/generate/CirclePointGenerator.java` - Seeded area-uniform circle sampling with Haversine gate
- `data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/CirclePointGeneratorTests.java` - Unit tests (in-radius, reproducibility, exhaustion, spacing)
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java` - CIRCLE dispatch; all four modes now reachable
- `data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java` - CIRCLE integration tests with Guangzhou-area center [113.3, 23.1] radius 500 m

## Decisions Made

- Duplicated `EARTH_RADIUS_METERS = 6_371_000` for local projection because `GeoHaversine` constant is private
- Adjusted retry-exhaustion test to 50 m radius (100 m diameter) with 400 m minDistance — original 500 m radius could fit two 400 m-spaced points

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Adjusted minDistance retry-exhaustion test parameters**
- **Found during:** Task 1 GREEN (CirclePointGenerator implementation)
- **Issue:** Test used 500 m radius with 400 m minDistance for 2 points — physically feasible, so no exception thrown
- **Fix:** Changed test to 50 m radius with 400 m minDistance to force retry exhaustion within 5 retries
- **Files modified:** `data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/CirclePointGeneratorTests.java`
- **Verification:** `CirclePointGeneratorTests` 5/5 pass
- **Committed in:** `3369cf0`

---

**Total deviations:** 1 auto-fixed (1 bug in test parameters)
**Impact on plan:** Test-only adjustment; implementation unchanged and matches D-11 semantics.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 18 generator half of GEO-02 complete (BBOX + CIRCLE both dispatchable through `GeoSyntheticGenerator`)
- Phase 19 can map V2 `GeoSyntheticSourceVO` to stable `GeoGenerationRequest` for all four modes
- Pipeline IT proof deferred to Phase 20

## Self-Check: PASSED

- FOUND: data-generator-geo/src/main/java/org/gensokyo/data/geo/generate/CirclePointGenerator.java
- FOUND: data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/CirclePointGeneratorTests.java
- FOUND: commit 99533b8
- FOUND: commit 3369cf0
- FOUND: commit 8d0cef3
- FOUND: commit b6c6da8

---
*Phase: 18-geo-generator-modes*
*Completed: 2026-07-30*
