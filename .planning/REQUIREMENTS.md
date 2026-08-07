# Requirements: data-generator v2.3

**Defined:** 2026-07-31  
**Core Value:** Operators can define, extend, and trust data-generation pipelines: register custom logic (UDFs), apply rich transforms, and verify behavior through an automated test harness before shipping.  
**Research:** `.planning/research/SUMMARY.md`

## v2.3 Requirements

Hosted GeoJSON assets in the metadata DB, asset-id template binding, and console map preview for assets and `geo_synthetic` — equal depth. Path/`classpath:` fallback preserved. P0 merge gate frozen at 15.

### Geo Assets (GEO)

- [x] **GEO-05**: Operator can upload a GeoJSON Feature or FeatureCollection via console API; platform validates geometry, enforces max bytes and max feature count, persists body in the metadata DB, and returns a stable asset-id
- [x] **GEO-08**: Operator can list assets (metadata without full body) and fetch a single asset’s GeoJSON by asset-id, including derived bbox and featureCount captured at ingest
- [x] **GEO-09**: Operator can delete an asset; if any stored template still references that asset-id / `asset:` location, API returns 409 with usage hints instead of orphaning runs
- [x] **GEO-10**: Operator can bind `geo_synthetic` (boundary/network) and `geojson` sources to an asset-id while path and `classpath:` locations remain valid (GEO-03 compatibility)
- [x] **GEO-11**: Template V2 runs resolve `asset:{id}` through a shared `GeoAssetResolver` on the execute path (same metadata DB for coordinator and worker) — not console-only
- [x] **GEO-07**: Operator can open a console geo-assets view and see the selected uploaded asset rendered on a map
- [x] **GEO-12**: Operator can preview a `geo_synthetic` source config on the map (boundary/network overlay and/or BBOX/CIRCLE guides; seed documented so preview is not mistaken for full run output)
- [x] **GEO-13**: Console template editor supports `geo_synthetic` as an editable source kind with an asset picker (equal-depth GEO-07; closes v2.2 YAML-only gap)

### Governance & Docs (GOV / DOC)

- [x] **GOV-01**: Upload and delete emit audit events; optional console RBAC continues to use the existing enable flag (default off)
- [ ] **DOC-01**: Maintainers/operators have docs for asset-id YAML examples, path vs asset-id, map preview usage, and size limits

### Harness (TEST)

- [ ] **TEST-11**: Feature matrix may link a **P1** `geo-assets` (or equivalent) row to real tests when stable; `verify-harness.ps1` P0 set remains **15** rows (no P0 promotion)

## Future Requirements

Deferred beyond v2.3.

- **GEO-06**: Polygon / MultiPolygon synthetic generation as primary output
- **DATA-01**: Operator-maintained common/reference data (code tables) with console CRUD
- **ORCH-01** / **ORCH-02**: Template orchestration / flow-control transforms
- **RES-02**: Full JDBC resolver consolidation
- **SEC-02**: Default-on console RBAC
- **DIST-02**: Full staging distributed AC matrix
- **DIAL-03**: Dameng live as P0 gate
- **RW-07**: Net-new connectors (Redis, S3, HTTP)
- Full GIS asset management (CRS reprojection, Shapefile/KML, versioning)

## Out of Scope

| Feature | Reason |
|---------|--------|
| GEO-06 polygon synthesis | Explicitly deferred this milestone |
| P0 promotion of geo-assets / map E2E | Keep merge gate stable |
| PostGIS / external GIS server | Metadata DB + MapLibre only |
| Replacing ephemeral `ConsoleUploadController` path paste | Path/`classpath:` remain; assets are additive |
| Collapsing `geojson` and `geo_synthetic` types | Preserve v2.2 type split |
| Mandatory RBAC for geo uploads | Follow SEC-01 opt-in; SEC-02 deferred |
| Map geometry editing | Preview/browse only |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| GEO-05 | 21 | Complete |
| GEO-08 | 21 | Complete |
| GEO-09 | 21 | Complete |
| GEO-10 | 21 | Complete |
| GEO-11 | 21 | Complete |
| GOV-01 | 21 | Complete |
| GEO-07 | 22 | Complete |
| GEO-12 | 22 | Complete |
| GEO-13 | 22 | Complete |
| DOC-01 | 23 | Pending |
| TEST-11 | 23 | Pending |

**Coverage:**

- v2.3 requirements: 11 total
- Mapped to phases: 11
- Unmapped: 0

---
*Requirements defined: 2026-07-31*  
*Source: `/gsd-new-milestone` questioning + research SUMMARY*
