---
phase: 03-udf-console-template-binding
plan: 04
subsystem: api
tags: [template, udf, validation, publish-gate, calcite, spring-boot]

requires:
  - phase: 02-udf-platform-core
    provides: D-27 typed reference contract, UdfRegistryException codes, sqlName metadata
  - phase: 03-udf-console-template-binding
    provides: JDBC-backed UdfRegistry (plan 01) resolved through UdfRegistryService
provides:
  - publish-time Template V2 UDF reference validation (unknown/unpublished/deprecated hard-fail)
  - reusable static reference extractor on TemplateV2Validator
affects: [03-02, 03-05]

tech-stack:
  added: []
  patterns:
    - "Static reference extraction helper additive to TemplateV2Validator, mirroring its transform traversal"
    - "Built-in SQL function allow-list bounds registry lookups; every non-built-in function token is a candidate sqlName"

key-files:
  created:
    - data-generator-service/src/main/java/org/gensokyo/data/template/UdfReferenceValidator.java
    - data-generator-service/src/test/java/org/gensokyo/data/template/UdfReferenceValidatorTests.java
    - data-generator-service/src/test/java/org/gensokyo/data/template/TemplatePublishUdfValidationTests.java
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java
    - data-generator-service/src/main/java/org/gensokyo/data/template/TemplateLifecycleService.java

key-decisions:
  - "Reused Phase 2 structured codes (UDF_NOT_FOUND/UDF_NOT_PUBLISHED/UDF_DEPRECATED) rather than inventing new ones"
  - "SQL sqlName tokens resolve only to PUBLISHED SQL-type UDFs by sqlName metadata; script udfRef resolves through the registry's own lifecycle checks"
  - "Validation wired into publish() only — draft-save paths stay lenient (D-11)"

patterns-established:
  - "Pattern 1: collect-all violations into one structured UdfRegistryException tagged with template paths (D-12)"
  - "Pattern 2: curated built-in allow-list as the false-positive control for SQL function-call detection"

requirements-completed: [UDF-06]

duration: ~25min
completed: 2026-06-18
---

# Phase 3 / Plan 04: Publish-Time UDF Reference Validation Summary

**Template publish now hard-fails on unknown/unpublished/deprecated UDF references (SQL sqlName tokens + script udfRef blocks) using the Phase 2 structured codes, while draft saves stay lenient.**

## Performance

- **Duration:** ~25 min
- **Tasks:** 2 (reference extractor + validator, publish-gate wiring)
- **Files modified:** 5 (3 created, 2 modified)

## Accomplishments
- `TemplateV2Validator.collectUdfReferences(...)` extracts SQL `sqlName` function-call tokens (minus a built-in allow-list) and script `udfRef:{id,version?}` blocks, mirroring the existing transform traversal (linear + compute-block + transform-graph).
- `UdfReferenceValidator` (`@Component`) resolves each reference against `UdfRegistryService`, aggregating failures into one structured `UdfRegistryException` tagged with the offending template path.
- `TemplateLifecycleService.publish(Long)` invokes the validator after structural + governance checks and before persisting the PUBLISHED status; no draft-save path calls it.

## Files Created/Modified
- `TemplateV2Validator.java` - added `UdfReference`/`UdfReferenceKind`, built-in allow-list, extraction patterns, `collectUdfReferences`.
- `UdfReferenceValidator.java` - publish-time resolver raising the Phase 2 codes.
- `TemplateLifecycleService.java` - injected validator, publish-only call.
- `UdfReferenceValidatorTests.java` - 7 unit cases (published/unknown/draft/deprecated/script + sql + built-in skip).
- `TemplatePublishUdfValidationTests.java` - 3 `@SpringBootTest` cases (publish unknown fails, publish known succeeds, draft save lenient).

## Decisions Made
- See key-decisions in frontmatter. Built-in allow-list covers standard SQL/Calcite functions and keyword-before-paren tokens to bound false positives.

## Deviations from Plan
None - plan executed as written.

## Issues Encountered
None.

## Next Phase Readiness
- Publish-time template↔UDF binding is enforced; the console REST API (03-02) surfaces these `UdfRegistryException`s as structured 400s.

---
*Phase: 03-udf-console-template-binding*
*Completed: 2026-06-18*
