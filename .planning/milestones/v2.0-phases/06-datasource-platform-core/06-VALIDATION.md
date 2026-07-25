---
phase: 6
slug: datasource-platform-core
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-06-24
approved: 2026-06-24
---

# Phase 6 — Validation Strategy

> Nyquist validation contract for Datasource Platform Core (DS-01, DS-02).
> Reconstructed from plan artifacts (State B) — all five plans executed with SUMMARY.md.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + Mockito |
| **Config file** | `application-phase7-test.yaml` (service ITs) |
| **Quick run command** | `.\mvnw-jdk25.ps1 -pl data-generator-datasource -am test -q` |
| **Full suite command** | `.\mvnw-jdk25.ps1 -pl "data-generator-service,data-generator-calcite,data-generator-datasource" -am test "-Dtest=DataSourceConfigServiceTests,ConsoleDataSourceControllerTest,ConnectionCatalogImplTests,ConnectionCatalogBootstrapTests,JdbcCatalogResolverTests,DynamicKafkaTemplateRegistryTests,DynamicElasticsearchClientRegistryTests,TemplateV2RuntimeServicesTests,KafkaSinkFactoryTests,ElasticsearchSinkFactoryTests,V2ScenarioTemplateIT" "-Dsurefire.failIfNoSpecifiedTests=false" -q` |
| **Estimated runtime** | ~150 seconds |

---

## Sampling Rate

- **After every task commit:** Run plan-specific `<automated>` verify from PLAN.md
- **After every plan wave:** Run quick module test for touched adapters
- **Before phase close:** Full suite command above must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | DS-01 | — | Api module compiles without adapter deps | compile | `mvnw-jdk25.ps1 -pl data-generator-datasource/data-generator-datasource-api -am compile -q` | ✅ | ✅ green |
| 06-01-02 | 01 | 1 | DS-01 | T-06-08 | Catalog list types carry metadata only | unit | api compile + type review | ✅ | ✅ green |
| 06-02-01 | 02 | 2 | DS-01 | — | JDBC adapter module scaffold | compile | `mvnw-jdk25.ps1 -pl data-generator-datasource/data-generator-datasource-jdbc -am compile -q` | ✅ | ✅ green |
| 06-02-02 | 02 | 2 | DS-01 | — | Managed + inline JDBC resolve | unit | `JdbcCatalogResolverTests` | ✅ | ✅ green |
| 06-03-01 | 03 | 2 | DS-01 | — | Kafka registry relocation + primary fallback | unit | `DynamicKafkaTemplateRegistryTests` + `KafkaSinkFactoryTests` | ✅ | ✅ green |
| 06-03-02 | 03 | 2 | DS-01 | — | ES registry relocation + primary fallback | unit | `DynamicElasticsearchClientRegistryTests` + `ElasticsearchSinkFactoryTests` | ✅ | ✅ green |
| 06-04-01 | 04 | 3 | DS-02 | T-06-07 | MANAGED wins over BOOTSTRAP on name collision | unit | `ConnectionCatalogImplTests` | ✅ | ✅ green |
| 06-04-02 | 04 | 3 | DS-02 | T-06-07 | BOOTSTRAP yaml entries registered at startup | integration | `ConnectionCatalogBootstrapTests` | ✅ | ✅ green |
| 06-05-01 | 05 | 4 | DS-01 | T-06-09 | Calcite resolves via ConnectionCatalog | unit | `TemplateV2RuntimeServicesTests` | ✅ | ✅ green |
| 06-05-02 | 05 | 4 | DS-02 | T-06-07 | Console + V2 regression on catalog path | integration | `V2ScenarioTemplateIT`, `ConsoleDataSourceControllerTest`, sink factory tests | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Roadmap Success Criteria Coverage

| Criterion | Verification | Status |
|-----------|--------------|--------|
| Templates resolve JDBC/Kafka/ES via abstraction without YAML shape change | `V2ScenarioTemplateIT`, `KafkaSinkFactoryTests`, `ElasticsearchSinkFactoryTests` | ✅ |
| `data-generator-datasource` has api + jdbc + kafka + es submodules | Reactor `pom.xml` + adapter unit tests | ✅ |
| Console CRUD + V2 run paths pass regression | `DataSourceConfigServiceTests`, `ConsoleDataSourceControllerTest`, `V2ScenarioTemplateIT` | ✅ |

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements.

Gap-fill tests added during validation audit:

- [x] `DataSourceConfigServiceTests.removeRejectsBootstrapOnlyYamlDatasource` — D-24 bootstrap delete guard
- [x] `ConsoleDataSourceControllerTest.overview_includesCatalogConnectionsWithSource` — D-26/D-37 source visibility

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Catalog list payloads never include passwords/api keys | D-10 | Static review of `CatalogEntry`/`CatalogMetadata` shapes | Grep list/serialize paths for secret fields; confirm only url/driver/broker hosts in metadata |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 180s (quick module runs)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-06-24

---

## Validation Audit 2026-06-24

| Metric | Count |
|--------|-------|
| Gaps found | 2 |
| Resolved | 2 |
| Escalated | 0 |

Gaps filled:
1. Bootstrap-only JDBC remove guard (D-24) — `DataSourceConfigServiceTests`
2. Console `catalogConnections` with BOOTSTRAP/MANAGED source — `ConsoleDataSourceControllerTest`
