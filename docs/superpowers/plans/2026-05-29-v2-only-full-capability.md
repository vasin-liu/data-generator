# V2-Only Full Capability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Phase **A** (production batch + observability + Phase D reservations) and Phase **A′** (V2-native workflow, transform DAG, JS, selectors) per `docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md`, without historical V1 template compatibility.

**Architecture:** Keep linear `TemplateV2VO` on `master` working. Add `StreamingPipeline` beside `InMemoryPipeline` / `ChunkedPipeline`. Introduce optional `workflow` + `computeBlocks[]` with `WorkflowRunner` → `ComputeBlockRunner` (L1 transform DAG inside blocks). Persist structured run reports on `task_execution`. Reserve pipeline fields for Phase D.

**Tech Stack:** Java 25, Spring Boot 4, Maven, Calcite module (`data-generator-calcite`), core V2 models (`data-generator-core`), service + React console, GraalJS (`data-generator-scripter-javascript`), embedded H2 tests (`application-phase7-test.yaml`).

**Spec:** `docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md`

**Branch:** `feature-4.1` (recommended) off current `master` / `feature-4.0`.

**Verify command (default):** `.\mvnw-jdk25.ps1 -pl data-generator-service -am test`

---

## File map (Phase A + A′)

| Path | Action | Responsibility |
|------|--------|----------------|
| `data-generator-common/.../ExecutionPolicyVO.java` | Modify | Document STREAMING; optional `maxTotalRows` |
| `data-generator-common/.../TemplateV2VO.java` | Modify | Optional `metadata`; Phase D `pipelineRef` |
| `data-generator-common/.../workflow/*.java` | Create (A′) | `WorkflowSpecVO`, step subtypes, `ComputeBlockVO` |
| `data-generator-common/.../TransformGraphVO.java` | Create (A′) | L1 `nodes` + `edges` |
| `data-generator-common/.../MaterializationPolicyVO.java` | Create (A′) | Selector semantics |
| `data-generator-common/.../JsTransformVO.java` | Create (A′) | JS transform subtype |
| `data-generator-calcite/.../StreamingPipeline.java` | Create (A) | STREAMING mode |
| `data-generator-calcite/.../TemplateV2Runner.java` | Modify | Dispatch STREAMING; workflow facade (A′) |
| `data-generator-calcite/.../WorkflowRunner.java` | Create (A′) | L2 step machine |
| `data-generator-calcite/.../ComputeBlockRunner.java` | Create (A′) | Block execution + L1 DAG |
| `data-generator-calcite/.../TransformDagExecutor.java` | Create (A′) | Topological transform execution |
| `data-generator-calcite/.../transform/JsTransformFactory.java` | Create (A′) | GraalJS row transform |
| `data-generator-calcite/.../sink/*RowSinkAdapter.java` | Modify (A) | Retry + partial counts |
| `data-generator-calcite/.../TemplateV2RunResult.java` | Modify (A) | `RunReportVO` attachment |
| `data-generator-service/.../TaskExecutionPO.java` | Modify (A) | `report_json`, `parent_pipeline_run_id`, `upstream_artifact_refs_json` |
| `data-generator-service/src/main/resources/db/schema.sql` | Modify (A) | New columns |
| `data-generator-service/.../RunReportCollector.java` | Create (A) | Metrics aggregation |
| `data-generator-service/.../TemplateV2ControlPlaneService.java` | Modify (A) | Staged preview |
| `data-generator-service/.../api/console/ConsoleJobController.java` | Modify (A) | Expose run report |
| `data-generator-service/.../TemplateV2Validator.java` | Modify (A′) | Workflow/DAG/JS validation |
| `data-generator-console-web/src/app/pages/JobDetailPage.tsx` | Modify (A) | Run report UI |
| `data-generator-console-web/src/app/editor/WorkflowPanel.tsx` | Create (A′) | Minimal step list editor |
| `data-generator-service/src/main/resources/template/v2-scenarios/**` | Create (A) | New scenario YAML (not V1 imports) |
| `data-generator-service/src/main/bundles/dev.bundle` | Delete (A) | Vaadin leftover binary |
| `docs/template-v2-streaming-execution-guide.md` | Create (A) | Operator guide |
| `docs/template-v2-workflow-authoring-guide.md` | Create (A′) | Workflow + DAG guide |

Phases **B**, **C**, **D** — see [Milestone appendix](#milestone-appendix-b-c-d) (separate plans when started).

---

# Phase A — Production batch + observability

## Task 1: Phase D run metadata reservations

**Files:**
- Modify: `data-generator-service/src/main/resources/db/schema.sql`
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/model/po/TaskExecutionPO.java`
- Modify: `data-generator-common/data-generator-core/src/main/java/org/gensokyo/data/model/v2/TemplateV2VO.java`
- Test: `data-generator-service/src/test/java/org/gensokyo/data/task/TaskExecutionServiceTests.java`

- [ ] **Step 1: Write failing test for new nullable columns**

Add to `TaskExecutionServiceTests`:

```java
@Test
void persistsPipelineReservationFields() {
    TaskExecutionPO po = new TaskExecutionPO();
    po.setId(99L);
    po.setTemplateId(1L);
    po.setInstanceId(99001L);
    po.setStatus("SUCCEEDED");
    po.setParentPipelineRunId("pipe-run-1");
    po.setUpstreamArtifactRefsJson("[{\"nodeId\":\"a\",\"artifactId\":\"art-1\"}]");
    repository.save(po);
    TaskExecutionPO loaded = repository.findById(99L).orElseThrow();
    assertThat(loaded.getParentPipelineRunId()).isEqualTo("pipe-run-1");
    assertThat(loaded.getUpstreamArtifactRefsJson()).contains("art-1");
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -Dtest=TaskExecutionServiceTests#persistsPipelineReservationFields test`

Expected: compile error (fields missing).

- [ ] **Step 3: Add schema + entity fields**

`schema.sql` (after existing `task_execution` columns):

```sql
ALTER TABLE task_execution ADD COLUMN IF NOT EXISTS parent_pipeline_run_id VARCHAR(64);
ALTER TABLE task_execution ADD COLUMN IF NOT EXISTS upstream_artifact_refs_json CLOB;
ALTER TABLE task_execution ADD COLUMN IF NOT EXISTS report_json CLOB;
```

`TaskExecutionPO`: add three fields with `@Column` matching names.

`TemplateV2VO`: add `private Map<String, Object> metadata;` (nullable; document `pipelineRef` key reserved).

- [ ] **Step 4: Run test — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(v2): reserve pipeline and run report columns on task_execution"
```

---

## Task 2: STREAMING execution mode (v1)

**Files:**
- Create: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/StreamingPipeline.java`
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/runtime/TemplateV2Runner.java`
- Test: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/StreamingPipelineTests.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void streamsQuerySourceToJdbcSinkInBatches() {
    // H2 query source, 500 rows, sinkBatchSize=100, mode=STREAMING
    TemplateV2RunResult result = runner.run(template);
    assertThat(result.getMetrics().getRowsWritten()).isEqualTo(500);
    assertThat(result.getMetrics().getPeakRowsInMemory()).isLessThanOrEqualTo(100);
}
```

- [ ] **Step 2: Run test — expect FAIL** (`STREAMING execution mode is not implemented`)

Run: `.\mvnw-jdk25.ps1 -pl data-generator-calcite -Dtest=StreamingPipelineTests test`

- [ ] **Step 3: Implement `StreamingPipeline`**

Behavior (v1 scope):
- Support **single** `QuerySourceVO` + linear SQL transforms + **one** JDBC sink (same shapes as `ChunkedPipeline` ROW_LOCAL).
- Read via `ChunkedRowSource` with `sourceChunkSize` from policy; apply `CalciteRowTransformer` per chunk; flush sink every `sinkBatchSize`.
- Reject BROADCAST_JOIN and multi-source in v1 with clear `IllegalArgumentException`.
- Record `peakRowsInMemory` on `TemplateV2RunMetrics`.

Wire in `TemplateV2Runner.run`:

```java
if ("STREAMING".equals(policy.mode())) {
    return new StreamingPipeline(this::createSink).run(template, policy, registry);
}
```

- [ ] **Step 4: Run `StreamingPipelineTests` + `TemplateV2RunnerTests` — PASS**

- [ ] **Step 5: Add `docs/template-v2-streaming-execution-guide.md`** (when to use STREAMING vs CHUNKED vs IN_MEMORY)

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(calcite): implement STREAMING execution mode for row-local jdbc export"
```

---

## Task 3: Execution guards (`maxTotalRows` / fail-fast)

**Files:**
- Modify: `data-generator-common/.../ExecutionPolicyVO.java`
- Modify: `data-generator-calcite/.../EffectiveExecutionPolicy.java`
- Modify: `data-generator-calcite/.../InMemoryPipeline.java`, `ChunkedPipeline.java`, `StreamingPipeline.java`
- Test: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/ExecutionGuardTests.java`

- [ ] **Step 1: Failing test — exceed `maxTotalRows` throws when `failOnLimitExceeded=true`**

- [ ] **Step 2: Add `Integer maxTotalRows` to `ExecutionPolicyVO`; resolve in `EffectiveExecutionPolicy`**

- [ ] **Step 3: Increment row counter in all three pipelines; throw `ExecutionLimitExceededException` with template name and limit**

- [ ] **Step 4: Validator warning when `maxTotalRows` unset on CHUNKED/STREAMING templates**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(calcite): enforce maxTotalRows execution guard across pipelines"
```

---

## Task 4: Sink retry and partial-success metrics

**Files:**
- Modify: `data-generator-common/.../SinkExecutionPolicyVO.java` (add `maxRetries`, `retryBackoffMs`)
- Modify: `data-generator-calcite/.../JdbcRowSinkAdapter.java` (and Kafka/ES if trivial)
- Modify: `data-generator-calcite/.../TemplateV2RunResult.java`, sink metrics DTO
- Test: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sink/SinkRetryPolicyTests.java`

- [ ] **Step 1: Failing test — sink fails twice then succeeds with `maxRetries=3`**

- [ ] **Step 2: Implement retry loop in `AbstractRowSinkAdapter` or JDBC adapter base**

- [ ] **Step 3: On `CONTINUE_ON_ERROR`, collect per-sink `rowsOk`, `rowsFailed`, `lastErrorSample` (max 500 chars)**

- [ ] **Step 4: Extend `TemplateV2RunResult` metrics map; assert in test**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(calcite): add sink retry policy and partial success metrics"
```

---

## Task 5: Structured run report + persistence

**Files:**
- Create: `data-generator-common/.../RunReportVO.java`, `StageMetricVO.java`
- Create: `data-generator-service/.../RunReportCollector.java`
- Modify: `data-generator-service/.../TaskExecutionService.java`
- Modify: `data-generator-service/.../task/TaskExecutionSummary.java`
- Modify: `data-generator-service/.../api/console/ConsoleJobController.java`
- Test: `data-generator-service/src/test/java/org/gensokyo/data/task/RunReportPersistenceTests.java`

- [ ] **Step 1: Define `RunReportVO`**

```java
public record RunReportVO(
    List<StageMetricVO> sources,
    List<StageMetricVO> transformers,
    List<StageMetricVO> sinks,
    String executionMode,
    Long durationMs,
    List<String> errorSamples
) {}
```

- [ ] **Step 2: Failing IT — after V2 run, `getExecution(instanceId).report().sources()` non-empty**

Use `@SpringBootTest` + `application-phase7-test.yaml` + small classpath V2 template.

- [ ] **Step 3: `RunReportCollector` hooks from `TaskController` / `TemplateV2Runner` result**

Serialize to `TaskExecutionPO.reportJson` (Jackson).

- [ ] **Step 4: Expose in `TaskExecutionSummary` and `GET /api/jobs/{instanceId}`**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(service): persist and expose structured v2 run reports"
```

---

## Task 6: Staged preview API

**Files:**
- Modify: `data-generator-service/.../TemplateV2ControlPlaneService.java`
- Modify: `data-generator-service/.../api/console/ConsoleTemplateEditorActionsController.java`
- Modify: `data-generator-service/.../api/console/dto/DraftPreviewRequest.java`
- Test: `data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2StagedPreviewTests.java`

- [ ] **Step 1: Add `Integer throughTransformIndex` (0-based, inclusive) to preview request DTO**

- [ ] **Step 2: Failing test — preview with `throughTransformIndex=0` returns fewer columns than full chain**

- [ ] **Step 3: Implement in control plane: run materialization + apply transformers subList(0, index+1) only; honor `previewRowLimit`**

- [ ] **Step 4: Console Review panel — optional dropdown "Preview through step N"**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(control-plane): add staged preview through transform index"
```

---

## Task 7: V2 scenario template library (greenfield YAML)

**Files:**
- Create: `data-generator-service/src/main/resources/template/v2-scenarios/scenario-a-synthetic.yaml`
- Create: `.../scenario-b-lookup-join.yaml`, `scenario-c-csv-export.yaml`, `scenario-d-chunked-jdbc.yaml`, `scenario-e-streaming-jdbc.yaml`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/template/V2ScenarioTemplateIT.java`
- Modify: `docs/template-v2-scenario-template-catalog.md`

- [ ] **Step 1: Author 5 YAML files** mapped to catalog scenarios A–E (new ids, `definitionKind: V2`, no V1 copy-paste)

- [ ] **Step 2: IT loads each from classpath, `TemplateV2Validator.validate` + `TemplateV2Runner.run` on H2**

Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -Dtest=V2ScenarioTemplateIT test`

- [ ] **Step 3: Document paths in scenario catalog**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(template): add greenfield v2 scenario template library and tests"
```

---

## Task 8: Console run report + migration tab de-emphasize

**Files:**
- Modify: `data-generator-console-web/src/app/pages/JobDetailPage.tsx`
- Modify: `data-generator-console-web/src/api/jobs.ts`, `api/types.ts`
- Modify: `data-generator-console-web/src/app/layout/ConsoleLayout.tsx`
- Modify: `data-generator-console-web/src/i18n/locales/en.json`, `zh-CN.json`

- [ ] **Step 1: Extend API types with `report?: RunReport`**

- [ ] **Step 2: Job detail — Ant Design `Descriptions` + per-stage table for sources/transformers/sinks**

- [ ] **Step 3: Hide Migration nav behind `import.meta.env.VITE_ENABLE_MIGRATION === 'true'` (default false in prod build)**

- [ ] **Step 4: `npm run build` in `data-generator-console-web`**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(console-web): show run report on job detail and hide migration by default"
```

---

## Task 9: Cleanup + Phase A doc sweep

**Files:**
- Delete: `data-generator-service/src/main/bundles/dev.bundle`
- Modify: `docs/migration/orchestration-retirement-boundary.md` (banner: historical pre-2026-05-29)
- Modify: `docs/calcite-implementation-status.md` (STREAMING done, run reports)

- [ ] **Step 1: Remove `dev.bundle`; verify not referenced in `pom.xml`**

- [ ] **Step 2: Full test**

Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -am test`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git commit -m "chore: remove vaadin dev.bundle and update v2-only status docs"
```

**Phase A checkpoint:** STREAMING works; guards + sink retry; run reports in API/UI; scenario library green.

---

# Phase A′ — Workflow, transform DAG, JS, selectors

## Task 10: Workflow and compute block models

**Files:**
- Create: `data-generator-common/.../workflow/WorkflowSpecVO.java`
- Create: `.../workflow/PauseStepVO.java`, `LogStepVO.java`, `BranchStepVO.java`, `SharedScopeStepVO.java`, `InvokeComputeBlockStepVO.java`
- Create: `.../ComputeBlockVO.java`, `.../TransformGraphVO.java`, `TransformNodeVO.java`, `TransformEdgeVO.java`
- Modify: `data-generator-common/.../TemplateV2VO.java` — `WorkflowSpecVO workflow`; deprecate nothing yet
- Modify: YAML/JSON codec registration for subtypes
- Test: `data-generator-common/src/test/java/.../WorkflowSpecVOSerializationTests.java` (or service-level)

- [ ] **Step 1: Failing serde test — round-trip YAML with one `PauseStepVO` + one `InvokeComputeBlockStepVO`**

- [ ] **Step 2: Add Jackson `@JsonSubTypes` on step base `WorkflowStepVO`**

- [ ] **Step 3: `ComputeBlockVO` fields: `id`, `sources`, `transformGraph`, `transformers` (linear fallback), `sinks`, `sharedScopeId`**

- [ ] **Step 4: Pass serde test**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(core): add workflow and compute block v2 model types"
```

---

## Task 11: MaterializationPolicyVO

**Files:**
- Create: `data-generator-common/.../MaterializationPolicyVO.java`
- Modify: `SourceVO` or per-source field `materializationPolicy`
- Modify: `data-generator-calcite/.../source/*RowSource` materialization paths
- Test: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/source/MaterializationPolicyTests.java`

- [ ] **Step 1: Policy enum: `EQUAL`, `WEIGHTED`, `ONCE`, `ORDERED`, `LIMIT` with documented semantics (new, not V1)**

- [ ] **Step 2: Failing tests — WEIGHTED produces expected distribution on fixed seed**

- [ ] **Step 3: Implement in iterator/query materialization layer**

- [ ] **Step 4: Validator rejects unknown mode + negative weights**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(v2): add materialization policy for source selection"
```

---

## Task 12: Transform DAG executor (L1)

**Files:**
- Create: `data-generator-calcite/.../TransformDagExecutor.java`
- Modify: `data-generator-calcite/.../ComputeBlockRunner.java`
- Modify: `data-generator-service/.../TemplateV2Validator.java`
- Test: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/TransformDagExecutorTests.java`

- [ ] **Step 1: Failing test — two SQL nodes merged by edge `n1.out -> n2.in`**

- [ ] **Step 2: Topological sort + cycle detection (throw `ValidationException` with cycle path)**

- [ ] **Step 3: Execute nodes; pass row table alias `input` between nodes per `TransformNodeVO.outputAlias`**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(calcite): execute transform dag inside compute block"
```

---

## Task 13: JsTransformVO + GraalJS factory

**Files:**
- Create: `data-generator-common/.../JsTransformVO.java`
- Create: `data-generator-calcite/.../transform/JsTransformFactory.java`
- Modify: `data-generator-calcite/.../CoreConfig` or registry factory list
- Reuse: `data-generator-scripter-javascript/.../JsScript.java` patterns
- Test: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/transform/JsTransformFactoryTests.java`

- [ ] **Step 1: Failing test — script `row.amount = row.amount * 2` doubles column**

- [ ] **Step 2: Sandbox: no `java.*`, timeout 5s default, max 64KB source, bind `row` map only**

- [ ] **Step 3: Register in `V2TransformFactory` SPI list**

- [ ] **Step 4: Validator rejects scripts > 64KB**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(calcite): add sandboxed js transform factory"
```

---

## Task 14: WorkflowRunner (L2)

**Files:**
- Create: `data-generator-calcite/.../WorkflowRunner.java`
- Create: `data-generator-calcite/.../WorkflowExecutionState.java` (pause checkpoint)
- Modify: `data-generator-calcite/.../TemplateV2Runner.java`
- Test: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/runtime/WorkflowRunnerTests.java`

- [ ] **Step 1: Failing test — steps: LOG → Pause 50ms → InvokeComputeBlock → LOG; assert ordering via mock collector**

- [ ] **Step 2: `PauseStepVO` — `Duration.parse(iso)` sleep on worker thread**

- [ ] **Step 3: `LogStepVO` — append to `RunReportCollector` diagnostic list**

- [ ] **Step 4: `BranchStepVO` — evaluate SpEL condition; run child step ids**

- [ ] **Step 5: `SharedScopeStepVO` — `ConcurrentHashMap` per workflow run id in `WorkflowExecutionState`**

- [ ] **Step 6: `TemplateV2Runner` — if `template.getWorkflow() != null`, delegate to `WorkflowRunner`**

- [ ] **Step 7: Commit**

```bash
git commit -m "feat(calcite): add workflow runner with pause log branch shared steps"
```

---

## Task 15: TemplateV2Validator extensions

**Files:**
- Modify: `data-generator-service/.../TemplateV2Validator.java`
- Test: `data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2WorkflowValidatorTests.java`

- [ ] **Step 1: Tests — reject DAG cycle; reject workflow + legacy linear transformers both set without blocks; reject JS without script body**

- [ ] **Step 2: Implement validation messages with JSON path (`workflow.steps[1].type`)**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(service): validate workflow compute blocks and transform dag"
```

---

## Task 16: Greenfield workflow scenario templates + IT

**Files:**
- Create: `template/v2-scenarios/scenario-wf-pause-log.yaml`
- Create: `template/v2-scenarios/scenario-wf-branch.yaml`
- Create: `template/v2-scenarios/scenario-dag-join.yaml`
- Create: `template/v2-scenarios/scenario-js-transform.yaml`
- Test: `V2WorkflowScenarioIT.java`

- [ ] **Step 1: Each template runs in IT with assertions on metrics / log steps**

- [ ] **Step 2: Add `docs/template-v2-workflow-authoring-guide.md`**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(template): add workflow dag and js scenario templates"
```

---

## Task 17: Console minimal workflow editor

**Files:**
- Create: `data-generator-console-web/src/app/editor/WorkflowPanel.tsx`
- Modify: `TemplateEditorPage.tsx`, `api/editor.ts`, i18n
- Modify: `ConsoleTemplateEditorController` if new fields needed

- [ ] **Step 1: Tab "Workflow" — editable table: step type, params JSON, compute block id**

- [ ] **Step 2: Compute blocks tab — reuse Sources/Sinks steps scoped per block id**

- [ ] **Step 3: Transform DAG — list nodes + "depends on" multi-select (no visual graph v1)**

- [ ] **Step 4: Build + manual smoke `/console/templates/{id}?tab=workflow`**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(console-web): minimal workflow and transform dag editor"
```

---

## Task 18: Policy docs + retire S1 for greenfield

**Files:**
- Modify: `docs/migration/orchestration-retirement-boundary.md`
- Modify: `docs/template-v2-product-roadmap.md`
- Modify: `docs/calcite-v1-parity-scorecard.md` (header note)

- [ ] **Step 1: Add historical banner; point to workflow authoring guide for PAUSE/LOG**

- [ ] **Step 2: Mark parity scorecard as reference-only**

- [ ] **Step 3: Commit**

```bash
git commit -m "docs: align migration docs with v2-only workflow policy"
```

**Phase A′ checkpoint:** New templates (not V1) run workflow, DAG, JS, policies; CI green.

---

# Milestone appendix (B, C, D)

Separate implementation plans when Phase A′ merges. Entry criteria and scope only.

## Phase B — Operable platform

**Entry:** Phase A′ checkpoint green.

| Epic | Key deliverables | Primary modules |
|------|------------------|-----------------|
| B1 Secrets | `secretRef` on datasource + template; resolver in `DataSourceConfigService` | service, core |
| B2 Publish flow | `TemplatePO.status`, publish API, validate gate | service, console |
| B3 RBAC | Spring Security or custom filter; roles on `/api/**` | service, console |
| B4 Task lifecycle | Cancel flag on run; retry creates new instance; workflow `PAUSED` resume API | service, calcite |
| B5 Audit | `audit_event` table + append on template/datasource/run | service |

**Plan file (future):** `docs/superpowers/plans/2026-05-XX-v2-phase-b-operable-platform.md`

## Phase C — Distributed execution

**Entry:** Phase B stable RBAC + run reports.

| Epic | Key deliverables |
|------|------------------|
| C1 In-process | `PartitionedComputeBlockRunner`; `ExecutionPolicyVO.partitionCount`; fork-join pool |
| C2 Multi-node | `coordinator` module or package; job queue table; worker heartbeat; lease |

**Plan file (future):** `docs/superpowers/plans/2026-05-XX-v2-phase-c-distributed.md`

## Phase D — Template pipeline DAG (far-term)

**Entry:** C2 coordinator can schedule multiple template runs.

| Epic | Key deliverables |
|------|------------------|
| D1 Model | `PipelineSpecVO`, `PipelineRunPO` |
| D2 Artifact store | H2/table or S3-compatible; TTL job |
| D3 Source | `TemplateRunSourceVO` reads artifact |
| D4 UI | Pipeline designer |

**Reservations already in Task 1 (Phase A).**

---

## Plan self-review

### Spec coverage

| Spec section | Plan tasks |
|--------------|------------|
| Phase A scale | Tasks 2, 3 |
| Phase A sinks | Task 4 |
| Phase A control plane | Tasks 5, 6 |
| Phase A product | Tasks 7, 8 |
| Phase A cleanup | Task 9 |
| Phase D reservations | Task 1 |
| Phase A′ L1 DAG | Tasks 10, 12 |
| Phase A′ L2 workflow | Tasks 10, 14 |
| Phase A′ JS | Task 13 |
| Phase A′ selector | Task 11 |
| Phase A′ console | Task 17 |
| Phase A′ policy/docs | Task 18 |
| Phase B/C/D | Appendix only (intentional) |

### Placeholder scan

No TBD steps in Tasks 1–18. Appendix points to future dated plan files (acceptable).

### Type consistency

- `WorkflowSpecVO` on `TemplateV2VO.workflow` used consistently Tasks 10–17.
- `RunReportVO` introduced Task 5, consumed Task 8.
- `TransformGraphVO` used Tasks 10, 12, 17.

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-29-v2-only-full-capability.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — fresh subagent per task (Task 1, Task 2, …), review between tasks  
2. **Inline Execution** — implement in this session with checkpoints after Task 9 (Phase A) and Task 18 (Phase A′)

**Which approach?**
