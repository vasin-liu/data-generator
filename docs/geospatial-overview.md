# Geospatial capability overview

Geospatial support on `feature-4.0` spans synthetic generation (Phase 1), Template V2 sources (Phase 2B), Calcite SQL over geo tables (Phase 2D), and in-memory geo SQL functions (Phase 2C).

**Operator guide:** [`geospatial-phase1-usage.md`](geospatial-phase1-usage.md) (YAML, SpEL, SQL examples).

## Phase status

| Phase | Scope | Status | Design |
|-------|--------|--------|--------|
| **1** | `iterator.type: GEO`, SpEL `#geo.*`, `data-generator-geo` | Complete | [`specs/2026-05-20-geospatial-phase1-design.md`](superpowers/specs/2026-05-20-geospatial-phase1-design.md) |
| **2B** | `GEOJSON` file source, `POSTGIS` table source, CHUNKED PostGIS | Complete | [`specs/2026-05-21-geospatial-phase2b-design.md`](superpowers/specs/2026-05-21-geospatial-phase2b-design.md) |
| **2D** | SQL transforms over geo `RowSource` types | Complete | [`specs/2026-05-21-geospatial-phase2d-design.md`](superpowers/specs/2026-05-21-geospatial-phase2d-design.md) |
| **2C** | `V2_GEO_*` Calcite SQL functions (distance, WKT/GeoJSON predicates, buffer) | Complete | [`specs/2026-05-21-geospatial-phase2c-design.md`](superpowers/specs/2026-05-21-geospatial-phase2c-design.md) |

## Modules

| Module | Role |
|--------|------|
| `data-generator-geo` | GeoJSON I/O, JTS predicates, haversine, synthetic generator, row formatting |
| `data-generator-iterator-geo` | V1/V2 `GEO` iterator |
| `data-generator-calcite` | `GeoJsonRowSource`, `PostGisQuerySourceSupport`, `TemplateV2GeoSqlFunctions` |
| `data-generator-core` | `GeoJsonSourceVO`, `PostGisQuerySourceVO`, iterator `GEO` VO |

## Template V2 source types

| `type` | Use when |
|--------|----------|
| `ITERATOR` + `GEO` | Synthetic points along boundary or network |
| `GEOJSON` | Read a GeoJSON file from classpath or disk |
| `POSTGIS` | Read geometries from a PostGIS-enabled JDBC datasource |
| `QUERY` | Custom SQL with native `ST_*` on the database |

Set `executionPolicy.mode: CHUNKED` for large **POSTGIS** / **QUERY** JDBC exports (same chunked pipeline as other JDBC sources).

## Built-in SQL functions (in-memory WGS84)

Registered in `TemplateV2SqlFunctionRegistry` — not PostGIS `ST_*`:

- Distance: `V2_GEO_DISTANCE_METERS`, `V2_GEO_WITHIN_RADIUS`
- WKT: `V2_GEO_POINT_IN_WKT`, `V2_GEO_WKT_CONTAINS`, `V2_GEO_WKT_INTERSECTS`, `V2_GEO_WKT_BUFFER`
- GeoJSON: `V2_GEO_POINT_IN_GEOJSON`, `V2_GEO_GEOJSON_CONTAINS`, `V2_GEO_GEOJSON_INTERSECTS`, `V2_GEO_GEOJSON_BUFFER`

Warehouse-scale spatial joins and survey-grade buffers remain on **PostGIS** via `POSTGIS` / `QUERY` sources.

## Verification

```powershell
.\mvnw-jdk25.ps1 -pl "data-generator-geo,data-generator-iterator/data-generator-iterator-geo,data-generator-calcite" -am test
```

Key tests: `TemplateV2RunnerGeoSourceTests`, `TemplateV2GeoSqlFunctionsTests`, `GeoJsonRowSourceTests`, `GeoJsonLoaderTests`, predicate/buffer units in `data-generator-geo`.

Migration workbench explains `GeoJsonSourceVO`, `PostGisQuerySourceVO`, and `ITERATOR`+`GEO` in `MigrationPlanExplainService`.

## Deferred (not on feature-4.0)

- Streaming / NDJSON GeoJSON reads
- Shapefile / GeoPackage import
- CRS reprojection beyond WGS84 assumptions
- Survey-grade geodesic buffer
- Native PostGIS `ST_*` inside the Calcite engine (use JDBC sources instead)
