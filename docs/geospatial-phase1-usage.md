# Geospatial usage (Phases 1, 2B, 2C, 2D)

**Overview:** [`geospatial-overview.md`](geospatial-overview.md)

This note documents synthetic **`iterator.type: GEO`** (Phase 1), **GEOJSON/POSTGIS** sources (2B), **SQL over geo tables** (2D), and **`V2_GEO_*` functions** (2C).

## Limitations

- **WGS84 only** (no reprojection). Coordinates are **longitude = x**, **latitude = y**.
- **Engineering accuracy** — JTS `contains` + envelope sampling; haversine used for spacing checks. Not for legal survey.
- **Large files** — GeoJSON is read fully into memory. Keep fixtures modest (see spec soft limit ~50 MB).

## V1 — root iterator

```yaml
iterator:
  type: GEO
  mode: BOUNDARY_POINTS
  boundaryPath: classpath:geo/南沙区边界.geojson
  featureIndex: 0
  count: 100
  seed: 42
  minDistanceMeters: 0
  output:
    format: columns   # columns | geojson | wkt
```

## V2 — `IteratorSourceVO`

```yaml
sources:
  - name: geo_in
    type: ITERATOR
    iterator:
      type: GEO
      mode: BOUNDARY_POINTS
      boundaryPath: classpath:geo/南沙区边界.geojson
      count: 10
      seed: 1
      output:
        format: columns
transforms:
  - type: SQL
    sql: SELECT lat, lon FROM geo_in
```

Template V2 materializes a **finite** row list (same model as number/constant/datetime iterators).

## Line sampling (road / network GeoJSON)

```yaml
iterator:
  type: GEO
  mode: LINE_SAMPLE
  networkPath: classpath:geo/南沙区道路路网.geojson
  featureIndex: 0
  randomFeature: false
  seed: 7
  sample:
    strategy: BY_COUNT              # or BY_SPACING_METERS
    spacingMeters: 100              # required when strategy = BY_SPACING_METERS
  count: 50                         # required for BY_COUNT; ignored for BY_SPACING_METERS
  output:
    format: wkt
    includeProperties: false        # true → extra columns prop.<key>
```

For `MultiLineString` features, the **longest** line component is sampled.

## SpEL

- **`#geo.pointsInBoundary('classpath:geo/南沙区边界.geojson', 5, 0, 99L)`** — returns `List<Map>` with `lat` / `lon`.
- **`#faker.geo().pointsInBoundary(...)`** — same helpers on the DataFaker provider.

Trajectory / line sampling is **not** exposed in SpEL (iterator only).

## Phase 2B — read GeoJSON files (`type: GEOJSON`)

Reads **real** GeoJSON `Feature` / `FeatureCollection` files (not synthetic). Same output knobs as the GEO iterator.

```yaml
sources:
  - name: poi_layer
    type: GEOJSON
    path: classpath:geo/two_feature_collection.geojson
    maxRows: 500
    output:
      format: columns        # columns | geojson | wkt
      includeProperties: true
transforms:
  - type: SQL
    sql: SELECT lat, lon, prop.id FROM poi_layer
```

- **`path`**: `classpath:` or filesystem, resolved by `GeoResourceResolver`.
- **`columns`**: Point features use coordinates; polygons/lines use an interior representative point (`getInteriorPoint()`).
## Phase 2B — PostGIS table (`type: POSTGIS`)

Requires a **PostGIS-enabled** PostgreSQL datasource (JDBC plugin). Projects geometries with PostGIS functions, then reads via the standard query path.

```yaml
sources:
  - name: sites_in
    type: POSTGIS
    dataSourceId: postgres_main
    table: sites
    geometryColumn: geom
    attributes: [id]
    output:
      format: columns
      includeProperties: true
transforms:
  - type: SQL
    sql: SELECT lat, lon, prop.id FROM sites_in
```

For ad-hoc spatial SQL, use **`type: QUERY`** with `ST_AsText(geom)` in your SQL.

**Chunked export:** set `executionPolicy.mode: CHUNKED` on the template; `POSTGIS` sources use the same JDBC chunk reader as `QUERY` (generated `ST_*` SQL).

See `docs/superpowers/specs/2026-05-21-geospatial-phase2b-design.md`.

## Phase 2D — SQL over geo sources (Calcite)

`GEOJSON`, `POSTGIS`, and `ITERATOR`+`GEO` sources are registered in the Calcite execution context like CSV/JSON. Use a normal **`type: SQL`** transform:

```yaml
sources:
  - name: geo_in
    type: GEOJSON
    path: classpath:geo/two_feature_collection.geojson
transforms:
  - type: SQL
    sql: SELECT lat, lon FROM geo_in WHERE lat > 22.0
```

Requires **`executionPolicy.mode: IN_MEMORY`** (default) so sources are materialized before SQL. See `docs/superpowers/specs/2026-05-21-geospatial-phase2d-design.md`.

## Phase 2C — geo SQL functions (in-memory lat/lon)

Built-in Calcite SQL helpers for WGS84 columns (not PostGIS `ST_*`):

| Function | Args | Returns |
|----------|------|---------|
| `V2_GEO_DISTANCE_METERS` | `lat1, lon1, lat2, lon2` | Great-circle distance in meters |
| `V2_GEO_WITHIN_RADIUS` | `lat, lon, centerLat, centerLon, radiusMeters` | `true` when point is within radius |
| `V2_GEO_POINT_IN_WKT` | `lat, lon, areaWkt` | `true` when point lies in WKT region |
| `V2_GEO_WKT_CONTAINS` | `outerWkt, innerWkt` | `true` when outer geometry contains inner |
| `V2_GEO_WKT_INTERSECTS` | `wkt1, wkt2` | `true` when geometries intersect |
| `V2_GEO_POINT_IN_GEOJSON` | `lat, lon, areaGeoJson` | `true` when point lies in GeoJSON geometry |
| `V2_GEO_GEOJSON_CONTAINS` | `outerGeoJson, innerGeoJson` | `true` when outer contains inner |
| `V2_GEO_GEOJSON_INTERSECTS` | `geoJson1, geoJson2` | `true` when geometries intersect |

GeoJSON arguments accept a **geometry object** (`{"type":"Polygon",...}`) or a **Feature** with a `geometry` field. Use with `output.format: geojson` so rows expose a `geometry` column.

**Buffer (approximate `ST_Buffer` on WGS84):**

| Function | Args | Returns |
|----------|------|---------|
| `V2_GEO_WKT_BUFFER` | `wkt, distanceMeters` | Buffered geometry as WKT |
| `V2_GEO_GEOJSON_BUFFER` | `geoJson, distanceMeters` | Buffered geometry as GeoJSON |

Meters are converted to degrees at the geometry centroid (engineering accuracy; keep radii modest, e.g. &lt; 10 km).

```sql
SELECT lat, lon
FROM geo_in
WHERE V2_GEO_POINT_IN_WKT(
  lat, lon,
  V2_GEO_WKT_BUFFER('POINT(113.2 22.2)', 5000))
```

```yaml
transforms:
  - type: SQL
    sql: |
      SELECT lat, lon,
             V2_GEO_DISTANCE_METERS(lat, lon, 22.2, 113.2) AS dist_m
      FROM geo_in
      WHERE V2_GEO_WITHIN_RADIUS(lat, lon, 22.2, 113.2, 5000)
```

See `docs/superpowers/specs/2026-05-21-geospatial-phase2c-design.md`.

## Related

- Design: `docs/superpowers/specs/2026-05-20-geospatial-phase1-design.md`
- Plan: `docs/superpowers/plans/2026-05-20-geospatial-phase1.md`
- Phase 2B: `docs/superpowers/specs/2026-05-21-geospatial-phase2b-design.md`
- Phase 2D: `docs/superpowers/specs/2026-05-21-geospatial-phase2d-design.md`
- Phase 2C: `docs/superpowers/specs/2026-05-21-geospatial-phase2c-design.md`
