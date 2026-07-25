---
phase: 07-datasource-governance-hot-reload
plan: 03
subsystem: api
tags: [datasource, governance, audit, connectivity-test, jdbc, kafka, elasticsearch]

requires:
  - phase: 07-datasource-governance-hot-reload
    provides: ConnectionCatalog API, HotReloadCoordinator, ConnectionSnapshotSupport (07-01/07-02)
provides:
  - Unified ConnectionCatalog.test() for JDBC/Kafka/ES via ConnectionConnectivityService
  - Governance flags in DataGeneratorProperties + profile yaml binding
  - DatasourceGovernanceSupport with managed-only, BOOTSTRAP, grandfather rules
  - ConnectivityTestGate for save/publish connectivity enforcement
  - Datasource audit action constants and summary-only detail payloads
  - ConsoleAuditController category=DATASOURCE filter
affects: [07-console-datasource-ui, phase-7-wave-4]

tech-stack:
  added: [KafkaConnectivityTester, ElasticsearchConnectivityTester, ConnectivityTestGate]
  patterns: [catalog.test delegation, profile-gated governance, summary-only audit detail]

key-files:
  created:
    - data-generator-datasource/data-generator-datasource-kafka/src/main/java/org/gensokyo/data/datasource/kafka/KafkaConnectivityTester.java
    - data-generator-datasource/data-generator-datasource-elasticsearch/src/main/java/org/gensokyo/data/datasource/elasticsearch/ElasticsearchConnectivityTester.java
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionConnectivityService.java
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectivityTestGate.java
    - data-generator-service/src/main/java/org/gensokyo/data/template/DatasourceGovernanceSupport.java
    - data-generator-service/src/main/java/org/gensokyo/data/audit/DatasourceAuditActions.java
    - data-generator-service/src/main/java/org/gensokyo/data/audit/DatasourceAuditDetail.java
    - data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogTestTests.java
    - data-generator-service/src/test/java/org/gensokyo/data/template/DatasourceGovernanceIT.java
    - data-generator-service/src/test/java/org/gensokyo/data/audit/DatasourceAuditTests.java
  modified:
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/ConnectionCatalogImpl.java
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/DataSourceConfigService.java
    - data-generator-service/src/main/java/org/gensokyo/data/messaging/MessagingClusterConfigService.java
    - data-generator-service/src/main/java/org/gensokyo/data/datasource/catalog/HotReloadCoordinator.java
    - data-generator-service/src/main/java/org/gensokyo/data/config/DataGeneratorProperties.java
    - data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java
    - data-generator-service/src/main/java/org/gensokyo/data/template/TemplateLifecycleService.java
    - data-generator-service/src/main/java/org/gensokyo/data/controller/TaskController.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleAuditController.java
    - data-generator-service/src/main/resources/application-staging.yaml
    - data-generator-service/src/test/resources/application-phase7-test.yaml

key-decisions:
  - "Connectivity probes live in kind-specific testers (Jdbc via existing driver support; Kafka AdminClient; ES RestClient HTTP ping)"
  - "Governance property names use require-* / allow-* kebab-case yaml binding per existing Phase B pattern"
  - "Grandfather rule: unchanged PUBLISHED templates skip managed-only enforcement at run until material edit"

patterns-established:
  - "ConnectionCatalogImpl.test() delegates to ConnectionConnectivityService; success records ConnectivityTestGate pass"
  - "Audit detail uses DatasourceAuditDetail.summary() — connectionName/kind/action only, sanitized via AuditDetailSanitizer"
  - "HotReloadCoordinator emits DATASOURCE_RELOAD on every attempt and DATASOURCE_DEGRADED on failure overlay"

requirements-completed: [DS-03, DS-04, DS-05]

duration: 45min
completed: 2026-06-27
---

# Phase 07 Plan 03 Summary

**Unified JDBC/Kafka/ES connectivity tests, profile-gated datasource governance at save/publish/run, and summary-only audit feed with DATASOURCE category filter**

## Performance

- **Duration:** ~45 min (Wave 3 execution on top of in-progress implementation)
- **Tasks:** 2
- **Files modified:** ~20 service/datasource sources + 4 test classes

## Accomplishments

- `ConnectionCatalogImpl.test()` delegates to `ConnectionConnectivityService` with JDBC/Kafka/ES adapters; draft and named-entry flows supported
- `DataGeneratorProperties.Governance` extended with managed-connections, connectivity-test-before-save/publish, and bootstrap-ref flags; wired in staging and phase7-test profiles
- `DatasourceGovernanceSupport` enforces inline-block rejection, BOOTSTRAP policy, and grandfather run bypass; wired in `TemplateV2Validator`, `TemplateLifecycleService`, and `TaskController`
- `ConnectivityTestGate` blocks save/publish when profile requires recent successful test
- Seven datasource audit actions (`CREATE` through `GOVERNANCE_BLOCK`); reload/degraded/connectivity-fail/governance-block wired; `ConsoleAuditController` accepts `category=DATASOURCE`

## Verification

```text
.\mvnw-jdk25.ps1 -pl data-generator-service -am test \
  -Dtest=ConnectionCatalogTestTests,DatasourceGovernanceIT,DatasourceAuditTests,ConsoleAuditControllerTest,HotReloadTests,DataSourceConfigServiceTests \
  -Dsurefire.failIfNoSpecifiedTests=false -q
```

Result: **PASS** (22 tests)

## Deviations from Plan

### Auto-fixed Issues

**1. [Test expectation] DataSourceConfigServiceTests message assertion**
- **Found during:** Task 1 verification
- **Issue:** Test expected legacy `"Connection OK"` but unified probe returns `"JDBC connection OK"`
- **Fix:** Updated test assertion to match actionable unified message
- **Files modified:** `data-generator-service/src/test/java/org/gensokyo/data/datasource/DataSourceConfigServiceTests.java`

---

**Total deviations:** 1 auto-fixed (test alignment)
**Impact on plan:** No functional change; test aligned with D-20 message format.

## Issues Encountered

None blocking.

## Next Phase Readiness

- Backend DS-04/DS-05 complete for console Wave 4 (connectivity UI, governance banners, audit feed filter)
- Production profile should set `allow-bootstrap-references: false` when prod yaml is introduced

---
*Phase: 07-datasource-governance-hot-reload*
*Completed: 2026-06-27*
