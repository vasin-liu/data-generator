# Phase 10 - Packaging, Runtime Validation, and Rollback Readiness

## Goal

Validate the production-facing package output on Spring Boot 4.0.5 and JDK 25, confirm that the packaged service still starts, and record the remaining runtime caveats.

## Findings

- Full reactor `clean package` now succeeds on JDK 25.
- The service module still produces the expected distribution artifacts under `target/`:
  - thin runtime jar
  - assembly `tar.gz`
- The service distribution layout remains aligned with the existing assembly model:
  - `bin/`
  - `conf/`
  - `lib/`
- The packaged service can start successfully from the assembled runtime layout when the known Kafka Boot 4 blocker is excluded in smoke configuration.
- The current Windows `tar` tool fails when extracting the full archive because some packaged template file names contain non-ASCII characters.
  - This did not block package creation.
  - It only affected local full-archive extraction during validation.

## Changes

- Updated the service packaging plugin configuration so Spring Boot repackaging is skipped at plugin level:
  - [pom.xml](D:/Work/99_Code/data-generator/data-generator-service/pom.xml)
  - The service continues to use `maven-assembly-plugin` as the single packaging path.
- Added the Phase 10 validation record:
  - [README.md](D:/Work/99_Code/data-generator/docs/phase-10-packaging-runtime-validation/README.md)

## Validation

Executed with JDK 25 at `E:\Home\vasin.GENSOKYO\sdk\zulu-jdk25.0.1` and local Maven settings `.mvn/settings-jdk25.xml`.

Commands:

```powershell
.\mvnw.cmd -s .mvn\settings-jdk25.xml -U -DskipTests clean package
```

Smoke validation:

- built package: `data-generator-service\target\data-generator-service.tar.gz`
- extracted runtime subset for local verification:
  - `bin/`
  - `lib/`
  - `conf/application.yaml`
  - `conf/logback-spring.xml`
  - `conf/db`
  - `conf/META-INF`
- started with a local smoke config backed by H2 and excluding the known Kafka Boot 4 blocker

Results:

- full package gate: `BUILD SUCCESS`
- packaged service smoke startup: `Started DataGeneratorApplication`

## Runtime Notes

- The smoke startup still logs the existing Druid warning:
  - `testWhileIdle is true, validationQuery not set`
- The smoke config had to exclude:
  - `org.gensokyo.boot.kafka.MultipleKafkaAutoConfiguration`
  - `org.gensokyo.data.writer.KafkaWriterConfig`
- This is consistent with the Phase 8 conclusion that Kafka integration is still bounded by the non-Boot-4-compatible internal starter path.

## Rollback Readiness

- The last documented Boot 3.5.x + JDK 25 green baseline remains:
  - [phase-0-jdk25-baseline README](D:/Work/99_Code/data-generator/docs/phase-0-jdk25-baseline/README.md)
- If rollback is required, use the Phase 0 documented baseline artifacts and the branch point that predates the Spring Boot 4 BOM move in Phase 2.

## Artifacts

- `docs/phase-10-packaging-runtime-validation/phase10-clean-package.log`
- `docs/phase-10-packaging-runtime-validation/phase10-smoke.out.log`
- `docs/phase-10-packaging-runtime-validation/phase10-smoke.err.log`

## Conclusion

- Phase 10 is complete.
- The repository now passes both full `test` and full `clean package` on Spring Boot 4.0.5 + JDK 25.
- The packaged service is startup-valid under a local smoke configuration.
- The remaining runtime caveat is still the Kafka internal starter compatibility boundary already documented in Phase 8.
