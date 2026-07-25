# Phase 6: Datasource Platform Core - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-24
**Phase:** 6-Datasource Platform Core
**Areas discussed:** Catalog API & identity model, Module packaging, Inline vs managed resolution, YAML bootstrap vs console catalog, Calcite dependency boundary, Migration strategy

---

## Catalog API & Identity Model

| Option | Description | Selected |
|--------|-------------|----------|
| Shared namespace | Single global name space; kind distinguishes JDBC/Kafka/ES | ✓ |
| Type-prefixed names | e.g. `jdbc:orders`, `kafka:events` | |
| Separate namespaces | Keep today’s separate JDBC vs cluster keys | |
| Unified CatalogEntry + kind | Single record model with typed config | ✓ |
| Typed facades only | Separate PO models behind facade | |
| Keep template fields | dataSourceId / cluster unchanged | ✓ |
| Add optional connectionRef | Unified alias with backward compat | |
| Unify to connectionRef now | Template migration required | |
| Keep primary fallback | Blank cluster → primary | ✓ |
| Require explicit cluster | Fail if cluster omitted | |
| Catalog-level default | Per-kind default marker | |
| Resolve-only API (Phase 6) | CRUD stays on existing services | ✓ |
| Full Catalog CRUD | Console delegates all mutations | |
| Merged listAll view | yaml + DB with BOOTSTRAP/MANAGED | ✓ |
| Live lookup (Phase 6) | Snapshot semantics in Phase 7 | ✓ |
| Catalog internal SecretResolver | Resolved handles, no secret leakage | ✓ |
| Console list delegates Catalog | REST paths unchanged | ✓ |
| Connectivity test out of Phase 6 | Phase 7 scope | ✓ |

**User's choice:** Shared namespace, unified entry model, keep template fields, primary fallback, resolve-only Catalog, merged listing, actionable errors, kind inferred from context, live lookup, internal secret resolution, console list delegation, no connectivity test in Phase 6.

**Notes:** User continued discussion for three rounds on Catalog API before moving on.

---

## Module Packaging

| Option | Description | Selected |
|--------|-------------|----------|
| Three submodules + parent | jdbc / kafka / elasticsearch adapters | ✓ |
| Single JAR with packages | One artifact, package split | |
| Resolution only from service | CRUD stays in service Phase 6 | ✓ |
| Move registries to datasource | Kafka/ES registries leave core | ✓ |
| JDBC pool: agent discretion | Self-contained jdbc adapter preferred | ✓ |
| service depends on adapters | calcite on api only | ✓ |
| jdbc adapter uses database-core | Reuse dialect utilities | ✓ |
| PF4J via TemplateV2RuntimeServices | No direct plugin → datasource dep | ✓ |
| Tests in each adapter | Embedded unit tests per module | ✓ |

**User's choice:** Four-module layout (api + three adapters), strangler move of resolution only, registry relocation, service wiring, database-core reuse.

**Notes:** User selected "you decide" for JDBC pool ownership — recorded as datasource-jdbc owning Druid/dynamic-datasource in CONTEXT.md discretion.

---

## Inline vs Managed Resolution

| Option | Description | Selected |
|--------|-------------|----------|
| Full parity | Inline + managed coexist unchanged | ✓ |
| dataSourceId first, then inline | Catalog for id; inline fallback | ✓ |
| Kafka/ES cluster or inline | Cluster via Catalog; inline via adapters | ✓ |
| Existing SecretResolver for inline | No change to secret semantics | ✓ |

**User's choice:** Preserve all inline paths; managed id/cluster takes precedence when present.

---

## YAML Bootstrap vs Console Catalog

| Option | Description | Selected |
|--------|-------------|----------|
| DB wins on collision | MANAGED overrides BOOTSTRAP | ✓ |
| Read-only BOOTSTRAP in Console | Cannot edit yaml-seeded entries | ✓ |
| Startup registration | Register yaml at boot | ✓ |
| BOOTSTRAP / MANAGED enum | Source tagging | ✓ |

**User's choice:** Managed overrides bootstrap; bootstrap entries read-only in console; startup load; enum source marker.

---

## Calcite Dependency Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| calcite → datasource-api only | No adapter impl on calcite classpath | ✓ |
| Inject ConnectionCatalog in RuntimeServices | Primary connection access | ✓ |
| Catalog implements RuntimeJdbcEndpointResolver | Minimal factory churn | ✓ |
| InMemoryCatalog for calcite tests | Lightweight embedded tests | ✓ |
| datasource-api submodule | Dedicated API artifact | ✓ |
| Kafka/ES factories use Catalog | Symmetric with JDBC | ✓ |
| Zero template change | Hard compatibility constraint | ✓ |
| No feature flag | Single resolve path | ✓ |

**User's choice:** API-only calcite dependency, Catalog in runtime services, resolver implemented via Catalog, all three kinds through Catalog in Phase 6, no flag.

---

## Migration Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Strangler / thin facade | Delegate then consolidate | ✓ |
| Existing datasource tests green | Regression scope | ✓ |
| Single-path cutover | No parallel resolve | ✓ |
| Unchanged REST API | Internal delegation only | ✓ |

**User's choice:** Strangler migration, existing test suite as gate, no feature flag, console API stable.

---

## Claude's Discretion

- JDBC pool ownership: **datasource-jdbc** owns Druid + dynamic-datasource construction.
- Exact interface seams and internal plan ordering within Phase 6.

## Deferred Ideas

- Snapshot resolve, governance, connectivity test in Catalog, audit — **Phase 7**
- Unified `connectionRef`, merged Console page — **future / out of scope**
- Feature flag for legacy path — **rejected**
