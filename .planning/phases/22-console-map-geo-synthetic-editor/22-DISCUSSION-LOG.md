# Phase 22: Console Map + geo_synthetic Editor - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-06  
**Phase:** 22-console-map-geo-synthetic-editor  
**Areas discussed:** Assets page layout, Map preview composition, Seed honesty UX, geo_synthetic editor + asset picker  
**Language:** zh (user-facing)

---

## Assets page layout

### Q1 Main layout

| Option | Description | Selected |
|--------|-------------|----------|
| Left list + right map | Select asset → map updates | ✓ |
| Full map + drawer | Assets in side drawer | |
| Table then detail | Extra hop before map | |

**User's choice:** Q1-1

### Q2 List fields

| Option | Description | Selected |
|--------|-------------|----------|
| name + featureCount + bbox | Matches list API without body | ✓ |
| + uploadedAt / uploadedBy | Extra metadata | |
| + contentType / size if API has them | Prefer when available; no hard invent | ✓ |

**User's choice:** Q2-1,3  
**Notes:** Planner should use existing DTO fields; contentType/size only if already exposed or trivial from PO.

### Q3 Upload entry

| Option | Description | Selected |
|--------|-------------|----------|
| Top-of-page Upload button | Multipart to geo-assets API | ✓ |
| Empty-state only | | |
| Separate wizard page | | |

**User's choice:** Q3-1

### Q4 Delete UX

| Option | Description | Selected |
|--------|-------------|----------|
| Inline delete + 409 Modal | Template usage hints | ✓ |
| Detail/drawer only | | |
| Batch delete | Deferred — scope creep | |

**User's choice:** Q4-1

---

## Map preview composition

### Q1 Asset rendering

| Option | Description | Selected |
|--------|-------------|----------|
| GET …/geojson → MapLibre fill/line | Same spine as runtime | ✓ |
| Bbox rectangle only | | |
| Server simplify endpoint | Deferred | |

**User's choice:** Q1-1

### Q2 Synthetic underlay

| Option | Description | Selected |
|--------|-------------|----------|
| asset-id GET + server resolve for path/classpath | | ✓ |
| asset-id only this phase | | |
| Client reads static assets | Drift risk | |

**User's choice:** Q2-1

### Q3 BBOX/CIRCLE guides

| Option | Description | Selected |
|--------|-------------|----------|
| Client Turf (or equiv.) | Hybrid preview | ✓ |
| All server GeoJSON overlays | | |
| Text-only for BBOX/CIRCLE | | |

**User's choice:** Q3-1

### Q4 Point sampling

| Option | Description | Selected |
|--------|-------------|----------|
| POST preview/synthetic, capped count + seed | Reuse generator | ✓ |
| Geometry/guides only | | |
| Browser reimplementation | Drift risk | |

**User's choice:** Q4-1

---

## Seed honesty UX

### Q1 Placement

| Option | Description | Selected |
|--------|-------------|----------|
| Persistent Alert above map preview | | ✓ |
| First-visit Modal only | | |
| Tooltip only | | |

**User's choice:** Q1-1

### Q2 Copy content

| Option | Description | Selected |
|--------|-------------|----------|
| Cap + seed + not a full run | | ✓ |
| Preview ≠ output only | | |
| Seed/count only | | |

**User's choice:** Q2-1

### Q3 Geometry-only preview

| Option | Description | Selected |
|--------|-------------|----------|
| Still show shorter hint | | ✓ |
| Only when sampling on | | |
| Two intensities | | |

**User's choice:** Q3-1

### Q4 i18n

| Option | Description | Selected |
|--------|-------------|----------|
| i18next zh + en keys | | ✓ |
| Chinese only | | |
| Hardcoded English | | |

**User's choice:** Q4-1

---

## geo_synthetic editor + asset picker

### Q1 Editor placement

| Option | Description | Selected |
|--------|-------------|----------|
| Extend SourceFieldsForm + Sources kind | | ✓ |
| Separate Geo wizard | | |
| YAML paste only | Violates equal-depth | |

**User's choice:** Q1-1

### Q2 Mode fields

| Option | Description | Selected |
|--------|-------------|----------|
| Mode-switched forms (all four modes) | | ✓ |
| All fields always visible | | |
| Only BOUNDARY + BBOX | | |

**User's choice:** Q2-1

### Q3 Asset picker

| Option | Description | Selected |
|--------|-------------|----------|
| Modal list + optional mini-map | Writes asset ids | ✓ |
| Select dropdown only | | |
| Manual UUID only | | |

**User's choice:** Q3-1

### Q4 Path coexistence

| Option | Description | Selected |
|--------|-------------|----------|
| Asset + path; asset wins with warning | Phase 21 D-02 | ✓ |
| Asset-only in form | | |
| Hide path after asset selected | | |

**User's choice:** Q4-1

---

## Claude's Discretion

Package versions, Vite lazy split, preview route naming, whether to surface contentType from PO, basemap style, E2E vs unit evidence bar.

## Deferred Ideas

- Batch delete
- Server geometry simplify API
- Phase 23 docs + optional P1 geo-assets row
- GEO-06 polygons
