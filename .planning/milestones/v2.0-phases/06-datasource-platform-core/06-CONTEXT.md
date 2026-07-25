# Phase 6: Datasource Platform Core - Context

**Gathered:** 2026-06-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver **DS-01** and **DS-02**: populate `data-generator-datasource` with unified JDBC, Kafka, and Elasticsearch connection abstractions and a **managed connection catalog** that resolves template references (`dataSourceId`, Kafka/ES cluster keys) without changing template YAML shape or breaking existing console CRUD and V2 run paths.

**In scope:** Catalog resolve API, module structure (api + jdbc/kafka/es adapters), runtime wiring into Calcite via `ConnectionCatalog`, registry relocation, yaml bootstrap + DB managed entries, full inline/managed parity, regression on existing datasource tests.

**Out of scope (later phases):** Snapshot-based hot-reload (Phase 7), governance policy enforcement and publish-time secret rules (Phase 7), connectivity test unified under Catalog (Phase 7), audit event model changes (Phase 7), streaming/upsert (Phase 8), dialect expansion (Phase 9), harness matrix expansion (Phase 10), unified `connectionRef` template field, merged single Console page, feature flags for old/new resolve paths.

</domain>

<decisions>
## Implementation Decisions

### Catalog API & Identity Model
- **D-01:** Use a **shared global namespace** for connection names across JDBC, Kafka, and Elasticsearch; distinguish types by `kind` (not name prefix).
- **D-02:** Model catalog entries as a **unified `CatalogEntry` record with a `kind` field** (`JDBC`, `KAFKA`, `ELASTICSEARCH`) and type-specific config payloads.
- **D-03:** **Keep existing template reference fields unchanged** — JDBC uses `dataSourceId`; Kafka/ES keep cluster/connection fields; Catalog resolves at runtime.
- **D-04:** **Preserve primary-cluster fallback** for Kafka/ES when cluster is blank (same semantics as `DynamicKafkaTemplateRegistry` / `DynamicElasticsearchClientRegistry`).
- **D-05:** Phase 6 Catalog exposes **resolve-only API** (`resolve`, `listAll`); CRUD mutations stay on existing service paths (`DataSourceConfigService`, `MessagingClusterConfigService`).
- **D-06:** `listAll()` returns a **merged view** of yaml bootstrap + DB-managed entries, each tagged with source `BOOTSTRAP` or `MANAGED`.
- **D-07:** Unknown connection → throw **actionable `IllegalArgumentException`** (name, kind, hints); align with `ConsoleApiAdvice` patterns.
- **D-08:** Infer **`kind` from template context** (JDBC source/sink → JDBC; Kafka sink → KAFKA; ES sink/source → ELASTICSEARCH) rather than requiring templates to declare kind.
- **D-09:** Phase 6 resolve uses **live lookup** (current registry/DB); snapshot-at-run-start semantics deferred to Phase 7 (no snapshot stub required).
- **D-10:** Catalog **internally calls `SecretResolver`** when building runtime handles; do not expose secret values outward.
- **D-11:** Console list endpoints **delegate to `Catalog.listAll()`** for unified BOOTSTRAP/MANAGED display; REST paths and DTO shapes stay unchanged.

### Module Packaging
- **D-12:** Structure `data-generator-datasource` as **parent aggregator + four submodules**: `data-generator-datasource-api`, `-jdbc`, `-kafka`, `-elasticsearch`.
- **D-13:** Phase 6 moves **resolution and runtime registry logic** into adapters; **CRUD/persistence stays in `data-generator-service`** initially.
- **D-14:** Move **`DynamicKafkaTemplateRegistry` and `DynamicElasticsearchClientRegistry`** from `data-generator-core` into kafka/elasticsearch adapter modules.
- **D-15:** **`data-generator-service` depends on datasource parent/adapters** for wiring; **`data-generator-calcite` depends only on `datasource-api`**.
- **D-16:** **`datasource-jdbc` depends on `data-generator-database-core`** for dialect utilities (reuse, do not duplicate).
- **D-17:** PF4J plugins access connections **via `TemplateV2RuntimeServices` / Catalog injection**, not direct datasource module dependency.
- **D-18:** **Unit tests live in each adapter module**; service integration tests provide regression coverage.

### Inline vs Managed Resolution
- **D-19:** **Full parity** — inline connection blocks and managed catalog references coexist with today's behavior.
- **D-20:** JDBC resolution order: **`dataSourceId` via Catalog first**; if absent, use inline `dataSource` block.
- **D-21:** Kafka/ES: **cluster name via Catalog**; inline config continues through existing adapter logic (symmetric to JDBC dual-path).
- **D-22:** Inline **`passwordSecretRef` / API key refs** continue through existing **`SecretResolver`** (unchanged semantics).

### YAML Bootstrap vs Console Catalog
- **D-23:** On name collision, **DB-managed entry wins** over yaml bootstrap (MANAGED overrides BOOTSTRAP in resolve and list).
- **D-24:** Yaml bootstrap entries are **read-only in Console** — cannot edit/delete `BOOTSTRAP` entries via API.
- **D-25:** Register yaml connections into Catalog at **application startup** (consistent with `DataSourceBootstrap` / `MessagingClusterConfigService` `@PostConstruct`).
- **D-26:** Tag source with **`BOOTSTRAP` / `MANAGED` enum** (no mandatory debug path in Phase 6).

### Calcite Runtime Boundary
- **D-27:** **`data-generator-calcite` depends only on `datasource-api`** (Catalog interfaces, kinds, entry types) — not adapter implementations.
- **D-28:** Evolve **`TemplateV2RuntimeServices` to inject `ConnectionCatalog`** as the primary connection access point.
- **D-29:** Implement **`RuntimeJdbcEndpointResolver` via Catalog** so existing JDBC factories need minimal change.
- **D-30:** **Kafka and ES sink factories also resolve through Catalog** in Phase 6 (not JDBC-only).
- **D-31:** Calcite embedded tests use an **`InMemoryCatalog` test bean** to register connections without full Spring Boot.
- **D-32:** **Zero template YAML changes** required for Phase 6 rollout.
- **D-33:** **No feature flag** — Catalog becomes the single resolve path once shipped; tests guarantee regression safety.

### Migration Strategy
- **D-34:** Use **strangler pattern** — introduce Catalog + adapters delegating to existing services/registries, then consolidate.
- **D-35:** **Single-path cutover** (no parallel old/new resolve paths in production code).
- **D-36:** Phase 6 regression gate: **existing datasource-related tests must pass** (`DataSourceConfigServiceTests`, `ConsoleDataSourceControllerTest`, `V2ScenarioTemplateIT`, messaging cluster tests, etc.).
- **D-37:** Console REST **paths and response shapes unchanged**; internal delegation to Catalog only.

### Claude's Discretion
- **JDBC pool ownership:** Implement **`datasource-jdbc` owning Druid + `dynamic-datasource` pool construction** so the JDBC adapter is self-contained and calcite embedded tests stay lightweight.
- **Exact `ConnectionCatalog` interface shape** and adapter delegation seams (as long as decisions D-01–D-37 hold).
- **Order of strangler steps** within Phase 6 plans (api → jdbc → kafka/es → calcite wiring) as long as all three kinds ship in Phase 6.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & Roadmap
- `.planning/ROADMAP.md` — Phase 6 goal, success criteria, DS-01/DS-02 mapping
- `.planning/REQUIREMENTS.md` — DS-01, DS-02 full requirement text and traceability
- `.planning/PROJECT.md` — v2.0 milestone scope, deferred items (hot-reload → Phase 7)
- `.planning/STATE.md` — current position, v2 dialect priority (Phase 9)

### Codebase Maps
- `.planning/codebase/ARCHITECTURE.md` — layer model, datasource registry in `CoreConfig`
- `.planning/codebase/INTEGRATIONS.md` — JDBC/Kafka/ES config, console APIs, secret registry
- `.planning/codebase/CONCERNS.md` — Boot 4 / internal starter compatibility pressure

### Existing Implementation (brownfield — migrate from)
- `data-generator-datasource/pom.xml` — empty aggregator (Phase 6 populates submodules)
- `data-generator-service/src/main/java/org/gensokyo/data/datasource/DataSourceConfigService.java` — JDBC CRUD + `DynamicRoutingDataSource` sync
- `data-generator-service/src/main/java/org/gensokyo/data/datasource/DataSourceBootstrap.java` — yaml JDBC bootstrap
- `data-generator-service/src/main/java/org/gensokyo/data/messaging/MessagingClusterConfigService.java` — Kafka/ES CRUD + runtime registration
- `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java` — wires registries, `RuntimeJdbcEndpointResolver`, `TemplateV2RuntimeServices`
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/RuntimeJdbcEndpointResolver.java` — JDBC endpoint resolution interface
- `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2RuntimeServices.java` — runtime service bundle passed to V2 pipeline
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/kafka/support/DynamicKafkaTemplateRegistry.java` — move to kafka adapter
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/elasticsearch/support/DynamicElasticsearchClientRegistry.java` — move to es adapter
- `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/secret/SecretResolver.java` — secret ref resolution for Catalog

### Testing & Verification
- `data-generator-service/src/test/java/org/gensokyo/data/datasource/DataSourceConfigServiceTests.java`
- `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleDataSourceControllerTest.java`
- `data-generator-service/src/test/java/org/gensokyo/data/template/V2ScenarioTemplateIT.java`
- `docs/testing-embedded-components.md` — embedded-first test patterns

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DataSourceConfigService` + `DataSourceConfigPO` — JDBC persistence and runtime pool registration (Catalog delegates here in Phase 6).
- `MessagingClusterConfigService` + `MessagingClusterConfigPO` — Kafka/ES persistence and client/template registration.
- `DynamicRoutingDataSource` (baomidou) — yaml + runtime JDBC pool routing already in production use.
- `DynamicKafkaTemplateRegistry` / `DynamicElasticsearchClientRegistry` — concurrent cluster maps with primary fallback (relocate to adapters).
- `RuntimeJdbcEndpointResolver` + `DefaultRuntimeJdbcEndpointResolver` — JDBC id resolution hook for Calcite factories (reimplement atop Catalog).
- `SecretResolver` — existing secret ref resolution for JDBC passwords and ES/Kafka auth.

### Established Patterns
- **Service owns REST + JPA persistence**; **Calcite owns pipeline execution** via injected runtime services.
- Console APIs return **`R<T>` envelope**; client errors use **`IllegalArgumentException`** → `ConsoleApiAdvice`.
- **Embedded-first tests**: H2 metadata, embedded Kafka, ES HTTP stub; `@SpringBootTest` with `application-phase7-test.yaml`.
- **Strangler-friendly**: `ObjectProvider<>` for optional registries; `@PostConstruct` bootstrap for persisted clusters.

### Integration Points
- `CoreConfig` beans: wire `ConnectionCatalog` implementation, pass into `TemplateV2RuntimeServices` and JDBC plugin providers.
- `ConsoleDataSourceController` / messaging console APIs: switch list aggregation to `Catalog.listAll()` while keeping mutation endpoints on existing services.
- Calcite `JdbcSinkFactory`, Kafka/ES sink factories: resolve cluster/datasource via Catalog instead of direct registry access.
- Maven reactor: add datasource submodules; update `data-generator-service` and `data-generator-calcite` POM dependencies.

</code_context>

<specifics>
## Specific Ideas

- User requested **Chinese discussion UI**; downstream artifacts remain English for agent consumption.
- **Shared namespace** is intentional — operators see one connection name space; kind disambiguates at resolve time.
- **No template migration** in Phase 6 — compatibility is non-negotiable.
- Console should show **whether a connection is yaml-seeded or operator-managed** (`BOOTSTRAP` vs `MANAGED` badge/field).

</specifics>

<deferred>
## Deferred Ideas

- **Snapshot-based resolve at run start** — Phase 7 (DS-03 hot-reload).
- **Catalog-owned connectivity test** — Phase 7 (DS-04).
- **Governance: reject plaintext secrets on publish** — Phase 7 (DS-04).
- **Audit records for datasource reload events** — Phase 7 (DS-05).
- **Unified template `connectionRef` field** — future; would require template migration.
- **Single merged Console "Connections" page** — UI scope beyond Phase 6 core.
- **Feature flag for legacy resolve path** — explicitly rejected; rely on tests instead.

</deferred>

---

*Phase: 6-Datasource Platform Core*
*Context gathered: 2026-06-24*
