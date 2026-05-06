# Calcite Coverage Matrix

## Goal

Assess which current repository capabilities can be covered by the planned Calcite-based V2 path, how they should be covered, and what should remain in the orchestration layer instead of moving into SQL transformation.

## Coverage Scale

- `High`: can be covered directly or with a thin adapter
- `Medium`: can be covered partially, but requires compatibility fallbacks or model constraints
- `Low`: only a narrow subset should move to V2
- `No`: should remain outside the Calcite transformation path

## Coverage Matrix

| Capability Area | Current Capability | Current Location | V2 Coverage | Coverage Method | Keep Compatibility Layer | Priority | Notes |
|---|---|---|---|---|---|---|---|
| Row model | `Value / SingleValue / ListValue / MapValue` | core value layer and pipelines | Medium | Introduce `Row + RowSchema` as V2 primary model | Yes | P0 | V2 should stop using `Value` as the main data path, but V1 must keep it |
| Field dependency graph | `FieldVO.dependsOn` + DAG ordering | `DefaultRowPipelineFactory` | High | Replace most dependency semantics with SQL projection dependency analysis | Yes | P0 | Only for V2 SQL templates; V1 still needs DAG |
| Field stages | `READ / SELECT / SCRIPT / MAPPING / CONDITION / CONVERT` | stage model | Medium | Collapse transform-oriented stages into SQL/UDFs | Yes | P0 | Not all stage types belong in SQL |
| Read stage | `ReadStageVO` | core stage model | Medium | Move reader semantics to `source` definitions | Yes | P0 | Read as a stage becomes less central in V2, but source semantics become richer |
| Select stage | dataset selection and sampling | `SelectStage`, strategy types | Medium | Preserve as `SourcePolicy` instead of pushing into SQL | Yes | P0 | Selection is a V2 source concern, not a Calcite transform concern |
| Script stage | script-based value derivation | `ScriptStage` | Medium | Replace expression-like scripts with SQL expressions and UDFs | Yes | P0 | Process-style scripts should remain compatible-only |
| Mapping stage | value mapping | `MappingStage` | High | Replace with `CASE WHEN` or lookup UDFs | Yes | P0 | Good migration candidate |
| Condition stage | conditional branching | `ConditionStage` | High | Replace row-local conditions with `CASE WHEN` or `WHERE` | Yes | P0 | Only row-local logic maps cleanly |
| Convert stage | type/format conversion | `ConvertStage` | High | Replace with `CAST`, conversion functions, UDFs | Yes | P0 | Good migration candidate |
| Log stage | logging of intermediate data | `LogStage` | No | Keep in orchestration/debug layer | Yes | P2 | Not a SQL transform concern |
| Pause stage | execution throttling and waiting | `PauseStage` | No | Keep in orchestration layer | Yes | P2 | Runtime control, not data transformation |
| Shared stage | shared state behavior | `SharedStage` | Low | Keep outside SQL path unless a narrow V2 equivalent is needed | Yes | P2 | Requires explicit runtime semantics |
| Iterator: number | generated numeric sequence | iterator modules | High | Adapt into `RowSource` | No | P0 | Strong V2 source candidate |
| Iterator: constant | generated constant dataset | iterator modules | High | Adapt into `RowSource` | No | P0 | Strong V2 source candidate |
| Iterator: datetime | generated time sequence | iterator modules | High | Adapt into `RowSource` | No | P0 | Strong V2 source candidate |
| Iterator: database | paged DB iterator | iterator modules | High | Converge with JDBC-backed source family | No | P0 | Should not remain a distinct long-term V2 concept |
| Iterator: csv/excel/json | file-backed iterators | iterator modules | High | CSV/JSON/Excel now have first-pass `RowSource` | No | P1 | Strong V2 source candidate |
| Reader: constant | in-memory constants | reader modules | High | Adapt into `RowSource` or inline logical table | No | P1 | Can also become SQL literal tables later |
| Reader: JDBC | query-based read | reader modules | High | Converge with database iterator into one query-backed source family | No | P0 | Natural source path |
| Reader: CSV/Excel/JSON | file-backed reads | reader modules | High | CSV/JSON/Excel now have first-pass `RowSource`; richer Excel compatibility examples still needed | No | P1 | Natural source path |
| Reader: SpEL | expression-driven read | reader modules | Medium | Replace simple cases with UDFs; preserve complex cases | Yes | P1 | Expression-only subset is migratable |
| Reader: AI | remote/AI backed read | reader modules | High | Official `AiSourceVO` with deterministic providers and a concrete Ollama-backed runtime bridge | Yes | P1 | Prefer a first-class V2 source, potentially aligned with Spring AI or plugin-provided providers later |
| Script: Plain | constant passthrough | script modules | High | Usually eliminate or replace with SQL literals | Yes | P1 | Often trivial to migrate |
| Script: SpEL | expression engine | script modules | Medium | Migrate expression subset to SQL/UDFs | Yes | P0 | Requires a function compatibility map |
| Script: JavaScript | procedural script engine | script modules | Low | Keep as compatibility-only for non-expression logic | Yes | P2 | Not a good direct SQL target |
| Generator sync/async | execution mode, batching, threads | generator modules | No | Keep orchestration layer unchanged initially | No | P0 | Calcite should not replace scheduling |
| Iterator control flow | choose/otherwise/pause in iterator path | generator/iterator execution | Low | Preserve in orchestration, optionally revisit later | Yes | P2 | Not a first-phase V2 concern |
| Sink: console | console output | writer modules | High | `Row -> writer payload` adapter | No | P0 | First sink to enable |
| Sink: DB writers | JDBC/MySQL/Postgres/ClickHouse | writer modules | High | `Row -> writer payload` adapter | No | P1 | Strong reuse candidate |
| Sink: Kafka | dynamic Kafka output | writer modules | High | `Row -> writer payload` adapter | No | P1 | Strong reuse candidate |
| Sink: Elasticsearch | dynamic ES output | writer modules | High | `Row -> writer payload` adapter | No | P1 | Strong reuse candidate |
| Sink: CSV/Excel/JSON | file output | writer modules | High | CSV/JSON/Excel now have first-pass `RowSink` | No | P2 | Lower priority than console/db |

## Coverage Summary

### High-coverage candidates

- field derivation
- mapping
- conversion
- row-local conditions
- iterator sources
- file/database readers as sources
- writer reuse through sink adapters

### Medium-coverage candidates

- script-based logic
- selection strategies
- read stage semantics
- AI reader path
- row/value model transition

### Low or no direct coverage candidates

- pause/log/shared stages
- generator scheduling and batching
- iterator-side control flow
- procedural JavaScript semantics

## Recommended Coverage Strategy

### Phase A - Cover the transformation core first

Cover first:

- `FieldVO.dependsOn`
- `ScriptStage` expression subset
- `MappingStage`
- `ConditionStage`
- `ConvertStage`

Approach:

- move row transformation logic into `transform.sql`
- register repository-local UDFs for common faker/date/string behavior
- keep V1 stage execution unchanged for templates not migrated yet

Expected result:

- most current row transformation templates can be rewritten without field DAG orchestration

### Phase B - Normalize source inputs

Cover next:

- number/constant/datetime iterators
- JDBC/CSV/Excel/JSON readers
- convergence of `DatabaseIterator` and `JdbcReader`
- source policy for selection, caching, and materialization
- official AI-backed source path

Approach:

- unify them under `RowSource`
- assign logical table names and explicit schemas
- let Calcite read from logical tables instead of field-level read chains
- move selection semantics into source policy

Expected result:

- V2 templates no longer need `READ + SELECT` for common source acquisition

### Phase C - Reuse sinks

Cover next:

- console
- database writers
- Kafka/Elasticsearch writers
- multi-sink fan-out
- configurable multi-sink failure handling

Approach:

- build `Row -> sink payload` adapters
- avoid rewriting writer internals first
- support configuration-driven sink failure modes

Expected result:

- V2 uses the current output ecosystem with minimal disruption

### Phase D - Reduce script dependency

Cover gradually:

- plain scripts
- simple SpEL expressions

Preserve as compatibility-only:

- complex SpEL object graph usage
- JavaScript procedural logic

Approach:

- define a V1-script to SQL/UDF mapping guide
- migrate only the expression subset

Expected result:

- SQL becomes the default transformation language
- scripts remain as fallback compatibility

### Phase E - Keep orchestration concerns outside Calcite

Do not force into V2 transform:

- pause
- log
- generator threading
- batch scheduling
- shared-state side effects

Approach:

- keep these in the orchestration/runtime layer
- only revisit if V2 requires a narrowly scoped equivalent

Expected result:

- Calcite stays focused on row transformation, not workflow control

### Phase F - Reach and exceed V1 functional parity

- cover the remaining V1 template families intentionally
- measure parity by capability, not by module count
- keep V1 only for features that are still intentionally compatibility-only

Expected result:

- V2 becomes the preferred authoring model and surpasses V1 in composability and extension points

## Phased Adoption Plan

### Phase 1 - Minimum viable V2

- [x] Cover row model with `Row + RowSchema`
- [~] Cover iterator number/constant/datetime as `RowSource`
- [x] Cover `SELECT` projection, alias, arithmetic, `CASE WHEN`, `WHERE`
- [x] Cover console sink
- [x] Cover first UDF batch

Success criteria:

- `iterator -> sql -> console` works end to end
- simple V1 field-stage templates can be rewritten as V2 SQL templates

Current gap:

- number, constant, datetime, and file-backed iterator/source coverage are now in place; the remaining iterator-side gap is compatibility-only iterator control flow

### Phase 2 - Practical source/sink coverage

- [x] Cover JDBC reader as source
- [x] Cover CSV/Excel/JSON readers as sources
- [x] Converge `DatabaseIterator` and JDBC reader into one V2 query-backed source family
- [x] Add official `AiSourceVO`
- [x] Keep `SelectStrategy` semantics as source policy
- [x] Cover DB sink
- [x] Cover Kafka sink
- [x] Cover Elasticsearch sink
- [x] Cover multi-sink fan-out with configurable failure policy

Success criteria:

- most repository-owned demo templates can be re-expressed with V2 sources and sinks

Current gap:

- CSV/Excel/JSON sources and sinks now have first-pass V2 coverage
- concrete Ollama-backed AI bridge is now in place behind `AiRuntimeBridge`; broader provider coverage remains pending
- `SourcePolicyVO` currently covers ordered/random materialization aliases plus `limit`, but not full V1 consumptive `SELECT` semantics such as depletion, repeated-use counts, or weighted reader pools
- the migration examples now also show that some selection-heavy templates should be rewritten explicitly in V2 relational form instead of being mapped mechanically into `SourcePolicyVO`
- multi-source migration candidate generation now infers simple lookup joins from param names and source schemas, but composite/business-specific join semantics still need explicit author review
- multi-source candidate SQL now emits explicit projection aliases when source schemas can be resolved, reducing duplicate-column ambiguity during V2 authoring and sink mapping
- structural join hints now cover common business scope columns, simple composite business keys, and basic date-window predicates; more complex temporal logic still needs explicit author review
- parameterized lookup guidance now explicitly points users toward relational source rewrites instead of implying that per-row param execution is already a first-class V2 join runtime

### Phase 3 - Transformation migration coverage

- [x] Cover mapping migration patterns
- [x] Cover conversion migration patterns
- [x] Cover row-local condition migration patterns
- [~] Cover simple SpEL migration patterns

Success criteria:

- a large share of current business templates can move from V1 field DAGs to V2 SQL transforms

Current gap:

- simple SpEL has a first UDF extension path and first migration examples, but the broad repository-local compatibility catalog is still incomplete

### Phase 4 - Compatibility stabilization

- [ ] Keep non-migrated V1 templates green
- [ ] Document unsupported direct migrations
- [ ] Mark V1-only features as compatibility-only
- [~] Build a migration guide and template examples

### Phase 5 - V1 parity and beyond

- [ ] Define a parity scorecard for V1 business scenarios
- [ ] Migrate representative orchestration-light business templates to V2
- [ ] Verify that V2 covers the intended V1 feature surface
- [ ] Add at least one V2-only enhancement path that is cleaner than V1

Success criteria:

- V1 and V2 coexistence is stable
- migration choices are explicit instead of implicit

## Recommended Priority by ROI

P0:

- row model
- field DAG replacement
- mapping/condition/convert replacement
- expression-subset script replacement
- iterator source adapters
- console sink

P1:

- query-backed source convergence
- official AI source
- DB/Kafka/ES sink adapters
- multi-sink policy

P2:

- AI source adaptation
- compatibility-only script paths
- log/pause/shared semantics documentation

## Recommendation

The repository should target a mixed end state:

- V2 becomes the default path for row transformation and declarative template authoring.
- V1 remains as a compatibility path for orchestration-heavy, selection-heavy, and script-heavy templates that do not map cleanly to SQL.

This means the realistic first target is not `100% replacement`, but:

- full coverage of source/sink infrastructure reuse
- high coverage of row transformation semantics
- partial coverage of script/selection semantics
- explicit non-coverage of orchestration and side-effect stages

With the current agreed direction, the long-term target is:

- V2 reaches V1 functional parity for business-relevant template families
- V2 exceeds V1 in authoring simplicity, source composition, and sink fan-out behavior
- V1 remains available during the migration window and for explicitly compatibility-only features
