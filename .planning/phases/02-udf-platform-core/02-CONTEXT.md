# Phase 2: UDF Platform Core - Context

**Gathered:** 2026-06-17
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver a **unified UDF registry** so Template V2 can resolve **published** user-defined functions across three types — `java-plugin`, `script`, and `sql` — with **governance at publish time** and **programmatic registration** for tests and internal callers.

**Requirements in scope:** UDF-01, UDF-02, UDF-03, UDF-04, UDF-07

**Explicitly out of scope (Phase 3+):** Console/API upload and version history UI (UDF-05), template publish-time UDF reference validation (UDF-06), in-repo sample UDFs with harness E2E (UDF-08), JDBC/file persistence for artifacts, Velocity script UDF engine, tenant-scoped registry rows.

**Depends on:** Phase 1 harness (matrix YAML, embedded-first fixtures, `verify-harness.ps1`).

</domain>

<decisions>
## Implementation Decisions

### Registry Storage & Lifecycle
- **D-01:** Phase 2 registry is **in-memory**; programmatic `register` / `publish` / `deprecate` APIs. **JDBC or filesystem persistence** deferred to Phase 3 when Console upload lands.
- **D-02:** Full lifecycle **FSM**: `draft` → `published` → `deprecated`. Only `published` versions are resolvable at runtime.
- **D-03:** Registry **domain model and interfaces** live in `data-generator-common`; **Spring `UdfRegistryService` implementation** in `data-generator-service`.
- **D-04:** UDF artifact **payload stored inline** in memory (`byte[]` for JAR, `String` for script/SQL definition) during Phase 2.
- **D-05:** **Multiple semver versions** may coexist per `udfId`; **reject duplicate** `udfId + version` on register.
- **D-06:** On `publish` / `deprecate`, trigger **`TemplateV2RuntimeRegistry.refresh()`** via existing `RefreshableTemplateV2RuntimeRegistryProvider` path.
- **D-07:** **Merge view** for SQL functions: registry-published UDFs merge into `TemplateV2SqlFunctionRegistry`; **built-in functions win** on name collision.
- **D-08:** Phase 2 registry is **global** (no `tenantId` column); tenant-scoped quotas remain separate (AI P10).

### UDF Type Boundaries
- **D-09:** **`java-plugin`** = PF4J JAR loaded through existing `PathBasedPf4j` / `data.generator.v2-plugin-directories` paths. Registry records plugin metadata and governs publish; does **not** invent a parallel Java UDF classloader contract.
- **D-10:** **`script`** = **callable function** with declared input/output schema, invocable from transform context — **not** an alias for `JsTransformVO` and **not** the pipeline scripter stage.
- **D-11:** **`sql`** = registry entry wrapping existing **`TemplateV2SqlFunction`** (Calcite validation metadata + runtime evaluator); injected into merged SQL function registry on refresh.
- **D-12:** **Script UDF publish** requires **JSON Schema** validation of parameters and return type (UDF-03).
- **D-13:** One PF4J artifact may contribute a **full runtime plugin** (`sqlFunctions()`, `transformFactories()`, etc.); registry tracks the artifact as one or more logical UDF entries per planner discretion.
- **D-14:** Phase 2 script engine: **GraalJS only**. Velocity script UDF deferred.

### ID, Versioning & Resolution
- **D-15:** Stable **`udfId`** format: **reverse DNS** (e.g. `com.example.format_phone`).
- **D-16:** Version format: **strict semver** (`major.minor.patch`).
- **D-17:** When version omitted at resolve time, use **latest `published`** for that `udfId`; **deprecated** versions excluded from latest resolution.
- **D-18:** For `sql` type, **`udfId` ≠ SQL function name**; metadata field **`sqlName`** (e.g. `V2_MY_FUNC`) is what appears in Template V2 SQL transforms.
- **D-19:** **Hard fail** on resolve for `draft` or `deprecated` versions (error code `UDF_DEPRECATED` / `UDF_NOT_PUBLISHED`).
- **D-20:** Each pipeline **job uses a UDF snapshot at run start**; publish refreshes registry for **subsequent** jobs without mutating in-flight runs.

### Governance (UDF-07)
- **D-21:** **Publish gate** runs all governance checks; `draft` registration may be lenient.
- **D-22:** **Plaintext secret scan** follows `TemplateGovernanceSupport` patterns extended for UDF payload paths (no hardcoded passwords, API keys, etc.).
- **D-23:** **Script content blacklist** for dangerous patterns (`Runtime`, `ProcessBuilder`, unauthorized reflection, etc.) aligned with existing GraalJS / `JsTransformFactory` sandbox posture.
- **D-24:** **Java plugin governance**: require PF4J directory isolation; validate JAR manifest basics; reject system-classpath references.
- **D-25:** **Audit events** on `publish` and `deprecate` (actor, timestamp, `udfId`, version) using the **same audit storage** as template publish events.
- **D-26:** Registry/governance failures return **structured errors**: `code`, `field`, `message` (e.g. `UDF_NOT_FOUND`, `UDF_GOVERNANCE_VIOLATION`, `UDF_DUPLICATE_VERSION`) — aligned with `TemplateV2Validator` style for Phase 3 Console display.

### Template Reference Contract (Phase 2 placeholder)
- **D-27:** Define **typed reference contract** for downstream Phase 3 validation (not full validator in Phase 2):
  - **SQL:** templates reference **`sqlName`** directly in SQL transform text.
  - **Script:** transform block uses **`udfRef: { id, version? }`** (version optional → latest published).
  - **Java:** capabilities exposed via PF4J plugin (`sqlName` / transform type) indexed in registry.

### PF4J Backward Compatibility
- **D-28:** **Dual path**: existing `data.generator.v2-plugin-directories` scanning **continues unchanged**; registry is an **additive governance layer**. Samples under `samples/template-v2-pf4j-plugin/` must keep working without registry registration.

### Harness & Test Matrix
- **D-29:** Add **three matrix rows** in `.planning/test-matrix.yaml`: `udf-java-plugin`, `udf-script-graaljs`, `udf-sql-calcite`; link embedded tests proving programmatic register → publish → template resolve (status may start `pending` until tests land).
- **D-30:** Reuse **`data-generator-test-fixtures`** for minimal UDF payloads in embedded tests; extend harness via existing `verify-harness.ps1` fast path.

### Claude's Discretion
- Exact JSON Schema shape for script UDF parameters/returns.
- Specific dangerous-pattern regex list for script governance.
- Whether one PF4J plugin maps to one registry row or one row per contributed SQL function.
- `UdfResolver` SPI naming and package placement within `data-generator-common`.
- Concrete `linked_tests` class names for the three new matrix rows.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & Roadmap
- `.planning/ROADMAP.md` — Phase 2 goal, success criteria, plans 02-01 through 02-03
- `.planning/REQUIREMENTS.md` — UDF-01 through UDF-04, UDF-07 definitions and phase mapping
- `.planning/phases/01-test-harness-foundation/01-CONTEXT.md` — Harness, matrix, fixture decisions (D-01–D-30)

### UDF / Calcite Runtime
- `docs/calcite-implementation-status.md` — `TemplateV2SqlFunctionRegistry`, PF4J `sqlFunctions()` contribution paths
- `docs/template-v2-transformer-strategy.md` — UDF vs transformer taxonomy; SQL-first guidance
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/TemplateV2SqlFunctionRegistry.java` — SQL UDF merge point
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/RefreshableTemplateV2RuntimeRegistryProvider.java` — refresh hook for registry changes
- `samples/template-v2-pf4j-plugin/README.md` — PF4J JAR layout, dual-path boundary, sample SQL UDF

### Governance & Service Patterns
- `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateGovernanceSupport.java` — Plaintext secret scan pattern to extend
- `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2Validator.java` — Structured validation error style
- `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateLifecycleService.java` — DRAFT / PUBLISHED / ARCHIVED precedent
- `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java` — PF4J plugin wiring, `TemplateV2RuntimeRegistry` beans

### Testing
- `docs/testing-embedded-components.md` — Embedded-first policy for UDF registry tests
- `.planning/test-matrix.yaml` — Add UDF rows (D-29)
- `data-generator-test-fixtures/` — Shared YAML/SQL fixtures for embedded UDF scenarios

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`TemplateV2SqlFunctionRegistry`** — Built-in + `with()` merge; registry UDFs should feed this on refresh rather than replacing built-ins.
- **`TemplateV2RuntimePlugin.sqlFunctions()`** — PF4J and built-in plugins already contribute SQL UDFs; Java UDF path should bridge registry-published JARs into this mechanism.
- **`JsTransformFactory` / GraalJS scripter modules** — Sandbox and script execution patterns for script UDF runtime and governance blacklist alignment.
- **`TemplateGovernanceSupport`** — Secret violation collection pattern for UDF-07 payload scanning.
- **`samples/template-v2-pf4j-plugin/`** — Reference PF4J packaging; must remain valid under D-28 dual-path rule.
- **Phase 1 `data-generator-test-fixtures`** — `FixtureTemplates.load`, `H2Seed.apply` for embedded register→run tests.

### Established Patterns
- **Refreshable runtime registry** — Publish-side effects should call `refresh()`, not require service restart.
- **Lifecycle FSM** — Template `DRAFT` / `PUBLISHED` / `ARCHIVED` in service layer; UDF registry mirrors this at publish gate.
- **Embedded-first tests** — H2 + in-memory registry; no production credentials.
- **Matrix-row coverage** — No JaCoCo; link concrete test classes to new UDF matrix rows.

### Integration Points
- `data-generator-common` — New `UdfRegistry`, `UdfRecord`, `UdfType`, lifecycle enums, `UdfResolver` SPI.
- `data-generator-service` — `UdfRegistryService`, governance, audit, Spring bean wiring.
- `data-generator-calcite` — Consume resolved UDFs into `TemplateV2SqlFunctionRegistry` and PF4J locator on refresh.
- `.planning/test-matrix.yaml` — Three new UDF rows consumed by `scripts/verify-harness.ps1`.

</code_context>

<specifics>
## Specific Ideas

- User prefers **Chinese for discussion**; technical artifacts (YAML keys, file paths, code, CONTEXT body) remain **English**.
- **Do not break** existing PF4J plugin directory workflow or `samples/template-v2-pf4j-plugin/` smoke paths.
- Registry is **additive governance**, not a greenfield replacement of Calcite SQL function infrastructure.
- Phase 2 proves **programmatic** register → publish → resolve; Console upload is Phase 3.

</specifics>

<deferred>
## Deferred Ideas

- **Console/API upload, list, version history** — Phase 3 / UDF-05
- **Template publish-time UDF reference validation** — Phase 3 / UDF-06
- **In-repo sample UDFs + harness E2E per type** — Phase 3 / UDF-08
- **JDBC persistence and filesystem artifact store** — Phase 3 when upload API lands
- **Velocity script UDF engine** — post–Phase 2 (UDF-03 mentions Velocity optionally)
- **Tenant-scoped UDF registry rows** — future; global registry for Phase 2
- **Unified `udf://` URI reference syntax** — rejected for Phase 2; typed refs per D-27 instead
- **Service restart required for UDF publish** — rejected; refresh + per-job snapshot instead

</deferred>

---

*Phase: 2-UDF Platform Core*
*Context gathered: 2026-06-17*
