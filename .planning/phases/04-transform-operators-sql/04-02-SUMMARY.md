---
phase: 04-transform-operators-sql
plan: 02
subsystem: calcite
tags: [template-v2, transform, calcite, json, mask, lookup, sql-function]

# Dependency graph
requires:
  - phase: 04-transform-operators-sql
    provides: JsonTransformVO / MaskTransformVO+MaskRuleVO / LookupTransformVO config VOs (04-01)
provides:
  - JsonTransformFactory (parse-only + opt-in flatten, D-02)
  - MaskTransformFactory (named-strategy in-place redaction, D-03)
  - LookupTransformFactory (in-template named-source key join, D-04)
  - Internal V2_JSON_EXTRACT(json, path) Calcite scalar (D-11, D-12)
  - json/mask/lookup registered as runtime capabilities (D-01)
affects: [04-03, 04-04, 04-05]

# Tech tracking
tech-stack:
  added: []
  patterns: [V2TransformFactory mirroring SpelTransformFactory, V2_-prefixed internal SQL scalar]

key-files:
  created:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/transform/JsonTransformFactory.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/transform/MaskTransformFactory.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/transform/LookupTransformFactory.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/TemplateV2JsonSqlFunctions.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/transform/JsonTransformFactoryTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/transform/MaskTransformFactoryTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/transform/LookupTransformFactoryTests.java
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sql/TemplateV2JsonSqlFunctionTests.java
  modified:
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/TemplateV2SqlFunctionRegistry.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/plugin/DefaultTemplateV2RuntimePlugin.java

key-decisions:
  - "json operator parses pure-Java with Jackson 3 (Object.class) and is independent of V2_JSON_EXTRACT"
  - "mask preserves non-alphanumeric separators and keeps only documented remainders (last 4 / first char)"
  - "lookup builds a single-pass HashMap index; missing source/dup key/lookup miss all fail-fast"
  - "V2_JSON_EXTRACT appended to builtIn() without reordering; internal-only, never cataloged"

patterns-established:
  - "New transform operators register via descriptor() capability + transformFactories() List.of entry"

requirements-completed: [XFORM-02, XFORM-03]

# Metrics
duration: 18 min
completed: 2026-06-22
---

# Phase 4 Plan 02: Transform Operator Runtime + SQL Surface Summary

**Three built-in Template V2 operators (`json`, `mask`, `lookup`) implemented as `V2TransformFactory`s and registered in the default runtime plugin, plus the internal `V2_JSON_EXTRACT` Calcite scalar for `sql` transforms — 12 embedded tests green.**

## Performance

- **Duration:** ~18 min
- **Completed:** 2026-06-22T20:25:00+08:00
- **Tasks:** 4
- **Files modified:** 10 (8 created, 2 modified)

## Accomplishments
- `JsonTransformFactory` — per-row JSON parse into `targetColumn`, or opt-in recursive `flatten` into `parent.child` columns via `separator`; parse failure throws a located, bounded-snippet `IllegalArgumentException` (D-02, D-08 seed).
- `MaskTransformFactory` — in-place redaction for `email`/`phone`/`credit-card`/`generic-fixed`; output schema == input schema; unknown strategy fails fast without echoing raw PII (D-03, D-10).
- `LookupTransformFactory` — single-pass HashMap index over an in-template named source; enriches rows with projected columns; missing-source/duplicate-key/lookup-miss all fail fast (D-04, D-10).
- `TemplateV2JsonSqlFunctions.jsonExtract` + `V2_JSON_EXTRACT` registration — internal dot-path scalar, `V2_` prefix, not cataloged (D-11, D-12).
- `DefaultTemplateV2RuntimePlugin` — json/mask/lookup capabilities + all six transform factories (D-01).

## Task Commits

1. **Tasks 1-4 implementation (factories + V2_JSON_EXTRACT + plugin/registry wiring)** - `dca4880` (feat)
2. **Tasks 1-4 tests (4 embedded test classes)** - `4ba55e0` (test)

## Files Created/Modified
- `JsonTransformFactory.java` / `MaskTransformFactory.java` / `LookupTransformFactory.java` - operator runtimes
- `TemplateV2JsonSqlFunctions.java` - internal V2_JSON_EXTRACT evaluator
- `TemplateV2SqlFunctionRegistry.java` - V2_JSON_EXTRACT registration (appended)
- `DefaultTemplateV2RuntimePlugin.java` - capabilities + factory registration
- 4 test classes - operator + SQL function coverage

## Decisions Made
- Operators mirror `SpelTransformFactory` (read table `input`, `mergeSchema` LinkedHashMap idiom, lowercase row keys).
- Mask strategies documented in class Javadoc; separators preserved so masked values stay realistic.

## Deviations from Plan
None. Implementation followed the plan's manifest and field names verbatim.

## Issues Encountered
- Surefire `-Dtest=A+B+C+D` (`+` separator) silently matched no tests; re-ran with comma separators which executed all four classes (12 tests, all green).

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Runtime operators + capabilities ready for 04-03 (transform catalog API) and 04-04 (transform error surfacing).

---
*Phase: 04-transform-operators-sql*
*Completed: 2026-06-22*
