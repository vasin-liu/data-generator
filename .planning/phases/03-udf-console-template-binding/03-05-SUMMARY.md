---
phase: 03-udf-console-template-binding
plan: 05
subsystem: testing
tags: [udf, samples, e2e, template-binding, graaljs, calcite, test-matrix]

requires:
  - phase: 03-udf-console-template-binding
    provides: JDBC-backed UdfRegistry (plan 01), Console UDF API + RBAC (plan 02), publish-time UDF reference validation (plan 04)
  - phase: 02-udf-platform-core
    provides: GraalJsScriptUdfExecutor, UdfGovernanceSupport, sqlName metadata contract
provides:
  - in-repo sample UDFs (script + sql; java reuses the PF4J sample)
  - embedded register→publish→reference→run E2E proof for sample UDFs
  - matrix linkage of the E2E into the three existing UDF rows
affects: []

tech-stack:
  added: []
  patterns:
    - "Spring runtime plugin must rebind SqlTransformFactory to the merged SQL-function registry so registry-backed UDF functions resolve at run time"
    - "GraalJS engine warm-up at class load moves one-time cold-start cost off the timed per-call path"

key-files:
  created:
    - samples/udf-samples/README.md
    - samples/udf-samples/format-phone.js
    - samples/udf-samples/format-phone.schema.json
    - samples/udf-samples/mask-email.sql
    - data-generator-service/src/test/java/org/gensokyo/data/udf/UdfConsoleTemplateBindingE2ETests.java
  modified:
    - .planning/test-matrix.yaml
    - data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java
    - data-generator-calcite/src/main/java/org/gensokyo/data/calcite/udf/GraalJsScriptUdfExecutor.java

key-decisions:
  - "Both SQL and script samples reduce to a GraalJS-callable function; sql comments use // since the body is JavaScript"
  - "E2E publishes through UdfRegistryService/UdfPublishService (service path), not direct registry mutation, then runs Template V2 via TemplateV2Runner against embedded H2"
  - "Two real production bugs surfaced by the E2E were fixed at source rather than worked around in the test"

patterns-established:
  - "Pattern: Spring-assembled runtime plugin overrides transformFactories(sqlFunctionRegistry) to rebind the SQL transform factory, mirroring DefaultTemplateV2RuntimePlugin"
  - "Pattern: shared GraalJS Engine warmed once in a static initializer so per-call timeout budgets are not consumed by cold start"

requirements-completed: [UDF-08]

duration: ~3h (incl. multiple full reactor test runs)
completed: 2026-06-18
---

# Phase 3 / Plan 05: Sample UDFs + Embedded E2E + Matrix Rows Summary

**One sample UDF per type ships in-repo and an embedded test proves the full register → publish → reference → run loop; the proof is wired into the three existing UDF matrix rows and passes through the harness fast path.**

## Performance

- **Duration:** ~3h (dominated by full-reactor `@SpringBootTest` runs while diagnosing two latent runtime bugs)
- **Tasks:** 3 (sample assets, embedded E2E, matrix linkage)
- **Files:** 5 created, 3 modified

## Accomplishments
- `samples/udf-samples/`: `format-phone.js` (+ `format-phone.schema.json`) script sample, `mask-email.sql` SQL sample, and a `README.md` documenting `udfId`/`version`/`sqlName` and naming `samples/template-v2-pf4j-plugin/` as the java-plugin sample. All credential-free and governance-clean.
- `UdfConsoleTemplateBindingE2ETests`: registers drafts and publishes both samples through the service path, builds an inline Template V2 SQL transform referencing `V2_MASK_EMAIL` / `V2_FORMAT_PHONE`, runs it via `TemplateV2Runner` against embedded H2, and asserts the concrete transformed output (`a***@example.com`, `15551234567`).
- `.planning/test-matrix.yaml`: appended `UdfConsoleTemplateBindingE2ETests` to the `linked_tests` of `udf-sql`, `udf-script`, and `udf-java-plugin` (no new rows; all retain `status: covered`).
- Harness fast path (`scripts/verify-harness.ps1`) runs the E2E via the matrix linkage and reports BUILD SUCCESS / `[SUCCESS] Harness verification passed.`

## Decisions Made
- See key-decisions in frontmatter.

## Deviations from Plan
Two production bugs were exposed by the E2E and fixed at the source (in scope for "prove the loop end-to-end"):

1. **Registry UDF SQL functions never reached SQL execution under Spring.** `CoreConfig.springTemplateV2RuntimePluginProvider` only overrode the no-arg `transformFactories()`, returning a `SqlTransformFactory` bound to the built-in-only function registry. The merged registry (which includes registry-backed published UDF functions) was therefore ignored at run time, so `V2_MASK_EMAIL` resolved as "No match found". Fixed by overriding `transformFactories(TemplateV2SqlFunctionRegistry)` to rebind the `SqlTransformFactory` to the merged registry, mirroring `DefaultTemplateV2RuntimePlugin`.
2. **GraalJS cold start tripped the per-call timeout.** The first script-UDF invocation in the JVM exceeded the 5s budget due to one-time polyglot initialization, so the first published UDF call after startup always timed out. Fixed by warming the shared `Engine` in a static initializer in `GraalJsScriptUdfExecutor`, moving cold-start cost to class load (Spring startup) off the timed path. Post-fix the E2E script call runs in <2s.

## Issues Encountered
- Harness initially failed with "Could not create the Java Virtual Machine" / `Unrecognized option: --enable-native-access=ALL-UNNAMED` because the shell's `JAVA_HOME` pointed at JDK 8; `verify-harness.ps1` only injects JDK 25 when `JAVA_HOME` is unset. Resolved by exporting `JAVA_HOME` to the JDK 25 path for the session (environment, not a code issue).

## Post-Review Hardening
The phase code-review gate found blockers the service-path E2E could not catch (it bypasses the console controller). All fixed at source and re-verified (see `VERIFICATION.md`):
- `ConsoleUdfController` now assembles the `ScriptUdfPayload` JSON envelope for script/sql (the raw body was unpublishable); added `sqlName`/`argCount`/`returnType`/`inputSchema`/`outputSchema` upload params. `UdfsPage` modal + i18n updated to collect them. New `ConsoleUdfUploadPublishTests` guards the upload→publish contract.
- `UdfReferenceValidator` resolves SQL `sqlName` from the payload envelope (was metadata) to match the runtime; `UdfReferenceValidatorTests`/`TemplatePublishUdfValidationTests` updated accordingly.
- `TemplateV2Validator` allow-list extended with structural SQL keywords (`SELECT/FROM/WHERE/JOIN/...`) to avoid false-positive UDF references.
- `GraalJsScriptUdfExecutor` warm-up catches `Throwable`; `JdbcUdfRegistry.list` and `UdfGroupView` order versions by semver.

## Next Phase Readiness
- The Phase 3 loop (persist → console upload/publish → template binding → run) is proven end-to-end with in-repo samples and exercised by the harness. The runtime fixes also harden real production paths (Spring-assembled SQL UDF resolution, first-call latency, and the console upload→publish contract).

---
*Phase: 03-udf-console-template-binding*
*Completed: 2026-06-18*
