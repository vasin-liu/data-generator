# Geo assets and console map preview

Operator/maintainer reference for the v2.3 **geo asset registry** (Phase 21) and **console map preview** (Phase 22).

**Landing page:** [`geospatial-overview.md`](geospatial-overview.md)  
**YAML / asset-id binding:** [`geo-synthetic-v2-source.md`](geo-synthetic-v2-source.md)

## What shipped

| Concern | Where |
|---------|--------|
| Upload / list / get / delete | Console REST under `/api/console/geo-assets`; metadata DB CLOB (`geo_asset`) |
| Template binding | `boundaryAssetId` / `networkAssetId` / geojson `assetId`, or `asset:{uuid}` on path fields |
| Runtime resolve | Shared `GeoAssetResolver` spine (same path as Template V2 execute) |
| Console browse + map | SPA route **`/geo-assets`** (list + MapLibre layer for selected asset) |
| Template editor preview | Hybrid preview on `geo_synthetic` sources (asset/path underlay + optional capped point sample) |

## Upload size and feature limits

Limits come from shipped `data.generator.geo-assets.*` on `DataGeneratorProperties.GeoAssets` — do not invent additional knobs in templates.

| Property | Default | Meaning |
|----------|---------|---------|
| `data.generator.geo-assets.max-bytes` | **52428800** (50 MiB) | Maximum GeoJSON upload body size |
| `data.generator.geo-assets.max-features` | **10000** | Maximum Feature count per asset |

Oversize or over-feature uploads are rejected **before** persist (`R.fail`, 400/413-style). Phase 21 also raises Spring multipart defaults (~16 MiB file / ~17 MiB request) so district-scale GeoJSON is not rejected by Boot’s default multipart caps before the geo-assets limits apply.

Accept Feature or FeatureCollection roots only; invalid GeoJSON fails validation via the same `GeoJsonLoader` / JTS path used at runtime.

## Console map preview usage

### Browse uploaded assets — `/geo-assets`

1. Open the operator console at `/console/` and navigate to **`/geo-assets`**.
2. Upload via the page Upload control (multipart to `/api/console/geo-assets`).
3. Select a list row (name, featureCount, bbox summary) to load that asset’s GeoJSON on the MapLibre map (`GET /api/console/geo-assets/{id}/geojson`).
4. Delete is inline on the list row; HTTP **409** means templates still reference the asset (usage hints include template id/name).

### Template editor — `geo_synthetic` hybrid preview

In the template editor Sources step, configure a `geo_synthetic` source (mode-switched fields + asset picker for boundary/network). The map preview region:

- Resolves **asset-id** or **path/`classpath:`** underlays through the **same server resolve spine** as execute (console does not reimplement `asset:` / classpath).
- Draws **BBOX / CIRCLE** guides client-side.
- Optionally samples synthetic points via the server preview endpoint, with a hard **count cap** (e.g. ≤500) and the configured **seed** surfaced in the UI.

### Seed honesty (preview ≠ full run)

Treat map preview as a **sampling aid**, not run output:

- When preview is active, the console shows a persistent alert: sample **count cap**, **seed** (when present), and explicit **“not a full run”** wording.
- Even when only geometry/guides are shown (no point sampling), a shorter honesty hint still appears.
- Full pipeline counts, transforms, and sinks apply only when you **run** the template — preview seed/cap do not redefine execute behavior.

Playwright/Podman map smoke exists from Phase 22 but is **not** a P0 merge-gate requirement; harness P1 linkage for geo-assets is owned by Phase 23 plan 02.

## Related API surface (operators)

| Method | Path | Role |
|--------|------|------|
| `POST` | `/api/console/geo-assets` | Multipart upload |
| `GET` | `/api/console/geo-assets` | List |
| `GET` | `/api/console/geo-assets/{id}` | Metadata |
| `GET` | `/api/console/geo-assets/{id}/geojson` | Authoritative GeoJSON body for map/runtime |
| `DELETE` | `/api/console/geo-assets/{id}` | Hard delete (409 if referenced) |
| `POST` | `/api/console/geo-assets/.../preview/synthetic` (or equivalent under the geo-assets controller) | Capped synthetic-point preview |

Exact preview path naming follows Phase 22 controller wiring; prefer the console UI over hand-crafted preview calls.
