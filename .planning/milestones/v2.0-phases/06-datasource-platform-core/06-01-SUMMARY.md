---
phase: 06-datasource-platform-core
plan: 01
subsystem: database
tags: [datasource, catalog, jdbc, kafka, elasticsearch, maven]

requires: []
provides:
  - data-generator-datasource-api Maven module
  - ConnectionCatalog resolve/list contracts
  - CatalogEntry, ConnectionKind, CatalogEntrySource types
  - Sealed ResolvedConnection hierarchy for JDBC/Kafka/ES handles
affects: [06-02, 06-03, 06-04, 06-05]

tech-stack:
  added: [data-generator-datasource-api]
  patterns: [sealed ResolvedConnection hierarchy, display-only CatalogMetadata]

key-files:
  created:
    - data-generator-datasource/data-generator-datasource-api/pom.xml
    - data-generator-datasource/data-generator-datasource-api/src/main/java/org/gensokyo/data/datasource/api/ConnectionCatalog.java
    - data-generator-datasource/data-generator-datasource-api/src/main/java/org/gensokyo/data/datasource/api/CatalogEntry.java
    - data-generator-datasource/data-generator-datasource-api/src/main/java/org/gensokyo/data/datasource/api/ConnectionKind.java
    - data-generator-datasource/data-generator-datasource-api/src/main/java/org/gensokyo/data/datasource/api/CatalogEntrySource.java
    - data-generator-datasource/data-generator-datasource-api/src/main/java/org/gensokyo/data/datasource/api/ResolvedConnection.java
  modified:
    - data-generator-datasource/pom.xml

key-decisions:
  - "Kafka/ES resolved handles use Object to avoid client dependencies in api module"
  - "CatalogMetadata sealed hierarchy carries non-secret list hints per kind"

patterns-established:
  - "Pattern: ConnectionCatalog is resolve-only; CRUD stays in service layer"
  - "Pattern: CatalogResolveSupport builds actionable IllegalArgumentException messages (D-07)"

requirements-completed: [DS-01]

duration: 25min
completed: 2026-06-24
---

# Phase 06 Plan 01 Summary

**Unified ConnectionCatalog API module with sealed resolve results and non-secret catalog list metadata for JDBC, Kafka, and Elasticsearch**

## Performance

- **Duration:** 25 min
- **Started:** 2026-06-24T00:00:00Z
- **Completed:** 2026-06-24T00:25:00Z
- **Tasks:** 2
- **Files modified:** 15

## Accomplishments

- Added `data-generator-datasource-api` submodule to the datasource aggregator reactor
- Published `ConnectionCatalog` with `resolve` and `listAll` contracts
- Defined `CatalogEntry`, `ConnectionKind`, `CatalogEntrySource`, and sealed `ResolvedConnection` types
- Kept api module free of Spring, Kafka, ES, and Druid dependencies

## Task Commits

1. **Task 1: Add datasource-api submodule to Maven reactor** - `93931bf` (feat)
2. **Task 2: Define ConnectionCatalog and entry model types** - `b531406` (feat)

## Files Created/Modified

- `data-generator-datasource/data-generator-datasource-api/pom.xml` - Api module with zero adapter deps
- `data-generator-datasource/pom.xml` - Registers api submodule
- `org.gensokyo.data.datasource.api.*` - Catalog contracts and metadata types

## Decisions Made

- Kafka and Elasticsearch runtime handles are `Object` in api records to preserve dependency-free boundary; adapters cast to framework types
- `CatalogMetadata` uses sealed permits per kind for list display hints without secrets

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## Self-Check: PASSED

- `data-generator-datasource-api` compiles via `mvnw-jdk25.ps1 -pl ... -am compile`
- No `spring-`, `kafka-`, or `elasticsearch` imports in api sources
- `ConnectionKind` has JDBC, KAFKA, ELASTICSEARCH
- `CatalogEntrySource` has BOOTSTRAP, MANAGED

## Next Phase Readiness

- JDBC adapter (06-02) and Kafka/ES adapter (06-03) can depend on `data-generator-datasource-api`
- Service catalog implementation (06-04) can implement `ConnectionCatalog`

---
*Phase: 06-datasource-platform-core*
*Completed: 2026-06-24*
