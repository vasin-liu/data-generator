# Phase 5 - Jackson 3 Migration

## Goal

Migrate the project's primary JSON/YAML path from Jackson 2 to Jackson 3 under JDK 25 and Spring Boot 4, while keeping unsupported libraries on isolated Jackson 2 compatibility paths.

## Changes

- Upgraded main Jackson dependencies to `tools.jackson.*:3.1.0`.
- Kept `com.fasterxml.jackson.annotation.*` on Jackson 2 annotations, which remain the annotation API used by current dependencies.
- Updated custom SPI integration in `data-generator-common/data-generator-core` for Jackson 3:
  - `SpiSubtypeModule`
  - `SpiSubTypeIdResolver`
  - `ModuleVersion`
- Switched YAML parsing in service module to Jackson 3 builder API.
- Removed obsolete `jackson-datatype-jsr310` usage because Java time support is integrated in Jackson 3.
- Cleaned and repaired corrupted source/test files encountered during migration.

## Compatibility Islands

The following areas remain on Jackson 2 intentionally because upstream dependencies are not yet Jackson 3 compatible:

- `data-generator-faker`
  - uses `com.graphhopper.external:jackson-datatype-jts:2.14`
- `data-generator-reader-ai`
  - keeps Jackson 2 databind for current AI/schema-related code paths
- `data-generator-service` schema-related tests
  - continue using `victools jsonschema` on Jackson 2 model assumptions

## Validation

Executed with:

```powershell
$env:JAVA_HOME='E:\Home\vasin.GENSOKYO\sdk\zulu-jdk25.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -s .mvn\settings-jdk25.xml -DskipTests compile
.\mvnw.cmd -s .mvn\settings-jdk25.xml test
```

Results:

- `compile`: passed
- full `test`: passed

Artifacts:

- `docs/phase-5-jackson3-migration/phase5-compile.log`
- `docs/phase-5-jackson3-migration/phase5-test-compile.log`
- `docs/phase-5-jackson3-migration/phase5-test.log`

## Follow-up

- Replace Jackson 2 compatibility islands when upstream libraries release Jackson 3 support.
- Revisit `victools jsonschema` integration after confirming a Jackson 3 compatible upgrade path.
