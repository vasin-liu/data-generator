# Geospatial Phase 1 — Synthetic Generation Design

## Metadata

| Field | Value |
|-------|-------|
| Status | Approved — revised 2026-05-20 (post-review) |
| Date | 2026-05-20 |
| Author | Gensokyo |
| Long-term vision | **D** — read real geo sources, spatial processing, Calcite QuerySource |
| **Phase 1 scope** | **A** — synthetic geospatial data generation only |
| Template surfaces | **V1** (`iterator` on classic template) **and** **V2** (`sources[].type: ITERATOR` via `IteratorRowSource`) |
| Integration | **C** — `iterator.type: GEO` primary; minimal SpEL; QuerySource (phase D) deferred |
| Phase 1 delivery | Iterator full (V1 + V2 materialization); SpEL minimal (`pointsInBoundary` only) |
| Output formats | `columns` \| `geojson` \| `wkt` (template-selectable) |
| Generation modes | `BOUNDARY_POINTS` + `LINE_SAMPLE` |
| CRS | WGS84 (EPSG:4326) assumed; no reprojection in Phase 1 |

## Related documents

- `docs/phase-5-jackson3-migration/README.md` (GeoJSON loader on Jackson 3 + JTS)
- `data-generator-faker` — existing `GeoJsonLoader`, `GeoKit`, `RandomPointGenerator`
- `docs/testing-embedded-components.md`
- `docs/calcite-v1-parity-scorecard.md` (`IteratorSourceVO` parity)
- `AGENTS.md` (new top-level module policy)

## Problem statement

The platform can parse GeoJSON and generate random points inside polygons (`data-generator-faker`), but there is no first-class **template iterator** for geospatial synthesis, no **line/network sampling**, and no consistent **row output contract** (`lat`/`lon` vs WKT vs GeoJSON). Operators cannot declare boundary- or road-based synthetic location data in YAML without ad-hoc scripts.

`IteratorRowSource` (Template V2) today only materializes `NUMBER`, `CONSTANT`, and `DATETIME` iterators. A `GEO` iterator must be added there for V2 templates on `feature-4.0`.

Long-term, the product needs full geospatial **source + process + sink** support (PostGIS, files, spatial SQL). Phase 1 delivers only **synthetic generation** with a shared library so later phases do not re-split dependencies.

## Goals (Phase 1)

1. Introduce **`data-generator-geo`** — shared engine: GeoJSON load, boundary points, line sampling, value formatting.
2. Introduce **`data-generator-iterator-geo`** with **`iterator.type: GEO`** (`@JsonSubType("GEO")`) and modes `BOUNDARY_POINTS`, `LINE_SAMPLE`.
3. Support **`output.format`**: `columns`, `geojson`, `wkt` on every generated row (V1 `MapValue` and V2 `Row` column maps).
4. Extend **`IteratorRowSource`** to materialize `GeoIteratorVO` with an inferred column schema.
5. Expose minimal SpEL via **`GeoVariable`** (`name = "geo"`): `#{geo.pointsInBoundary(...)}` returning `List<Map<String, Object>>` in `columns` shape.
6. Reuse test fixtures (`南沙区边界.geojson`, `南沙区道路路网.geojson` on classpath) and keep full reactor `mvn test` green without Docker/PostGIS.

## Non-goals (Phase 1)

- PostGIS / Shapefile / GeoPackage readers or writers
- CRS transformation, spatial indexes, or validity repair beyond JTS defaults
- Calcite `ST_*` functions, spatial joins, or GeoJSON **QuerySource** (phase D)
- Road-graph routing (shortest path); only **linear interpolation along a single LineString component**
- GeoTools runtime dependency (stay on **JTS core + Jackson 3** GeoJSON parser)
- Full `GeoProvider` / `faker.geo()` method surface (trajectory helpers deferred to Iterator)
- Surveying-grade accuracy (see **Limitations**)
- Streaming GeoJSON or chunked file read
- `CONSTANT` iterator `repeat: -1` style infinite geo iteration in V2 (finite `count` only)

## Current baseline

| Component | Behavior |
|-----------|----------|
| `GeoJsonLoader` | Parses Feature / FeatureCollection / geometry types into JTS |
| `RandomPointGenerator` | Random points in polygon/multipolygon with min distance (planar envelope + haversine check) |
| `GeoKit` | `generateRandomPointsFromGeoJson` for boundary files |
| `GeoProvider` | Empty shell on `DataFaker` |
| Calcite `IteratorRowSource` | Only `NumberIteratorVO`, `ConstantIteratorVO`, `DateTimeIteratorVO` |
| Root POM | GeoTools BOM present; faker pins `jts-core` **1.19.0** locally |

## Architecture decision: shared `data-generator-geo` module (approved)

```
data-generator-geo
    ↑                    ↑
iterator-geo         data-generator-faker (GeoVariable + GeoKit delegate)
    ↑                    ↑
service classpath    calcite IteratorRowSource (V2 materialization)
```

**Rationale:** Phase 2–4 will share formatters and loaders. Faker-only placement would force another extract before PostGIS / QuerySource.

### Package layout (`data-generator-geo`)

Base package: **`org.gensokyo.data.geo`**

| Type | Responsibility |
|------|----------------|
| `geo.io.GeoJsonLoader` | Move from faker; parse geometries |
| `geo.io.GeoResourceResolver` | `classpath:` and filesystem paths |
| `geo.generate.BoundaryPointGenerator` | Refactor from `RandomPointGenerator` |
| `geo.generate.BoundaryGeometryNormalizer` | Polygon / MultiPolygon / GeometryCollection → single area for sampling |
| `geo.generate.LineSampleGenerator` | Arc-length sampling along selected LineString |
| `geo.generate.LineComponentSelector` | **Longest LineString** in MultiLineString |
| `geo.format.GeoOutputFormat` | `COLUMNS`, `GEOJSON`, `WKT` |
| `geo.format.GeoValueFormatter` | `Point` → `Map<String, Object>` row |
| `geo.GeoGenerationRequest` | Immutable run config (used by iterator + tests) |

Faker keeps thin `GeoKit` delegating to geo-core.

## Template integration

### V1 (classic template)

Root `iterator.type: GEO` discovered by `IteratorFactory` + `@AutoService(IteratorVO.class)`. `GeoIterator.next()` returns **`MapValue.fromMap(...)`** (same as `CsvIterator`).

### V2 (Template V2 / Calcite)

```yaml
sources:
  - name: geo_input
    type: ITERATOR
    iterator:
      type: GEO
      mode: BOUNDARY_POINTS
      boundaryPath: classpath:geo/南沙区边界.geojson
      count: 100
      output:
        format: columns
```

`IteratorRowSource` adds a `case GeoIteratorVO` branch: materialize all rows (finite), build `RowSchema` from `output.format` + `columnNames`, expose columns to SQL (`SELECT lat, lon FROM geo_input`).

## Iterator contract

### YAML shape (V1 root `iterator` or V2 nested `sources[].iterator`)

```yaml
iterator:
  type: GEO                        # MUST be uppercase GEO (JsonSubType)
  mode: BOUNDARY_POINTS            # or LINE_SAMPLE
  boundaryPath: classpath:geo/南沙区边界.geojson
  networkPath: classpath:geo/南沙区道路路网.geojson
  featureIndex: 0                  # ignored when randomFeature=true
  randomFeature: false               # LINE_SAMPLE only
  count: 1000
  seed: 42
  minDistanceMeters: 50            # BOUNDARY_POINTS; default 0
  sample:
    strategy: BY_COUNT             # LINE_SAMPLE required: BY_COUNT | BY_SPACING_METERS
    spacingMeters: 100             # required when strategy=BY_SPACING_METERS
  output:
    format: columns                # columns | geojson | wkt
    columnNames:                   # optional rename
      lat: lat
      lon: lon
      geometry: geometry
    includeProperties: false       # LINE_SAMPLE only
    crs: EPSG:4326                 # metadata only; no transform
```

### Mode: `BOUNDARY_POINTS`

- **Requires** `boundaryPath`; `networkPath` must be absent or ignored.
- Loads geometry via `GeoJsonLoader` + `BoundaryGeometryNormalizer`:
  - `Polygon` / `MultiPolygon` → use as-is (MultiPolygon kept for area-weighted split in generator)
  - `GeometryCollection` → **union of all polygonal components**; if none, fail-fast
  - Other types → fail-fast
- Generates exactly **`count`** points (unless `count` overridden by spacing rules — N/A in this mode).
- **Materialization:** all points computed in iterator constructor, held in a queue; `next()` dequeues one `MapValue` per call.

### Mode: `LINE_SAMPLE`

- **Requires** `networkPath` and `sample.strategy`; `boundaryPath` must be absent or ignored.
- Loads `FeatureCollection` or single `Feature`.
- Feature selection: `featureIndex` when `randomFeature: false`; else `Random(seed)` uniform index.
- **Longest LineString** from feature geometry (`LineString` or longest component of `MultiLineString`).
- **Does not** walk multiple features as a graph.

#### `count` vs `sample.strategy` (precedence)

| `sample.strategy` | `count` field | Result row count |
|-------------------|---------------|------------------|
| `BY_COUNT` | **Required** (> 0) | Exactly `count` points along line (endpoints included when count ≥ 2) |
| `BY_SPACING_METERS` | **Ignored** (if present, validator may warn in logs; must not affect row count) | `floor(totalLength / spacingMeters) + 1` points along line, `spacingMeters` > 0 |

Arc length for spacing uses **haversine** between vertices; interpolation uses **linear lon/lat** between vertices (see Limitations).

### Output formats

| `output.format` | V1/V2 column keys |
|-----------------|-------------------|
| `columns` | `lat` (= JTS Y), `lon` (= JTS X); optional `alt` if Z; optional extra column when `columnNames.geometry` set |
| `geojson` | Single column `geometry` (default name; overridable via `columnNames.geometry`) — GeoJSON geometry JSON for `Point` |
| `wkt` | Single column `geometry` — WKT `POINT(...)` |

GeoJSON coordinate order: **[longitude, latitude]**; `columns.lat` / `columns.lon` map to **y / x** respectively.

### `includeProperties` (LINE_SAMPLE only)

When `true`, each feature property is copied to the row as **`prop.<key>`** (stringified via `Objects.toString`; nested JSON values as string). Keys must not collide with geometry column names; collision → fail-fast at iterator build time.

## SpEL minimal API (Phase 1)

| Expression | Returns | Notes |
|------------|---------|-------|
| `#{geo.pointsInBoundary(path, count, minDistanceMeters, seed)}` | `List<Map<String,Object>>` | `columns` layout; path supports `classpath:` |
| `#{geo.randomPointInBoundary(path, seed)}` | `Map<String,Object>` | Single point |

- **Not** exposed: line sampling / trajectory (Iterator only).
- **Registration:** new Spring bean `GeoVariable implements Variable` with `name() → "geo"` and `value() → GeoSpelFunctions` (or equivalent namespace object). Do **not** rely on `faker` root for `geo.*`.
- Implementation classes in **`data-generator-faker`**; logic in **`data-generator-geo`**.

## Configuration validation (fail-fast at iterator construction)

| Rule | Action |
|------|--------|
| `type` not `GEO` | Jackson subtype mismatch (author must use `GEO`) |
| Unknown `mode` | `IllegalArgumentException` |
| `BOUNDARY_POINTS` without `boundaryPath` | `IllegalArgumentException` |
| `LINE_SAMPLE` without `networkPath` or `sample.strategy` | `IllegalArgumentException` |
| `BY_SPACING_METERS` with `spacingMeters` ≤ 0 | `IllegalArgumentException` |
| `BY_COUNT` with `count` ≤ 0 | `IllegalArgumentException` |
| Both paths set for active mode’s unused path | Allowed but **unused path ignored**; doc warns |
| `randomFeature: true` | `featureIndex` ignored |
| Blank paths | `IllegalArgumentException` |
| `count` > 1_000_000 | `IllegalArgumentException` (hard cap Phase 1) |

## Error handling

| Condition | Behavior |
|-----------|----------|
| Missing/invalid GeoJSON | `IllegalArgumentException` at iterator construction |
| Unsupported geometry for mode | Fail-fast with mode + JTS type |
| Boundary point exhaustion (retries) | `RuntimeException` with retry context (existing behavior) |
| Zero-length line | `IllegalArgumentException` |
| Property key collision | `IllegalArgumentException` at construction |

## Limitations (Phase 1 — document for operators)

- **Not surveying-grade:** JTS treats lon/lat as a Cartesian plane for `contains` and envelope sampling; **min distance** uses haversine. Suitable for synthetic test data and district-scale fixtures, not legal/survey boundaries.
- **Line sampling:** arc length via haversine along segments; intermediate points by linear interpolation in lon/lat — acceptable for short road segments, inaccurate for very long geodesics.
- **Memory:** full GeoJSON file + full point list in heap (see Performance).

## Performance & limits (Phase 1)

- Whole GeoJSON file read into memory.
- Soft limit: **50 MB** per GeoJSON file (operator doc); no hard parser limit in v1.
- Hard limit: **`count` ≤ 1_000_000** per geo iterator instance.
- No spatial index.

## Testing

| Layer | Scope |
|-------|--------|
| `data-generator-geo` unit | Loader, normalizer, longest-line, boundary points, line sampling, formatters |
| `data-generator-iterator-geo` | `GeoIteratorVO` YAML/JSON round-trip (`type: GEO`), both modes, three formats, `MapValue` rows |
| `data-generator-calcite` | `IteratorRowSource` + `GeoIteratorVO` materialization + schema |
| `data-generator-faker` | `GeoVariable` + SpEL expressions |
| Resources | `classpath:geo/*.geojson` (copy from faker tests); **no absolute paths** in tests |
| CI | No Docker; no new skipped tests |

## Future phases (out of scope)

| Phase | Letter | Content |
|-------|--------|---------|
| 2 | B | GeoJSON/Shapefile `RowSource`, PostGIS geometry columns on write |
| 3 | C | Spatial transforms (buffer, intersect) in pipeline or Calcite UDFs |
| 4 | D | GeoJSON registered as Template V2 QuerySource |

## Success criteria (Phase 1)

- [ ] **V1:** Template with root `iterator.type: GEO` runs (`BOUNDARY_POINTS` + `LINE_SAMPLE`) against classpath 南沙 fixtures.
- [ ] **V2:** Template with `sources[].type: ITERATOR` + `type: GEO` materializes in `IteratorRowSource` and runs through `TemplateV2Runner` / SQL `SELECT` over geo columns.
- [ ] `output.format` switch among `columns`, `geojson`, `wkt` without code changes.
- [ ] `#{geo.pointsInBoundary(...)}` works in SpEL reader column tests.
- [ ] `.\mvnw-jdk25.ps1 test` — BUILD SUCCESS, 0 failures.
- [ ] `docs/geospatial-phase1-usage.md` with V1 + V2 YAML and SpEL examples.

## Open questions (resolved)

| Question | Decision |
|----------|----------|
| Module strategy | **data-generator-geo** core |
| MultiLineString handling | **Longest component** for `LINE_SAMPLE` |
| SpEL return type | **`List<Map<String,Object>>`** for `pointsInBoundary` |
| Trajectory in SpEL | **No** (Iterator only) |
| V2 support in Phase 1 | **Yes** — extend `IteratorRowSource` |
| JsonSubType / YAML `type` | **`GEO`** (uppercase) |
| SpEL registration | **`GeoVariable`** bean, not `faker` |
| Iterator row type | **`MapValue.fromMap`** |
| GeometryCollection (boundary) | **Union of polygons** |
| `BY_SPACING` vs `count` | **Spacing ignores `count`**; `BY_COUNT` requires `count` |

## Revision history

| Date | Change |
|------|--------|
| 2026-05-20 | Initial approved design (brainstorming) |
| 2026-05-20 | Post-review: V2 `IteratorRowSource`, `GEO` subtype, `GeoVariable`, validation, limitations, spacing precedence |
