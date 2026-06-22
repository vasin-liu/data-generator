# Plan 04-04 Summary — Transform Error Surfacing

**Requirement:** XFORM-05 (D-08, D-09, D-10, D-13)
**Wave:** 3
**Commit:** `da43642`
**Status:** Complete — all tests green (6/6)

## Accomplishments

Made transform/UDF failures actionable in both run reports and console job detail, reusing a single structured shape end-to-end.

- **`TransformErrorVO`** (core model): `Serializable` record with `step`, `operatorType`, `operatorName`, `message`, `row`, `column`. Javadoc documents that `message` must arrive already sanitized by the throwing factory (PII-safe).
- **`RunReportVO`** (D-13 additive): new `transformErrors` component, null-normalized in the compact constructor (mirrors the existing `aiCalls` handling). Added a back-compat 7-arg constructor so existing call sites (e.g. `AiUsageServiceTests`) and persisted legacy JSON keep working with zero edits.
- **`RunReportCollector.collectFailure(template, throwable, durationMs)`** (D-08/D-10): walks the exception cause chain, parses the runtime registry's `"... for type [<type>] ..."` wrapper to recover the operator type, matches it against `template.getTransformers()` to build a `transformers[index]` step path, and emits exactly one fail-fast error with the root-cause message. Falls back to `step="transform"` when the type cannot be resolved (never drops the error). Message capped at 2000 chars.
- **`TaskExecutionService.markFailed(id, message, reportJson)`** overload persists the structured report on the failed row; the 2-arg form delegates with `null`.
- **Wiring** in `TaskController.runV2Tracked` and `DistributedJobLeaseRunner.runLease` failure branches: build the failure report and persist via the new overload. The error reaches `JobExecutionDetail.execution().report().transformErrors()` through the existing `toSummary → parseReport` carrier — no new field, no second error shape (D-09).

## Tests

- **`RunReportCollectorFailureTests`** (unit, 4 tests): step-path + operator-type derivation from the runtime wrapper; fallback to `transform` when unresolved; legacy report JSON without `transformErrors` deserializes to an empty list (D-13); PII-safe message assertion.
- **`RunReportPersistenceTests`** (service `@SpringBootTest` H2 slice): new test runs a `mask` transform with an unknown strategy, polls to `FAILED`, and asserts `getByInstanceId(...).report().transformErrors()` carries operator type `mask` + `transformers[0]` step path and the persisted report JSON contains `transformErrors` — proving the failure survives persist → job detail (D-09).

## Deviation (necessary)

Plan 04-02 registered the `json`/`mask`/`lookup` factories only in `DefaultTemplateV2RuntimePlugin`, but the **service** runtime registry is assembled from Spring `V2TransformFactory` beans (`springTemplateV2RuntimePluginProvider`). The new operators had no bean definitions, so the service runtime reported `Unsupported V2 transformer in current runner: MaskTransformVO`. Added `jsonTransformFactory` / `maskTransformFactory` / `lookupTransformFactory` `@Bean`s in `CoreConfig` (mirroring `jsTransformFactory`). This was required for the operators to function end-to-end in the running service and for this plan's failing-transform test to exercise the real runtime path.

## Files

Created: `TransformErrorVO.java`, `RunReportCollectorFailureTests.java`
Modified: `RunReportVO.java`, `RunReportCollector.java`, `TaskExecutionService.java`, `TaskController.java`, `DistributedJobLeaseRunner.java`, `CoreConfig.java`, `RunReportPersistenceTests.java`

## Verification

`.\mvnw-jdk25.ps1 -pl data-generator-service -am test -Dtest=RunReportCollectorFailureTests,RunReportPersistenceTests` → **BUILD SUCCESS**, Tests run: 6, Failures: 0, Errors: 0.
