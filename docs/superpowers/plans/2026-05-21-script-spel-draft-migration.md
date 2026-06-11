# SCRIPT → SpEL Draft Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Emit **SQL + SpEL** V2 migration drafts for built-in synthetic and JDBC/multi_source templates with plain/SPEL field scripts, closing WS1 draft gap per `docs/superpowers/specs/2026-05-21-script-spel-draft-migration-design.md`.

**Architecture:** Fix analyzer `SPEL` detection → new `V1ScriptToSpelDraftConverter` (field topo sort + `#dataset`→`#row` rewrite) → `MigrationDraftService` appends `SpelTransformVO` after SQL draft → `SpelTransformFactory` registers `#faker` for 2b parking/idps expressions.

**Tech Stack:** Java 25, Maven (`mvnw-jdk25.ps1`), Spring SpEL, `data-generator-service` migration package, `data-generator-calcite` `SpelTransformFactory`, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-05-21-script-spel-draft-migration-design.md` (Approved 2026-05-21)

---

## File map

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `V1TemplateMigrationAnalyzer.java` | Treat `language.type: SPEL` as plain script |
| Modify | `V1TemplateMigrationAnalyzerTests.java` | SPEL yaml → `PATH_SPEL` |
| Create | `V1SpelExpressionRewriter.java` | `#dataset` / `#dataset['x']` → `#row` forms |
| Create | `V1ScriptToSpelDraftConverter.java` | Fields → `SpelTransformVO` |
| Create | `V1SpelExpressionRewriterTests.java` | Rewrite unit tests |
| Create | `V1ScriptToSpelDraftConverterTests.java` | Converter + topo sort tests |
| Modify | `MigrationDraftService.java` | Attach SpEL transform after SQL draft |
| Create | `MigrationDraftServiceSpelTests.java` | Integration: iterator + JDBC fixtures |
| Modify | `SpelTransformFactory.java` | Register `#faker` (Datafaker) per row eval |
| Modify | `SpelTransformFactoryTests.java` | `#faker.number...` smoke |
| Modify | `BuiltinClasspathTemplateRegressionTests.java` | Assert SpEL in drafts for `spel` path |
| Modify | `BuiltinClasspathTemplateMigrationWorkflowTests.java` | Optional draft shape checks |
| Modify | `docs/template-v2-transformer-strategy.md` | SpEL migration draft status |
| Modify | `docs/migration/retirement-readiness.md` | P1 checkbox when done |
| Modify | `docs/migration/reports/sample-regression-v1-iterator-simple.md` | SpEL note (after compare regen if needed) |

---

## Task 1: Analyzer — recognize `language.type: SPEL`

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/template/migration/V1TemplateMigrationAnalyzer.java`
- Modify: `data-generator-service/src/test/java/org/gensokyo/data/template/migration/V1TemplateMigrationAnalyzerTests.java`

- [ ] **Step 1: Write failing test**

Add to `V1TemplateMigrationAnalyzerTests.java`:

```java
@Test
void recommendsSpelPathForExplicitSpelLanguageType() throws Exception {
    String yaml = """
            name: spel-lang-field
            fields:
              - name: PARKING_LOT_ID
                stages:
                  - type: SCRIPT
                    language:
                      type: SPEL
                      content: "#dataset.ID"
            """;
    TemplateVO v1 = yamlParser.parse(yaml, TemplateVO.class);
    TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);

    Assertions.assertEquals("spel", analysis.getRecommendedPath());
    Assertions.assertNotEquals(MigrationClassification.COMPATIBILITY_ONLY, analysis.getSuggestedClass());
}
```

- [ ] **Step 2: Run test — expect FAIL**

```powershell
cd D:\Work\99_Code\data-generator
.\mvnw-jdk25.ps1 -pl data-generator-service `
  "-Dtest=V1TemplateMigrationAnalyzerTests#recommendsSpelPathForExplicitSpelLanguageType" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL — `recommendedPath` is `sql` or not `spel` (SPEL type not recognized).

- [ ] **Step 3: Implement `isPlainScriptStage`**

In `V1TemplateMigrationAnalyzer.java`, replace the type check tail with:

```java
String type = language.getType().trim();
if ("JAVASCRIPT".equalsIgnoreCase(type)) {
    return false;
}
return type.isBlank()
        || "PLAIN".equalsIgnoreCase(type)
        || "SPEL".equalsIgnoreCase(type);
```

- [ ] **Step 4: Run analyzer tests — expect PASS**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service `
  "-Dtest=V1TemplateMigrationAnalyzerTests" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- [ ] **Step 5: Commit**

```bash
git add data-generator-service/src/main/java/org/gensokyo/data/template/migration/V1TemplateMigrationAnalyzer.java
git add data-generator-service/src/test/java/org/gensokyo/data/template/migration/V1TemplateMigrationAnalyzerTests.java
git commit -m "fix(migration): treat SPEL script language as plain script path"
```

Footer: `AI-Assisted-by: Cursor`, `Co-authored-by: Gensokyo <liuweixing@pcitech.com>`

---

## Task 2: Expression rewriter (`#dataset` → `#row`)

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/template/migration/V1SpelExpressionRewriter.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/template/migration/V1SpelExpressionRewriterTests.java`

- [ ] **Step 1: Write failing tests**

Create `V1SpelExpressionRewriterTests.java`:

```java
@Test
void rewritesDatasetPropertyAccess() {
    Assertions.assertEquals(
            "#row['PARKING_LOT_NAME']",
            V1SpelExpressionRewriter.rewrite("#dataset.PARKING_LOT_NAME"));
}

@Test
void rewritesDatasetBracketAccess() {
    Assertions.assertEquals(
            "#row['ID']",
            V1SpelExpressionRewriter.rewrite("#dataset['ID']"));
}

@Test
void rewritesBareDatasetToRow() {
    Assertions.assertEquals("#row", V1SpelExpressionRewriter.rewrite("#dataset"));
}

@Test
void preservesFakerReferences() {
    String expr = "#faker.number.numberBetween(1,#dataset)";
    String rewritten = V1SpelExpressionRewriter.rewrite(expr);
    Assertions.assertTrue(rewritten.contains("#faker"));
    Assertions.assertTrue(rewritten.contains("#row"));
}
```

- [ ] **Step 2: Run tests — expect FAIL**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service `
  "-Dtest=V1SpelExpressionRewriterTests" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- [ ] **Step 3: Implement rewriter**

Create `V1SpelExpressionRewriter.java` (final class, private ctor):

```java
public final class V1SpelExpressionRewriter {

    private static final Pattern DATASET_DOT = Pattern.compile("#dataset\\.([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern DATASET_BRACKET = Pattern.compile("#dataset\\['([^']+)'\\]");
    private static final Pattern DATASET_BARE = Pattern.compile("#dataset\\b");

    public static String rewrite(String expression) {
        if (expression == null || expression.isBlank()) {
            return expression;
        }
        String result = DATASET_BRACKET.matcher(expression).replaceAll("#row['$1']");
        result = DATASET_DOT.matcher(result).replaceAll("#row['$1']");
        result = DATASET_BARE.matcher(result).replaceAll("#row");
        return result;
    }
}
```

Add copyright block + class Javadoc per repo Java rules.

- [ ] **Step 4: Run tests — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(migration): add V1 SpEL expression rewriter for dataset to row"
```

---

## Task 3: `V1ScriptToSpelDraftConverter`

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/template/migration/V1ScriptToSpelDraftConverter.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/template/migration/V1ScriptToSpelDraftConverterTests.java`

- [ ] **Step 1: Write failing test — parking field**

```java
@Test
void convertsSpelFieldFromBuiltinParkingFixture() throws Exception {
    String yaml = new ClassPathResource("template/tocc/parking/11_parking_online_space_record.yaml")
            .getContentAsString(StandardCharsets.UTF_8);
    TemplateVO v1 = yamlParser.parse(yaml, TemplateVO.class);

    SpelTransformVO spel = V1ScriptToSpelDraftConverter.convert(v1);

    Assertions.assertNotNull(spel);
    Assertions.assertFalse(spel.getColumns().isEmpty());
    SpelColumnMapping lotId = spel.getColumns().stream()
            .filter(c -> "PARKING_LOT_ID".equals(c.getName()))
            .findFirst()
            .orElseThrow();
    Assertions.assertEquals("#row['ID']", lotId.getExpression());
}
```

Note: test uses main resources path — if test module cannot read `src/main/resources`, load via `BuiltinClasspathTemplateCatalog` fixture for `tocc/parking/11_...` instead.

- [ ] **Step 2: Run test — expect FAIL** (class missing)

- [ ] **Step 3: Implement converter**

`V1ScriptToSpelDraftConverter` public API:

```java
public static boolean hasMigratableScriptFields(TemplateVO v1);
public static SpelTransformVO convert(TemplateVO v1);
```

Implementation sketch:

1. If `V1TemplateMigrationAnalyzer.analyze(v1).getSuggestedClass() == COMPATIBILITY_ONLY` → return `null` / empty.
2. Build ordered field list: topological sort on `dependsOn` (Kahn); cycle → `IllegalArgumentException`.
3. For each field, find **last** `ScriptStageVO` where `isPlainScriptStage` (extract package-private helper to package-local `V1ScriptStageSupport` or duplicate minimal check: not JAVASCRIPT, type blank/PLAIN/SPEL).
4. Skip field when no SCRIPT stage.
5. `SpelColumnMapping`: `name` = field name, `expression` = `V1SpelExpressionRewriter.rewrite(content.trim())`.
6. Return `SpelTransformVO` with columns list (empty → return `null`).

- [ ] **Step 4: Add topo-sort unit test**

```java
@Test
void ordersFieldsByDependsOnBeforeDependent() {
    // field B depends on A; assert columns list index of A < B
}
```

- [ ] **Step 5: Run converter tests — expect PASS**

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(migration): add V1 script to SpEL draft converter"
```

---

## Task 4: Wire `MigrationDraftService`

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/template/migration/MigrationDraftService.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/template/migration/MigrationDraftServiceSpelTests.java`

- [ ] **Step 1: Write failing integration test**

```java
@Test
void iteratorDraftIncludesSpelTransformForRegressionIteratorSimple() throws Exception {
    TemplateVO v1 = loadFixture("migration/regression/v1-iterator-simple.yaml");
    TemplateV2DraftVO draft = draftService.buildDraft(v1);
    TemplateV2VO normalized = TemplateV2Normalizer.normalize(draft);

    Assertions.assertEquals(2, normalized.getTransformers().size());
    Assertions.assertInstanceOf(SqlTransformVO.class, normalized.getTransformers().get(0));
    Assertions.assertInstanceOf(SpelTransformVO.class, normalized.getTransformers().get(1));
    SpelTransformVO spel = (SpelTransformVO) normalized.getTransformers().get(1);
    Assertions.assertFalse(spel.getColumns().isEmpty());
    TemplateV2Validator.validate(normalized);
}
```

- [ ] **Step 2: Run test — expect FAIL** (only one transformer today)

- [ ] **Step 3: Implement wiring**

In `MigrationDraftService.buildDraft`:

```java
TemplateV2DraftVO draft;
// existing query / iterator branch ...
SpelTransformVO spel = V1ScriptToSpelDraftConverter.convert(v1);
if (spel != null && CollectKit.isNotEmpty(spel.getColumns())) {
    if (draft.getTransformers() == null) {
        draft.setTransformers(new ArrayList<>());
    }
    draft.getTransformers().add(spel);
}
return draft;
```

Keep existing `draft.setTransform(transform)` for SQL; normalizer merges `[sql, spel]`.

- [ ] **Step 4: Add JDBC fixture test**

Load `template/tocc/parking/11_parking_online_space_record.yaml` (or catalog fixture); assert normalized draft has SQL + SpEL and validator passes.

- [ ] **Step 5: Run `MigrationDraftServiceSpelTests` — expect PASS**

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(migration): attach SpEL transform to V2 migration drafts"
```

---

## Task 5: `SpelTransformFactory` — `#faker` support

**Files:**
- Modify: `data-generator-calcite/src/main/java/org/gensokyo/data/calcite/sql/SpelTransformFactory.java`
- Modify: `data-generator-calcite/src/test/java/org/gensokyo/data/calcite/sql/SpelTransformFactoryTests.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void evaluatesFakerExpression() {
    RowSchema schema = schema(new ColumnDef("id", "VARCHAR", false));
    List<Row> rows = List.of(new Row(Map.of("id", "x")));
    CalciteExecutionContext context = new CalciteExecutionContext()
            .addTable("input", schema, rows);

    SpelTransformVO transform = new SpelTransformVO();
    transform.setColumns(List.of(mapping("n", "#faker.number.numberBetween(1,5)")));

    SpelTransformFactory factory = new SpelTransformFactory();
    CalciteRowTransformer.TransformResult result = factory.apply(transform, context);

    Object n = result.rows().getFirst().values().get("n");
    Assertions.assertInstanceOf(Number.class, n);
    int value = ((Number) n).intValue();
    Assertions.assertTrue(value >= 1 && value <= 5);
}
```

- [ ] **Step 2: Run test — expect FAIL** (SpEL cannot resolve `#faker`)

- [ ] **Step 3: Register faker on evaluation context**

In `applyMappings`, after `setVariable(ROW_VARIABLE, values)`:

```java
evaluationContext.setVariable(Const.SCRIPT_VAR_FAKER, FAKER);
```

Add module import or dependency: `data-generator-calcite` already has `datafaker` — use:

```java
private static final Faker FAKER = new Faker(Locale.CHINA);
```

Import `org.gensokyo.data.constant.Const` — add `data-generator-core` dependency if not already transitive from calcite (verify `pom.xml`; likely already present).

- [ ] **Step 4: Run `SpelTransformFactoryTests` — expect PASS**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(calcite): register faker variable in SpEL transform"
```

---

## Task 6: Built-in catalog regression (floor 30+ SpEL drafts)

**Files:**
- Modify: `data-generator-service/src/test/java/org/gensokyo/data/template/BuiltinClasspathTemplateRegressionTests.java`

- [ ] **Step 1: Add test method**

```java
@Test
void spelPathBuiltinTemplatesIncludeSpelTransformInDraft() {
    List<String> failures = new ArrayList<>();
    int spelDraftCount = 0;

    for (BuiltinClasspathTemplateCatalog.Fixture fixture : BuiltinClasspathTemplateCatalog.loadAll()) {
        TemplateVO v1 = loadV1(fixture);
        TemplateMigrationAnalysisDTO analysis = V1TemplateMigrationAnalyzer.analyze(v1);
        if (analysis.getSuggestedClass() == MigrationClassification.COMPATIBILITY_ONLY) {
            continue;
        }
        if (!"spel".equals(analysis.getRecommendedPath())) {
            continue;
        }
        try {
            TemplateV2DraftVO draft = draftService.buildDraft(v1);
            TemplateV2VO normalized = TemplateV2Normalizer.normalize(draft);
            boolean hasSpel = normalized.getTransformers().stream()
                    .anyMatch(SpelTransformVO.class::isInstance);
            if (!hasSpel) {
                failures.add(fixture.displayName() + ": missing SpelTransformVO");
                continue;
            }
            spelDraftCount++;
            TemplateV2Validator.validate(normalized);
        } catch (Exception e) {
            failures.add(fixture.displayName() + ": " + e.getMessage());
        }
    }
    Assertions.assertTrue(spelDraftCount >= 30,
            () -> "expected >=30 spel-path drafts, got " + spelDraftCount);
    Assertions.assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
}
```

- [ ] **Step 2: Run — expect PASS** (after Tasks 1–4)

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am `
  "-Dtest=BuiltinClasspathTemplateRegressionTests#spelPathBuiltinTemplatesIncludeSpelTransformInDraft" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

If count &lt; 30, fix analyzer/converter gaps before lowering floor — spec minimum is 30.

- [ ] **Step 3: Commit**

```bash
git commit -m "test(migration): assert SpEL transform on builtin spel-path templates"
```

---

## Task 7: Cohort fixtures (demo/28, parking/11)

**Files:**
- Modify: `data-generator-service/src/test/java/org/gensokyo/data/controller/BuiltinClasspathTemplateMigrationWorkflowTests.java` (optional assert)
- Or extend: `MigrationDraftServiceSpelTests.java` with catalog ids

- [ ] **Step 1: Assert demo/28 draft has SpEL**

Use `BuiltinClasspathTemplateCatalog` to resolve `demo/28_常量迭代器重复多次样例.yaml`; build draft; assert `SpelTransformVO` present.

- [ ] **Step 2: Run workflow slice**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am `
  "-Dtest=MigrationDraftServiceSpelTests,BuiltinClasspathTemplateMigrationWorkflowTests" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- [ ] **Step 3: Commit** (if separate from Task 6)

---

## Task 8: Documentation and retirement checklist

**Files:**
- Modify: `docs/template-v2-transformer-strategy.md`
- Modify: `docs/migration/retirement-readiness.md`
- Modify: `docs/superpowers/specs/2026-05-21-script-spel-draft-migration-design.md` (status → Implemented)

- [ ] **Step 1: Update transformer strategy**

Under built-in non-SQL section, change status to implemented and link `V1ScriptToSpelDraftConverter`.

- [ ] **Step 2: Add P1 readiness line**

```markdown
- [x] SCRIPT → SpEL migration draft bridge (`V1ScriptToSpelDraftConverter`, SQL+SpEL chain)
```

- [ ] **Step 3: Set spec status Implemented**

- [ ] **Step 4: Commit**

```bash
git commit -m "docs(migration): mark SCRIPT to SpEL draft bridge complete"
```

---

## Task 9: Verification slice + full module test

**Files:** None (verification)

- [x] **Step 1: Run epic CI slice**

```powershell
cd D:\Work\99_Code\data-generator
.\mvnw-jdk25.ps1 -pl data-generator-service,data-generator-calcite -am `
  "-Dtest=V1SpelExpressionRewriterTests,V1ScriptToSpelDraftConverterTests,V1TemplateMigrationAnalyzerTests,MigrationDraftServiceSpelTests,SpelTransformFactoryTests,BuiltinClasspathTemplateRegressionTests" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: `BUILD SUCCESS`, 0 failures.

- [ ] **Step 2: Optional full service module**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am test
```

---

## Spec coverage (plan self-review)

| Spec requirement | Task |
|------------------|------|
| Analyzer SPEL type | Task 1 |
| `V1ScriptToSpelDraftConverter` | Tasks 2–3 |
| `MigrationDraftService` wiring | Task 4 |
| `#faker` runtime | Task 5 |
| ≥35 / floor 30 CI drafts | Task 6 |
| Fixtures demo/28, parking/11, v1-iterator-simple | Tasks 4, 7 |
| Docs + retirement P1 | Task 8 |
| Orchestration excluded | Task 3 guard via analyzer |
| Multi-source 2b | Tasks 3–4, 7 |

---

## Revision history

| Date | Change |
|------|--------|
| 2026-05-21 | Initial plan from approved spec (scope 2b) |
| 2026-05-22 | Implemented; full reactor green; compare draft split documented |
