# Phase 19: V2 Geo Synthetic Source - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-30  
**Phase:** 19-V2 Geo Synthetic Source  
**Mode:** `--auto`  
**Areas discussed:** VO YAML shape, Request mapper placement, Validation & errors, Output & schema, Phase 19 test boundary

---

## VO YAML shape

| Option | Description | Selected |
|--------|-------------|----------|
| YAML arrays (bbox[4], center[2]) matching design | Mapper expands to GeoGenerationRequest flats | ✓ |
| Flat fields on VO (bboxMinLon…) | Mirror request fields 1:1 in YAML | |
| Nested bbox/center objects | `{minLon, maxLon, …}` objects | |

**User's choice:** [auto] YAML arrays per design; mapper expands to request flats  
**Notes:** Seed omitted → default `0`. `@JsonSubType("GEO_SYNTHETIC")` / `type: geo_synthetic`.

---

## Request mapper placement

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated calcite mapper; leave V1 unchanged | Ship GEO-01 without iterator-geo refactor | ✓ |
| Extract shared mapper in data-generator-geo now | Both V1 and V2 call shared helpers | |
| Extend GeoIteratorRequestMapper for V2 VO | Couples calcite path to iterator-geo module | |

**User's choice:** [auto] Dedicated `GeoSyntheticRequestMapper` in calcite; V1 unchanged  
**Notes:** Shared extraction noted as optional follow-up.

---

## Validation & errors

| Option | Description | Selected |
|--------|-------------|----------|
| Map → request.validate(); prefix with source name | Align with GeoJsonRowSource | ✓ |
| Duplicate all validation on VO only | Risk of drift from generator | |
| New typed exception hierarchy | Out of phase scope | |

**User's choice:** [auto] request.validate() + source-name IllegalArgumentException  
**Notes:** Path IO failures include path in message.

---

## Output & schema

| Option | Description | Selected |
|--------|-------------|----------|
| Parallel GeoSyntheticSourceOutputVO | Same fields as geojson output, independent type | ✓ |
| Reuse GeoJsonSourceOutputVO | Couples read source type to synthetic | |
| Reuse V1 GeoOutputConfigVO from iterator-geo | Bad module dependency for core VO | |

**User's choice:** [auto] Parallel GeoSyntheticSourceOutputVO  
**Notes:** Optional RowSchema; else GeoRowSchemaSupport inference.

---

## Phase 19 test boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Factory/RowSource/mapping tests only | Runner IT + docs + P1 → Phase 20 | ✓ |
| Include TemplateV2Runner IT in Phase 19 | Overlaps Phase 20 success criteria | |
| Mapping-only unit tests, skip RowSource IT | Too thin for GEO-01 evidence | |

**User's choice:** [auto] Factory/RowSource/mapping only; runner IT → Phase 20  
**Notes:** Reuse calcite `src/test/resources/geo/*`; keep geojson tests green.

---

## Claude's Discretion

- Nested sample VO naming / package
- Exact mapper package under calcite
- Small BBOX/CIRCLE fixture counts

## Deferred Ideas

- TemplateV2Runner IT, docs, P1 matrix → Phase 20
- Shared V1/V2 mapper extraction → optional follow-up
- Upload / polygons / console map → beyond v2.2
