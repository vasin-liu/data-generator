# W3 Orchestration Blocking Assessment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Quantify builtin template orchestration/JS blocking for honest V1 retirement boundaries (path B, policy S1) without staging.

**Architecture:** Test-scoped `BuiltinTemplateMigrationCensus` runs `V1TemplateMigrationAnalyzer` over `BuiltinClasspathTemplateCatalog`, emits markdown summary + detail table, committed under `docs/migration/reports/`. One-page boundary doc for product. No runtime API changes.

**Tech Stack:** Java 25, Maven `mvnw-jdk25.ps1`, JUnit 5, existing migration analyzer + builtin catalog.

**Spec:** `docs/superpowers/specs/2026-05-22-w3-orchestration-blocking-assessment-design.md`

---

## File map

| Action | Path |
|--------|------|
| Create | `data-generator-service/src/test/java/org/gensokyo/data/template/migration/BuiltinTemplateMigrationCensus.java` |
| Create | `data-generator-service/src/test/java/org/gensokyo/data/template/migration/BuiltinTemplateMigrationCensusTest.java` |
| Create | `docs/migration/reports/builtin-orchestration-census.md` (generated content, committed) |
| Create | `docs/migration/orchestration-retirement-boundary.md` |
| Modify | `docs/migration/wave-freeze-schedule.md` |
| Modify | `docs/migration/compatibility-only-templates.md` |
| Modify | `docs/migration/retirement-readiness.md` |
| Modify | `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md` |

---

### Task 1: Census engine

**Files:**
- Create: `data-generator-service/src/test/java/org/gensokyo/data/template/migration/BuiltinTemplateMigrationCensus.java`

- [ ] **Step 1: Add census types and runner**

Implement `BuiltinTemplateMigrationCensus` in test sources (same package as analyzer for package-private stage helpers if needed):

- `record Row(String relativePath, TemplateMigrationAnalysisDTO analysis)`
- `record Summary(int total, int compatibilityOnly, Map<String, Long> byFamily, byPath, byBlockerSignal)`
- `static CensusResult run()` — load catalog, analyze each, build rows + summary
- Blocker signals: scan `analysis.getBlockers()` for substrings `PAUSE`, `LOG`, `SHARED`, `JavaScript` (case per analyzer messages)

- [ ] **Step 2: Add markdown renderer**

- `static String toMarkdown(CensusResult result)` — summary bullets + markdown table sorted by `relativePath`
- Include generated timestamp line: `Generated: 2026-05-22 (BuiltinTemplateMigrationCensusTest)`

- [ ] **Step 3: Commit**

```bash
git add data-generator-service/src/test/java/org/gensokyo/data/template/migration/BuiltinTemplateMigrationCensus.java
git commit -m "test(migration): add builtin template migration census reporter"
```

---

### Task 2: Census tests and committed report

**Files:**
- Create: `data-generator-service/src/test/java/org/gensokyo/data/template/migration/BuiltinTemplateMigrationCensusTest.java`
- Create: `docs/migration/reports/builtin-orchestration-census.md`

- [ ] **Step 1: Write failing invariants test**

```java
@Test
void censusCoversBuiltinCatalog() {
    BuiltinTemplateMigrationCensus.CensusResult result = BuiltinTemplateMigrationCensus.run();
    Assertions.assertTrue(result.summary().total() >= 50);
}

@Test
void pauseRegressionFixtureIsCompatibilityOnly() {
    var row = findRow("migration/regression/v1-with-pause.yaml");
    Assertions.assertEquals(MigrationClassification.COMPATIBILITY_ONLY, row.analysis().getSuggestedClass());
    Assertions.assertEquals("orchestration_legacy", row.analysis().getScenarioFamily());
}

@Test
void parking11IsSpelMigratable() {
    var row = findRow("tocc/parking/11_parking_online_space_record.yaml");
    Assertions.assertNotEquals(MigrationClassification.COMPATIBILITY_ONLY, row.analysis().getSuggestedClass());
    Assertions.assertEquals("spel", row.analysis().getRecommendedPath());
}
```

- [ ] **Step 2: Run tests**

```powershell
cd D:\Work\99_Code\data-generator
.\mvnw-jdk25.ps1 -pl data-generator-service `
  "-Dtest=BuiltinTemplateMigrationCensusTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS (after census class exists)

- [ ] **Step 3: Regenerate and commit report**

Test method `writesBuiltinOrchestrationCensusReport()`:

```java
@Test
void writesBuiltinOrchestrationCensusReport() throws Exception {
    var result = BuiltinTemplateMigrationCensus.run();
    Path report = Path.of("..", "docs", "migration", "reports", "builtin-orchestration-census.md");
    Files.writeString(report, BuiltinTemplateMigrationCensus.toMarkdown(result));
    Assertions.assertTrue(Files.size(report) > 500);
}
```

Run once, commit `docs/migration/reports/builtin-orchestration-census.md`.

- [ ] **Step 4: Commit**

```bash
git add data-generator-service/src/test/java/org/gensokyo/data/template/migration/BuiltinTemplateMigrationCensusTest.java docs/migration/reports/builtin-orchestration-census.md
git commit -m "test(migration): add builtin orchestration census invariants and report"
```

---

### Task 3: Product boundary one-pager

**Files:**
- Create: `docs/migration/orchestration-retirement-boundary.md`

- [ ] **Step 1: Write boundary doc**

Sections:

1. Purpose (honest retirement while staging unavailable)
2. Three buckets: migratable W1/W2, W3 exempt, M2 production unknown
3. Link to `builtin-orchestration-census.md` for numbers
4. S1 policy: no W3 freeze date
5. What SpEL 2b did / did not solve

- [ ] **Step 2: Commit**

```bash
git add docs/migration/orchestration-retirement-boundary.md
git commit -m "docs(migration): add orchestration retirement boundary one-pager"
```

---

### Task 4: Cross-link existing migration docs

**Files:**
- Modify: `docs/migration/wave-freeze-schedule.md`, `compatibility-only-templates.md`, `retirement-readiness.md`, `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md`

- [ ] **Step 1: Add census links and W3 S1 wording**

- `wave-freeze-schedule.md` — W3 row cites census report
- `compatibility-only-templates.md` — top link to census + boundary doc
- `retirement-readiness.md` — M1 table row for W3 census; new evidence row in samples table
- `deferred-ops-design.md` — path B deliverable checked

- [ ] **Step 2: Commit**

```bash
git add docs/migration/*.md docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md
git commit -m "docs(migration): link builtin orchestration census to retirement gates"
```

---

### Task 5: Verification

- [ ] **Step 1: Run census + regression slice**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am `
  "-Dtest=BuiltinTemplateMigrationCensusTest,BuiltinClasspathTemplateRegressionTests" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Optional full reactor**

```powershell
.\mvnw-jdk25.ps1 test
```

---

## Plan self-review (spec coverage)

| Spec requirement | Task |
|------------------|------|
| Census engine | Task 1 |
| Committed report | Task 2 |
| Boundary one-pager | Task 3 |
| Doc cross-links | Task 4 |
| No runtime change | N/A (test/docs only) |
| M2 deferred | Task 3 text |
| S1 policy | Task 3, 4 |
