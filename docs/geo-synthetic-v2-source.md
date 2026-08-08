# Template V2 `geo_synthetic` source

Short maintainer reference for the v2.2 synthetic geo source and v2.3 asset-id binding.  
**Overview landing page:** [`geospatial-overview.md`](geospatial-overview.md)  
**Geo asset upload / map preview:** [`geo-assets.md`](geo-assets.md)  
**Design spec:** [`superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md`](superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md)

Use `type: geo_synthetic` when operators need generated point rows (boundary, line sample, bbox, circle) in a Template V2 pipeline **without** the V1 `ITERATOR` + `GEO` tree. Use `type: geojson` when reading an existing Feature / FeatureCollection file (classpath, filesystem, or metadata-DB geo asset).

## Minimal template example

Classpath fixtures below match `data-generator-calcite/src/test/resources/geo/` (e.g. `南沙区边界.geojson`, `南沙区道路路网.geojson`).

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: BOUNDARY_POINTS   # BOUNDARY_POINTS | LINE_SAMPLE | BBOX | CIRCLE
    count: 100
    seed: 42
    minDistanceMeters: 0
    boundaryPath: classpath:geo/南沙区边界.geojson      # BOUNDARY_POINTS
    networkPath: classpath:geo/南沙区道路路网.geojson    # LINE_SAMPLE
    featureIndex: 0
    randomFeature: false
    sample:
      strategy: EVEN
      spacingMeters: 50
    bbox: [113.2, 23.0, 113.5, 23.2]   # [minLon, minLat, maxLon, maxLat] — BBOX
    center: [113.3, 23.1]                # CIRCLE
    radiusMeters: 500
    output:
      format: columns
      columnNames:
        lon: lon
        lat: lat
      includeProperties: false

transform:
  - type: sql
    sql: |
      select lon, lat from pts

sink:
  - type: console
```

Only the fields required by the chosen `mode` are read; others are ignored (see validation table).

### Mode-specific snippets

**BOUNDARY_POINTS** — points along a polygon boundary (classpath path):

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: BOUNDARY_POINTS
    count: 10
    seed: 42
    boundaryPath: classpath:geo/南沙区边界.geojson
```

**BOUNDARY_POINTS** — same mode with a metadata-DB geo asset (`boundaryAssetId`):

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: BOUNDARY_POINTS
    count: 10
    seed: 42
    boundaryAssetId: 11111111-1111-1111-1111-111111111111
```

**LINE_SAMPLE** — points sampled along a line network (classpath path):

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: LINE_SAMPLE
    networkPath: classpath:geo/南沙区道路路网.geojson
    sample:
      strategy: EVEN
      spacingMeters: 50
```

**LINE_SAMPLE** — same mode with `networkAssetId`:

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: LINE_SAMPLE
    networkAssetId: 22222222-2222-2222-2222-222222222222
    sample:
      strategy: EVEN
      spacingMeters: 50
```

**BBOX** — uniform random points in a lon/lat rectangle (no file):

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: BBOX
    count: 4
    seed: 1
    bbox: [113.2, 23.0, 113.5, 23.2]
```

**CIRCLE** — area-uniform points inside a Haversine radius (no file):

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: CIRCLE
    count: 4
    seed: 1
    center: [113.3, 23.1]
    radiusMeters: 500
```

### Mode validation

| Mode | Required | Notes |
|------|----------|-------|
| `BOUNDARY_POINTS` | `boundaryPath` **or** `boundaryAssetId`, `count > 0` | Ignores bbox, circle, network |
| `LINE_SAMPLE` | `networkPath` **or** `networkAssetId`, sample strategy | Ignores bbox, circle, boundary |
| `BBOX` | valid four-tuple `bbox`, `count > 0` | `minLon < maxLon`, `minLat < maxLat`; no file |
| `CIRCLE` | `center`, `radiusMeters > 0`, `count > 0` | No file; points inside Haversine radius |

Invalid config throws `IllegalArgumentException` with source name and field detail (console-visible).  
`count` hard cap: `<= 1_000_000`.

### Path vs asset-id binding (v2.3)

Boundary and network GeoJSON resolve through the shared `GeoResourceResolver` / `GeoAssetResolver` spine:

| Role | Dedicated field | Path field | Notes |
|------|-----------------|------------|-------|
| Boundary (`BOUNDARY_POINTS`) | `boundaryAssetId` | `boundaryPath` | Dedicated field is a metadata-DB UUID |
| Network (`LINE_SAMPLE`) | `networkAssetId` | `networkPath` | Same for line networks |
| Read-only GeoJSON source | `assetId` | `path` | `type: geojson` / `GEOJSON` |

**Dual-binding (two paths — do not conflate):** when both a path field and the matching asset-id field appear for the same role, behavior depends on surface:

| Surface | Behavior |
|---------|----------|
| **Editor / console preview** | **Asset-id is preferred** for preview calls (client clears path when asset-id is set); inline warning remains UX. See [`geo-assets.md`](geo-assets.md) console map preview. |
| **Runtime execute** | Mapper **fail-fasts** if both path and asset-id are present on the saved source (`IllegalArgumentException` naming source + fields). Leave **one binding only** in YAML/draft before run — there is **no** silent runtime “asset-id wins” preference. |

Path and `classpath:` remain first-class when asset-id is absent (GEO-03).

**Wire format `asset:{uuid}`:** authors may also put `asset:{uuid}` on the path fields (`boundaryPath`, `networkPath`, or geojson `path`). Dedicated asset-id fields normalize to the same `asset:{uuid}` form before resolve — one spine, two authoring styles.

Example — `asset:{uuid}` on a path field:

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: BOUNDARY_POINTS
    count: 10
    seed: 42
    boundaryPath: "asset:11111111-1111-1111-1111-111111111111"
```

Example — `type: geojson` with `assetId`:

```yaml
sources:
  districts:
    type: geojson
    assetId: 33333333-3333-3333-3333-333333333333
```

Equivalent wire form on `path`:

```yaml
sources:
  districts:
    type: geojson
    path: "asset:33333333-3333-3333-3333-333333333333"
```

Upload, list, delete, size limits, and console map preview are documented in [`geo-assets.md`](geo-assets.md) (not in this YAML reference).

## Pipeline shape

Phase 20 pipeline proof uses passthrough SQL and console sink (see `TemplateV2RunnerGeoSyntheticSourceTests`):

```
geo_synthetic source → SQL transform (select lon, lat from <source>) → sink
```

Adjust column names in SQL when `output.columnNames` overrides defaults.

## Output formats

`output.format` maps to `GeoOutputFormatKind` on `GeoSyntheticSourceOutputVO`:

| `format` | Row shape | SQL notes |
|----------|-----------|-----------|
| `columns` (default) | Separate lon/lat columns via `output.columnNames` (defaults `lon`, `lat`) | Passthrough: `select lon, lat from pts` |
| `wkt` | Single geometry column (WKT `POINT`) | Select the WKT column name declared in output config |
| `geojson` | Single geometry column (GeoJSON geometry object) | Select the geometry column; use with `V2_GEO_*` GeoJSON predicates |

`includeProperties: true` adds `prop.*` columns from source GeoJSON features (boundary/line modes only).  
Column name collisions are rejected at generation time (`ensureNoColumnCollisions`).

Example **wkt** output:

```yaml
    output:
      format: wkt
      columnNames:
        geometry: geom_wkt
```

Example **geojson** output:

```yaml
    output:
      format: geojson
      columnNames:
        geometry: geom_json
```

## SQL companion

This milestone adds **no new `ST_*` or additional `V2_GEO_*` functions**. Transform SQL may use the existing in-memory geo functions already registered in `TemplateV2SqlFunctionRegistry` (listed in [`geospatial-overview.md`](geospatial-overview.md#built-in-sql-functions-in-memory-wgs84)):

- Filter synthetic points inside a boundary: `V2_GEO_POINT_IN_GEOJSON(lon, lat, '<geojson>')`
- Radius filter: `V2_GEO_WITHIN_RADIUS(lon, lat, centerLon, centerLat, meters)`
- Distance: `V2_GEO_DISTANCE_METERS(lon1, lat1, lon2, lat2)`

Example filter (optional — default pipeline proof uses passthrough only):

```yaml
transform:
  - type: sql
    sql: |
      select lon, lat
      from pts
      where V2_GEO_WITHIN_RADIUS(lon, lat, 113.3, 23.1, 1000)
```

Warehouse-scale spatial joins and native PostGIS `ST_*` remain on **`POSTGIS`** / **`QUERY`** JDBC sources, not inside the Calcite in-memory function set.

## Related docs

- V1 iterator / SpEL guide: [`geospatial-phase1-usage.md`](geospatial-phase1-usage.md) — legacy `ITERATOR` + `GEO` only; prefer `geo_synthetic` for new Template V2 templates.
- Read-only file source: `GEOJSON` row in [`geospatial-overview.md`](geospatial-overview.md#template-v2-source-types).
- Geo asset registry, upload limits, and console map preview: [`geo-assets.md`](geo-assets.md).
