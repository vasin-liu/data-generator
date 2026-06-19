# Phase 4: Transform Operators & SQL - Context

**Gathered:** 2026-06-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver **new built-in Template V2 transform operators + the SQL support they need**, plus **operator discoverability** (catalog metadata API) and **actionable failure surfacing** (run reports + console job detail). Concretely: ship three new built-in operators (`json` parse/flatten, `mask` redaction, `lookup` join), add Calcite scalar functions only where an operator internally needs them, expose a unified operator catalog over an API endpoint, and make transform/UDF failures locate the offending template step.

**Requirements in scope:** XFORM-01 (catalog/metadata API), XFORM-02 (≥3 new built-in operators), XFORM-03 (Calcite scalar functions/types for the operators), XFORM-04 (schema/version notes, no breaking changes), XFORM-05 (actionable error surfacing in run reports + console job detail), XFORM-06 (harness matrix rows per operator).

**Builds on prior phases (do not re-decide):**
- Transform v1 = built-in operators + SQL enhancement only — **no flow-control/DAG** (branch/retry/parallel deferred to a later version; PROJECT.md key decision).
- New operators integrate via the existing pattern: polymorphic `TransformVO` subtype (`type` discriminator) + `V2TransformFactory` (`supports`/`apply`), registered through `JsonSubtypeRegistry` (AutoService). Existing built-ins: `sql`, `js`, `spel`.
- Operators and published UDFs coexist in the transform layer (Phase 2/3); new built-in SQL helper names must not collide with the UDF `sqlName` namespace.
- Embedded-first testing (H2/embedded infra) + matrix-row linkage (Phases 1–3).

**Explicitly out of scope (later phases):** coverage ramp + CI merge gate (Phase 5, COV-*), per-row error tolerance / skip-and-continue pipelines, new Reader/Writer integrations, datasource abstraction refactor, template-level orchestration, console operator-catalog UI page.

**Depends on:** Phase 2 (UDF coexistence in transform layer), Phase 3 (UDF registry/runtime), Phase 1 harness.

</domain>

<decisions>
## Implementation Decisions

### Operator Set (XFORM-02)
- **D-01:** Ship exactly three new built-in operators this phase: **`json`** (parse/flatten), **`mask`** (masking/redaction), **`lookup`** (join helper). `type coerce/cast` and `regex extract/replace` were considered and deferred.
- **D-02:** **`json`** parses a string column to an object and **optionally flattens** nested fields into columns using a separator-named convention (e.g. `addr.city`). Both "parse only" and "parse + flatten" usages are supported by one operator (flatten is opt-in).
- **D-03:** **`mask`** uses **predefined named strategies** selected by name — `email`, `phone`, `credit-card`, `generic-fixed` (keep-pattern fixed-char masking). It is the "no-config" complement to the Phase 3 `mask-email` UDF sample (custom masking still goes through UDFs).
- **D-04:** **`lookup`** joins by key against **another source already declared in the same template** (a named source, e.g. a csv/query source). It does **not** read from a configured JDBC datasource (keeps the phase clear of the deferred datasource work) and does **not** define inline maps as the primary mechanism.

### Catalog Discovery (XFORM-01)
- **D-05:** Discovery is an **API metadata endpoint only** (e.g. `/api/console/transforms`) **+ documentation**. No new console-web page this phase (matches the 3-plan roadmap with no console-web plan).
- **D-06:** The catalog is **unified**: it lists **built-in operators + published UDFs** (SQL and script) in one response, each entry tagged with its source/kind.
- **D-07:** Each catalog entry carries **rich metadata**: `type` name, description, parameter schema (fields + types), and at least one usage example snippet — enough for an operator to author the YAML directly.

### Error Surfacing (XFORM-05)
- **D-08:** Failure entries are **rich**: operator `type`/name + the failing transform step path (e.g. `transformers[2]` / compute-block path) + root-cause message, plus row/field locators when row-level resolvable — enough to fix the template YAML.
- **D-09:** Errors surface in **both** the run report and the console job detail, reusing one structured error shape (extend `RunReportCollector`).
- **D-10:** Failure policy is **fail-fast**: the first transform/UDF error terminates the run and reports the failing step + cause. Per-row tolerance/thresholds are out of scope (deferred).

### SQL Enhancement (XFORM-03)
- **D-11:** **Minimal SQL surface**: operators are standalone transform types; add Calcite scalar functions **only where an operator genuinely needs them internally** (e.g. JSON path extraction backing `json`). No SQL-first reimplementation.
- **D-12:** Internally-added Calcite scalar functions are **internal-only — not listed in the catalog** (catalog granularity stays operator/UDF level). They are documented in notes only and must avoid the UDF `sqlName` namespace.

### Schema/Versioning (XFORM-04)
- **D-13:** New operator `type`s and fields are **additive** to the Template V2 schema — existing templates must keep parsing/running unchanged. Document the new operator fields in schema/version notes rather than bumping a breaking template version.

### Claude's Discretion
- Concrete operator `type` string names, `*TransformVO` field shapes, and factory class placement (within `data-generator-calcite` transform packages, mirroring `JsTransformFactory`/`SqlTransformFactory`).
- The exact catalog endpoint route, DTO names (under `api.console.dto`), and how operator metadata is sourced (derived from registered factories/subtypes vs. authored descriptors).
- Naming/prefix convention for any internal Calcite scalar functions (must not collide with UDF `sqlName`).
- `lookup` mechanics: how the referenced named source is materialized/indexed for key joins, and behavior on missing/duplicate keys (surface via D-08 error contract where it is a failure).
- Concrete `mask` strategy patterns and the harness sample templates per operator.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & Roadmap
- `.planning/ROADMAP.md` — Phase 4 goal, success criteria, plans 04-01/04-02/04-03
- `.planning/REQUIREMENTS.md` — XFORM-01..XFORM-06 definitions and phase mapping
- `.planning/PROJECT.md` — milestone scope; "Transform v1: operators + SQL enhancement (not flow-control DAG)" key decision
- `.planning/phases/03-udf-console-template-binding/03-CONTEXT.md` — UDF runtime/registry decisions operators must coexist with (D-27 reference contract, sqlName namespace)

### Transform Layer (extend these)
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/V2TransformFactory.java` — factory contract (`supports`/`apply`) every new operator implements
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/TransformVO.java` — polymorphic base (`@JsonTypeInfo` by `type`); new operators add subtypes
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/transform/JsTransformFactory.java`, `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/SqlTransformFactory.java`, `.../sql/SpelTransformFactory.java` — reference factory implementations to mirror
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/json/JsonSubtypeRegistry.java` + `data-generator-service/src/main/java/org/gensokyo/data/config/TemplateModelSubtypeRegistrar.java` + `data-generator-service/src/main/java/org/gensokyo/data/json/TemplateObjectMapperFactory.java` — subtype registration path for new `TransformVO` types

### Runtime, Catalog & Errors
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2Runner.java` — V2 execution entry where transforms apply
- `data-generator-service/src/main/java/org/gensokyo/data/task/RunReportCollector.java` — run-report structure to extend for XFORM-05 error entries
- `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleUdfController.java` + `.../api/console/dto/` — `R<T>` console controller/DTO style to mirror for the catalog endpoint
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/udf/DefaultRegistrySqlFunctionSource.java` — published-UDF source to merge into the unified catalog

### Samples, Harness & Testing
- `.planning/test-matrix.yaml` — add a matrix row per new operator (XFORM-06)
- `scripts/verify-harness.ps1` — embedded harness fast path for the operator E2E rows
- `docs/testing-embedded-components.md` — embedded-first test policy
- `.planning/codebase/ARCHITECTURE.md`, `.planning/codebase/STRUCTURE.md`, `.planning/codebase/CONVENTIONS.md` — module boundaries, console feature locations, Java/Javadoc conventions

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `V2TransformFactory` + `TransformVO` polymorphic subtype model — new operators (`json`/`mask`/`lookup`) plug in with no runtime-dispatch changes.
- `JsTransformFactory`/`SqlTransformFactory`/`SpelTransformFactory` — concrete templates for a new factory (`supports` by `type`, `apply` returning a `TransformResult`).
- `JsonSubtypeRegistry` + `TemplateModelSubtypeRegistrar` — register new `TransformVO` subtypes so YAML `type:` resolves them.
- `RunReportCollector` — existing run-report assembly to extend with structured operator/UDF error entries.
- `ConsoleUdfController` + `api.console.dto` + `R<T>` envelope + `ConsoleApiAdvice` — console API style to mirror for the catalog endpoint.
- `DefaultRegistrySqlFunctionSource` / UDF registry list — source of published UDFs to fold into the unified catalog.

### Established Patterns
- Polymorphic Template V2 model via Jackson `@JsonTypeInfo`/`type` discriminator + ServiceLoader subtype registration.
- `Console*Controller` returning `R<T>`; client errors as `IllegalArgumentException`/structured exceptions handled by `ConsoleApiAdvice`.
- Embedded-first tests + matrix-row linkage through `scripts/verify-harness.ps1`.

### Integration Points
- `data-generator-calcite` — new `*TransformVO` subtypes (in core model) + `V2TransformFactory` impls + any internal Calcite scalar functions; wiring through `CoreConfig`/runtime plugin where factories are assembled.
- `data-generator-service` — catalog endpoint (`api/console` + `dto`), run-report error surfacing in `RunReportCollector`, subtype registration.
- `data-generator-console-web` — none this phase (API-only discovery; D-05).
- `.planning/test-matrix.yaml` + `scripts/verify-harness.ps1` — per-operator harness rows.

</code_context>

<specifics>
## Specific Ideas

- `json` flatten uses a separator-named column convention (e.g. `addr.city`) — both parse-only and parse+flatten from one operator.
- `mask` ships named strategies: `email`, `phone`, `credit-card`, `generic-fixed`; positioned as the no-config complement to the Phase 3 `mask-email` UDF sample.
- `lookup` references an existing in-template named source (e.g. csv/query) and joins by key — deliberately avoids JDBC datasource coupling.
- Catalog is API-only and unified (built-in + UDF), rich per-entry metadata (type + description + param schema + example).
- Fail-fast error policy with step-path + root-cause locators in both run report and console job detail.

</specifics>

<deferred>
## Deferred Ideas

- `type coerce/cast` and `regex extract/replace` built-in operators — viable future built-ins, not in this phase's set (D-01).
- Per-row error tolerance / skip-and-continue with thresholds — bigger pipeline-behavior feature; deferred (D-10 keeps fail-fast).
- Console operator-catalog UI page — discovery is API-only this phase (D-05); a console page could come later.
- First-class SQL scalar functions usable directly in `sql` transforms (e.g. `JSON_EXTRACT`, `MASK`) and listing them in the catalog — kept internal-only this phase (D-11/D-12).
- `lookup` against configured JDBC datasources/tables — excluded to avoid the deferred datasource work (D-04).

</deferred>

---

*Phase: 4-Transform Operators & SQL*
*Context gathered: 2026-06-19*
