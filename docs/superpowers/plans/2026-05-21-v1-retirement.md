# V1 Retirement Program Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retire V1 template execution on a wave-freeze schedule by closing P1–P4 gates: SpEL non-SQL transformer, control-plane validate/explain/preview, staging dual-run evidence, and a config flag to disable V1 runtime.

**Architecture:** Three engineering workstreams (WS1 SpEL transform, WS2 control plane REST, WS3 migration evidence) converge at R0/R3 staging; V1 shutdown uses `data.generator.v1-execution.enabled` before any code deletion. Spec: `docs/superpowers/specs/2026-05-21-v1-retirement-alignment-design.md`.

**Tech Stack:** Java 25, Maven, Spring Boot 4, Calcite V2 runner, Spring SpEL (`SpelExpressionParser`), existing migration workbench REST, H2 phase7-test profile.

---

## File map (planned)

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `DataGeneratorProperties.java` | `v1ExecutionEnabled`, optional `previewMaxRows` |
| Modify | `TaskController.java` | Gate `runV1` / V1 detection path when flag false |
| Create | `SpelTransformVO.java` | non-SQL transform model (`type: spel`) |
| Create | `SpelColumnMapping.java` (or inner record) | `name` + `expression` per output column |
| Create | `SpelTransformFactory.java` | `V2TransformFactory` row-local SpEL |
| Modify | `DefaultTemplateV2RuntimePlugin.java` | Register `SpelTransformFactory` |
| Modify | `data-generator-calcite/pom.xml` | `spring-expression` or `data-generator-scripter-spel` |
| Modify | `TemplateV2Validator.java` | Validate `SpelTransformVO` fields |
| Create | `TemplateV2ControlPlaneService.java` | validate / explain / preview orchestration |
| Create | `TemplateV2ExplainDTO.java`, `TemplateV2PreviewDTO.java` | API response shapes |
| Modify | `TemplateController.java` | `POST validateV2`, `GET explainV2`, `POST previewV2` |
| Modify | `MigrationPromoteService.java` | Reject COMPATIBILITY_ONLY / BLOCKED |
| Modify | `V1TemplateMigrationAnalyzer.java` | `recommendedPath: spel` for plain SCRIPT fields |
| Create | `docs/migration/wave-freeze-schedule.md` | R0 output: proposed W1/W2 dates |
| Modify | `docs/migration/retirement-readiness.md` | Checkboxes as evidence lands |
| Test | `SpelTransformFactoryTests.java`, `TaskControllerV1ExecutionFlagTests.java`, `TemplateControllerControlPlaneTests.java` | Regression |

---

## Phase R0 — Staging evidence (no code required first)

**Objective:** Produce P3 starter evidence and a wave-freeze date proposal before W1/W2 engineering freeze.

### Task R0.1: Run single-template staging workflow

**Files:**
- Read: `docs/migration/staging-runbook.md`
- Run: `scripts/migration-staging.ps1`

- [ ] **Step 1:** Start `data-generator-service` against staging DB (or local H2 with imported template ids).

- [ ] **Step 2:** Pick **one synthetic** template id `{synId}` and **one JDBC export** template id `{jdbcId}` from staging catalog.

- [ ] **Step 3:** Run workflow for each:

```powershell
.\scripts\migration-staging.ps1 -BaseUrl "http://localhost:9876/template" -TemplateId {synId} -Action workflow
.\scripts\migration-staging.ps1 -BaseUrl "http://localhost:9876/template" -TemplateId {jdbcId} -Action workflow
```

- [ ] **Step 4:** Verify compare reports exist under `docs/migration/reports/` and inventory rows show `lastCompareReportPath`.

- [ ] **Step 5:** Record classifications in a scratch table (EXACT / ADAPTED / BLOCKED / COMPATIBILITY_ONLY). Do **not** promote BLOCKED or COMPATIBILITY_ONLY.

**Expected:** Two markdown reports; at least one classification in {EXACT, ADAPTED}.

### Task R0.2: Write wave-freeze schedule proposal

**Files:**
- Create: `docs/migration/wave-freeze-schedule.md`

- [ ] **Step 1:** Create `docs/migration/wave-freeze-schedule.md` with sections: R0 evidence summary, proposed W1 freeze date (synthetic), proposed W2 freeze date (multi_source/JDBC), owners, rollback note.

- [ ] **Step 2:** Link template ids and report paths from R0.1.

- [ ] **Step 3:** Commit:

```bash
git add docs/migration/wave-freeze-schedule.md docs/migration/reports/
git commit -m "docs(migration): add wave-freeze schedule after R0 staging"
```

---

## Phase R1 — Control plane MVP (WS2)

### Task R1.1: TemplateV2ControlPlaneService — validate

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/template/TemplateV2ControlPlaneService.java`
- Test: `data-generator-service/src/test/java/org/gensokyo/data/template/TemplateV2ControlPlaneServiceTests.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void validateRejectsBlankSqlTransform() {
    TemplateV2DraftVO draft = minimalDraft();
    ((SqlTransformVO) draft.getTransform()).setSql("");
    TemplateV2ControlPlaneService svc = new TemplateV2ControlPlaneService(...);
    TemplateV2ValidationResult result = svc.validate(draft);
    assertFalse(result.isValid());
    assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("SQL")));
}
```

- [ ] **Step 2:** Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -Dtest=TemplateV2ControlPlaneServiceTests -Dsurefire.failIfNoSpecifiedTests=false test`  
  **Expected:** FAIL (class missing)

- [ ] **Step 3:** Implement `validate(TemplateV2DraftVO draft)` — normalize → `TemplateV2Validator.validate` → catch `IllegalArgumentException` → errors list; add execution-shape warnings via `ExecutionShapeClassifier` when CHUNKED.

- [ ] **Step 4:** Run test — **Expected:** PASS

### Task R1.2: Control plane — explain

**Files:**
- Modify: `TemplateV2ControlPlaneService.java`
- Reuse: `MigrationPlanExplainService`, `MigrationPlanExplainServiceTests` patterns

- [ ] **Step 1:** Add `explain(Long templateId)` — load `TemplatePO`, detect V1/V2, for V2 build `MigrationPlanExplain` (sources, SQL snippet, execution mode, diff notes).

- [ ] **Step 2:** Test: load fixture template from `TemplateRepository` seed in `@DataJpaTest` or `@SpringBootTest` phase7 profile.

- [ ] **Step 3:** Run tests — PASS

### Task R1.3: Control plane — bounded preview

**Files:**
- Modify: `TemplateV2ControlPlaneService.java`
- Modify: `DataGeneratorProperties.java` — `previewMaxRows = 100`

- [ ] **Step 1:** Add `preview(Long templateId, Integer maxRows)` — cap rows via `ExecutionPolicyVO.maxRowsInMemory` override or truncate source rows after `TemplateV2Runner.run` (IN_MEMORY only for MVP).

- [ ] **Step 2:** Return `TemplateV2PreviewDTO` with `schema`, `rows` (≤ maxRows), `warnings`.

- [ ] **Step 3:** Test with `CapturingTemplateV2Runner` or real small iterator template — PASS

### Task R1.4: REST endpoints + controller tests

**Files:**
- Modify: `TemplateController.java`
- Create: `TemplateControllerControlPlaneTests.java`

- [ ] **Step 1:** Add endpoints:

| Method | Path | Body |
|--------|------|------|
| POST | `/template/v2/validate` | YAML string or template id query param |
| GET | `/template/v2/explain/{templateId}` | — |
| POST | `/template/v2/preview/{templateId}` | optional `{ "maxRows": 50 }` |

- [ ] **Step 2:** `@SpringBootTest` + phase7-test — assert `R.ok` and non-empty explain for seeded number iterator.

- [ ] **Step 3:** Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -am test "-Dtest=TemplateControllerControlPlaneTests" "-Dsurefire.failIfNoSpecifiedTests=false"`

- [ ] **Step 4:** Commit: `feat(service): add template v2 validate explain preview control plane`

---

## Phase R2 — SpelTransformVO (WS1)

### Task R2.1: Model + codec registration

**Files:**
- Create: `data-generator-common/.../SpelTransformVO.java`
- Create: `data-generator-common/.../SpelColumnMapping.java`

```java
@Getter @Setter
@AutoService(TransformVO.class)
@JsonSubType("SPEL")
public class SpelTransformVO extends TransformVO {
    public SpelTransformVO() { setType("spel"); }
    /** Output columns to add or replace; evaluated per row. */
    private List<SpelColumnMapping> columns = new ArrayList<>();
}

@Data
public class SpelColumnMapping implements Serializable {
    private String name;
    /** SpEL expression, e.g. "#row['id'] + '-x'" */
    private String expression;
}
```

- [ ] **Step 1:** Add classes with copyright + Javadoc per repo rules.

- [ ] **Step 2:** Verify subtype appears in codec tests (`TemplateV2SupportTests` or JSON round-trip).

- [ ] **Step 3:** Commit: `feat(core): add SpelTransformVO for row-level spel transforms`

### Task R2.2: SpelTransformFactory + unit tests

**Files:**
- Create: `data-generator-calcite/.../sql/SpelTransformFactory.java`
- Create: `data-generator-calcite/.../sql/SpelTransformFactoryTests.java`
- Modify: `DefaultTemplateV2RuntimePlugin.java`

- [ ] **Step 1: Failing test**

```java
@Test
void addsComputedColumnFromSpel() {
    RowSchema schema = RowSchema.of(ColumnDef.string("id"));
    List<Row> rows = List.of(new Row(Map.of("id", "a")));
    CalciteExecutionContext ctx = new CalciteExecutionContext()
        .addTable("input", schema, rows);
    SpelTransformVO t = new SpelTransformVO();
    t.setColumns(List.of(mapping("label", "#row['id'] + '-1'")));
    SpelTransformFactory factory = new SpelTransformFactory();
    var result = factory.apply(t, ctx);
    assertEquals("a-1", result.rows().get(0).values().get("label"));
}
```

- [ ] **Step 2:** Implement `supports(SpelTransformVO)` and `apply` — read table `input`, loop rows, `StandardEvaluationContext` with `#row` variable, merge columns into new `Row`.

- [ ] **Step 3:** Register in `DefaultTemplateV2RuntimePlugin.transformFactories()` alongside `SqlTransformFactory`.

- [ ] **Step 4:** `TemplateV2Validator` — require non-empty `columns`, each `name` and `expression` non-blank.

- [ ] **Step 5:** Run calcite tests — PASS

- [ ] **Step 6:** Commit: `feat(calcite): add SpelTransformFactory for row-level spel`

### Task R2.3: Runner integration test (SQL → SpEL chain)

**Files:**
- Create: `data-generator-calcite/.../TemplateV2RunnerSpelTransformTests.java`

- [ ] **Step 1:** Template: iterator source → SQL `SELECT id FROM input` → SpEL add label column.

- [ ] **Step 2:** Assert runner output row count and computed field.

- [ ] **Step 3:** Commit: `test(calcite): cover sql then spel transform chain`

### Task R2.4: Migration analyzer + draft hints

**Files:**
- Modify: `V1TemplateMigrationAnalyzer.java`
- Modify: `docs/template-v2-transformer-strategy.md` — document SpEL as first non-SQL family

- [ ] **Step 1:** When V1 field stage is `SCRIPT` + `plain` language → set `recommendedPath` to `spel` (not `compatibility_only` unless JS/PAUSE).

- [ ] **Step 2:** Unit test in `V1TemplateMigrationAnalyzerTests` with fixture YAML.

- [ ] **Step 3:** Commit: `feat(migration): recommend spel path for plain script fields`

---

## Phase R3 — Promote guards + P3 evidence (WS3)

### Task R3.1: Block promote for COMPATIBILITY_ONLY / BLOCKED

**Files:**
- Modify: `MigrationPromoteService.java`
- Test: `MigrationPromoteServiceTests.java`

- [ ] **Step 1: Failing test** — inventory entry `COMPATIBILITY_ONLY` → promote throws with clear message.

- [ ] **Step 2:** Before persist, `migrationInventoryService.findById("db-" + templateId)` — if class is `COMPATIBILITY_ONLY` or `BLOCKED`, throw `IllegalArgumentException`.

- [ ] **Step 3:** Controller test `TemplateControllerMigrationDraftTests` — promote returns `R.fail` for compat template.

- [ ] **Step 4:** Commit: `fix(migration): reject promote for blocked and compatibility-only classes`

### Task R3.2: Complete W1/W2 staging cohort

**Files:**
- Modify: `docs/migration/retirement-readiness.md`

- [ ] **Step 1:** Run batch compare (cap 50): `.\scripts\migration-staging.ps1 -Action batch-compare`

- [ ] **Step 2:** Promote all staging templates with EXACT/ADAPTED + sign-off API.

- [ ] **Step 3:** `POST /migration/inventory/{id}/signoff` for `synthetic` and `multi_source` families.

- [ ] **Step 4:** Check P3 boxes in `retirement-readiness.md`.

- [ ] **Step 5:** Commit evidence updates.

---

## Phase R4 — V1 execution config flag (P4)

### Task R4.1: Property + TaskController gate

**Files:**
- Modify: `DataGeneratorProperties.java`
- Modify: `TaskController.java`
- Test: `TaskControllerV1ExecutionFlagTests.java`

```java
// DataGeneratorProperties
/** When false, TaskController refuses to run V1 templates (P4 retirement). */
private boolean v1ExecutionEnabled = true;
```

- [ ] **Step 1: Failing test** — set `data.generator.v1-execution.enabled=false`, `runById` on V1-only template → `R.fail` message contains `V1 execution is disabled`.

- [ ] **Step 2:** In `TaskController.run(TemplatePO)` after detect kind V1, if `!properties.isV1ExecutionEnabled()` return fail (do not submit executor).

- [ ] **Step 3:** Document in `docs/migration/retirement-readiness.md` and `application.yaml` example.

- [ ] **Step 4:** Add `application-phase7-test.yaml` entry `v1-execution.enabled: true` so tests keep working.

- [ ] **Step 5:** Commit: `feat(service): gate v1 task execution behind config flag`

### Task R4.2: Production cutover checklist

**Files:**
- Modify: `docs/migration/retirement-readiness.md`
- Modify: `docs/migration/wave-freeze-schedule.md`

- [ ] **Step 1:** Verify P4 metrics from spec (≥90% promoted, family sign-off complete).

- [ ] **Step 2:** Set staging/prod `data.generator.v1-execution.enabled=false`.

- [ ] **Step 3:** Announce compatibility-only templates still on V1 via dedicated runbook entry.

- [ ] **Step 4:** Final commit: `docs(migration): record v1 execution disable cutover`

---

## Verification commands (full slice)

```powershell
.\mvnw-jdk25.ps1 -pl "data-generator-calcite,data-generator-service" -am test
.\mvnw-jdk25.ps1 -pl data-generator-service -am test "-Dtest=*ControlPlane*,*Spel*,*Promote*,*V1Execution*" "-Dsurefire.failIfNoSpecifiedTests=false"
```

---

## Spec coverage checklist

| Spec requirement | Plan task |
|------------------|-----------|
| Config-flag V1 disable | R4.1 |
| SpelTransformVO row SpEL | R2.1–R2.3 |
| Control plane validate/explain/preview | R1.1–R1.4 |
| Staging W1/W2 evidence | R0, R3.2 |
| Promote guard COMPATIBILITY_ONLY | R3.1 |
| Wave dates after R0 | R0.2 |
| Geo out of scope | (no tasks) |

## Revision history

| Date | Change |
|------|--------|
| 2026-05-21 | Initial plan from approved v1-retirement-alignment design |
