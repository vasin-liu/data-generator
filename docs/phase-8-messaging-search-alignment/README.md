# Phase 8 - Messaging and Search Alignment

## Goal

Align the Kafka and Elasticsearch reader/writer wrapper modules to Spring Boot 4 auto-configuration conventions, and verify the current runtime compatibility boundary of the internal messaging/search starters.

## Findings

- The repository-owned wrapper modules can be aligned to Boot 4 auto-configuration successfully.
  - They now use `@AutoConfiguration` instead of plain `@Configuration`.
  - They now publish `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- `org.gensokyo.boot:kafka-spring-boot-starter:2.7.0` is not Boot 4 runtime-compatible as-is.
  - Its auto-configuration path still depends on `org.springframework.boot.autoconfigure.kafka.KafkaProperties`.
  - Boot 4 no longer provides that package location.
- `org.gensokyo.boot:es-spring-boot-starter:2.7.0` is not Boot 4 runtime-compatible as-is.
  - Its property model still depends on `org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties`.
  - Boot 4 no longer provides that package location.
- The Elasticsearch writer path still depends on `RestHighLevelClient`.
  - This remains a legacy API and should be treated as follow-up migration debt beyond the Phase 8 boundary.

## Changes

- Updated Boot 4 auto-configuration registration for:
  - [KafkaWriterConfig.java](D:/Work/99_Code/data-generator/data-generator-writer/data-generator-writer-kafka/src/main/java/org/gensokyo/data/writer/KafkaWriterConfig.java)
  - [EsWriterConfig.java](D:/Work/99_Code/data-generator/data-generator-writer/data-generator-writer-elasticsearch/src/main/java/org/gensokyo/data/writer/EsWriterConfig.java)
  - [EsReaderConfig.java](D:/Work/99_Code/data-generator/data-generator-reader/data-generator-reader-elasticsearch/src/main/java/org/gensokyo/data/reader/EsReaderConfig.java)
- Added Boot 4 auto-configuration import metadata for:
  - [org.springframework.boot.autoconfigure.AutoConfiguration.imports](D:/Work/99_Code/data-generator/data-generator-writer/data-generator-writer-kafka/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
  - [org.springframework.boot.autoconfigure.AutoConfiguration.imports](D:/Work/99_Code/data-generator/data-generator-writer/data-generator-writer-elasticsearch/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
  - [org.springframework.boot.autoconfigure.AutoConfiguration.imports](D:/Work/99_Code/data-generator/data-generator-reader/data-generator-reader-elasticsearch/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
- Added wrapper-level context tests for:
  - [KafkaWriterAutoConfigurationTests.java](D:/Work/99_Code/data-generator/data-generator-writer/data-generator-writer-kafka/src/test/java/org/gensokyo/data/writer/KafkaWriterAutoConfigurationTests.java)
  - [EsWriterAutoConfigurationTests.java](D:/Work/99_Code/data-generator/data-generator-writer/data-generator-writer-elasticsearch/src/test/java/org/gensokyo/data/writer/EsWriterAutoConfigurationTests.java)
  - [EsReaderAutoConfigurationTests.java](D:/Work/99_Code/data-generator/data-generator-reader/data-generator-reader-elasticsearch/src/test/java/org/gensokyo/data/reader/EsReaderAutoConfigurationTests.java)
- Converted the internal starter assertions into explicit compatibility-boundary tests.
  - Kafka now asserts failure on missing Boot 3 `KafkaProperties`.
  - Elasticsearch now asserts failure on missing Boot 3 `ElasticsearchProperties`.

## Validation

Executed with JDK 25 at `E:\Home\vasin.GENSOKYO\sdk\zulu-jdk25.0.1` and local Maven settings `.mvn/settings-jdk25.xml`.

Commands:

```powershell
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-writer\data-generator-writer-kafka,data-generator-writer\data-generator-writer-elasticsearch,data-generator-reader\data-generator-reader-elasticsearch -am test
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-writer\data-generator-writer-kafka,data-generator-writer\data-generator-writer-elasticsearch,data-generator-reader\data-generator-reader-elasticsearch -am -DskipTests package
```

Results:

- module test gate: `BUILD SUCCESS`
- package gate: `BUILD SUCCESS`
- compile note: `data-generator-writer-elasticsearch` still reports deprecated API usage from `ElasticsearchWriter`

Artifacts:

- `docs/phase-8-messaging-search-alignment/phase8-module-test.log`
- `docs/phase-8-messaging-search-alignment/phase8-package.log`

## Conclusion

- Phase 8 is complete for repository-owned wrapper alignment.
- The next real blocker is not wrapper registration; it is the Boot 3 API dependency inside the internal Kafka and Elasticsearch starters.
- A later phase should either upgrade or replace those internal starters, and should also remove the legacy `RestHighLevelClient` path from the Elasticsearch writer stack.
