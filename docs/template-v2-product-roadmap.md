# Template V2 Product Roadmap

## Purpose

This document translates the current Calcite / Template V2 technical plan into a product-facing roadmap.

It answers four questions:

- which application scenarios the current product should optimize for
- which future capabilities are still required beyond the current engine refactor
- how those capabilities should be grouped into execution core, control plane, and governance plane
- what should be delivered first so V2 can replace V1 as the mainline product path

This is not a replacement for the implementation and refactor plans. It is the product-level planning view built on top of them.

Related references:

- `docs/calcite-refactor-plan.md`
- `docs/calcite-implementation-status.md`
- `docs/calcite-templatev2-model-design.md`
- `docs/template-v2-datasource-and-secret-governance.md`
- `docs/template-v2-transformer-strategy.md`
- `docs/calcite-v1-parity-scorecard.md`
- `docs/calcite-v1-v2-mapping.md`
- `docs/calcite-plugin-framework-evaluation.md`

## Product Goal

Template V2 should become the default authoring and runtime model for the product.

The target product shape is:

- simpler than V1 for common data-generation and data-shaping scenarios
- stronger than V1 for multi-source composition, sink fan-out, and extensibility
- open to repository-owned and plugin-provided source / transformer / sink / UDF capabilities
- suitable for both local deterministic generation and business-facing integration tasks

V2 should not stop at "SQL execution exists". It should become a complete product path with:

- source definition and connection governance
- transformation authoring choices
- sink delivery and failure control
- runtime observability and diagnostics
- migration tooling and scenario templates
- plugin lifecycle and extension governance

## Product Positioning

The future product is best understood as three coordinated layers.

### 1. Execution core

Responsible for:

- reading from sources
- transforming rows or row sets
- writing to sinks
- managing runtime execution semantics such as batching, retries, and memory limits

Primary contracts:

- `SourceVO`
- `TransformVO`
- `WriterVO` / sink model
- runtime registry / plugin contracts

### 2. Control plane

Responsible for:

- validation
- explain / preview / dry-run
- task execution lifecycle
- replay / retry / cancel
- metrics, logs, run summaries, and diagnostics

### 3. Governance plane

Responsible for:

- template versioning and promotion
- datasource and secret management
- plugin installation and compatibility
- permissions, audit, and quotas
- environment and tenant isolation

## Primary Product Scenarios

The current repository direction suggests six major scenario families.

### Scenario A. Synthetic data generation

Typical needs:

- iterator-backed seeds
- faker and deterministic utility functions
- field derivation and conditional shaping
- multi-table related data generation
- output to console, files, database, Kafka, or Elasticsearch

Why it matters:

- this is closest to the historical V1 value proposition
- it is the main replacement target for many current templates

### Scenario B. Multi-source enrichment and lookup assembly

Typical needs:

- query-backed sources
- file and API style inputs
- joins against lookup or dimension data
- grouped or deduplicated output
- author-friendly schema handling and diagnostics

Why it matters:

- this is where V2 can exceed V1 most clearly
- Calcite-style table composition is a better long-term model than field DAGs

### Scenario C. File / database / message conversion

Typical needs:

- CSV / JSON / Excel / JDBC / Kafka / Elasticsearch interoperability
- schema mapping and type conversion
- sink failure policy and retry behavior
- large file or large result-set handling

Why it matters:

- this is a likely high-frequency business use case
- it turns the product into a reusable data-shaping tool, not only a generator

### Scenario D. AI-assisted structured generation

Typical needs:

- `AiSourceVO`
- prompt templating
- structured parser selection
- schema-constrained output
- provider fallback and error diagnostics

Why it matters:

- this is a strong future differentiator
- it only becomes product-grade if observability, cost control, and parser governance are included

### Scenario E. Project-specific business logic extension

Typical needs:

- custom UDFs
- custom transformers
- custom sources and sinks
- hot-loaded plugin jars
- class isolation and compatibility diagnostics

Why it matters:

- this determines whether the platform can support multiple industries or delivery projects without forking the core

### Scenario F. Migration and coexistence during V1 retirement

Typical needs:

- V1 to V2 analysis
- migration example sets
- approximation warnings
- compatibility boundaries
- side-by-side validation of business outputs

Why it matters:

- technical completeness alone is not enough; migration confidence is required

## Scenario Capability Matrix

| Scenario | Current Baseline | Future Capabilities Still Needed | Priority |
|---|---|---|---|
| Synthetic data generation | iterator sources, SQL transform, first faker/UDF batch, multi-sink | broader faker catalog, official non-SQL transformer, related-record generation patterns, replay/reporting | P0 |
| Multi-source enrichment | multi-source SQL, joins, grouping, distinct, query-source migration aids | richer SQL, better join authoring, source policy clarity, preview/explain, lookup performance modes | P0 |
| File / database / message conversion | CSV/JSON/Excel/JDBC/Kafka/ES baseline | streaming, batching, retry/idempotency, richer format mapping, schema governance | P0 |
| AI-assisted structured generation | `AiSourceVO`, deterministic providers, Ollama bridge | prompt templates, parser registry, timeout/retry/rate limits, cost and response tracing | P1 |
| Project-specific extension | PF4J, UDF extension, runtime refresh | typed custom transformer SPI, plugin governance, signed packaging, richer non-happy-path diagnostics | P0 |
| V1 migration and retirement | parity scorecard, migration examples, analysis hints | migration assistant, scenario template library, acceptance scorecards, retirement gates | P1 |

## Product Requirements By Layer

### Execution Core Requirements

### Source and connection requirements

The future product should support a Seatunnel-style source model.

Requirements:

- allow inline connection information in source definitions where repository policy permits
- keep datasource-id-based references for centrally managed connections
- support hot loading and refresh of datasource definitions
- keep read configuration next to the source instead of scattering it across old stage concepts
- allow future plugin-provided sources without changing the core model
- support explicit schema and schema inference, with clear precedence rules
- add secret reference support instead of relying on plain-text credentials long term

Recommended future source families:

- JDBC and dynamic relational endpoints
- CSV / JSON / Excel
- AI providers
- HTTP or API-driven sources only when a concrete business need appears
- future plugin-provided domain or industry sources

### Transformer requirements

SQL should remain the default transformer family, but not the only family.

Requirements:

- keep `SqlTransformVO` as the preferred path for relational projection, filter, join, aggregate, and lookup logic
- introduce at least one official non-SQL built-in transformer for repeated residual logic that is awkward in SQL + UDF form
- support typed custom transformer subtypes through the same registry and plugin mechanism as built-in transforms
- require every transformer to declare or infer output schema clearly
- keep transformer execution linear and ordered; do not move to arbitrary DAG transformers in the near term
- make the SQL/UDF vs built-in non-SQL vs custom transformer boundary explicit in docs and validation guidance

Recommended first non-SQL product direction:

- a deterministic expression or script-style transformer for row-local logic that should not force SQL growth forever

### Sink and delivery requirements

Requirements:

- keep multi-sink fan-out as a first-class product feature
- make sink failure policy configurable by template or sink group
- add retry, idempotency, and partial-success reporting where delivery scenarios require them
- decide case by case whether MySQL/Postgres/ClickHouse should stay on generic JDBC or gain dedicated sink behavior
- support future plugin-provided sink types without changing the orchestration model

### Runtime scale requirements

The current in-memory skeleton should evolve toward larger workloads.

Requirements:

- add streaming or chunked read paths where materialization is too expensive
- support paging and bounded in-memory execution for large query results
- add batch flush control for sinks
- expose runtime concurrency and buffering policy explicitly
- define memory and row-count protection for unsafe templates

### Control Plane Requirements

### Authoring and validation

Requirements:

- validate templates structurally and semantically before run time
- provide explain output for source schemas, transformer chain, and sink plan
- support preview and dry-run for a bounded sample of rows
- support middle-stage preview for multi-transform templates
- keep diagnostics actionable at source, transformer, and sink granularity

### Task execution lifecycle

Requirements:

- support ad hoc run, scheduled run, replay, retry, and cancellation
- define in-flight behavior during datasource or plugin refresh
- record run identifiers, template version, plugin set, and datasource snapshot
- support future checkpoint or resume only if real scenarios justify the complexity

### Observability and debugging

Requirements:

- expose row counts per source, transformer, and sink
- expose filtered, error, and success counts
- keep error samples and context for failed rows or sink deliveries
- retain AI response diagnostics where AI scenarios are enabled
- provide sink preflight checks where possible

### Governance Plane Requirements

### Template governance

Requirements:

- draft, publish, rollback, and version comparison
- environment promotion rules
- association between a run and the exact template version used
- policy-driven template validation gates before publish

### Datasource and secret governance

Requirements:

- central catalog of managed connections
- template-owned inline connection policy with audit
- encrypted or referenced secrets
- connectivity test and health visibility
- hot-reload audit trail

### Plugin governance

Requirements:

- install, enable, disable, remove, and refresh lifecycle
- capability and subtype collision detection
- compatibility checks against host version and plugin version
- classloader isolation diagnostics
- signed or trusted plugin packaging if external distribution grows

### Security, audit, and quotas

Requirements:

- role-based permissions for edit, run, publish, datasource use, and plugin actions
- audit trail for template changes, task runs, datasource updates, and plugin lifecycle
- quota and rate controls for expensive AI or heavy data workloads

## Product Capability Backlog

### P0 - Mainline replacement foundation

These items should be planned and delivered first because they decide whether V2 can become the mainline product path.

- official non-SQL transformer family
- typed custom transformer SPI
- formal SQL/UDF vs custom transformer decision rules
- clearer `SourcePolicyVO` boundary against V1 selection semantics
- explain / preview / dry-run and stronger diagnostics
- datasource hot load, inline connection governance, and secret handling
- streaming or chunked execution basics for larger datasets
- sink retry / idempotency / partial-success model where applicable

Acceptance signal:

- the product can support the main V1 replacement scenarios without forcing every residual rule into SQL or every large run into full in-memory materialization

### P1 - Business delivery readiness

- data quality rules and run result reporting
- richer AI provider governance and structured parser registry
- migration assistant and scenario template library
- task lifecycle control beyond simple ad hoc execution
- plugin governance dashboards and operational diagnostics
- clearer writer dialect strategy for major relational targets

Acceptance signal:

- delivery teams can use V2 for repeated business scenarios with predictable operations and migration support

### P2 - Platformization and operating scale

- multi-tenant or multi-environment isolation
- template publication workflow and approval gates
- resource quotas, AI cost accounting, and platform-level controls
- plugin marketplace or curated extension catalog
- optional visual authoring or orchestration tooling

Acceptance signal:

- the product can be governed as a reusable platform rather than a project-specific tool

## Migration Strategy Requirements

V1 retirement should be scenario-driven, not module-driven.

Requirements:

- keep a parity scorecard by business scenario family
- maintain migration examples for high-value templates
- mark exact, approximate, and unsupported migrations clearly
- avoid promising byte-for-byte parity where V2 intentionally uses a cleaner model
- identify which V1 script patterns should become SQL/UDF, which should become custom transformers, and which should remain compatibility-only

Retirement principle:

- V1 should only be retired when the key scenario families are covered and their migration boundaries are explicit

## Product Principles

The following principles should guide future feature planning.

- keep SQL as the default authoring path for relational logic
- do not let SQL become the only extension path
- add built-in transformer families only when repeated evidence justifies them
- keep all new capabilities extensible by design
- prefer typed extension contracts over opaque config maps
- keep plugin lifecycle, validation, and refresh consistent across sources, transformers, sinks, and UDFs
- do not reintroduce V1-style stage sprawl in V2 form
- do not prioritize compatibility over architecture when the two are in direct conflict

## Non-Goals For The Near Term

The following should not be expanded prematurely.

- arbitrary transformer DAG execution
- recreating every V1 stage as a dedicated V2 transformer family
- unbounded connector proliferation without scenario evidence
- exposing PF4J-native types directly in business runtime contracts
- promising exact parity for orchestration-heavy V1 features before a separate V2 orchestration design exists

## Recommended Next Planning Artifacts

After this roadmap, the most useful follow-up planning artifacts are:

1. a control-plane requirements document
   - explain / preview / dry-run
   - task lifecycle
   - run reporting
2. a migration program document
   - V1 scenario inventory
   - parity gates
   - migration assistant scope
