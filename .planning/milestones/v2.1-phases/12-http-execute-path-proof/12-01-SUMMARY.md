---
phase: 12-http-execute-path-proof
plan: 01
subsystem: testing
tags: [mockmvc, task-run, managed-jdbc, catalog, h2, surefire, exec-01]

requires:
  - phase: 11-closeout-hardening
    provides: ManagedJdbcCatalogSinkE2eIT in-process managed-catalog sink baseline
provides:
  - ManagedJdbcCatalogHttpExecuteIT proving EXEC-01 via MockMvc POST /task/run
  - Publish-gate override + poll helpers (instanceId parse, FAILED/CANCELLED fail-fast)
affects: [12-02-postgres-upsert-http, 17-harness-p1]

tech-stack:
  added: []
  patterns:
    - "Compose Phase 11 managed seed+COUNT with RunReportPersistenceTests poll + MockMvc /task/run"
    - "IT-local require-published-for-task-run=true; leave application-phase7-test.yaml default false"

key-files:
  created:
    - data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpExecuteIT.java
  modified: []

key-decisions:
  - "Primary evidence is MockMvc POST /task/run/{id}, not TemplateV2Runner.run and not console run API"
  - "Use inline_rows YAML + TemplateLifecycleService.publish under IT gate override"
  - "Distinct H2 mem URL/table from Phase 11 to avoid same-JVM clash; leave Phase 11 IT untouched"

patterns-established:
  - "HTTP execute proof: save managed DS → DDL → TemplatePO YAML → publish → MockMvc enqueue → parse instanceId= → poll SUCCESS → COUNT(*)"
  - "Poll ~50s (250×200ms) with immediate fail on FAILED and CANCELLED"

requirements-completed: [EXEC-01]

coverage:
  - id: D1
    description: "Managed JDBC catalog sink through MockMvc POST /task/run reaches SUCCESS with countable rows"
    requirement: EXEC-01
    verification:
      - kind: integration
        ref: "data-generator-service/.../ManagedJdbcCatalogHttpExecuteIT.java#httpTaskRun_managedCatalogSink_reachesSuccessWithCountableRows"
        status: pass
      - kind: integration
        ref: "mvnw-jdk25.ps1 -pl data-generator-service -am -Dtest=ManagedJdbcCatalogHttpExecuteIT,ManagedJdbcCatalogSinkE2eIT test"
        status: pass
    human_judgment: false
  - id: D2
    description: "Phase 11 ManagedJdbcCatalogSinkE2eIT remains green and unmodified as in-process regression"
    requirement: EXEC-01
    verification:
      - kind: integration
        ref: "data-generator-service/.../ManagedJdbcCatalogSinkE2eIT.java (Tests run: 1, Failures: 0)"
        status: pass
    human_judgment: false

duration: 41min
completed: 2026-07-25
status: complete
---

# Phase 12 Plan 01: HTTP Managed-Catalog Execute Proof Summary

**MockMvc `POST /task/run/{id}` proves managed JDBC `dataSourceId` sink reaches SUCCESS with managed-pool COUNT(*) under publish gate (EXEC-01)**

## Performance

- **Duration:** 41 min
- **Started:** 2026-07-25T10:40:40Z
- **Completed:** 2026-07-25T11:21:49Z
- **Tasks:** 2
- **Files modified:** 1 created

## Accomplishments

- Scaffolded `ManagedJdbcCatalogHttpExecuteIT` with publish-gate override, `instanceId=` parse, 30–60s fail-fast poll (FAILED+CANCELLED), and managed-pool `countRows`
- Implemented happy path: managed DS save → DDL → published V2 YAML template → MockMvc `/task/run` → SUCCESS → COUNT(*) ≥ 2
- Left `ManagedJdbcCatalogSinkE2eIT` and `application-phase7-test.yaml` defaults untouched; Phase 11 regression still green

## Task Commits

Each task was committed atomically:

1. **Task 1: Scaffold bootstrap, seed, publish, and poll helpers** - `f3cdcc2` (test)
2. **Task 2: HTTP managed-catalog happy path** - `26b72d2` (test)

**Plan metadata:** (this commit)

## Files Created/Modified

- `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpExecuteIT.java` - EXEC-01 HTTP spine IT

## Decisions Made

- Evidence endpoint: MockMvc `POST /task/run/{id}` only (D-01); no console run primary path
- Publish via `TemplateLifecycleService.publish` with IT property `require-published-for-task-run=true` (D-02); yaml default stays false
- Source type `inline_rows` (production VO/parser), not the PATTERNS sketch `inline` alias
- No `TemplateV2Runner` autowire; no snap-key asserts (D-11)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for Plan 02 (`12-02`) EXEC-02 PostgreSQL Testcontainers upsert on the same HTTP spine
- Do not promote this IT to P0 harness until Phase 17

## Self-Check: PASSED

- [x] `12-01-SUMMARY.md` present
- [x] Commits grep `12-01` ≥ 2 task commits
- [x] Acceptance criteria Task 1–2 verified; Surefire BUILD SUCCESS (2 tests, 0 failures)
- [x] `ManagedJdbcCatalogSinkE2eIT` unmodified by this plan

---
*Phase: 12-http-execute-path-proof*
*Completed: 2026-07-25*
