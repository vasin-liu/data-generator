# Geospatial Phase 1 — Synthetic Generation Design

## Metadata

| Field | Value |
|-------|-------|
| Status | Approved (brainstorming 2026-05-20) |
| Date | 2026-05-20 |
| Author | Gensokyo |
| Long-term vision | **D** — read real geo sources, spatial processing, Calcite QuerySource |
| **Phase 1 scope** | **A** — synthetic geospatial data generation only |
| Integration | **C** — `iterator.type: geo` primary; minimal SpEL; QuerySource deferred |
| Phase 1 delivery | Iterator full; SpEL minimal (`pointsInBoundary` only; no trajectory in SpEL) |
| Output formats | `columns` \| `geojson` \| `wkt` (template-selectable) |
| Generation modes | `BOUNDARY_POINTS` + `LINE_SAMPLE` |
| CRS | WGS84 (EPSG:4326) assumed; no reprojection in Phase 1 |

## Related documents

- `docs/phase-5-jackson3-migration/README.md` (GeoJSON loader on Jackson 3 + JTS)
- `data-generator-faker` — existing `GeoJsonLoader`, `GeoKit`, `RandomPointGenerator`
- `docs/testing-embedded-components.md`

## Problem statement

The platform can parse GeoJSON and generate random points inside polygons (`data-generator-faker`), but there is no first-class **template iterator** for geospatial synthesis, no **line/network sampling**, and no consistent **row output contract** (`lat`/`lon` vs WKT vs GeoJSON). Operators cannot declare boundary- or road-based synthetic location data in YAML without ad-hoc scripts.

Long-term, the product needs full geospatial **source + process + sink** support (PostGIS, files, spatial SQL). Phase 1 delivers only **synthetic generation** with a shared library so later phases do not re-split dependencies.

## Goals (Phase 1)

1. Introduce **`data-generator-geo`** — shared engine: GeoJSON load, boundary points, line sampling, value formatting.
2. Introduce **`data-generator-iterator-geo`** with **`iterator.type: geo`** and modes `BOUNDARY_POINTS`, `LINE_SAMPLE`.
3. Support **`output.format`**: `columns`, `geojson`, `wkt` on every generated row.
4. Expose minimal SpEL: **`geo.pointsInBoundary(...)`** returning `List<Map<String, Object>>` aligned with `columns` format.
5. Reuse existing test fixtures (`南沙区边界.geojson`, `南沙区道路路网.geojson`) and keep full reactor `mvn test` green without Docker/PostGIS.

## Non-goals (Phase 1)

- PostGIS / Shapefile / GeoPackage readers or writers
- CRS transformation, spatial indexes, or validity repair beyond JTS defaults
- Calcite `ST_*` functions, spatial joins, or GeoJSON **QuerySource** (phase D)
- Road-graph routing (shortest path); only **linear interpolation along a single LineString component**
- GeoTools runtime dependency (stay on **JTS core + Jackson 3** GeoJSON parser)
- Full `GeoProvider` faker surface (trajectory helpers deferred to Iterator)

## Current baseline

| Component | Behavior |
|-----------|----------|
| `GeoJsonLoader` | Parses Feature / FeatureCollection / geometry types into JTS |
| `RandomPointGenerator` | Random points in polygon/multipolygon with min distance (planar envelope + haversine check) |
| `GeoKit` | `generateRandomPointsFromGeoJson` for boundary files |
| `GeoProvider` | Empty shell on `DataFaker` |
| Calcite `IteratorRowSource` | Dispatches by `IteratorVO` subtype; no geo iterator |
| Root POM | GeoTools BOM present but unused in calcite; faker uses `jts-core` directly |

## Architecture decision: shared `data-generator-geo` module (approved)

```
data-generator-geo
    ↑                    ↑
iterator-geo         data-generator-faker (SpEL bridge)
    ↑
service / existing iterator classpath (AutoService)
```

**Rationale:** Phase 2–4 (file/PostGIS sources, spatial ops, QuerySource) will share types and formatters. Keeping generation in `faker` only would force a later extract/refactor.

### Package layout (`data-generator-geo`)

| Package / type | Responsibility |
|----------------|----------------|
| `geo.io.GeoJsonLoader` | Move from faker; parse paths (`classpath:` + file) |
| `geo.io.GeoResourceResolver` | Resolve template paths consistently |
| `geo.generate.BoundaryPointGenerator` | Refactor from `RandomPointGenerator` |
| `geo.generate.LineSampleGenerator` | Arc-length sampling along longest LineString in feature |
| `geo.generate.LineComponentSelector` | **Longest LineString** rule for MultiLineString features |
| `geo.format.GeoOutputFormat` | enum: `COLUMNS`, `GEOJSON`, `WKT` |
| `geo.format.GeoValueFormatter` | `Point` → row map / geometry string |
| `geo.GeoGenerationRequest` | Immutable config for one generation run |

Faker module keeps thin `GeoKit` delegating to geo-core (deprecated adapters acceptable for one release).

## Iterator contract

### YAML shape

```yaml
iterator:
  type: geo
  mode: BOUNDARY_POINTS          # or LINE_SAMPLE
  boundaryPath: classpath:geo/南沙区边界.geojson
  networkPath: classpath:geo/南沙区道路路网.geojson
  featureIndex: 0
  randomFeature: false             # LINE_SAMPLE only: pick random feature
  count: 1000
  seed: 42
  minDistanceMeters: 50            # BOUNDARY_POINTS optional, default 0
  sample:
    strategy: BY_COUNT             # BY_COUNT | BY_SPACING_METERS
    spacingMeters: 100
  output:
    format: columns                # columns | geojson | wkt
    columnNames:                   # optional override
      lat: lat
      lon: lon
      geometry: geom_wkt
    includeProperties: false       # LINE_SAMPLE: copy feature properties into row
    crs: EPSG:4326                 # metadata only in Phase 1
```

### Mode: `BOUNDARY_POINTS`

- Requires `boundaryPath`.
- Loads geometry via `GeoJsonLoader`; supports polygon / multipolygon / geometry collection containing polygons.
- Generates `count` points with optional `minDistanceMeters` and `seed`.
- Each iterator step produces one row (standard `AbstractIterator` pattern).

### Mode: `LINE_SAMPLE`

- Requires `networkPath`.
- Loads `FeatureCollection` (or single `Feature`).
- Selects feature: `featureIndex` when `randomFeature: false`, else uniform random by `seed`.
- Extracts **longest LineString component** from feature geometry:
  - `LineString` → use as-is
  - `MultiLineString` → component with maximum length
  - Other types → fail-fast with clear message
- Sampling:
  - `BY_COUNT`: `count` points evenly by arc length along line (endpoints included when count ≥ 2)
  - `BY_SPACING_METERS`: walk from start with fixed spacing using haversine length until line end; row count = floor(length/spacing)+1 (document exact formula in implementation)
- Does **not** traverse graph connectivity across features.

### Output formats

| `output.format` | Row shape |
|---------------|-----------|
| `columns` | Default keys `lat`, `lon`; optional `alt` if Z present; optional extra `geometry` column when `columnNames.geometry` set |
| `geojson` | Single column `geometry` — GeoJSON geometry JSON for `Point` |
| `wkt` | Single column `geometry` — WKT `POINT(...)` |

All formats use WGS84 order **longitude, latitude** in GeoJSON; `columns.lat` / `columns.lon` map to **y / x** respectively (document in operator guide).

## SpEL minimal API (Phase 1)

| Function | Returns | Notes |
|----------|---------|-------|
| `geo.pointsInBoundary(path, count, minDistanceMeters, seed)` | `List<Map<String,Object>>` | Same as Iterator `columns` rows |
| `geo.randomPointInBoundary(path, seed)` | `Map<String,Object>` | Single point; convenience |

Trajectory sampling is **not** exposed via SpEL in Phase 1.

Registration: extend existing SpEL variable surface used by templates (same discovery as `DataFaker`), implementation classes live in `data-generator-faker`, call `data-generator-geo`.

## Error handling

| Condition | Behavior |
|-----------|----------|
| Missing/invalid GeoJSON | `IllegalArgumentException` at iterator construction or first `hasNext` |
| Unsupported geometry for mode | Fail-fast with mode + actual JTS type |
| Boundary point exhaustion (retries) | Propagate existing `RuntimeException` with retry context |
| Zero-length line | `IllegalArgumentException` |
| Blank paths | `IllegalArgumentException` |

## Performance & limits (Phase 1)

- Whole GeoJSON file read into memory (current behavior).
- Document soft limit: **50 MB** per file in operator guide; no hard enforcement in v1.
- No spatial index; acceptable for district-scale fixtures.

## Testing

| Layer | Scope |
|-------|--------|
| `data-generator-geo` unit | Loader, longest-line selection, boundary points, line sampling, formatters |
| `data-generator-iterator-geo` | VO parsing, both modes, three formats |
| `data-generator-faker` | SpEL functions return column maps |
| Regression | Copy/move test resources under `data-generator-geo/src/test/resources` |
| CI | No Docker; no new skipped tests |

## Future phases (out of scope)

| Phase | Letter | Content |
|-------|--------|---------|
| 2 | B | GeoJSON/Shapefile `RowSource`, PostGIS geometry columns on write |
| 3 | C | Spatial transforms (buffer, intersect) in pipeline or Calcite UDFs |
| 4 | D | GeoJSON registered as Template V2 QuerySource |

## Success criteria (Phase 1)

- [ ] Operator can run a template with `iterator.type: geo` + `BOUNDARY_POINTS` and `LINE_SAMPLE` against bundled 南沙 fixtures.
- [ ] Same template can switch `output.format` among `columns`, `geojson`, `wkt` without code changes.
- [ ] SpEL `geo.pointsInBoundary` usable from an existing SpEL reader column.
- [ ] `.\mvnw-jdk25.ps1 test` — BUILD SUCCESS, 0 failures.
- [ ] Operator doc: `docs/geospatial-phase1-usage.md` (or section in README) with YAML examples.

## Open questions (resolved)

| Question | Decision |
|----------|----------|
| Module strategy | **data-generator-geo** core (approved) |
| MultiLineString handling | **Longest component** for `LINE_SAMPLE` |
| SpEL return type | **`List<Map<String,Object>>`** for `pointsInBoundary` |
| Trajectory in SpEL | **No** (Iterator only) |
