# Phase 22: Console Map + geo_synthetic Editor - Context

**Gathered:** 2026-08-06  
**Status:** Ready for planning  
**Mode:** interactive discuss (all gray areas; zh replies)

<domain>
## Phase Boundary

Deliver equal-depth console UX for geo assets and `geo_synthetic`: a geo-assets page with list + MapLibre map (uploaded asset layers), hybrid preview for `geo_synthetic` config (server GeoJSON + client Turf guides + optional capped synthetic-point preview), seed/honesty messaging so preview ≠ full run, and template-editor support for `geo_synthetic` as an editable source kind with an asset picker. Closes the v2.2 YAML-only gap for GEO-07 / GEO-12 / GEO-13.

**Requirements:** GEO-07, GEO-12, GEO-13

**Out of scope:** Phase 23 docs/P1 harness row; GEO-06 polygons; new GIS platform features; P0 matrix inflation; inventing a second resolve spine for preview.

</domain>

<decisions>
## Implementation Decisions

### Assets page layout
- **D-01:** Main layout is **left asset list + right map**. Selecting a list row loads that asset on the map.
- **D-02:** List columns: **name + featureCount + bbox summary**. Also show **contentType / size when the list API already exposes them**; do not invent new list fields in this phase if absent — planner may note a small DTO extension only if trivial and already stored on `GeoAssetPO`.
- **D-03:** Upload entry is a **top-of-page Upload button** (multipart to `/api/console/geo-assets`), consistent with UDF-style pages.
- **D-04:** Delete is **inline on the list row**. On HTTP 409, show a **Modal** with template usage hints (id/name) from Phase 21 delete semantics — do not soft-delete or orphan.

### Map preview composition
- **D-05:** Uploaded assets render via **`GET /api/console/geo-assets/{id}/geojson`** into MapLibre fill/line layers — same resolve spine as runtime (Phase 21 D-10).
- **D-06:** For `geo_synthetic` boundary/network underlays: if **asset-id** is set, fetch GeoJSON the same way; if **path/classpath**, a **server preview helper** resolves and returns GeoJSON (console must not reimplement `classpath:` / filesystem / `asset:`).
- **D-07:** **BBOX / CIRCLE guides** are drawn **client-side** (Turf or equivalent) — no server round-trip for those guides.
- **D-08:** Optional **synthetic point sampling** via **`POST …/preview/synthetic`** (or equivalent under `/api/console/geo-assets`), reusing `GeoSyntheticGenerator`, with a hard **count cap (e.g. ≤500)** and **seed surfaced in the UI**.

### Seed honesty UX
- **D-09:** Show a **persistent Alert at the top of the map preview region** whenever preview is active.
- **D-10:** Alert copy must include **sample count cap + seed (when present) + explicit “not a full run”** wording.
- **D-11:** Even when **only geometry/guides** are shown (no point sampling), still show a **shorter** honesty hint (preview ≠ run).
- **D-12:** All copy goes through existing **i18next** with **zh + en** keys.

### geo_synthetic editor + asset picker
- **D-13:** Extend **`SourceFieldsForm` + Sources step kind picker** — do not add a separate geo wizard page.
- **D-14:** **Mode-switched fields**: BOUNDARY/LINE → asset or path fields; BBOX/CIRCLE → geometry params + seed/count (and related mode fields as needed for all four modes).
- **D-15:** Asset picker is a **Modal**: list (name / featureCount) + **optional mini-map preview**; confirm writes `boundaryAssetId` / `networkAssetId` (and geojson `assetId` where applicable).
- **D-16:** Form supports **both asset-id and path**; when both set for the same role, **asset-id wins** with a clear inline warning (Phase 21 D-02).

### Claude's Discretion
- Exact MapLibre / react-map-gl / Turf package versions within research ballpark; Vite lazy-split of the map chunk
- Exact preview API path naming under `/api/console/geo-assets` as long as D-06/D-08 hold
- Whether list DTO already has contentType (show it) vs skip size until PO exposes length
- Mini-map inside asset picker: how rich vs list-only fallback if map chunk not loaded
- Default basemap / style choice (no Mapbox token required)
- Playwright/E2E depth for Phase 22 vs unit/component first (planner decides evidence bar)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — v2.3 equal-depth GEO-07; P0 freeze
- `.planning/REQUIREMENTS.md` — GEO-07, GEO-12, GEO-13
- `.planning/ROADMAP.md` — Phase 22 success criteria (map + synthetic preview + seed honesty + editor + same resolve spine)
- `.planning/research/SUMMARY.md` — MapLibre + Turf hybrid preview; anti-patterns
- `.planning/research/ARCHITECTURE.md` — Pattern 4 hybrid preview; console owns visualization
- `.planning/research/STACK.md` — MapLibre GL / react-map-gl / Turf versions guidance
- `.planning/research/PITFALLS.md` — preview≠runtime; MapLibre Vite/CSS bundling

### Prior phase locks
- `.planning/phases/21-geo-asset-registry-runtime-resolution/21-CONTEXT.md` — D-01..D-11 (asset-id binding, GET geojson, resolve spine)
- `.planning/phases/21-geo-asset-registry-runtime-resolution/21-VERIFICATION.md` — shipped API/runtime evidence

### Console / geo code
- `data-generator-console-web/src/app/editor/SourceFieldsForm.tsx` — extend for `geo_synthetic`
- `data-generator-console-web/src/app/editor/steps/SourcesStep.tsx` — kind registration
- `data-generator-console-web/src/app/App.tsx` — add `/geo-assets` (or equivalent) route
- `data-generator-service/.../ConsoleGeoAssetController.java` — list/get/geojson/delete/upload to call from UI
- `docs/geo-synthetic-v2-source.md` — mode/fields semantics (editor must match); Phase 23 adds asset-id docs

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ConsoleGeoAssetController` + summary/geojson endpoints — assets page data plane
- `SourceFieldsForm` geojson/iterator branches — pattern for mode-specific forms
- Ant Design layout + React Query pages (Udfs/Datasources) — list + actions patterns
- Phase 21 `GeoAssetResolver` / GET geojson — map layer bytes

### Established Patterns
- Console routes under React Router inside `ConsoleLayout`
- i18next for all operator-facing strings
- Fail-fast / clear errors via `R` envelope; 409 usages already shaped for Modal
- Keep `geojson` vs `geo_synthetic` type split — do not collapse

### Integration Points
- New nav item + page for geo assets
- Lazy MapLibre panel shared by assets page, synthetic preview, and asset-picker mini-map
- Possible new preview endpoints on service for path/classpath resolve + synthetic points
- Template draft model: add `geo_synthetic` kind + asset-id fields in editor state

</code_context>

<specifics>
## Specific Ideas

- User selected **all recommended defaults** across four areas (layout, preview, honesty, editor).
- Q2 list fields: prefer contentType/size **if API already has them**; otherwise do not block the phase on new metadata columns.
- Preview honesty is non-negotiable for both point sampling and geometry-only modes.

</specifics>

<deferred>
## Deferred Ideas

- Batch multi-select delete — out of Phase 22 scope
- Server-side geometry simplify/decimate endpoint — new capability
- Mapbox-token / proprietary basemap — research says MapLibre without token
- Full docs + optional P1 `geo-assets` harness row — Phase 23 (DOC-01, TEST-11)
- GEO-06 polygon synthesis — future

</deferred>

---

*Phase: 22-console-map-geo-synthetic-editor*  
*Context gathered: 2026-08-06*
