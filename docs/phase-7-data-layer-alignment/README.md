# Phase 7 - Data Layer and Starter Alignment

## Goal

Validate the data-layer path on Spring Boot 4.0.5 and JDK 25, and close the highest-risk compatibility gap in the current starter set.

Status note as of `2026-04-28`: the initial local shim used during Phase 7 has since been removed. The repository now uses the upstream `dynamic-datasource-spring-boot4-starter:4.5.0` path directly, so the temporary bridge code is no longer needed.

## Findings

- `dynamic-datasource-spring-boot-starter:3.6.1` was not Boot 4 compatible as-is.
  - Its auto-configuration metadata still references `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration`.
  - On Boot 4, the class moved to `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`.
- `dynamic-datasource-spring-boot4-starter:4.5.0` is now available and can replace the old Boot 3 starter directly.
- `mybatis-plus` and `mybatis-flex` are present in dependency management only.
  - No active runtime wiring was found in this repository during Phase 7.
- JDBC driver classes for MySQL, PostgreSQL, ClickHouse, and DM are present and loadable under JDK 25.
- The Kafka internal starter is present on the service classpath.
- Elasticsearch internal starter modules compile successfully, but their deeper runtime review remains a Phase 8 item.

## Changes

- Initial Phase 7 stabilization excluded the broken `DynamicDataSourceAutoConfiguration` and used a local replacement configuration to keep the service bootable on Boot 4.
- That temporary shim has now been removed.
- The repository has now been switched to `dynamic-datasource-spring-boot4-starter:4.5.0` in:
  - parent dependency management
  - `data-generator-service`
  - `data-generator-reader-database`
  - `data-generator-writer-database`
- Tightened the service startup test to validate dynamic datasource wiring against a dedicated H2-backed test configuration:
  - [DefaultDataGeneratorApplicationTests.java](D:/Work/99_Code/data-generator/data-generator-service/src/test/java/org/gensokyo/data/generator/DefaultDataGeneratorApplicationTests.java)
  - [application-phase7-test.yaml](D:/Work/99_Code/data-generator/data-generator-service/src/test/resources/application-phase7-test.yaml)
- Added a runtime classpath compatibility test for data-layer drivers and starter support classes:
  - [DataLayerCompatibilityTests.java](D:/Work/99_Code/data-generator/data-generator-service/src/test/java/org/gensokyo/data/generator/DataLayerCompatibilityTests.java)

## Validation

Executed with JDK 25 at `E:\Home\vasin.GENSOKYO\sdk\zulu-jdk25.0.1` and local Maven settings `.mvn/settings-jdk25.xml`.

Commands:

```powershell
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-service -am test
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-writer\data-generator-writer-kafka,data-generator-writer\data-generator-writer-elasticsearch,data-generator-reader\data-generator-reader-elasticsearch -am -DskipTests compile
```

Results:

- service validation: `BUILD SUCCESS`
- summary: `Tests run: 26, Failures: 0, Errors: 0, Skipped: 2`
- internal starter module compile: `BUILD SUCCESS`

Artifacts:

- `docs/phase-7-data-layer-alignment/phase7-service-test-am.log`
- `docs/phase-7-data-layer-alignment/phase7-starter-compile.log`

## Conclusion

- The service data-layer path is now stable on Boot 4 without an application-level exclusion, a repository-local datasource auto-configuration shim, or a local package-name bridge.
- The old `dynamic-datasource-spring-boot-starter:3.6.1` compatibility workaround is no longer part of the active code path.
- Kafka/Elasticsearch starter modules compile, but Elasticsearch runtime behavior still requires the dedicated Phase 8 review.
