---
status: complete
phase: 21-geo-asset-registry-runtime-resolution
source:
  - 21-01-SUMMARY.md
  - 21-02-SUMMARY.md
  - 21-03-SUMMARY.md
started: "2026-08-01T10:15:00.000Z"
updated: "2026-08-04T05:35:00.000Z"
---

## Current Test

[testing complete]

## Tests

### 1. Upload Valid GeoJSON Asset
expected: POST multipart GeoJSON (Feature or FeatureCollection) to /api/console/geo-assets returns success with a stable UUID asset id, display name, and featureCount. Invalid Geometry-only roots are rejected (4xx / success=false).
result: pass
source: automated
evidence:
  - ConsoleGeoAssetControllerIT#upload_validFeatureCollection_returnsSummaryWithId
  - ConsoleGeoAssetControllerIT#upload_geometryOnlyRoot_returnsBadRequest
  - GeoAssetServiceTests (ingest size/feature rejection)

### 2. List and Fetch Asset Metadata + Body
expected: GET /api/console/geo-assets lists summaries without GeoJSON bodies. GET /{id} returns metadata including featureCount and bbox fields. GET /{id}/geojson returns raw application/geo+json body for that asset.
result: pass
source: automated
evidence:
  - ConsoleGeoAssetControllerIT#list_returnsSummariesWithoutGeoJsonBody
  - ConsoleGeoAssetControllerIT#getById_returnsMetadataOnly
  - ConsoleGeoAssetControllerIT#getGeoJson_returnsRawApplicationGeoJsonBody

### 3. Delete Unreferenced Asset
expected: DELETE /api/console/geo-assets/{id} for an asset not referenced by any template succeeds; subsequent GET /{id} fails as unknown; a GEO_ASSET_DELETE audit event is recorded for that asset id.
result: pass
source: automated
evidence:
  - ConsoleGeoAssetControllerIT#delete_unreferencedAsset_succeedsAndAudits

### 4. Delete Blocked When Template References Asset
expected: After saving a template that references the asset (boundaryAssetId / assetId / path asset:{uuid}), DELETE returns HTTP 409 with a message naming the template and a structured usages list (templateId, templateName). Asset remains fetchable.
result: pass
source: automated
evidence:
  - ConsoleGeoAssetControllerIT#delete_referencedByTemplate_returns409WithUsageHints
  - GeoAssetReferenceScannerTests (5 cases)

### 5. Template V2 Run Resolves Asset Id
expected: A Template V2 run using geo_synthetic with boundaryAssetId (or geojson with assetId / path asset:{uuid}) against an uploaded asset completes successfully and produces lon/lat rows. classpath: geo paths still work without asset ids.
result: pass
source: automated
evidence:
  - TemplateV2RunnerGeoAssetSourceTests#boundaryPoints_withBoundaryAssetId_returnsExpectedRowCount
  - TemplateV2RunnerGeoAssetSourceTests#geojson_withAssetId_returnsNonEmptyLonLatRows
  - TemplateV2RunnerGeoAssetSourceTests#boundaryPoints_withAssetWireFormatPath_matchesDedicatedField
  - GeoJsonLoaderTests#loadFeatureCollectionFromAsset_resolvesViaStub

### 6. Unknown Asset Id Fails the Run
expected: Running a template that references a non-existent asset UUID fails the run; the error chain names that asset id (not a silent empty result).
result: pass
source: automated
evidence:
  - TemplateV2RunnerGeoAssetSourceTests#unknownAssetId_failsWithIllegalArgumentNamingId

## Summary

total: 6
passed: 6
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none yet]

## Automated Verification Run

date: 2026-08-04T12:09+08:00
commands:
  - |
    .\mvnw-jdk25.ps1 -pl data-generator-calcite,data-generator-geo -am
    -Dtest=TemplateV2RunnerGeoAssetSourceTests,GeoJsonLoaderTests,GeoSyntheticRequestMapperTests
    -Dsurefire.failIfNoSpecifiedTests=false test
  - |
    .\mvnw-jdk25.ps1 -pl data-generator-service -am
    -Dskip.npm=true -Dskip.installnodenpm=true -Dskip.console.frontend=true
    -Dtest=ConsoleGeoAssetControllerIT,GeoAssetServiceTests,GeoAssetReferenceScannerTests
    -Dsurefire.failIfNoSpecifiedTests=false test
results:
  - ConsoleGeoAssetControllerIT: 8/8 pass (surefire 2026-08-04T12:08:55)
  - GeoAssetServiceTests: 5/5 pass (surefire 2026-08-04T12:09:07)
  - GeoAssetReferenceScannerTests: 5/5 pass (surefire 2026-08-04T12:09:07)
  - TemplateV2RunnerGeoAssetSourceTests: 4/4 pass (surefire 2026-08-04T11:59:43)
  - GeoJsonLoaderTests: 4/4 pass (surefire 2026-08-04T11:59:09)
note: Earlier attempts failed on console npm-build; re-ran with skip.npm. No Playwright E2E for geo-assets UI this phase; MockMvc IT covers REST.
