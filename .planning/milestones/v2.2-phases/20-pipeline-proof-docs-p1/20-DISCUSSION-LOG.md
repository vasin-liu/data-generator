# Phase 20: Pipeline Proof + Docs + P1 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-30  
**Phase:** 20-Pipeline Proof + Docs + P1  
**Mode:** `--auto`  
**Areas discussed:** Pipeline IT shape, Docs placement, Harness P1 matrix

---

## Pipeline IT shape

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated TemplateV2RunnerGeoSyntheticSourceTests; 4 mode tests; console sink | Mirror geojson runner helpers; clear SC1 evidence | ✓ |
| Extend TemplateV2RunnerGeoSourceTests only | Risk of further bloating mixed geojson/iterator class | |
| Single parameterized shared fixture only | Harder failure diagnosis per mode | |
| Spring Boot service IT | Over-scoped vs ROADMAP SC1 / Phase 19 D-14 | |

**User's choice:** [auto] Dedicated runner test class; four mode tests; passthrough SQL; console sink  
**Notes:** No new ST_*; in-process TemplateV2Runner only.

---

## Docs placement

| Option | Description | Selected |
|--------|-------------|----------|
| Update geospatial-overview.md; dedicated page if long | Primary landing page already exists | ✓ |
| Only new dedicated doc, leave overview stale | Breaks discoverability | |
| Only design-spec (no operator/maintainer docs) | Fails GEO-04 | |

**User's choice:** [auto] Overview update + optional dedicated page per length heuristic  
**Notes:** Document output formats; SQL companion = existing V2_GEO_* docs only.

---

## Harness P1 matrix

| Option | Description | Selected |
|--------|-------------|----------|
| P1 covered; link runner IT + Phase 19 units; P0 frozen | Matches TEST-10 | ✓ |
| Leave P2 / pending | Fails TEST-10 | |
| Promote to P0 | Explicitly forbidden this milestone | |

**User's choice:** [auto] tier P1, adapter geo_synthetic, owner calcite, linked_tests set, p0.total unchanged  
**Notes:** Sync docs/test-feature-matrix.md.

---

## Claude's Discretion

- Exact row counts for BBOX/CIRCLE IT
- Inline vs dedicated docs file length split

## Deferred Ideas

- P0 promotion, upload/polygons/console map, V1 hard removal
