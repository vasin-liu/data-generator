# Geospatial Phase 2C — Template V2 Geo SQL Functions

## Metadata

| Field | Value |
|-------|-------|
| Status | Implemented (distance, radius, WKT + GeoJSON predicates) |
| Date | 2026-05-21 |
| Depends on | Phase 1, Phase 2B/D (geo sources in Calcite) |
| **Phase 2C scope** | **C (minimal)** — lat/lon SQL helpers backed by `data-generator-geo` |

## Problem statement

Operators filter and enrich geo pipelines in SQL transforms. PostGIS `ST_*` is available only on JDBC `POSTGIS`/`QUERY` sources. For in-memory `GEOJSON` / `GEO` iterator rows, we need portable functions on `lat`/`lon` columns.

## Goals (Phase 2C — distance + radius slice)

1. Register **`V2_GEO_DISTANCE_METERS(lat1, lon1, lat2, lon2)`** in `TemplateV2SqlFunctionRegistry.builtIn()`.
2. Register **`V2_GEO_WITHIN_RADIUS(lat, lon, centerLat, centerLon, radiusMeters)`** (boolean filter helper).
3. Delegate to **`GeoHaversine.distanceMeters`** (`data-generator-geo`).
4. Add unit + runner tests; document in `docs/geospatial-phase1-usage.md`.

## Goals (Phase 2C — WKT predicate slice)

1. **`GeoWktPredicates`** in `data-generator-geo` (parse + JTS `intersects` / `contains` / point-in-region).
2. SQL functions **`V2_GEO_WKT_INTERSECTS`**, **`V2_GEO_WKT_CONTAINS`**, **`V2_GEO_POINT_IN_WKT`**.
3. Unit tests in `data-generator-geo` and runner tests over `GEOJSON` sources.

## Goals (Phase 2C — GeoJSON predicate slice)

1. **`GeoJsonLoader.parseGeometryJson`** for inline geometry / Feature JSON.
2. **`GeoJsonPredicates`** + SQL **`V2_GEO_POINT_IN_GEOJSON`**, **`V2_GEO_GEOJSON_CONTAINS`**, **`V2_GEO_GEOJSON_INTERSECTS`**.

## Non-goals (remaining)

- `ST_Buffer`, buffer on geometry columns
- CRS reprojection
- Replacing PostGIS for warehouse-scale spatial joins

## Example

```sql
SELECT lat, lon,
       V2_GEO_DISTANCE_METERS(lat, lon, 22.2, 113.2) AS dist_m
FROM geo_in
WHERE V2_GEO_WITHIN_RADIUS(lat, lon, 22.2, 113.2, 5000)
```

Equivalent filter using distance:

```sql
WHERE V2_GEO_DISTANCE_METERS(lat, lon, 22.2, 113.2) < 5000
```

## Revision history

| Date | Change |
|------|--------|
| 2026-05-21 | `V2_GEO_DISTANCE_METERS` built-in SQL function |
| 2026-05-21 | `V2_GEO_WITHIN_RADIUS` built-in SQL function |
| 2026-05-21 | WKT predicates: `V2_GEO_POINT_IN_WKT`, `V2_GEO_WKT_CONTAINS`, `V2_GEO_WKT_INTERSECTS` |
| 2026-05-21 | GeoJSON predicates: `V2_GEO_POINT_IN_GEOJSON`, `V2_GEO_GEOJSON_CONTAINS`, `V2_GEO_GEOJSON_INTERSECTS` |
