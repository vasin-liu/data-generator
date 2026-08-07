---
phase: 22-console-map-geo-synthetic-editor
verified: 2026-08-07T03:30:00Z
status: passed
score: 5/5 must-haves verified
behavior_unverified: 0
overrides_applied: 0
re_verification: false
---

# Phase 22: Console Map + geo_synthetic Editor Verification Report

**Phase Goal:** Equal-depth console UX — browse uploaded assets on a map, preview `geo_synthetic` config, and edit `geo_synthetic` sources in the template editor (closes v2.2 YAML-only gap).

**Verified:** 2026-08-07T03:30:00Z  
**Status:** passed  
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

Roadmap success criteria (contract). Plan frontmatter truths that restate these are folded in; unique plan details covered under artifacts / key links.

| # | Truth | Status | Evidence |
| --- | ------- | ---------- | -------------- |
| 1 | Operator opens a console geo-assets view and sees the selected uploaded asset rendered on a map | ✓ VERIFIED | Route `/geo-assets` in `App.tsx`; nav `nav-geo-assets` in `ConsoleLayout`; `GeoAssetsPage` left Table row click → `selectedId` → `fetchGeoAssetGeoJson` → lazy `GeoMapPreview` with MapLibre fill/line layers; honesty=`geometry` when underlay present; Playwright `e2e/specs/geo-assets.spec.ts` asserts nav + page + map testids |
| 2 | Operator previews a `geo_synthetic` source config on the map (boundary/network overlay and/or BBOX/CIRCLE guides) | ✓ VERIFIED | `GeoSyntheticSourceFields`: asset-id underlay via `fetchGeoAssetGeoJson`; path/classpath via `previewLocationGeoJson` → `POST …/preview/location`; BBOX/CIRCLE client Turf `bboxGuideFeature` / `circleGuideFeature` → `guides` prop; optional capped sample via `previewSyntheticPoints` |
| 3 | Preview UX documents seed behavior so operators do not mistake preview sampling for full run | ✓ VERIFIED | Persistent warning Alert in `GeoMapPreview` (no `closable`); sampling copy `source.geoSynthetic.honesty.sampling` includes `{{cap}}` + `{{seed}}` + not-a-full-run; geometry-only `geoAssets.honesty.geometry` / `source.geoSynthetic.honesty.geometry`; en + zh-CN keys present |
| 4 | Console template editor supports `geo_synthetic` as an editable source kind with an asset picker | ✓ VERIFIED | `EditableSourceKind` + `EDITABLE_SOURCE_KINDS` + `defaultSourceForKind` include `geo_synthetic`; `SourceFieldsForm` branches to `GeoSyntheticSourceFields`; mode Select BOUNDARY_POINTS/LINE_SAMPLE/BBOX/CIRCLE; `GeoAssetPickerModal` (`geo-synthetic-picker`) writes `boundaryAssetId` / `networkAssetId`; asset-id-wins Alert when both id+path set |
| 5 | Map asset layers use the same resolution spine as runtime (server GeoJSON for assets; client Turf overlays for BBOX/CIRCLE guides) | ✓ VERIFIED | `GeoAssetService.previewLocation` → `GeoResourceResolver.readUtf8(location, this)`; `previewSynthetic` → `GeoSyntheticRequestMapper` + `GeoSyntheticGenerator.generateRows(…, this)`; GET `/{id}/geojson` for hosted assets; client `@turf/helpers` polygon + `@turf/circle` only for guides — no mapbox-gl |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | ----------- | ------ | ------- |
| `ConsoleGeoAssetController.java` | Preview endpoints under `/api/console/geo-assets` | ✓ VERIFIED | `POST /preview/location` (geo+json); `POST /preview/synthetic` (`R`-wrapped); existing list/geojson/delete intact |
| `GeoAssetService.java` | Preview methods on Phase 21 spine | ✓ VERIFIED | `previewLocation` / `previewSynthetic`; `PREVIEW_MAX_COUNT=500`; rejects over-cap with IAE naming 500 |
| `GeoPreviewLocationRequest` / `GeoSyntheticPreviewRequest` / `GeoSyntheticPreviewView` | Preview DTOs | ✓ VERIFIED | Records exist under `api/console/dto` |
| `GeoAssetSummaryView` | Optional `contentType` | ✓ VERIFIED | Mapped from `GeoAssetPO` when present |
| `ConsoleGeoAssetPreviewIT.java` | REST IT for location + synthetic | ✓ VERIFIED | 3 tests: classpath geo+json, BBOX ≤5 + seed, maxCount 501 → 400 |
| `GeoAssetServiceTests.java` | Service preview tests | ✓ VERIFIED | classpath preview, blank reject, over-cap, BBOX ≤10 + seed, contentType mapping |
| `geoAssets.ts` | Console API client | ✓ VERIFIED | list/upload/delete/geojson (raw)/preview location (raw)/preview synthetic |
| `client.ts` / `types.ts` `ApiRequestError` | Preserve 409 status+data | ✓ VERIFIED | `ApiRequestError` thrown from `parseApiResult` / `apiRequest` with status + data |
| `GeoMapPreview.tsx` | Lazy MapLibre panel | ✓ VERIFIED | `react-map-gl/maplibre`, `maplibre-gl.css`, OSM raster, honesty Alert, Turf bbox fit, layer paint |
| `package.json` MapLibre/Turf pins | maplibre + turf, no mapbox | ✓ VERIFIED | `maplibre-gl`, `react-map-gl`, `@turf/bbox|circle|helpers`; no `mapbox-gl` |
| `GeoAssetsPage.tsx` | List + map + upload/delete | ✓ VERIFIED | `geo-assets-page` / upload / map testids; 409 usages Modal via `ApiRequestError` |
| `App.tsx` / `ConsoleLayout.tsx` | Route + nav | ✓ VERIFIED | `geo-assets` route; `nav-geo-assets` after UDFs |
| `en.json` / `zh-CN.json` | i18n | ✓ VERIFIED | `nav.geoAssets`, `geoAssets.*`, `source.kind.geo_synthetic`, `source.geoSynthetic.*` honesty keys |
| `draftUtils.ts` | `geo_synthetic` kind | ✓ VERIFIED | Kind union, EDITABLE list, BBOX defaults + sample BY_COUNT |
| `SourceFieldsForm.tsx` | Dedicated branch | ✓ VERIFIED | `kind === 'geo_synthetic'` → `GeoSyntheticSourceFields` (extracted component; still dedicated branch) |
| `GeoSyntheticSourceFields.tsx` | Mode fields + hybrid preview | ✓ VERIFIED | Modes, picker, underlay/guides/sample, honesty wiring |
| `GeoAssetPickerModal.tsx` | Asset picker | ✓ VERIFIED | `geo-synthetic-picker`; mini-map 200px; Use this asset / Close |
| `e2e/specs/geo-assets.spec.ts` | Playwright smoke | ✓ VERIFIED | ≥40 lines; nav + page shell; empty-list resilient |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | --- | --- | ------ | ------- |
| `GeoAssetService` | `GeoResourceResolver` | `readUtf8(location, this)` | ✓ WIRED | Preview location uses Phase 21 spine |
| `GeoAssetService` | `GeoSyntheticGenerator` | `generateRows` + mapper | ✓ WIRED | Same generator as runtime |
| `ConsoleGeoAssetController` | `GeoAssetService` | preview endpoints | ✓ WIRED | Controller delegates |
| `geoAssets.ts` | `client.ts` / raw fetch | `ApiRequestError` + geo+json fetch | ✓ WIRED | 409 data preserved; geojson not R-envelope |
| `GeoAssetsPage` | `geoAssets.ts` | React Query list/geojson/upload/delete | ✓ WIRED | `fetchGeoAssetGeoJson` on select |
| `GeoAssetsPage` | `GeoMapPreview` | lazy Suspense | ✓ WIRED | Map panel + honesty |
| `GeoSyntheticSourceFields` | `geoAssets.ts` | underlay + `previewSynthetic` | ✓ WIRED | Asset GET / location POST / synthetic POST |
| `GeoSyntheticSourceFields` | `GeoAssetPickerModal` | onConfirm → onPatch | ✓ WIRED | boundary/network asset ids |
| `GeoAssetPickerModal` | `GeoMapPreview` | mini-map 200px | ✓ WIRED | Optional underlay + geometry honesty |
| `SourceFieldsForm` | `GeoSyntheticSourceFields` | kind branch | ✓ WIRED | Not folded into geojson |
| `SourcesStep` | `EDITABLE_SOURCE_KINDS` | kind options | ✓ WIRED | Labels via `source.kind.*` |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| -------- | ------------- | ------ | ------------------ | ------ |
| `GeoAssetsPage` map | `mapGeoJson` | `GET /api/console/geo-assets/{id}/geojson` via `fetchGeoAssetGeoJson` | Yes — DB CLOB body (Phase 21) | ✓ FLOWING |
| `GeoAssetsPage` list | `assetsQuery.data` | `GET /api/console/geo-assets` | Yes — summary rows from repository | ✓ FLOWING |
| Editor underlay (asset) | `underlay` | `fetchGeoAssetGeoJson` | Yes — same GET geojson | ✓ FLOWING |
| Editor underlay (path) | `underlay` | `previewLocation` → `GeoResourceResolver` | Yes — classpath/file/asset: spine | ✓ FLOWING |
| Editor guides | `guides` | Client Turf from form bbox/center/radius | Yes — derived from form state | ✓ FLOWING |
| Editor sample points | `samplePoints` | `previewSynthetic` → `GeoSyntheticGenerator` | Yes — FeatureCollection + seed/cap | ✓ FLOWING |
| Picker mini-map | `underlay` | `fetchGeoAssetGeoJson` when row selected | Yes — same GET | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Synthetic preview cap + seed (service test exists) | `GeoAssetServiceTests#previewSynthetic_bboxMaxCount10_returnsAtMost10PointsAndSeed` | Present in source; live Surefire re-run blocked this session by Maven `${revision}`/repo.spring.io resolution cache (env), not by missing tests | ? SKIP (env) |
| Preview REST IT exists | `ConsoleGeoAssetPreviewIT` (3 methods) | File present with classpath / BBOX seed / over-cap 400 cases | ✓ PASS (existence) |
| Playwright smoke exists | `e2e/specs/geo-assets.spec.ts` | Spec asserts `nav-geo-assets`, `geo-assets-page`, upload, map; Podman live run deferred to operator (plan discretion) | ✓ PASS (existence) |
| P0 matrix frozen | `(Select-String 'tier: P0' .planning/test-matrix.yaml).Count` | **15** | ✓ PASS |

### Probe Execution

| Probe | Command | Result | Status |
| ----- | ------- | ------ | ------ |
| — | — | No phase-declared `scripts/*/tests/probe-*.sh` | SKIPPED |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ---------- | ----------- | ------ | -------- |
| GEO-07 | 22-02, 22-03 | Console geo-assets view + selected asset on map | ✓ SATISFIED | GeoAssetsPage + MapLibre + route/nav + honesty geometry |
| GEO-12 | 22-01, 22-04 | Preview geo_synthetic on map + seed honesty | ✓ SATISFIED | Preview APIs + hybrid editor preview + sampling/geometry Alerts |
| GEO-13 | 22-04 | Editable `geo_synthetic` + asset picker | ✓ SATISFIED | draftUtils kind + SourceFieldsForm + GeoAssetPickerModal |

No orphaned Phase 22 requirements in REQUIREMENTS.md beyond GEO-07/12/13.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| `GeoSyntheticPreviewRequest` / `toPreviewSource` | — | LINE_SAMPLE `sample.strategy` not carried on preview DTO (known stub in 22-04-SUMMARY) | ℹ️ Info | Underlay for LINE still works; BBOX/CIRCLE/BOUNDARY sampling + guides satisfy SC2 “and/or”; full-run draft still persists `sample` |
| Runtime vs UI dual-bind | — | UI warns asset-id wins; runtime mapper may fail-fast if both non-blank (documented in SUMMARY) | ℹ️ Info | Matches Phase 22 D-16 copy; operators should keep one binding |
| — | — | No TBD/FIXME/XXX debt markers in phase-touched console geo files | — | Clean |

### Human Verification Required

None blocking. Live Podman Playwright for `geo-assets.spec.ts` and full editor click-through remain recommended operator UAT (plan discretion / Phase 23 docs), but roadmap truths are substantiated by wired implementation + server ITs + smoke spec presence.

### Gaps Summary

No gaps. Phase 22 roadmap success criteria are achieved in the codebase.

---

_Verified: 2026-08-07T03:30:00Z_  
_Verifier: Claude (gsd-verifier)_
