# Geospatial Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver synthetic geospatial row generation via `iterator.type: GEO` for **V1 and V2** templates, plus minimal SpEL (`#{geo.pointsInBoundary(...)}`), backed by `data-generator-geo`.

**Architecture:** Extract GeoJSON/JTS code into `data-generator-geo`; add `data-generator-iterator-geo` (`GeoIteratorVO` + `GeoIterator` → `MapValue`); extend calcite `IteratorRowSource`; register `GeoVariable` in faker. No QuerySource (phase D).

**Tech Stack:** Java 25, Maven, `jts-core` (centralized **1.19.0**), Jackson 3, `@AutoService` + `@JsonSubType("GEO")`, `AbstractIterator`, `IteratorRowSource`.

**Spec:** `docs/superpowers/specs/2026-05-20-geospatial-phase1-design.md` (revised 2026-05-20)

**Conventions:** New `.java` files follow repository copyright + Javadoc rules (`.cursor/rules/java-copyright-class-javadoc.mdc`).

---

## File map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `data-generator-geo/pom.xml` | `jts-core`, Jackson 3 |
| Create | `org/gensokyo/data/geo/**` | io, generate, format, `GeoGenerationRequest` |
| Create | `data-generator-iterator-geo/**` | `GeoIteratorVO`, `GeoIterator` |
| Modify | root `pom.xml` | `<module>data-generator-geo</module>`, `jts-core` in `dependencyManagement` |
| Modify | `data-generator-dependencies/pom.xml` | Managed coords: `data-generator-geo`, `data-generator-iterator-geo` |
| Modify | `data-generator-iterator/pom.xml` | Add `iterator-geo` submodule |
| Modify | `data-generator-faker/pom.xml` | Depend on `data-generator-geo`; remove local `jts-core` version pin |
| Modify | `data-generator-service/pom.xml` | Depend on `iterator-geo` |
| Modify | `data-generator-calcite/pom.xml` | Depend on `iterator-geo` |
| Modify | `IteratorRowSource.java` | `GeoIteratorVO` materialization + schema |
| Create | `GeoVariable.java`, `GeoSpelFunctions.java` | SpEL namespace `geo` |
| Create | `docs/geospatial-phase1-usage.md` | V1/V2 YAML + SpEL |
| Test resources | `data-generator-geo/src/test/resources/geo/*.geojson` | Classpath fixtures |

---

### Task 1: Maven modules and BOM

**Files:**
- Create: `data-generator-geo/pom.xml`
- Modify: root `pom.xml`, `data-generator-dependencies/pom.xml`

- [x] **Step 1:** Add `data-generator-geo` module; `dependencyManagement` entry for `org.locationtech.jts:jts-core:1.19.0`.
- [x] **Step 2:** Register `data-generator-geo` and (placeholder) `data-generator-iterator-geo` artifacts in `data-generator-dependencies`.
- [x] **Step 3:** `.\mvnw-jdk25.ps1 -pl data-generator-geo install -DskipTests` — SUCCESS.

---

### Task 2: Move GeoJSON loader + resource resolver

**Files:**
- Create: `org/gensokyo/data/geo/io/GeoJsonLoader.java`, `GeoResourceResolver.java`
- Modify: `data-generator-faker` — remove/move old loader; delegate `GeoKit`

- [x] **Step 1:** Implement `GeoResourceResolver` (`classpath:`, file).
- [x] **Step 2:** Move `GeoJsonLoader` to geo module; unit tests (Point, Polygon, FeatureCollection).
- [x] **Step 3:** Faker `GeoKit` delegates to geo module.
- [x] **Step 4:** Fix `GeoKitTests` to use `classpath:geo/南沙区边界.geojson` (remove Windows absolute path).
- [x] **Step 5:** `.\mvnw-jdk25.ps1 -pl data-generator-faker,data-generator-geo -am test -Dtest=GeoKitTests,GeoJsonLoaderTests` — pass.

---

### Task 3: Boundary point generator + normalizer

**Files:**
- Create: `BoundaryPointGenerator.java`, `BoundaryGeometryNormalizer.java`, tests

- [x] **Step 1:** Move/refactor `RandomPointGenerator` → `BoundaryPointGenerator`.
- [x] **Step 2:** Implement normalizer (Polygon/MultiPolygon; GeometryCollection → union of polygons).
- [x] **Step 3:** Unit test: N points inside 南沙 boundary; optional min-distance smoke.

---

### Task 4: Line component selector + line sampler

**Files:**
- Create: `LineComponentSelector.java`, `LineSampleGenerator.java`, tests

- [x] **Step 1:** Test longest LineString in MultiLineString.
- [x] **Step 2:** `BY_COUNT` — exactly `count` points; `BY_SPACING_METERS` — ignore `count`, use `spacingMeters`.
- [x] **Step 3:** Test against `classpath:geo/南沙区道路路网.geojson`.

---

### Task 5: Output formatters

**Files:**
- Create: `GeoOutputFormat.java`, `GeoValueFormatter.java`, `GeoGenerationRequest.java`, tests

- [x] **Step 1:** `COLUMNS` / `GEOJSON` / `WKT` + optional `columnNames`.
- [x] **Step 2:** `includeProperties` → `prop.<key>` with collision check.

---

### Task 6: Iterator module `data-generator-iterator-geo`

**Files:**
- Create: `data-generator-iterator-geo/pom.xml`
- Create: `GeoIteratorVO.java`, `GeoIterator.java`
- Modify: `data-generator-iterator/pom.xml`, `data-generator-service/pom.xml`

- [x] **Step 1:** Scaffold module (mirror `iterator-constant`: core + auto-service + lombok).
- [x] **Step 2:** `GeoIteratorVO` — `@AutoService(IteratorVO.class)`, `@JsonSubType("GEO")`, fields per spec.
- [x] **Step 3:** `GeoIterator` — validate config; precompute points; `next()` → **`MapValue.fromMap(formatter.format(point))`**.
- [x] **Step 4:** Test YAML/JSON deserialize `type: GEO` → `GeoIteratorVO`.
- [x] **Step 5:** Integration tests: `BOUNDARY_POINTS`+`columns` (row count); `LINE_SAMPLE`+`wkt`.
- [x] **Step 6:** Add `data-generator-iterator-geo` to service `pom.xml`.

---

### Task 7: Template V2 — `IteratorRowSource`

**Files:**
- Modify: `data-generator-calcite/.../IteratorRowSource.java`
- Modify: `data-generator-calcite/pom.xml`
- Create: `IteratorRowSourceGeoTests.java` (or under calcite test tree)

- [x] **Step 1:** Add calcite dependency on `data-generator-iterator-geo`.
- [x] **Step 2:** Add `case GeoIteratorVO` — materialize rows (reuse geo formatters or delegate to shared materializer in geo module).
- [x] **Step 3:** Build `RowSchema`: `columns` → lat/lon/(alt); `geojson`/`wkt` → `geometry` VARCHAR.
- [x] **Step 4:** Test: V2 iterator source with `count: 10`, `SELECT lat, lon FROM geo_input` row count 10.

---

### Task 8: SpEL — `GeoVariable`

**Files:**
- Create: `GeoVariable.java`, `GeoSpelFunctions.java`, `GeoSpelFunctionsTest.java`
- Modify: `DataFakerConfig` or new `GeoConfig` — register `Variable` bean

- [x] **Step 1:** `GeoVariable` with `name() = "geo"` (constant in `Const` if desired).
- [x] **Step 2:** `pointsInBoundary` / `randomPointInBoundary` delegate to geo-core.
- [x] **Step 3:** SpEL test: `#{geo.pointsInBoundary('classpath:geo/...', 5, 0, 1L)}` size 5.

---

### Task 9: Documentation + full verification

**Files:**
- Create: `docs/geospatial-phase1-usage.md`

- [x] **Step 1:** Document V1 iterator YAML, V2 `ITERATOR` source YAML, SpEL examples, limitations section.
- [x] **Step 2:** `.\mvnw-jdk25.ps1 test` — BUILD SUCCESS.
- [x] **Step 3:** Commit: `feat(geo): add phase1 synthetic geospatial iterator, v2 source, and spel`.

---

## Dependency note

New top-level module `data-generator-geo` requires root + `data-generator-dependencies` updates (see `AGENTS.md`). Align with team if outside `feature-4.0` scope.

## Out of plan (explicit)

- PostGIS / file RowSource (phase B)
- Calcite GeoJSON QuerySource (phase D)
- GeoTools dependency
- `faker.geo()` rich API / trajectory in SpEL
- Vaadin operator UI

## Revision history

| Date | Change |
|------|--------|
| 2026-05-20 | Initial plan |
| 2026-05-20 | Added Task 7 (V2), `GEO` subtype, `MapValue`, `GeoVariable`, BOM, calcite dep, classpath tests |
| 2026-05-20 | Phase 1 shipped on `feature-4.0`; plan checkboxes marked complete |
