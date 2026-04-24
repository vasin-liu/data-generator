# Phase 4 Jakarta EE 11 Review

Generated on `2026-04-24`.

## Scope

This phase reviews the active Jakarta EE 11 surface after the Spring Boot `4.0.5` baseline move and confirms that the current repository does not still rely on legacy `javax.*` APIs in Spring-managed application code.

Baseline:

- Spring Boot: `4.0.5`
- Build JDK: `25.0.1`
- Compiler target: Java `17`

## Review result

### `javax.*` source usage

Repository source scanning found only these remaining `javax.*` imports in active code:

- `javax.sql.DataSource`
  - `data-generator-common/data-generator-database-core/.../DbTypeKit.java`
  - `data-generator-iterator/data-generator-iterator-database/.../SqlKit.java`

This is acceptable and expected because `javax.sql` belongs to Java SE, not to the Jakarta EE namespace migration.

No active source usage was found for legacy Jakarta-migrated namespaces such as:

- `javax.validation.*`
- `javax.persistence.*`
- `javax.servlet.*`
- `javax.annotation.*`
- `javax.inject.*`
- `javax.transaction.*`

### Active Jakarta usage already in place

The active Spring Boot application surface is already on Jakarta APIs where required:

- validation
  - `jakarta.validation.constraints.NotBlank`
  - `jakarta.validation.constraints.NotNull`
- persistence
  - `jakarta.persistence.Entity`
  - `jakarta.persistence.Table`
  - `jakarta.persistence.Column`
  - `jakarta.persistence.Id`

### Dependency train observation

The Boot 4 baseline already resolves the expected Jakarta APIs on the build path:

- `jakarta.persistence-api`
- `jakarta.validation-api`

The old `javax.annotation-api` reference only appeared in the archived Phase 0 effective POM snapshot and not in current active source usage.

## Service configuration review

The service module currently uses:

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- explicit `hibernate-validator`
- H2 + Druid + dynamic datasource configuration

No additional Jakarta namespace code changes were required in this phase.

## Validation

Compile:

```powershell
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-service -am -DskipTests compile
```

Result:

- `BUILD SUCCESS`
- finished at `2026-04-24T17:33:54+08:00`
- total time: `28.419 s`

Test:

```powershell
.\mvnw.cmd -s .mvn\settings-jdk25.xml -pl data-generator-service -am test
```

Result:

- `BUILD SUCCESS`
- `Tests run: 25, Failures: 0, Errors: 0, Skipped: 2`
- finished at `2026-04-24T17:37:49+08:00`
- total time: `02:32 min`

## Notes

- The first `-pl data-generator-service` compile attempt failed only because Maven was not instructed to also build local reactor dependencies.
- Re-running with `-am` confirmed there is no Jakarta compatibility blocker in the service module.

## Artifacts

- `service-compile.log`
- `service-test.log`
