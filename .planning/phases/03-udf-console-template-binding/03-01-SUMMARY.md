---
phase: 03-udf-console-template-binding
plan: 01
subsystem: database
tags: [jpa, hibernate, h2, udf, registry, spring-boot, persistence]

requires:
  - phase: 02-udf-platform-core
    provides: UdfRegistry interface, UdfRecord, UdfType, UdfLifecycleState, UdfRegistryException, InMemoryUdfRegistry contract
provides:
  - JDBC-backed UdfRegistry persisting versioned artifacts across restart
  - udf_artifact JPA entity + Spring Data repository
  - startup rehydration of published UDFs into the Template V2 runtime merge view
affects: [03-02, 03-04, 03-05]

tech-stack:
  added: []
  patterns:
    - "Entity DDL pinned via @Column(columnDefinition) for H2 PostgreSQL-mode compatibility"
    - "Conditional in-memory default backs off to a primary JDBC-backed registry bean"

key-files:
  created:
    - data-generator-service/src/main/java/org/gensokyo/data/model/po/UdfArtifactPO.java
    - data-generator-service/src/main/java/org/gensokyo/data/repository/UdfArtifactRepository.java
    - data-generator-service/src/main/java/org/gensokyo/data/udf/JdbcUdfRegistry.java
    - data-generator-service/src/main/java/org/gensokyo/data/udf/UdfStartupReloader.java
    - data-generator-service/src/test/java/org/gensokyo/data/udf/JdbcUdfRegistryTests.java
    - data-generator-service/src/test/java/org/gensokyo/data/udf/UdfStartupReloaderTests.java
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java
    - data-generator-service/src/main/resources/db/schema.sql

key-decisions:
  - "Replaced the ConcurrentHashMap store with repository CRUD while preserving the exact Phase 2 contract (validation, FSM, codes, latest-published resolution)"
  - "payload stored as bytea/VARBINARY, not BLOB — H2 PostgreSQL mode rejects BLOB"
  - "Startup reload is a runtime refresh (no record copy) since the registry reads straight from the DB"

patterns-established:
  - "Pattern 1: column types pinned via columnDefinition keep Hibernate create-drop DDL in lockstep with db/schema.sql under H2 MODE=PostgreSQL"
  - "Pattern 2: nullable TemplateV2RuntimeRegistryProvider injection lets minimal/test contexts skip runtime refresh safely"

requirements-completed: [UDF-04, UDF-05]

duration: ~90min
completed: 2026-06-18
---

# Phase 3 / Plan 01: UDF JDBC Persistence Summary

**JDBC-backed UdfRegistry persisting versioned udf_artifact rows through Spring Data JPA, with published UDFs rehydrated into the Template V2 runtime on startup.**

## Performance

- **Duration:** ~90 min (dominated by multi-module Maven cycles + an H2 type-compatibility investigation)
- **Tasks:** persistence entity/repo, JdbcUdfRegistry, startup reloader, bean wiring, tests
- **Files modified:** 8 (6 created, 2 modified)

## Accomplishments
- `JdbcUdfRegistry` mirrors the Phase 2 `InMemoryUdfRegistry` contract exactly (reverse-DNS id + strict semver validation, same `UdfRegistryException` codes, `DRAFT → PUBLISHED → DEPRECATED` FSM, latest-published semver resolution) over durable storage.
- `UdfArtifactPO` + `UdfArtifactRepository` provide the version-history, by-state, and point-lookup finders the registry needs.
- `UdfStartupReloader` refreshes the runtime registry on `ApplicationReadyEvent` so persisted published UDFs resolve without re-upload.
- `CoreConfig` now registers the JDBC-backed registry as the active `UdfRegistry`, with the conditional in-memory bean backing off.

## Files Created/Modified
- `UdfArtifactPO.java` - JPA entity for one versioned UDF row (unique `udf_id+version`, payload as `bytea`).
- `UdfArtifactRepository.java` - finders for history, by-state reload, and duplicate/transition lookup.
- `JdbcUdfRegistry.java` - repository-backed `UdfRegistry` implementation.
- `UdfStartupReloader.java` - ready-event runtime refresh of persisted published UDFs.
- `CoreConfig.java` - primary `jdbcUdfRegistry` bean.
- `db/schema.sql` - `udf_artifact` DDL.
- `JdbcUdfRegistryTests.java`, `UdfStartupReloaderTests.java` - integration/unit coverage (11 tests).

## Decisions Made
- See key-decisions in frontmatter. The decisive one: H2 in `MODE=PostgreSQL` rejects `BLOB`, so the binary `payload` is pinned to `bytea` (entity `columnDefinition` + schema.sql), keeping Hibernate's `create-drop` DDL and the init script consistent.

## Deviations from Plan

### Auto-fixed Issues

**1. [Blocking] H2 PostgreSQL-mode BLOB incompatibility**
- **Found during:** plan verification (Spring context failed to load — `Table "UDF_ARTIFACT" not found`)
- **Issue:** Both the `db/schema.sql` script and Hibernate's `create-drop` DDL emitted `BLOB`, which H2's `MODE=PostgreSQL` rejects with `Unknown data type: "BLOB"`, so the table was never created.
- **Fix:** Pinned the `payload` column to `bytea` via `@Column(columnDefinition = "bytea")` (mirroring the entity's existing `CLOB` `metadata_json` pattern) and set `BYTEA` in `db/schema.sql`.
- **Files modified:** UdfArtifactPO.java, db/schema.sql
- **Verification:** all 19 service UDF tests green (`JdbcUdfRegistryTests`, `UdfStartupReloaderTests`, plus Phase 2 `UdfRegistryServiceTests`/`UdfPublishServiceTests` regression).

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Necessary for correctness under the embedded H2 test datasource. No scope creep.

## Issues Encountered
- An intermittent `Script UDF timed out after 5000 ms` in `UdfPublishServiceTests` while the broken Spring context burned ~300s on retries; it cleared once the schema fix let the context load cleanly (JVM no longer thrashing).

## Next Phase Readiness
- Durable registry + runtime rehydration are ready to back the console REST API (03-02) and publish-time template validation (03-04).

---
*Phase: 03-udf-console-template-binding*
*Completed: 2026-06-18*
