---
phase: 06-datasource-platform-core
plan: 04
subsystem: database
tags: [datasource, catalog, spring, bootstrap]

requires:
  - phase: 06-02
    provides: JDBC catalog adapter
  - phase: 06-03
    provides: Kafka/ES registry adapters
provides:
  - ConnectionCatalogImpl merging BOOTSTRAP + MANAGED entries
  - CatalogBootstrapSupport for yaml bootstrap registration
  - CoreConfig ConnectionCatalog bean wiring
affects: [06-05]

tech-stack:
  added: [ConnectionCatalogImpl, CatalogBootstrapSupport, CatalogBootstrapRegistry]
  patterns: [strangler catalog in service layer, repository-direct wiring to avoid cycles]

key-files:
  created:
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogImpl.java
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/CatalogBootstrapSupport.java
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/CatalogBootstrapRegistry.java
    - data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogImplTests.java
    - data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogBootstrapTests.java
  modified:
    - data-generator-service/pom.xml
    - data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/DataSourceConfigService.java
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/DataSourceBootstrap.java

key-decisions:
  - "ConnectionCatalogImpl uses repositories directly (not DataSourceConfigService) to break Spring cycle"
  - "Bootstrap-only JDBC names rejected on remove() via connectionCatalog.isBootstrapOnly()"

requirements-completed: [DS-02]

duration: 90min
completed: 2026-06-24
---

# Phase 06 Plan 04 Summary

**Production ConnectionCatalog in service layer merges yaml bootstrap and DB-managed JDBC/Kafka/ES entries**

## Accomplishments

- Implemented `ConnectionCatalogImpl` with `resolve` and `listAll` delegating to dynamic routing + messaging registries and persistence repos
- Added `CatalogBootstrapSupport` registering yaml JDBC/Kafka/ES entries at startup (D-25)
- Wired `ConnectionCatalog` bean in `CoreConfig`; bootstrap guard on `DataSourceConfigService.remove()`
- Unit and integration tests for MANAGED-over-BOOTSTRAP merge and bootstrap catalog visibility

## Self-Check: PASSED

- ConnectionCatalogImplTests, ConnectionCatalogBootstrapTests, DataSourceConfigServiceTests, ConsoleDataSourceControllerTest pass

---
*Phase: 06-datasource-platform-core*
*Completed: 2026-06-24*
