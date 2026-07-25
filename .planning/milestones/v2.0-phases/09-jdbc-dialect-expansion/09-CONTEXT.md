# Phase 9: JDBC Dialect Expansion - Context

**Gathered:** 2026-07-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver **RW-05** and **RW-06**: first-class JDBC dialect writers and console datasource presets for Dameng (达梦), Kingbase (金仓), HighGo (翰高), PostgreSQL, and ClickHouse — dialect-correct INSERT plus documented upsert/bulk behavior per engine; console URL/driver hints and connectivity test for these five engines.

**In scope:** Explicit sink `options.dialect` keys for the five engines; upsert SQL for Kingbase/HighGo (PG `ON CONFLICT` path), Dameng (`MERGE INTO`); ClickHouse hard-reject of `upsert: true` with operator docs; publish+run fail-fast for unsupported combinations; complete/correct console driver presets + bundled drivers + connectivity error summaries; layered embedded tests without production credentials; Phase 9 UAT verify script; operator doc updates for per-dialect limits.

**Out of scope (later phases):** Harness P0 matrix rows / CI gate expansion (Phase 10); console dialect capability hints after preset selection; template editor dialect↔datasource preset linking; net-new non-JDBC connectors; exhaustive COPY/bulk for every dialect (ship high-value only, document the rest); requiring licensed DM/Kingbase/HighGo images in default CI.

**Depends on:** Phase 8 writer upsert/streaming patterns (`options.upsert` + `upsertKeys`, PG/MySQL paths, ClickHouse insert/bulk baselines).

</domain>

<decisions>
## Implementation Decisions

### Per-dialect upsert / merge (RW-05)
- **D-01:** Kingbase and HighGo **reuse the PostgreSQL `ON CONFLICT` upsert path** (same SQL generation as Phase 8 PG).
- **D-02:** Dameng upsert uses **`MERGE INTO`**, still configured via `options.upsert: true` and `options.upsertKeys: [...]` (same YAML contract as Phase 8).
- **D-03:** ClickHouse **hard-rejects** `upsert: true` at publish and run; operator docs must describe alternatives (`ReplacingMergeTree` / application-level keys) — no silent insert-as-upsert.
- **D-04:** Unsupported dialect capabilities (missing/invalid `upsertKeys`, CK upsert, etc.) use **dual fail-fast: publish validation + run-time** — same severity as Phase 8 D-14 / governance blocks. Draft save may warn; publish and run block.

### Dialect identity in sink YAML
- **D-05:** Sink YAML **must set explicit `options.dialect`** — no auto-detect-from-URL as the primary path for Phase 9.
- **D-06:** Use **independent dialect keys `kingbase` and `highgo`** in YAML; both **map internally** to the PostgreSQL upsert SQL path (D-01). Do not force operators to write `postgres` for those engines.
- **D-07:** **YAML `dialect` is the source of truth for SQL generation.** Do **not** hard-fail when dialect disagrees with datasource URL/driver family; connectivity tests validate connection only. Document that operators should keep dialect and connection aligned.
- **D-08:** **`dialect: generic` + `upsert: true` → publish + run fail-fast** (tighten vs Phase 8 “ignore upsert on generic”). No silent ignore.

### Console presets & connectivity (RW-06)
- **D-09:** Phase 9 depth: **complete/correct presets** (URL templates, driver class names, bundled flags) for Dameng, Kingbase, HighGo, PostgreSQL, and ClickHouse, and ensure **connectivity test works** for these kinds. **No** new dialect capability-hint UI and **no** template-editor dialect↔preset linking in this phase.
- **D-10:** Proprietary JDBC drivers (**DM / Kingbase / HighGo**) **remain packaged in `jdbc-bundled/`** with presets marked **`bundled: true`** (parity with existing MySQL/PG packaging).
- **D-11:** Connectivity failure messages are **actionable summaries** (host/port/driver class/common causes) — **no passwords and no full JDBC URL** in the message (align with Phase 7 audit summary hygiene).
- **D-12:** Verification: **API/unit tests** for preset catalog correctness + connectivity contracts; **Playwright** covers **at least one** “select preset → fill form → save” path (not five live-engine E2Es).

### Embedded test strategy
- **D-13:** **Layered proof without production credentials:**
  - PostgreSQL & ClickHouse: **Testcontainers** integration tests (read/write / upsert-or-insert as applicable).
  - Kingbase & HighGo: **PostgreSQL container as proxy** for the shared `ON CONFLICT` path **plus** unit tests asserting `dialect=kingbase|highgo` mapping.
  - Dameng: **`MERGE` SQL generation unit tests** primary; optional real DM IT when an image is available.
  - ClickHouse upsert reject: **contract/unit tests** proving fail-fast.
- **D-14:** Real Dameng IT **skipped by default** (`assumeTrue` / env flag); document how to enable (e.g. `-Ddm.it=true` or equivalent) when a DM image/host is available.
- **D-15:** Kingbase/HighGo **PG-proxy + dialect-mapping tests count as fulfilling** the “one read/write scenario per target dialect” success criterion; operator/dev docs must state this explicitly.
- **D-16:** Add **`scripts/verify-phase9-uat-jdbc-dialect.ps1`** following Phase 6/7/8 pattern (Maven dialect slice; optional Podman Playwright; support **`-SkipPlaywright`**).

### Operator documentation
- **D-17:** Update `docs/template-v2-jdbc-sink-guide.md` (and related operator docs) for: `dameng` / `kingbase` / `highgo` / `postgres` / `clickhouse` dialect keys; Dameng MERGE; Kingbase/HighGo → ON CONFLICT; ClickHouse upsert rejection; generic+upsert fail-fast; per-engine bulk/upsert limits (no silent failures).
- **D-18:** Update `AGENTS.md` with the Phase 9 verify script entry when the UAT script lands.

### Claude's Discretion
- Exact MERGE INTO SQL shape for Dameng (column list / bind style) as long as D-02 and fail-fast D-04 hold.
- Internal mapping implementation for `kingbase`/`highgo` → PG upsert generator.
- Optional DM IT flag naming and discovery mechanism.
- Whether PostgreSQL preset polish is drive-by within D-09 or already sufficient.
- Playwright spec file naming (mirror Phase 6/7/8 UAT helpers).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & Roadmap
- `.planning/ROADMAP.md` — Phase 9 goal, success criteria, RW-05/RW-06 mapping
- `.planning/REQUIREMENTS.md` — RW-05, RW-06 full text; out-of-scope note on COPY/bulk for every dialect
- `.planning/PROJECT.md` — v2.0 dialect priority (DM, Kingbase, HighGo, PG, CK)
- `.planning/phases/08-rw-streaming-upsert/08-CONTEXT.md` — Phase 8 upsert contract (`upsert`/`upsertKeys`), PG/MySQL only, CK deferred to Phase 9
- `.planning/phases/06-datasource-platform-core/06-CONTEXT.md` — Catalog / JDBC adapter baseline
- `.planning/phases/07-datasource-governance-hot-reload/07-CONTEXT.md` — connectivity test & audit summary hygiene

### Operator & gap docs
- `docs/template-v2-jdbc-sink-guide.md` — current dialect/upsert/bulk options (extend for Phase 9 engines)
- `docs/calcite-implementation-status.md` — engine ceilings
- `docs/superpowers/specs/2026-06-07-v1-to-v2-native-gap-matrix.md` — dialect upsert partial status
- `docs/testing-embedded-components.md` — embedded-first + Testcontainers patterns
- `.planning/codebase/CONCERNS.md` — dialect writer debt; ClickHouse plain insert only
- `.planning/codebase/INTEGRATIONS.md` — supported JDBC drivers and URL patterns
- `.planning/codebase/STACK.md` — driver artifact versions (dm-jdbc, kingbase, HighGo, clickhouse-jdbc)

### Existing implementation (extend, do not rewrite)
- `data-generator-common/data-generator-database-core/src/main/java/org/gensokyo/data/database/dialect/DialectFactory.java` — DbType → dialect mapping (DM, KINGBASE_ES, HIGH_GO, CLICK_HOUSE already present)
- `data-generator-common/data-generator-database-core/src/main/java/org/gensokyo/data/database/dialect/impl/DmDialectImpl.java`
- `data-generator-common/data-generator-database-core/src/main/java/org/gensokyo/data/database/dialect/impl/ClickhouseDialectImpl.java`
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcBulkWriteExecutor.java` — upsert metrics, ClickHouse insert bulk, fail-fast upsert keys
- `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/support/UpsertParitySupport.java` — PG/MySQL upsert IT pattern to extend
- `data-generator-service/src/main/java/org/gensokyo/data/datasource/JdbcDriverPresetCatalog.java` — console presets (dm8, kingbase8/9, highgo, clickhouse*)
- `data-generator-console-web/src/app/datasources/DriverPresetFields.tsx` — preset UI
- `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleDataSourceController.java` — driverPresets + connectivity
- `scripts/verify-phase8-uat-rw-streaming-upsert.ps1` — UAT script pattern to mirror for Phase 9

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `JdbcDriverPresetCatalog` / `DriverPresetFields`: presets for DM/Kingbase/HighGo/ClickHouse already exist — Phase 9 corrects completeness and validates connectivity, not a greenfield preset UI.
- `JdbcBulkWriteExecutor` + Phase 8 upsert options: extend dialect switch for `dameng` MERGE and `kingbase`/`highgo` → PG path; keep CK reject.
- `UpsertParitySupport` / Testcontainers PG & MySQL ITs: template for PG/CK ITs and Kingbase/HighGo proxy tests.
- `DialectFactory`: DbType routing already includes DM, KINGBASE_ES, HIGH_GO, CLICK_HOUSE — writer SQL generation is the gap, not factory registration alone.

### Established Patterns
- Phase 8: explicit `options.upsert` + `upsertKeys`; publish+run fail-fast; operator docs over silent degradation.
- Phase 6/7/8: `scripts/verify-phaseN-uat-*.ps1` + optional Playwright; embedded-first Maven slice.
- Console connectivity/audit: summary errors without secrets (Phase 7).

### Integration Points
- Calcite JDBC sink option resolution and SQL generation (`JdbcBulkWriteExecutor` / related writer option helpers).
- `TemplateV2Validator` publish checks for dialect+upsert combinations.
- Console datasource create/edit + `ConnectionCatalog.test` path.
- Assembly `jdbc-bundled/` packaging for proprietary drivers.

</code_context>

<specifics>
## Specific Ideas

- Success criteria “one read/write scenario per dialect” is **intentionally satisfied** for Kingbase/HighGo via **PostgreSQL proxy + dialect-key mapping unit tests**, documented as such (D-15).
- ClickHouse remains insert/bulk-capable; upsert is explicitly unsupported in product behavior (D-03), matching Phase 8 doc intent carried into Phase 9.

</specifics>

<deferred>
## Deferred Ideas

- Console **dialect capability hints** after selecting a driver preset (upsert/bulk supported or not).
- Template editor **dialect dropdown linked to datasource preset**.
- Harness **P0/P1 matrix rows and CI merge gate** for dialect paths → Phase 10 (TEST-07, TEST-08).
- Requiring licensed **DM / Kingbase / HighGo** images in default CI.

</deferred>

---

*Phase: 9-JDBC Dialect Expansion*
*Context gathered: 2026-07-21*
