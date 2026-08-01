---
phase: 21-geo-asset-registry-runtime-resolution
plan: 02
subsystem: api
tags: [geojson, calcite, template-v2, asset-resolution, jts]

requires:
  - phase: 21-01
    provides: GeoAssetResolver contract and GeoAssetService.resolveUtf8
provides:
  - VO asset-id fields on geo_synthetic and geojson sources
  - asset: resolution spine in GeoResourceResolver / GeoJsonLoader / GeoSyntheticGenerator
  - GeoSyntheticRequestMapper and GeoJsonLocationMapper normalization (D-01..D-03)
  - GeoAssetResolver injection via CoreConfig into geo source factories
affects:
  - 21-03-delete-governance
  - 22-console-map-ui

tech-stack:
  added: []
  patterns:
    - "Dedicated asset-id fields normalize to asset:{uuid} at mapper layer"
    - "ObjectProvider<GeoAssetResolver> in CoreConfig mirrors AiSourceFactory optional wiring"
    - "GeoAssetResolver interface colocated in data-generator-geo for geo-module spine access"

key-files:
  created:
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoAssetResolver.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoJsonLocationMapper.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoJsonLocationMapperTests.java
  modified:
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoSyntheticSourceVO.java
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoJsonSourceVO.java
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/io/GeoResourceResolver.java
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/io/GeoJsonLoader.java
    - data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapper.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticSourceFactory.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoJsonSourceFactory.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticRowSource.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoJsonRowSource.java
    - data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java
    - data-generator-geo/src/test/java/org/gensokyo/data/geo/io/GeoJsonLoaderTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapperTests.java
  removed:
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/geo/GeoAssetResolver.java

key-decisions:
  - "Moved GeoAssetResolver from data-generator-core to data-generator-geo so geo module owns resolution spine without Spring"
  - "Dual path+asset-id binding fails fast with IAE naming source and field (D-02) rather than silent asset-id preference"

patterns-established:
  - "Mapper layer converts boundaryAssetId/networkAssetId/assetId to asset:{uuid} before GeoResourceResolver"
  - "Null GeoAssetResolver preserves v2.2 classpath-only geo source IT behavior"

requirements-completed: [GEO-10, GEO-11]

coverage:
  - id: D1
    description: "Template V2 geo sources accept dedicated asset-id fields normalized to asset:{uuid}"
    requirement: GEO-10
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRequestMapperTests.java"
        status: pass
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoJsonLocationMapperTests.java"
        status: pass
    human_judgment: false
  - id: D2
    description: "Execute path resolves asset:{id} via GeoAssetResolver through geo factories and CoreConfig"
    requirement: GEO-11
    verification:
      - kind: unit
        ref: "data-generator-geo/src/test/java/org/gensokyo/data/geo/io/GeoJsonLoaderTests.java"
        status: pass
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoSyntheticRowSourceTests.java"
        status: pass
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoJsonRowSourceTests.java"
        status: pass
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerGeoSourceTests.java"
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-08-01
status: complete
---

# Phase 21 Plan 02 Summary

**Template V2 geo sources resolve `asset:{uuid}` on the execute path via GeoResourceResolver spine, mapper normalization, and CoreConfig-injected GeoAssetResolver — classpath v2.2 regression preserved**

## Performance

- **Duration:** ~45 min (including verify build)
- **Started:** 2026-08-01T10:10:00+08:00
- **Completed:** 2026-08-01T10:28:00+08:00
- **Tasks:** 4
- **Files modified:** 18

## Accomplishments

- Added `boundaryAssetId` / `networkAssetId` on `GeoSyntheticSourceVO` and `assetId` on `GeoJsonSourceVO` (path/classpath unchanged per GEO-03)
- Extended `GeoResourceResolver`, `GeoJsonLoader`, and `GeoSyntheticGenerator` with `asset:` branch and resolver-aware overloads
- `GeoSyntheticRequestMapper` and new `GeoJsonLocationMapper` normalize asset-id fields and fail fast when path and asset-id both set (D-02)
- `GeoSyntheticSourceFactory` / `GeoJsonSourceFactory` and row sources accept nullable `GeoAssetResolver`; `CoreConfig` registers bean and wires via `ObjectProvider`
- Moved `GeoAssetResolver` interface from core to `data-generator-geo` module for spine access without Spring imports

## Task Commits

1. **Tasks 1–4: Runtime asset resolution spine + factory wiring** — (see commit hash below)

## Files Created/Modified

- `GeoAssetResolver.java` (geo module) — resolution contract relocated from core
- `GeoResourceResolver.java` / `GeoJsonLoader.java` / `GeoSyntheticGenerator.java` — `asset:` UTF-8 loading
- `GeoSyntheticSourceVO.java` / `GeoJsonSourceVO.java` — asset-id binding fields
- `GeoSyntheticRequestMapper.java` / `GeoJsonLocationMapper.java` — normalization and dual-field validation
- Geo source factories, row sources, `CoreConfig.java` — resolver injection on execute path
- Mapper/loader/row-source tests — 31 tests green in verify command

## Decisions Made

- Relocated `GeoAssetResolver` to `data-generator-geo` so loader/generator/resolver share one module-local contract (service still implements via same package name)
- Dual path+asset-id sets throw `IllegalArgumentException` instead of silently preferring asset-id, matching D-02 fail-fast guidance

## Deviations from Plan

**1. GeoAssetResolver package location**
- **Issue:** Plan referenced core-module interface; geo module cannot depend on core for resolver threading
- **Fix:** Moved interface to `data-generator-geo`; removed duplicate from `data-generator-core`
- **Impact:** Same method contract; `GeoAssetService` unchanged (same package `org.gensokyo.data.geo`)

## Issues Encountered

- PowerShell Maven quoting: `-pl` module list and `-Dtest` must be quoted separately
- Full `-am` reactor build ~12 min; calcite geo tests (31) all pass

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 21-03 can add DELETE with template reference scan (409) and delete audit
- Full pipeline IT with uploaded assets deferred to 21-03 per plan scope
- Phase 22 can bind console editor fields to new VO asset-id properties

---
*Phase: 21-geo-asset-registry-runtime-resolution*
*Completed: 2026-08-01*
