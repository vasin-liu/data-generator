---
phase: 12-http-execute-path-proof
plan: 02
subsystem: testing
tags: [mockmvc, task-run, managed-jdbc, catalog, postgres, testcontainers, upsert, on-conflict, exec-02]

requires:
  - phase: 12-http-execute-path-proof
    provides: ManagedJdbcCatalogHttpExecuteIT HTTP spine patterns (publish, MockMvc, poll, COUNT)
  - phase: 11-closeout-hardening
    provides: ManagedJdbcCatalogSinkE2eIT in-process managed-catalog sink baseline
provides:
  - ManagedJdbcCatalogHttpPostgresUpsertIT proving EXEC-02 via MockMvc POST /task/run + PG ON CONFLICT
  - Service-module DockerTestSupport + Testcontainers 1.20.6 test deps
affects: [17-harness-p1]

tech-stack:
  added: [org.testcontainers:junit-jupiter@1.20.6, org.testcontainers:postgresql@1.20.6]
  patterns:
    - "Docker-gated @EnabledIf DockerTestSupport + @Testcontainers PostgreSQLContainer postgres:16-alpine"
    - "HTTP snap pools require passwordSecretRef (never plaintext) for non-empty PG passwords"
    - "Reuse EXEC-01 publish → MockMvc → instanceId parse → poll SUCCESS → COUNT + second-run idempotency"

key-files:
  created:
    - data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpPostgresUpsertIT.java
    - data-generator-service/src/test/java/org/gensokyo/data/support/DockerTestSupport.java
  modified:
    - data-generator-service/pom.xml

key-decisions:
  - "Separate IT from EXEC-01 H2 class (D-08); Docker-gated Testcontainers PG (D-09)"
  - "Writer options dialect=postgres, upsert=true, upsertKeys=[id] for ON CONFLICT (D-10)"
  - "Register Testcontainers password via SecretService passwordSecretRef so HTTP snap materialization authenticates (snapshots forbid plaintext password)"

patterns-established:
  - "Service DockerTestSupport copy of calcite helper (test-scoped calcite class not on service classpath)"
  - "EXEC-02 evidence: managed PG catalog + upsert options + publish + MockMvc /task/run + SUCCESS + COUNT + optional second-run idempotency"

requirements-completed: [EXEC-02]

coverage:
  - id: D1
    description: "Managed catalog + PostgreSQL dialect upsert (ON CONFLICT) through MockMvc POST /task/run reaches SUCCESS with countable idempotent rows"
    requirement: EXEC-02
    verification:
      - kind: integration
        ref: "data-generator-service/.../ManagedJdbcCatalogHttpPostgresUpsertIT.java#httpTaskRun_managedPostgresUpsert_reachesSuccessWithCountableRows"
        status: pass
      - kind: integration
        ref: "mvnw-jdk25.ps1 -pl data-generator-service -am -Dtest=ManagedJdbcCatalogHttpPostgresUpsertIT,ManagedJdbcCatalogHttpExecuteIT,ManagedJdbcCatalogSinkE2eIT test"
        status: pass
    human_judgment: false
  - id: D2
    description: "Service module Testcontainers 1.20.6 test deps + DockerTestSupport gate for clean skip without Docker"
    requirement: EXEC-02
    verification:
      - kind: other
        ref: "data-generator-service/pom.xml test-scoped org.testcontainers:postgresql + junit-jupiter 1.20.6; DockerTestSupport#dockerAvailable"
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-07-25
status: complete
---

# Phase 12 Plan 02: HTTP Postgres Upsert Execute Proof Summary

**Docker-gated ManagedJdbcCatalogHttpPostgresUpsertIT proves managed dataSourceId + postgres ON CONFLICT upsert through MockMvc POST /task/run (EXEC-02)**

## Performance

- **Duration:** 45 min
- **Started:** 2026-07-25T11:38:46Z
- **Completed:** 2026-07-25T12:23:33Z
- **Tasks:** 2
- **Files modified:** 3 (2 created + pom)

## Accomplishments

- Added test-scoped Testcontainers `junit-jupiter` + `postgresql` 1.20.6 to `data-generator-service` and service-local `DockerTestSupport`
- Implemented separate Docker-gated IT: Testcontainers `postgres:16-alpine` → managed catalog save → upsert writer options → publish → MockMvc `/task/run` → SUCCESS → COUNT(*) + second-run idempotency
- Left `ManagedJdbcCatalogSinkE2eIT` and `ManagedJdbcCatalogHttpExecuteIT` unmodified; both remain green alongside EXEC-02

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Testcontainers test deps and DockerTestSupport for service module** - `a5e76dc` (chore)
2. **Task 2: Implement ManagedJdbcCatalogHttpPostgresUpsertIT (HTTP spine + ON CONFLICT)** - `c348177` (test)

**Plan metadata:** (this commit)

## Files Created/Modified

- `data-generator-service/pom.xml` - test-scoped Testcontainers 1.20.6 (postgresql + junit-jupiter)
- `data-generator-service/src/test/java/org/gensokyo/data/support/DockerTestSupport.java` - Docker availability gate for service ITs
- `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpPostgresUpsertIT.java` - EXEC-02 HTTP PG upsert IT

## Decisions Made

- Separate class from EXEC-01 (D-08); engine = Testcontainers PostgreSQL `postgres:16-alpine` (D-09)
- Upsert via YAML `options.dialect/upsert/upsertKeys` (D-10); no snap-key asserts (D-11)
- IT uses `SecretService` + `passwordSecretRef` instead of plaintext `save(..., password, null, ...)` because HTTP snap materialization only carries secret refs (ExecutionConnectionSnapshot contract)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] HTTP snap pools drop plaintext managed JDBC passwords**
- **Found during:** Task 2 (ManagedJdbcCatalogHttpPostgresUpsertIT)
- **Issue:** Plan sketched `save(..., POSTGRES.getPassword(), null, driver, ...)`. Live catalog works, but `ConnectionSnapshotSupport.jdbcParamsFromRow` only snapshots `passwordSecretRef`, so `snap:{instanceId}:{ds}` Druid pools authenticate with no password (SCRAM failure → run stuck RUNNING).
- **Fix:** Register Testcontainers password via `SecretService.upsert` and save managed DS with `passwordSecretRef` (matches snapshot/security contract; no plaintext in snapshot JSON).
- **Files modified:** `ManagedJdbcCatalogHttpPostgresUpsertIT.java`
- **Verification:** Surefire BUILD SUCCESS — UpsertIT + HttpExecuteIT + SinkE2eIT (3 tests, 0 failures) with Docker available
- **Committed in:** `c348177` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Necessary for HTTP spine with non-empty passwords; EXEC-02 evidence bar unchanged. Product gap (plaintext managed password → snap) remains for a later ownership/docs phase.

## Issues Encountered

None beyond the passwordSecretRef deviation above.

## User Setup Required

None - Docker optional (IT skips via `@EnabledIf` when unavailable).

## Next Phase Readiness

- Phase 12 DoD complete: Plan 01 (EXEC-01) + Plan 02 (EXEC-02) both summarized green
- Ready for `/gsd-verify-work 12` then next roadmap phase
- Do not promote EXEC-02 to P0 harness until Phase 17

## Self-Check: PASSED

- [x] `12-02-SUMMARY.md` present
- [x] Commits grep `12-02` ≥ 2 task commits (`a5e76dc`, `c348177`)
- [x] Acceptance criteria Task 1–2 verified; Surefire BUILD SUCCESS with Docker (3 tests, 0 failures, 0 skipped)
- [x] `ManagedJdbcCatalogSinkE2eIT` and `ManagedJdbcCatalogHttpExecuteIT` unmodified by this plan

---
*Phase: 12-http-execute-path-proof*
*Completed: 2026-07-25*
