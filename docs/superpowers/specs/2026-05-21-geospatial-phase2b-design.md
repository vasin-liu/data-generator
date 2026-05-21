# Geospatial Phase 2B — GeoJSON File RowSource Design

## Metadata

| Field | Value |
|-------|-------|
| Status | Implemented (file + PostGIS table slices) |
| Date | 2026-05-21 |
| Author | Gensokyo |
| Depends on | Phase 1 (`data-generator-geo`, GEO iterator, SpEL) |
| **Phase 2B scope** | **B (partial)** — read real GeoJSON files as Template V2 `RowSource` |
| Deferred | Chunked PostGIS reads, arbitrary spatial SQL, Calcite spatial UDFs (Phase C) |

## Problem statement

Phase 1 only **synthesizes** locations (boundary points, line samples). Operators also need to **read existing GeoJSON** features (boundaries, assets, POIs) into Template V2 SQL pipelines without ad-hoc JSON parsing.

## Goals (Phase 2B — file slice)

1. Add **`sources[].type: GEOJSON`** (`GeoJsonSourceVO`, `@JsonSubType("GEOJSON")`).
2. Materialize **`Feature`** and **`FeatureCollection`** roots via `GeoJsonLoader.loadFeatureCollection`.
3. Reuse Phase 1 **output contract**: `columns` (representative point for non-Point geometries), `geojson`, `wkt`, optional `prop.<key>`.
4. Register **`GeoJsonSourceFactory`** in builtin plugin + Spring `CoreConfig`.
5. Share **`GeoRowSchemaSupport`** with `IteratorRowSource` for schema inference.

## Goals (Phase 2B — PostGIS table slice)

1. Add **`sources[].type: POSTGIS`** (`PostGisQuerySourceVO`).
2. Generate **`ST_PointOnSurface` / `ST_AsText` / `ST_AsGeoJSON`** projections from `table` + `geometryColumn`.
3. Delegate execution to existing **`QueryRowSource`** (finite JDBC materialization).
4. Register factory on **`JdbcTemplateTemplateV2RuntimePluginProvider`** (requires JDBC + PostGIS extension).

## Non-goals (remaining)

- Chunked streaming PostGIS source (use `QUERY` + `CHUNKED` policy for raw SQL today)
- Streaming / NDJSON GeoJSON
- CRS reprojection
- Shapefile / GeoPackage

## Architecture

```
GeoJsonSourceVO (core)
    ↓
GeoJsonRowSource (calcite) → GeoJsonLoader.loadFeatureCollection
    ↓
GeoFeatureRowFormatter → Row + RowSchema (GeoRowSchemaSupport)
```

### New / moved types

| Module | Type | Role |
|--------|------|------|
| `data-generator-core` | `GeoJsonSourceVO`, `GeoJsonSourceOutputVO` | Template V2 config |
| `data-generator-geo` | `GeoJsonLoader.loadFeatureCollection` | Parse all features |
| `data-generator-geo` | `GeoFeatureRowFormatter`, `GeoJsonGeometryEncoder` | Row + geometry encoding |
| `data-generator-calcite` | `GeoJsonRowSource`, `GeoJsonSourceFactory`, `GeoRowSchemaSupport` | V2 materialization |

### `columns` format for arbitrary geometries

Non-`Point` geometries expose **lat/lon** from JTS `getInteriorPoint()` (representative location), not full geometry vertices. Use `output.format: geojson` or `wkt` for full geometry in one column.

## Template example

```yaml
sources:
  - name: assets
    type: GEOJSON
    path: classpath:geo/two_feature_collection.geojson
    maxRows: 1000
    output:
      format: columns
      includeProperties: true
transforms:
  - type: SQL
    sql: SELECT lat, lon, prop.id FROM assets
```

## Revision history

| Date | Change |
|------|--------|
| 2026-05-21 | Initial Phase 2B file-source design + implementation |
| 2026-05-21 | PostGIS table source (`POSTGIS`) via generated SQL + QueryRowSource |
