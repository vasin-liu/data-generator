# Wave 0 — V1 Hard Cut Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove V1 templates and migration from all product paths (console, public REST, CI, operator docs) so only Template V2 remains operable.

**Architecture:** Cut from the outside in — (1) reject V1 in editor/catalog APIs, (2) delete console migration UI and `/api/migration/**` + `/template/migration/**` REST, (3) delete migration Java package and wiring, (4) delete migration/V1 tests, (5) run `verify-console.ps1`.

**Tech Stack:** Java 25, Spring Boot 4, React console (`data-generator-console-web`), Playwright E2E, Maven Surefire.

**Spec:** `docs/superpowers/specs/2026-06-02-v2-capability-roadmap-design.md` (Wave 0, Approved 2026-06-02)

**Branch:** `feature/wave0-v1-retirement` off `master`

**Verify command (after each task group):**

```powershell
.\scripts\verify-console-unit.ps1 -IncludeWebBuild
.\scripts\verify-console.ps1
```

---

## File map

| Action | Paths |
|--------|-------|
| Delete (console) | `data-generator-console-web/src/app/pages/MigrationPage.tsx`, `src/app/editor/MigrationTab.tsx`, `src/api/migration.ts`, `src/config/features.ts` (migration flag) |
| Modify (console) | `src/app/routes*.tsx` or router, `ConsoleLayout.tsx`, `TemplateEditorPage.tsx`, `HomePage.tsx`, `ReviewPanel.tsx`, `e2e/specs/navigation.spec.ts`, `e2e/specs/api.console.spec.ts`, `e2e/helpers/test-ids.ts`, i18n JSON |
| Modify (service) | `TemplateEditorService.java`, `ConsoleTemplateController.java`, `TemplateController.java`, `TaskController.java`, `ConsoleRuntimeController.java`, `DataGeneratorProperties.java` |
| Delete (service) | `ConsoleMigrationController.java`, `console/migration/MigrationConsoleService.java`, `config/Migration*.java`, entire `template/migration/**` package |
| Delete (tests) | 31 files matching `*Migration*`, `*V1*` migration tests (list in Task 6) |
| Docs | `docs/operator-console-usage.md`; move `docs/migration/**` → `docs/archive/migration/**` |

---

### Task 1: Reject V1 templates in editor load API

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/template/editor/TemplateEditorService.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/template/editor/TemplateEditorServiceV1RejectionTests.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void loadForEditor_rejectsV1Template() {
    TemplatePO v1 = persistV1Template(); // helper inserts kind=V1 row
    assertThatThrownBy(() -> service.loadForEditor(v1.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("V1");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -Dtest=TemplateEditorServiceV1RejectionTests test`

Expected: FAIL (no rejection yet)

- [ ] **Step 3: Implement rejection in `loadForEditor`**

At start of `loadForEditor(Long id)`, after loading `TemplatePO`, if `TemplateDefinitionDetector` (or stored kind) is `V1`, throw:

```java
throw new IllegalStateException("Legacy V1 templates are no longer supported; create a Template V2.");
```

Also reject in `save` if id points to V1 entity.

- [ ] **Step 4: Run test to verify it passes**

Run: same `-Dtest=TemplateEditorServiceV1RejectionTests`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add data-generator-service/src/main/java/org/gensokyo/data/template/editor/TemplateEditorService.java \
  data-generator-service/src/test/java/org/gensokyo/data/template/editor/TemplateEditorServiceV1RejectionTests.java
git commit -m "feat(template): reject V1 templates in editor service"
```

---

### Task 2: Exclude V1 from console template catalog

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleTemplateController.java`
- Modify: `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleTemplateControllerTest.java`

- [ ] **Step 1: Write failing test** — list endpoint must not return V1 rows (seed one V1 + one V2, assert only V2 id present).

- [ ] **Step 2: Run test** — expect FAIL.

- [ ] **Step 3: Filter** — in list/query method, exclude templates where definition kind is V1 (use existing detector on `TemplatePO` yaml/json fields).

- [ ] **Step 4: Run test** — expect PASS.

- [ ] **Step 5: Commit** — `feat(console): hide legacy V1 templates from catalog`

---

### Task 3: Remove public migration REST facades

**Files:**
- Delete: `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleMigrationController.java`
- Delete: `data-generator-service/src/main/java/org/gensokyo/data/console/migration/MigrationConsoleService.java`
- Delete: `data-generator-service/src/test/java/org/gensokyo/data/console/migration/MigrationConsoleServiceTests.java`
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/controller/TemplateController.java` — remove fields + all 11 `/migration/**` mappings (lines ~332–507)

- [ ] **Step 1: Delete console migration controller + service + test.**

- [ ] **Step 2: Strip `TemplateController`** — remove migration service injections and endpoint methods; remove unused imports.

- [ ] **Step 3: Compile check**

Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests compile`

Expected: may FAIL on orphaned `MigrationConfig` beans — proceed to Task 4.

- [ ] **Step 4: Commit** — `refactor(service): remove public migration REST endpoints`

---

### Task 4: Delete migration backend package and config

**Files:**
- Delete: `data-generator-service/src/main/java/org/gensokyo/data/template/migration/**` (entire directory)
- Delete: `data-generator-service/src/main/java/org/gensokyo/data/config/MigrationConfig.java`
- Delete: `data-generator-service/src/main/java/org/gensokyo/data/config/MigrationSchedulingConfig.java`
- Delete: `data-generator-service/src/main/java/org/gensokyo/data/config/MigrationBatchCompareProperties.java`
- Modify: any remaining imports (grep `template.migration` under `data-generator-service`)

- [ ] **Step 1: Grep for references**

Run: `rg "template\.migration|MigrationConfig" data-generator-service/src/main/java`

- [ ] **Step 2: Delete package + config classes.**

- [ ] **Step 3: Fix compile errors** — remove dead imports in `TemplateController`, `querysource`, etc. Keep `TemplateV1Loader` only if still required by non-product code; if only migration used it, delete loader too.

- [ ] **Step 4: Compile + unit slice**

Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests compile`

- [ ] **Step 5: Commit** — `chore(service): remove migration backend package`

---

### Task 5: V1 execution — hard 410 on `/task/run`

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/controller/TaskController.java`
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/config/DataGeneratorProperties.java` (document v1 flag deprecated)
- Delete: `data-generator-service/src/test/java/org/gensokyo/data/controller/TaskControllerV1ExecutionFlagTests.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/controller/TaskControllerV1RetiredTests.java`

- [ ] **Step 1: Test** — when template is V1 kind, `POST /task/run` returns 4xx with clear message regardless of `v1-execution.enabled`.

- [ ] **Step 2: Implement** — if request resolves to V1 template, return `R.fail` / `ResponseStatusException` before pipeline start.

- [ ] **Step 3: Remove flag-gated V1 execution path** (or force property default false and ignore true in prod profile).

- [ ] **Step 4: Run targeted tests.**

- [ ] **Step 5: Commit** — `feat(task): retire V1 template execution path`

---

### Task 6: Delete migration and V1 test suite

**Files:** Delete all:

```text
data-generator-service/src/test/java/org/gensokyo/data/console/migration/
data-generator-service/src/test/java/org/gensokyo/data/controller/BuiltinClasspathTemplateMigrationWorkflowTests.java
data-generator-service/src/test/java/org/gensokyo/data/controller/MigrationWaveCohortSignoffTests.java
data-generator-service/src/test/java/org/gensokyo/data/controller/StagingSimulatedPromoteWorkflowTests.java
data-generator-service/src/test/java/org/gensokyo/data/controller/TemplateControllerMigration*.java
data-generator-service/src/test/java/org/gensokyo/data/controller/TemplateControllerQuerySourceMigrationTests.java
data-generator-service/src/test/java/org/gensokyo/data/template/migration/**
data-generator-service/src/test/java/org/gensokyo/data/template/querysource/V1QuerySourceExecutionPolicySuggesterTests.java
```

- [ ] **Step 1: Delete files listed above.**

- [ ] **Step 2: Run service tests**

Run: `.\scripts\verify-console-unit.ps1`

Expected: PASS (no references to deleted tests)

- [ ] **Step 3: Commit** — `test: remove migration and V1 retirement test suite`

---

### Task 7: Remove console migration UI and V1 read-only UX

**Files:**
- Delete: `MigrationPage.tsx`, `MigrationTab.tsx`, `api/migration.ts`, `config/features.ts`
- Modify: router (remove `/migration` route), `ConsoleLayout.tsx` (remove nav item), `TemplateEditorPage.tsx` (remove migration tab + V1 read-only alert; show error state only), `ReviewPanel.tsx` (remove v1Blocked branches — V1 never loads), `HomePage.tsx` (remove migration card), `ConsoleRuntimeController` DTO if needed (drop v1 flag from UI or mark retired static)

- [ ] **Step 1: Delete migration components and API client.**

- [ ] **Step 2: Update router + layout** — no `nav-migration`, no `/console/migration`.

- [ ] **Step 3: Simplify editor** — remove `migration` from `TAB_KEYS`; remove V1 note banner (load error already handled).

- [ ] **Step 4: Build frontend**

Run: `cd data-generator-console-web && npm run build`

Expected: SUCCESS, no missing imports

- [ ] **Step 5: Commit** — `feat(console): remove migration UI and V1 editor paths`

---

### Task 8: Update E2E specs

**Files:**
- Modify: `data-generator-console-web/e2e/specs/navigation.spec.ts` — delete `migration nav when enabled` test
- Modify: `data-generator-console-web/e2e/specs/api.console.spec.ts` — remove `GET /api/migration/summary` test
- Modify: `data-generator-console-web/e2e/helpers/test-ids.ts` — remove `nav.migration`, `pages.migration`
- Modify: `data-generator-console-web/e2e/helpers/api.ts` — remove migration helper if unused

- [ ] **Step 1: Apply E2E edits.**

- [ ] **Step 2: Run E2E**

Run: `.\scripts\verify-console.ps1`

Expected: all tests pass (count drops by 1–2 vs prior 28 pass / 1 skip)

- [ ] **Step 3: Commit** — `test(console): drop migration E2E coverage`

---

### Task 9: Archive docs and update operator guide

**Files:**
- Move: `docs/migration/**` → `docs/archive/migration/**`
- Modify: `docs/operator-console-usage.md` — remove Migration section, V1 re-enable instructions, runtime flag row for v1
- Modify: `README.md` — only if it mentions migration UI (minimal touch)

- [ ] **Step 1: `git mv docs/migration docs/archive/migration`**

- [ ] **Step 2: Edit operator-console-usage.md** — V2-only workflow; link archived docs for historical reference.

- [ ] **Step 3: Commit** — `docs: archive migration runbooks and update console guide`

---

### Task 10: Final verification and merge prep

- [ ] **Step 1: Full verify pipeline**

Run: `.\scripts\verify-console.ps1`

Expected: unit + build + 27+ Playwright tests PASS

- [ ] **Step 2: Grep product paths**

Run: `rg -i "VITE_ENABLE_MIGRATION|/api/migration|nav-migration" data-generator-console-web data-generator-service/src/main`

Expected: no matches (except archive comments)

- [ ] **Step 3: Open PR** with summary referencing spec Wave 0 acceptance criteria.

---

## Spec coverage self-review

| Spec requirement | Task |
|------------------|------|
| V1 execution off | Task 5 |
| V1 API/list/editor reject | Tasks 1–2, 7 |
| Migration UI/API removed | Tasks 3–4, 7–8 |
| CI no V1/migration tests | Task 6 |
| Docs archive | Task 9 |
| verify-console green | Task 10 |

## Explicit deferrals (not in Wave 0)

- Wave 1 MaterializationPolicy UI
- B-lite publish/RBAC
- Deleting V1 **runtime engine** classes in `data-generator-core` (only product path cut; engine stubs may remain until separate cleanup if compile requires)

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-02-wave0-v1-retirement.md`.

**Two execution options:**

1. **Subagent-driven (recommended)** — dispatch fresh subagent per task, review between tasks
2. **Inline execution** — implement task-by-task in current session with `verify-console.ps1` after each task group
