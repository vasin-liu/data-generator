# Phase 18: Geo Generator Modes - Context

**Gathered:** 2026-07-30  
**Status:** Ready for planning  
**Mode:** `--auto` (recommended defaults applied in one pass)

<domain>
## Phase Boundary

Deliver **generator-layer** support for `BBOX` and `CIRCLE` modes in `data-generator-geo`, plus validation and unit tests. Existing `BOUNDARY_POINTS` / `LINE_SAMPLE` remain green.

**In scope:** `GeoGenerationMode` extension, `GeoGenerationRequest` fields + validate, `GeoSyntheticGenerator` dispatch, new sampling helpers, unit tests (in-domain, seed reproducibility, illegal config).

**Out of scope this phase:** V2 `GeoSyntheticSourceVO` / Factory / RowSource (Phase 19); pipeline IT / docs / P1 matrix (Phase 20); asset upload; polygon synthesis; console UI.

</domain>

<decisions>
## Implementation Decisions

### BBOX sampling
- **D-01:** Uniform random lon/lat inside `[minLon, maxLon] × [minLat, maxLat]` (inclusive bounds as continuous uniform).
- **D-02:** Optional `minDistanceMeters` with the **same retry semantics** as `BoundaryPointGenerator` (including `DEFAULT_MAX_RETRIES` and throw when exhausted — do not silently return fewer points).
- **D-03:** Degenerate bbox (`minLon >= maxLon` or `minLat >= maxLat`) fails in `validate()` with `IllegalArgumentException` naming the fields.

### CIRCLE sampling
- **D-04:** Area-uniform polar sampling: `r = R * sqrt(u)`, `θ = 2πv` with seeded `Random`; convert local easting/northing meters to degrees using Earth radius and `cos(lat)` at the center; accept only if Haversine distance ≤ `radiusMeters`.
- **D-05:** `radiusMeters <= 0` fails in `validate()`; center lon/lat must be finite and within plausible WGS84 ranges (lon ∈ [-180,180], lat ∈ [-90,90]).

### Seed & determinism
- **D-06:** Always construct `new Random(request.getSeed())` — `seed` is a required long on the request (0 is a valid deterministic seed). No separate “unset → wall-clock” path in Phase 18; callers who want uniqueness pick a seed.
- **D-07:** Same `(mode, geometry params, count, minDistanceMeters, seed)` → identical point coordinates across runs.

### Code layout
- **D-08:** Add dedicated helpers parallel to `BoundaryPointGenerator` — e.g. `BboxPointGenerator` and `CirclePointGenerator` under `org.gensokyo.data.geo.generate` — rather than overloading boundary geometry APIs.
- **D-09:** Extend `GeoSyntheticGenerator.generatePointsInternal` switch for `BBOX` / `CIRCLE`; keep boundary/line paths unchanged.
- **D-10:** Prefer extracting shared “far enough / retry loop” only if duplication is painful; otherwise copy the small retry pattern for clarity (planner may extract if justified).

### Errors
- **D-11:** Config validation → `IllegalArgumentException` (clear field names). Sampling retry exhaustion → match existing boundary behavior (`RuntimeException` with retries message) unless a cleaner shared type already exists — do not invent a new checked exception hierarchy in this phase.

### Claude's Discretion
- Exact field names on `GeoGenerationRequest` (`bboxMinLon`… vs nested type) — prefer readable Java fields that map cleanly to Phase 19 VO.
- Whether tests live in new `*Tests` classes or extend `GeoSyntheticGeneratorTests`.

### Auto log
```
[--auto] Selected all gray areas: BBOX sampling, CIRCLE sampling, Seed & determinism, Code layout, Errors.
[auto] BBOX — Q: "minDistance support?" → Selected: "Same as boundary (retry + fail)" (recommended)
[auto] CIRCLE — Q: "sampling algorithm?" → Selected: "Area-uniform polar + Haversine accept" (recommended / design-locked)
[auto] Seed — Q: "unset seed behavior?" → Selected: "Always use request seed (0 valid)" (recommended)
[auto] Layout — Q: "new classes vs overload boundary?" → Selected: "BboxPointGenerator + CirclePointGenerator" (recommended)
[auto] Errors — Q: "retry exhaustion?" → Selected: "Match BoundaryPointGenerator RuntimeException" (recommended)
```

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone / requirements
- `docs/superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md` — locked milestone design (modes, CIRCLE math, acceptance)
- `.planning/REQUIREMENTS.md` — GEO-02 (generator half for Phase 18)
- `.planning/ROADMAP.md` — Phase 18 goal and success criteria

### Existing geo implementation
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java` — dispatch + row formatting
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoGenerationRequest.java` — validate + count cap
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoGenerationMode.java` — extend enum
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/generate/BoundaryPointGenerator.java` — retry / minDistance pattern
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoHaversine.java` — distance helpers
- `data-generator-geo/src/test/java/org/gensokyo/data/geo/GeoSyntheticGeneratorTests.java` — existing coverage to keep green

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `BoundaryPointGenerator` — retry loop + `minDistanceMeters` + seeded `Random`
- `GeoHaversine.distanceMeters` — CIRCLE acceptance and spacing checks
- `GeoGenerationRequest.validate()` / `MAX_COUNT` — extend for new modes

### Established Patterns
- Mode switch in `generatePointsInternal`
- Fail hard on sampling exhaustion (no silent shortfall)
- Unit tests in `data-generator-geo` module (no Spring)

### Integration Points
- Phase 19 will map V2 VO → this request; keep request API stable and documented
- V1 `GeoIteratorRequestMapper` may later learn BBOX/CIRCLE — **not required** in Phase 18

</code_context>

<specifics>
## Specific Ideas

From approved design: CIRCLE formula `r = R * sqrt(u)`, `θ = 2πv`; BBOX four-tuple validation; path assets deferred to Phase 19+.

</specifics>

<deferred>
## Deferred Ideas

- V2 `geo_synthetic` SourceVO / Factory / RowSource → Phase 19
- TemplateV2Runner IT, docs, P1 matrix → Phase 20
- Shared mapper with `GeoIteratorVO` → Phase 19 (optional)
- GeoJSON upload, common-data CRUD, polygons, console map → backlog beyond v2.2

</deferred>

---

*Phase: 18-Geo Generator Modes*  
*Context gathered: 2026-07-30 (--auto)*
