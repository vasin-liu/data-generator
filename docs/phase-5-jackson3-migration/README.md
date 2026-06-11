# Phase 5 - Jackson 3 Migration

## Goal

Migrate the project's primary JSON/YAML path from Jackson 2 to Jackson 3 under JDK 25 and Spring Boot 4, then remove temporary Jackson 2 compatibility paths once the repository no longer needs them.

## Changes

- Upgraded main Jackson dependencies to `tools.jackson.*:3.1.0`.
- Kept `com.fasterxml.jackson.annotation.*` on Jackson 2 annotations, which remain the annotation API used by current dependencies.
- Updated custom SPI integration in `data-generator-common/data-generator-core` for Jackson 3:
  - `SpiSubtypeModule`
  - `SpiSubTypeIdResolver`
  - `ModuleVersion`
- Switched YAML parsing in service module to Jackson 3 builder API.
- Removed obsolete `jackson-datatype-jsr310` usage because Java time support is integrated in Jackson 3.
- Replaced the `data-generator-faker` GeoJSON loader with a Jackson 3 + JTS-core implementation and removed the `jackson-datatype-jts` compatibility path.
- Moved `data-generator-reader-ai` runtime JSON/object-mapping utilities to Jackson 3 and removed unused schema-generation helpers from the main source set.
- Cleaned and repaired corrupted source/test files encountered during migration.

## Compatibility Islands

There are no remaining active Jackson 2 compatibility islands in the current source or test tree.

Residual notes:

- `com.fasterxml.jackson.annotation.*` remains the annotation API exposed by several dependencies; this is expected and does not imply a Jackson 2 databind/runtime path.
- Historical `docs/` artifacts still contain pre-migration Jackson 2 and `victools` references captured from earlier phases.

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

- Remove no-longer-needed explicit parent Jackson version pinning once the repository baseline is considered fully stabilized.
- Keep historical `docs/` artifacts as-is unless a later documentation cleanup wants to distinguish archived outputs from current-state guidance more aggressively.
