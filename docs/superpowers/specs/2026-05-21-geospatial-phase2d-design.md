# Geospatial Phase 2D — Calcite SQL over Geo Sources

## Metadata

| Field | Value |
|-------|-------|
| Status | Implemented (runner validation) |
| Date | 2026-05-21 |
| Depends on | Phase 1 (GEO iterator), Phase 2B (GEOJSON / POSTGIS file & table sources) |
| **Phase 2D scope** | **D (minimal)** — Geo `RowSource` types materialize into `CalciteExecutionContext` and are queryable via Template V2 SQL transforms |

## Problem statement

Phase 2B added file and PostGIS **readers**, but operators need confidence that geo tables appear in the Calcite schema like CSV/JSON sources and that `SELECT lat, lon FROM …` works in a full `TemplateV2Runner` pipeline.

## Goals

1. Document that **`GEOJSON`**, **`POSTGIS`**, and **`ITERATOR`+`GEO`** sources register through `CalciteExecutionContext.addSource(RowSource)` (same path as other V2 sources).
2. Add **`TemplateV2RunnerGeoSourceTests`** — end-to-end SQL filter/projection over GEOJSON and GEO iterator fixtures.
3. No new source subtype required for Phase D (distinct from Phase 2B `GEOJSON` **file** source type).

## Non-goals

- Calcite spatial UDFs (`ST_*` in SQL engine) — Phase C
- Streaming geo sources
- GeoJSON as a separate `QUERY` JDBC subtype (use `POSTGIS` or `QUERY` with `ST_AsText` today)

## Architecture

```
TemplateV2Runner (IN_MEMORY)
  → registry.createSource(name, GeoJsonSourceVO | …)
  → RowSource.rows() + schema()
  → CalciteExecutionContext.addSource
  → SqlTransformFactory / Calcite SQL
```

## Success criteria

- [x] `TemplateV2RunnerGeoSourceTests` passes in `data-generator-calcite` module tests.
- [x] Operator doc references Phase D in `docs/geospatial-phase1-usage.md`.

## Revision history

| Date | Change |
|------|--------|
| 2026-05-21 | Phase 2D runner tests + design note |
