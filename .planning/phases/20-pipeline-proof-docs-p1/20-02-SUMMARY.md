---
phase: 20-pipeline-proof-docs-p1
plan: 02
subsystem: docs
tags: [geo_synthetic, template-v2, yaml, geospatial, maintainer-docs]

requires:
  - phase: 19-v2-geo-synthetic-source
    provides: GeoSyntheticSourceVO, Factory, RowSource, four generation modes
provides:
  - Maintainer landing page with geo_synthetic vs geojson vs ITERATOR+GEO split
  - Dedicated geo-synthetic-v2-source.md with YAML, output formats, SQL companion
affects: [20-03, test-feature-matrix, GEO-04]

tech-stack:
  added: []
  patterns: ["Overview + dedicated reference page when YAML exceeds ~40 lines"]

key-files:
  created:
    - docs/geo-synthetic-v2-source.md
  modified:
    - docs/geospatial-overview.md

key-decisions:
  - "Dedicated geo-synthetic-v2-source.md for YAML/modes/output detail; overview stays landing page per D-06"
  - "Classpath fixtures reference 南沙区 test resources aligned with calcite ITs"
  - "SQL companion documents existing V2_GEO_* only; no new ST_* per D-07"

patterns-established:
  - "Three-way geo entry path table: geo_synthetic (preferred), GEOJSON (read), ITERATOR+GEO (legacy)"

requirements-completed: [GEO-04]

coverage:
  - id: D1
    description: "geospatial-overview.md lists geo_synthetic and distinguishes from GEOJSON and ITERATOR+GEO"
    requirement: GEO-04
    verification:
      - kind: other
        ref: "powershell Select-String geo_synthetic count -ge 2 in docs/geospatial-overview.md"
        status: pass
    human_judgment: false
  - id: D2
    description: "Minimal V2 YAML example with transform and sink passthrough"
    requirement: GEO-04
    verification:
      - kind: other
        ref: "docs/geo-synthetic-v2-source.md contains type: geo_synthetic YAML blocks"
        status: pass
    human_judgment: false
  - id: D3
    description: "Output formats columns/wkt/geojson and SQL companion for existing V2_GEO_*"
    requirement: GEO-04
    verification:
      - kind: other
        ref: "docs/geo-synthetic-v2-source.md Output formats and SQL companion sections"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-07-30
status: complete
---

# Phase 20 Plan 02: Geospatial Maintainer Docs Summary

**GEO-04 maintainer docs: geo_synthetic vs geojson split, classpath YAML reference, output formats, and V2_GEO_* SQL companion (docs only)**

## Performance

- **Duration:** 15 min
- **Started:** 2026-07-30T11:39:29Z
- **Completed:** 2026-07-30T11:54:00Z
- **Tasks:** 3
- **Files modified:** 2

## Accomplishments

- Extended `docs/geospatial-overview.md` with v2.2 phase row, Phase 19 modules, three-way source split, and verification test references (D-05)
- Created `docs/geo-synthetic-v2-source.md` with full YAML example, four mode snippets, validation table, and passthrough pipeline shape (D-06)
- Documented `columns` / `wkt` / `geojson` output formats and SQL companion using existing `V2_GEO_*` only — no new `ST_*` (D-07)

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend geospatial-overview.md source types and modules** - `3c0f57f` (docs)
2. **Task 2: Add minimal YAML example (dedicated page)** - `e4ae698` (docs)
3. **Task 3: Document output formats and SQL companion** - `cc38a47` (docs)

**Plan metadata:** pending final gsd-tools commit

## Files Created/Modified

- `docs/geospatial-overview.md` — Primary maintainer landing page with geo_synthetic entry, module list, source split, links
- `docs/geo-synthetic-v2-source.md` — Dedicated reference: YAML, modes, output formats, SQL companion

## Decisions Made

- Used dedicated `geo-synthetic-v2-source.md` because combined YAML + modes + output formats exceeded ~40-line overview budget (D-06 discretion)
- Referenced `classpath:geo/南沙区边界.geojson` and `classpath:geo/南沙区道路路网.geojson` to match calcite test fixtures
- Documented V2 preference over ITERATOR+GEO without V1 hard removal (docs-only per phase boundary)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- GEO-04 complete; Plan 20-03 can sync test-matrix.yaml and test-feature-matrix.md (TEST-10) without doc changes
- Plan 20-01 pipeline IT can reference docs paths for maintainer onboarding

## Self-Check: PASSED

- FOUND: docs/geospatial-overview.md
- FOUND: docs/geo-synthetic-v2-source.md
- FOUND: 3c0f57f
- FOUND: e4ae698
- FOUND: cc38a47

---
*Phase: 20-pipeline-proof-docs-p1*
*Completed: 2026-07-30*
