---
phase: 06-datasource-platform-core
plan: 03
subsystem: database
tags: [kafka, elasticsearch, datasource, registry, adapter]

requires:
  - phase: 06-01
    provides: datasource-api module
provides:
  - data-generator-datasource-kafka module with DynamicKafkaTemplateRegistry
  - data-generator-datasource-elasticsearch module with DynamicElasticsearchClientRegistry
  - Updated import graph across core, calcite, service, writers, readers
affects: [06-04, 06-05]

tech-stack:
  added: [data-generator-datasource-kafka, data-generator-datasource-elasticsearch]
  patterns: [registry relocation to adapter modules, primary-cluster fallback]

key-files:
  created:
    - data-generator-datasource/data-generator-datasource-kafka/src/main/java/org/gensokyo/data/datasource/kafka/DynamicKafkaTemplateRegistry.java
    - data-generator-datasource/data-generator-datasource-elasticsearch/src/main/java/org/gensokyo/data/datasource/elasticsearch/DynamicElasticsearchClientRegistry.java
  modified:
    - data-generator-common/data-generator-core/pom.xml
    - data-generator-calcite/pom.xml

key-decisions:
  - "Package org.gensokyo.data.datasource.kafka/elasticsearch (single cutover, no deprecated shim per D-35)"

patterns-established:
  - "Pattern: messaging registries live in datasource adapter modules, not core"

requirements-completed: [DS-01]

duration: 45min
completed: 2026-06-24
---

# Phase 06 Plan 03 Summary

**Kafka and Elasticsearch cluster registries relocated to dedicated datasource adapter modules with primary fallback preserved**

## Performance

- **Duration:** 45 min
- **Tasks:** 2
- **Files modified:** 33

## Accomplishments

- Created `data-generator-datasource-kafka` with relocated `DynamicKafkaTemplateRegistry`
- Created `data-generator-datasource-elasticsearch` with relocated `DynamicElasticsearchClientRegistry`
- Updated all import sites across core, calcite, service, writer-kafka, writer-elasticsearch, reader-elasticsearch
- Added unit tests for primary fallback and unknown cluster errors in both adapters

## Task Commits

1. **Task 1: Relocate Kafka registry** - `4e06a23` (feat)
2. **Task 2: Relocate Elasticsearch registry** - `aa49791` (feat)

## Deviations from Plan

None - plan executed as specified.

## Self-Check: PASSED

- KafkaSinkFactoryTests and ElasticsearchSinkFactoryTests pass
- Adapter module unit tests pass (4 tests total)
- No remaining imports of `org.gensokyo.data.kafka.support.DynamicKafkaTemplateRegistry`
- No remaining imports of `org.gensokyo.data.elasticsearch.support.DynamicElasticsearchClientRegistry`

## Next Phase Readiness

- Service-layer ConnectionCatalog (06-04) can delegate to JDBC/Kafka/ES adapters
- Calcite wiring (06-05) can inject ConnectionCatalog into runtime services

---
*Phase: 06-datasource-platform-core*
*Completed: 2026-06-24*
