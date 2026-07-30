# Template V2 `geo_synthetic` source

Short maintainer reference for the v2.2 synthetic geo source.  
**Overview landing page:** [`geospatial-overview.md`](geospatial-overview.md)  
**Design spec:** [`superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md`](superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md)

Use `type: geo_synthetic` when operators need generated point rows (boundary, line sample, bbox, circle) in a Template V2 pipeline **without** the V1 `ITERATOR` + `GEO` tree. Use `type: geojson` when reading an existing Feature / FeatureCollection file.

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

**BOUNDARY_POINTS** — points along a polygon boundary:

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: BOUNDARY_POINTS
    count: 10
    seed: 42
    boundaryPath: classpath:geo/南沙区边界.geojson
```

**LINE_SAMPLE** — points sampled along a line network:

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
| `BOUNDARY_POINTS` | `boundaryPath`, `count > 0` | Ignores bbox, circle, network |
| `LINE_SAMPLE` | `networkPath`, sample strategy | Ignores bbox, circle, boundary |
| `BBOX` | valid four-tuple `bbox`, `count > 0` | `minLon < maxLon`, `minLat < maxLat`; no file |
| `CIRCLE` | `center`, `radiusMeters > 0`, `count > 0` | No file; points inside Haversine radius |

Invalid config throws `IllegalArgumentException` with source name and field detail (console-visible).  
`count` hard cap: `<= 1_000_000`.

Paths resolve via `GeoResourceResolver` (`classpath:` or filesystem). Asset upload is **not** in v2.2 scope.

## Pipeline shape

Phase 20 pipeline proof uses passthrough SQL and console sink (see `TemplateV2RunnerGeoSyntheticSourceTests`):

```
geo_synthetic source → SQL transform (select lon, lat from <source>) → sink
```

Adjust column names in SQL when `output.columnNames` overrides defaults.

## Related docs

- V1 iterator / SpEL guide: [`geospatial-phase1-usage.md`](geospatial-phase1-usage.md) — legacy `ITERATOR` + `GEO` only; prefer `geo_synthetic` for new Template V2 templates.
- Read-only file source: `GEOJSON` row in [`geospatial-overview.md`](geospatial-overview.md#template-v2-source-types).
