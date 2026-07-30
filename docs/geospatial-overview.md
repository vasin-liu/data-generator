# Geospatial capability overview

Geospatial support on `feature-4.0` spans synthetic generation (Phase 1), Template V2 sources (Phase 2B), Calcite SQL over geo tables (Phase 2D), in-memory geo SQL functions (Phase 2C), and the v2.2 **`geo_synthetic`** source (Phase 19).

**Operator guide:** [`geospatial-phase1-usage.md`](geospatial-phase1-usage.md) (YAML, SpEL, SQL examples for V1 iterator paths).

## Phase status

| Phase | Scope | Status | Design |
|-------|--------|--------|--------|
| **1** | `iterator.type: GEO`, SpEL `#geo.*`, `data-generator-geo` | Complete | [`specs/2026-05-20-geospatial-phase1-design.md`](superpowers/specs/2026-05-20-geospatial-phase1-design.md) |
| **2B** | `GEOJSON` file source, `POSTGIS` table source, CHUNKED PostGIS | Complete | [`specs/2026-05-21-geospatial-phase2b-design.md`](superpowers/specs/2026-05-21-geospatial-phase2b-design.md) |
| **2D** | SQL transforms over geo `RowSource` types | Complete | [`specs/2026-05-21-geospatial-phase2d-design.md`](superpowers/specs/2026-05-21-geospatial-phase2d-design.md) |
| **2C** | `V2_GEO_*` Calcite SQL functions (distance, WKT/GeoJSON predicates, buffer) | Complete | [`specs/2026-05-21-geospatial-phase2c-design.md`](superpowers/specs/2026-05-21-geospatial-phase2c-design.md) |
| **v2.2** | `geo_synthetic` Template V2 source (BOUNDARY_POINTS, LINE_SAMPLE, BBOX, CIRCLE) | Complete | [`specs/2026-07-30-geo-synthetic-v2-source-design.md`](superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md) |

## Modules

| Module | Role |
|--------|------|
| `data-generator-geo` | GeoJSON I/O, JTS predicates, haversine, synthetic generator, row formatting |
| `data-generator-iterator-geo` | V1/V2 `GEO` iterator |
| `data-generator-calcite` | `GeoJsonRowSource`, `GeoSyntheticSourceFactory`, `GeoSyntheticRowSource`, `PostGisQuerySourceSupport`, `TemplateV2GeoSqlFunctions` |
| `data-generator-core` | `GeoJsonSourceVO`, `GeoSyntheticSourceVO`, `PostGisQuerySourceVO`, iterator `GEO` VO |

## Template V2 source types

| `type` | Use when |
|--------|----------|
| `geo_synthetic` / `GEO_SYNTHETIC` | Synthesize boundary, line, bbox, or circle points without the V1 geo iterator |
| `GEOJSON` | Read a GeoJSON Feature / FeatureCollection file from classpath or disk |
| `ITERATOR` + `GEO` | Legacy V1 path — synthetic points along boundary or network via iterator tree |
| `POSTGIS` | Read geometries from a PostGIS-enabled JDBC datasource |
| `QUERY` | Custom SQL with native `ST_*` on the database |

### Choosing a geo entry path

| Path | Role | Prefer for new work? |
|------|------|----------------------|
| **`geo_synthetic`** | Generate point rows from boundary/line assets, bbox, or circle config | **Yes** — first-class Template V2 source (v2.2) |
| **`GEOJSON`** | Read existing geometries from a file into rows | When input is already GeoJSON on disk/classpath |
| **`ITERATOR` + `GEO`** | V1 iterator bridge to the same generator | Legacy templates only; no hard removal this milestone |

**Reference:** [`geo-synthetic-v2-source.md`](geo-synthetic-v2-source.md) — minimal YAML, mode validation, output formats, SQL companion.  
**Design spec:** [`specs/2026-07-30-geo-synthetic-v2-source-design.md`](superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md).

Set `executionPolicy.mode: CHUNKED` for large **POSTGIS** / **QUERY** JDBC exports (same chunked pipeline as other JDBC sources).

## Built-in SQL functions (in-memory WGS84)

Registered in `TemplateV2SqlFunctionRegistry` — not PostGIS `ST_*`:

- Distance: `V2_GEO_DISTANCE_METERS`, `V2_GEO_WITHIN_RADIUS`
- WKT: `V2_GEO_POINT_IN_WKT`, `V2_GEO_WKT_CONTAINS`, `V2_GEO_WKT_INTERSECTS`, `V2_GEO_WKT_BUFFER`
- GeoJSON: `V2_GEO_POINT_IN_GEOJSON`, `V2_GEO_GEOJSON_CONTAINS`, `V2_GEO_GEOJSON_INTERSECTS`, `V2_GEO_GEOJSON_BUFFER`

Warehouse-scale spatial joins and survey-grade buffers remain on **PostGIS** via `POSTGIS` / `QUERY` sources.  
Synthetic `geo_synthetic` output can be filtered or joined in transform SQL using these existing `V2_GEO_*` functions — see [`geo-synthetic-v2-source.md`](geo-synthetic-v2-source.md#sql-companion).

## Verification

```powershell
.\mvnw-jdk25.ps1 -pl "data-generator-geo,data-generator-iterator/data-generator-iterator-geo,data-generator-calcite" -am test
```

Key tests: `TemplateV2RunnerGeoSourceTests`, `TemplateV2RunnerGeoSyntheticSourceTests`, `TemplateV2GeoSqlFunctionsTests`, `GeoJsonRowSourceTests`, `GeoSyntheticRowSourceTests`, `GeoJsonLoaderTests`, predicate/buffer units in `data-generator-geo`.

Migration workbench explains `GeoJsonSourceVO`, `PostGisQuerySourceVO`, `GeoSyntheticSourceVO`, and `ITERATOR`+`GEO` in `MigrationPlanExplainService`.

## Deferred (not on feature-4.0)

- Streaming / NDJSON GeoJSON reads
- Shapefile / GeoPackage import
- CRS reprojection beyond WGS84 assumptions
- Survey-grade geodesic buffer
- Native PostGIS `ST_*` inside the Calcite engine (use JDBC sources instead)
- GeoJSON asset upload, polygon synthesis, console map UI (v2.2 follow-ups)
