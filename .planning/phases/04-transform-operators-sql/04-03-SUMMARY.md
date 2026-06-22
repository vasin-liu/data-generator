---
phase: 04-transform-operators-sql
plan: 03
subsystem: service
tags: [console-api, transform-catalog, udf, discovery, R-envelope]

# Dependency graph
requires:
  - phase: 04-transform-operators-sql
    provides: json/mask/lookup operators registered as runtime capabilities (04-02)
  - phase: 03-udf-console-template-binding
    provides: UdfRegistry/UdfRegistryService + published-UDF lifecycle
provides:
  - GET /api/console/transforms unified catalog endpoint (XFORM-01, D-05)
  - TransformCatalogEntryView / TransformCatalogParam DTO records (D-06, D-07)
  - BuiltinTransformCatalog authored operator descriptors
  - TransformCatalogSource merging built-ins + published UDFs (D-06)
affects: [04-05]

# Tech tracking
tech-stack:
  added: []
  patterns: [console R<T> read endpoint mirroring ConsoleUdfController, payload-free catalog projection]

key-files:
  created:
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/TransformCatalogParam.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/TransformCatalogEntryView.java
    - data-generator-service/src/main/java/org/gensokyo/data/udf/BuiltinTransformCatalog.java
    - data-generator-service/src/main/java/org/gensokyo/data/udf/TransformCatalogSource.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleTransformCatalogController.java
    - data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleTransformCatalogControllerTest.java
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java

key-decisions:
  - "TransformCatalogSource injects UdfRegistryService (Spring facade) for clean slice-test mocking"
  - "UDF sqlName resolved by parsing the ScriptUdfPayload for SQL/SCRIPT; null for java-plugin"
  - "Unknown kind filter throws IllegalArgumentException -> ConsoleApiAdvice 400"
  - "Internal V2_JSON_EXTRACT never added to the catalog (D-12)"

patterns-established:
  - "Catalog read endpoints follow ConsoleUdfController @RestController + R<T>, no controller try/catch"

requirements-completed: [XFORM-01]

# Metrics
duration: 14 min
completed: 2026-06-22
---

# Phase 4 Plan 03: Unified Transform Catalog API Summary

**`GET /api/console/transforms` returns one `R<List<TransformCatalogEntryView>>` listing built-in operators (json/mask/lookup + sql/spel/js) with rich authoring metadata and published UDFs, excluding drafts and internal scalar functions — 3 slice tests green.**

## Performance

- **Duration:** ~14 min
- **Completed:** 2026-06-22T20:45:00+08:00
- **Tasks:** 3
- **Files modified:** 7 (6 created, 1 modified)

## Accomplishments
- `TransformCatalogEntryView` + `TransformCatalogParam` immutable records — discovery metadata only, no payload bytes (D-06, D-07).
- `BuiltinTransformCatalog.entries()` — authored descriptors for json/mask/lookup (param schema matching the VOs + YAML example) plus sql/spel/js; excludes V2_JSON_EXTRACT (D-12).
- `TransformCatalogSource.entries(kindFilter)` — merges built-ins with PUBLISHED UDFs (kind UDF, sqlName from payload), filters by kind, drops drafts (D-06).
- `ConsoleTransformCatalogController` — `GET /api/console/transforms` R<T> endpoint, no try/catch; unknown kind -> 400 (D-05).
- `CoreConfig` — additive `transformCatalogSource` bean mirroring `registrySqlFunctionSource`.

## Task Commits

1. **Tasks 1-3 implementation (DTOs + catalog + source + controller + bean)** - `a6df0e2` (feat)
2. **Task 3 test (catalog controller slice)** - `c19e5ce` (test)

## Files Created/Modified
- `TransformCatalogParam.java` / `TransformCatalogEntryView.java` - catalog DTO records
- `BuiltinTransformCatalog.java` - authored built-in descriptors
- `TransformCatalogSource.java` - built-in + published-UDF merge
- `ConsoleTransformCatalogController.java` - GET /api/console/transforms
- `CoreConfig.java` - transformCatalogSource bean
- `ConsoleTransformCatalogControllerTest.java` - slice test

## Decisions Made
- Injected `UdfRegistryService` rather than the raw `UdfRegistry` (plan granted discretion) so the slice test mocks the same facade as `ConsoleUdfControllerTest`.

## Deviations from Plan
None functionally. Constructor arg uses `UdfRegistryService` (facade) per the plan's "adjust constructor args to the final design" note.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Catalog endpoint ready for 04-05 documentation/fixtures; error surfacing (04-04) is independent.

---
*Phase: 04-transform-operators-sql*
*Completed: 2026-06-22*
