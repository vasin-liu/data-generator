---
phase: 21-geo-asset-registry-runtime-resolution
plan: 01
subsystem: api
tags: [geojson, jpa, spring-boot, multipart, audit, jts]

requires: []
provides:
  - GeoAssetResolver core interface
  - geo_asset JPA persistence with CLOB storage
  - GeoAssetService upload/list/get/resolveUtf8
  - ConsoleGeoAssetController REST at /api/console/geo-assets
  - Configurable max-bytes/max-features and raised multipart limits
affects:
  - 21-02-runtime-resolution
  - 21-03-delete-governance
  - 22-console-map-ui

tech-stack:
  added: []
  patterns:
    - "SecretResolver-style core interface + service implementation"
    - "UdfArtifactPO-style CLOB entity in metadata DB"
    - "ConsoleUdfController multipart + R envelope (dedicated controller per D-05)"

key-files:
  created:
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/geo/GeoAssetResolver.java
    - data-generator-service/src/main/java/org/gensokyo/data/model/po/GeoAssetPO.java
    - data-generator-service/src/main/java/org/gensokyo/data/repository/GeoAssetRepository.java
    - data-generator-service/src/main/java/org/gensokyo/data/geo/GeoAssetIngestSupport.java
    - data-generator-service/src/main/java/org/gensokyo/data/geo/GeoAssetService.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleGeoAssetController.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/GeoAssetSummaryView.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/GeoAssetUploadView.java
    - data-generator-service/src/test/java/org/gensokyo/data/geo/GeoAssetServiceTests.java
    - data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetControllerIT.java
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/config/DataGeneratorProperties.java
    - data-generator-service/src/main/resources/application.yaml

key-decisions:
  - "GeoJsonLoader validation via temp file in GeoAssetIngestSupport (no geo module API change in 21-01)"
  - "GET /{id}/geojson returns raw application/geo+json; other endpoints use R envelope"
  - "Upload audit action GEO_ASSET_UPLOAD via AuditService (delete deferred to 21-03)"

patterns-established:
  - "Geo asset registry mirrors SecretResolver/SecretService split"
  - "Console geo assets isolated from ConsoleUploadController ephemeral paths"

requirements-completed: [GEO-05, GEO-08, GOV-01]

coverage:
  - id: D1
    description: "Operators upload validated GeoJSON via POST /api/console/geo-assets with size/feature limits"
    requirement: GEO-05
    verification:
      - kind: integration
        ref: "data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetControllerIT.java#upload_validFeatureCollection_returnsSummaryWithId"
        status: pass
      - kind: unit
        ref: "data-generator-service/src/test/java/org/gensokyo/data/geo/GeoAssetServiceTests.java#ingest_exceedsMaxBytes_rejectsBeforePersist"
        status: pass
    human_judgment: false
  - id: D2
    description: "List/get metadata and GET /{id}/geojson authoritative body with bbox and featureCount"
    requirement: GEO-08
    verification:
      - kind: integration
        ref: "data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetControllerIT.java#getGeoJson_returnsRawApplicationGeoJsonBody"
        status: pass
    human_judgment: false
  - id: D3
    description: "Upload emits GEO_ASSET_UPLOAD audit event"
    requirement: GOV-01
    verification:
      - kind: integration
        ref: "data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetControllerIT.java#upload_createsAuditRecord"
        status: pass
    human_judgment: false
  - id: D4
    description: "GeoAssetResolver.resolveUtf8 fails fast with asset id in message"
    requirement: GEO-05
    verification:
      - kind: unit
        ref: "data-generator-service/src/test/java/org/gensokyo/data/geo/GeoAssetServiceTests.java#resolveUtf8_unknownId_throwsWithIdInMessage"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-08-01
status: complete
---

# Phase 21 Plan 01 Summary

**Durable GeoJSON asset registry in metadata DB with console upload/list/get REST, ingest validation, and GeoAssetResolver contract — no runtime wiring or delete yet**

## Performance

- **Duration:** ~25 min
- **Started:** 2026-08-01T08:11:00+08:00
- **Completed:** 2026-08-01T08:36:00+08:00
- **Tasks:** 4
- **Files modified:** 12

## Accomplishments

- `GeoAssetResolver` core interface with `resolveUtf8(String assetId)` for Plan 21-02 runtime wiring
- `geo_asset` JPA entity (CLOB body, bbox, featureCount, geometry summary, SHA-256, actor/timestamps)
- `GeoAssetIngestSupport` validates Feature/FeatureCollection via `GeoJsonLoader`, enforces `max-bytes` / `max-features`
- `ConsoleGeoAssetController` at `/api/console/geo-assets` — multipart upload, list, metadata GET, raw geo+json GET
- Upload audit via `AuditService` (`GEO_ASSET_UPLOAD` / `GEO_ASSET`)
- Spring multipart limits raised to 55MB/56MB; defaults `data.generator.geo-assets.*` in `application.yaml`

## Task Commits

1. **Task 1–4: Geo asset registry persistence + REST** — (see commit hash below)

## Files Created/Modified

- `GeoAssetResolver.java` — core resolution contract (no Spring)
- `GeoAssetPO.java` / `GeoAssetRepository.java` — metadata DB persistence
- `GeoAssetIngestSupport.java` / `GeoAssetService.java` — validation, CRUD, audit, resolver impl
- `ConsoleGeoAssetController.java` + DTOs — operator REST surface
- `DataGeneratorProperties.GeoAssets` + `application.yaml` — limits and multipart caps
- `GeoAssetServiceTests.java` / `ConsoleGeoAssetControllerIT.java` — 11 tests green

## Decisions Made

- Used temp-file bridge to `GeoJsonLoader.loadFeatureCollection` to avoid geo-module API changes in 21-01
- Oversize rejection covered in service unit tests; REST IT focuses on happy path + invalid root type

## Deviations from Plan

None — plan executed as written. Delete endpoint, runtime factory wiring, and VO asset-id fields remain in Plans 21-02/21-03.

## Issues Encountered

- Maven `-pl data-generator-service` alone fails dependency resolution; verify command requires `-am` and quoted `-Dtest` on PowerShell
- IT initially asserted `geometrySummary` on upload response; fixed to GET `/{id}` since `GeoAssetUploadView` omits that field per plan

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 21-02 can wire `GeoAssetResolver` into `GeoResourceResolver` and calcite source factories
- Plan 21-03 can add DELETE with reference scan and delete audit
- Phase 22 can consume GET `/{id}/geojson` for map preview

---
*Phase: 21-geo-asset-registry-runtime-resolution*
*Completed: 2026-08-01*
