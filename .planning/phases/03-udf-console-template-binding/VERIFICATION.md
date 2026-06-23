# Phase 3 Verification — UDF Console & Template Binding

**Verified:** 2026-06-18
**Result:** PASS (all success criteria met; code-review blockers fixed and re-verified)

## Success Criteria

### 1. Console operator can upload a UDF artifact, publish it, and see it listed with version history — PASS
- `ConsoleUdfController` (`/api/console/udfs`) exposes unified multipart upload, publish, deprecate, list (grouped by `udfId`), and per-id version history (D-05/D-06/D-14).
- Upload assembles the `ScriptUdfPayload` JSON envelope for script/sql so the artifact is actually publishable through the Phase 2 governance gate (fixed during review — see below).
- React `UdfsPage` renders the grouped list with lifecycle tags and inline publish/deprecate, and the type-driven upload modal collects the envelope fields (sqlName/argCount/returnType + input/output schema for script). i18n parity in `en.json`/`zh-CN.json`.
- Persistence via `JdbcUdfRegistry` + `UdfArtifactRepository`/`UdfArtifactPO`; `UdfStartupReloader` rehydrates published UDFs into the runtime on startup (D-01/D-02).
- Evidence: `ConsoleUdfControllerTest` (MockMvc), `ConsoleUdfUploadPublishTests` (real upload→publish against governance + H2), `JdbcUdfRegistryTests`, `UdfStartupReloaderTests`.

### 2. Publishing a template referencing an unknown UDF fails with a clear validation error — PASS
- `TemplateV2Validator.collectUdfReferences` extracts SQL `sqlName` tokens (minus a built-in + structural-keyword allow-list) and script `udfRef:{id,version?}` blocks; `UdfReferenceValidator` resolves them at publish only, raising structured `UDF_NOT_FOUND`/`UDF_NOT_PUBLISHED`/`UDF_DEPRECATED` (D-09–D-12).
- SQL `sqlName` is resolved from the payload envelope — the same source the Calcite runtime registers from — so publish-validation and execution stay consistent (fixed during review).
- Evidence: `UdfReferenceValidatorTests` (7 cases), `TemplatePublishUdfValidationTests` (publish unknown fails, publish known succeeds, draft lenient).

### 3. In-repo sample UDFs (one per type) pass harness E2E when referenced in a template run — PASS
- Samples: `samples/udf-samples/format-phone.js` (+ schema), `mask-email.sql`, README; java-plugin reuses `samples/template-v2-pf4j-plugin/` (D-17/D-18). All credential-free and governance-clean.
- `UdfConsoleTemplateBindingE2ETests` registers → publishes (service path) → references in a Template V2 SQL transform → runs via `TemplateV2Runner` on embedded H2 → asserts `a***@example.com` / `15551234567` (D-19).
- Linked into `udf-sql`/`udf-script`/`udf-java-plugin` matrix rows (D-20); `scripts/verify-harness.ps1` → BUILD SUCCESS, `[SUCCESS] Harness verification passed.`

## Code Review Gate

A review found blockers/highs that the service-path E2E missed (it bypasses the console controller). All fixed at source and re-verified:

1. **Blocker** — console upload stored raw script/SQL bytes; publish + runtime require the `ScriptUdfPayload` JSON envelope. Controller now assembles the envelope (+ sqlName/argCount/returnType/inputSchema/outputSchema params); frontend modal + i18n updated. Guarded by `ConsoleUdfUploadPublishTests`.
2. **High** — `UdfReferenceValidator` read `sqlName` from metadata while the runtime read it from the payload. Both now read the payload envelope.
3. **High** — SQL reference scanner false-positived structural keywords; allow-list extended with `SELECT/FROM/WHERE/JOIN/...`.
4. **Low** — GraalJS warm-up now catches `Throwable`; version ordering uses semver in `JdbcUdfRegistry.list` and `UdfGroupView`.

Also fixed two latent runtime bugs surfaced by the E2E: the Spring runtime plugin now rebinds `SqlTransformFactory` to the merged SQL-function registry (published UDFs were unresolvable at run time), and the GraalJS engine is warmed at startup so the first UDF call no longer trips the per-call timeout.

## Verification Commands
- `.\mvnw-jdk25.ps1 -pl data-generator-service -am -Dtest=ConsoleUdfUploadPublishTests,UdfReferenceValidatorTests,TemplatePublishUdfValidationTests,ConsoleUdfControllerTest,UdfConsoleTemplateBindingE2ETests -Dsurefire.failIfNoSpecifiedTests=false test` → 18 tests, 0 failures.
- `npm run build` (data-generator-console-web) → tsc + vite build clean.
- `scripts/verify-harness.ps1` → BUILD SUCCESS, harness verification passed (UDF E2E included via matrix linkage).

## Requirements
- UDF-05 (upload/list/version-history/publish-deprecate API + persistence) — met
- UDF-06 (publish-time template UDF reference validation) — met
- UDF-08 (sample UDFs + harness E2E) — met
