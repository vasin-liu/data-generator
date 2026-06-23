---
phase: 03-udf-console-template-binding
plan: 02
subsystem: api
tags: [rest, spring-mvc, console, udf, rbac, multipart, dto]

requires:
  - phase: 03-udf-console-template-binding
    provides: JDBC-backed UdfRegistry, UdfRegistryService, UdfPublishService
  - phase: 02-udf-platform-core
    provides: UdfRecord, UdfType, UdfLifecycleState, UdfRegistryException, UdfValidationError
provides:
  - "/api/console/udfs REST surface: multipart upload→draft, publish, deprecate, grouped list, version history"
  - Payload-free console DTOs (UdfVersionView, UdfGroupView)
  - UdfRegistryException→HTTP 400 mapping with structured code + field violations
  - UDF RBAC branch (operator-grade mutations, viewer-grade reads)
affects: [03-03, 03-05]

tech-stack:
  added: []
  patterns:
    - "DTO static factory from(UdfRecord) drops payload bytes so list/history never leak code-bearing artifacts (D-14)"
    - "Lifecycle transitions route through UdfPublishService (governance+audit+runtime refresh), never the registry directly"
    - "RBAC branch ordered before the generic /publish branch so /udfs/.../publish is classified by path prefix"

key-files:
  created:
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/UdfVersionView.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/UdfGroupView.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleUdfController.java
    - data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleUdfControllerTest.java
    - data-generator-service/src/test/java/org/gensokyo/data/security/ConsoleUdfAuthorizationFilterTest.java
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleApiAdvice.java
    - data-generator-service/src/main/java/org/gensokyo/data/security/ConsoleAuthorizationFilter.java

key-decisions:
  - "Added a dedicated @ExceptionHandler(UdfRegistryException) returning R.fail(message, payload) with a nested UdfErrorPayload(code, violations) — chosen over translating to IllegalArgumentException so structured codes/fields survive to the client (D-12)"
  - "RBAC reuses TEMPLATE_PUBLISH (operator-grade) for all UDF POST mutations and TEMPLATE_READ (viewer-grade) for GET — no new ConsolePermission introduced"
  - "Filter test asserts the private static requiredPermission via reflection (direct classification assertion) rather than driving doFilterInternal through roles"

patterns-established:
  - "Pattern 1: console DTO records expose only lifecycle metadata + timestamps + metadata map, never payload() bytes (D-14)"
  - "Pattern 2: controllers stay try/catch-free; UdfRegistryException + IllegalArgumentException bubble to ConsoleApiAdvice"

requirements-completed: [UDF-05]

duration: ~25min
completed: 2026-06-18
---

# Phase 3 / Plan 02: Console UDF REST API + RBAC + DTOs Summary

**The `/api/console/udfs` operator surface — unified multipart upload→draft plus publish/deprecate/list/version-history over the `R<T>` envelope, with structured `UdfRegistryException` error mapping and RBAC gating on mutations.**

## Performance

- **Duration:** ~25 min (single multi-module Maven verification cycle dominated wall-clock)
- **Tasks:** DTOs, controller, advice handler + RBAC branch, two test classes
- **Files modified:** 7 (5 created, 2 modified)

## Accomplishments
- `ConsoleUdfController` exposes `POST /api/console/udfs` (multipart: JAR file part for java-plugin, `scriptBody`/`sql` text fields for script/sql, `sqlName` captured into metadata), `POST /{udfId}/{version}/publish`, `POST /{udfId}/{version}/deprecate`, `GET /api/console/udfs` (grouped by udfId), and `GET /api/console/udfs/{udfId}` (version history) — all returning `R<...>`.
- Publish/deprecate route through `UdfPublishService` so the Phase 2 governance gate, audit log, and runtime refresh all run; upload creates a DRAFT only (governance defers to publish, D-07).
- `UdfVersionView`/`UdfGroupView` records project `UdfRecord` into wire shapes without ever mapping `payload()` bytes (D-14).
- `ConsoleApiAdvice` maps `UdfRegistryException` → HTTP 400 with a `UdfErrorPayload(code, violations)` body so the console can render `UDF_NOT_FOUND` / `UDF_NOT_PUBLISHED` / `UDF_DEPRECATED` / `UDF_GOVERNANCE_VIOLATION` with field detail (D-12).
- `ConsoleAuthorizationFilter` gains a `/api/console/udfs` branch placed before the generic `/publish` branch: POST → `TEMPLATE_PUBLISH` (operator-grade), GET → `TEMPLATE_READ` (viewer-grade) (D-15).

## Files Created/Modified
- `UdfVersionView.java` - per-version record + `from(UdfRecord)` factory (no payload).
- `UdfGroupView.java` - udfId-grouped record + `of(String, List<UdfRecord>)` factory (versions sorted).
- `ConsoleUdfController.java` - REST surface at `/api/console/udfs`.
- `ConsoleApiAdvice.java` - new `@ExceptionHandler(UdfRegistryException)` + nested `UdfErrorPayload`.
- `ConsoleAuthorizationFilter.java` - UDF RBAC branch before the generic publish branch.
- `ConsoleUdfControllerTest.java`, `ConsoleUdfAuthorizationFilterTest.java` - 7 tests.

## Decisions Made
- See key-decisions in frontmatter. The notable one: a dedicated exception handler with a structured `UdfErrorPayload` preserves the registry's stable codes and field-level violations end-to-end (D-12), rather than flattening them into a plain message.

## Deviations from Plan
None — implemented as specified. The RBAC test uses reflection on `requiredPermission` (one of the two access options the plan explicitly allowed) for a direct classification assertion.

## Issues Encountered
None. All 7 new tests passed on the first verification run (`Tests run: 7, Failures: 0, Errors: 0`); full reactor `BUILD SUCCESS`.

## Next Phase Readiness
- The REST surface and payload-free DTOs are ready for the console UDFs React page (03-03) and the sample-UDF E2E harness (03-05).

---
*Phase: 03-udf-console-template-binding*
*Completed: 2026-06-18*
