# Spring Boot 4.0 Upgrade Plan

## Scope

This document defines the next-step plan after the repository has already reached the "JDK 25 minimum viable upgrade complete" state.

Current validated baseline:

- Build JDK: `25.0.1`
- Compiler target: Java `25`
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

## Current status as of 2026-04-28

Repository state verified against current `docs/` artifacts and parent POM:

- parent BOM is already on Spring Boot `4.0.5`
- parent Jackson baseline is already on `tools.jackson.*:3.1.0`
- full repository `test` and full `clean package` have both been validated on JDK `25.0.1`
- packaged service smoke startup has been validated under a local Boot 4-compatible smoke configuration

What remains is no longer the core Boot 4 migration. The remaining work is compatibility debt around a small number of third-party and historical compatibility residues:

- repository-local dynamic Kafka cluster loading is now provided by `data-generator-core`, and `data-generator-writer-kafka` no longer depends on the internal Kafka starter
- repository-local dynamic Elasticsearch cluster loading is now provided by `data-generator-core`, and the reader/writer Elasticsearch modules no longer depend on the internal Elasticsearch starter
- `data-generator-writer-elasticsearch` has been migrated off `RestHighLevelClient` to low-level `RestClient` bulk requests
- repository data-layer modules now use `dynamic-datasource-spring-boot4-starter:4.5.0`, and the temporary datasource shim/bridge path has been removed
- active source/test Jackson 2 compatibility islands have now been removed; remaining Jackson 2 references are limited to annotation APIs and historical documentation artifacts

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
  - Hibernate Validator
  - Druid
  - MyBatis Plus / MyBatis Flex
  - Elasticsearch low-level client
- test runtime customizations already present
  - Mockito javaagent
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
- [x] Compiler baseline is JDK `25` (`maven.compiler.release` in the root build).
- [x] Run the Phase 2 compile gate and capture the output under `docs/`.
- [ ] Confirm Boot 4.0 still manages:
  - [ ] `httpclient5`
  - [ ] Hibernate ORM / Validator
  - [ ] Reactor / Netty

Validation gate:

- [x] `.\mvnw-jdk25.ps1 -DskipTests compile`

Phase 2 decisions for the minimum move:

- Selected target: `Spring Boot 4.0.5`
- Compiler baseline: JDK `25` (`maven.compiler.release` in the root build).
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
- the service smoke test temporarily excluded known-later-phase incompatible auto-configurations so Phase 3 could validate the Boot 4 / Framework 7 core baseline before the later datasource and messaging work landed

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

- [x] Review all direct `com.fasterxml.jackson.*` usage in source and tests.
- [x] Verify `jackson-dataformat-yaml` behavior under Jackson 3.
- [x] Verify `jackson-datatype-jsr310` behavior under Jackson 3.
- [x] Re-run and fix:
  - [x] `JsonSchemaGeneratorTests`
  - [x] service YAML parsing / generation tests
- [x] Check whether third-party libraries in this repository still require Jackson 2 compatibility.
- [ ] Remove any no-longer-needed explicit Jackson version pinning after stabilization.

Repository-specific focus:

- [x] `data-generator-service/src/test/java/org/gensokyo/data/generator/yaml/JsonSchemaGeneratorTests.java`
- [x] any module using YAML template loading

Validation gate:

- [x] `.\mvnw-jdk25.ps1 test -Dtest=JsonSchemaGeneratorTests`
- [x] full `.\mvnw-jdk25.ps1 test`

Phase 5 decisions:

- main Jackson baseline moved to `tools.jackson.*:3.1.0`
- `jackson-datatype-jsr310` is no longer needed on the main Jackson 3 path
- no active Jackson 2 compatibility islands remain in the current source/test tree

Artifacts:

- [`docs/phase-5-jackson3-migration/README.md`](D:\Work\99_Code\data-generator\docs\phase-5-jackson3-migration\README.md)
- [`docs/phase-5-jackson3-migration/phase5-compile.log`](D:\Work\99_Code\data-generator\docs\phase-5-jackson3-migration\phase5-compile.log)
- [`docs/phase-5-jackson3-migration/phase5-test-compile.log`](D:\Work\99_Code\data-generator\docs\phase-5-jackson3-migration\phase5-test-compile.log)
- [`docs/phase-5-jackson3-migration/phase5-test.log`](D:\Work\99_Code\data-generator\docs\phase-5-jackson3-migration\phase5-test.log)

## Phase 6: Reactor / Netty / WebFlux alignment

Goal: validate all reactive HTTP paths on the new Boot 4.0 baseline.

TODO:

- [x] Re-check explicit Reactor override in parent `pom.xml`.
- [x] Verify whether the override is still required under Boot 4.
- [x] Re-test active WebFlux modules:
  - [x] `data-generator-service`
  - [x] `data-generator-reader-ai`
- [x] Check whether test JVM flags still need:
  - [x] `-Dio.netty.noUnsafe=true`
  - [x] `-Xshare:off`
- [x] Validate that disabling Netty unsafe in tests does not hide real runtime problems.

Validation gate:

- [x] `.\mvnw-jdk25.ps1 -pl data-generator-service,data-generator-reader\data-generator-reader-ai test`

Phase 6 decisions:

- removed the parent Reactor version override and returned version management to the Boot BOM
- removed `-Dio.netty.noUnsafe=true` from the standard test JVM path
- removed `-Xshare:off` from the standard test JVM path
- updated the `data-generator-reader-ai` WebFlux request publishing path for Boot 4 compatibility

Artifacts:

- [`docs/phase-6-reactor-netty-webflux/README.md`](D:\Work\99_Code\data-generator\docs\phase-6-reactor-netty-webflux\README.md)
- [`docs/phase-6-reactor-netty-webflux/phase6-test-final.log`](D:\Work\99_Code\data-generator\docs\phase-6-reactor-netty-webflux\phase6-test-final.log)
- [`docs/phase-6-reactor-netty-webflux/phase6-test-no-unsafe.log`](D:\Work\99_Code\data-generator\docs\phase-6-reactor-netty-webflux\phase6-test-no-unsafe.log)
- [`docs/phase-6-reactor-netty-webflux/phase6-test-no-extra-jvm-args.log`](D:\Work\99_Code\data-generator\docs\phase-6-reactor-netty-webflux\phase6-test-no-extra-jvm-args.log)

## Phase 7: Data layer and third-party starter alignment

Goal: validate libraries that are outside the main Spring-managed path.

TODO:

- [x] Verify `dynamic-datasource` starter compatibility and replacement path for Boot 4.0.
- [x] Verify `druid` compatibility with Boot 4.0 and current test warnings.
- [x] Verify `mybatis-plus-spring-boot3-starter` replacement path for Boot 4.0.
- [x] Verify `mybatis-flex` Boot 4-compatible starter path.
- [x] Verify internal starters:
  - [x] `org.gensokyo.boot:kafka-spring-boot-starter`
  - [x] `org.gensokyo.boot:es-spring-boot-starter`
- [x] Re-check JDBC driver compatibility on JDK 25 + Boot 4:
  - [x] MySQL
  - [x] PostgreSQL
  - [x] ClickHouse
  - [x] DM JDBC

Repository-specific files:

- [x] [`pom.xml`](D:\Work\99_Code\data-generator\pom.xml)
- [x] [`data-generator-service/pom.xml`](D:\Work\99_Code\data-generator\data-generator-service\pom.xml)

Validation gate:

- [x] `.\mvnw-jdk25.ps1 -pl data-generator-service test`
- [x] verify the application context still starts

Phase 7 decisions:

- `dynamic-datasource-spring-boot-starter:3.6.1` was not Boot 4 compatible as-is
- repository data-layer modules now use `dynamic-datasource-spring-boot4-starter:4.5.0`
- the temporary service-side exclusion, local datasource shim, and local package-name bridge have all been removed
- `mybatis-plus` and `mybatis-flex` are currently dependency-managed only and were not found to be active runtime blockers in this repository
- JDBC driver class availability for MySQL, PostgreSQL, ClickHouse, and DM has been validated on JDK 25
- internal Kafka and Elasticsearch starter runtime behavior was later removed from the active Kafka/Elasticsearch modules through repository-local Boot 4-native replacements

Artifacts:

- [`docs/phase-7-data-layer-alignment/README.md`](D:\Work\99_Code\data-generator\docs\phase-7-data-layer-alignment\README.md)
- [`docs/phase-7-data-layer-alignment/phase7-service-test-am.log`](D:\Work\99_Code\data-generator\docs\phase-7-data-layer-alignment\phase7-service-test-am.log)
- [`docs/phase-7-data-layer-alignment/phase7-starter-compile.log`](D:\Work\99_Code\data-generator\docs\phase-7-data-layer-alignment\phase7-starter-compile.log)

## Phase 8: Elasticsearch and Kafka component review

Goal: validate messaging and search dependencies that are sensitive to Spring release train changes.

TODO:

- [x] Confirm whether Boot 4.0 still supports the current Elasticsearch client path used here.
- [x] Replace legacy high-level REST client usage with a supported low-level client path.
- [x] Verify Kafka auto-configuration from internal starter still initializes on Boot 4.0.
- [x] Re-check `data-generator-reader-elasticsearch` and `data-generator-writer-elasticsearch`.
- [x] Re-check `data-generator-writer-kafka`.

Validation gate:

- [x] module compile
- [x] context startup
- [x] package build

Phase 8 decisions:

- repository-owned Kafka/Elasticsearch wrapper modules were aligned to Boot 4 auto-configuration conventions
- repository-local dynamic Kafka cluster loading replaced the Boot 3-only internal Kafka starter path in `data-generator-writer-kafka`
- repository-local dynamic Elasticsearch cluster loading replaced the Boot 3-only internal Elasticsearch starter path in `data-generator-reader-elasticsearch` and `data-generator-writer-elasticsearch`
- `data-generator-writer-elasticsearch` now writes through low-level `RestClient` `_bulk` requests instead of `RestHighLevelClient`
- focused Boot 4 context tests passed for Kafka writer, Elasticsearch reader/writer, and service compatibility coverage after the replacement

Artifacts:

- [`docs/phase-8-messaging-search-alignment/README.md`](D:\Work\99_Code\data-generator\docs\phase-8-messaging-search-alignment\README.md)
- [`docs/phase-8-messaging-search-alignment/phase8-module-test.log`](D:\Work\99_Code\data-generator\docs\phase-8-messaging-search-alignment\phase8-module-test.log)
- [`docs/phase-8-messaging-search-alignment/phase8-package.log`](D:\Work\99_Code\data-generator\docs\phase-8-messaging-search-alignment\phase8-package.log)

## Phase 9: Test framework and observability cleanup

Goal: finish the Boot 4.0 migration without leaving temporary migration aids behind.

TODO:

- [x] Temporarily add `spring-boot-properties-migrator` if configuration migration diagnostics are needed.
- [x] Capture all renamed/removed Boot properties during test startup.
- [x] Apply config changes in source files instead of keeping the migrator long-term.
- [x] Remove the migrator after property cleanup is complete.
- [x] Re-check Boot 4 logging behavior:
  - [x] UTF-8 default charset expectations
  - [x] `logback-spring.xml`
- [ ] Re-check Actuator / health probe defaults if Actuator is introduced later.

Validation gate:

- [x] full `.\mvnw-jdk25.ps1 test`

Phase 9 decisions:

- no `spring-boot-properties-migrator` dependency was needed and none was added
- active service configuration did not surface Boot 4 property migration blockers during the full test run
- logging output remains explicitly configured for UTF-8 in `logback-spring.xml`
- template YAML parsing noise was reduced from startup error stacks to a single compatibility summary, and later eliminated after Jackson 3 template codec alignment
- Boot 4 YAML parsing now accepts both historical uppercase and lowercase subtype ids used by repository templates
- current service startup test no longer reports template compatibility warnings; only intentional ignored `!` templates remain in startup logs
- intentional ignored `!` templates are now reported as a single info summary instead of per-file warnings
- `DataSourceController` no longer uses the JDK 25-deprecated `URL(String)` path during dynamic driver loading

Artifacts:

- [`docs/phase-9-test-observability-cleanup/README.md`](D:\Work\99_Code\data-generator\docs\phase-9-test-observability-cleanup\README.md)
- [`docs/phase-9-test-observability-cleanup/phase9-full-test.log`](D:\Work\99_Code\data-generator\docs\phase-9-test-observability-cleanup\phase9-full-test.log)

## Phase 10: Packaging, runtime validation, and rollback readiness

Goal: validate production-facing packaging and keep a clean rollback path.

TODO:

- [x] Verify `clean package` still produces the expected service tarball.
- [x] Verify assembly output layout is unchanged unless intentionally modified.
- [x] Run a smoke startup with the packaged service artifact.
- [x] Record any runtime-only warnings or property migrations.
- [x] Document rollback instructions to the last Boot 3.5.x + JDK 25 green commit.

Validation gate:

- [x] `.\mvnw-jdk25.ps1 -U -DskipTests clean package`
- [x] packaged smoke run

Phase 10 decisions:

- packaging remains centered on `maven-assembly-plugin`
- Spring Boot repackaging is skipped at plugin level in the service module
- packaged smoke startup is valid on Boot 4 on the current repository-local Kafka/Elasticsearch integration path
- Windows `tar` extraction still has a local validation limitation for some non-ASCII archive entries, but package creation itself is not blocked

Artifacts:

- [`docs/phase-10-packaging-runtime-validation/README.md`](D:\Work\99_Code\data-generator\docs\phase-10-packaging-runtime-validation\README.md)
- [`docs/phase-10-packaging-runtime-validation/phase10-clean-package.log`](D:\Work\99_Code\data-generator\docs\phase-10-packaging-runtime-validation\phase10-clean-package.log)
- [`docs/phase-10-packaging-runtime-validation/phase10-smoke.out.log`](D:\Work\99_Code\data-generator\docs\phase-10-packaging-runtime-validation\phase10-smoke.out.log)
- [`docs/phase-10-packaging-runtime-validation/phase10-smoke.err.log`](D:\Work\99_Code\data-generator\docs\phase-10-packaging-runtime-validation\phase10-smoke.err.log)

## Suggested next-step sequence

1. Optionally trim remaining non-functional startup/build noise:
   `DataSourceController` deprecation compile note, Druid init info logging, and Maven/JDK 25 `Unsafe` warnings from third-party tooling.
2. Re-run full `.\mvnw-jdk25.ps1 test` if any further Jackson or logging cleanup touches shared infrastructure.
3. Treat Boot 4 as closed baseline work and move the main implementation focus to Template V2 / Calcite runtime completion.
4. Keep the current repository-local V2 runtime abstractions for pluginization and hot loading:
   `TemplateV2RuntimeContext`, `TemplateV2RuntimeServices`, `TemplateV2RuntimePluginProvider`, `TemplateV2RuntimeRegistryProvider`.
5. Finish built-in V2 runtime capabilities first:
   query-source convergence, sink execution policy, Kafka sink/provider, Elasticsearch sink/provider, AI source path.
   Kafka and Elasticsearch now both have first-pass V2 runtime sink providers; the next built-in runtime gap is AI source execution.
6. Keep PF4J as the default external plugin loading/lifecycle layer, and continue evolving only that external layer without rewriting the full V2 runtime around PF4J-native concepts.

Current blocker for step 2:

- no datasource-specific Boot 4 blocker remains after switching to `dynamic-datasource-spring-boot4-starter:4.5.0`
- no remaining template-parser blocker is known on the service startup path
- the remaining cleanup work is optional log/build noise reduction rather than Boot 4 functional compatibility

Current recommendation for the post-Boot-4 phase:

- keep the current custom V2 runtime abstraction layer
- do not treat the coarse external `ServiceLoader + URLClassLoader` plugin path as the final design; it remains fallback only
- continue the current hybrid evolution where PF4J is already the default external plugin lifecycle/classloading layer and the host/runtime contract remains repository-local

Related V2 planning references:

- [`docs/calcite-implementation-status.md`](D:\Work\99_Code\data-generator\docs\calcite-implementation-status.md)
- [`docs/calcite-refactor-plan.md`](D:\Work\99_Code\data-generator\docs\calcite-refactor-plan.md)
- [`docs/calcite-plugin-framework-evaluation.md`](D:\Work\99_Code\data-generator\docs\calcite-plugin-framework-evaluation.md)

## Suggested remaining commit breakdown

1. `fix: upgrade or replace boot4-incompatible internal kafka starter`
2. `fix: replace boot4-incompatible internal elasticsearch starter path with repository-local dynamic registry`
3. `refactor: migrate elasticsearch writer off rest high level client`
4. `cleanup: remove boot4 smoke exclusions and compatibility shims where possible`

## Definition of done

The Spring Boot 4.0 baseline move is already validated. The remaining Boot 4 completion criteria are:

- [x] parent BOM is on Spring Boot `4.0.x`
- [x] repository builds on JDK 25
- [x] full `test` passes
- [x] full `clean package` passes
- [x] no temporary migrator dependency remains
- [x] the remaining warnings are understood and documented
- [x] this document is updated with final decisions and any deviations from the plan
- [x] service startup no longer depends on excluding Boot 3-only Kafka/Elasticsearch auto-configuration paths
- [x] internal `org.gensokyo.boot` Kafka/Elasticsearch starter paths used by active modules are replaced for native Boot 4 compatibility
- [x] legacy Elasticsearch `RestHighLevelClient` usage is removed
- [x] template YAML startup parsing is clean on the Boot 4 / Jackson 3 path
