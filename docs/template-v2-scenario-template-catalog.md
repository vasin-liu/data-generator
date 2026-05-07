# Template V2 Scenario Template Catalog And Acceptance Matrix

## Purpose

This document defines the scenario catalog and acceptance matrix for Template V2.

It turns the product roadmap and migration program into a concrete scenario-level backlog:

- which representative template families the product should support
- which sample templates should exist for each family
- what evidence is required before a scenario family is considered covered
- which scenarios should be treated as exact, adapted, approximate, or compatibility-only

This is the bridge between broad architecture planning and concrete scenario acceptance.

Related references:

- `docs/template-v2-product-roadmap.md`
- `docs/template-v2-transformer-strategy.md`
- `docs/template-v2-datasource-and-secret-governance.md`
- `docs/template-v2-control-plane-requirements.md`
- `docs/template-v2-migration-program.md`
- `docs/calcite-v1-parity-scorecard.md`
- `docs/calcite-v1-v2-migration-examples.md`
- `docs/calcite-implementation-status.md`

## Goal

Template V2 should be accepted scenario by scenario, not only feature by feature.

The catalog should help answer:

- which scenario families already have a credible V2 path
- which ones still need source, transformer, sink, or governance work
- what sample artifacts and test evidence should exist
- what migration confidence is currently justified

## Catalog Principles

- catalog scenario families, not only low-level features
- require at least one representative template per high-value scenario family
- keep acceptance evidence explicit
- separate exact support from adapted or approximate migration
- avoid treating compatibility-only scenarios as hidden failures
- prefer repository-real examples over synthetic examples wherever possible

## Acceptance Status Model

Each scenario template should be tracked with one of the following outcomes.

- `Exact`
  - V2 reproduces the intended behavior without material approximation
- `Adapted`
  - V2 uses a cleaner or different model, but business behavior is still acceptable
- `Approximate`
  - V2 is usable, but one or more known semantics differ and need review
- `Compatibility-only`
  - scenario should remain on V1 until a later V2 capability exists

## Evidence Types

Acceptance should not rely on intuition alone.

Recommended evidence types:

- focused unit or integration tests
- runnable sample template
- migration example entry
- preview or explain output review
- dual-run or output comparison evidence where needed
- business sign-off for sensitive scenarios

## Scenario Families

The current V2 direction suggests six main scenario families.

### Family A. Synthetic generation

Typical patterns:

- iterator-only generation
- iterator plus faker/UDF shaping
- related synthetic rows
- time-series or event generation

### Family B. Query-backed lookup and enrichment

Typical patterns:

- one query source plus SQL projection
- query source plus lookup join
- multi-query-source enrichment
- grouped or deduplicated query outputs

### Family C. File / database / message conversion

Typical patterns:

- CSV/JSON/Excel to database
- database to CSV/JSON/Excel
- database to Kafka
- file or query to Elasticsearch

### Family D. AI-assisted structured generation

Typical patterns:

- AI source plus SQL projection
- AI source plus parser and schema constraints
- AI source plus sink fan-out

### Family E. Extension-heavy business logic

Typical patterns:

- plugin UDF
- custom transformer
- plugin-provided source or sink
- mixed built-in plus plugin execution

### Family F. Compatibility-boundary scenarios

Typical patterns:

- orchestration-heavy V1 flows
- iterator branching
- shared-state or procedural script behavior
- exact V1 source-consumption semantics not yet modeled in V2

## Recommended Catalog Entries

Each catalog entry should eventually map to one or more concrete sample templates and test coverage.

| Scenario Id | Family | Representative Scenario | Expected Primary V2 Shape | Current Acceptance Target |
|---|---|---|---|---|
| `SG-01` | Synthetic generation | number iterator -> SQL projection -> console | iterator source + SQL + console sink | Exact |
| `SG-02` | Synthetic generation | datetime iterator -> faker/date shaping -> file sink | iterator source + SQL/UDF + CSV/JSON sink | Adapted |
| `SG-03` | Synthetic generation | parent/child or related synthetic dataset generation | multi-source or staged transformers + multi-sink | Adapted |
| `QE-01` | Query enrichment | single query source -> SQL projection -> DB sink | query source + SQL + JDBC sink | Exact |
| `QE-02` | Query enrichment | query source + lookup query source -> join -> DB sink | multi-source SQL + JDBC sink | Adapted |
| `QE-03` | Query enrichment | grouped or deduplicated query result publication | query source + SQL aggregate/distinct + sink | Adapted |
| `CV-01` | Conversion | CSV -> SQL normalize -> DB | file source + SQL + JDBC sink | Exact |
| `CV-02` | Conversion | DB -> SQL shape -> Kafka | query source + SQL + Kafka sink | Adapted |
| `CV-03` | Conversion | DB or file -> SQL shape -> Elasticsearch | query or file source + SQL + ES sink | Adapted |
| `AI-01` | AI generation | AI source -> SQL projection -> console | AI source + SQL + console | Adapted |
| `AI-02` | AI generation | AI source -> parser/schema -> DB or file | AI source + parser + SQL/custom transform + sink | Approximate |
| `EXT-01` | Extension | built-in SQL + plugin UDF | SQL + plugin UDF | Exact |
| `EXT-02` | Extension | built-in source + custom transformer + built-in sink | typed custom transformer + sink | Adapted |
| `EXT-03` | Extension | plugin source + plugin/custom transform + plugin sink | pluginized full path | Adapted |
| `CB-01` | Compatibility boundary | V1 source policy exact once/weight semantics | source policy or explicit V2 rewrite | Approximate |
| `CB-02` | Compatibility boundary | V1 JavaScript/procedural scripts | custom transform or V1 fallback | Compatibility-only |
| `CB-03` | Compatibility boundary | pause/shared/orchestration-heavy flow | future orchestration or V1 fallback | Compatibility-only |

## Acceptance Matrix

The product should evaluate each scenario entry against the same matrix.

| Dimension | What Must Be Proven | Typical Evidence |
|---|---|---|
| Runtime support | sources, transformers, sinks execute end to end | focused tests, sample run |
| Authoring model | template shape is understandable and not excessively awkward | sample template review, explain output |
| Validation | structural and semantic validation catch common errors | validation tests, preview failures |
| Diagnostics | failures identify source/transformer/sink and extension context | run report, failure fixtures |
| Governance | datasource, secret, and plugin rules are satisfied | governance policy review, preflight |
| Migration confidence | exact/adapted/approximate boundary is explicit | migration example, comparison notes |
| Operational viability | preview, run, retry, and sink behavior are acceptable | control-plane evidence, task reports |

## Detailed Family Acceptance Guidance

### Family A. Synthetic generation

Minimum acceptance:

- iterator-backed source works
- SQL/UDF path handles common shaping
- representative sink path exists
- sample output can be previewed and explained

Higher acceptance:

- related synthetic datasets documented
- replay and report semantics available
- first non-SQL transformer path available for residual logic

### Family B. Query-backed lookup and enrichment

Minimum acceptance:

- query source with schema handling works
- join or lookup scenario works
- distinct/grouping coverage exists where needed
- source-policy approximations are surfaced clearly

Higher acceptance:

- richer SQL shapes such as union or subquery where business scenarios demand them
- multi-source explain and preview are usable

### Family C. File / database / message conversion

Minimum acceptance:

- one source family to one sink family path runs end to end
- schema conversion is visible
- sink failure behavior is explicit

Higher acceptance:

- bounded streaming or batch behavior documented
- idempotency and retry expectations are explicit

### Family D. AI-assisted structured generation

Minimum acceptance:

- provider and parser path is testable without live network dependency
- schema-constrained output path is documented
- run report records provider, parser, and diagnostics

Higher acceptance:

- timeout, retry, fallback, and cost reporting

### Family E. Extension-heavy business logic

Minimum acceptance:

- plugin UDF path works
- custom transformer path is documented and tested
- plugin diagnostics and refresh are acceptable

Higher acceptance:

- one mixed built-in and plugin scenario per major extension type

### Family F. Compatibility-boundary scenarios

Acceptance rule:

- these scenarios should not be mislabeled as covered
- they must remain explicitly marked as approximate or compatibility-only until the required V2 capability exists

## Template Artifact Recommendations

Each high-priority scenario should ideally have:

- one concise reference template
- one migration-oriented example
- one automated test or fixture
- one acceptance note showing exact, adapted, approximate, or compatibility-only status

Recommended future artifact shapes:

- `docs/examples/template-v2-<scenario>.yaml`
- `docs/examples/template-v2-<scenario>-notes.md`
- test fixture or sample data under the relevant module

## Suggested First Catalog Backlog

The first scenario backlog should prioritize business value and migration pressure.

### P0 catalog entries

- `SG-01`
- `SG-02`
- `QE-01`
- `QE-02`
- `CV-01`
- `CV-02`
- `EXT-01`
- `CB-01`

### P1 catalog entries

- `SG-03`
- `QE-03`
- `CV-03`
- `AI-01`
- `EXT-02`

### P2 catalog entries

- `AI-02`
- `EXT-03`
- `CB-02`
- `CB-03`

## Suggested Acceptance Workflow

### Step 1. Register scenario

- assign scenario id
- assign family
- state expected V2 shape

### Step 2. Attach evidence

- sample template
- tests or fixture
- explain/preview evidence
- migration notes

### Step 3. Classify acceptance

- mark as exact, adapted, approximate, or compatibility-only

### Step 4. Review periodically

- upgrade or downgrade classification as capabilities change
- keep links to evidence current

## Relationship To Existing Docs

This catalog should be used together with:

- `docs/calcite-v1-parity-scorecard.md`
  - capability and parity snapshot
- `docs/calcite-v1-v2-migration-examples.md`
  - example migration narratives
- `docs/template-v2-migration-program.md`
  - migration waves and gates

The catalog is the scenario-index layer above those documents.

## Acceptance Exit Criteria

The catalog is useful when:

- high-value scenario families have explicit entries
- each important scenario has sample and evidence links
- exact versus approximate boundaries are visible
- compatibility-only scenarios are intentional instead of hidden
- roadmap discussions can point to scenario ids instead of broad vague labels

## Non-Goals

The scenario catalog should avoid:

- pretending every scenario must be exact before V2 is usable
- becoming a low-level feature checklist with no business context
- hiding compatibility gaps under generic "supported" labels
