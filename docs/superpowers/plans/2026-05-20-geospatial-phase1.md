# Geospatial Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver synthetic geospatial row generation via `iterator.type: geo` (boundary points + line sampling) and minimal SpEL, backed by a shared `data-generator-geo` library.

**Architecture:** Extract/move GeoJSON + JTS generation from `data-generator-faker` into new `data-generator-geo`; add `data-generator-iterator-geo` with `GeoIteratorVO` / `GeoIterator`; thin SpEL bridge in faker. No Calcite QuerySource in this plan.

**Tech Stack:** Java 25, Maven, JTS (`jts-core`), Jackson 3, `@AutoService` iterator discovery, existing `AbstractIterator` / `IteratorVO` patterns.

**Spec:** `docs/superpowers/specs/2026-05-20-geospatial-phase1-design.md`

---

## File map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `data-generator-geo/pom.xml` | Module; depends on `jts-core`, Jackson 3 |
| Create | `data-generator-geo/src/main/java/.../geo/io/*` | GeoJsonLoader, GeoResourceResolver |
| Create | `data-generator-geo/src/main/java/.../geo/generate/*` | Boundary + line sampling |
| Create | `data-generator-geo/src/main/java/.../geo/format/*` | Output formatters |
| Create | `data-generator-iterator-geo/pom.xml` | Depends on geo + core |
| Create | `.../iterator/GeoIteratorVO.java` | `@AutoService(IteratorVO)` |
| Create | `.../iterator/GeoIterator.java` | `AbstractIterator` impl |
| Modify | `data-generator-iterator/pom.xml` | Add module |
| Modify | `data-generator/pom.xml` (root) | Add `data-generator-geo` module |
| Modify | `data-generator-faker/pom.xml` | Depend on geo; delegate GeoKit |
| Modify | `data-generator-faker/.../GeoKit.java` | Delegate to geo-core |
| Modify | `data-generator-service/pom.xml` | Depend on `iterator-geo` |
| Create | `docs/geospatial-phase1-usage.md` | Operator YAML + SpEL examples |
| Test resources | `data-generator-geo/src/test/resources/*.geojson` | Copy from faker test resources |

---

### Task 1: Maven module `data-generator-geo`

**Files:**
- Create: `data-generator-geo/pom.xml`
- Modify: root `pom.xml` (`<modules>`, `dependencyManagement` for geo artifact if needed)

- [ ] **Step 1:** Add parent module `data-generator-geo` with `jts-core` + `jackson-databind` (3.x, align with faker).
- [ ] **Step 2:** `.\mvnw-jdk25.ps1 -pl data-generator-geo install -DskipTests` — SUCCESS.

---

### Task 2: Move GeoJSON loader + resource resolver

**Files:**
- Create: `data-generator-geo/src/main/java/org/gensokyo/data/geo/io/GeoJsonLoader.java`
- Create: `data-generator-geo/src/main/java/org/gensokyo/data/geo/io/GeoResourceResolver.java`
- Modify: `data-generator-faker/.../GeoJsonLoader.java` — delegate or delete after move

- [ ] **Step 1:** Copy `GeoJsonLoader` into geo module (update package); add `GeoResourceResolver` supporting `classpath:` and file paths.
- [ ] **Step 2:** Write `GeoJsonLoaderTests` (point, polygon, feature collection) — green.
- [ ] **Step 3:** Point faker `GeoKit` at geo module loader; remove duplicate class from faker.
- [ ] **Step 4:** `.\mvnw-jdk25.ps1 -pl data-generator-faker -am test -Dtest=GeoKitTests` — pass.

---

### Task 3: Boundary point generator

**Files:**
- Create: `.../geo/generate/BoundaryPointGenerator.java` (from `RandomPointGenerator`)
- Create: `.../geo/generate/BoundaryPointGeneratorTest.java`

- [ ] **Step 1:** Move/refactor `RandomPointGenerator` → `BoundaryPointGenerator` in geo module.
- [ ] **Step 2:** Unit test: N points inside 南沙 boundary fixture, min distance respected (smoke).
- [ ] **Step 3:** Faker `GeoKit.generateRandomPointsFromGeoJson` delegates to `BoundaryPointGenerator`.

---

### Task 4: Line component selector + line sampler

**Files:**
- Create: `.../geo/generate/LineComponentSelector.java`
- Create: `.../geo/generate/LineSampleGenerator.java`
- Create: `.../geo/generate/LineSampleGeneratorTest.java`

- [ ] **Step 1:** Test `LineComponentSelector` picks longest line in MultiLineString.
- [ ] **Step 2:** Implement `LineSampleGenerator` with `BY_COUNT` and `BY_SPACING_METERS` (haversine length).
- [ ] **Step 3:** Test against `南沙区道路路网.geojson` — count > 0, points on line (distance-to-line < small epsilon).

---

### Task 5: Output formatters

**Files:**
- Create: `.../geo/format/GeoOutputFormat.java`
- Create: `.../geo/format/GeoValueFormatter.java`
- Create: `GeoValueFormatterTest.java`

- [ ] **Step 1:** Test `COLUMNS` → `lat`/`lon` keys.
- [ ] **Step 2:** Test `GEOJSON` / `WKT` single `geometry` column.
- [ ] **Step 3:** Optional custom `columnNames` mapping.

---

### Task 6: Iterator module `data-generator-iterator-geo`

**Files:**
- Create: `data-generator-iterator-geo/pom.xml`
- Create: `GeoIteratorVO.java`, `GeoIterator.java`, `GeoIteratorConfig.java` (if needed)
- Modify: `data-generator-iterator/pom.xml`

- [ ] **Step 1:** Scaffold module mirroring `iterator-constant` (core + auto-service).
- [ ] **Step 2:** `GeoIteratorVO` fields match spec YAML (`mode`, paths, `output`, `sample`, etc.).
- [ ] **Step 3:** `GeoIterator` implements `hasNext`/`next` producing `Map` rows via formatters.
- [ ] **Step 4:** Integration test: `BOUNDARY_POINTS` + `columns` — row count == `count`.
- [ ] **Step 5:** Integration test: `LINE_SAMPLE` + `wkt` — geometry column present.
- [ ] **Step 6:** Add dependency to `data-generator-service/pom.xml`.

---

### Task 7: SpEL minimal functions

**Files:**
- Create: `data-generator-faker/.../geo/GeoSpelFunctions.java` (or extend existing SpEL config)
- Create: `GeoSpelFunctionsTest.java`

- [ ] **Step 1:** Register `geo.pointsInBoundary` → `List<Map<String,Object>>`.
- [ ] **Step 2:** Register `geo.randomPointInBoundary` → single map.
- [ ] **Step 3:** Test via existing SpEL test harness (mirror `SpelTests` pattern).

---

### Task 8: Documentation + full verification

**Files:**
- Create: `docs/geospatial-phase1-usage.md`
- Modify: `README.md` (one-line link, optional)

- [ ] **Step 1:** Document YAML examples for both modes and three formats.
- [ ] **Step 2:** `.\mvnw-jdk25.ps1 test` — BUILD SUCCESS.
- [ ] **Step 3:** Commit: `feat(geo): add phase1 synthetic geospatial iterator and spel`.

---

## Dependency note

Adding a **new top-level module** (`data-generator-geo`) touches root `pom.xml`. Confirm with team policy if required; spec assumes approval as part of feature-4.0 branch work.

## Out of plan (explicit)

- PostGIS reader/writer
- Calcite `GeoRowSource` / QuerySource
- GeoTools dependency
- Vaadin UI for geo templates
