# Calcite / Template V2 V1 Parity Scorecard

## Purpose

This document checks the current Template V2 / Calcite implementation against the V1 feature surface and records what is still missing.

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

- additional iterator adapters: constant and datetime
- Excel source and sink
- database-writer dialect-specific behavior for MySQL/Postgres/ClickHouse
- broad SpEL/faker compatibility UDF catalog
- JavaScript/procedural script migration policy
- V1 orchestration features: pause, log, shared state, generator scheduling, iterator branching
- migration examples and business-scenario parity validation

## Template And Runtime

| V1 Feature | V2 Status | Evidence | Gap / Decision |
|---|---|---|---|
| Template root with iterator, generator, fields, output | Partial | `TemplateV2VO` uses `sources`, `transformers`, `sinks`; service routing supports V1/V2 | `GeneratorVO` scheduling/batching is not a V2 runtime contract yet |
| Field dependency graph | Covered | SQL projection and Calcite validation replace row-local DAG dependency | Need representative V1-to-V2 examples |
| Multiple fields / transformations | Covered | ordered `transformers` chain exists | no arbitrary transformer DAG by design |
| Multiple outputs | Covered | sequential multi-sink fan-out exists | no parallel sink execution yet |
| Sink failure policy | Covered | `FAIL_FAST` / `CONTINUE_ON_ERROR` | partial-success reporting remains thin |
| V1/V2 coexistence | Covered | template detection and mixed loading exist | V1 retirement criteria still undefined |

## Iterators And Sources

| V1 Feature | V2 Status | Current V2 Replacement | Gap / Next Action |
|---|---|---|---|
| Number iterator | Covered | `IteratorSourceVO` + `IteratorRowSource` | none for baseline |
| Constant iterator | Missing | should become `IteratorSourceVO` or `InlineDataSourceVO` backed `RowSource` | implement P0 adapter |
| Datetime iterator | Missing | should become `IteratorSourceVO` backed `RowSource` | implement P0 adapter |
| Database iterator | Covered | `QuerySourceVO` / `QueryRowSource` | old iterator-specific pagination semantics need migration examples |
| CSV iterator/reader | Covered | `CsvSourceVO` / `CsvRowSource` | multiline quoted fields not supported |
| JSON iterator/reader | Covered | `JsonSourceVO` / `JsonRowSource` | nested object expansion intentionally deferred |
| Excel iterator/reader | Missing | future `ExcelSourceVO` / `ExcelRowSource` | implement P1 source |
| JDBC reader | Covered | `QuerySourceVO` / `QueryRowSource` | dynamic datasource endpoint resolution exists |
| Constant reader | Partial | SQL literals or future inline source | no first-class inline table source runtime yet |
| SpEL reader | Partial | query params support lightweight SpEL; SQL/UDF path exists | broad SpEL migration map missing |
| AI reader | Partial | `AiSourceVO`, deterministic local providers, `AiRuntimeBridge` contract | concrete remote bridge not implemented |

## Source Policies And Selection

| V1 Feature | V2 Status | Current V2 Replacement | Gap / Next Action |
|---|---|---|---|
| Reader select equal/weight | Partial | `SourcePolicyVO` source materialization policy | weight/equal reader-pool semantics are not fully modeled |
| Value select repeat random/order | Partial | `SourcePolicyVO` ordered/random selection and `limit` | exact V1 once/multiple/repeat semantics need scorecard examples |
| Iterator choose/otherwise | Compatibility-only | orchestration layer | do not force into SQL transform |
| Iterator pause | Compatibility-only | orchestration layer | keep V1 unless V2 orchestration is introduced |

## Stages And Transformations

| V1 Feature | V2 Status | Current V2 Replacement | Gap / Next Action |
|---|---|---|---|
| READ stage | Covered | named `sources` | richer reader options still handled per source type |
| SELECT stage | Partial | source policy | exact V1 selector semantics incomplete |
| SCRIPT plain | Covered | SQL literal/projection or query param constant | migration examples needed |
| SCRIPT SpEL | Partial | SQL functions + plugin/repository UDFs | faker/SpEL UDF catalog and guide missing |
| SCRIPT JavaScript | Compatibility-only | none | keep V1 or introduce explicit script plugin later |
| MAPPING stage | Covered | `CASE WHEN`, SQL expressions | examples needed |
| CONDITION stage | Covered | `CASE WHEN` / `WHERE` | examples needed |
| CONVERT stage | Covered | `CAST`, standard functions, V2 UDFs | add more V1 converter examples |
| LOG stage | Compatibility-only | runtime diagnostics | do not model as SQL transform |
| PAUSE stage | Compatibility-only | orchestration | do not model as SQL transform |
| SHARED stage | Compatibility-only | orchestration/shared state | requires separate runtime design if needed |

## Scripts, Faker, And UDFs

| V1 Feature | V2 Status | Current V2 Replacement | Gap / Next Action |
|---|---|---|---|
| Plain script | Covered | SQL constants / expressions | none for baseline |
| Simple SpEL expression | Partial | SQL + `TemplateV2SqlFunctionRegistry` | define compatibility function list |
| Faker date formatting | Partial | `V2_FORMAT_DATE`, date UDFs | broader faker UDF catalog missing |
| Faker snowflake/text/common providers | Missing | future repository/plugin UDFs | implement common faker UDF batch |
| Custom project SpEL utilities | Partial | plugin UDFs | migration guide missing |
| JavaScript scripts | Compatibility-only | none | keep V1 unless a script transform plugin is explicitly required |
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
| Excel | Missing | none | implement P1/P2 `ExcelSinkFactory` |

## Extension And Plugin Parity

| V1/Target Capability | V2 Status | Evidence | Gap / Next Action |
|---|---|---|---|
| Built-in extension points | Covered | source/transform/sink factories and UDF registry | none |
| Spring-provided runtime services | Covered | JDBC/Kafka/Elasticsearch/AI bridge providers | concrete AI bridge pending |
| External plugin class isolation | Covered | PF4J integration tests | malformed jar / descriptor policy can be hardened further |
| Hot reload / refresh | Covered | refreshable registry and PF4J refresh tests | in-flight refresh policy documented and tested |
| Plugin-provided source/transform/sink | Covered | PF4J runtime execution tests | sample plugin remains minimal |
| Plugin-provided UDF | Covered | PF4J UDF fixture | none for baseline |

## Highest-Value Missing Items

P0:

- implement constant iterator / inline source adapter
- implement datetime iterator source adapter
- add representative V1-to-V2 examples for mapping, condition, convert, and simple SpEL
- create a first faker/UDF compatibility batch for the most common V1 expressions

P1:

- implement Excel source
- implement Excel sink
- add concrete remote AI runtime bridge behind `AiRuntimeBridge`
- clarify source policy coverage for V1 value select strategies with examples

P2:

- decide whether MySQL/Postgres/ClickHouse need dialect-specific V2 sink factories or documented generic JDBC migration
- document JavaScript, log, pause, shared, iterator branching, and generator scheduling as compatibility-only unless V2 orchestration is introduced
- add business-scenario parity examples and acceptance criteria for V1 retirement

## Immediate Recommendation

The next implementation slice should close the P0 source parity gaps before adding more sink formats:

1. Add constant/inline source runtime coverage.
2. Add datetime iterator source runtime coverage.
3. Add V1-to-V2 migration examples for row-local transformation templates.
4. Start a small faker/UDF compatibility catalog only for expressions observed in repository templates or business examples.
