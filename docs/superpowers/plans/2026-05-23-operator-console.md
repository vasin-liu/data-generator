# Operator Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a Vaadin operator console with V2 structured template forms (+ YAML advanced), archived templates, persisted JDBC datasources (D1), task execution history, and a migration tab.

**Architecture:** Backend-first slices (REST + JPA) per phase, then Vaadin views calling the same services. `TemplateEditorService` owns draft parse/normalize/validate/persist. `TaskExecutionService` wraps run + lifecycle. `DataSourceConfigService` owns D1 persistence + runtime sync. UI package `org.gensokyo.data.ui`.

**Tech Stack:** Java 25, Spring Boot 4, Vaadin 24.4 (`vaadin-spring-boot-starter`), JPA/H2 or team DB, existing `TemplateV2Validator`, `TemplateV2ControlPlaneService`, migration APIs.

**Spec:** `docs/superpowers/specs/2026-05-23-operator-console-design.md` (Approved 2026-05-23)

**Branch:** `feature-operator-console` (recommended) off current `feature-4.0` / `master`

**Verify command (repeat after each task group):**

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am test
```

---

## File map (by phase)

| Phase | Create / modify (primary) |
|-------|---------------------------|
| P0 | `data-generator-service/pom.xml`, `org/gensokyo/data/ui/**`, `DataGeneratorApplication` |
| P1 backend | `TemplatePO` (+archived), `TemplateEditorService`, `TemplateEditorController`, DTOs, tests |
| P1 UI | `TemplateListView`, `TemplateEditorView`, form steps under `ui/template/editor/**` |
| P2 | `DataSourceConfigPO`, `DataSourceConfigRepository`, `DataSourceBootstrap`, `DataSourceConfigService`, controller extensions, `ui/datasource/**` |
| P3 | `TaskExecutionPO`, `TaskExecutionRepository`, `TaskExecutionService`, `TaskExecutionController`, hooks in `TaskController` + `DefaultDataPipelineTask` + V2 runner wrapper, `ui/job/**` |
| P4 | `ui/migration/**`, migration panel in editor |
| Docs | `docs/operator-console-usage.md` |

---

## P0 — Vaadin shell

### Task 0.1: Enable Vaadin in service module

**Files:**
- Modify: `data-generator-service/pom.xml`
- Modify: `data-generator-service/src/main/resources/application.yaml` (or phase7 test yaml pattern)

- [ ] **Step 1:** Add `com.vaadin:vaadin-spring-boot-starter` dependency (version from parent BOM `${vaadin.version}`).
- [ ] **Step 2:** Ensure `spring.main.allow-bean-definition-overriding` / Vaadin servlet path does not clash with existing REST (`server.servlet.context-path` if any — document chosen prefix e.g. UI at `/` REST unchanged).
- [ ] **Step 3:** Run `.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests compile` — expect SUCCESS.

### Task 0.2: Main layout and placeholder routes

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/ui/MainLayout.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/ui/HomeView.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/ui/PlaceholderView.java` (reusable)
- Create: stubs: `ui/template/TemplateListView.java`, `ui/datasource/DataSourceListView.java`, `ui/job/JobListView.java`, `ui/migration/MigrationDashboardView.java`

- [ ] **Step 1:** `MainLayout` with side nav: Templates, Datasources, Jobs, Migration.
- [ ] **Step 2:** Banner showing `pci.data.generator.v1-execution.enabled` from `DataGeneratorProperties`.
- [ ] **Step 3:** Each route shows placeholder title; manual smoke: start app, open `/` in browser.
- [ ] **Step 4:** Commit: `feat(ui): add vaadin operator console shell`

---

## P1 — Template editor (backend)

### Task 1.1: Template archived columns

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/model/po/TemplatePO.java`
- Modify: `data-generator-service/src/test/resources/application-phase7-test.yaml` (if schema auto-update)
- Create: `data-generator-service/src/test/java/org/gensokyo/data/repository/TemplateArchiveTests.java`

- [ ] **Step 1:** Add fields: `Boolean archived` (default false), `Instant archivedAt` (nullable).
- [ ] **Step 2:** Test: save template, set archived, `findAll` filter excludes archived when `includeArchived=false`.
- [ ] **Step 3:** Commit: `feat(template): add archived flag on template entity`

### Task 1.2: TemplateEditorService

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/template/editor/TemplateEditorService.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/template/editor/TemplateEditorPayload.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/template/editor/TemplateEditorServiceTests.java`

- [ ] **Step 1:** `createEmptyDraft()` — minimal V2: name, iterator source, console sink, sql transform stub.
- [ ] **Step 2:** `loadForEditor(Long id)` — detect kind via `TemplateDefinitionDetector`; return draft + kind + v1Yaml if legacy.
- [ ] **Step 3:** `save(Long id, TemplateV2DraftVO draft)` — normalize, validate, dump yaml + json, update `TemplatePO`; reject if archived.
- [ ] **Step 4:** `archive(Long id)` / `restore(Long id)` — set/clear archived; fail archive if `TaskExecutionService.isRunning(templateId)` (stub false until P3, then wire).
- [ ] **Step 5:** Tests: round-trip save/load; archive hides from list query.
- [ ] **Step 6:** Commit: `feat(template): add v2 template editor service`

### Task 1.3: Template editor REST

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/controller/TemplateEditorController.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/controller/TemplateEditorControllerTests.java`
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/controller/TaskController.java` — `list()` excludes `archived=true`

- [ ] **Step 1:** Endpoints per spec: `POST /template/v2/create`, `GET /template/v2/editor/{id}`, `PUT /template/v2/editor/{id}`, `POST /template/{id}/archive`, `POST /template/{id}/restore`.
- [ ] **Step 2:** Integration tests with `@SpringBootTest` + H2 (mirror `TemplateControllerAdminApiTests` pattern).
- [ ] **Step 3:** Commit: `feat(template): expose v2 editor and archive rest api`

---

## P1 — Template editor (Vaadin forms + YAML)

### Task 1.4: Template list view

**Files:**
- Modify: `ui/template/TemplateListView.java`
- Create: `ui/template/TemplateListPresenter.java` (optional thin delegate)

- [ ] **Step 1:** Grid columns: id, name, kind (V1/V2 badge), archived filter toggle.
- [ ] **Step 2:** Actions: New → navigate `templates/new`; Open → `templates/{id}`; Archive with confirm.
- [ ] **Step 3:** Data via `TaskController.list` or new `GET /template/v2/list?includeArchived=` if added.
- [ ] **Step 4:** Commit: `feat(ui): template list with archive filter`

### Task 1.5: Editor shell — stepper + YAML toggle

**Files:**
- Create: `ui/template/TemplateEditorView.java`
- Create: `ui/template/editor/EditorModeToggle.java`
- Create: `ui/template/editor/YamlAdvancedPanel.java`

- [ ] **Step 1:** Load `TemplateEditorPayload` on enter; show V1 legacy banner (read-only form, link to Migration tab).
- [ ] **Step 2:** `Tabs` or `Stepper`: General | Sources | Transform | Sinks | Execution | Review.
- [ ] **Step 3:** YAML panel: TextArea/Monaco; buttons Apply YAML → rebind bean; Sync from Form.
- [ ] **Step 4:** Conflict dialog: YAML wins on Apply (per spec).
- [ ] **Step 5:** Commit: `feat(ui): template editor shell with yaml advanced mode`

### Task 1.6: Form steps — General + Sources

**Files:**
- Create: `ui/template/editor/steps/GeneralStep.java`
- Create: `ui/template/editor/steps/SourcesStep.java`
- Create: `ui/template/editor/binder/TemplateV2DraftBinder.java`

- [ ] **Step 1:** General: name, generator type, batchSize, executor pool fields bound to `GeneratorVO`.
- [ ] **Step 2:** Sources: add/remove named sources; type ComboBox → `query` / `iterator` subforms (`QuerySourceVO`, `IteratorSourceVO`); `dataSourceId` ComboBox populated from `/datasource/database/list` (runtime list until P2).
- [ ] **Step 3:** Commit: `feat(ui): v2 editor general and sources steps`

### Task 1.7: Form steps — Transform + Sinks

**Files:**
- Create: `ui/template/editor/steps/TransformStep.java`
- Create: `ui/template/editor/steps/SinksStep.java`

- [ ] **Step 1:** Transform: radio sql vs spel; SQL textarea; SpEL list editor for `SpelTransformVO` columns.
- [ ] **Step 2:** Sinks: repeatable; type = **CONSOLE | JDBC | KAFKA | ELASTICSEARCH** — bind to `ConsoleWriterVO`, `JdbcWriterVO`, `KafkaWriterVO`, `ElasticsearchWriterVO` (read writer modules for required fields: topic, bootstrapServers, index, hosts, table, dataSourceId, etc.).
- [ ] **Step 3:** Unit test (optional): binder round-trip for each sink type sample yaml fixtures under `src/test/resources/ui/`.
- [ ] **Step 4:** Commit: `feat(ui): v2 editor transform and multi-sink steps`

### Task 1.8: Form steps — Execution + Review

**Files:**
- Create: `ui/template/editor/steps/ExecutionStep.java`
- Create: `ui/template/editor/steps/ReviewStep.java`

- [ ] **Step 1:** Execution: `executionPolicy.mode`, chunk sizes, preview limits.
- [ ] **Step 2:** Review: Save (PUT editor), Validate (`POST /template/v2/validate`), Preview (`POST /template/v2/preview/{id}` — save first if unsaved), show validation errors / sample grid.
- [ ] **Step 3:** Run button disabled until P3 (or stub navigate with toast).
- [ ] **Step 4:** Commit: `feat(ui): v2 editor review with validate and preview`

### Task 1.9: P1 acceptance test

- [ ] **Step 1:** Manual script in `docs/operator-console-usage.md` § P1: create V2 template with JDBC sink + query source, validate, preview, archive, restore.
- [ ] **Step 2:** `.\mvnw-jdk25.ps1 -pl data-generator-service -am test` green.
- [ ] **Step 3:** Commit: `docs: operator console p1 usage`

---

## P2 — Datasource D1 persistence

### Task 2.1: datasource_config entity + repository

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/model/po/DataSourceConfigPO.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/repository/DataSourceConfigRepository.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/datasource/DataSourceConfigRepositoryTests.java`

- [ ] **Step 1:** Columns: `name` (PK), `url`, `username`, `password`, `driverClassName`, `driverJarPath`, `enabled`, `createdAt`, `updatedAt`.
- [ ] **Step 2:** Repository CRUD; unique name constraint.
- [ ] **Step 3:** Commit: `feat(datasource): add datasource_config persistence`

### Task 2.2: Bootstrap + service sync

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/datasource/DataSourceBootstrap.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/datasource/DataSourceConfigService.java`
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/controller/DataSourceController.java`

- [ ] **Step 1:** `@PostConstruct` or `ApplicationRunner` load enabled rows → `DynamicRoutingDataSource.addDataSource`.
- [ ] **Step 2:** `add` / `remove` / `testConnection` update DB then runtime (delegate existing driver upload logic).
- [ ] **Step 3:** `POST /datasource/database/test` — `DriverManager.getConnection` without persisting.
- [ ] **Step 4:** Tests: add config, restart context (or call bootstrap), list contains name.
- [ ] **Step 5:** Commit: `feat(datasource): bootstrap jdbc sources from database config`

### Task 2.3: Vaadin datasource views

**Files:**
- Modify: `ui/datasource/DataSourceListView.java`
- Create: `ui/datasource/DataSourceFormDialog.java`

- [ ] **Step 1:** Grid + Add/Edit dialog + Test connection + Remove.
- [ ] **Step 2:** P2 acceptance: add H2, restart app, source still listed and usable in template editor `dataSourceId` dropdown.
- [ ] **Step 3:** Commit: `feat(ui): datasource management views`

---

## P3 — Task execution history

### Task 3.1: task_execution entity + repository

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/model/po/TaskExecutionPO.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/repository/TaskExecutionRepository.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/task/TaskExecutionRepositoryTests.java`

- [ ] **Step 1:** Fields per spec (`instance_id` unique, status enum as string, metrics_json CLOB).
- [ ] **Step 2:** Query methods: `findByTemplateIdOrderByFinishedAtDesc`, `findByInstanceId`.
- [ ] **Step 3:** Commit: `feat(task): add task_execution persistence`

### Task 3.2: TaskExecutionService + REST

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/task/TaskExecutionService.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/controller/TaskExecutionController.java`
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/controller/TaskController.java`

- [ ] **Step 1:** `startExecution(templateId)` — insert QUEUED, submit async work, return instanceId.
- [ ] **Step 2:** `markRunning`, `markSuccess`, `markFailed` — used by callbacks.
- [ ] **Step 3:** `isRunning(templateId)` for archive guard.
- [ ] **Step 4:** REST: `POST /task/run/{templateId}`, `GET /task/executions`, `GET /task/executions/{instanceId}`.
- [ ] **Step 5:** Deprecate GET run endpoints or keep as aliases calling same service.
- [ ] **Step 6:** Tests: `@SpringBootTest` minimal template run → terminal SUCCESS row exists.
- [ ] **Step 7:** Commit: `feat(task): execution history service and api`

### Task 3.3: Pipeline hooks

**Files:**
- Modify: `data-generator-common/.../DefaultDataPipelineTask.java`
- Modify: `data-generator-service/.../TaskController.java` (V2 submit wrapper) or create `TaskExecutionRunner` facade in service module

- [ ] **Step 1:** Inject `TaskExecutionService` into V1 task wrapper (use `ObjectProvider` in service module factory bean if core cannot depend on service — prefer listener/callback interface in core + impl in service).
- [ ] **Step 2:** V2: wrap `templateV2Runner.run` with same lifecycle + metrics JSON from `TemplateV2RunResult`.
- [ ] **Step 3:** Re-run Task 3.2 tests with real pipeline.
- [ ] **Step 4:** Commit: `feat(task): record v1 and v2 execution lifecycle`

### Task 3.4: Vaadin job center

**Files:**
- Modify: `ui/job/JobListView.java`
- Create: `ui/job/JobDetailView.java`

- [ ] **Step 1:** Job list grid with filters; poll every 2s when any RUNNING.
- [ ] **Step 2:** Detail: status, timestamps, error, metrics pretty-print.
- [ ] **Step 3:** Template editor Review: Run → POST run → navigate to detail.
- [ ] **Step 4:** Commit: `feat(ui): job list and execution detail views`

---

## P4 — Migration tab

### Task 4.1: Migration panel in template editor

**Files:**
- Create: `ui/template/editor/MigrationPanel.java`
- Modify: `ui/template/TemplateEditorView.java`
- Modify: `ui/migration/MigrationDashboardView.java`

- [ ] **Step 1:** Panel buttons: Analyze, Draft (show in dialog), Compare, Sign-off, Promote — call existing `TemplateController` migration endpoints via `RestTemplate` or injected controller beans.
- [ ] **Step 2:** Disable promote when analysis `COMPATIBILITY_ONLY`; show link to `builtin-orchestration-census.md` text.
- [ ] **Step 3:** Dashboard: summary cards from `/migration/summary`, backlog grid from `/migration/backlog`.
- [ ] **Step 4:** Commit: `feat(ui): migration tab and dashboard`

### Task 4.2: Docs + retirement checkbox

**Files:**
- Create: `docs/operator-console-usage.md`
- Modify: `docs/migration/retirement-readiness.md`

- [ ] **Step 1:** Document routes, P1–P4 flows, archived templates, D1 datasources.
- [ ] **Step 2:** Mark P2 operator UI partial when P1+P3+P4 done (Vaadin migration + jobs).
- [ ] **Step 3:** Commit: `docs: operator console usage guide`

---

## Deferred (P5)

- W3 orchestration V2 spike — only if product revokes S1.
- Spring Security roles — P4+ in spec.
- Additional source types in form (json, csv, geo) — P1.5 increments.

---

## Suggested commit order (conventional)

1. `feat(ui): vaadin shell`
2. `feat(template): archived + editor service + rest`
3. `feat(ui): template editor wizard`
4. `feat(datasource): config table + bootstrap`
5. `feat(ui): datasource views`
6. `feat(task): execution history`
7. `feat(ui): job center`
8. `feat(ui): migration tab`
9. `docs: operator console usage`

Footer on each commit per `.cursor/rules/git-commit-conventional-ai.mdc`.
