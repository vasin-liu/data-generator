---
phase: 22-console-map-geo-synthetic-editor
plan: 01
subsystem: api
tags: [geo, preview, console, GeoResourceResolver, GeoSyntheticGenerator]

requires:
  - phase: 21-geo-asset-registry-runtime-resolution
    provides: GeoAssetResolver / GeoResourceResolver asset: spine and ConsoleGeoAssetController CRUD
provides:
  - POST /api/console/geo-assets/preview/location (application/geo+json via GeoResourceResolver)
  - POST /api/console/geo-assets/preview/synthetic (capped ≤500 via GeoSyntheticGenerator)
  - contentType on GeoAssetSummaryView from GeoAssetPO
affects:
  - 22-02 console map + geo client wiring
  - 22-03 geo_synthetic editor asset picker

tech-stack:
  added: []
  patterns:
    - "Console preview reuses Phase 21 resolve spine (GeoResourceResolver.readUtf8 + GeoAssetResolver)"
    - "Synthetic preview maps via GeoSyntheticRequestMapper then GeoSyntheticGenerator.generateRows"
    - "Hard maxCount ≤ 500 reject-before-generate for honesty/DoS"

key-files:
  created:
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/GeoPreviewLocationRequest.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/GeoSyntheticPreviewRequest.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/GeoSyntheticPreviewView.java
    - data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetPreviewIT.java
    - data-generator-service/src/test/resources/geo/preview-point.geojson
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/geo/GeoAssetService.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleGeoAssetController.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/GeoAssetSummaryView.java
    - data-generator-service/src/test/java/org/gensokyo/data/geo/GeoAssetServiceTests.java

key-decisions:
  - "Reject maxCount > 500 with IAE naming the cap (no silent clamp)"
  - "preview/location returns raw application/geo+json like GET /{id}/geojson; synthetic returns R envelope"
  - "No second resolve spine — GeoAssetService implements GeoAssetResolver and passes this into resolver/generator"

patterns-established:
  - "Preview DTOs under api.console.dto; service methods on GeoAssetService; endpoints on existing ConsoleGeoAssetController"

requirements-completed: [GEO-12]

coverage:
  - id: D1
    description: "Classpath/path/asset location preview via GeoResourceResolver.readUtf8"
    requirement: GEO-12
    verification:
      - kind: unit
        ref: data-generator-service/src/test/java/org/gensokyo/data/geo/GeoAssetServiceTests.java#previewLocation_classpathFixture_returnsFeatureCollectionUtf8
        status: pass
      - kind: integration
        ref: data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetPreviewIT.java#previewLocation_classpathFixture_returnsApplicationGeoJson
        status: pass
    human_judgment: false
  - id: D2
    description: "Capped synthetic point preview (maxCount ≤ 500) via GeoSyntheticGenerator"
    requirement: GEO-12
    verification:
      - kind: unit
        ref: data-generator-service/src/test/java/org/gensokyo/data/geo/GeoAssetServiceTests.java#previewSynthetic_maxCountOverCap_throwsNamingCap
        status: pass
      - kind: integration
        ref: data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetPreviewIT.java#previewSynthetic_maxCount501_returnsBadRequest
        status: pass
    human_judgment: false
  - id: D3
    description: "GeoAssetSummaryView.contentType mapped from GeoAssetPO"
    requirement: GEO-12
    verification:
      - kind: unit
        ref: data-generator-service/src/test/java/org/gensokyo/data/geo/GeoAssetServiceTests.java#summaryView_from_includesContentTypeWhenPresentOnPo
        status: pass
    human_judgment: false

duration: ~45min
completed: 2026-08-06
status: complete
---

# Phase 22 Plan 01: Console geo preview APIs Summary

**Server preview helpers for MapLibre underlays and capped synthetic points reuse the Phase 21 GeoResourceResolver / GeoAssetResolver spine — no parallel client resolve.**

## Performance

- **Duration:** ~45 min
- **Started:** 2026-08-06T13:46:11Z
- **Completed:** 2026-08-06T14:30:00Z
- **Tasks:** 2/2
- **Files modified:** 9

## Accomplishments

- `POST /api/console/geo-assets/preview/location` resolves classpath/filesystem/`asset:` via `GeoResourceResolver.readUtf8(location, this)` and returns raw `application/geo+json`.
- `POST /api/console/geo-assets/preview/synthetic` maps console mode fields through `GeoSyntheticRequestMapper`, calls `GeoSyntheticGenerator.generateRows`, rejects `maxCount > 500`, returns seed + `effectiveSampleCount` + FeatureCollection in `R`.
- List DTO exposes stored `contentType` from `GeoAssetPO` (D-02); no invented `byteSize`.

## Task Commits

| Task | Name | Commit | Type |
|------|------|--------|------|
| 1 RED | Failing GeoAssetServiceTests + DTOs/stubs | `885e575` | test |
| 1 GREEN | Preview service methods + contentType mapping | `cbe697e` | feat |
| 2 RED | Failing ConsoleGeoAssetPreviewIT | `1b87297` | test |
| 2 GREEN | Controller preview endpoints | `556df24` | feat |

## Deviations from Plan

None - plan executed exactly as written.

## TDD Gate Compliance

- RED commits: `885e575`, `1b87297`
- GREEN commits: `cbe697e`, `556df24`
- No REFACTOR commit needed

## Threat Flags

None — endpoints stay under existing `/api/console/geo-assets` with IAE→400 advice and maxCount hard cap (T-22-01..03 mitigations applied).

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: GeoPreviewLocationRequest.java, GeoSyntheticPreviewRequest.java, GeoSyntheticPreviewView.java
- FOUND: ConsoleGeoAssetPreviewIT.java, preview-point.geojson
- FOUND: commits 885e575, cbe697e, 1b87297, 556df24, docs 34e474f
- VERIFY: GeoAssetServiceTests + ConsoleGeoAssetPreviewIT + ConsoleGeoAssetControllerIT Surefire exit 0
- VERIFY: no files under `data-generator-console-web/` modified
