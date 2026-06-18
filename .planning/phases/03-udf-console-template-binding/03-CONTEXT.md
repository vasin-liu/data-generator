# Phase 3: UDF Console & Template Binding - Context

**Gathered:** 2026-06-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver the **operator-facing UDF lifecycle**: a console/API surface to **upload, list, version, publish, and deprecate** UDF artifacts (java-plugin, script, sql), **persistence** so published UDFs survive restart, **publish-time template validation** that rejects unknown/unpublished UDF references, and **in-repo sample UDFs** (one per type) proven end-to-end through the harness.

**Requirements in scope:** UDF-05 (upload/list/version-history/publish-deprecate API), UDF-06 (template publish-time UDF reference validation), UDF-08 (sample UDFs + harness E2E).

**Builds on Phase 2 (do not re-decide):** lifecycle FSM `draft → published → deprecated` with publish-gate governance (D-02/D-21), typed reference contract D-27, structured error-code style, reverse-DNS `udfId` + strict semver, in-memory registry contract (`UdfRegistry`, `UdfRecord`, `UdfRegistryService`, `UdfPublishService`).

**Explicitly out of scope (later phases):** new built-in transform operators / SQL surface (Phase 4, XFORM-*), coverage ramp + CI merge gate (Phase 5, COV-*), Velocity script UDF engine, tenant-scoped registry rows, AI-assisted UDF authoring.

**Depends on:** Phase 2 (UDF Platform Core) and Phase 1 harness.

</domain>

<decisions>
## Implementation Decisions

### Artifact Persistence
- **D-01:** Persistence backend is a **JDBC table** reusing the existing Spring Data pattern (`TemplatePO` / `TemplateRepository` style under `model/po/` + `repository/`). Metadata and inline payload both persist in the DB; H2 embedded keeps tests credential-free.
- **D-02:** On service startup, **published** UDF records are **reloaded into the registry** and re-enter the Template V2 runtime merge view (via the Phase 2 refresh path), so uploaded UDFs remain usable across restarts without re-upload.
- **D-03:** Registry stays **global** — no `tenantId` column (aligns Phase 2 D-08).
- **D-04:** The Phase 2 in-memory `UdfRegistry` contract is preserved; persistence is introduced behind it (a JDBC-backed `UdfRegistry` implementation or a persistence layer the registry delegates to — exact shape is planner discretion, but the `UdfRegistry`/`UdfRecord` API must not break Phase 2 callers).

### Upload & Publish API
- **D-05:** New dedicated **`ConsoleUdfController` @ `/api/console/udfs`**, resource-style (upload/draft, publish, deprecate, list, version history). Returns the standard **`R<T>`** envelope; client errors raised as `IllegalArgumentException`/structured `UdfRegistryException` handled by `ConsoleApiAdvice`.
- **D-06:** **Unified multipart endpoint** handles all three types — JAR as a file part, script/sql as text fields plus metadata (`sqlName`, schemas) — rather than per-type endpoints.
- **D-07:** **Two-step flow**: upload creates a `draft` → operator reviews → explicit `publish`. Governance gate runs at publish (Phase 2 D-21), not at upload.
- **D-08:** Re-uploading the same `udfId` **requires a new `version`**; duplicate `udfId + version` is rejected (Phase 2 D-05). Older versions are retained as history; `published` records are immutable.

### Template Reference Validation (UDF-06)
- **D-09:** Validation **extends `TemplateV2Validator`** so UDF reference checks run through the same template-validation/governance entry and reuse its structured-error collection.
- **D-10:** Reference detection follows **Phase 2 D-27**: `sqlName` occurrences in SQL transform text and `udfRef:{id, version?}` in script transform blocks. Java/PF4J capabilities are provided by the plugin path and are **not** strictly reference-validated here.
- **D-11:** Validation is **publish-only hard fail** — `draft` template saves tolerate dangling/unpublished references; publishing a template with unknown/unpublished/deprecated UDF refs fails (Phase 2 D-19/D-21).
- **D-12:** Failures use **structured error codes** with `field` — `UDF_NOT_FOUND`, `UDF_NOT_PUBLISHED`, `UDF_DEPRECATED` — aligned with `TemplateV2Validator` style for console display.

### Console UI & RBAC
- **D-13:** New **top-level "UDFs" page** — `data-generator-console-web/src/app/pages/UdfsPage.tsx` + route in `App.tsx` + typed client `src/api/udfs.ts`, peer to Templates/Jobs.
- **D-14:** List is **grouped by `udfId`** with expandable version history, lifecycle status tags (`draft`/`published`/`deprecated`), and inline publish/deprecate actions.
- **D-15:** **RBAC**: upload/publish/deprecate require **OPERATOR or higher**; viewing requires **VIEWER+**. (Console RBAC is default-off but the `X-Console-Role` contract is honored where enabled — UDF mutation is governance-sensitive.)
- **D-16:** Upload form is **type-driven**: java = file drag/drop, script = code editor + schema fields, sql = SQL text + `sqlName`. i18n strings added to `en.json` and `zh-CN.json`.

### Sample UDFs & Harness E2E (UDF-08)
- **D-17:** Sample UDFs (one per type) live under **`samples/`**; harness fixtures reference them.
- **D-18:** The **java-plugin sample reuses/extends `samples/template-v2-pf4j-plugin/`** as the java-plugin UDF, preserving the Phase 2 D-28 dual-path guarantee; script and sql samples are added alongside.
- **D-19:** E2E proof is **embedded integration** — register → publish (via the new API path) → reference in a Template V2 → run → assert output — wired through the existing `scripts/verify-harness.ps1` fast path. (No Playwright UI E2E required for UDF-08; console UI is exercised by unit/MockMvc + the existing console smoke.)
- **D-20:** **Reuse the existing three matrix rows** (`udf-sql`, `udf-script`, `udf-java-plugin`) and extend their `linked_tests` to include the Phase 3 console/template-binding E2E, rather than adding new rows.

### Claude's Discretion
- Concrete JDBC schema/columns for the UDF table and whether large JAR bytes use a separate BLOB column or LOB streaming.
- Whether persistence is a `JdbcUdfRegistry` implementation or a repository the existing registry delegates to (must keep the Phase 2 `UdfRegistry` API stable).
- Exact `ConsoleUdfController` route shapes, DTO names (`api.console.dto`), and pagination of version history.
- Reference-scanning mechanics inside `TemplateV2Validator` (how `sqlName` tokens are extracted from SQL text without false positives).
- Sample UDF package names, the script/sql sample function semantics, and concrete `linked_tests` class names.
- React UDFs page component decomposition and Ant Design table/drawer layout.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & Roadmap
- `.planning/ROADMAP.md` — Phase 3 goal, success criteria, plans 03-01/03-02/03-03
- `.planning/REQUIREMENTS.md` — UDF-05, UDF-06, UDF-08 definitions and phase mapping
- `.planning/phases/02-udf-platform-core/02-CONTEXT.md` — Phase 2 decisions D-01–D-30 (registry, lifecycle, D-27 reference contract, governance, matrix rows)

### UDF Registry (Phase 2 deliverables to extend)
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/udf/UdfRegistry.java` — registry contract to keep stable while adding persistence
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/udf/UdfRecord.java` — versioned entry (inline `byte[]` payload, metadata, timestamps)
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/udf/InMemoryUdfRegistry.java` — current in-memory implementation
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/udf/UdfType.java`, `UdfLifecycleState.java`, `UdfValidationError.java`, `UdfRegistryException.java` — type/state/error model
- `data-generator-service/src/main/java/org/gensokyo/data/udf/UdfRegistryService.java` — Spring facade (register/publish/deprecate/list/find/resolve)
- `data-generator-service/src/main/java/org/gensokyo/data/udf/UdfPublishService.java` — publish-gate (governance → registry → audit → runtime refresh)
- `data-generator-service/src/main/java/org/gensokyo/data/udf/UdfGovernanceSupport.java` — publish-time governance checks
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/udf/RegistryBackedRuntimePluginProvider.java`, `RegistrySqlFunctionSource.java` — registry → runtime merge / refresh path to re-trigger on startup reload

### Persistence pattern to mirror
- `data-generator-service/src/main/java/org/gensokyo/data/model/po/` and `data-generator-service/src/main/java/org/gensokyo/data/repository/` — `*PO` + Spring Data repo precedent (Template persistence)
- `data-generator-service/src/main/resources/application.yaml` — datasource/schema config; check `resources/db/` for schema bootstrap convention

### Console API & Template validation
- `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleJobController.java` — `Console*Controller` + `R<T>` reference style
- `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleUploadController.java` — existing multipart upload precedent (`/api/console/uploads`)
- `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleApiAdvice.java` — exception → `R.fail` mapping (400/500)
- `data-generator-service/src/main/java/org/gensokyo/data/model/vo/R.java` — response envelope
- `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java` — extend for UDF-06 reference validation
- `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateLifecycleService.java` — publish-gate entry point
- `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java` — bean wiring (registry, runtime registry provider)

### Console UI
- `.planning/codebase/STRUCTURE.md` §7 (adding console features) — page/route/api/i18n/E2E locations
- `.planning/codebase/CONVENTIONS.md` §10 — React/AntD conventions, `X-Console-Role` RBAC headers
- `data-generator-console-web/src/app/App.tsx`, `src/app/pages/`, `src/api/`, `src/i18n/locales/{en,zh-CN}.json`

### Samples, Harness & Testing
- `samples/template-v2-pf4j-plugin/README.md` — PF4J packaging to reuse as java-plugin UDF sample (D-28 dual path)
- `.planning/test-matrix.yaml` — existing `udf-sql`/`udf-script`/`udf-java-plugin` rows to extend `linked_tests`
- `scripts/verify-harness.ps1` — embedded harness fast path for the E2E
- `data-generator-test-fixtures/` — `FixtureTemplates.load` / `H2Seed.apply` for embedded register→run tests
- `docs/testing-embedded-components.md` — embedded-first test policy

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`UdfRegistry` / `UdfRecord` / `UdfRegistryService` / `UdfPublishService`** — Phase 2 contract; Phase 3 adds JDBC persistence behind the registry and a console controller above the services.
- **`ConsoleUploadController`** — multipart upload precedent for the unified UDF upload endpoint.
- **`R<T>` + `ConsoleApiAdvice`** — console response/error envelope; `UdfRegistryException` codes map cleanly to `R.fail` with field detail.
- **`TemplateV2Validator`** — structured validation error collector to extend for UDF-06.
- **`RegistryBackedRuntimePluginProvider` / refresh path** — already merges published UDFs into runtime; startup reload (D-02) should drive the same refresh after rehydrating persisted records.
- **`samples/template-v2-pf4j-plugin/`** — reuse as the java-plugin sample.
- **`data-generator-test-fixtures`** — embedded register→publish→run scenario fixtures.

### Established Patterns
- **`*PO` + Spring Data repository** for persistence (mirror Template persistence for the UDF table).
- **`Console*Controller` returning `R<T>`**, errors via `ConsoleApiAdvice`; client errors as `IllegalArgumentException`.
- **Lifecycle FSM at publish gate** (Template `DRAFT/PUBLISHED/ARCHIVED` precedent; UDF mirrors `draft/published/deprecated`).
- **Refreshable runtime registry** — mutations call `refresh()`, no restart needed; startup reload re-enters the merge view.
- **Embedded-first tests + matrix-row linkage** — extend `linked_tests` on existing UDF rows.
- **Console feature locations** — page in `src/app/pages/`, client in `src/api/`, i18n in `en.json`/`zh-CN.json`, E2E in `e2e/specs/`.

### Integration Points
- `data-generator-service` — new `ConsoleUdfController` (`api/console/` + `dto/`), UDF `*PO`/`repository`, persistence-backed registry wiring in `CoreConfig`, `TemplateV2Validator` extension.
- `data-generator-common` — registry contract stability; any shared persistence-facing types if needed.
- `data-generator-calcite` — startup reload must drive the existing registry→runtime refresh.
- `data-generator-console-web` — new UDFs page, API client, i18n, RBAC role gating.
- `samples/` + `.planning/test-matrix.yaml` + `scripts/verify-harness.ps1` — samples and E2E wiring.

</code_context>

<specifics>
## Specific Ideas

- **Discussion language:** user prefers **Chinese** for discussion; all technical artifacts (YAML keys, file paths, code, this CONTEXT body) remain **English** (carried from Phase 2).
- Keep the **Phase 2 `UdfRegistry` API stable** — persistence is additive, not a rewrite.
- Preserve the **PF4J dual-path** guarantee (D-28): the java-plugin sample must keep working through both the directory-scan path and registry publish.
- Governance remains a **publish-time gate** — upload/draft stays lenient; publish enforces.

</specifics>

<deferred>
## Deferred Ideas

- **Playwright console UI E2E for the full upload→publish→run flow** — optional later hardening; Phase 3 proves UDF-08 via embedded integration (D-19). Console UI covered by unit/MockMvc + existing smoke.
- **Audit-endpoint surfacing / dedicated UDF audit view in console** — Phase 2 already records publish/deprecate audit events; a console audit view is not in Phase 3 scope.
- **Delete / rollback of UDF versions** beyond deprecate — not in scope; lifecycle stays `draft → published → deprecated`.
- **Payload size limits / artifact scanning quotas** — not decided this phase; revisit if needed.
- **Tenant-scoped UDF registry rows** — deferred (registry stays global, D-03).
- **Velocity script UDF engine** — deferred (GraalJS only, Phase 2 D-14).
- **New built-in transform operators / SQL surface** — Phase 4 (XFORM-*).

</deferred>

---

*Phase: 3-UDF Console & Template Binding*
*Context gathered: 2026-06-18*
