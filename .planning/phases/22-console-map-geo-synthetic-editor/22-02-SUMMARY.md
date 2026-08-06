---
phase: 22-console-map-geo-synthetic-editor
plan: 02
subsystem: ui
tags: [geo, maplibre, react-map-gl, turf, ApiRequestError, console]

requires:
  - phase: 22-console-map-geo-synthetic-editor
    provides: POST preview/location + preview/synthetic locked paths (Plan 01)
  - phase: 21-geo-asset-registry-runtime-resolution
    provides: ConsoleGeoAssetController CRUD + 409 usages payload
provides:
  - ApiRequestError preserving HTTP status + R.data (409 usages)
  - src/api/geoAssets.ts client (list/upload/delete/geojson/preview)
  - Lazy GeoMapPreview with MapLibre OSM basemap + honesty Alert
  - maplibre-gl / react-map-gl / @turf/* package pins (no mapbox-gl)
affects:
  - 22-03 GeoAssetsPage delete Modal + map wiring
  - 22-04 geo_synthetic editor preview + asset picker mini-map

tech-stack:
  added:
    - maplibre-gl@^5.24.0
    - react-map-gl@^8.1.2
    - "@turf/bbox@^7.4.0"
    - "@turf/circle@^7.4.0"
    - "@turf/helpers@^7.4.0"
  patterns:
    - "ApiRequestError carries status+data; err.message stays toast-compatible"
    - "Raw geo+json fetches bypass apiRequest (Accept application/geo+json + role headers)"
    - "maplibre-gl.css imported only inside GeoMapPreview lazy module"

key-files:
  created:
    - data-generator-console-web/src/api/geoAssets.ts
    - data-generator-console-web/src/app/geo/GeoMapPreview.tsx
  modified:
    - data-generator-console-web/src/api/client.ts
    - data-generator-console-web/src/api/types.ts
    - data-generator-console-web/package.json
    - data-generator-console-web/package-lock.json

key-decisions:
  - "preview/location uses dedicated raw fetch (geo+json); preview/synthetic uses apiRequest R envelope — matches Plan 01"
  - "Export consoleRoleHeaders for raw geo fetches; re-export ApiRequestError from client"
  - "GeoMapPreview default+named export for React.lazy; mapLib=import('maplibre-gl') for Vite split"

patterns-established:
  - "Failed R envelopes throw ApiRequestError(status, data) from apiRequest and parseApiResult"
  - "Geo map paint tokens from antd theme.useToken (accent/warning) per UI-SPEC"

requirements-completed: [GEO-07]

coverage:
  - id: D1
    description: "ApiRequestError preserves HTTP status and envelope data for 409 usages"
    requirement: GEO-07
    verification:
      - kind: other
        ref: data-generator-console-web; npx tsc -p tsconfig.json --noEmit
        status: pass
    human_judgment: false
  - id: D2
    description: "geoAssets client with locked preview paths and raw geojson fetch"
    requirement: GEO-07
    verification:
      - kind: other
        ref: data-generator-console-web; npx tsc -p tsconfig.json --noEmit
        status: pass
    human_judgment: false
  - id: D3
    description: "GeoMapPreview MapLibre OSM + honesty Alert; map deps without mapbox-gl"
    requirement: GEO-07
    verification:
      - kind: other
        ref: data-generator-console-web; npm run build
        status: pass
    human_judgment: false

duration: ~17min
completed: 2026-08-06
status: complete
---

# Phase 22 Plan 02: Console map foundation Summary

**MapLibre/Turf stack, ApiRequestError (409 usages survivable), and lazy GeoMapPreview ship as the shared GEO-07 rendering foundation for later asset/editor pages.**

## Performance

- **Duration:** ~17 min
- **Started:** 2026-08-06T14:45:33Z
- **Completed:** 2026-08-06T15:03:00Z
- **Tasks:** 2/2
- **Files modified:** 6

## Accomplishments

- `ApiRequestError` thrown from `apiRequest` / `parseApiResult` preserves `status` + `data` so Plan 03 delete Modal can read `data.usages` while existing `err.message` callers stay compatible.
- `geoAssets.ts` covers list/upload/delete, raw `fetchGeoAssetGeoJson`, and locked preview paths `/console/geo-assets/preview/location` (raw geo+json) + `/preview/synthetic` (R envelope).
- `GeoMapPreview` uses `react-map-gl/maplibre`, OSM raster basemap, UI-SPEC paint, Turf bbox fit, honesty Alert slot, client-only mount; CSS side-effect only in this module.

## Task Commits

| Task | Name | Commit | Type |
|------|------|--------|------|
| 1 | Preserve 409 usages + geoAssets API | `faa2446` | feat |
| 2 | MapLibre/Turf + GeoMapPreview | `baf7e26` | feat |

## Files Created/Modified

- `src/api/types.ts` — `ApiRequestError`, geo asset / preview TypeScript types
- `src/api/client.ts` — throw `ApiRequestError`; export `consoleRoleHeaders`
- `src/api/geoAssets.ts` — React Query-friendly geo asset client
- `src/app/geo/GeoMapPreview.tsx` — shared lazy map panel
- `package.json` / `package-lock.json` — maplibre-gl, react-map-gl, @turf/* pins

## Decisions Made

- Location preview returns raw `application/geo+json` (Plan 01) → dedicated fetch, not `apiRequest`.
- Honesty copy is caller-supplied (`honestyText`) so i18n keys land with pages in later plans.
- No routes/pages wired yet (per plan); consumers will `React.lazy(() => import('./GeoMapPreview'))`.

## Deviations from Plan

None - plan executed exactly as written.

## Threat Flags

None beyond plan register — npm installs used STACK.md package names only; GeoMapPreview does not render GeoJSON properties as HTML (T-22-04).

## Known Stubs

None. (`honestyText` is intentional caller-supplied prop until i18n pages land.)

## Self-Check: PASSED

- FOUND: `data-generator-console-web/src/api/geoAssets.ts`
- FOUND: `data-generator-console-web/src/app/geo/GeoMapPreview.tsx`
- FOUND: commits `faa2446`, `baf7e26`
- VERIFY: `tsc --noEmit` exit 0; `npm run build` exit 0
- VERIFY: `package.json` lists maplibre-gl / react-map-gl / @turf/*; no `mapbox-gl` dependency
- VERIFY: preview helpers use `/console/geo-assets/preview/location` and `/console/geo-assets/preview/synthetic`
