---
phase: 19-v2-geo-synthetic-source
verified: 2026-07-30T10:08:00Z
status: passed
score: 18/18 must-haves verified
behavior_unverified: 0
overrides_applied: 0
deferred:
  - truth: "Full TemplateV2Runner pipeline IT for geo_synthetic four modes"
    addressed_in: "Phase 20"
    evidence: "ROADMAP Phase 20 SC1; CONTEXT D-14 explicitly deferred"
  - truth: "Operator docs distinguishing geo_synthetic vs geojson (GEO-04)"
    addressed_in: "Phase 20"
    evidence: "ROADMAP Phase 20 SC2; REQUIREMENTS.md GEO-04 mapped to Phase 20"
  - truth: "P1 test-matrix row geo-synthetic linkage (TEST-10)"
    addressed_in: "Phase 20"
    evidence: "ROADMAP Phase 20 SC3; REQUIREMENTS.md TEST-10 mapped to Phase 20"
---

# Phase 19: V2 Geo Synthetic Source Verification Report

**Phase Goal:** Expose synthesis as Template V2 `type: geo_synthetic` — VO + Factory + RowSource + CoreConfig; path assets; `geojson` untouched.

**Verified:** 2026-07-30T10:08:00Z  
**Status:** passed  
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `GeoSyntheticSourceVO` + `GeoSyntheticSourceFactory` + `GeoSyntheticRowSource` registered like `GeoJsonSourceFactory` (ROADMAP SC1, D-11) | ✓ VERIFIED | `CoreConfig.geoSyntheticSourceFactory()` with `@ConditionalOnMissingBean`; `GeoSyntheticSourceFactory` implements `V2SourceFactory`; mirrors `geoJsonSourceFactory` pattern |
| 2 | All four modes produce `Row`/`RowSchema` (ROADMAP SC2, D-13) | ✓ VERIFIED | `GeoSyntheticRowSourceTests`: BOUNDARY_POINTS (6 rows + lon/lat schema), LINE_SAMPLE (non-empty), BBOX (5 rows), CIRCLE (4 rows) — all pass |
| 3 | Paths resolve only via `GeoResourceResolver`; no upload API (ROADMAP SC3, GEO-03) | ✓ VERIFIED | Mapper/RowSource do not read files; `GeoSyntheticGenerator.generateRows` uses resolver; no upload/asset-id references in Phase 19 files |
| 4 | `type: geojson` behavior unchanged — regression green (ROADMAP SC4, D-15) | ✓ VERIFIED | `GeoJsonRowSourceTests` (3 tests) + `TemplateV2RunnerGeoSourceTests` (7 tests) pass; `GeoJsonSourceFactory`/`GeoJsonRowSource`/`GeoJsonSourceVO` not modified in Phase 19 commits |
| 5 | Invalid config throws `IllegalArgumentException` with source name + field (ROADMAP SC5, D-06/D-07) | ✓ VERIFIED | `GeoSyntheticRequestMapperTests`: blank boundaryPath, invalid bbox, zero radius; messages contain source name `pts` and field names |
| 6 | VO YAML shape: mode, count, seed, paths, sample, output, bbox/center arrays (D-01) | ✓ VERIFIED | `GeoSyntheticSourceVO` declares all design fields as `List<Double>` bbox/center arrays |
| 7 | `@JsonSubType("GEO_SYNTHETIC")` + runtime type `geo_synthetic` (D-02) | ✓ VERIFIED | Constructor `setType("geo_synthetic")`; `@AutoService(SourceVO.class)` SPI registration |
| 8 | Omitted seed defaults to `0L` (D-03) | ✓ VERIFIED | Field initializer `private long seed = 0L` in `GeoSyntheticSourceVO` |
| 9 | Independent `GeoSyntheticSourceOutputVO` (D-09) | ✓ VERIFIED | Separate class; does not import/extend `GeoJsonSourceOutputVO` |
| 10 | Mapper expands bbox/center arrays to flat request fields (D-04) | ✓ VERIFIED | `GeoSyntheticRequestMapper.expandBbox` / `expandCenter`; tests `bbox_expandsBboxArrayToFlatFields`, `circle_expandsCenterArrayAndRadius` |
| 11 | Dedicated calcite mapper; V1 `GeoIteratorRequestMapper` unchanged (D-05) | ✓ VERIFIED | `GeoSyntheticRequestMapper` in calcite; last commit on `GeoIteratorRequestMapper` predates Phase 19 (`669a107`) |
| 12 | Declared schema honored; else `GeoRowSchemaSupport` infers (D-10) | ✓ VERIFIED | Constructor: `source.getSchema() != null ? source.getSchema() : GeoRowSchemaSupport.schemaForGeoRows(...)`; inference exercised in boundary test |
| 13 | Eager finite row materialization in constructor (D-12) | ✓ VERIFIED | `GeoSyntheticRowSource` builds `rows`/`schema` in constructor via `GeoSyntheticGenerator.generateRows` |
| 14 | Unreadable GeoJSON path fails with source name + path (D-08) | ✓ VERIFIED | `GeoSyntheticRowSourceTests.invalidPathFailsWithSourceAndPath` — message contains `missing_src` and `classpath:geo/missing.geojson` |
| 15 | Factory supports `GeoSyntheticSourceVO` and creates `GeoSyntheticRowSource` (GEO-01) | ✓ VERIFIED | `GeoSyntheticSourceFactoryTests`: supports true for synthetic, false for geojson; create returns `GeoSyntheticRowSource` |
| 16 | `GeoSyntheticRequestMapper` calls `validate()` after mapping (D-06) | ✓ VERIFIED | `request.validate()` in try/catch with source-scoped rethrow |
| 17 | Blank boundaryPath/networkPath guards for required modes (D-06) | ✓ VERIFIED | `enforceModePaths` in mapper; `blankBoundaryPath_throwsWithSourceNameAndField` test |
| 18 | Phase 19 test boundary: Factory/RowSource/mapping only — no TemplateV2Runner geo_synthetic IT (D-14) | ✓ VERIFIED | No new `TemplateV2Runner` geo_synthetic pipeline IT added; existing `TemplateV2RunnerGeoSourceTests` covers geojson only |

**Score:** 18/18 truths verified (0 present, behavior-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | Full `TemplateV2Runner` pipeline IT for four geo_synthetic modes | Phase 20 | CONTEXT D-14; ROADMAP Phase 20 SC1 |
| 2 | Operator docs `geo_synthetic` vs `geojson` (GEO-04) | Phase 20 | ROADMAP Phase 20 SC2 |
| 3 | P1 matrix `geo-synthetic` row (TEST-10) | Phase 20 | ROADMAP Phase 20 SC3 |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `GeoSyntheticSourceVO.java` | V2 geo_synthetic config model | ✓ VERIFIED | Exists, substantive (64 lines), SPI wired |
| `GeoSyntheticSourceOutputVO.java` | Independent output knobs | ✓ VERIFIED | Exists, no GeoJson coupling |
| `GeoSyntheticSampleVO.java` | Nested sample block | ✓ VERIFIED | strategy + spacingMeters |
| `GeoSyntheticRequestMapper.java` | VO → GeoGenerationRequest | ✓ VERIFIED | `toRequest` + validate |
| `GeoSyntheticSourceFactory.java` | V2SourceFactory | ✓ VERIFIED | supports/create wired |
| `GeoSyntheticRowSource.java` | Eager RowSource | ✓ VERIFIED | generateRows + schema |
| `CoreConfig.java` | geoSyntheticSourceFactory bean | ✓ VERIFIED | `@ConditionalOnMissingBean` at line 134 |
| `GeoSyntheticRequestMapperTests.java` | Mapping unit tests | ✓ VERIFIED | 7 tests pass |
| `GeoSyntheticRowSourceTests.java` | Four-mode integration | ✓ VERIFIED | 5 tests pass |
| `GeoSyntheticSourceFactoryTests.java` | Factory smoke | ✓ VERIFIED | 3 tests pass |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `CoreConfig` | `GeoSyntheticSourceFactory` | `@Bean geoSyntheticSourceFactory` | ✓ WIRED | Returns `new GeoSyntheticSourceFactory()` |
| `GeoSyntheticRowSource` | `GeoSyntheticGenerator` | `generateRows` after mapper | ✓ WIRED | Constructor line 50 |
| `GeoSyntheticSourceVO` | `SourceVO` SPI | `@AutoService(SourceVO.class)` | ✓ WIRED | Polymorphic registration |
| `GeoSyntheticRequestMapper` | `GeoGenerationRequest` | `validate()` | ✓ WIRED | Post-mapping validation |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Phase 19 geo test bundle | `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am "-Dtest=GeoSyntheticRequestMapperTests,GeoSyntheticRowSourceTests,GeoSyntheticSourceFactoryTests,GeoJsonRowSourceTests,TemplateV2RunnerGeoSourceTests" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 25 tests, 0 failures, BUILD SUCCESS | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| GEO-01 | 19-01, 19-02, 19-03 | V2 `geo_synthetic` source via Factory → RowSource (Phase 19 scope) | ✓ SATISFIED | Factory/RowSource/tests complete; full TemplateV2Runner pipeline IT deferred Phase 20 per D-14 |
| GEO-03 | 19-02, 19-03 | Path-only GeoJSON via GeoResourceResolver; geojson unchanged | ✓ SATISFIED | No upload API; geojson regression 10/10 tests green |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| — | — | None | — | No TBD/FIXME/stub markers in Phase 19 files |

### Gaps Summary

None. Phase 19 goal achieved at the Factory → RowSource layer with four-mode test evidence, path-only asset resolution, and geojson regression green. TemplateV2Runner pipeline IT, operator docs, and P1 matrix linkage are correctly deferred to Phase 20 (D-14, GEO-04, TEST-10).

---

_Verified: 2026-07-30T10:08:00Z_  
_Verifier: Claude (gsd-verifier)_
