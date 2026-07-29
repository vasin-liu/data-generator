# Phase 14: Resolver Ownership Docs - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning
**Mode:** --auto (recommended defaults selected in one pass)

<domain>
## Phase Boundary

Give maintainers a clear **ownership model** and **call-site inventory** for the dual JDBC resolvers — `JdbcCatalogResolver` (catalog-side / datasource-module helper) vs `DefaultRuntimeJdbcEndpointResolver` (V2 Template execute-path authority implementing `RuntimeJdbcEndpointResolver`) — **without merging them** in this milestone.

This phase is **documentation + inventory only** (RES-01). It does **not** implement RES-02 consolidation, change Spring wiring, rewrite product code, or promote any P0 harness change.

</domain>

<decisions>
## Implementation Decisions

### Doc location & packaging
- **D-01:** Deliver a **single focused ownership doc** under `docs/` (recommended name: `docs/jdbc-resolver-ownership.md`), plus **one AGENTS.md Commands/docs pointer** (same pattern as Phase 13 Dameng recipe entry — comment + path).
- **D-02:** Do **not** bury the ownership model only inside class Javadoc or only in `.planning/` — maintainers need a durable product-doc home. Existing class Javadoc on `DefaultRuntimeJdbcEndpointResolver` may be lightly cross-linked to the new doc if needed, but the doc is the source of truth for ownership narrative.

### Call-site inventory shape
- **D-03:** Inventory is a **Markdown table set** in the ownership doc, grouped by role:
  1. **Execute-path production** — `DefaultRuntimeJdbcEndpointResolver` / `RuntimeJdbcEndpointResolver` consumers (`QuerySourceFactory`, `JdbcSinkFactory` / `JdbcRowSinkAdapter`, PostGIS factories, `CoreConfig` bean wiring, HTTP/`TemplateV2Runner` path).
  2. **Catalog-side** — `JdbcCatalogResolver` itself and any production callers (scout notes: may be **test-heavy / sparsely wired** — document honestly if production call sites are absent or limited).
  3. **Tests & stubs** — `JdbcCatalogResolverTests`, `JdbcSnapshotExecutePathIT`, `NoopRuntimeJdbcEndpointResolver` usages, calcite unit/IT stubs.
- **D-04:** Inventory must be **rg/code-derived** (file:symbol level), not hand-waved. Prefer current `main` + representative `src/test` rows; do not invent callers.
- **D-05:** Include **HTTP /task/run** and Phase 12 execute-path proof as the primary “run path” narrative for the execute-path resolver (Phase 12 D-11 deferred ownership docs here).

### Ownership narrative depth
- **D-06:** Ownership section must explain:
  - Catalog-side vs execute-path authority (who is intended for what)
  - That the two **coexist by design** and do **not** currently delegate to each other
  - **`snap:{instanceId}:…` run-start snapshot routing** as part of the execute-path story (DS-03 / `DefaultRuntimeJdbcEndpointResolver` Javadoc) — this is in-scope documentation depth for Phase 14, not a new feature
- **D-07:** Do **not** require a line-by-line algorithm diff of both classes unless needed to clarify ownership; prefer roles, boundaries, and call sites. A short “similar catalog-resolve / register-if-absent semantics; independent implementations” note is enough.

### RES-02 deferral framing
- **D-08:** Include a short **Deferred: RES-02** section: consolidation into a single authority is **out of scope** for v2.1; document what “consolidation later” would mean at a high level (single authority / remove duplication) **without** a migration plan, tickets, or code sketches that look like implementation.
- **D-09:** Explicit non-goals in the doc: no Spring bean merge, no deleting either class, no P0/test-matrix changes, no behavior changes.

### Claude's Discretion
- Exact filename under `docs/` if `jdbc-resolver-ownership.md` collides or a better existing home is found during research (must stay a single primary maintainer-facing doc)
- Exact inventory table columns (path / symbol / role / notes)
- Whether to add a one-line cross-link from `docs/template-v2-datasource-and-secret-governance.md` if research finds it is the natural sibling doc
- How thoroughly to list every `NoopRuntimeJdbcEndpointResolver` test usage (may summarize as “calcite tests use Noop stub; see rg” rather than dozens of rows)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — RES-01 (active), RES-02 (deferred beyond v2.1)
- `.planning/ROADMAP.md` — Phase 14 goal and success criteria
- `.planning/PROJECT.md` — dual JDBC resolvers ownership split; docs + inventory only
- `.planning/STATE.md` — tech_debt row: dual JDBC resolver consolidation → docs-only RES-01
- `.planning/milestones/v2.0-MILESTONE-AUDIT.md` — tech_debt note on duplicate JDBC resolvers
- `.planning/research/SUMMARY.md` — hardening milestone; dual-resolver ownership docs in scope

### Prior phase decisions
- `.planning/phases/12-http-execute-path-proof/12-CONTEXT.md` — D-11: snap: ownership docs deferred to Phase 14
- `.planning/phases/13-dameng-live-path-nyquist-hygiene/13-CONTEXT.md` — dual-resolver consolidation deferred to Phase 14 / RES-02

### Source of truth classes
- `data-generator-datasource/data-generator-datasource-jdbc/src/main/java/org/gensokyo/data/datasource/jdbc/JdbcCatalogResolver.java` — catalog-side helper
- `data-generator-service/src/main/java/org/gensokyo/data/config/DefaultRuntimeJdbcEndpointResolver.java` — V2 execute-path authority (+ existing ownership Javadoc)
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/RuntimeJdbcEndpointResolver.java` — execute-path SPI
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/NoopRuntimeJdbcEndpointResolver.java` — test/noop stub
- `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java` — `RuntimeJdbcEndpointResolver` bean wiring
- `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/JdbcSnapshotExecutePathIT.java` — snap: execute-path proof referencing the SPI

### Related docs (sibling, not replacements)
- `docs/template-v2-datasource-and-secret-governance.md` — governance context; optional cross-link only
- `AGENTS.md` — Commands / docs pointer home for maintainers

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DefaultRuntimeJdbcEndpointResolver` already documents execute-path vs `JdbcCatalogResolver` coexistence and snap: routing in class Javadoc — Phase 14 should **promote** that into maintainer docs + inventory, not invent a conflicting story
- `CoreConfig.runtimeJdbcEndpointResolver` wires the execute-path bean
- `JdbcSnapshotExecutePathIT` and Phase 12 HTTP proofs exercise the execute-path resolver story
- Phase 13 AGENTS.md + `docs/` recipe pattern is the packaging precedent for a single maintainer-facing doc + Commands pointer

### Established Patterns
- Docs-only phases still commit via conventional commits and must not touch P0/`verify-harness` / test-matrix unless explicitly required (here: **do not touch**)
- Honest inventories: if `JdbcCatalogResolver` has few/no production Spring injection sites, say so — do not fabricate callers

### Integration Points
- Execute path: calcite `QuerySourceFactory` / `JdbcSinkFactory` / `JdbcRowSinkAdapter` / PostGIS factories → `RuntimeJdbcEndpointResolver`
- Catalog path: datasource-jdbc module `JdbcCatalogResolver` (+ tests)
- HTTP run spine: service `TaskController` / Template V2 runner stack (document as consumer of execute-path resolution, not as a third resolver)

</code_context>

<specifics>
## Specific Ideas

- Recommended doc title tone: “ownership + inventory”, not “refactor plan”
- Scout note for planner/researcher: `JdbcCatalogResolver` may currently appear mainly in its unit test + Javadoc references — verify with fresh `rg` during research and document accurately
- Include `NoopRuntimeJdbcEndpointResolver` as a **test stub**, not a third production authority

</specifics>

<deferred>
## Deferred Ideas

- **RES-02** — Full JDBC resolver code consolidation into a single authority (explicitly out of v2.1)
- Any Spring wiring / behavior change to make catalog-side and execute-path share one implementation
- Expanding Phase 12 proofs to assert `snap:` in HTTP IT (already deferred; not this phase)

</deferred>

---
*Phase: 14-resolver-ownership-docs*
*Discussed: 2026-07-29 (--auto)*
