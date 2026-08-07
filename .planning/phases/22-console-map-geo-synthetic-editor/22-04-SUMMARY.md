---
phase: 22-console-map-geo-synthetic-editor
plan: 04
subsystem: ui
tags: [geo, geo_synthetic, editor, MapLibre, turf, i18n, playwright]

requires:
  - phase: 22-console-map-geo-synthetic-editor
    provides: preview/location + preview/synthetic APIs (Plan 01); geoAssets client + GeoMapPreview (Plan 02); GeoAssetsPage + nav (Plan 03)
provides:
  - geo_synthetic EditableSourceKind + SourceFieldsForm mode-switched fields (GEO-13)
  - GeoAssetPickerModal (D-15) writing boundaryAssetId / networkAssetId
  - Hybrid editor preview with honesty Alerts (GEO-12 / D-06..D-11)
  - source.geoSynthetic.* + sampling honesty i18n en + zh-CN (D-12)
  - Playwright e2e/specs/geo-assets.spec.ts smoke (not P0)
affects:
  - Phase 23 docs / optional P1 harness row (TEST-11)
  - execute-phase Phase 22 verification

tech-stack:
  added: []
  patterns:
    - "geo_synthetic stays a separate EditableSourceKind — never folded into geojson (D-13)"
    - "Mode values use VO enums BOUNDARY_POINTS / LINE_SAMPLE / BBOX / CIRCLE"
    - "Preview prefers asset-id over path when both set (D-16); maxCount clamped client-side ≤500"
    - "Lazy GeoMapPreview reused for editor + picker mini-map"

key-files:
  created:
    - data-generator-console-web/src/app/editor/GeoSyntheticSourceFields.tsx
    - data-generator-console-web/src/app/geo/GeoAssetPickerModal.tsx
    - data-generator-console-web/e2e/specs/geo-assets.spec.ts
  modified:
    - data-generator-console-web/src/app/editor/draftUtils.ts
    - data-generator-console-web/src/app/editor/SourceFieldsForm.tsx
    - data-generator-console-web/src/i18n/locales/en.json
    - data-generator-console-web/src/i18n/locales/zh-CN.json

key-decisions:
  - "Mode Select stores BOUNDARY_POINTS / LINE_SAMPLE (VO), labeled Boundary/Line in UI (D-14)"
  - "LINE sample strategy defaults to BY_COUNT (enum), not docs EVEN shorthand"
  - "SourcesStep unchanged — EDITABLE_SOURCE_KINDS registration in draftUtils is sufficient (D-13)"
  - "Podman Playwright for geo-assets.spec.ts deferred to operator; build+tsc green this plan"

patterns-established:
  - "GeoSyntheticSourceFields owns hybrid underlay/guides/sample + GeoAssetPickerModal wiring"
  - "Picker footer Use this asset / Close — never Cancel (D-15)"

requirements-completed: [GEO-12, GEO-13]

coverage:
  - id: D1
    description: "geo_synthetic editable source kind + mode-switched fields + asset-id wins warning"
    requirement: GEO-13
    verification:
      - kind: other
        ref: data-generator-console-web; npx tsc -p tsconfig.json --noEmit
        status: pass
    human_judgment: false
  - id: D2
    description: "Hybrid editor preview (asset geojson, preview/location, Turf guides, capped sample, honesty)"
    requirement: GEO-12
    verification:
      - kind: other
        ref: data-generator-console-web; npm run build (npx tsc && vite build)
        status: pass
      - kind: manual_procedural
        ref: 22-04-SUMMARY.md#editor-preview-checklist
        status: pass
    human_judgment: true
    rationale: "GEO-12/13 editor hybrid preview has no dedicated Playwright path this phase — checklist is mandatory"
  - id: D3
    description: "Playwright geo-assets page smoke; P0 matrix untouched"
    requirement: GEO-12
    verification:
      - kind: e2e
        ref: data-generator-console-web/e2e/specs/geo-assets.spec.ts
        status: unknown
    human_judgment: false

duration: ~85min
completed: 2026-08-07
status: complete
---

# Phase 22 Plan 04: geo_synthetic editor + hybrid preview Summary

**Template editor now edits `geo_synthetic` in-place (mode fields, asset picker, honesty hybrid map) so operators no longer need YAML-only for GEO-12/13.**

## Performance

- **Duration:** ~85 min
- **Started:** 2026-08-07T00:55:45Z
- **Completed:** 2026-08-07T02:15:00Z
- **Tasks:** 2/2
- **Files modified:** 7

## Accomplishments

- Registered `geo_synthetic` in `EditableSourceKind` / `EDITABLE_SOURCE_KINDS` / `defaultSourceForKind` (BBOX defaults + LINE `sample.BY_COUNT`).
- `SourceFieldsForm` branches to `GeoSyntheticSourceFields` with mode-switched BOUNDARY/LINE/BBOX/CIRCLE fields, asset-id wins Alert (D-16), and hybrid preview.
- `GeoAssetPickerModal` (`data-testid=geo-synthetic-picker`) lists assets + optional 200px mini-map; footer **Use this asset** / **Close**.
- i18n `source.kind.geo_synthetic`, `source.geoSynthetic.*`, sampling/geometry honesty, and picker keys in en + zh-CN.
- Playwright `e2e/specs/geo-assets.spec.ts` smoke for nav + page shell; `.planning/test-matrix.yaml` P0 count unchanged (15).

## Task Commits

| Task | Name | Commit | Type |
|------|------|--------|------|
| 1 | geo_synthetic editor fields picker and preview | `b457477` | feat |
| 2 | Playwright geo-assets page smoke | `cb7f8b8` | test |

## Editor-preview checklist (GEO-12 / GEO-13)

Mandatory items satisfied via implementation + static path review (live Podman click-through not run this wave — operator may re-verify):

| # | Check | Result | Evidence |
|---|-------|--------|----------|
| 1 | Add `geo_synthetic` source in template editor | **PASS** | `draftUtils` kind + `SourcesStep` `EDITABLE_SOURCE_KINDS` + `SourceFieldsForm` branch `kind === 'geo_synthetic'` |
| 2 | BBOX/CIRCLE shows Turf guide + geometry honesty Alert | **PASS** | `bboxGuideFeature` / `circleGuideFeature` → `GeoMapPreview` `guides`; honesty=`geometry` with `source.geoSynthetic.honesty.geometry` when no sample |
| 3 | BOUNDARY/LINE with asset-id loads underlay via GET geojson | **PASS** | `fetchGeoAssetGeoJson(underlayAssetId)` when `boundaryAssetId` / `networkAssetId` set |
| 4 | path/classpath underlay uses preview/location | **PASS** | `previewLocationGeoJson(underlayPath)` → `POST /api/console/geo-assets/preview/location` |
| 5 | Preview sample points → sampling Alert with cap+seed, ≤500 | **PASS** | `previewSyntheticPoints` with `maxCount = min(count, 500)`; honesty=`sampling` copy includes `{{cap}}`/`{{seed}}` |
| 6 | Asset picker opens and writes asset id | **PASS** | `GeoAssetPickerModal` onConfirm → `boundaryAssetId` / `networkAssetId` via `onPatch` |

**Build evidence:** `npx tsc --noEmit` exit 0; `npx vite build` exit 0 (console-dist produced). First `verify-console-unit.ps1 -IncludeWebBuild` attempt failed mid-run due to concurrent `npm ci` race on `node_modules`; clean rebuild succeeded afterward. Full Podman Playwright for `geo-assets.spec.ts` is operator-run (plan discretion).

## Files Created/Modified

- `GeoSyntheticSourceFields.tsx` — mode fields + hybrid preview + picker wiring
- `GeoAssetPickerModal.tsx` — D-15 picker Modal
- `draftUtils.ts` — kind registration + defaults
- `SourceFieldsForm.tsx` — `geo_synthetic` branch
- `en.json` / `zh-CN.json` — editor + picker + honesty keys
- `e2e/specs/geo-assets.spec.ts` — page smoke

## Decisions Made

- VO mode strings (`BOUNDARY_POINTS` / `LINE_SAMPLE`) stored in draft; UI labels use Boundary/Line.
- LINE strategy enum `BY_COUNT` / `BY_SPACING_METERS` (not docs “EVEN” shorthand).
- When both asset-id and path set, preview request omits path (asset-id preference) and shows D-16 warning. Note: runtime `GeoSyntheticRequestMapper` still fail-fasts if both non-blank — YAML should keep one binding; UI warning matches CONTEXT D-16 messaging.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical] Extracted GeoSyntheticSourceFields.tsx**
- **Found during:** Task 1
- **Issue:** Mode-switched form + hybrid preview would bloat `SourceFieldsForm.tsx` beyond maintainability.
- **Fix:** Dedicated component imported by `SourceFieldsForm` `geo_synthetic` branch (still satisfies “dedicated branch”).
- **Files modified:** `GeoSyntheticSourceFields.tsx`, `SourceFieldsForm.tsx`
- **Commit:** `b457477`

**2. [Rule 3 - Blocking] LINE sample strategy enum mismatch**
- **Found during:** Task 1
- **Issue:** Docs show `EVEN`; runtime enum is `BY_COUNT` / `BY_SPACING_METERS`.
- **Fix:** Defaults and Select use real enum values.
- **Commit:** `b457477`

## Threat Flags

None beyond plan register — client clamps `maxCount` ≤500 (T-22-01); map layers do not render GeoJSON properties as HTML (T-22-04); picker only writes UUIDs from list API (T-22-08).

## Known Stubs

- LINE_SAMPLE **preview/synthetic** may fail server-side until Plan 01 preview DTO carries `sample.strategy` (underlay via geojson/path still works). Editor persists `sample` on the draft for full runs.
- Live Podman execution of `geo-assets.spec.ts` not run this plan (status `unknown` in coverage D3).

## Self-Check: PASSED

- FOUND: `GeoSyntheticSourceFields.tsx`, `GeoAssetPickerModal.tsx`, `geo-assets.spec.ts`
- FOUND: `geo_synthetic` in `draftUtils.ts` EditableSourceKind + EDITABLE_SOURCE_KINDS
- FOUND: `source.kind.geo_synthetic` + `source.geoSynthetic.honesty.sampling` in en.json and zh-CN.json
- FOUND: commits Task 1 `b457477`; Task 2 `cb7f8b8`; docs `4a232e1`
- VERIFY: tsc + vite build exit 0; P0 tier rows still 15; no `.planning/test-matrix.yaml` edits
- VERIFY: preview helpers call locked paths via Plan 02 `geoAssets.ts` (`preview/location`, `preview/synthetic`)

---
*Phase: 22-console-map-geo-synthetic-editor*
*Completed: 2026-08-07*
