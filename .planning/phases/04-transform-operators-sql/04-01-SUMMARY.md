---
phase: 04-transform-operators-sql
plan: 01
subsystem: api
tags: [template-v2, transform, jackson, polymorphism, autoservice, json, mask, lookup]

# Dependency graph
requires:
  - phase: 02-udf-platform-core
    provides: TransformVO polymorphic subtype model + JsonSubtypeRegistry AutoService discovery
provides:
  - JsonTransformVO (type json) with sourceColumn/targetColumn/flatten/separator (D-02)
  - MaskTransformVO (type mask) + MaskRuleVO list element with named strategies (D-03)
  - LookupTransformVO (type lookup) with source/leftKey/rightKey/columns (D-04)
  - OperatorTransformSubtypeTests proving additive subtype resolution (D-13)
affects: [04-02, 04-03, 04-04, 04-05]

# Tech tracking
tech-stack:
  added: [junit-jupiter test scope in data-generator-core]
  patterns: [polymorphic TransformVO subtype via AutoService + JsonSubType discriminator]

key-files:
  created:
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/JsonTransformVO.java
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/MaskTransformVO.java
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/MaskRuleVO.java
    - data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/LookupTransformVO.java
    - data-generator-common/data-generator-core/src/test/java/org/gensokyo/data/model/v2/OperatorTransformSubtypeTests.java
  modified:
    - data-generator-common/data-generator-core/pom.xml

key-decisions:
  - "JSON operator field set models both parse-only (targetColumn) and parse+flatten (flatten/separator) in one VO"
  - "MaskRuleVO carries a closed strategy name set (email/phone/credit-card/generic-fixed); custom masking stays in UDFs"
  - "LookupTransformVO references an in-template named source, never a JDBC datasource (D-04)"

patterns-established:
  - "New operators plug in via @AutoService(TransformVO.class) + @JsonSubType(...) with no central-file edit"

requirements-completed: [XFORM-02]

# Metrics
duration: 8 min
completed: 2026-06-22
---

# Phase 4 Plan 01: Transform Operator VOs Summary

**Three additive Template V2 operator config VOs — `json` parse/flatten, `mask` named-strategy redaction, `lookup` in-template join — plus a subtype round-trip test proving legacy `sql` still binds.**

## Performance

- **Duration:** ~8 min
- **Completed:** 2026-06-22T20:02:32+08:00
- **Tasks:** 4
- **Files modified:** 6 (5 created, 1 modified)

## Accomplishments
- `JsonTransformVO` (`type: json`) with `sourceColumn`/`targetColumn`/`flatten`/`separator` (D-02)
- `MaskTransformVO` (`type: mask`) holding `List<MaskRuleVO>`; `MaskRuleVO` carries `column` + named `strategy` (D-03)
- `LookupTransformVO` (`type: lookup`) with `source`/`leftKey`/`rightKey`/`columns` referencing an in-template named source (D-04)
- `OperatorTransformSubtypeTests` — 5 tests green, including legacy `sql` additive proof (D-13) and lowercase/uppercase alias resolution

## Task Commits

1. **Task 1: JsonTransformVO** - `d9e078a` (feat)
2. **Task 2: MaskTransformVO + MaskRuleVO** - `d7e1e82` (feat)
3. **Task 3: LookupTransformVO** - `f1d30e8` (feat)
4. **Task 4: OperatorTransformSubtypeTests + pom** - `7e75093` (test)

## Files Created/Modified
- `JsonTransformVO.java` - json operator config VO, `@JsonSubType("JSON")`
- `MaskTransformVO.java` / `MaskRuleVO.java` - mask operator config + rule element
- `LookupTransformVO.java` - lookup operator config VO
- `OperatorTransformSubtypeTests.java` - additive subtype resolution test
- `data-generator-core/pom.xml` - added `junit-jupiter` test dependency

## Decisions Made
- Field shapes follow the planner's manifest verbatim so downstream plans (04-02..04-05) bind to exact names.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added junit-jupiter test dependency to data-generator-core**
- **Found during:** Task 4 (subtype round-trip test)
- **Issue:** `data-generator-core` had no test dependency or `src/test` tree; the plan's required JUnit 5 test could not compile.
- **Fix:** Added `org.junit.jupiter:junit-jupiter` (test scope, version from root BOM), mirroring `data-generator-calcite`.
- **Files modified:** `data-generator-common/data-generator-core/pom.xml`
- **Verification:** `OperatorTransformSubtypeTests` compiles and runs (5/5 green).
- **Committed in:** `7e75093` (Task 4 commit)

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Necessary to satisfy the plan's verification command. No scope creep.

## Issues Encountered
- First `-am` test run failed because the test filter matched no tests in the upstream `data-generator-geo` module; resolved by adding `-Dsurefire.failIfNoSpecifiedTests=false`.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- VO layer ready for 04-02 (factories + `V2_JSON_EXTRACT` SQL function).

---
*Phase: 04-transform-operators-sql*
*Completed: 2026-06-22*
