---
phase: 21-geo-asset-registry-runtime-resolution
plan: 03
subsystem: api
tags: [geojson, delete-guard, audit, template-v2, pipeline-it, jpa]

requires:
  - phase: 21-02
    provides: asset:{uuid} execute-path resolution spine and VO asset-id fields
provides:
  - GeoAssetReferenceScanner template reference detection (D-08)
  - Hard delete with 409 GeoAssetInUseException + usage hints
  - GEO_ASSET_DELETE audit via AuditService (GOV-01)
  - TemplateV2RunnerGeoAssetSourceTests pipeline proof (GEO-10/GEO-11)
affects:
  - 22-console-map-ui
  - 23-docs-harness-closeout

tech-stack:
  added: []
  patterns:
    - "Parse TemplateV2VO then fallback substring scan for asset:{uuid} and UUID literals"
    - "ConsoleApiAdvice 409 ResponseEntity with structured GeoAssetInUsePayload"
    - "In-memory GeoAssetResolver stub for calcite pipeline IT (same spine as GeoAssetService)"

key-files:
  created:
    - data-generator-service/src/main/java/org/gensokyo/data/geo/GeoAssetReferenceScanner.java
    - data-generator-service/src/main/java/org/gensokyo/data/geo/GeoAssetInUseException.java
    - data-generator-service/src/main/java/org/gensokyo/data/geo/GeoAssetTemplateUsage.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/GeoAssetTemplateUsageView.java
    - data-generator-service/src/test/java/org/gensokyo/data/geo/GeoAssetReferenceScannerTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerGeoAssetSourceTests.java
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/geo/GeoAssetService.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleGeoAssetController.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleApiAdvice.java
    - data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetControllerIT.java

key-decisions:
  - "Pipeline IT uses in-memory GeoAssetResolver seeded from classpath fixture — same create-path as service bean without calcite→service Maven dependency"
  - "Unknown asset at run time: root IAE names id; TemplateV2RuntimeRegistry wraps as IllegalStateException — test asserts cause chain"

patterns-established:
  - "Delete guard: findUsages → GeoAssetInUseException → HTTP 409 with template id/name list"
  - "Hard delete only after empty usage scan; audit only on successful delete"

requirements-completed: [GEO-09, GEO-10, GEO-11, GOV-01]

coverage:
  - id: D1
    description: "DELETE blocked with 409 and usage hints when templates reference asset"
    requirement: GEO-09
    verification:
      - kind: unit
        ref: "data-generator-service/src/test/java/org/gensokyo/data/geo/GeoAssetReferenceScannerTests.java"
        status: pass
      - kind: integration
        ref: "data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetControllerIT.java#delete_referencedByTemplate_returns409WithUsageHints"
        status: pass
    human_judgment: false
  - id: D2
    description: "Successful delete emits GEO_ASSET_DELETE audit"
    requirement: GOV-01
    verification:
      - kind: integration
        ref: "data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleGeoAssetControllerIT.java#delete_unreferencedAsset_succeedsAndAudits"
        status: pass
    human_judgment: false
  - id: D3
    description: "Template V2 pipeline resolves dedicated asset-id and asset: wire format"
    requirement: GEO-10
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerGeoAssetSourceTests.java"
        status: pass
    human_judgment: false
  - id: D4
    description: "Unknown asset id fails run with id named in exception cause chain"
    requirement: GEO-11
    verification:
      - kind: unit
        ref: "data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerGeoAssetSourceTests.java#unknownAssetId_failsWithIllegalArgumentNamingId"
        status: pass
    human_judgment: false

duration: 90min
completed: 2026-08-01
status: complete
---

# Phase 21 Plan 03 Summary

**Hard-delete with template reference 409 + delete audit, plus TemplateV2Runner asset-id pipeline proof — Phase 21 success criteria observable; P0 gate still 15**

## Performance

- **Duration:** ~90 min
- **Started:** 2026-08-01T15:11:00+08:00
- **Completed:** 2026-08-01T16:30:00+08:00
- **Tasks:** 4
- **Files modified:** 10

## Accomplishments

- `GeoAssetReferenceScanner` walks active templates (JSON preferred, YAML fallback, raw substring fallback) for dedicated asset-id fields and `asset:{uuid}` paths
- `DELETE /api/console/geo-assets/{id}` hard-deletes when unreferenced; returns 409 with template usage payload when referenced
- Successful deletes emit `GEO_ASSET_DELETE` / `GEO_ASSET` audit events
- `TemplateV2RunnerGeoAssetSourceTests` proves boundaryAssetId, geojson assetId, `asset:` wire format, and unknown-id failure on the execute path
- Full Phase 21 verify bundle green; `.planning/test-matrix.yaml` P0 tier unchanged (15 rows); no console-web edits

## Task Commits

1. **Tasks 1–4: Delete guard + pipeline IT + verification** — (pending commit)

## Files Created/Modified

- `GeoAssetReferenceScanner.java` / `GeoAssetInUseException.java` / `GeoAssetTemplateUsage.java` — delete-guard domain
- `GeoAssetTemplateUsageView.java` — 409 console DTO
- `GeoAssetService.delete` / `ConsoleGeoAssetController` DELETE / `ConsoleApiAdvice` 409 handler
- `GeoAssetReferenceScannerTests.java` — five scan scenarios
- `ConsoleGeoAssetControllerIT.java` — delete success + audit + 409 blocked
- `TemplateV2RunnerGeoAssetSourceTests.java` — four pipeline proofs

## Decisions Made

- Calcite pipeline IT uses stub `GeoAssetResolver` (research allows calcite or service) to avoid module dependency cycle while exercising the same factory/row-source spine as production
- Unknown-asset assertion walks cause chain because `TemplateV2RuntimeRegistry` wraps source-factory IAE as `IllegalStateException`

## Deviations from Plan

**1. Pipeline IT package path**
- **Issue:** Plan listed `…/calcite/runtime/TemplateV2RunnerGeoAssetSourceTests.java`
- **Fix:** Placed next to existing geo runner tests in `org.gensokyo.data.calcite`
- **Impact:** Class name and surefire `-Dtest` unchanged; matches Phase 20 layout

**2. Pipeline IT does not call GeoAssetService/H2 upload**
- **Issue:** Calcite module must not depend on service JPA
- **Fix:** In-memory resolver seeded from `classpath:geo/南沙区边界.geojson`
- **Impact:** Same `GeoAssetResolver` contract and factory wiring; service upload path already covered by 21-01 ITs

## Issues Encountered

- PowerShell: quote `-pl` module list; use `-Dsurefire.failIfNoSpecifiedTests=false` with `-am`
- First unknown-asset assert expected bare IAE; fixed to cause-chain check

## User Setup Required

None.

## Next Phase Readiness

- Phase 21 complete — ready for `/gsd-verify-work 21` or `/gsd-plan-phase 22`
- Phase 22 can consume DELETE 409 UX, GET `/{id}/geojson`, and VO asset-id fields for map + editor
- TEST-11 / DOC-01 remain Phase 23

---
*Phase: 21-geo-asset-registry-runtime-resolution*
*Completed: 2026-08-01*
