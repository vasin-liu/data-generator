# Phase 4 Verification — Transform Operators & SQL

**Verified:** 2026-06-22
**Result:** PASS (all success criteria met; full `data-generator-service` module gate green)

## Success Criteria

### 1. New built-in transform operators (`json`, `mask`, `lookup`) exist and run end-to-end — PASS
- VOs added in `model.v2` with `@JsonSubType` + `@AutoService(TransformVO.class)` polymorphic registration (04-01): `JsonTransformVO`, `MaskTransformVO` + `MaskRuleVO`, `LookupTransformVO`. Additive to the Template V2 schema; legacy templates and `SqlTransformVO` unaffected.
- `V2TransformFactory` implementations (04-02): `JsonTransformFactory` (parse + opt-in flatten), `MaskTransformFactory` (named PII-safe strategies), `LookupTransformFactory` (in-template join enrichment). Registered in `DefaultTemplateV2RuntimePlugin` and — surfaced during 04-04 verification — as Spring `V2TransformFactory` beans in `CoreConfig` (the service runtime registry is bean-driven, not plugin-list driven).
- Evidence: `OperatorTransformSubtypeTests`, `JsonTransformFactoryTests`, `MaskTransformFactoryTests`, `LookupTransformFactoryTests`, and embedded end-to-end `FixtureTransform{Json,Mask,Lookup}ExampleTests` via `TemplateV2Runner`.

### 2. SQL surface enhancement (XFORM-03) — PASS
- Internal `V2_JSON_EXTRACT(json, path)` Calcite scalar function (`TemplateV2JsonSqlFunctions`) registered in `TemplateV2SqlFunctionRegistry` under the reserved `V2_` prefix. Intentionally NOT exposed in the `/api/console/transforms` catalog (D-12).
- Evidence: `TemplateV2JsonSqlFunctionTests`.

### 3. Unified operator discoverability catalog API (XFORM-04) — PASS
- `GET /api/console/transforms` (`ConsoleTransformCatalogController`) merges authored built-ins (`BuiltinTransformCatalog`) with published UDFs (`TransformCatalogSource` over `UdfRegistryService`); excludes non-published UDFs and internal `V2_*` scalar functions; supports a `kind` filter (`BUILTIN`/`UDF`) with 400 on unknown.
- Evidence: `ConsoleTransformCatalogControllerTest` (MockMvc slice).

### 4. Actionable transform/UDF failure surfacing (XFORM-05) — PASS
- `TransformErrorVO` (step, operatorType, operatorName, message, row, column) + additive, null-normalizing `RunReportVO.transformErrors` (D-13 back-compat). `RunReportCollector.collectFailure` derives the operator type + `transformers[index]` step path from the runtime's wrapped `IllegalStateException` and emits one fail-fast, PII-safe error. Persisted via `TaskExecutionService.markFailed(id, message, reportJson)` from both `TaskController` and `DistributedJobLeaseRunner`; reaches console job detail through the existing `report` carrier (no new field, D-09).
- Evidence: `RunReportCollectorFailureTests`, `RunReportPersistenceTests` (failing-transform → FAILED → structured error in job-detail carrier).

### 5. Documentation + harness coverage (XFORM-04, XFORM-06) — PASS
- `docs/transform-operators.md`: operator reference, additive schema/version note (D-13), internal `V2_JSON_EXTRACT` note (D-12).
- `.planning/test-matrix.yaml`: `transform-json`/`transform-mask`/`transform-lookup` rows (status `covered`) linked to the fixture tests.

## Module Gate

`.\mvnw-jdk25.ps1 -pl data-generator-service -am test` → **285 tests, 0 failures, 0 errors** (after fix below).

Pre-existing gate fix: the `Pf4jRuntimeConfigTests` `ApplicationContextRunner` slice hand-mocks `CoreConfig` dependencies and lacked `UdfArtifactRepository` (Phase 3 `jdbcUdfRegistry`) and `UdfRegistryService` (Phase 4 `transformCatalogSource`). Both mocks were added, restoring the slice.

## Deviations
- `data-generator-core` gained a `junit-jupiter` test dependency (04-01) so subtype round-trip tests can compile.
- `CoreConfig` gained `json`/`mask`/`lookup` `V2TransformFactory` beans (04-04) — required for the service runtime to resolve the new operators (the plugin-list registration in 04-02 only feeds the standalone/calcite path).

## Verification Commands
- `.\mvnw-jdk25.ps1 -pl data-generator-calcite -am test -Dtest=JsonTransformFactoryTests,MaskTransformFactoryTests,LookupTransformFactoryTests,TemplateV2JsonSqlFunctionTests` → green (04-02).
- `.\mvnw-jdk25.ps1 -pl data-generator-service -am test` → BUILD SUCCESS, 285/285.
- `.\mvnw-jdk25.ps1 -pl data-generator-test-fixtures -am test -Dtest=FixtureTransformJsonExampleTests,FixtureTransformMaskExampleTests,FixtureTransformLookupExampleTests` → green (04-05).

## Requirements
- XFORM-01 (operator set: json/mask/lookup) — met
- XFORM-02 (operator runtime behavior) — met
- XFORM-03 (SQL surface enhancement: V2_JSON_EXTRACT) — met
- XFORM-04 (operator catalog API + docs) — met
- XFORM-05 (actionable failure surfacing) — met
- XFORM-06 (harness matrix rows + embedded E2E) — met
