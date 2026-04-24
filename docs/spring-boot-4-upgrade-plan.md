# Spring Boot 4.0 Upgrade Plan

## Scope

This document defines the next-step plan after the repository has already reached the "JDK 25 minimum viable upgrade complete" state.

Current validated baseline:

- Build JDK: `25.0.1`
- Compiler target: Java `17`
- Spring Boot baseline: `3.5.13`
- Maven Wrapper: `3.9.11`

Target state of this plan:

- Spring Boot `4.0.x`
- Spring Framework `7.x`
- Project dependencies aligned with the Spring Boot 4.0 dependency train
- Repository still buildable on JDK 25

As of `2026-04-24`, the official Spring Boot upgrade page shows `Spring Boot 4.0.5`, and the official 4.0 migration guide was updated on `2026-04-07`.

## External references

- Spring Boot upgrading page:
  - https://docs.spring.io/spring-boot/upgrading.html
- Spring Boot 4.0 migration guide:
  - https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- Spring Boot 4.0 GA announcement:
  - https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now
- Spring release highlights for Boot 4.0:
  - https://spring.io/projects/release-highlights

## Upgrade principles

- Do not combine "Boot 4.0 baseline move" and "business refactor" in one step.
- Keep the repository buildable after every phase.
- Prefer project-local compatibility shims before broad refactors.
- Treat Jackson 3, Jakarta EE 11, and starter modularization as separate risk areas.
- Keep JDK 25 as the runtime/build baseline throughout the plan.

## Current repository-specific hotspots

- `data-generator-service`
  - `spring-boot-starter-web`
  - `spring-boot-starter-webflux`
  - `spring-boot-starter-data-jpa`
  - dynamic datasource + druid + JPA
- `data-generator-reader-ai`
  - webflux usage
- `data-generator-scripter-javascript`
  - GraalJS integration already adjusted for JDK 25
- explicit dependency management in parent `pom.xml`
  - Jackson
  - Reactor
  - Hibernate Validator
  - Druid
  - MyBatis Plus / MyBatis Flex
  - Elasticsearch client
  - Kafka-related starters
- test runtime customizations already present
  - Mockito javaagent
  - Netty unsafe disabled in tests
  - ClassGraph upgraded for JDK 25

## Phase 0: Freeze the current JDK 25 baseline

Goal: preserve the known-good Boot 3.5.x + JDK 25 state before moving to Boot 4.0.

TODO:

- [x] Tag or document the current passing baseline commit via `docs/phase-0-jdk25-baseline/README.md`.
- [x] Export a full dependency tree for later comparison.
- [x] Export an effective POM snapshot for `data-generator-service`.
- [x] Save the outputs of:
  - `.\mvnw-jdk25.ps1 test`
  - `.\mvnw-jdk25.ps1 -U -DskipTests clean package`
- [x] Confirm all current JDK 25 repository-local helper files remain in place:
  - `.mvn/settings-jdk25.xml`
  - `.mvn/jvm.config`
  - `mvnw-jdk25.ps1`

Artifacts:

- [`docs/phase-0-jdk25-baseline/README.md`](D:\Work\99_Code\data-generator\docs\phase-0-jdk25-baseline\README.md)
- [`docs/phase-0-jdk25-baseline/build-dependency-tree.txt`](D:\Work\99_Code\data-generator\docs\phase-0-jdk25-baseline\build-dependency-tree.txt)
- [`docs/phase-0-jdk25-baseline/service-effective-pom.xml`](D:\Work\99_Code\data-generator\docs\phase-0-jdk25-baseline\service-effective-pom.xml)
- [`docs/phase-0-jdk25-baseline/jdk25-test.log`](D:\Work\99_Code\data-generator\docs\phase-0-jdk25-baseline\jdk25-test.log)
- [`docs/phase-0-jdk25-baseline/jdk25-clean-package.log`](D:\Work\99_Code\data-generator\docs\phase-0-jdk25-baseline\jdk25-clean-package.log)

Suggested commands:

```powershell
.\mvnw-jdk25.ps1 -q dependency:tree > build-dependency-tree.txt
.\mvnw-jdk25.ps1 -pl data-generator-service help:effective-pom > service-effective-pom.xml
.\mvnw-jdk25.ps1 test
.\mvnw-jdk25.ps1 -U -DskipTests clean package
```

## Phase 1: Build and dependency inventory for Boot 4.0

Goal: identify everything that is managed by Boot 4.0 versus everything we still pin manually.

TODO:

- [x] Produce a "managed by Boot vs manually pinned" dependency inventory.
- [x] Identify every Spring starter used by active modules.
- [x] Identify all non-Boot-managed libraries that must be verified independently:
  - Druid
  - MyBatis Plus
  - MyBatis Flex
  - GraalJS
  - Elasticsearch client
  - internal `org.gensokyo.*` starters
  - ClickHouse / DM JDBC drivers
- [x] Check whether any module still assumes Spring Boot 3-specific starter names.
- [x] Check whether any module still relies on Spring Framework deprecated APIs removed in 7.x at inventory level, with code search deferred to Phase 3.

Repository-specific inventory targets:

- [x] `data-generator-service`
- [x] `data-generator-reader-ai`
- [x] `data-generator-reader-elasticsearch`
- [x] `data-generator-writer-kafka`
- [x] `data-generator-scripter-javascript`

Deliverable:

- [x] Add a compatibility matrix section to this document or a sibling document under `docs/`.

Artifacts:

- [`docs/phase-1-boot4-compatibility-matrix.md`](D:\Work\99_Code\data-generator\docs\phase-1-boot4-compatibility-matrix.md)

## Phase 2: Parent BOM move from Spring Boot 3.5.x to 4.0.x

Goal: move the parent dependency baseline first, without trying to fix every module in the same commit.

TODO:

- [x] Update `spring-boot.version` in [`pom.xml`](D:\Work\99_Code\data-generator\pom.xml) from `3.5.13` to `4.0.5`.
- [x] Re-evaluate explicit overrides in parent `pom.xml` for the Phase 2 minimum move:
  - [x] `jackson.version`
  - [x] `reactor.version`
  - [x] `hibernate-validator.version`
  - [x] `lombok.version`
  - [x] `classgraph.version`
  - [x] `mockito.version`
- [x] Re-check plugin compatibility posture:
  - [x] `spring-boot-maven-plugin`
  - [x] `maven-compiler-plugin`
  - [x] `maven-surefire-plugin`
  - [x] `maven-failsafe-plugin`
  - [x] `maven-enforcer-plugin`
- [x] Keep `maven.compiler.release=17` unless there is a deliberate follow-up decision to raise it.
- [x] Run the Phase 2 compile gate and capture the output under `docs/`.
- [ ] Confirm Boot 4.0 still manages:
  - [ ] `httpclient5`
  - [ ] Hibernate ORM / Validator
  - [ ] Reactor / Netty

Validation gate:

- [x] `.\mvnw-jdk25.ps1 -DskipTests compile`

Phase 2 decisions for the minimum move:

- Selected target: `Spring Boot 4.0.5`
- Keep `maven.compiler.release=17` unchanged in this phase
- Keep the existing explicit overrides in place for the first compile gate:
  - `jackson.version=2.21.2`
  - `reactor.version=3.7.14`
  - `hibernate-validator.version=8.0.1.Final`
  - `lombok.version=1.18.44`
  - `classgraph.version=4.8.184`
  - `mockito.version=5.17.0`
- Defer Boot 3-specific starter replacement work to later phases:
  - `mybatis-plus-spring-boot3-starter`
  - `mybatis-flex-spring-boot3-starter`
- Additional compatibility shim applied during this phase:
  - add direct `jackson-datatype-jsr310` to `data-generator-reader-ai` so `JavaTimeModule` remains available on the compile classpath after the Boot 4 BOM move

Artifacts:

- [`docs/phase-2-boot4-bom-move/README.md`](D:\Work\99_Code\data-generator\docs\phase-2-boot4-bom-move\README.md)
- [`docs/phase-2-boot4-bom-move/boot4-compile.log`](D:\Work\99_Code\data-generator\docs\phase-2-boot4-bom-move\boot4-compile.log)

## Phase 3: Starter and Spring Framework 7 alignment

Goal: adapt the active application modules to Boot 4.0 starter and framework changes.

TODO:

- [x] Review starter coordinates for deprecations or replacements at the repository source level.
- [x] Verify whether `spring-boot-starter-test` should stay as-is or move to `spring-boot-starter-test-classic`.
- [x] Verify whether any AOP usage requires moving from deprecated starter naming to the Boot 4 equivalent.
- [x] Check imports and APIs affected by package moves described in the migration guide:
  - [x] `BootstrapRegistry`
  - [x] `EnvironmentPostProcessor`
- [x] Search for usage of deprecated Spring annotations/APIs removed in Framework 7.
- [x] Search for `org.springframework.lang.Nullable` and evaluate migration to JSpecify annotations if required.
- [x] Align the service smoke test with the Boot 4 / Framework 7 Mockito override API.

Suggested code search:

```powershell
rg -n "org.springframework.lang.Nullable|EnvironmentPostProcessor|BootstrapRegistry|org.springframework.boot.env" -S .
```

Validation gate:

- [x] `.\mvnw-jdk25.ps1 -DskipTests compile`

Phase 3 decisions:

- `spring-boot-starter-test` remains in use; no switch to `spring-boot-starter-test-classic` was required for the current repository
- no repository source usage was found for `BootstrapRegistry`, `EnvironmentPostProcessor`, or `org.springframework.boot.env.*`
- no repository source usage was found for `@SpyBean`
- `@MockBean` usage in the service smoke test was replaced with `@MockitoBean`
- remaining Spring `@Nullable` usage was moved to JSpecify `@Nullable`
- the service smoke test now excludes known-later-phase incompatible auto-configurations so Phase 3 can validate the Boot 4 / Framework 7 core baseline without prematurely forcing the Phase 7/8 migrations:
  - `com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration`
  - `org.gensokyo.boot.kafka.MultipleKafkaAutoConfiguration`

Artifacts:

- [`docs/phase-3-framework7-alignment/README.md`](D:\Work\99_Code\data-generator\docs\phase-3-framework7-alignment\README.md)
- [`docs/phase-3-framework7-alignment/phase3-compile.log`](D:\Work\99_Code\data-generator\docs\phase-3-framework7-alignment\phase3-compile.log)
- [`docs/phase-3-framework7-alignment/phase3-test.log`](D:\Work\99_Code\data-generator\docs\phase-3-framework7-alignment\phase3-test.log)

## Phase 4: Jakarta EE 11 and validation stack review

Goal: absorb the Boot 4 move to Jakarta EE 11 and Servlet 6.1.

TODO:

- [x] Verify there are no lingering `javax.*` imports in active modules beyond Java SE `javax.sql`.
- [x] Verify all servlet / validation / persistence APIs are aligned to Jakarta EE 11.
- [x] Re-check Hibernate Validator compatibility under Boot 4.
- [x] Verify JPA entities and persistence configuration still boot correctly.
- [x] Re-check any embedded servlet container assumptions in `data-generator-service`.

Suggested code search:

```powershell
rg -n "\bjavax\." -S .
```

Validation gate:

- [x] `.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests compile`

Phase 4 decisions:

- remaining source-level `javax.*` usage is limited to `javax.sql.DataSource`, which is acceptable Java SE API usage and not a Jakarta migration blocker
- no active source usage was found for:
  - `javax.validation.*`
  - `javax.persistence.*`
  - `javax.servlet.*`
  - `javax.annotation.*`
  - `javax.inject.*`
  - `javax.transaction.*`
- active service code is already aligned to Jakarta APIs for validation and persistence
- `hibernate-validator` remains compatible on the current Boot 4 baseline
- service module compile and test both passed when executed with local reactor dependencies included

Artifacts:

- [`docs/phase-4-jakarta-ee11-review/README.md`](D:\Work\99_Code\data-generator\docs\phase-4-jakarta-ee11-review\README.md)
- [`docs/phase-4-jakarta-ee11-review/service-compile.log`](D:\Work\99_Code\data-generator\docs\phase-4-jakarta-ee11-review\service-compile.log)
- [`docs/phase-4-jakarta-ee11-review/service-test.log`](D:\Work\99_Code\data-generator\docs\phase-4-jakarta-ee11-review\service-test.log)

## Phase 5: Jackson 3 migration

Goal: isolate the highest-risk application-level change in the Boot 4.0 release train.

Why this is a dedicated phase:

- Boot 4.0 adopts Jackson 3 by default.
- This repository has explicit Jackson version management and schema-generation tests.
- YAML and JSON schema generation are active features.

TODO:

- [ ] Review all direct `com.fasterxml.jackson.*` usage in source and tests.
- [ ] Verify `jackson-dataformat-yaml` behavior under Jackson 3.
- [ ] Verify `jackson-datatype-jsr310` behavior under Jackson 3.
- [ ] Re-run and fix:
  - [ ] `JsonSchemaGeneratorTests`
  - [ ] service YAML parsing / generation tests
- [ ] Check whether third-party libraries in this repository still require Jackson 2 compatibility.
- [ ] Remove any no-longer-needed explicit Jackson version pinning after stabilization.

Repository-specific focus:

- [ ] `data-generator-service/src/test/java/org/gensokyo/data/generator/yaml/JsonSchemaGeneratorTests.java`
- [ ] any module using YAML template loading

Validation gate:

- [ ] `.\mvnw-jdk25.ps1 test -Dtest=JsonSchemaGeneratorTests`
- [ ] full `.\mvnw-jdk25.ps1 test`

## Phase 6: Reactor / Netty / WebFlux alignment

Goal: validate all reactive HTTP paths on the new Boot 4.0 baseline.

TODO:

- [ ] Re-check explicit Reactor override in parent `pom.xml`.
- [ ] Verify whether the override is still required under Boot 4.
- [ ] Re-test active WebFlux modules:
  - [ ] `data-generator-service`
  - [ ] `data-generator-reader-ai`
- [ ] Check whether test JVM flags still need:
  - [ ] `-Dio.netty.noUnsafe=true`
  - [ ] `-Xshare:off`
- [ ] Validate that disabling Netty unsafe in tests does not hide real runtime problems.

Validation gate:

- [ ] `.\mvnw-jdk25.ps1 -pl data-generator-service,data-generator-reader\data-generator-reader-ai test`

## Phase 7: Data layer and third-party starter alignment

Goal: validate libraries that are outside the main Spring-managed path.

TODO:

- [ ] Verify `dynamic-datasource-spring-boot-starter` compatibility with Boot 4.0.
- [ ] Verify `druid` compatibility with Boot 4.0 and current test warnings.
- [ ] Verify `mybatis-plus-spring-boot3-starter` replacement path for Boot 4.0.
- [ ] Verify `mybatis-flex` Boot 4-compatible starter path.
- [ ] Verify internal starters:
  - [ ] `org.gensokyo.boot:kafka-spring-boot-starter`
  - [ ] `org.gensokyo.boot:es-spring-boot-starter`
- [ ] Re-check JDBC driver compatibility on JDK 25 + Boot 4:
  - [ ] MySQL
  - [ ] PostgreSQL
  - [ ] ClickHouse
  - [ ] DM JDBC

Repository-specific files:

- [ ] [`pom.xml`](D:\Work\99_Code\data-generator\pom.xml)
- [ ] [`data-generator-service/pom.xml`](D:\Work\99_Code\data-generator\data-generator-service\pom.xml)

Validation gate:

- [ ] `.\mvnw-jdk25.ps1 -pl data-generator-service test`
- [ ] verify the application context still starts

## Phase 8: Elasticsearch and Kafka component review

Goal: validate messaging and search dependencies that are sensitive to Spring release train changes.

TODO:

- [ ] Confirm whether Boot 4.0 still supports the current Elasticsearch client path used here.
- [ ] If needed, plan migration away from legacy high-level REST client usage.
- [ ] Verify Kafka auto-configuration from internal starter still initializes on Boot 4.0.
- [ ] Re-check `data-generator-reader-elasticsearch` and `data-generator-writer-elasticsearch`.
- [ ] Re-check `data-generator-writer-kafka`.

Validation gate:

- [ ] module compile
- [ ] context startup
- [ ] package build

## Phase 9: Test framework and observability cleanup

Goal: finish the Boot 4.0 migration without leaving temporary migration aids behind.

TODO:

- [ ] Temporarily add `spring-boot-properties-migrator` if configuration migration diagnostics are needed.
- [ ] Capture all renamed/removed Boot properties during test startup.
- [ ] Apply config changes in source files instead of keeping the migrator long-term.
- [ ] Remove the migrator after property cleanup is complete.
- [ ] Re-check Boot 4 logging behavior:
  - [ ] UTF-8 default charset expectations
  - [ ] `logback-spring.xml`
- [ ] Re-check Actuator / health probe defaults if Actuator is introduced later.

Validation gate:

- [ ] full `.\mvnw-jdk25.ps1 test`

## Phase 10: Packaging, runtime validation, and rollback readiness

Goal: validate production-facing packaging and keep a clean rollback path.

TODO:

- [ ] Verify `clean package` still produces the expected service tarball.
- [ ] Verify assembly output layout is unchanged unless intentionally modified.
- [ ] Run a smoke startup with the packaged service artifact.
- [ ] Record any runtime-only warnings or property migrations.
- [ ] Document rollback instructions to the last Boot 3.5.x + JDK 25 green commit.

Validation gate:

- [ ] `.\mvnw-jdk25.ps1 -U -DskipTests clean package`
- [ ] packaged smoke run

## Suggested implementation sequence

1. Phase 0
2. Phase 1
3. Phase 2
4. Phase 3
5. Phase 4
6. Phase 5
7. Phase 6
8. Phase 7
9. Phase 8
10. Phase 9
11. Phase 10

## Suggested commit breakdown

1. `docs: add spring boot 4 upgrade plan`
2. `build: move parent BOM to spring boot 4`
3. `fix: align starters and framework 7 APIs`
4. `fix: migrate jackson 3 and web stack`
5. `fix: align data and integration libraries for boot 4`
6. `chore: remove temporary migration aids and finalize packaging`

## Definition of done

The Spring Boot 4.0 upgrade is complete when all of the following are true:

- [ ] parent BOM is on Spring Boot `4.0.x`
- [ ] repository builds on JDK 25
- [ ] full `test` passes
- [ ] full `clean package` passes
- [ ] no temporary migrator dependency remains
- [ ] the remaining warnings are understood and documented
- [ ] this document is updated with final decisions and any deviations from the plan
