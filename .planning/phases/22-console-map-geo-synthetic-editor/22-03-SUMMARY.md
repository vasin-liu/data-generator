---
phase: 22-console-map-geo-synthetic-editor
plan: 03
subsystem: ui
tags: [geo, geo-assets, maplibre, i18n, console, ApiRequestError]

requires:
  - phase: 22-console-map-geo-synthetic-editor
    provides: ApiRequestError + geoAssets client + lazy GeoMapPreview (Plan 02)
provides:
  - GeoAssetsPage left list + right map (D-01..D-05, D-11)
  - Route /geo-assets + nav-geo-assets after UDFs
  - en + zh-CN nav.geoAssets and geoAssets.* page keys (D-12 page scope)
affects:
  - 22-04 geo_synthetic editor + asset picker + source.geoSynthetic.* keys
  - Phase 23 docs / optional Playwright coverage

tech-stack:
  added: []
  patterns:
    - "Geo assets page mirrors UdfsPage React Query + ConsolePageHeader; split Row/Col gap 32px"
    - "Delete 409 → Modal.info with templateName (templateId) usages; no soft-delete"
    - "Lazy Suspense GeoMapPreview; honesty=geometry with caller i18n text"

key-files:
  created:
    - data-generator-console-web/src/app/pages/GeoAssetsPage.tsx
  modified:
    - data-generator-console-web/src/app/App.tsx
    - data-generator-console-web/src/app/layout/ConsoleLayout.tsx
    - data-generator-console-web/src/i18n/locales/en.json
    - data-generator-console-web/src/i18n/locales/zh-CN.json

key-decisions:
  - "contentType column shown only when list rows expose it; size omitted (D-02)"
  - "409 conflict uses Modal.info (not force-delete); list unchanged"
  - "Plan 03 owns geoAssets.* + nav only — source.geoSynthetic.* deferred to Plan 04"

patterns-established:
  - "nav item after UDFs / before Audit with EnvironmentOutlined and testId nav-geo-assets"
  - "Upload primary CTA okText from i18n never Ant Design default OK"

requirements-completed: [GEO-07]

coverage:
  - id: D1
    description: "GeoAssetsPage left Table + right lazy GeoMapPreview with upload/delete 409"
    requirement: GEO-07
    verification:
      - kind: other
        ref: data-generator-console-web; npx tsc -p tsconfig.json --noEmit
        status: pass
    human_judgment: false
  - id: D2
    description: "Route /geo-assets + nav-geo-assets + en/zh geoAssets.* and nav.geoAssets"
    requirement: GEO-07
    verification:
      - kind: other
        ref: data-generator-console-web; npm run build
        status: pass
    human_judgment: false

duration: ~12min
completed: 2026-08-06
status: complete
---

# Phase 22 Plan 03: Geo assets page Summary

**Operators can open `/geo-assets`, browse uploaded GeoJSON on a MapLibre map, upload, and delete with a 409 usages Modal — all localized en/zh.**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-08-06T15:12:44Z
- **Completed:** 2026-08-06T15:25:00Z
- **Tasks:** 2/2
- **Files modified:** 5

## Accomplishments

- `GeoAssetsPage` implements D-01 layout (left Table min 320px / right map), D-03 Upload GeoJSON header CTA, D-04 inline delete + 409 usages Modal via `ApiRequestError`, D-05 geojson fetch into lazy `GeoMapPreview`, and D-11 geometry honesty Alert.
- Route `/geo-assets` and `nav-geo-assets` inserted after UDFs / before Audit per PATTERNS.
- i18n `nav.geoAssets` + full `geoAssets.*` page copy in en + zh-CN (D-12 page scope); no `source.geoSynthetic.*` keys (Plan 04).

## Task Commits

| Task | Name | Commit | Type |
|------|------|--------|------|
| 1 | Build GeoAssetsPage list + map + upload/delete | `8858c7a` | feat |
| 2 | Wire route, nav, and geoAssets i18n | `8951f6a` | feat |

## Files Created/Modified

- `src/app/pages/GeoAssetsPage.tsx` — list/map/upload/delete UX
- `src/app/App.tsx` — `geo-assets` route
- `src/app/layout/ConsoleLayout.tsx` — selectedKey + nav item
- `src/i18n/locales/en.json` / `zh-CN.json` — nav + page strings

## Decisions Made

- Show `contentType` only when present on list data; never invent size (D-02).
- 409 surfaced with `Modal.info` listing `templateName (templateId)`; honor conflict — no client force-delete (T-22-06).
- Honesty copy passed as `honestyText` from i18n into Plan 02 `GeoMapPreview`.

## Deviations from Plan

None - plan executed exactly as written.

## Threat Flags

None beyond plan register — GeoJSON properties not injected as HTML (T-22-04); 409 honored (T-22-06).

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: `data-generator-console-web/src/app/pages/GeoAssetsPage.tsx`
- FOUND: `geo-assets` in App.tsx; `nav-geo-assets` in ConsoleLayout.tsx
- FOUND: `nav.geoAssets` + `geoAssets.title` in en.json and zh-CN.json
- FOUND: commits `8858c7a`, `8951f6a`
- VERIFY: `tsc --noEmit` exit 0; `npm run build` exit 0
- VERIFY: page uses `fetchGeoAssetGeoJson`, lazy `GeoMapPreview`, `ApiRequestError` status 409

---
*Phase: 22-console-map-geo-synthetic-editor*
*Completed: 2026-08-06*
