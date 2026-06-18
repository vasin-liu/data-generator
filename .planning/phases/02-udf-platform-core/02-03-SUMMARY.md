# Phase 2 Plan 03 Summary

**Plan:** 02-03 — SQL UDF merge + governance + matrix rows  
**Status:** Complete  
**Date:** 2026-06-18

## Delivered

- `DefaultRegistrySqlFunctionSource` (service) — translates every published SQL/SCRIPT registry entry into a `TemplateV2SqlFunction` whose evaluator runs the entry's GraalJS body; wired into the runtime via the registry-backed plugin provider, merged with built-in SQL functions (built-in wins, D-07)
- `UdfGovernanceSupport` (service) — publish-gate checks: plaintext-secret detection (D-20), dangerous script-pattern scan (D-22), JAR manifest validation for Java plugins (D-23), script JSON-Schema presence (D-12); structured error codes (D-27)
- `UdfPublishService` (service) — publish gate orchestration: governance → registry transition → audit log (D-24) → runtime refresh (D-08); deprecate path also audits and refreshes
- `CoreConfig` beans: `GraalJsScriptUdfExecutor`, `RegistrySqlFunctionSource`, `registryBackedTemplateV2RuntimePluginProvider`
- Harness matrix: 3 new rows (`udf-sql`, `udf-script`, `udf-java-plugin`) in `.planning/test-matrix.yaml` (D-30), all `covered`
- Tests: `UdfPublishServiceTests` (6), `UdfGovernanceSupportTests` (3)

## Verification

```
.\mvnw-jdk25.ps1 -pl "data-generator-calcite,data-generator-service" -am test "-Dtest=GraalJsScriptUdfExecutorTests,UdfPublishServiceTests,UdfGovernanceSupportTests,UdfRegistryServiceTests" "-Dsurefire.failIfNoSpecifiedTests=false"
.\scripts\verify-harness.ps1   # BUILD SUCCESS, rows=43, harness verification passed
```

## Requirements

- **UDF-04:** Satisfied (registry SQL UDFs callable through the Template V2 SQL runtime, built-in merge)
- **UDF-07:** Satisfied (publish-gate governance + append-only audit on publish/deprecate)

## Next

- Phase 2 complete — proceed to phase verification / next phase planning
