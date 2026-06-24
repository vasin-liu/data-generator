---
phase: 06-datasource-platform-core
plan: 05
subsystem: database
tags: [calcite, catalog, runtime, console]

requires:
  - phase: 06-04
    provides: production ConnectionCatalog bean
provides:
  - TemplateV2RuntimeServices backed by ConnectionCatalog
  - InMemoryCatalog test utility
  - Catalog-backed DefaultRuntimeJdbcEndpointResolver
  - Console overview catalogConnections with BOOTSTRAP/MANAGED source
affects: []

tech-stack:
  added: [InMemoryCatalog, CatalogConnectionSummaryDto]
  patterns: [calcite depends on datasource-api only; kafka/es adapters test-scoped]

key-files:
  created:
    - data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/InMemoryCatalog.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/CatalogConnectionSummaryDto.java
  modified:
    - data-generator-calcite/pom.xml
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2RuntimeServices.java
    - data-generator-service/src/main/java/org/gensokyo/data/config/DefaultRuntimeJdbcEndpointResolver.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleDataSourceController.java

requirements-completed: [DS-01, DS-02]

duration: 75min
completed: 2026-06-24
---

# Phase 06 Plan 05 Summary

**Calcite runtime and console delegate connection resolution to ConnectionCatalog without template YAML changes**

## Accomplishments

- `TemplateV2RuntimeServices` now exposes `ConnectionCatalog`; Kafka/ES helpers resolve via catalog
- `DefaultRuntimeJdbcEndpointResolver` validates managed JDBC ids through catalog (inline fallback unchanged)
- `InMemoryCatalog` enables calcite unit tests without full Boot context
- Console `/api/datasources` overview adds `catalogConnections` with `source` (BOOTSTRAP/MANAGED)
- calcite main dependency: `data-generator-datasource-api` only (kafka/es adapters test-scoped)

## Self-Check: PASSED

- TemplateV2RuntimeServicesTests, KafkaSinkFactoryTests, ElasticsearchSinkFactoryTests pass
- V2ScenarioTemplateIT passes without template YAML changes
- ConnectionCatalog regression slice green

---
*Phase: 06-datasource-platform-core*
*Completed: 2026-06-24*
