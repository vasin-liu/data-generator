# Requirements: data-generator v2.2

**Defined:** 2026-07-30  
**Core Value:** Operators can define, extend, and trust data-generation pipelines: register custom logic (UDFs), apply rich transforms, and verify behavior through an automated test harness before shipping.  
**Spec:** `docs/superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md`

## v2.2 Requirements

First-class Template V2 geographic point synthesis. No V1 geo iterator required for the happy path. P0 merge gate unchanged.

### Geo Synthetic Source (GEO)

- [x] **GEO-01**: Operator can define a Template V2 source with `type: geo_synthetic` that materializes point rows through `TemplateV2Runner` (Factory → RowSource) without using the V1 geo iterator
- [x] **GEO-02**: The same source supports four modes with automated evidence: `BOUNDARY_POINTS`, `LINE_SAMPLE`, `BBOX`, and `CIRCLE` (seed reproducibility for BBOX/CIRCLE; path GeoJSON for boundary/line)
- [x] **GEO-03**: Boundary/network GeoJSON resolve only via path/`classpath:` (`GeoResourceResolver`); `type: geojson` remains a read-only Feature/Feature source (behavior unchanged)
- [ ] **GEO-04**: Maintainers have docs that distinguish `geo_synthetic` vs `geojson`, document output formats, and include a minimal V2 template example (SQL companion = docs only; no large new `ST_*` surface)

### Harness (TEST)

- [ ] **TEST-10**: Feature matrix links `geo-synthetic` to real tests at **P1**; `verify-harness.ps1` P0 set and merge-gate semantics remain unchanged (still 15 P0 rows)

## Future Requirements

Deferred beyond v2.2.

### Common Data & Assets

- **DATA-01**: Operator-maintained common/reference data (code tables / dictionaries) with console CRUD
- **GEO-05**: Hosted GeoJSON asset upload + asset-id references from `geo_synthetic`
- **GEO-06**: Polygon / MultiPolygon synthetic generation as primary output
- **GEO-07**: Console visual map config / preview for geo sources

### From prior backlog (still deferred)

- **ORCH-01** / **ORCH-02**: Template orchestration / flow-control transforms
- **RES-02**: Full JDBC resolver consolidation
- **SEC-02**: Default-on console RBAC
- **DIST-02**: Full staging distributed AC matrix
- **DIAL-03**: Dameng live as P0 gate
- **RW-07**: Net-new connectors (Redis, S3, HTTP)
- **TEST-V2**: Exhaustive console matrix coverage

## Out of Scope

| Feature | Reason |
|---------|--------|
| Common-data CRUD | Separate product lane; deferred past v2.2 by design |
| GeoJSON asset upload | Path-only milestone; upload is follow-up |
| Polygon synthesis | First version is point modes only |
| Console map UI | Not required for V2 source proof |
| Large new Calcite `ST_*` library | Minimal SQL companion = documentation |
| P0 promotion of geo-synthetic | Keep merge gate stable; P1 only |
| V1 geo iterator hard removal | Docs may note preference for V2; no hard cut |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| GEO-01 | 19 | Complete |
| GEO-02 | 18–20 | Complete |
| GEO-03 | 19 | Complete |
| GEO-04 | 20 | Pending |
| TEST-10 | 20 | Pending |

**Coverage:**

- v2.2 requirements: 5 total
- Mapped to phases: 5 (100%)
- Unmapped: 0

---
*Requirements defined: 2026-07-30*  
*Source: brainstorming + approved geo_synthetic design spec*
