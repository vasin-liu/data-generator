# Geospatial Phase 1 — Usage (synthetic GEO iterator)

This note documents **Phase 1** synthetic geospatial generation: `iterator.type: GEO` (V1 + V2) and minimal SpEL.

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

For ad-hoc spatial SQL or chunked export, use **`type: QUERY`** with `ST_AsText(geom)` in your SQL instead.

See `docs/superpowers/specs/2026-05-21-geospatial-phase2b-design.md`.

## Related

- Design: `docs/superpowers/specs/2026-05-20-geospatial-phase1-design.md`
- Plan: `docs/superpowers/plans/2026-05-20-geospatial-phase1.md`
- Phase 2B: `docs/superpowers/specs/2026-05-21-geospatial-phase2b-design.md`
