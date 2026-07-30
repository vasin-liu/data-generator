---
phase: 18-geo-generator-modes
plan: 02
subsystem: geo
tags: [java, jts, wgs84, bbox, sampling, minDistance]

requires:
  - phase: 18-01
    provides: GeoGenerationMode.BBOX, bbox fields on GeoGenerationRequest, validateBbox()
provides:
  - BboxPointGenerator seeded uniform bbox sampling with minDistance retry
  - GeoSyntheticGenerator BBOX dispatch to BboxPointGenerator
  - BboxPointGeneratorTests and GeoSyntheticGenerator BBOX integration tests
affects: [18-03, 19-geo-synthetic-v2-source]

tech-stack:
  added: []
  patterns:
    - "Dedicated BboxPointGenerator parallel to BoundaryPointGenerator (D-08)"
    - "Copied retry/isFarEnough pattern with DEFAULT_MAX_RETRIES=10000 (D-10/D-11)"

key-files:
  created:
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/generate/BboxPointGenerator.java
    - data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/BboxPointGeneratorTests.java
  modified:
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java
    - data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java

key-decisions:
  - "Copied BoundaryPointGenerator retry loop rather than extracting shared helper (D-10)"
  - "maxRetries overload exposed for fast retry-exhaustion tests"
  - "CIRCLE left as UnsupportedOperationException until Plan 18-03"

patterns-established:
  - "Mode-specific generator class under org.gensokyo.data.geo.generate with static generate() API"

requirements-completed: [GEO-02]

coverage:
  - id: D1
    description: "BboxPointGenerator produces count points uniformly inside configured WGS84 bbox"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/BboxPointGeneratorTests.java#inDomain_allPointsInsideBbox"
        status: pass
    human_judgment: false
  - id: D2
    description: "Same seed and bbox params produce identical coordinates across runs"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/BboxPointGeneratorTests.java#sameSeed_producesIdenticalPoints"
        status: pass
    human_judgment: false
  - id: D3
    description: "minDistanceMeters uses BoundaryPointGenerator-equivalent retry and exhaustion semantics"
    requirement: GEO-02
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/BboxPointGeneratorTests.java#minDistanceTooLarge_throwsRuntimeExceptionOnRetryExhaustion"
        status: pass
    human_judgment: false
  - id: D4
    description: "GeoSyntheticGenerator.generatePoints dispatches BBOX to BboxPointGenerator end-to-end"
    requirement: GEO-02
    verification:
      - kind: integration
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java#bboxModeProducesRequestedCount"
        status: pass
    human_judgment: false
  - id: D5
    description: "Existing boundary and line sample tests remain green"
    requirement: GEO-02
    verification:
      - kind: integration
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java"
        status: pass
    human_judgment: false

duration: 10min
completed: 2026-07-30
status: complete
---

# Phase 18 Plan 02: BboxPointGenerator Summary

**Seeded uniform BBOX point synthesis with minDistance retry wired into GeoSyntheticGenerator and 38/38 geo module tests green**

## Performance

- **Duration:** 10 min
- **Started:** 2026-07-30T12:20:00Z
- **Completed:** 2026-07-30T12:30:00Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Created `BboxPointGenerator` with uniform lon/lat sampling in `[minLon,maxLon]×[minLat,maxLat]` using `new Random(seed)` per D-06
- Implemented minDistance spacing via `GeoHaversine` with `DEFAULT_MAX_RETRIES=10_000` matching `BoundaryPointGenerator` per D-02, D-10, D-11
- Wired `GeoSyntheticGenerator.generatePointsInternal` BBOX case to delegate bbox fields, count, minDistance, and seed per D-09
- Added 5 unit tests (`BboxPointGeneratorTests`) and 2 integration tests (`GeoSyntheticGeneratorTests`) for in-domain, reproducibility, and retry exhaustion

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: BboxPointGenerator tests** - `be5155d` (test)
2. **Task 1 GREEN: BboxPointGenerator implementation** - `c8adeb7` (feat)
3. **Task 2: Wire BBOX dispatch in GeoSyntheticGenerator** - `f68cdf5` (feat)
4. **Task 3: GeoSyntheticGenerator BBOX integration tests** - `522b83d` (test)

**Plan metadata:** `847b6cf` (docs: complete plan)

## Files Created/Modified

- `data-generator-geo/src/main/java/org/gensokyo/data/geo/generate/BboxPointGenerator.java` - Seeded uniform bbox sampling with minDistance retry
- `data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/BboxPointGeneratorTests.java` - Unit tests (in-domain, reproducibility, exhaustion, spacing)
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java` - BBOX dispatch; CIRCLE still throws UnsupportedOperationException
- `data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java` - BBOX integration tests with Guangzhou-area bbox

## Decisions Made

- Copied small retry/isFarEnough pattern from BoundaryPointGenerator instead of extracting shared helper (per D-10)
- Exposed maxRetries overload on generate() for fast retry-exhaustion test without waiting 10_000 iterations
- Left CIRCLE mode as UnsupportedOperationException stub for Plan 18-03

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 18-03 can implement `CirclePointGenerator` and wire CIRCLE dispatch
- BBOX generator path is complete and validated end-to-end through `GeoSyntheticGenerator`

## Self-Check: PASSED

- FOUND: data-generator-geo/src/main/java/org/gensokyo/data/geo/generate/BboxPointGenerator.java
- FOUND: data-generator-geo/src/test/java/org/gensokyo/data/geo/generate/BboxPointGeneratorTests.java
- FOUND: commit be5155d
- FOUND: commit c8adeb7
- FOUND: commit f68cdf5
- FOUND: commit 522b83d

---
*Phase: 18-geo-generator-modes*
*Completed: 2026-07-30*
