# Phase 19: V2 Geo Synthetic Source - Context

**Gathered:** 2026-07-30  
**Status:** Ready for planning  
**Mode:** `--auto` (recommended defaults applied in one pass)

<domain>
## Phase Boundary

Expose geographic point synthesis as Template V2 `type: geo_synthetic` (Approach A): `GeoSyntheticSourceVO` + `GeoSyntheticSourceFactory` + `GeoSyntheticRowSource` + `CoreConfig` bean registration. Path-referenced GeoJSON only via `GeoResourceResolver`. Leave `type: geojson` read-only behavior unchanged.

**In scope:** VO (core) with four-mode config; VO → `GeoGenerationRequest` mapping; Factory/RowSource (calcite) materializing finite `List<Row>` + `RowSchema`; CoreConfig registration parallel to `GeoJsonSourceFactory`; unit/integration tests for factory + RowSource + mapping; invalid-config `IllegalArgumentException` with source name + field.

**Out of scope this phase:** TemplateV2Runner end-to-end pipeline IT, docs distinguishing sources, P1 matrix linkage (Phase 20); asset upload; polygon synthesis; console map UI; V1 geo iterator hard removal or mandatory shared-mapper refactor.

</domain>

<decisions>
## Implementation Decisions

### Carrying forward (locked — do not reopen)
- **Approach A:** new `geo_synthetic` source — do not extend `geojson` or bridge via V1 iterator (design + ROADMAP).
- **Module placement:** VO in `data-generator-common/data-generator-core`; Factory + RowSource in `data-generator-calcite`; bean in `CoreConfig` (same pattern as `GeoJsonSourceFactory`).
- **Generator:** Phase 18 BBOX/CIRCLE + existing boundary/line via `GeoSyntheticGenerator.generateRows`.
- **Paths:** `GeoResourceResolver` / classpath|filesystem only — no upload API (GEO-03).

### VO YAML shape
- **D-01:** Match design example: `type: geo_synthetic`, `mode`, `count`, `seed`, `minDistanceMeters`, `boundaryPath`, `networkPath`, `featureIndex`, `randomFeature`, nested `sample` / `output`, plus `bbox: [minLon, minLat, maxLon, maxLat]` and `center: [lon, lat]` as YAML arrays (not flat bboxMinLon fields on the VO).
- **D-02:** `@JsonSubType("GEO_SYNTHETIC")` + constructor `setType("geo_synthetic")` mirroring `GEOJSON` / `geojson`.
- **D-03:** When YAML omits `seed`, default to `0` (Phase 18 treats seed as always set; `0` is a valid deterministic seed).

### Request mapper
- **D-04:** Add a dedicated `GeoSyntheticRequestMapper` (calcite, next to Factory/RowSource) that expands VO arrays into `GeoGenerationRequest` flat fields (`bboxMinLon`…, `centerLon`/`centerLat`, `radiusMeters`, sample/output).
- **D-05:** Do **not** refactor `GeoIteratorRequestMapper` / V1 iterator in this phase. Shared helper extraction with V1 is optional follow-up — avoid dual-module churn while shipping GEO-01.

### Validation & errors
- **D-06:** Build `GeoGenerationRequest` then call existing `validate()`; mode-required path presence for boundary/line also enforced in mapper (blank path → fail before/at validate).
- **D-07:** Surface failures as `IllegalArgumentException` including logical **source name** and **field** (align with `GeoJsonRowSource` messaging). Do not invent a new exception hierarchy.
- **D-08:** Unreadable/missing GeoJSON path: fail with path in message (same spirit as geojson source `IOException` wrap).

### Output & schema
- **D-09:** Introduce `GeoSyntheticSourceOutputVO` parallel to `GeoJsonSourceOutputVO` (`format`, `columnNames`, `includeProperties`) — same knobs, independent type (do not couple read-only geojson output type to synthetic).
- **D-10:** Optional declared `RowSchema` on VO; otherwise infer via existing `GeoRowSchemaSupport` / same pattern as `GeoJsonRowSource` after `GeoSyntheticGenerator.generateRows`.

### Registration & RowSource behavior
- **D-11:** `CoreConfig` bean `geoSyntheticSourceFactory` with `@ConditionalOnMissingBean`, returning `GeoSyntheticSourceFactory` — mirror `geoJsonSourceFactory`.
- **D-12:** `GeoSyntheticRowSource` eagerly materializes finite rows in constructor (like `GeoJsonRowSource`); no streaming source for this phase.

### Phase 19 test boundary
- **D-13:** Calcite tests: Factory `supports`/`create`, RowSource row count + schema for all four modes (reuse `data-generator-calcite/src/test/resources/geo/*` for BOUNDARY/LINE), mapping unit coverage, invalid config cases.
- **D-14:** Full `TemplateV2Runner` pipeline IT, operator docs, and P1 `geo-synthetic` matrix row → **Phase 20** only.
- **D-15:** Existing `GeoJsonRowSourceTests` / `TemplateV2RunnerGeoSourceTests` and geo generator suite must stay green (regression for GEO-03).

### Claude's Discretion
- Exact nested sample VO class name (`GeoSyntheticSampleVO` vs reuse iterator sample type if accessible without bad module deps).
- Whether mapper lives in `org.gensokyo.data.calcite.source` vs a small `…mapper` package under calcite.
- Fixture counts for BBOX/CIRCLE tests (small count + seed assert is enough).

### Auto log
```
[--auto] Selected all gray areas: VO YAML shape, Request mapper placement, Validation & errors, Output & schema, Phase 19 test boundary.
[auto] VO shape — Q: "bbox/center representation?" → Selected: "YAML arrays per design; mapper expands to request flats" (recommended)
[auto] Mapper — Q: "shared with V1 now?" → Selected: "Dedicated calcite mapper; leave V1 unchanged" (recommended)
[auto] Validation — Q: "validation ownership?" → Selected: "request.validate() + source-name IllegalArgumentException" (recommended)
[auto] Output — Q: "reuse GeoJsonSourceOutputVO?" → Selected: "Parallel GeoSyntheticSourceOutputVO" (recommended)
[auto] Tests — Q: "Phase 19 depth?" → Selected: "Factory/RowSource/mapping only; runner IT → Phase 20" (recommended)
```

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone / requirements
- `docs/superpowers/specs/2026-07-30-geo-synthetic-v2-source-design.md` — locked Approach A, YAML model, module placement, acceptance
- `.planning/REQUIREMENTS.md` — GEO-01, GEO-03 (this phase); GEO-04 / TEST-10 are Phase 20
- `.planning/ROADMAP.md` — Phase 19 goal and success criteria
- `.planning/phases/18-geo-generator-modes/18-CONTEXT.md` — generator decisions (seed, BBOX/CIRCLE, errors) that VO must map onto

### Prior art / geospatial foundations
- `docs/superpowers/specs/2026-05-20-geospatial-phase1-design.md` — established generator / geojson foundations (cited by design)

### Patterns to mirror
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoJsonSourceVO.java` — `@AutoService` + `@JsonSubType` + `setType`
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/GeoJsonSourceOutputVO.java` — output knobs to parallel
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoJsonSourceFactory.java` — factory pattern
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/GeoJsonRowSource.java` — finite rows + schema + named errors
- `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java` — `geoJsonSourceFactory` bean registration
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoSyntheticGenerator.java` — `generateRows` / modes
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/GeoGenerationRequest.java` — validate + flat bbox/circle fields
- `data-generator-geo/src/main/java/org/gensokyo/data/geo/io/GeoResourceResolver.java` — path resolution (GEO-03)
- `data-generator-iterator/data-generator-iterator-geo/src/main/java/org/gensokyo/data/iterator/GeoIteratorRequestMapper.java` — reference mapping only (do not require refactor)
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/GeoJsonRowSourceTests.java` — test style / fixtures under `src/test/resources/geo/`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GeoJsonSourceFactory` / `GeoJsonRowSource` — clone structural pattern for synthetic source
- `GeoSyntheticGenerator.generateRows` — already formats columns/wkt/geojson maps
- `GeoRowSchemaSupport` — schema inference used by geojson RowSource
- Calcite test GeoJSON under `data-generator-calcite/src/test/resources/geo/` — BOUNDARY_POINTS / LINE_SAMPLE fixtures
- Phase 18 `BboxPointGenerator` / `CirclePointGenerator` — no file needed for BBOX/CIRCLE tests

### Established Patterns
- `@AutoService(SourceVO.class)` + `@JsonSubType("…")` + lowercase `type` string
- `V2SourceFactory.supports` instanceof check; `CoreConfig` `@Bean` + `@ConditionalOnMissingBean`
- Eager materialization in RowSource constructor; `IllegalArgumentException` with source name

### Integration Points
- `TemplateV2RuntimeRegistry` discovers factories via Spring beans from CoreConfig
- Downstream transforms/sinks unchanged once rows + schema exist
- Phase 20 will wrap this source in TemplateV2Runner IT + docs + P1 matrix

</code_context>

<specifics>
## Specific Ideas

Design YAML example is the operator-facing contract (arrays for bbox/center, nested sample/output). Prefer matching that example literally so docs in Phase 20 can copy it without VO redesign.

</specifics>

<deferred>
## Deferred Ideas

- TemplateV2Runner IT for four modes → Phase 20
- Docs `geo_synthetic` vs `geojson` + minimal YAML → Phase 20 (GEO-04)
- P1 matrix `geo-synthetic` linkage; P0 frozen → Phase 20 (TEST-10)
- Shared mapper extraction with `GeoIteratorRequestMapper` → optional follow-up
- GeoJSON upload / asset-id (GEO-05), polygons (GEO-06), console map UI (GEO-07) → backlog beyond v2.2
- V1 geo iterator hard removal → out of milestone

</deferred>

---

*Phase: 19-V2 Geo Synthetic Source*  
*Context gathered: 2026-07-30 (--auto)*
