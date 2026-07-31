# Phase 21: Geo Asset Registry + Runtime Resolution - Context

**Gathered:** 2026-07-31  
**Status:** Ready for planning  
**Mode:** `--auto` (single-pass; recommended defaults from v2.3 research)

<domain>
## Phase Boundary

Deliver durable GeoJSON assets in the metadata DB with upload/list/get/delete, size/feature validation, audit, template asset-id binding for `geo_synthetic` / `geojson`, and execute-path `asset:{id}` resolution via a shared `GeoAssetResolver`. Console map UI and `geo_synthetic` editor belong to Phase 22 — out of scope here except APIs Phase 22 will call.

**Requirements:** GEO-05, GEO-08, GEO-09, GEO-10, GEO-11, GOV-01

</domain>

<decisions>
## Implementation Decisions

### Asset identity & template binding
- **D-01:** Console/API primary fields are dedicated IDs: `boundaryAssetId` / `networkAssetId` on `geo_synthetic`, and `assetId` (or equivalent) on `geojson`. At runtime, mappers normalize these to `asset:{uuid}` for the shared resolver spine.
- **D-02:** Path and `classpath:` remain first-class (GEO-03). If both path and asset-id are set for the same role, **asset-id wins** and path is ignored with a clear validation warning/error message naming the source + field (fail-fast preferred over silent dual-read).
- **D-03:** Wire format `boundaryPath: asset:{uuid}` (and network) MUST also resolve — so YAML authors and tests can use either dedicated fields or `asset:` prefix without a second code path.

### Persistence & upload limits
- **D-04:** Persist validated GeoJSON text as **CLOB** in metadata DB table `geo_asset` (mirror secrets/UDF blob patterns). No filesystem spill in v2.3 Phase 21.
- **D-05:** Do **not** reuse `ConsoleUploadController` / `../uploaded-sources/` for durable assets — new `ConsoleGeoAssetController` under `/api/console/geo-assets`.
- **D-06:** Configurable limits via `data.generator.geo-assets.*` (e.g. `max-bytes`, `max-features`); raise Spring multipart defaults (~16MB file / ~17MB request) so district-scale GeoJSON is not rejected by Boot defaults. Reject oversize / over-feature **before** persist with 400/413-style `R.fail` messages.
- **D-07:** Accept Feature or FeatureCollection roots only; validate with existing `GeoJsonLoader` / JTS path before store. Derive and store bbox + featureCount (+ optional geometry-type summary) at ingest for list APIs.

### Delete & references
- **D-08:** **Hard delete**. Before delete, scan stored templates for `assetId` / `boundaryAssetId` / `networkAssetId` / `asset:{uuid}` references; if any hit, return **409** with usage hints (template id/name). No soft-delete tombstones in Phase 21.

### Runtime resolution
- **D-09:** Introduce `GeoAssetResolver` (core interface) implemented by `GeoAssetService` in service; inject into `GeoResourceResolver` / source factories so TemplateV2Runner and workers resolve the same way (shared metadata DB). Missing asset → `IllegalArgumentException` with asset id (fail the run).
- **D-10:** Preview (Phase 22) MUST call the same resolve spine — Phase 21 ships GET-by-id that returns authoritative GeoJSON body for map layers.

### Governance
- **D-11:** Audit **upload** and **delete** always via existing `AuditService`. Optional console RBAC uses existing enable flag (default off) — do not invent a separate geo RBAC gate.

### Claude's Discretion
- Exact property names/defaults for `max-bytes` / `max-features` (within research ballpark)
- Table/PO field layout details (checksum, contentType, actor, timestamps)
- Template reference-scan implementation (JSON path walk vs regex) as long as D-08 semantics hold
- Pagination defaults for list endpoint

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — v2.3 goals; metadata DB decision; equal-depth GEO-07 deferred to Phase 22
- `.planning/REQUIREMENTS.md` — GEO-05, GEO-08, GEO-09, GEO-10, GEO-11, GOV-01
- `.planning/ROADMAP.md` — Phase 21 success criteria
- `.planning/research/SUMMARY.md` — stack/architecture/pitfalls synthesis
- `.planning/research/ARCHITECTURE.md` — component map, `asset:` spine, anti-patterns
- `.planning/research/STACK.md` — multipart limits, CLOB, MapLibre (console later)
- `.planning/research/PITFALLS.md` — H2 blow-up, dual resolution, P0 freeze
- `.planning/research/FEATURES.md` — table stakes vs anti-features

### Prior geo (v2.2)
- `docs/geo-synthetic-v2-source.md` — path-only YAML today; extend with asset-id examples in Phase 23
- `docs/superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md` — upload deferred from v2.2
- `.planning/milestones/v2.2-REQUIREMENTS.md` — GEO-05 deferred history

### Code patterns
- `data-generator-geo/.../GeoResourceResolver.java` — extend with `asset:`
- `data-generator-common/.../GeoSyntheticSourceVO.java` — add asset-id fields
- `data-generator-calcite/.../GeoSyntheticRequestMapper.java` / factories — inject resolver
- `data-generator-service/.../ConsoleUdfController.java` — multipart + R envelope pattern (not ephemeral upload path)
- Secrets/UDF PO + repository patterns for CLOB/metadata DB

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GeoJsonLoader` + JTS — upload validation and runtime parse
- `GeoResourceResolver` — single place to add `asset:` branch
- `AuditService` / console `R<T>` / `ConsoleApiAdvice` — API + audit
- Secret/UDF JPA entities — CLOB + UUID id patterns

### Established Patterns
- Type split `geojson` vs `geo_synthetic` must stay (do not collapse)
- Factories use Spring `ObjectProvider` / CoreConfig beans for optional collaborators
- Fail-fast `IllegalArgumentException` with source name + field for bad config

### Integration Points
- New REST under `/api/console/geo-assets`
- CoreConfig wires `GeoAssetResolver` into geo source factories
- Template YAML/JSON documents in metadata DB scanned on delete

</code_context>

<specifics>
## Specific Ideas

[auto] Selected all gray areas: Asset identity & binding; Persistence & limits; Delete & references; Runtime resolution; Governance.

[auto] Asset identity — Q: "Dedicated asset-id fields vs only asset: in path?" → Selected: "Dedicated fields + asset: wire format both supported; asset-id wins if both set" (recommended default from research)

[auto] Persistence — Q: "CLOB vs filesystem spill?" → Selected: "CLOB in metadata DB only for Phase 21" (milestone lock)

[auto] Upload surface — Q: "Reuse ConsoleUploadController?" → Selected: "No — new ConsoleGeoAssetController" (recommended)

[auto] Delete — Q: "Soft vs hard delete?" → Selected: "Hard delete with 409 on references" (recommended)

[auto] Missing asset — Q: "Skip vs fail run?" → Selected: "Fail run with IllegalArgumentException naming asset id" (recommended)

[auto] Dual path+asset — Q: "Merge, prefer path, or prefer asset?" → Selected: "Prefer asset-id; fail-fast if ambiguous preference needs clarity — asset wins" (recommended)

</specifics>

<deferred>
## Deferred Ideas

- Console map / MapLibre / `geo_synthetic` editor — Phase 22 (GEO-07, GEO-12, GEO-13)
- Docs + optional P1 harness row — Phase 23 (DOC-01, TEST-11)
- Filesystem spill for huge assets — only if CLOB proves insufficient later
- GEO-06 polygon synthesis, DATA-01, P0 promotion — out of milestone

None — discussion stayed within phase scope (auto pass)

</deferred>

---

*Phase: 21-Geo Asset Registry + Runtime Resolution*  
*Context gathered: 2026-07-31 (--auto)*
