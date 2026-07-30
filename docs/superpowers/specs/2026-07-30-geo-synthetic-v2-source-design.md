# Design: Template V2 `geo_synthetic` Source

**Date:** 2026-07-30  
**Status:** Approved for planning (brainstorming)  
**Milestone intent:** Next product milestone after v2.1 — geographic generation as a first-class Template V2 source  
**Out of this milestone:** Operator-maintained common/reference data dictionaries (separate backlog item)

## Problem

Geographic synthetic generation exists in `data-generator-geo` (`GeoSyntheticGenerator`) and is exposed primarily via the **V1** geo iterator and a **read-only** V2 `geojson` source. Operators who define Template V2 pipelines cannot generate boundary/line/bbox/circle points without leaning on V1 paths. The feature matrix row `geo-synthetic` remains pending.

## Goals

1. Add a first-class Template V2 source `type: geo_synthetic` that produces point rows without using the V1 geo iterator.
2. Support four modes: `BOUNDARY_POINTS`, `LINE_SAMPLE`, `BBOX`, `CIRCLE`.
3. Keep GeoJSON assets **path-referenced** (`classpath:` / filesystem via `GeoResourceResolver`); defer console upload.
4. Leave `type: geojson` as a read-only Feature/Feature source (no mode overloading).
5. Minimal SQL companion only: document existing geo predicates/output formats; no large new `ST_*` surface in this milestone.

## Non-goals

- Common-data / code-table operator CRUD (deferred milestone)
- Hosted GeoJSON asset upload / asset IDs
- Polygon / MultiPolygon synthetic generation as primary output
- Console visual map config UI (optional later)
- Promoting geo proofs into the P0 merge gate (recommend P1 matrix linkage)

## Decisions (locked)

| Decision | Choice |
|----------|--------|
| Scope vs common data | Geo only this milestone |
| V2 exposure | **Source first**; SQL minimal companion |
| Approach | **A** — new `geo_synthetic` source (not extend `geojson`, not V1 bridge) |
| Modes | Existing boundary + line sample **plus** bbox and circle |
| Assets | Path reference only; upload documented as follow-up |

## Architecture

```
Template V2 YAML
  sources.<name>.type: geo_synthetic
        │
        ▼
GeoSyntheticSourceVO  (@AutoService SourceVO, @JsonSubType GEO_SYNTHETIC)
        │
        ▼
GeoSyntheticSourceFactory → GeoSyntheticRowSource (RowSource)
        │
        ▼
GeoSyntheticGenerator.generateRows(GeoGenerationRequest)
  modes: BOUNDARY_POINTS | LINE_SAMPLE | BBOX | CIRCLE
        │
        ▼
Formatted columns (GeoOutputFormatKind: columns | wkt | geojson)
        │
        ▼
Existing transform SQL / sinks
```

### Module placement

| Concern | Module |
|---------|--------|
| Generation algorithms (`BBOX`/`CIRCLE` + existing) | `data-generator-geo` |
| `GeoSyntheticSourceVO` (+ nested output/sample/bbox/circle fields) | `data-generator-common/data-generator-core` |
| Factory + `GeoSyntheticRowSource` | `data-generator-calcite` |
| Bean registration | `data-generator-service` `CoreConfig` (same pattern as `GeoJsonSourceFactory`) |

### Boundaries

| Keep unchanged | This work | Explicitly deferred |
|----------------|-----------|---------------------|
| `GeoJsonSourceVO` / `GeoJsonRowSource` read path | New synthetic source type | Asset upload API |
| V1 geo iterator (may annotate deprecated in docs) | Extend `GeoGenerationMode` + request validation | Common-data maintenance |
| Existing geo SQL predicates | Path resolution via `GeoResourceResolver` | Full `ST_*` library expansion |

## Configuration model

Example:

```yaml
sources:
  pts:
    type: geo_synthetic
    mode: BOUNDARY_POINTS   # BOUNDARY_POINTS | LINE_SAMPLE | BBOX | CIRCLE
    count: 100
    seed: 42
    minDistanceMeters: 0
    boundaryPath: classpath:geo/district.geojson
    networkPath: classpath:geo/roads.geojson
    featureIndex: 0
    randomFeature: false
    sample:
      strategy: EVEN
      spacingMeters: 50
    bbox: [113.2, 23.0, 113.5, 23.2]   # [minLon, minLat, maxLon, maxLat]
    center: [113.3, 23.1]
    radiusMeters: 500
    output:
      format: columns
      columnNames:
        lon: lon
        lat: lat
      includeProperties: false
```

### Validation

| Mode | Required | Constraints |
|------|----------|-------------|
| `BOUNDARY_POINTS` | `boundaryPath`, `count > 0` | Ignore bbox/circle/network |
| `LINE_SAMPLE` | `networkPath`, sample strategy | Ignore bbox/circle/boundary |
| `BBOX` | valid four-tuple bbox, `count > 0` | `minLon < maxLon`, `minLat < maxLat`; no file |
| `CIRCLE` | `center`, `radiusMeters > 0`, `count > 0` | No file; points inside Haversine radius |

Failures throw `IllegalArgumentException` with source name and missing/invalid field (console-visible).  
`count` hard cap remains `<= 1_000_000` (existing generator limit).

### Relationship to `geojson`

- `type: geojson` — read Feature / FeatureCollection files into rows  
- `type: geo_synthetic` — synthesize points from boundary/line/bbox/circle  

Docs must state this split explicitly.

## Data flow and errors

1. Map VO → `GeoGenerationRequest` (shared mapper preferred with V1 iterator mapping where possible).  
2. `GeoSyntheticGenerator.generateRows` returns `List<Map<String,Object>>`.  
3. `GeoSyntheticRowSource` materializes finite `List<Row>` + inferred or declared `RowSchema` (same pattern as `GeoJsonRowSource`).  
4. Downstream pipeline unchanged.

### BBOX / CIRCLE semantics

- **BBOX:** Uniform random lon/lat in rectangle; optional `minDistanceMeters` with same retry semantics as boundary sampling.  
- **CIRCLE:** Area-uniform polar sampling around center (`r = R * sqrt(u)`, `θ = 2πv` with seeded RNG), then convert offset via local meters→degrees using `GeoHaversine` / Earth radius; accept only points with Haversine distance ≤ `radiusMeters`.  
- **Seed:** When set, same config → same point set.

### Error table

| Case | Behavior |
|------|----------|
| Missing mode fields / illegal bbox / radius ≤ 0 | `IllegalArgumentException` |
| Missing/unreadable GeoJSON path | Fail with path in message (align with geojson source) |
| Boundary sampling cannot satisfy count/spacing | Same as existing `BoundaryPointGenerator` (no silent shortfall) |
| Column collisions (`prop.*` vs lon/lat) | Existing `ensureNoColumnCollisions` |

### SQL companion (minimal)

No new large `ST_*` surface. Document:

- Output format options (`columns` / `wkt` / `geojson`)  
- How to join/filter with existing geo predicates if already registered  
- Example V2 template: `geo_synthetic` → passthrough SQL → sink  

## Testing and acceptance

### Test layers

| Layer | What | Where |
|-------|------|-------|
| Unit | BBOX/CIRCLE in-domain, seed reproducibility, invalid config | `data-generator-geo` |
| Unit | VO → request mapping for four modes | core/calcite |
| Integration | Factory → RowSource schema + row count | `data-generator-calcite` |
| Pipeline | Minimal Template V2 run via `TemplateV2Runner` | calcite or service IT |
| Regression | Existing geo generator + geojson source tests stay green | existing |

### Must-be-true acceptance

1. A V2 template with `type: geo_synthetic` completes successfully without V1 geo iterator.  
2. Automated evidence exists for all four modes.  
3. `geojson` read-only behavior unchanged.  
4. Paths resolve via `GeoResourceResolver` only (no upload API).  
5. Invalid config messages include mode/field names.  
6. Matrix row `geo-synthetic` linked to real tests at **P1** (P0 gate unchanged unless explicitly reopened later).

### Not accepted this milestone

Console map UI, hosted upload, polygon synthesis, common-data CRUD.

## Follow-ups (backlog)

- Operator-maintained common/reference data libraries  
- GeoJSON asset upload + asset-id references  
- Richer geometry synthesis (polygons)  
- Optional console geo source form / preview  
- Optional P0 promotion after P1 stabilizes  

## Prior art in repo

- `docs/superpowers/specs/2026-05-20-geospatial-phase1-design.md` and phase 2b–2d — established generator, iterator, and geojson source foundations this design extends onto the V2 **synthetic** source path.

## Open implementation notes (non-blocking)

- Prefer a shared mapper between `GeoIteratorVO` and `GeoSyntheticSourceVO` to avoid dual validation drift.  
- V1 geo iterator deprecation is documentation-only in this milestone (no hard removal).
