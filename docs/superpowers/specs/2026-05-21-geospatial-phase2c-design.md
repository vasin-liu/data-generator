# Geospatial Phase 2C — Template V2 Geo SQL Functions

## Metadata

| Field | Value |
|-------|-------|
| Status | Implemented (distance slice) |
| Date | 2026-05-21 |
| Depends on | Phase 1, Phase 2B/D (geo sources in Calcite) |
| **Phase 2C scope** | **C (minimal)** — lat/lon SQL helpers backed by `data-generator-geo` |

## Problem statement

Operators filter and enrich geo pipelines in SQL transforms. PostGIS `ST_*` is available only on JDBC `POSTGIS`/`QUERY` sources. For in-memory `GEOJSON` / `GEO` iterator rows, we need portable functions on `lat`/`lon` columns.

## Goals (Phase 2C — distance slice)

1. Register **`V2_GEO_DISTANCE_METERS(lat1, lon1, lat2, lon2)`** in `TemplateV2SqlFunctionRegistry.builtIn()`.
2. Delegate to **`GeoHaversine.distanceMeters`** (`data-generator-geo`).
3. Add unit + runner tests; document in `docs/geospatial-phase1-usage.md`.

## Non-goals (remaining)

- Full JTS geometry UDFs (`ST_Buffer`, `ST_Intersects` on WKT/GeoJSON columns)
- CRS reprojection
- Replacing PostGIS for warehouse-scale spatial joins

## Example

```sql
SELECT lat, lon,
       V2_GEO_DISTANCE_METERS(lat, lon, 22.2, 113.2) AS dist_m
FROM geo_in
WHERE V2_GEO_DISTANCE_METERS(lat, lon, 22.2, 113.2) < 5000
```

## Revision history

| Date | Change |
|------|--------|
| 2026-05-21 | `V2_GEO_DISTANCE_METERS` built-in SQL function |
