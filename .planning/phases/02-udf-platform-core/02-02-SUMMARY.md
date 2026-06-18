# Phase 2 Plan 02 Summary

**Plan:** 02-02 — PF4J + GraalJS script UDF runtimes  
**Status:** Complete  
**Date:** 2026-06-18

## Delivered

- `org.gensokyo.data.calcite.udf.GraalJsScriptUdfExecutor` — sandboxed, timeout-bounded GraalJS executor for callable script UDFs (shared engine, host access denied, list-arg binding)
- `org.gensokyo.data.calcite.udf.RegistrySqlFunctionSource` — calcite-side seam for registry-contributed SQL functions
- `org.gensokyo.data.calcite.udf.RegistryBackedRuntimePluginProvider` — bridges published registry UDFs into the Template V2 runtime as a synthetic plugin; re-read on every `createPlugin` so refresh observes registry state (D-08); built-in name collisions dropped so built-ins win (D-07)
- `ScriptUdfPayload` (service) — shared JSON payload parser for SQL/script UDFs with structured error codes (D-27)
- Java plugins record metadata and are governed but load through the existing directory/PF4J path — no parallel classloader (D-09)
- Tests: `GraalJsScriptUdfExecutorTests` (5), schema gate covered via `UdfPublishServiceTests`

## Verification

```
.\mvnw-jdk25.ps1 -pl "data-generator-calcite,data-generator-service" -am test "-Dtest=GraalJsScriptUdfExecutorTests,UdfPublishServiceTests,UdfGovernanceSupportTests,UdfRegistryServiceTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## Requirements

- **UDF-02:** Satisfied (PF4J JAR registration + governance; directory load path preserved, D-09)
- **UDF-03:** Satisfied (GraalJS callable script UDFs with sandbox, timeout, JSON Schema gate)

## Next

- **02-03:** SQL UDF runtime merge, governance, audit, matrix rows
