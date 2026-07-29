# JDBC resolver ownership and call-site inventory

Maintainer-facing ownership model for the dual JDBC resolvers in this repo (RES-01). This is **documentation and inventory only** — not a refactor plan, Spring merge guide, or migration checklist.

## Two authorities

| Authority | Type | Role |
|-----------|------|------|
| `JdbcCatalogResolver` | Catalog-side helper (`data-generator-datasource-jdbc`) | Catalog-first / inline pool semantics for the datasource module. **Not** injected on the V2 Template execute path today. |
| `DefaultRuntimeJdbcEndpointResolver` | V2 execute-path authority | Implements calcite SPI `RuntimeJdbcEndpointResolver`. Wired in `CoreConfig` and used by V2 JDBC sources/sinks at run time. |

`NoopRuntimeJdbcEndpointResolver` is a **test/noop stub** of the same SPI — not a third production authority.

## Coexistence

Both resolvers **coexist by design**. Neither delegates to the other. Each independently mirrors similar catalog-resolve / register-if-absent semantics for its own callers. Class Javadoc on `DefaultRuntimeJdbcEndpointResolver` already states this; this doc promotes that story for maintainers.

## snap: run-start snapshot routing (DS-03)

When a `WorkflowRunContext` is bound for the current run (`instanceId` set), the execute-path managed JDBC path returns the run-start snapshot routing key `snap:{instanceId}:{name}` instead of the logical `dataSourceId`. In-flight runs keep their pre-reload pool across catalog hot-reload (DS-03 / Phase 07.1).

Automated proof: `JdbcSnapshotExecutePathIT`.

## HTTP run-path narrative

Operator/API runs do **not** call a third resolver. The spine is:

`TaskController` `POST /task/run/{id}` (and console `/api/templates/{id}/run`) → `TaskExecutionService` → `TemplateV2Runner` → calcite JDBC factories (`QuerySourceFactory`, `PostGisQuerySourceFactory`, `JdbcSinkFactory` / `JdbcRowSinkAdapter`) → `RuntimeJdbcEndpointResolver` → production bean `DefaultRuntimeJdbcEndpointResolver`.

Phase 12 HTTP proof: `ManagedJdbcCatalogHttpExecuteIT` (managed JDBC catalog → HTTP execute path).

## Which resolver to use

| You are… | Use |
|----------|-----|
| Extending V2 JDBC sources/sinks, HTTP/template runs, or execute-path SPI consumers | `RuntimeJdbcEndpointResolver` / `DefaultRuntimeJdbcEndpointResolver` |
| Working inside the datasource-jdbc module or standalone catalog resolve experiments | `JdbcCatalogResolver` (today: unit tests primarily) |
| Writing calcite unit tests that must not hit Spring catalog wiring | `NoopRuntimeJdbcEndpointResolver` |

## Deferred: RES-02

A future milestone may consolidate into a **single authority** and remove duplicated catalog-resolve / register-if-absent logic. That consolidation is **out of scope for v2.1**. This section intentionally omits migration steps, tickets, and code sketches.

## Non-goals (this milestone)

- No Spring bean merge of the two resolvers
- No deleting either class
- No P0 / `.planning/test-matrix.yaml` / `scripts/verify-harness.ps1` changes
- No product behavior changes

## Related docs

- `docs/template-v2-datasource-and-secret-governance.md` — datasource/secret governance (sibling)
- `AGENTS.md` — Commands pointer for maintainers (Phase 14 packaging)

---

## Call-site inventory

Inventory is code-derived from the working tree (see [Inventory methodology](#inventory-methodology)). Paths are repo-relative.

### Execute-path production

| Path | Symbol / role | Notes |
|------|---------------|-------|
| `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java` | `runtimeJdbcEndpointResolver` `@Bean` | Wires `DefaultRuntimeJdbcEndpointResolver` when no override bean |
| `data-generator-service/src/main/java/org/gensokyo/data/config/CoreConfig.java` | `templateV2RuntimeContext` | Injects `RuntimeJdbcEndpointResolver` into V2 runtime context |
| `data-generator-service/src/main/java/org/gensokyo/data/config/DefaultRuntimeJdbcEndpointResolver.java` | class | Production execute-path implementation |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/RuntimeJdbcEndpointResolver.java` | SPI interface | Execute-path contract |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2RuntimeContext.java` | context holder | Carries `runtimeJdbcEndpointResolver()` for plugins |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/plugin/JdbcTemplateTemplateV2RuntimePluginProvider.java` | `sourceFactories` / `sinkFactories` | Passes `context.runtimeJdbcEndpointResolver()` into `QuerySourceFactory`, `PostGisQuerySourceFactory`, `JdbcSinkFactory` |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/QuerySourceFactory.java` | constructor field | Resolves managed/inline JDBC source routing keys |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/PostGisQuerySourceFactory.java` | constructor field | Same execute-path contract for PostGIS sources |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/source/PostGisQuerySourceSupport.java` | support helper | Uses execute-path resolver with PostGIS sources |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcSinkFactory.java` | constructor field | Builds `JdbcRowSinkAdapter` with injected resolver |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sink/JdbcRowSinkAdapter.java` | field + ctor | Production sink adapter; no-arg ctor defaults to `NoopRuntimeJdbcEndpointResolver` for tests only |
| `data-generator-service/src/main/java/org/gensokyo/data/template/E2eV2ScenarioFixtureService.java` | injected field | Service-side fixture helper on execute-path SPI |

### Catalog-side

| Path | Symbol / role | Notes |
|------|---------------|-------|
| `data-generator-datasource/data-generator-datasource-jdbc/src/main/java/org/gensokyo/data/datasource/jdbc/JdbcCatalogResolver.java` | class definition | Catalog-side helper; **no production Spring bean injects `JdbcCatalogResolver` today** |
| `data-generator-service/src/main/java/org/gensokyo/data/config/DefaultRuntimeJdbcEndpointResolver.java` | Javadoc `@link` only | Documentation cross-reference — **not** a runtime caller |

Scout result: production callers beyond the class itself are **absent**. The only executable caller on current tree is the unit test below.

### Tests and stubs

| Path | Symbol | Notes |
|------|--------|-------|
| `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/JdbcSnapshotExecutePathIT.java` | `@Autowired RuntimeJdbcEndpointResolver` | DS-03 `snap:` execute-path proof |
| `data-generator-service/src/test/java/org/gensokyo/data/datasource/catalog/ManagedJdbcCatalogHttpExecuteIT.java` | HTTP IT | Phase 12 managed JDBC HTTP execute proof (run spine; may not name the resolver type in source) |
| `data-generator-service/src/test/java/org/gensokyo/data/generator/RuntimeJdbcEndpointResolverTests.java` | Spring context test | Inline registration through `QuerySourceFactory` |
| `data-generator-service/src/test/java/org/gensokyo/data/template/V2ScenarioTemplateIT.java` | autowired resolver | Scenario IT on real bean |
| `data-generator-datasource/data-generator-datasource-jdbc/src/test/java/org/gensokyo/data/datasource/jdbc/JdbcCatalogResolverTests.java` | direct `new JdbcCatalogResolver(...)` | Catalog-side unit tests — primary catalog-side caller today |
| `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/NoopRuntimeJdbcEndpointResolver.java` | stub impl | Test/noop authority — **not** a third production resolver |
| `data-generator-calcite/src/test/**` (summary) | `NoopRuntimeJdbcEndpointResolver` | Calcite unit/IT stubs — full list via `rg -l NoopRuntimeJdbcEndpointResolver --glob "*.java"` (also appears in some test-fixtures and service watcher tests) |

Representative additional execute-path / SPI test touchpoints (not exhaustive): `UpsertParitySupport`, `ChunkedJdbcParitySupport`, `TemplateV2RunnerTests`, `StreamingPipelineTests`, `ChunkedPipelineTests`, dialect/upsert ITs under `data-generator-calcite/src/test`.

---

## Inventory methodology

Scouted on the Phase 14 working tree with ripgrep-equivalent file search (`*.java`):

```text
rg -l "JdbcCatalogResolver" --glob "*.java"
rg -l "DefaultRuntimeJdbcEndpointResolver|RuntimeJdbcEndpointResolver" --glob "*.java"
rg -l "NoopRuntimeJdbcEndpointResolver" --glob "*.java"
```

`JdbcCatalogResolver` matches: definition, `JdbcCatalogResolverTests`, and Javadoc reference in `DefaultRuntimeJdbcEndpointResolver` only.

Re-run these commands before updating this inventory after large JDBC/catalog refactors.
