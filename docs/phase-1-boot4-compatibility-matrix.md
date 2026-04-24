# Phase 1 Boot 4 Compatibility Matrix

Generated on `2026-04-24`.

## Purpose

This document converts the current JDK 25 + Spring Boot 3.5.13 baseline into a repository-specific compatibility inventory for the Spring Boot 4.0 upgrade.

Current baseline:

- Build JDK: `25.0.1`
- Compiler target: Java `17`
- Spring Boot baseline: `3.5.13`
- Maven Wrapper: `3.9.11`

## Active Spring starters by module

| Module | Active starters / Spring-facing dependencies | Boot 4 focus |
| --- | --- | --- |
| `data-generator-service` | `spring-boot-starter-web`, `spring-boot-starter-logging`, `spring-boot-starter-webflux`, `spring-boot-starter-data-jpa`, `dynamic-datasource-spring-boot-starter`, `druid`, `hibernate-validator`, `httpclient5` | Highest-risk application entrypoint. Mixed MVC + WebFlux + JPA + datasource stack must be revalidated first after BOM move. |
| `data-generator-reader-ai` | `spring-boot-starter-webflux`, `reactor-core`, `jsonschema-module-jackson` | Reactive path plus schema/Jackson coupling. Also candidate for later Spring AI alignment. |
| `data-generator-reader-elasticsearch` | `spring-boot-starter`, `es-spring-boot-starter`, `elasticsearch-rest-high-level-client` | Legacy Elasticsearch client path is likely a migration hotspot under Boot 4 / Spring Framework 7. |
| `data-generator-writer-kafka` | `kafka-spring-boot-starter` | Internal starter must be validated independently from the Boot BOM upgrade. |
| `data-generator-scripter-javascript` | `org.graalvm.polyglot:*`, `org.graalvm.js:*` | JDK 25 compatibility is already stabilized, but Boot 4 transitive drift still needs verification. |
| `data-generator-faker` | `spring-boot-starter`, `jackson-databind`, `jackson-datatype-jts` | Jackson 3 impact area. |
| `data-generator-stage` | `spring-boot-starter` | Low-risk Spring baseline check. |
| `data-generator-common/data-generator-core` | `spring-boot-starter`, `jackson-dataformat-yaml`, `jackson-datatype-jsr310`, `jackson-annotations`, `jackson-databind` | Shared Jackson/YAML surface; Phase 5 focus. |
| `data-generator-reader-database` | `spring-boot-starter`, `dynamic-datasource-spring-boot-starter`, `druid` | Data-layer compatibility check with Boot 4. |
| `data-generator-iterator-database` | `spring-boot-starter`, `mybatis-plus-spring-boot3-starter` | Direct Boot 3 starter naming dependency. Must be replaced or upgraded in Phase 7. |

## Parent POM version ownership

### Boot baseline

| Property | Current value | Ownership | Phase 2 action |
| --- | --- | --- | --- |
| `spring-boot.version` | `3.5.13` | Boot platform baseline | Move to selected `4.0.x` target. |

### Explicitly pinned versions in parent `pom.xml`

These items are already outside a pure "follow Boot defaults" posture and must be reviewed when the BOM moves.

| Property | Current value | Primary scope | Boot 4 handling |
| --- | --- | --- | --- |
| `lombok.version` | `1.18.44` | annotation processing / compile | Verify whether explicit pin is still needed. |
| `mockito.version` | `5.17.0` | tests, javaagent | Recheck test runtime and agent path under Boot 4. |
| `classgraph.version` | `4.8.184` | classpath scanning | Keep unless Boot 4 baseline proves sufficient; verify no reflective warning drift. |
| `vaadin.version` | `24.4.0.beta3` | optional UI BOM/starter | Keep isolated; not on critical path unless module is activated. |
| `spring-ai.version` | `1.0.0-SNAPSHOT` | optional AI BOM/starter | High churn; treat as independent track from Boot 4 BOM move. |
| `graal.js.version` | `24.2.2` | JavaScript runtime | Preserve current JDK 25-compatible version unless a stronger Boot 4 requirement appears. |
| `hibernate-validator.version` | `8.0.1.Final` | validation stack | Re-evaluate against Boot 4 Jakarta EE 11 alignment. |
| `elasticsearch.version` | `7.17.8` | legacy Elasticsearch client | High-risk legacy component; likely cannot stay untouched. |
| `reactor.version` | `3.7.14` | reactive stack | Recheck if the explicit override remains necessary on Boot 4. |
| `jackson.version` | `2.21.2` | JSON/YAML/schema stack | Dedicated Phase 5 migration item because Boot 4 adopts Jackson 3. |
| `dynamic-datasource.version` | `3.6.1` | datasource routing | Independent compatibility verification required. |
| `mybatis-plus.version` | `3.5.7` | data access | Current artifact name is Boot 3-specific; expect replacement work. |
| `mybatis-flex.version` | `1.9.5` | data access / processor | Current artifact name is Boot 3-specific; expect replacement work. |
| `druid.version` | `1.2.23` | datasource pool | Independent compatibility verification required. |

### Dependencies that are likely Boot-managed after the BOM move

These still need confirmation after switching to Boot 4, but they are expected to follow the platform more than project-local pins.

| Dependency family | Evidence in repo | Phase 2 check |
| --- | --- | --- |
| Spring starters | `spring-boot-starter-*` across service, core, reader, stage, faker modules | Confirm starter coordinates remain valid on Boot 4. |
| `spring-boot-starter-test` | parent POM | Confirm whether the repo should stay on the default test starter or move to `spring-boot-starter-test-classic`. |
| `spring-boot-starter-validation` | parent POM | Confirm Jakarta EE 11 alignment and whether explicit `hibernate-validator` pin can be relaxed. |
| `httpclient5` | `data-generator-service` | Confirm Boot 4 still manages the selected major line. |
| Reactor / Netty transitive stack | service and AI reader | Recheck explicit `reactor.version` override and test JVM flags. |

## Independently managed libraries requiring validation

These are not safe to assume just because the Boot BOM moves cleanly.

| Component | Current repo usage | Risk | Why it is independent |
| --- | --- | --- | --- |
| `dynamic-datasource-spring-boot-starter` | service, reader-database, writer-database | High | Third-party starter outside the core Spring dependency train. |
| `druid` | service, reader-database, writer-database | High | Boot integration and runtime warnings are library-specific. |
| `mybatis-plus-spring-boot3-starter` | parent POM, iterator-database | High | Artifact name is explicitly tied to Boot 3. |
| `mybatis-flex-spring-boot3-starter` | parent POM | High | Artifact name is explicitly tied to Boot 3. |
| `org.gensokyo.boot:kafka-spring-boot-starter` | dependency BOM, writer-kafka | High | Internal starter; must be validated against Boot 4 autoconfiguration changes. |
| `org.gensokyo.boot:es-spring-boot-starter` | dependency BOM, reader/writer Elasticsearch | High | Internal starter plus legacy Elasticsearch client coupling. |
| `org.elasticsearch.client:elasticsearch-rest-high-level-client` | dependency BOM, reader/writer Elasticsearch | High | Legacy client path may conflict with Boot 4 ecosystem direction. |
| `jsonschema-module-jackson` | service, reader-ai, parent POM | High | Sensitive to Jackson 3 adoption. |
| `jackson-module-jsonSchema` | parent POM | High | Jackson 3 compatibility must be checked explicitly. |
| `org.graalvm.polyglot` / `org.graalvm.js` | scripter-javascript, parent POM | Medium | Already stable on JDK 25, but still outside Spring's managed lane. |
| `spring-ai-ollama-spring-boot-starter` | parent POM optional dependency management | Medium | Snapshot line with separate release cadence. |
| `vaadin-spring-boot-starter` | parent POM optional dependency management | Medium | Optional and currently not on the critical runtime path. |
| DB drivers (`mysql`, `postgresql`, `clickhouse`, `dm`) | managed in parent/dependency POMs | Medium | Need runtime validation on JDK 25 + Boot 4 even if compile succeeds. |

## Boot 4 hotspot modules and priority

| Priority | Module | Why it is on the critical path | First validation gate |
| --- | --- | --- | --- |
| P0 | `data-generator-service` | Main runtime assembly plus web/reactive/data/validation integration in one module | `compile`, context startup, focused tests |
| P0 | `data-generator-reader-ai` | WebFlux + Reactor + JSON schema interactions | module compile and reactive tests |
| P0 | `data-generator-reader-elasticsearch` | Internal starter + legacy Elasticsearch client | module compile and context startup |
| P0 | `data-generator-writer-kafka` | Internal starter integration with Boot autoconfiguration | module compile and context startup |
| P1 | `data-generator-scripter-javascript` | JDK 25 already handled, but runtime transitive drift must be checked | module test/compile |
| P1 | `data-generator-iterator-database` | Direct Boot 3 starter naming dependency | module compile |
| P1 | `data-generator-common/data-generator-core` | Shared Jackson/YAML surface across modules | schema/YAML tests |

## Phase 2 input checklist

Use this inventory before changing `spring-boot.version`:

1. Choose a concrete Boot target, currently expected to be `4.0.5`.
2. Decide which explicit version pins remain intentional on day 1 of the BOM move:
   - `jackson.version`
   - `reactor.version`
   - `hibernate-validator.version`
   - `classgraph.version`
   - `mockito.version`
3. Confirm replacement or upgrade path for Boot 3-specific artifacts:
   - `mybatis-plus-spring-boot3-starter`
   - `mybatis-flex-spring-boot3-starter`
4. Confirm whether internal starters already support:
   - Spring Boot 4
   - Spring Framework 7
   - Jakarta EE 11
5. Prepare a focused compile order after the BOM move:
   - `data-generator-service`
   - `data-generator-reader-ai`
   - `data-generator-reader-elasticsearch`
   - `data-generator-writer-kafka`
6. Treat Jackson 3 as a separate stabilization stream even if the initial Boot 4 compile fails elsewhere first.

## Exit criteria for Phase 1

- Active Spring starter usage is enumerated by module.
- Parent POM version ownership is classified.
- Independent third-party and internal starter risks are identified.
- Phase 2 has an explicit input list instead of starting from a blind BOM move.
