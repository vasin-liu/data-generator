# V1 Migration and Dual-Run Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a migration workbench in `data-generator-service` with scenario inventory (DB + repo), dual-run comparison (V1 vs V2), and promote workflow—producing auditable evidence for V1 retirement gates.

**Architecture:** Inventory in committed YAML; new `org.gensokyo.data.template.migration` package with analyzer, compare, and promote services; REST on `TemplateController`; compare runs V1 task path and `TemplateV2Runner` then classifies results. Reports written under `docs/migration/reports/`.

**Tech Stack:** Java 25, Spring Boot, Maven, JUnit 5, H2, existing `TemplateV2Runner` / `TaskController` execution paths, Jackson YAML.

**Spec:** `docs/superpowers/specs/2026-05-19-v1-migration-dual-run-design.md`

---

## File map

| File | Responsibility |
|------|----------------|
| `docs/migration/scenario-inventory.yaml` | Canonical inventory (A3) |
| `docs/migration/reports/.gitkeep` | Report output directory |
| `data-generator-service/.../migration/MigrationInventoryEntry.java` | Inventory record model |
| `data-generator-service/.../migration/MigrationInventoryService.java` | Load/save YAML, merge DB export |
| `data-generator-service/.../migration/MigrationClassification.java` | enum: exact, adapted, approximate, compatibility_only, blocked, unclassified |
| `data-generator-service/.../migration/TemplateMigrationAnalysisDTO.java` | Full-template analysis response |
| `data-generator-service/.../migration/V1TemplateMigrationAnalyzer.java` | Scenario family + blockers |
| `data-generator-service/.../migration/MigrationComparisonReport.java` | Compare result DTO |
| `data-generator-service/.../migration/MigrationCompareService.java` | Dual-run orchestration |
| `data-generator-service/.../migration/MigrationCompareOptions.java` | sampleSize, keyColumns, preferChunked |
| `data-generator-service/.../migration/MigrationClassificationRules.java` | Pure classification from metrics |
| `data-generator-service/.../migration/MigrationPromoteService.java` | Promote draft after review |
| `data-generator-service/.../migration/MigrationReportWriter.java` | Markdown report to `docs/migration/reports/` |
| `data-generator-service/.../controller/TemplateController.java` | New migration endpoints |
| `data-generator-service/.../migration/MigrationInventoryExportCommand.java` | Optional CLI or `@Component` for DB export |
| `data-generator-service/src/test/resources/migration/regression/*.yaml` | Repo regression V1 fixtures |
| `data-generator-service/src/test/java/.../migration/*Tests.java` | Unit + integration tests |

All new `.java` files: copyright block, class Javadoc, public API Javadoc per `.cursor/rules/java-copyright-class-javadoc.mdc`.

**Build (repeat):**

```powershell
cd D:\Work\99_Code\data-generator
.\mvnw-jdk25.ps1 -pl data-generator-service -am test "-Dtest=MigrationCompareServiceTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

---

## Phase M0 — Scenario inventory

### Task 1: Inventory schema and empty inventory file

**Files:**
- Create: `docs/migration/scenario-inventory.yaml`
- Create: `docs/migration/reports/.gitkeep`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/template/migration/MigrationInventoryEntry.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/template/migration/MigrationClassification.java`

- [ ] **Step 1: Write failing test**

Create `data-generator-service/src/test/java/org/gensokyo/data/template/migration/MigrationInventoryServiceTests.java`:

```java
@Test
void loadsEmptyInventory() throws Exception {
    Path path = Files.createTempFile("inventory", ".yaml");
    Files.writeString(path, "templates: []\n");
    MigrationInventoryService service = new MigrationInventoryService(path);
  Assertions.assertEquals(0, service.listAll().size());
}
```

- [ ] **Step 2: Run — expect FAIL** (class not found)

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am test "-Dtest=MigrationInventoryServiceTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- [ ] **Step 3: Implement**

`MigrationClassification` enum values: `UNCLASSIFIED`, `EXACT`, `ADAPTED`, `APPROXIMATE`, `COMPATIBILITY_ONLY`, `BLOCKED`.

`MigrationInventoryEntry` fields: `id`, `name`, `origin` (`database`|`repository`), `scenarioFamily`, `migrationClass`, `wave`, `blockers` (List), `dbTemplateId` (Long nullable), `v2DraftPresent`, `lastCompareReportPath`, `notes`.

`MigrationInventoryService`: load/save YAML with Jackson; path from constructor defaulting to repo-relative `docs/migration/scenario-inventory.yaml` when run from module root use `Paths.get("docs/migration/scenario-inventory.yaml")` with fallback from user.dir.

Commit root `docs/migration/scenario-inventory.yaml`:

```yaml
version: 1
templates: []
```

- [ ] **Step 4: Run — PASS**

- [ ] **Step 5: Commit**

```bash
git add docs/migration/ data-generator-service/src/main/java/org/gensokyo/data/template/migration/ data-generator-service/src/test/java/org/gensokyo/data/template/migration/MigrationInventoryServiceTests.java
git commit -m "feat(service): add migration scenario inventory model"
```

---

### Task 2: Seed regression templates + DB export hook

**Files:**
- Create: `data-generator-service/src/test/resources/migration/regression/v1-iterator-simple.yaml`
- Create: `data-generator-service/src/test/resources/migration/regression/v1-query-lookup.yaml`
- Modify: `docs/migration/scenario-inventory.yaml`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/template/migration/MigrationInventorySeeder.java`

- [ ] **Step 1: Add regression YAML fixtures** (minimal valid V1 shapes copied from `TemplateControllerQuerySourceMigrationTests` patterns)

- [ ] **Step 2: Write test `seedsRegressionEntries`**

```java
@Test
void seedsRegressionEntriesFromClasspath() {
    MigrationInventorySeeder seeder = new MigrationInventorySeeder();
    List<MigrationInventoryEntry> entries = seeder.regressionEntriesFromClasspath();
    Assertions.assertTrue(entries.stream().anyMatch(e -> "repository".equals(e.getOrigin())));
}
```

- [ ] **Step 3: Implement `MigrationInventorySeeder`**

- Scan `classpath:migration/regression/*.yaml`
- Assign ids `regression-{filename}`
- `scenarioFamily` heuristic: query in yaml → `multi_source`, iterator → `synthetic`

- [ ] **Step 4: Implement `mergeFromDatabase(TemplateRepository repo)`**

- For each `TemplatePO` where content parses as V1 (`TemplateDefinitionDetector`):
  - Add entry `id: db-{po.id}`, `origin: database`, `migrationClass: UNCLASSIFIED`
- Skip if id already in inventory

- [ ] **Step 5: Add REST or test-only bootstrap**

`MigrationInventoryService.refreshFromRepository(TemplateRepository)` called from test `MigrationInventoryBootstrapTests` to prove DB merge; optional `POST /migration/inventory/refresh` behind profile `migration-admin` (YAGNI: test-only in M0 is OK).

- [ ] **Step 6: Update `scenario-inventory.yaml` with ≥2 regression rows manually or via test write to temp**

- [ ] **Step 7: Commit** — `feat(service): seed migration inventory from repo and db`

---

## Phase M1 — Analysis and compare core

### Task 3: Full-template migration analyzer

**Files:**
- Create: `TemplateMigrationAnalysisDTO.java`
- Create: `V1TemplateMigrationAnalyzer.java`
- Modify: `TemplateController.java` — add `GET /migration/analyze/{templateId}`

- [ ] **Step 1: Failing test** in `V1TemplateMigrationAnalyzerTests.java`:

```java
@Test
void flagsCompatibilityOnlyWhenPauseStagePresent() {
    TemplateVO v1 = loadFixture("migration/regression/v1-with-pause.yaml");
    TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);
    Assertions.assertEquals(MigrationClassification.COMPATIBILITY_ONLY, analysis.getSuggestedClass());
    Assertions.assertTrue(analysis.getBlockers().stream().anyMatch(b -> b.contains("pause")));
}
```

(Create minimal `v1-with-pause.yaml` fixture with PAUSE stage or stub analyzer rule for `LogStage` / pause types from V1 model.)

- [ ] **Step 2: Implement analyzer**

Rules (explicit):

| Signal | Result |
|--------|--------|
| JavaScript script stage | `COMPATIBILITY_ONLY` |
| PAUSE / SHARED / LOG orchestration | `COMPATIBILITY_ONLY` |
| Only iterator + simple fields | Wave 1, path `sql` |
| JDBC readers + field DAG | Wave 2, path `sql` or `sql_udf` |
| Multi-reader EQUAL/WEIGHT | `APPROXIMATE` + warning reuse `V1QuerySourceMigrationWarningAnalyzer` |

Delegate query warnings: `V1QuerySourceMigrationWarningAnalyzer.analyze(v1)`.

- [ ] **Step 3: Wire controller endpoint** returning `R<TemplateMigrationAnalysisDTO>`

- [ ] **Step 4: Run tests PASS**

- [ ] **Step 5: Commit** — `feat(service): add full-template migration analyzer`

---

### Task 4: Classification rules (pure)

**Files:**
- Create: `MigrationClassificationRules.java`
- Create: `MigrationClassificationRulesTests.java`

- [ ] **Step 1: Tests**

```java
@Test
void exactWhenCountsMatchAndSampleRateHigh() {
    MigrationClassification c = MigrationClassificationRules.classify(
            1000, 1000, 0.999, List.of());
    Assertions.assertEquals(MigrationClassification.EXACT, c);
}

@Test
void blockedWhenSampleRateLow() {
    MigrationClassification c = MigrationClassificationRules.classify(
            1000, 1000, 0.90, List.of());
    Assertions.assertEquals(MigrationClassification.BLOCKED, c);
}

@Test
void approximateWhenWarningsNonEmptyAndRateHigh() {
    MigrationClassification c = MigrationClassificationRules.classify(
            1000, 1000, 0.99, List.of("SourcePolicyVO approximation"));
    Assertions.assertEquals(MigrationClassification.APPROXIMATE, c);
}
```

Threshold constants: `SAMPLE_MATCH_EXACT = 0.999`, `SAMPLE_MATCH_BLOCKED = 0.95`.

- [ ] **Step 2: Implement**

- [ ] **Step 3: Commit** — `feat(service): add migration comparison classification rules`

---

### Task 5: MigrationComparisonReport + row sampler

**Files:**
- Create: `MigrationComparisonReport.java`
- Create: `RowSampleComparator.java`
- Create: `RowSampleComparatorTests.java`

- [ ] **Step 1: `RowSampleComparator`**

Compare two `List<Map<String,Object>>` samples on `keyColumns` (default: all keys in intersection); return match rate 0..1.

- [ ] **Step 2: `MigrationComparisonReport`**

Fields: `templateId`, `v1RowCount`, `v2RowCount`, `sampleSize`, `sampleMatchRate`, `classification`, `warnings`, `recommendation` (`accept` | `accept_with_review` | `reject`), `reportPath`.

- [ ] **Step 3: Unit tests with synthetic rows**

- [ ] **Step 4: Commit** — `feat(service): add migration comparison report model`

---

### Task 6: MigrationCompareService (fixture-based integration)

**Files:**
- Create: `MigrationCompareService.java`
- Create: `MigrationCompareOptions.java`
- Create: `MigrationCompareServiceTests.java`
- Create: `data-generator-service/src/test/resources/migration/regression/v1-constant-five-rows.yaml`

- [ ] **Step 1: Design compare for testability**

Extract package-private interface:

```java
interface TemplateRunExecutor {
    RunOutcome runV1(TemplateVO v1, Map<String, Object> params);
    RunOutcome runV2(TemplateV2VO v2, Map<String, Object> params);
}
record RunOutcome(long rowCount, List<Map<String, Object>> sample) {}
```

Production impl delegates to existing task/V2 runner; tests use stub returning fixed rows.

- [ ] **Step 2: Failing test**

```java
@Test
void classifiesExactWhenStubRunsMatch() {
    MigrationCompareService service = new MigrationCompareService(stubExecutorMatching());
    MigrationComparisonReport report = service.compare(v1Template, v2Template, MigrationCompareOptions.defaults());
    Assertions.assertEquals(MigrationClassification.EXACT, report.getClassification());
}
```

- [ ] **Step 3: Implement `MigrationCompareService.compare`**

1. Collect warnings from `V1TemplateMigrationAnalyzer` + existing query analyzer
2. Run V1 and V2 via executor
3. `v1RowCount` / `v2RowCount` from outcomes
4. Sample first `min(sampleSize, count)` rows (default 500)
5. `MigrationClassificationRules.classify(...)`
6. Set `recommendation` from classification

- [ ] **Step 4: Run PASS**

- [ ] **Step 5: Commit** — `feat(service): add migration compare service`

---

### Task 7: Report writer + REST compare endpoint

**Files:**
- Create: `MigrationReportWriter.java`
- Modify: `TemplateController.java`
- Create: `MigrationCompareServiceTests.java` (controller slice) or extend existing migration tests

- [ ] **Step 1: `MigrationReportWriter.write(MigrationComparisonReport)`**

Write `docs/migration/reports/db-{id}-{yyyyMMdd-HHmmss}.md` with sections: Summary, Counts, Sample match, Warnings, Classification.

- [ ] **Step 2: Add endpoint**

```java
@PostMapping("/migration/compare/{templateId}")
public R<MigrationComparisonReport> compareMigration(@PathVariable Long templateId,
        @RequestBody(required = false) MigrationCompareOptions options)
```

Flow:

1. Load `TemplatePO`, build V1
2. Build V2 draft: if entity already has V2 yaml use it; else `buildQuerySourceDraft(entity)` (existing)
3. `compareService.compare(...)`
4. `reportWriter.write`
5. Update inventory entry via `MigrationInventoryService.updateCompareResult(...)`

- [ ] **Step 3: Integration test** using `@SpringBootTest` + H2 + fixture template in DB (follow `TemplateControllerQuerySourceMigrationTests`)

- [ ] **Step 4: Commit** — `feat(service): add migration compare endpoint and reports`

---

## Phase M2 — Draft expansion and Wave 2

### Task 8: CHUNKED policy injection on migrate

**Files:**
- Modify: `V1QuerySourceDraftConverter` or `buildQuerySourceDraft` path
- Modify: `TemplateControllerQuerySourceMigrationTests.java`

- [ ] **Step 1: Test** — when V1 has single JDBC reader and no small `maxRows`, migrated draft includes:

```yaml
executionPolicy:
  mode: CHUNKED
  sourceChunkSize: 5000
  sinkBatchSize: 1000
  maxRowsInMemory: 500000
```

- [ ] **Step 2: Implement heuristic in draft builder** (only single query source, no compatibility-only flags)

- [ ] **Step 3: Commit** — `feat(service): suggest chunked execution policy on jdbc migrate`

---

### Task 9: Iterator / faker draft path (Wave 1)

**Files:**
- Create: `V1IteratorDraftConverter.java` (or extend existing converter package)
- Modify: `migrateQuerySourceV2ById` → rename alias; add `POST /migration/draft/{templateId}` calling unified `MigrationDraftService`

- [ ] **Step 1: Test** with `v1-iterator-simple.yaml` fixture — draft has `IteratorSourceVO` + simple SQL + console sink

- [ ] **Step 2: Implement minimal iterator migration** (number/constant iterator only in M2)

- [ ] **Step 3: Commit** — `feat(service): add iterator-family v1 draft migration`

---

### Task 10: Inventory + scorecard evidence (docs)

**Files:**
- Modify: `docs/migration/scenario-inventory.yaml` (≥10 entries with `lastCompareReportPath`)
- Modify: `docs/calcite-v1-parity-scorecard.md`
- Modify: `docs/calcite-v1-v2-migration-examples.md`

- [ ] **Step 1: Run compare for 5 Wave 1 + 3 Wave 2 templates** (DB or regression), commit reports under `docs/migration/reports/`

- [ ] **Step 2: Link inventory rows to report paths; set `migrationClass`**

- [ ] **Step 3: Add scorecard rows** for "migration dual-run evidence" and "inventory maintained"

- [ ] **Step 4: Commit** — `docs: add migration dual-run evidence and inventory`

---

## Phase M3 — Promote and retirement readiness

### Task 11: MigrationPromoteService

**Files:**
- Create: `MigrationPromoteService.java`
- Modify: `TemplateController.java` — `POST /migration/promote/{templateId}`

- [ ] **Step 1: Test**

```java
@Test
void promoteRequiresValidatedV2Draft() {
    // invalid draft -> IllegalArgumentException
}
@Test
void promoteUpdatesInventoryClassification() {
    // after promote, inventory migrationClass matches last compare
}
```

- [ ] **Step 2: Implement**

1. `TemplateV2Validator.validate(TemplateV2Normalizer.normalize(draft))`
2. Persist V2 yaml/json on `TemplatePO` (same as migrate today)
3. Inventory: set `migrationClass` from last report, `v2DraftPresent: true`
4. Do **not** delete V1 yaml in v1 (spec: notes only unless archive column exists)

- [ ] **Step 3: Commit** — `feat(service): add migration promote workflow`

---

### Task 12: Compatibility-only appendix + retirement checklist

**Files:**
- Create: `docs/migration/compatibility-only-templates.md`
- Create: `docs/migration/retirement-readiness.md`

- [ ] **Step 1: Generate compatibility-only list** from inventory where class = `COMPATIBILITY_ONLY`

- [ ] **Step 2: Retirement readiness** — per scenario family: P1 technical / P2 operational / P3 business gates with checkboxes referencing inventory ids (honest: P2 operational partial until explain ships)

- [ ] **Step 3: Commit** — `docs: add v1 migration retirement readiness checklist`

---

### Task 13: Full module verification

- [ ] **Run**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service,data-generator-calcite -am test "-Dsurefire.failIfNoSpecifiedTests=false"
```

Expected: BUILD SUCCESS

- [ ] **Fix failures, commit if needed**

---

## Plan self-review vs spec

| Spec requirement | Task |
|------------------|------|
| A3 inventory DB + repo | 1, 2 |
| Dual-run compare | 4–7 |
| Classification exact/approx/blocked | 4, 6 |
| Promote | 11 |
| CHUNKED Wave 2 | 8 |
| Wave 1 iterator draft | 9 |
| Scorecard + examples | 10 |
| Retirement gates doc | 12 |
| ≥10 templates compared | 10 (manual/CI step) |

No TBD in task steps. Type names consistent across tasks.

---

## Suggested commit sequence

1. `feat(service): add migration scenario inventory model`
2. `feat(service): seed migration inventory from repo and db`
3. `feat(service): add full-template migration analyzer`
4. `feat(service): add migration comparison classification rules`
5. `feat(service): add migration comparison report model`
6. `feat(service): add migration compare service`
7. `feat(service): add migration compare endpoint and reports`
8. `feat(service): suggest chunked execution policy on jdbc migrate`
9. `feat(service): add iterator-family v1 draft migration`
10. `docs: add migration dual-run evidence and inventory`
11. `feat(service): add migration promote workflow`
12. `docs: add v1 migration retirement readiness checklist`

Footer on AI-assisted commits per `.cursor/rules/git-commit-conventional-ai.mdc`.

---

## Execution handoff

**Plan saved to:** `docs/superpowers/plans/2026-05-19-v1-migration-dual-run.md`

**Two execution options:**

1. **Subagent-Driven (recommended)** — one subagent per task (M0→M3), review between tasks  
2. **Inline Execution** — implement in this session with checkpoints after M0, M1, M2, M3  

Reply **1** or **2** (or `从 Task 1 开始 subagent`) to start implementation.
