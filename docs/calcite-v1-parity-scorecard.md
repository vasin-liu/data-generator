# Calcite / Template V2 V1 Parity Scorecard

> **Reference only (V2-only program).** V1 parity status in this scorecard is historical migration context — **not** acceptance criteria for greenfield Template V2 work after the 2026-05-29 program. For orchestration formerly marked `Compatibility-only`, see [`docs/template-v2-workflow-authoring-guide.md`](template-v2-workflow-authoring-guide.md) and [`docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md`](superpowers/specs/2026-05-29-v2-only-full-capability-design.md).

## Purpose

This document checks the current Template V2 / Calcite implementation against the V1 feature surface and records what is still missing. Use it when migrating or comparing **historical** V1 templates; do not treat `Compatibility-only` rows as blockers for **new** V2 workflow authoring.

Status values:

- `Covered`: implemented and covered by focused tests or integration tests
- `Partial`: V2 has a usable replacement, but not full V1 semantic coverage
- `Missing`: no current V2 implementation
- `Compatibility-only`: intentionally not a V2 SQL/runtime feature; V1 should remain the migration path until a separate orchestration design exists

## Summary

V2 now covers the main declarative path:

- named sources
- SQL transformation
- custom/plugin UDF extension
- console/JDBC/Kafka/Elasticsearch/CSV/JSON sinks
- multi-source, multi-transform linear chains, multi-sink fan-out, and sink failure policy
- PF4J-backed plugin loading, class isolation, UDF contribution, refresh, and diagnostics

The remaining V1 parity gaps are concentrated in:

- official repository-owned non-SQL transformer families beyond the current SQL-first path
- clear migration policy for residual script/business logic that should use a custom transformer instead of more SQL/UDF expansion
- database-writer dialect-specific behavior for MySQL/Postgres/ClickHouse
- broad SpEL/faker compatibility UDF catalog
- JavaScript/procedural script migration policy
- V1 orchestration features: pause, log, shared state, generator scheduling, iterator branching
- business-scenario parity validation beyond the first migration-example batch

## Template And Runtime

| V1 Feature | V2 Status | Evidence | Gap / Decision |
|---|---|---|---|
| Template root with iterator, generator, fields, output | Partial | `TemplateV2VO` uses `sources`, `transformers`, `sinks`; service routing supports V1/V2 | `GeneratorVO` scheduling/batching is not a V2 runtime contract yet |
| Field dependency graph | Covered | SQL projection and Calcite validation replace row-local DAG dependency | first repository-backed migration examples are documented; continue adding business-family coverage |
| Multiple fields / transformations | Covered | ordered `transformers` chain exists | no arbitrary transformer DAG by design |
| Multiple outputs | Covered | sequential multi-sink fan-out exists | no parallel sink execution yet |
| Sink failure policy | Covered | `FAIL_FAST` / `CONTINUE_ON_ERROR` | partial-success reporting remains thin |
| V1/V2 coexistence | Covered | template detection and mixed loading exist | V1 retirement criteria still undefined |

## Iterators And Sources

| V1 Feature | V2 Status | Current V2 Replacement | Gap / Next Action |
|---|---|---|---|
| Number iterator | Covered | `IteratorSourceVO` + `IteratorRowSource` | none for baseline |
| Constant iterator | Covered | `IteratorSourceVO` + `IteratorRowSource` | finite repeated datasets are supported; `repeat=-1` remains intentionally unsupported in finite V2 materialization |
| Datetime iterator | Covered | `IteratorSourceVO` + `IteratorRowSource` | inclusive range materialization and SQL timestamp comparison are covered |
| Database iterator | Covered | `QuerySourceVO` / `QueryRowSource` | old iterator-specific pagination semantics still need broader business validation |
| CSV iterator/reader | Covered | `CsvSourceVO` / `CsvRowSource` | multiline quoted fields not supported |
| JSON iterator/reader | Covered | `JsonSourceVO` / `JsonRowSource` | nested object expansion intentionally deferred |
| Excel iterator/reader | Covered | `ExcelSourceVO` / `ExcelRowSource` | sheet row-window semantics and SQL path are covered; business examples can be added later |
| JDBC reader | Covered | `QuerySourceVO` / `QueryRowSource` | dynamic datasource endpoint resolution exists |
| Large JDBC export (bounded memory) | Covered | `executionPolicy.mode: CHUNKED`, `ChunkedPipeline`, `ChunkedQueryRowSource`, Pattern S/B in `docs/template-v2-jdbc-chunked-execution-guide.md` | MySQL needs `useCursorFetch=true` on datasource URL for server-side cursors; `GROUP BY`/two large queries rejected; `STREAMING` not implemented |
| Constant reader | Partial | SQL literals or future inline source | no first-class inline table source runtime yet |
| SpEL reader | Partial | query params support lightweight SpEL; SQL/UDF path exists | first migration examples are in place; long-tail expression coverage remains |
| AI reader | Covered | `AiSourceVO`, deterministic local providers, service-wired Ollama `AiRuntimeBridge` | additional providers, richer parser catalogs, and business examples can be added incrementally |

## Source Policies And Selection

| V1 Feature | V2 Status | Current V2 Replacement | Gap / Next Action |
|---|---|---|---|
| Reader select equal/weight | Partial | `SourcePolicyVO` source materialization policy | migration analysis now warns explicitly when multi-reader `EQUAL` / `WEIGHT` dispatch semantics are only approximated; exact parity is still not implemented |
| Value select repeat random/order | Partial | `SourcePolicyVO` ordered/random selection and `limit` | migration analysis now warns explicitly for `ONCE_*` / `MULTIPLE_ORDER`; exact V1 once/multiple/repeat consumptive semantics are still not implemented |
| Iterator choose/otherwise | Compatibility-only | orchestration layer | do not force into SQL transform |
| Iterator pause | Compatibility-only | orchestration layer | keep V1 unless V2 orchestration is introduced |

## Stages And Transformations

| V1 Feature | V2 Status | Current V2 Replacement | Gap / Next Action |
|---|---|---|---|
| READ stage | Covered | named `sources` | richer reader options still handled per source type |
| SELECT stage | Partial | source policy | exact V1 selector semantics incomplete |
| Non-SQL built-in transformer families | Partial | ordered `transformers` chain plus transform factory SPI | runtime can already host plugin/custom transformers, but the repository still lacks an official built-in non-SQL transformer family |
| SCRIPT plain | Covered | SQL literal/projection or query param constant | first migration examples are documented |
| SCRIPT SpEL | Partial | SQL functions + plugin/repository UDFs, including first-pass `FAKER_*` compatibility functions | first migration guide/examples are in place; long-tail expression coverage still missing and some cases may be better served by a dedicated non-SQL/custom transformer |
| SCRIPT JavaScript | Compatibility-only | none | keep V1 or introduce an explicit script/custom transformer later |
| MAPPING stage | Covered | `CASE WHEN`, SQL expressions | first real migration examples are documented |
| CONDITION stage | Covered | `CASE WHEN` / `WHERE` | first real migration examples are documented |
| CONVERT stage | Covered | `CAST`, standard functions, V2 UDFs | continue adding converter-heavy business examples only as needed |
| LOG stage | Compatibility-only | runtime diagnostics | do not model as SQL transform |
| PAUSE stage | Compatibility-only | orchestration | do not model as SQL transform |
| SHARED stage | Compatibility-only | orchestration/shared state | requires separate runtime design if needed |

## Scripts, Faker, And UDFs

| V1 Feature | V2 Status | Current V2 Replacement | Gap / Next Action |
|---|---|---|---|
| Plain script | Covered | SQL constants / expressions | none for baseline |
| Simple SpEL expression | Partial | SQL + `TemplateV2SqlFunctionRegistry` | compatibility list exists for the first high-frequency batch; long-tail expressions remain |
| Faker date formatting | Partial | `V2_FORMAT_DATE` plus `FAKER_DATETIME_FORMAT` / related datetime helpers, including default-format and relative-time variants | broader faker UDF catalog still has long-tail gaps outside the high-frequency datetime path |
| Faker snowflake/text/common providers | Partial | `FAKER_SNOWFLAKE`, `FAKER_TEXT`, `FAKER_NUMBER_BETWEEN`, `FAKER_PHONE_CELL`, `FAKER_DATE_PAST`, and expanded datetime helpers across `before/after/plus/minus` day-hour-minute-second variants | add provider-specific and business-specific long-tail functions only as needed |
| Custom project SpEL utilities | Partial | plugin UDFs | migration guide exists for the common path; project-specific utility chains still need case-by-case migration |
| JavaScript scripts | Compatibility-only | none | keep V1 unless a script/custom transform path is explicitly required |
| Plugin-provided UDF | Covered | `TemplateV2RuntimePlugin.sqlFunctions()` and PF4J fixtures | none for baseline |

## Writers And Sinks

| V1 Writer | V2 Status | Current V2 Replacement | Gap / Next Action |
|---|---|---|---|
| Console | Covered | `ConsoleSinkFactory` | none |
| JDBC generic | Covered | `JdbcSinkFactory` / `JdbcRowSinkAdapter` | limited to generic insert/template mapping |
| MySQL | Partial | generic JDBC sink | MySQL-specific writer behavior not separately modeled |
| Postgres | Partial | generic JDBC sink | `COPY`/dialect-specific behavior not modeled |
| ClickHouse | Partial | generic JDBC sink | V1 bulk CSV insert behavior not modeled |
| Kafka | Covered | `KafkaSinkFactory` / `KafkaRowSinkAdapter` | runtime dynamic cluster support exists through services; verify business examples |
| Elasticsearch | Covered | `ElasticsearchSinkFactory` / `ElasticsearchRowSinkAdapter` | verify business examples |
| CSV | Covered | `CsvSinkFactory`, delimiter/header/append diagnostics | old `CsvWriterVO.custom` compatibility bridge not implemented |
| JSON | Covered | `JsonSinkFactory`, `ARRAY` / `NDJSON` modes | nested object fidelity deferred to row model decision |
| Excel | Covered | `ExcelSinkFactory` / `ExcelRowSinkAdapter` | baseline sheet/header write path is covered; append/multi-sheet extensions can be added later |

## Extension And Plugin Parity

| V1/Target Capability | V2 Status | Evidence | Gap / Next Action |
|---|---|---|---|
| Built-in extension points | Covered | source/transform/sink factories and UDF registry | none |
| Spring-provided runtime services | Covered | JDBC/Kafka/Elasticsearch plus service-wired Ollama AI bridge providers | expand only when another provider or plugin boundary becomes necessary |
| External plugin class isolation | Covered | PF4J integration tests | malformed jar / descriptor policy can be hardened further |
| Hot reload / refresh | Covered | refreshable registry and PF4J refresh tests | in-flight refresh policy documented and tested |
| Plugin-provided source/transform/sink | Covered | PF4J runtime execution tests | sample plugin remains minimal |
| Plugin-provided UDF | Covered | PF4J UDF fixture | none for baseline |

## Highest-Value Missing Items

P0:

- define the first official non-SQL transformer family and the decision rule for SQL/UDF vs custom transformer
- extend business-family migration validation beyond the first documented V1-to-V2 example batch
- create the next faker/UDF compatibility batch only for newly observed high-value V1 expressions

P1:

- add one repository-owned or PF4J-supplied custom transformer example that covers residual script/business logic
- clarify source policy coverage for V1 value select strategies with examples
- add migration examples that exercise the new AI source bridge in realistic V2 templates

P2:

- decide whether MySQL/Postgres/ClickHouse need dialect-specific V2 sink factories or documented generic JDBC migration
- document JavaScript, log, pause, shared, iterator branching, and generator scheduling as compatibility-only unless V2 orchestration is introduced
- add business-scenario parity examples and acceptance criteria for V1 retirement

## Migration dual-run evidence

| Capability | V2 Status | Evidence | Gap / Decision |
|---|---|---|---|
| Scenario inventory (DB + repo) | Covered | `docs/migration/scenario-inventory.yaml`, `MigrationInventoryService` | DB refresh via tests / future admin profile |
| Dual-run compare (V1 vs V2) | Covered | `POST /template/migration/compare/{templateId}`, `docs/migration/reports/*.md` | Production executor parity still business-validated |
| Migration classification | Covered | `MigrationClassificationRules`, inventory `migrationClass` | APPROXIMATE templates need manual review |
| Unified draft migration | Covered | `POST /template/migration/draft/{templateId}`, `MigrationDraftService` | Complex multi-source transforms still manual |
| Promote after review | Covered | `POST /template/migration/promote/{templateId}`, `MigrationPromoteService` | V1 yaml retained on entity until archive policy |
| CHUNKED policy on JDBC migrate | Covered | `V1QuerySourceExecutionPolicySuggester`, tests | Skips explicit small `maxRows` and `Const.AMOUNT` default |
| Inventory maintained with report paths | Covered | ≥10 rows in `scenario-inventory.yaml`, sample reports committed | Automate nightly compare in CI optional |

## Immediate Recommendation

The next implementation slice should shift from iterator parity to migration usability:

1. Define the first official non-SQL transformer family and document when authors should choose SQL/UDF versus a custom transformer.
2. Expand migration examples from the current first batch into more business-template families, especially AI source, selection-heavy, and residual script-heavy templates.
3. Continue the faker/UDF catalog only for expressions observed in repository templates or business examples beyond the current covered batch.
4. Decide whether inline rows deserve a dedicated V2 source type or should remain an iterator-backed compatibility shape.
