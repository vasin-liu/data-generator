# V1 → V2 Native Capability Gap Matrix

## Metadata

| Field | Value |
|-------|-------|
| Status | **Living document** (2026-06-07) |
| Policy | V2-only greenfield; **no** V1 YAML compatibility or V1 stage reuse |
| Parent | `docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md` |
| Historical reference | `docs/calcite-v1-parity-scorecard.md` (migration context only) |

## How to read this matrix

| Status | Meaning |
|--------|---------|
| **Done** | V2-native path shipped (engine + console or documented operator path) |
| **Partial** | Runtime or console covers subset; gap noted |
| **Planned** | Agreed V2-native design; not yet shipped |
| **Defer** | Explicitly out of current cycle |
| **N/A** | V1 concept intentionally dropped (use different V2 layer) |

**V2 replacement layer:** L0 linear template · L1 transform DAG (compute block) · L2 workflow · L3 template pipeline (Phase D).

---

## 1. Orchestration (V1 stages → L2 workflow)

| V1 capability | V2-native replacement | Status | Notes |
|---------------|----------------------|--------|-------|
| PAUSE stage | `pause` workflow step + `PAUSED` job status | **Done** | Manual pause + duration pause |
| LOG stage | `log` workflow step + run report | **Done** | |
| SHARED stage | `shared_scope` step + `#shared` in SpEL/SQL | **Partial** | Engine + editor fields; thin E2E/docs |
| Iterator choose/otherwise | `branch` workflow step (SpEL condition) | **Done** | Not SQL transform |
| Multi-block orchestration | `invoke_compute_block` + `computeBlocks[]` | **Done** | |
| Generator scheduling (async batch) | `executionPolicy` (`partitionCount`, `sinkBatchSize`, `CHUNKED`/`STREAMING`) | **Partial** | `GeneratorVO` on draft unused at runtime; use execution policy |
| Iterator pause (throttle) | `pause` step with `durationMs` | **Done** | |

---

## 2. Sources & materialization (V1 readers/iterators → named sources)

| V1 capability | V2-native replacement | Status | Notes |
|---------------|----------------------|--------|-------|
| Number/datetime/geo iterators | `iterator` source | **Done** | |
| Constant iterator (single value) | `iterator` type `constant` | **Done** | Finite repeat only |
| **Constant reader / inline table** | **`inline_rows` source** | **Done** | Arbitrary column rows in template |
| JDBC / database iterator | `query` source + inline or registry JDBC | **Done** | |
| CSV / JSON / Excel | `csv` / `json` / `excel` sources | **Done** | |
| AI reader | `ai` source | **Done** | |
| Reader EQUAL / WEIGHT dispatch | `materializationPolicy` `EQUAL` / `WEIGHTED` | **Done** | Engine + console |
| Value select ONCE / REPEAT / MULTIPLE | `materializationPolicy` `ONCE` / `ORDERED` / `LIMIT` | **Partial** | V2 semantics differ from V1 byte-for-byte |
| Legacy `SourcePolicyVO` | Deprecated; prefer `materializationPolicy` | **Partial** | Still in runtime for old drafts |
| Inline JDBC endpoint | `dataSource` on `query` / sink writer | **Done** | `InlineDataSourceVO` |
| PostGIS query | `postgis_query` source | **Done** | Engine |
| Kafka/ES cluster registry | Console datasources API + `DatasourcesPage` | **Done** | Not `application.yaml`-only |

---

## 3. Transforms (V1 stages → L1 chain or DAG)

| V1 capability | V2-native replacement | Status | Notes |
|---------------|----------------------|--------|-------|
| SCRIPT plain | SQL literals / expressions | **Done** | |
| MAPPING / CONDITION | SQL `CASE` / `WHERE` | **Done** | |
| CONVERT | `CAST` + UDFs | **Done** | |
| SCRIPT SpEL | `spel` transform (column expressions) | **Done** | Console + engine |
| SCRIPT JavaScript | `js` transform (GraalJS sandbox) | **Done** | |
| Non-SQL custom logic | `CustomTransformVO` / PF4J plugin | **Partial** | SPI exists; few built-in examples |
| Field `dependsOn` graph | SQL projection + L1 DAG edges | **Done** | |
| Linear transformer chain | `transformers[]` or compute block | **Done** | |
| Transform DAG | `transformGraph` in compute block | **Done** | Console `TransformDagEditor` |
| Staged preview (partial chain) | `throughTransformIndex` (linear) | **Partial** | API + Review UI for linear only |
| Staged preview per DAG node | `throughTransformNodeId` (proposed) | **Planned** | Needs API + UI |

---

## 4. Sinks & execution scale

| V1 capability | V2-native replacement | Status | Notes |
|---------------|----------------------|--------|-------|
| Console / JDBC / Kafka / ES / file sinks | V2 sink adapters | **Done** | |
| MySQL / Postgres / ClickHouse dialect writers | Generic JDBC sink | **Partial** | No dialect-specific bulk/COPY yet |
| Sink failure policy | `sinkExecutionPolicy` FAIL_FAST / CONTINUE | **Done** | Partial-success reporting thin |
| Parallel multi-sink | Sequential fan-out only | **Planned** | Defer unless product requires |
| CHUNKED JDBC | `executionPolicy.mode: CHUNKED` | **Done** | Scenarios C/D |
| STREAMING JDBC | `executionPolicy.mode: STREAMING` | **Done** | Scenario E |
| V1 `template.generator` batching | `executionPolicy.sinkBatchSize` + `partitionCount` | **Partial** | Document mapping; no `GeneratorVO` runner |

---

## 5. Operations & governance (Phase B)

| V1 capability | V2-native replacement | Status | Notes |
|---------------|----------------------|--------|-------|
| Template publish | DRAFT → PUBLISHED lifecycle | **Done** | |
| RBAC | `X-Console-Role` + `ConsoleAuthorizationFilter` | **Partial** | Staging profile + E2E in progress |
| Audit trail | `audit_event` + Audit page | **Done** | |
| Cron schedules | `task_schedule` + Schedules page | **Done** | Requires PUBLISHED |
| Workflow job pause/resume | Job detail + API | **Done** | `pause_reason` column (schema migration) |
| Secret refs | `passwordSecretRef` + governance | **Done** | |

---

## 6. Explicit deferrals (not V1 parity gaps for this cycle)

| Item | V2 stance |
|------|-----------|
| V1 template load / dual-run / migration UI | **Removed** (Wave 0) |
| Byte-for-byte V1 selector RNG | **N/A** — V2 `MaterializationPolicyVO` defines own semantics |
| Phase D inter-template pipeline DAG | **Defer** — API reservations only |
| C2 multi-node distributed | **Defer** — after C Done + B-lite Done |
| Full Faker/SpEL UDF long tail | **Incremental** — add UDFs when templates need them |

---

## 7. Recommended implementation packs (V2-native)

### Pack V1 — Static & lookup data (shipped 2026-06-07)

1. **`inline_rows` source** — multi-column static rows (replaces V1 constant reader).
2. Scenario `scenario-inline-rows.yaml` + IT.
3. Console source kind `inline_rows` (JSON rows editor).

### Pack V2 — Selection & scale clarity

1. Deprecation note in UI for legacy `SourcePolicyVO` fields.
2. Execution step copy: map V1 generator batching → `executionPolicy`.
3. Shared-scope E2E (`scenario-wf-shared-state.yaml`).

### Pack V3 — Transform & preview depth

1. DAG node staged preview API + Review UI.
2. Official **lookup join** scenario in catalog (GF-B variant using `scenario-b-lookup-join.yaml`).
3. Custom transform PF4J sample in docs.

### Pack V4 — Sink & runtime polish

1. Partial-success metrics on CONTINUE_ON_ERROR.
2. Dialect JDBC sink options (or documented generic JDBC limits).
3. Optional parallel sink execution spike.

---

## 8. Gate alignment

| Product gate | Remaining V1-gap work |
|--------------|----------------------|
| **C Done** | Scenario wizard (Done), branch editor (Done), catalog E2E (Done), DAG staged preview UI, shared-state E2E |
| **B-lite Done** | RBAC staging E2E (in progress), catalog run publish gate |
| **V2 parity (operator trust)** | Pack V1–V2 above |

---

## References

- `docs/template-v2-workflow-authoring-guide.md`
- `docs/template-v2-policy-to-runtime-mapping-guide.md`
- `docs/superpowers/specs/2026-06-02-c-b-lite-done-gap-checklist.md`
- `data-generator-service/src/main/resources/template/v2-scenarios/`
