# Phase 20: Pipeline Proof + Docs + P1 - Context

**Gathered:** 2026-07-30  
**Status:** Ready for planning  
**Mode:** `--auto` (recommended defaults applied in one pass)

<domain>
## Phase Boundary

Close v2.2 by proving Template V2 end-to-end `geo_synthetic` → transform → sink for all four modes, documenting the `geo_synthetic` vs `geojson` split with a minimal YAML example, and promoting the harness matrix row `geo-synthetic` to **P1** with linked tests — without expanding the P0 merge gate (`p0.total` stays 15).

**In scope:** TemplateV2Runner IT (calcite) covering BOUNDARY_POINTS, LINE_SAMPLE, BBOX, CIRCLE; maintainer docs (GEO-04); `.planning/test-matrix.yaml` + `docs/test-feature-matrix.md` sync for TEST-10; note V2 preference over V1 geo iterator in docs (no hard removal).

**Out of scope:** Asset upload (GEO-05), polygons (GEO-06), console map UI (GEO-07), new Calcite `ST_*` surface, P0 promotion of geo-synthetic, V1 iterator deletion, reopening Phase 18/19 generator/VO/Factory work.

</domain>

<decisions>
## Implementation Decisions

### Carrying forward (locked — do not reopen)
- Approach A `type: geo_synthetic` already shipped in Phase 19 (VO → Mapper → Factory → RowSource → CoreConfig).
- Path-only assets via `GeoResourceResolver`; `type: geojson` read-only unchanged (GEO-03).
- Phase 18 generator modes + seed/BBOX/CIRCLE semantics locked in `18-CONTEXT.md`.
- Design YAML example in `docs/superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md` is the operator-facing contract for docs copy.

### Pipeline IT shape (GEO-02 pipeline closeout)
- **D-01:** Add a dedicated test class `TemplateV2RunnerGeoSyntheticSourceTests` in `data-generator-calcite` (do **not** bloat `TemplateV2RunnerGeoSourceTests` further) — parallel helper pattern: `geoSyntheticRegistry()` registering `GeoSyntheticSourceFactory` + `SqlTransformFactory` + `ConsoleSinkFactory`.
- **D-02:** Four explicit `@Test` methods (one per mode), not a single shared-only fixture — each asserts run SUCCESS and expected row count (small counts: boundary/line via existing `classpath:geo/*` fixtures; BBOX/CIRCLE with tiny bbox/center + seed).
- **D-03:** Pipeline shape: `geo_synthetic` source → minimal SQL transform `select lon, lat from <source>` (or declared column names) → console sink. No new SQL UDFs. Optional one test may filter with existing `V2_GEO_*` only if it stays one-liner; default is passthrough.
- **D-04:** Do **not** require Spring Boot service IT or Playwright for this phase — calcite `TemplateV2Runner` in-process is the acceptance bar (matches Phase 19 D-14 deferral wording and ROADMAP SC1).

### Docs (GEO-04)
- **D-05:** Update `docs/geospatial-overview.md` as the primary maintainer landing page: add `GEO_SYNTHETIC` / `geo_synthetic` to the Template V2 source types table; distinguish vs `GEOJSON` and vs `ITERATOR`+`GEO`; list Phase 19 modules (`GeoSyntheticSourceVO`, Factory/RowSource); note V2 preference for new work.
- **D-06:** Add a minimal YAML example (copy/adapt design-spec example with classpath fixtures) either as a new short section in `docs/geospatial-overview.md` or a sibling `docs/geo-synthetic-v2-source.md` linked from overview — prefer **section in overview + link** if content stays short; if >~40 lines of YAML/modes detail, use dedicated `docs/geo-synthetic-v2-source.md` and link it from overview.
- **D-07:** Document output formats (`columns` / `wkt` / `geojson`) and that SQL companion is documentation of existing `V2_GEO_*` only — **no new `ST_*` APIs**.
- **D-08:** Sync `docs/test-feature-matrix.md` row for `geo-synthetic` when the YAML matrix changes.

### Harness P1 (TEST-10)
- **D-09:** In `.planning/test-matrix.yaml`, update row `geo-synthetic`: `tier: P1`, `status: covered` (or `partial` only if harness scripts cannot see all linked classes yet — prefer `covered` once IT + unit links exist), `test_types: [unit, integration]`, `owner_module: data-generator-calcite`, `adapter: geo_synthetic` (replace stale `geojson` adapter label), `linked_tests` include at least `TemplateV2RunnerGeoSyntheticSourceTests` plus Phase 19 `GeoSyntheticRowSourceTests` / `GeoSyntheticRequestMapperTests` as supporting evidence.
- **D-10:** **Freeze P0:** do not add geo-synthetic to P0; do not change `verify-harness.ps1` P0 gate semantics; confirm `p0.total` remains **15** after the edit (re-count P0 rows if the file has no explicit `p0.total` field — zero new `tier: P0` entries).
- **D-11:** Update notes to reflect V2 `geo_synthetic` source (not “GeoJSON synthetic geometry” only).

### Claude's Discretion
- Exact SQL column aliases if output format uses custom `columnNames`.
- Whether BBOX/CIRCLE IT counts are 2 vs 4 rows.
- Whether docs YAML lives inline in overview vs dedicated file (per D-06 heuristic).

### Auto log
```
[--auto] Selected all gray areas: Pipeline IT shape, Docs placement, Harness P1 matrix.
[auto] Pipeline — Q: "test class layout?" → Selected: "Dedicated TemplateV2RunnerGeoSyntheticSourceTests; 4 mode tests; console sink" (recommended)
[auto] Pipeline — Q: "SQL depth?" → Selected: "Passthrough select lon,lat; no new ST_*" (recommended / design-locked)
[auto] Docs — Q: "where to document?" → Selected: "Update geospatial-overview.md; dedicated page only if long" (recommended)
[auto] Matrix — Q: "tier / links?" → Selected: "P1 covered; link runner IT + Phase 19 unit tests; P0 frozen at 15" (recommended / TEST-10)
```

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone / requirements
- `docs/superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md` — YAML example, acceptance, SQL companion = docs only
- `.planning/REQUIREMENTS.md` — GEO-04, TEST-10 (pending); GEO-02 pipeline evidence closeout
- `.planning/ROADMAP.md` — Phase 20 goal and success criteria
- `.planning/phases/19-v2-geo-synthetic-source/19-CONTEXT.md` — shipped V2 source decisions; D-14 deferred items now this phase
- `.planning/phases/19-v2-geo-synthetic-source/19-VERIFICATION.md` — Phase 19 passed evidence
- `.planning/phases/18-geo-generator-modes/18-CONTEXT.md` — mode/seed semantics for IT fixtures

### Pipeline / harness patterns
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/TemplateV2RunnerGeoSourceTests.java` — registry + TemplateV2Runner + console sink pattern to mirror
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoSyntheticSourceFactory.java` — register in test registry
- `data-generator-calcite/src/test/resources/geo/` — boundary/network fixtures
- `.planning/test-matrix.yaml` — row `geo-synthetic` (currently P2/pending)
- `docs/test-feature-matrix.md` — human-readable matrix mirror
- `docs/geospatial-overview.md` — primary docs landing page to update
- `docs/testing-embedded-components.md` / `docs/test-harness.md` — P0 vs P1 gate semantics (P0 frozen)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `TemplateV2RunnerGeoSourceTests.geoJsonRegistry()` — clone for `GeoSyntheticSourceFactory`
- Phase 19 `GeoSyntheticRowSourceTests` — mode config examples for IT VO setup
- Existing console sink + SQL transform factories already used in geo runner tests

### Established Patterns
- In-process `TemplateV2Runner` + `TemplateV2RuntimeRegistry` list of factories (no Spring context required)
- Matrix rows use `linked_tests` class simple names; P1 is non-blocking for merge

### Integration Points
- Docs overview still lists only ITERATOR+GEO and GEOJSON — must add geo_synthetic
- Matrix `adapter: geojson` / `owner_module: data-generator-geo` is stale relative to V2 source

</code_context>

<specifics>
## Specific Ideas

Copy the design-spec YAML example into docs nearly verbatim (classpath paths may point at documented sample names). Keep SQL companion as “use existing V2_GEO_* if needed” — no new functions.

</specifics>

<deferred>
## Deferred Ideas

- P0 promotion of geo-synthetic — future milestone only
- GeoJSON upload / polygons / console map UI — beyond v2.2
- Shared V1/V2 mapper extraction — optional follow-up
- Hard removal of V1 geo iterator — docs preference only this phase

</deferred>

---

*Phase: 20-Pipeline Proof + Docs + P1*  
*Context gathered: 2026-07-30 (--auto)*
