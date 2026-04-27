# Phase 7 - Data Layer and Starter Alignment

## Goal

Validate the data-layer path on Spring Boot 4.0.5 and JDK 25, and close the highest-risk compatibility gap in the current starter set.

## Findings

- `dynamic-datasource-spring-boot-starter:3.6.1` is not Boot 4 compatible as-is.
  - Its auto-configuration metadata still references `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration`.
  - On Boot 4, the class moved to `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`.
- `mybatis-plus` and `mybatis-flex` are present in dependency management only.
  - No active runtime wiring was found in this repository during Phase 7.
- JDBC driver classes for MySQL, PostgreSQL, ClickHouse, and DM are present and loadable under JDK 25.
- The Kafka internal starter is present on the service classpath.
- Elasticsearch internal starter modules compile successfully, but their deeper runtime review remains a Phase 8 item.

## Changes

- Excluded the broken `DynamicDataSourceAutoConfiguration` from the Boot 4 application bootstrap.
- Added a local Boot 4 compatibility configuration:
  - [Boot4DynamicDataSourceConfiguration.java](D:/Work/99_Code/data-generator/data-generator-service/src/main/java/org/gensokyo/data/config/Boot4DynamicDataSourceConfiguration.java)
  - Reuses the existing `dynamic-datasource` routing classes and datasource creators.
  - Registers a `DynamicDataSourceProvider` bean for Spring-managed initialization.
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

- The service data-layer path is now stable on Boot 4 with the local compatibility shim.
- The current `dynamic-datasource` starter should still be treated as an upstream incompatibility that can be removed once a native Boot 4-compatible release is available.
- Kafka/Elasticsearch starter modules compile, but Elasticsearch runtime behavior still requires the dedicated Phase 8 review.
