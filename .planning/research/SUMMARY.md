# Project Research Summary

**Project:** data-generator  
**Domain:** Brownfield Template V2 — GeoJSON asset library + console map preview (v2.3)  
**Researched:** 2026-07-31  
**Confidence:** HIGH

## Executive Summary

v2.3 closes the operator gap left by v2.2: durable **hosted GeoJSON assets** in the metadata DB (GEO-05) and a **console map** to browse those assets and preview `geo_synthetic` configuration (GEO-07). Experts build this as a thin asset registry (UUID + validated GeoJSON CLOB) plus a single resolution spine (`classpath:` / filesystem / `asset:{uuid}`), with MapLibre in the existing React/Ant Design console — not a GIS platform.

Recommended approach: mirror secrets/UDF persistence patterns; extend `GeoResourceResolver` with an injectable `GeoAssetResolver`; raise multipart limits; cap size/feature count at ingest; hybrid preview (server GeoJSON for assets, client Turf overlays for BBOX/CIRCLE). Keep `geojson` vs `geo_synthetic` type split. Do **not** require PostGIS, new Maven modules, or P0 gate expansion.

Key risks: H2/metadata DB blow-up from large GeoJSON; dual path vs asset-id resolution bugs; preview≠runtime sampling; MapLibre Vite/CSS bundling; upload surface with RBAC default-off. Mitigate with hard limits, one resolver spine for preview and run, seed-aware preview docs, and audit + size gates even when RBAC is off.

## Key Findings

### Recommended Stack

Reuse Java 25 / Spring Boot 4 / JPA metadata DB / React 19 console. Add MapLibre + react-map-gl and small Turf helpers only.

**Core technologies:**
- **MapLibre GL JS ^5.24** + **react-map-gl ^8.1** — WebGL map without Mapbox token; React 19 bindings
- **`@turf/bbox` / `@turf/circle` / `@turf/helpers` ^7** — client fit-bounds and BBOX/CIRCLE overlays
- **JPA CLOB GeoJSON** in metadata DB — same backup surface as secrets/templates/UDFs
- **Existing `GeoJsonLoader` + JTS** — upload validation and runtime parse; no GeoTools expansion
- **Spring multipart** raised to ~16MB — Boot defaults reject real district GeoJSON

### Expected Features

**Must have (table stakes):**
- Multipart GeoJSON upload → UUID asset-id, list/get/delete, derived bbox/featureCount
- Template binding: `boundaryAssetId` / `networkAssetId` (or `asset:` path) with path/`classpath:` fallback
- Runtime `GeoAssetResolver` on execute path (coordinator + worker share metadata DB)
- Map: asset browse render + `geo_synthetic` config preview (equal depth)
- Size/feature caps, GeoJSON validation, audit on upload/delete

**Should have (competitive):**
- Referential delete guard (409 if templates still reference asset)
- Console `geo_synthetic` editor + asset picker (v2.2 was backend-only — required for equal-depth GEO-07)
- Optional P1 harness row `geo-assets` when ITs stable

**Defer (v2+):**
- GEO-06 polygon synthesis, Shapefile/KML import, CRS reprojection, map geometry editing
- DATA-01 common-data CRUD, mandatory RBAC, P0 promotion of geo-assets

### Architecture Approach

No new top-level modules. Service owns `GeoAssetService` / PO / `ConsoleGeoAssetController`; core defines `GeoAssetResolver`; geo module extends `GeoResourceResolver` with `asset:` prefix; calcite factories inject resolver; console adds assets page + lazy MapLibre panel.

**Major components:**
1. `GeoAssetService` + `geo_asset` table — CRUD, validate, resolveUtf8
2. `GeoResourceResolver` + `asset:` — single spine for run and preview
3. `ConsoleGeoAssetController` + map UI — list without full payload; GET body for layers
4. Hybrid preview — server GeoJSON for assets; client Turf for bbox/circle overlays (not full server re-synthesis for every pan)

### Critical Pitfalls

1. **Huge GeoJSON → H2 blow-up** — enforce max bytes + feature count before persist
2. **Path vs asset-id dual bugs** — one resolver; never treat asset-id as filesystem path
3. **Preview ≠ runtime** — same resolver for map GET and TemplateV2Runner; document seed for point modes
4. **XSS / malicious properties in GeoJSON** — validate geometry roots; sanitize/ignore HTML in props for map popups
5. **P0 accidental inflation** — keep verify-harness P0 at 15; P1 optional only

## Implications for Roadmap

Suggested phase numbering continues from v2.2 (last phase 20):

### Phase 21: GEO-05 Backend — Asset Registry + Runtime Resolution
**Rationale:** Map and editor depend on durable assets and `asset:` resolution.  
**Delivers:** PO/Repository/Service, multipart upload API, size/validation gates, audit, `GeoAssetResolver` wired into geojson + geo_synthetic factories, pipeline IT with asset-id.  
**Addresses:** Asset CRUD table stakes + template binding runtime.  
**Avoids:** Unbounded CLOB; ephemeral `ConsoleUploadController` reuse for durable assets.

### Phase 22: GEO-07 Console — Assets UI + Map Preview + geo_synthetic Editor
**Rationale:** Equal-depth delivery; v2.2 left geo_synthetic out of `EDITABLE_SOURCE_KINDS`.  
**Delivers:** Geo assets page, MapLibre preview (asset layer + synthetic overlays), asset picker, `geo_synthetic` source fields in template editor.  
**Uses:** MapLibre/react-map-gl/Turf; list-without-payload + GET body APIs.  
**Avoids:** Dual preview resolver; collapsing geojson/geo_synthetic types.

### Phase 23: Docs + Harness Closeout
**Rationale:** Operator docs and optional P1 row without P0 churn.  
**Delivers:** Docs for asset-id YAML + map usage; optional `geo-assets` P1 matrix row; console verify green.  
**Avoids:** P0 promotion; GEO-06 scope creep.

## Research Flags (deeper research during plan/execute)

| Topic | Why | Suggested phase |
|-------|-----|-----------------|
| Exact H2 CLOB size behavior under concurrent worker | Staging dual-JVM sensitivity | 21 |
| Template reference scan for delete-409 | Need reliable JSON path for assetId/`asset:` | 21–22 |
| MapLibre CSS + Vite code-splitting | Bundle size / load errors | 22 |
| Preview sample count vs run count UX | Avoid false confidence on CIRCLE/BBOX | 22 |

## Gaps to Address in Requirements

- Explicit REQ for **path/`classpath:` fallback** (preserve GEO-03)
- Explicit REQ for **`geo_synthetic` console editor** as part of GEO-07 equal depth
- Explicit **size + feature-count limits** as first-class requirements
- Explicit **P0 freeze** + optional P1 only
- Clarify **asset-id field shape** (`boundaryAssetId` vs `asset:` in path) — pick one primary for console, support resolver wire format

## Sources

- `.planning/research/STACK.md`, `FEATURES.md`, `ARCHITECTURE.md`, `PITFALLS.md` (2026-07-31)
- `.planning/PROJECT.md` v2.3 charter; v2.2 shipped geo stack and archives
- Code patterns: `GeoResourceResolver`, `ConsoleUploadController`, `SecretService` / UDF persistence, `SourceFieldsForm` / `EDITABLE_SOURCE_KINDS`

---
*Synthesized for `/gsd-new-milestone` v2.3 — replaces prior 2026-07-25 SUMMARY.md*
