# Phase 2 Boot 4 BOM Move

Generated on `2026-04-24`.

## Scope

This phase performs the minimum parent BOM move from Spring Boot `3.5.13` to `4.0.5` while keeping the repository on:

- Build JDK: `25.0.1`
- Compiler target: Java `25`

## Changes applied

- Updated parent `spring-boot.version` to `4.0.5`
- Kept existing explicit overrides in place for the first compile gate:
  - `jackson.version=2.21.2`
  - `reactor.version=3.7.14`
  - `hibernate-validator.version=8.0.1.Final`
  - `lombok.version=1.18.44`
  - `classgraph.version=4.8.184`
  - `mockito.version=5.17.0`
- Added a direct `jackson-datatype-jsr310` dependency to `data-generator-reader-ai`
  - reason: after the Boot 4 BOM move, `ModelOptionsUtils` still imports `com.fasterxml.jackson.datatype.jsr310.JavaTimeModule`, but the module no longer received that datatype jar on its compile classpath transitively

## Validation

Command:

```powershell
.\mvnw.cmd -s .mvn\settings-jdk25.xml -DskipTests compile
```

Result:

- `BUILD SUCCESS`
- completed at `2026-04-24T16:16:47+08:00`
- total time: `02:23 min`

## Artifacts

- `boot4-compile.log`
  - full compile log for the Boot 4 BOM move gate

## Known remaining follow-up

- Boot 3-specific starter replacement is still pending:
  - `mybatis-plus-spring-boot3-starter`
  - `mybatis-flex-spring-boot3-starter`
- Jackson 3 functional migration is still pending even though the compile gate now passes
- Boot-managed dependency ownership still needs deeper cleanup in later phases
