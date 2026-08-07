# Phase 23: Docs + Harness Closeout - Context

**Gathered:** 2026-08-07  
**Status:** Ready for planning  
**Mode:** --auto (recommended defaults; single pass)

<domain>
## Phase Boundary

Close v2.3 by documenting asset-id binding and console map preview for operators/maintainers (DOC-01), and linking a **P1** (non-blocking) `geo-assets` harness row to real Maven tests without expanding the **P0** merge gate (TEST-11; `p0.total` stays **15**). Depends on Phase 21–22 shipped code — this phase is docs + matrix/harness linkage, not new product features.

**Requirements:** DOC-01, TEST-11

**Out of scope:** P0 promotion of geo-assets / map E2E; GEO-06 polygons; new GIS platform features; inventing a second resolve spine; rewriting Phase 21/22 APIs; `/gsd-complete-milestone` ceremony (run after this phase verifies — not a DOC/TEST task).

</domain>

<decisions>
## Implementation Decisions

### Docs placement & content (DOC-01)
- **D-01:** Primary YAML/asset-id reference stays in **`docs/geo-synthetic-v2-source.md`** — replace the v2.2 “asset upload not in scope” line with asset-id examples (`boundaryAssetId` / `networkAssetId` / geojson `assetId`), path vs asset-id precedence (**asset-id wins** per Phase 21 D-02), and `asset:{uuid}` wire-format notes.
- **D-02:** Update **`docs/geospatial-overview.md`** as the landing page: add v2.3 phase row (asset registry + console map), remove/replace “upload deferred / console map follow-up” wording, link to the geo_synthetic doc and console `/geo-assets` / editor preview UX.
- **D-03:** If map preview + upload limits + console usage exceed ~40 lines of new prose, add a short sibling **`docs/geo-assets.md`** (upload/list/delete, size/feature limits, map preview honesty) and link it from overview — same heuristic as Phase 20 D-06. Prefer overview section + link if content stays short.
- **D-04:** Document upload limits from shipped config: `data.generator.geo-assets.max-bytes` (default 50 MiB / `52428800`) and `max-features` (default `10000`), plus Spring multipart raise behavior from Phase 21 D-06 — do not invent new knobs.
- **D-05:** Document console map preview usage: `/geo-assets` browse; template-editor `geo_synthetic` hybrid preview; seed honesty (preview ≠ full run). Point at Phase 22 behavior — no new UI work.
- **D-06:** Sync generated **`docs/test-feature-matrix.md`** when the YAML matrix changes (`scripts/generate-test-matrix-doc.ps1`).

### Harness P1 (TEST-11)
- **D-07:** Add a new matrix row **`geo-assets`** (capability/adapter naming aligned with existing `geo-synthetic` / `console-api-*` style) at **`tier: P1`**, **`status: covered`** once links resolve, **`owner_module: data-generator-service`**, `test_types: [unit, integration]`.
- **D-08:** `linked_tests` MUST include at least: `ConsoleGeoAssetControllerIT`, `ConsoleGeoAssetPreviewIT`, `GeoAssetServiceTests` — add `GeoAssetReferenceScannerTests` if it remains a meaningful unit slice. Do **not** require Playwright/Podman for the matrix row (smoke exists from Phase 22; P1 bar is Maven IT/unit like Phase 20).
- **D-09:** **`verify-harness.ps1` P0 set remains 15 rows** — never promote `geo-assets` or map E2E to P0 in this phase. P1 stays non-blocking for merge.
- **D-10:** Prove “geo-assets verification slice green” by ensuring linked Surefire classes are collected into `target/test-matrix-summary.json` when the service/calcite modules under test are run (same harness summary path as other P1 rows). No mandatory new UAT script; optional thin `scripts/verify-phase23-uat-geo-assets.ps1` only if planner finds it improves operator replay — must stay supplementary (not merge gate).

### Claude's Discretion
- Whether DOC-01 map/upload prose lives only in overview vs `docs/geo-assets.md` (per D-03 length heuristic)
- Exact matrix `notes` / capability strings and whether to also note Phase 21 resolver ITs if they live outside service module
- Optional Phase 23 UAT script vs matrix-only evidence
- Ordering of doc edits vs matrix edit within plans

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone & requirements
- `.planning/PROJECT.md` — v2.3 remaining DOC-01 / TEST-11; P0 freeze
- `.planning/REQUIREMENTS.md` — DOC-01, TEST-11
- `.planning/ROADMAP.md` — Phase 23 success criteria (docs + P1 link + P0=15 + harness summary green)
- `.planning/test-matrix.yaml` — source of truth; existing `geo-synthetic` P1 row as pattern
- `docs/test-harness.md` — P0 vs P1 gate semantics
- `scripts/verify-harness.ps1` — merge gate; must stay P0=15
- `scripts/generate-test-matrix-doc.ps1` — regenerate `docs/test-feature-matrix.md`

### Prior phase locks
- `.planning/phases/21-geo-asset-registry-runtime-resolution/21-CONTEXT.md` — D-02 asset-id wins; D-06 limits; Phase 23 docs deferral
- `.planning/phases/21-geo-asset-registry-runtime-resolution/21-VERIFICATION.md` — shipped API/runtime evidence
- `.planning/phases/22-console-map-geo-synthetic-editor/22-CONTEXT.md` — map preview + seed honesty; Phase 23 docs deferral
- `.planning/phases/22-console-map-geo-synthetic-editor/22-VERIFICATION.md` — console UX shipped
- `.planning/milestones/v2.2-phases/20-pipeline-proof-docs-p1/20-CONTEXT.md` — docs + P1 closeout pattern to mirror

### Docs to update
- `docs/geo-synthetic-v2-source.md` — path-only today; add asset-id
- `docs/geospatial-overview.md` — landing; still says upload/map follow-ups
- `docs/test-feature-matrix.md` — generated from YAML

### Code / config evidence for docs
- `data-generator-service/.../DataGeneratorProperties.GeoAssets` — `maxBytes` / `maxFeatures` defaults
- `data-generator-service/.../ConsoleGeoAssetController.java` — upload/list/geojson/preview APIs
- `data-generator-common/.../GeoSyntheticSourceVO.java` — asset-id fields
- Tests: `ConsoleGeoAssetControllerIT`, `ConsoleGeoAssetPreviewIT`, `GeoAssetServiceTests`, `GeoAssetReferenceScannerTests`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Phase 20 pattern: update overview + dedicated geo_synthetic doc + P1 matrix row with `linked_tests`
- Service ITs already exist for controller/preview/service — link them; do not rewrite product code unless a doc claim is wrong
- `generate-test-matrix-doc.ps1` regenerates feature matrix from YAML

### Established Patterns
- P1 rows are non-blocking; P0 freeze is explicit in ROADMAP/REQUIREMENTS
- Docs live under `docs/` with overview as landing page
- Harness summary JSON at `target/test-matrix-summary.json`

### Integration Points
- Edit docs + `.planning/test-matrix.yaml` (+ regenerate matrix doc)
- Optionally point AGENTS.md / CLAUDE.md verify script list at a Phase 23 UAT script if added
- Milestone complete is a separate post-verify command, not a plan task

</code_context>

<specifics>
## Specific Ideas

- [--auto] Selected all gray areas: Docs placement/content, Harness P1 matrix, P0 freeze / harness green bar.
- [auto] Docs — Q: "Where do asset-id + map docs live?" → Selected: "Extend geo-synthetic-v2-source.md + update geospatial-overview.md; optional geo-assets.md if long" (recommended / Phase 20 D-06 heuristic)
- [auto] Docs — Q: "What must DOC-01 cover?" → Selected: "Asset-id YAML, path vs asset-id wins, map preview usage, max-bytes/max-features limits" (ROADMAP SC1)
- [auto] Matrix — Q: "P1 row?" → Selected: "Add geo-assets P1 covered; link ConsoleGeoAsset* IT + GeoAssetServiceTests; P0 frozen at 15" (recommended / TEST-11)
- [auto] Evidence — Q: "UAT script required?" → Selected: "Matrix + harness summary green; optional thin UAT script only if useful" (recommended)

</specifics>

<deferred>
## Deferred Ideas

- `/gsd-complete-milestone` for v2.3 — after Phase 23 verification (ceremony, not DOC/TEST scope)
- P0 promotion of geo-assets / map Playwright — explicitly out of scope
- GEO-06 polygon synthesis — future
- Fixing Phase 22 LINE_SAMPLE preview `sample.strategy` DTO gap — product fix, not docs (track separately if needed)
- PostGIS / external GIS server — already deferred

</deferred>

---

*Phase: 23-docs-harness-closeout*  
*Context gathered: 2026-08-07 via --auto*
