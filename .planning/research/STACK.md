# Stack Research

**Domain:** v2.3 Geo Assets & Map Preview (GEO-05 upload + GEO-07 console map)
**Researched:** 2026-07-31
**Confidence:** HIGH

> Brownfield subsequent-milestone research. **Reuse** the shipped Java 25 / Spring Boot 4 / JPA metadata DB / React 19 console stack. Add **one** frontend map stack (MapLibre + react-map-gl), **one** JPA table pattern (CLOB inline GeoJSON), and **extend** existing resolver/upload patterns — no new Maven modules, no GIS servers, no PostGIS requirement for this milestone.

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Java | 25 | Runtime (unchanged) | Enforced by enforcer; no language bump for geo assets |
| Spring Boot | 4.0.5 | REST `/api/console/geo-assets`, multipart ingest | Same BOM as service; follow `ConsoleUdfController` multipart + `ConsoleApiAdvice` error envelope |
| Jakarta Persistence (H2 file metadata DB) | existing | Persist uploaded GeoJSON + list metadata | Project decision: assets live in metadata DB with secrets/templates/UDFs — single backup surface |
| `data-generator-geo` (`GeoJsonLoader`, JTS) | JTS 1.19.0 (root BOM) | Server-side GeoJSON parse/validate + runtime load | Already parses Feature/FeatureCollection; reuse for upload gate and `asset:` resolution — **no GeoTools addition** |
| **MapLibre GL JS** | **^5.24.0** (pin ≤5.x; avoid 6.0 pre-releases) | WebGL map renderer in console | Open-source, no Mapbox token; handles district-scale polygon boundaries (e.g. 南沙 fixtures) better than SVG/Canvas |
| **react-map-gl** (`react-map-gl/maplibre`) | **^8.1.0** | React 19 bindings for MapLibre | Official vis.gl wrapper; v8 splits MapLibre endpoint so **no** `mapbox-gl` dependency; peer `react >= 16.3` |
| React + Ant Design + Vite | React ^19, antd ^5.22, Vite ^6 | Asset browse/upload UI + embedded map panel | Existing console stack; map is a new page/panel, not a new SPA |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@turf/bbox` + `@turf/circle` + `@turf/helpers` | ^7.2.0 | Client fit-bounds, circle/bbox overlay for `geo_synthetic` preview | GEO-07 preview of BBOX/CIRCLE modes without round-tripping full synthesis; tree-shaken subpath imports |
| OpenStreetMap raster tiles | N/A (URL template) | Basemap for preview | `https://tile.openstreetmap.org/{z}/{x}/{y}.png` + OSM attribution; no tile server to deploy |
| Jackson 3 (`tools.jackson`) | 3.1.0 (existing) | Metadata JSON on asset rows (bbox summary, geometry types) | Same as `UdfArtifactPO.metadataJson` pattern |
| Spring `MultipartFile` | Boot 4 servlet | Binary upload transport | Same as UDF JAR + datasource driver upload paths |
| Playwright | ^1.49.1 (existing) | Console upload + map smoke E2E | Reuse Podman verify pipeline; mock GeoJSON fixtures |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| `.\mvnw-jdk25.ps1 -pl data-generator-service -am test` | Backend slice for geo-asset REST + resolver ITs | Extend calcite/geo modules only when runtime `asset:` resolution lands |
| `npm run build` in `data-generator-console-web` | Bundle size gate after MapLibre add | MapLibre adds ~400–600 KB gzip; acceptable for operator-only console |
| `.\scripts\verify-console.ps1` | Mandatory console regression after UI work | Per `.cursor/rules/console-verify.mdc` |
| `data.generator.geo-assets.*` properties | Operator-tunable limits | Add to `DataGeneratorProperties` (mirrors governance knobs elsewhere) |

## Installation

```bash
# Console — map preview (GEO-07)
cd data-generator-console-web
npm install react-map-gl@^8.1.0 maplibre-gl@^5.24.0
npm install @turf/bbox@^7.2.0 @turf/circle@^7.2.0 @turf/helpers@^7.2.0

# CSS import in map component entry (required by MapLibre)
# import 'maplibre-gl/dist/maplibre-gl.css';
```

```yaml
# data-generator-service/src/main/resources/application.yaml (GEO-05 — raise Boot defaults)
spring:
  servlet:
    multipart:
      max-file-size: 16MB      # Boot default 1MB rejects real district GeoJSON
      max-request-size: 17MB   # Boot default 10MB; headroom for multipart overhead

data:
  generator:
    geo-assets:
      max-bytes: 16777216      # 16 MiB service-layer cap (match servlet limit)
      max-feature-count: 5000  # reject pathological FeatureCollections at ingest
```

```java
// Runtime reference convention (extend GeoResourceResolver — no new Maven artifact)
// asset:550e8400-e29b-41d4-a716-446655440000
```

**Backend Maven:** no new production dependencies for GEO-05; validation calls existing `GeoJsonLoader` / `GeoResourceResolver` in `data-generator-geo` (already on service classpath via calcite).

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| **MapLibre GL JS + react-map-gl/maplibre** | **Leaflet + react-leaflet** | Leaflet wins for minimal bundle and simple pin maps; **reject** for v2.3 because boundary previews (MultiPolygon districts, road networks) need WebGL performance and consistent GeoJSON layer styling |
| **MapLibre GL JS** | **Mapbox GL JS** | Only if product later wants Mapbox-hosted styles/tiles and accepts token billing + ToS |
| **MapLibre GL JS** | **OpenLayers** | Better for full GIS editing suites; heavier API and bundle for read-only preview |
| **MapLibre GL JS** | **deck.gl** | Overkill for 2D asset browse + point overlay; adds WebGL data-viz stack unrelated to form workflows |
| **Inline CLOB GeoJSON in metadata DB** | **Filesystem + DB metadata row** | Filesystem is what `ConsoleUploadController` does today for CSV; **reject** for geo assets — orphan files, split backup, conflicts with PROJECT.md “metadata DB” decision |
| **Inline CLOB** | **Object storage (S3/MinIO)** | Future scale-out path; not v2.3 — adds infra, credentials, and out-of-scope connector work |
| **CLOB UTF-8 text** | **`bytea` / `@Lob` BLOB** | UdfArtifactPO documents H2 PostgreSQL-mode BLOB DDL pain; GeoJSON is UTF-8 text — CLOB matches `TaskExecutionPO.report_json` |
| **`asset:{uuid}` resolver prefix** | **Only absolute path refs** | Path/classpath stays for fixtures/tests; asset-id is the operator upload path (GEO-05) |
| **Server preview API** (`POST /api/console/geo/preview`) | **Client-only synthesis preview** | Use **both**: server endpoint returns authoritative sample points via `GeoSyntheticGenerator` (small `count` cap); client `@turf` draws bbox/circle/footprint instantly while config edits |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **PostGIS as a v2.3 requirement** | POSTGIS source already exists for warehouse reads; asset upload + preview are metadata-DB + in-memory JTS | Optional POSTGIS unchanged; do not gate GEO-05/07 on spatial DB |
| **GeoServer / MapServer / Martin / pg_tileserv** | Heavy ops footprint for read-only preview | MapLibre + OSM raster tiles + inline GeoJSON sources |
| **New Java geo stack** (GeoTools beyond existing, spatial4j, etc.) | `GeoJsonLoader` + JTS already parse/validate WGS84 GeoJSON | Extend `GeoResourceResolver` with `asset:` branch |
| **Shapefile / GeoPackage / KML ingest** | Explicitly deferred in geospatial overview | GeoJSON-only upload; clear 400 on wrong extension/MIME |
| **CRS reprojection service (PROJ server-side)** | WGS84-only assumption is documented project-wide | Reject non-WGS84 at upload with actionable error |
| **Polygon/MultiPolygon synthesis (GEO-06)** | Out of scope v2.3 | Preview may *display* uploaded polygons; do not generate them |
| **Replacing `ConsoleUploadController` disk path flow** | CSV/JSON template sources still need filesystem paths | Parallel **geo-asset registry**; deprecate disk GeoJSON paths later if desired |
| **Spring Security / OAuth for upload** | Console RBAC remains header opt-in default-off | Reuse existing `ConsoleAuthorizationFilter` when enabled |
| **P0 harness row inflation** | PROJECT.md freezes P0 at 15 | Add P1 row `geo-assets` + Playwright smoke; non-blocking |
| **mapbox-gl npm package** | Token + license; react-map-gl v8 MapLibre endpoint removes need | `react-map-gl/maplibre` + `maplibre-gl` only |
| **Embedded map tile generation on JVM** | No server-side rendering requirement | Client fetches public OSM tiles |
| **Redis / CDN cache for assets** | Premature; H2 CLOB reads are fine at operator scale | Direct JPA fetch; add cache only if profiling demands |

## Stack Patterns by Variant

**If GEO-05 — GeoJSON asset upload + persistence:**

- REST surface: `ConsoleGeoAssetController` at `/api/console/geo-assets` mirroring UDF/secrets CRUD shape (`R<T>` envelope, `ConsoleApiAdvice`).
- Upload: `POST multipart/form-data` with `file` + optional `name`; validate **before** persist:
  1. Size ≤ `data.generator.geo-assets.max-bytes`
  2. Filename ends with `.geojson` or `.json` (after sanitize — reuse `ConsoleUploadController.sanitizeFileName` pattern)
  3. Content-Type in `{application/geo+json, application/json}` OR `application/octet-stream` with geo extension
  4. Parse via `GeoJsonLoader` / Jackson — root must be `Feature` or `FeatureCollection`
  5. Compute bbox + geometry-type summary + SHA-256 hash for list views
- Schema (`geo_asset` table):

  | Column | Type | Notes |
  |--------|------|-------|
  | `id` | `VARCHAR(36)` PK | UUID string returned as asset-id |
  | `name` | `VARCHAR(256)` | Operator label (default from filename) |
  | `content` | `CLOB` | Full GeoJSON UTF-8 (inline — not echoed in list DTO) |
  | `content_hash` | `CHAR(64)` | SHA-256 hex for dedup/audit |
  | `byte_size` | `BIGINT` | Stored bytes |
  | `feature_count` | `INT` | Denormalized for list |
  | `geometry_summary` | `VARCHAR(512)` | e.g. `Polygon,MultiLineString` |
  | `bbox_json` | `VARCHAR(128)` | `[minLon,minLat,maxLon,maxLat]` for map zoom |
  | `created_at` / `updated_at` | `TIMESTAMP` | Instant |
  | `status` | `VARCHAR(16)` | `ACTIVE` / `DEPRECATED` |

- List/get DTOs **omit** `content` (same rule as UDF payload omission); download/preview endpoints return GeoJSON explicitly.
- Runtime: extend `GeoResourceResolver.readUtf8` — `asset:{uuid}` loads CLOB via `GeoAssetRepository`; wire into `GeoSyntheticRowSource` + `GeoJsonRowSource`.
- VO fields: add optional `boundaryAssetId` / `networkAssetId` on `GeoSyntheticSourceVO`, `assetId` on `GeoJsonSourceVO`; validation = exactly one of path or asset-id per slot.

**If GEO-07 — console map preview (assets + geo_synthetic config):**

- Pick **MapLibre + react-map-gl/maplibre** (single map stack for the whole milestone).
- Page layout: Ant Design `Upload.Dragger` + `Table` asset list + split `Map` panel (reuse console list/detail patterns from datasources/UDFs).
- Asset preview: `GET /api/console/geo-assets/{id}/geojson` → MapLibre `Source type="geojson"` + `Layer` fill/line depending on geometry types; `fitBounds` via `@turf/bbox`.
- `geo_synthetic` preview (equal depth):
  - **Config panel** on template editor geo source step — live map beside YAML form fields.
  - Overlays by mode: boundary/network GeoJSON from asset-id or fixture path; bbox rectangle; circle from `@turf/circle`; sample points from `POST /api/console/geo/preview` (cap `count` ≤ 200, reuse `GeoSyntheticGenerator`).
  - Do **not** implement polygon synthesis preview (GEO-06 deferred) — only show uploaded polygon boundaries as context for point modes.
- Basemap: OSM raster tiles; document attribution in map footer (license compliance).
- i18n: add keys under existing `i18next` namespaces; no new i18n library.

**If multipart limits / abuse:**

- Configure `spring.servlet.multipart.*` globally (see Installation) — 1 MB Boot default **will break** real uploads silently.
- Service-layer byte cap + feature-count cap as defense in depth (reject before CLOB write).
- Optional: wire `AuditService` on upload/delete like UDF publish (governance parity, not a new stack).

**If testing:**

- Backend: `MockMultipartFile` tests like `ConsoleUdfControllerTest` / `ConsoleUploadControllerTest`; resolver IT with H2 CLOB round-trip.
- Frontend: Playwright spec uploads small fixture GeoJSON, asserts map canvas visible + asset appears in list.
- Harness: P1 row only (`geo-assets`); P0 stays 15.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| `react-map-gl@^8.1` | `maplibre-gl@^4 \|\| ^5` | Import from `react-map-gl/maplibre`, not default export (Mapbox-typed) |
| `react-map-gl@^8.1` | `react@^19` | Peer dep `>=16.3`; MapLibre ecosystem validated React 19 in 2025–2026; pin and run `npm run build` in CI |
| `maplibre-gl@5.x` | Avoid `6.0` pre-releases for v2.3 | v6 ESM breaking migration — upgrade separately after v2.3 ships |
| Spring Boot 4.0.5 | Multipart defaults 1 MB / 10 MB | **Must override** for GEO-05 |
| H2 file metadata (PostgreSQL mode) | `columnDefinition = "CLOB"` | Same Hibernate DDL pattern as `UdfArtifactPO.metadataJson`, `TaskExecutionPO.report_json` |
| JTS 1.19.0 | GeoJSON WGS84 (`x=lon, y=lat`) | Matches existing loader; no CRS transform |
| Vite 6 + MapLibre CSS | Side-effect CSS import in map component | No SSR — console is SPA-only |
| OSM tile usage policy | Operator console low traffic | Cache-bust via standard `{z}/{x}/{y}`; show attribution |

## Sources

- `.planning/PROJECT.md` — v2.3 milestone scope, metadata DB decision, GEO-05/07 equal depth, GEO-06 out
- `data-generator-geo/.../GeoResourceResolver.java` — classpath/filesystem resolution to extend with `asset:`
- `data-generator-geo/.../GeoJsonLoader.java` — parse/validate reuse for upload gate
- `data-generator-service/.../UdfArtifactPO.java` — CLOB/`bytea` DDL patterns for metadata DB blobs
- `data-generator-service/.../ConsoleUdfController.java` — multipart upload + governance precedent
- `data-generator-service/.../ConsoleUploadController.java` — filename sanitize + empty-file rejection
- `data-generator-common/.../GeoSyntheticSourceVO.java`, `GeoJsonSourceVO.java` — path fields to complement with asset-id
- `docs/geospatial-overview.md` — deferred items (upload, map UI) now v2.3 scope
- `data-generator-console-web/package.json` — React 19 / antd 5 / Vite 6 baseline
- [react-map-gl docs](https://visgl.github.io/react-map-gl/docs/whats-new) — v8 MapLibre endpoint, MapLibre GL v5 support (HIGH)
- [MapLibre GL JS releases](https://maplibre.org/maplibre-gl-js/docs/) — v5 stable line (HIGH)
- [Spring Boot MultipartProperties](https://docs.spring.io/spring-boot/4.1/api/java/org/springframework/boot/servlet/autoconfigure/MultipartProperties.html) — 1 MB / 10 MB defaults (HIGH)

---
*Stack research for: v2.3 Geo Assets & Map Preview (GEO-05 + GEO-07)*
*Researched: 2026-07-31*
