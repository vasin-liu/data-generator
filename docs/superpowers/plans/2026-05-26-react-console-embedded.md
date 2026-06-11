# React Console (Embedded) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Vaadin operator console with a React SPA in `data-generator-console-web`, served at `/console/*` from `data-generator-service`, backed by new `/api/*` Facade controllers.

**Architecture:** New Maven module builds static assets; `data-generator-service` copies `target/console-dist` into `classpath:/static/console/` at package time. Spring serves SPA with fallback; React calls `/api/*` only. Existing `*Service` classes keep business logic; Vaadin `ui/*` deleted at M6.

**Tech Stack:** Java 25, Spring Boot 4, Maven, React 19, Vite 6, TypeScript, React Router 7, Ant Design, TanStack Query, react-i18next; reuse `org.gensokyo.data.model.vo.R` for JSON responses.

**Spec:** `docs/superpowers/specs/2026-05-26-react-console-embedded-design.md`

---

## File map (create / modify / delete)

| Path | Action | Responsibility |
|------|--------|----------------|
| `data-generator-console-web/pom.xml` | Create | `packaging=pom`, frontend-maven-plugin |
| `data-generator-console-web/package.json` | Create | npm scripts, engines |
| `data-generator-console-web/vite.config.ts` | Create | `base: '/console/'`, proxy `/api` |
| `data-generator-console-web/src/**` | Create | React app |
| `pom.xml` (root) | Modify | Add module before `data-generator-service` |
| `data-generator-service/pom.xml` | Modify | Embed dist; remove Vaadin at M6 |
| `.../config/ConsoleWebConfig.java` | Create | SPA fallback, `/` redirect |
| `.../api/console/**` | Create | Facade controllers |
| `.../api/console/dto/**` | Create | UI-facing DTOs where needed |
| `data-generator-service/src/test/java/.../ConsoleStaticResourceIT.java` | Create | Assert `index.html` in jar |
| `data-generator-service/src/main/java/org/gensokyo/data/ui/**` | Delete (M6) | Vaadin views |
| `data-generator-service/src/main/java/org/gensokyo/data/AppShell.java` | Delete (M6) | Vaadin shell |
| `docs/operator-console-usage.md` | Modify (M6) | React URLs |

---

## Milestone M1 — Scaffold + embedded shell

### Task 1: Register Maven module `data-generator-console-web`

**Files:**
- Create: `data-generator-console-web/pom.xml`
- Modify: `pom.xml` (root) — add `<module>data-generator-console-web</module>` **before** `data-generator-service`

- [ ] **Step 1: Create `data-generator-console-web/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.gensokyo.data.generator</groupId>
        <artifactId>data-generator</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>data-generator-console-web</artifactId>
    <packaging>pom</packaging>
    <properties>
        <console.web.dir>${project.basedir}</console.web.dir>
        <node.version>v22.14.0</node.version>
        <npm.version>10.9.2</npm.version>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>com.github.eirslett</groupId>
                <artifactId>frontend-maven-plugin</artifactId>
                <version>1.15.1</version>
                <configuration>
                    <workingDirectory>${console.web.dir}</workingDirectory>
                    <installDirectory>${project.build.directory}/node</installDirectory>
                </configuration>
                <executions>
                    <execution>
                        <id>install-node-and-npm</id>
                        <goals><goal>install-node-and-npm</goal></goals>
                        <configuration>
                            <nodeVersion>${node.version}</nodeVersion>
                            <npmVersion>${npm.version}</npmVersion>
                        </configuration>
                    </execution>
                    <execution>
                        <id>npm-ci</id>
                        <goals><goal>npm</goal></goals>
                        <configuration><arguments>ci</arguments></configuration>
                    </execution>
                    <execution>
                        <id>npm-build</id>
                        <goals><goal>npm</goal></goals>
                        <phase>generate-resources</phase>
                        <configuration><arguments>run build</arguments></configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Add module to root `pom.xml`**

Insert after `data-generator-geo` (or before `data-generator-service`):

```xml
<module>data-generator-console-web</module>
```

- [ ] **Step 3: Verify reactor lists module**

Run: `.\mvnw-jdk25.ps1 -pl data-generator-console-web -N help:evaluate -Dexpression=project.artifactId -q -DforceStdout`  
Expected: `data-generator-console-web`

---

### Task 2: Vite + React minimal app

**Files:**
- Create: `data-generator-console-web/package.json`
- Create: `data-generator-console-web/vite.config.ts`
- Create: `data-generator-console-web/tsconfig.json`, `tsconfig.node.json`
- Create: `data-generator-console-web/index.html`
- Create: `data-generator-console-web/src/main.tsx`
- Create: `data-generator-console-web/src/app/App.tsx`
- Create: `data-generator-console-web/src/app/routes.tsx`
- Create: `data-generator-console-web/src/app/layout/ConsoleLayout.tsx`
- Create: `data-generator-console-web/src/app/pages/HomePage.tsx`

- [ ] **Step 1: Create `package.json`**

```json
{
  "name": "data-generator-console-web",
  "private": true,
  "type": "module",
  "engines": { "node": ">=22" },
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-router-dom": "^7.0.0"
  },
  "devDependencies": {
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "@vitejs/plugin-react": "^4.3.0",
    "typescript": "~5.7.0",
    "vite": "^6.0.0"
  }
}
```

- [ ] **Step 2: Create `vite.config.ts`**

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: '/console/',
  build: {
    outDir: 'target/console-dist',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:9876', changeOrigin: true },
    },
  },
});
```

- [ ] **Step 3: Create `src/main.tsx` + router with `basename="/console"`**

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './app/App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter basename="/console">
      <App />
    </BrowserRouter>
  </React.StrictMode>,
);
```

`App.tsx` renders `<Routes>`: `/` → `HomePage` with text "Data Generator Console".

- [ ] **Step 4: Build frontend**

Run: `cd data-generator-console-web && npm ci && npm run build`  
Expected: `data-generator-console-web/target/console-dist/index.html` exists.

---

### Task 3: Embed static assets into service JAR

**Files:**
- Modify: `data-generator-service/pom.xml`

- [ ] **Step 1: Add property and copy execution**

Inside `<properties>`:

```xml
<console.web.dist>${project.basedir}/../data-generator-console-web/target/console-dist</console.web.dist>
<skip.console.frontend>false</skip.console.frontend>
```

Before `spring-boot-maven-plugin`, add:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-antrun-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>copy-console-frontend</id>
            <phase>prepare-package</phase>
            <goals><goal>run</goal></goals>
            <configuration>
                <skip>${skip.console.frontend}</skip>
                <target>
                    <copy todir="${project.build.outputDirectory}/static/console" overwrite="true">
                        <fileset dir="${console.web.dist}" includes="**/*"/>
                    </copy>
                </target>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 2: Full package**

Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests package`  
Expected: BUILD SUCCESS; file exists:  
`data-generator-service/target/classes/static/console/index.html`

---

### Task 4: Spring SPA routing

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/config/ConsoleWebConfig.java`

- [ ] **Step 1: Implement config (copyright + Javadoc per project rules)**

```java
@Configuration
public class ConsoleWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/console/");
        registry.addRedirectViewController("/console", "/console/");
    }

    @Bean
    public RouterFunction<ServerResponse> consoleSpaRouter() {
        return RouterFunctions.route(
                GET("/console/{*path}"),
                req -> {
                    String p = req.path();
                    if (p.contains(".") && !p.endsWith("/")) {
                        return ServerResponse.notFound().build();
                    }
                    ClassPathResource index = new ClassPathResource("static/console/index.html");
                    return ServerResponse.ok().contentType(MediaType.TEXT_HTML)
                            .body(new InputStreamResource(index.getInputStream()));
                });
    }
}
```

**Note:** Prefer `ResourceResolver` pattern if team already has one; minimal version above is acceptable for M1. Static `*.js`/`*.css` are served by Spring Boot default resource handler from `/static/console/`.

- [ ] **Step 2: Manual smoke**

Run: `.\mvnw-jdk25.ps1 -pl data-generator-service spring-boot:run`  
Open: `http://localhost:9876/console/` → React home visible.

- [ ] **Step 3: Commit**

```bash
git add data-generator-console-web pom.xml data-generator-service/pom.xml \
  data-generator-service/src/main/java/org/gensokyo/data/config/ConsoleWebConfig.java
git commit -m "feat(console): scaffold react module and embed spa shell"
```

---

## Milestone M2 — `/api/*` Facade foundation

### Task 5: API package + reuse `R<T>`

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleApiAdvice.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleRuntimeController.java`

- [ ] **Step 1: `ConsoleRuntimeController`**

```java
@RestController
@RequestMapping("/api/console")
@RequiredArgsConstructor
public class ConsoleRuntimeController {
    private final DataGeneratorProperties properties;

    @GetMapping("/runtime")
    public R<ConsoleRuntimeDto> runtime() {
        return R.ok(new ConsoleRuntimeDto(properties.isV1ExecutionEnabled()));
    }
}
```

Record `ConsoleRuntimeDto(boolean v1ExecutionEnabled)` in `api/console/dto/`.

- [ ] **Step 2: `ConsoleApiAdvice`**

Map `IllegalArgumentException` → `R.fail(message)` with HTTP 400; log unexpected as 500.

- [ ] **Step 3: WebMvcTest**

Create: `data-generator-service/src/test/java/org/gensokyo/data/api/console/ConsoleRuntimeControllerTest.java`

```java
@WebMvcTest(ConsoleRuntimeController.class)
class ConsoleRuntimeControllerTest {
    @Autowired MockMvc mvc;
    @MockBean DataGeneratorProperties properties;

    @Test
    void runtime_returnsV1Flag() throws Exception {
        when(properties.isV1ExecutionEnabled()).thenReturn(true);
        mvc.perform(get("/api/console/runtime"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.v1ExecutionEnabled").value(true));
    }
}
```

Run: `.\mvnw-jdk25.ps1 -pl data-generator-service -Dtest=ConsoleRuntimeControllerTest test`

---

### Task 6: Jobs API (`/api/jobs`)

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleJobController.java`

- [ ] **Step 1: Delegate to `TaskExecutionService`**

```java
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class ConsoleJobController {
    private final TaskExecutionService taskExecutionService;

    @GetMapping
    public R<List<TaskExecutionSummary>> list(@RequestParam(required = false) Long templateId) {
        return R.ok(taskExecutionService.list(templateId));
    }

    @GetMapping("/{instanceId}")
    public R<TaskExecutionSummary> get(@PathVariable Long instanceId) {
        return R.ok(taskExecutionService.getByInstanceId(instanceId));
    }
}
```

- [ ] **Step 2: Test** — mirror `TaskExecutionController` behavior with MockMvc + mocked service.

---

### Task 7: Templates catalog API

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleTemplateController.java`
- Create: `data-generator-service/src/main/java/org/gensokyo/data/api/console/dto/TemplateSummaryDto.java`

Vaadin list uses `TemplateRepository` directly — expose:

```java
@GetMapping
public R<List<TemplateSummaryDto>> list(
    @RequestParam(defaultValue = "false") boolean includeArchived,
    @RequestParam(required = false) String q) {
    List<TemplatePO> rows = includeArchived
        ? templateRepository.findAll()
        : templateRepository.findByArchivedFalse();
    // filter by q on name/id (same logic as TemplateListView.matchesFilter)
    return R.ok(rows.stream().map(TemplateSummaryDto::from).toList());
}
```

- [ ] **Step 1: Implement DTO + controller**
- [ ] **Step 2: Repository test or WebMvcTest with `@DataJpaTest` slice optional; prefer WebMvcTest + mocked repo**

---

### Task 8: Template editor API (`/api/templates/{id}`)

**Files:**
- Create: `data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleTemplateEditorController.java`

Map spec endpoints to existing `TemplateEditorController` / `TemplateEditorService` / `TemplateV2ControlPlaneService` / `TemplateEditorRunSupport`:

| `/api/templates/...` | Existing |
|---------------------|----------|
| POST `/api/templates` | `createAndSave` |
| GET `/api/templates/{id}` | `loadForEditor` |
| PUT `/api/templates/{id}` | `save` |
| POST `.../archive` | `archive` |
| POST `.../restore` | `restore` |
| POST `.../validate` | `TemplateController` validate or control plane |
| POST `.../preview` | preview endpoint |
| POST `.../run` | `TemplateEditorRunSupport.runExisting` / saveAndRun |
| PUT `.../yaml` | parse YAML → draft (reuse `TemplateEditorYamlSupport`) |

- [ ] **Step 1: Implement controller methods one-by-one with WebMvcTest each**
- [ ] **Step 2: Run** `.\mvnw-jdk25.ps1 -pl data-generator-service test`

---

### Task 9: Datasources + Migration API

**Files:**
- Create: `ConsoleDataSourceController.java` — delegate to `DataSourceController` logic or `DataSourceAdminService` if extracted
- Create: `ConsoleMigrationController.java` — delegate to `MigrationConsoleService` + `TemplateController` migration endpoints

Follow paths in spec § API. Reuse `MigrationConsoleService` for analyze/draft/compare/signoff/promote/inventory.

- [ ] **Step 1: Datasources** — GET list, POST upsert, DELETE, POST test, POST driver-upload (multipart)
- [ ] **Step 2: Migration** — summary, backlog, per-template routes
- [ ] **Step 3: Integration smoke** — curl `/api/migration/summary` while app running

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(api): add console facade under /api for jobs templates migration"
```

---

## Milestone M3 — React: layout, API client, list pages

### Task 10: Shared frontend infrastructure

**Files:**
- Create: `data-generator-console-web/src/api/client.ts`
- Create: `data-generator-console-web/src/api/types.ts`
- Create: `data-generator-console-web/src/app/layout/ConsoleLayout.tsx` (sidebar nav)
- Modify: `package.json` — add `antd`, `@tanstack/react-query`, `react-i18next`

- [ ] **Step 1: API client**

```ts
export type ApiResult<T> = { code: number; message: string; success: boolean; data: T };

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(`/api${path}`);
  const body = (await res.json()) as ApiResult<T>;
  if (!body.success) throw new Error(body.message || 'API error');
  return body.data;
}
```

Align with `R<T>` JSON field names (`success`, `message`, `data`).

- [ ] **Step 2: ConsoleLayout** — nav links: `/`, `/templates`, `/datasources`, `/jobs`, `/migration`; top bar shows V1 flag from `/api/console/runtime`.

- [ ] **Step 3: Add i18n** — `src/i18n/en.json`, `zh-CN.json`; default `zh-CN`.

---

### Task 11: Templates page

**Files:**
- Create: `src/app/pages/TemplatesPage.tsx`
- Modify: `routes.tsx`

Features (parity `TemplateListView`):
- Grid: id, name, archived
- Filter by name/id; checkbox include archived
- Actions: New, Edit, Run (confirm), Archive/Restore (confirm)
- Calls: `GET /api/templates`, `POST /api/templates/{id}/archive`, etc.

- [ ] **Step 1: Implement page with Ant Design Table**
- [ ] **Step 2: Dev test** — `npm run dev` + backend running
- [ ] **Step 3: Commit** `feat(console-web): templates list page`

---

### Task 12: Jobs list + detail pages

**Files:**
- Create: `JobsPage.tsx`, `JobDetailPage.tsx`
- Create: `src/app/components/JobStatusBadge.tsx` (mirror CSS classes `dg-job-status-*` or Ant Design Tag colors)

- [ ] **Step 1: Jobs list** — poll every 2s when any row QUEUED/RUNNING (`useQuery` + `refetchInterval`)
- [ ] **Step 2: Job detail** — show status badge, timing, metrics pre block; link to `/console/templates/{id}`
- [ ] **Step 3: Commit**

---

## Milestone M4 — Template editor

### Task 13: Editor shell + tabs

**Files:**
- Create: `TemplateEditorPage.tsx`
- Create: `src/app/features/template/steps/*.tsx` (General, Sources, Transform, Sinks, Execution, Review, Migration)

- [ ] **Step 1: Load** `GET /api/templates/{id}` or `POST /api/templates` for new
- [ ] **Step 2: Tab state** — `useSearchParams` `?tab=`
- [ ] **Step 3: Save** — `PUT /api/templates/{id}`; **Save and return** navigates to `/console/templates`
- [ ] **Step 4: YAML panel** — toggle; CodeMirror; `PUT .../yaml` with confirm modal
- [ ] **Step 5: Review tab** — validate, preview dialog, run → navigate to job detail
- [ ] **Step 6: V1 read-only** — disable step fields when `kind === V1`; show read-only YAML block

---

### Task 14: Editor migration tab

- [ ] **Step 1: Wire analyze/draft/compare/signoff/promote to `/api/migration/templates/{id}/*`**
- [ ] **Step 2: Disable promote when COMPATIBILITY_ONLY (use API error + disable button from analyze classification)**

---

## Milestone M5 — Datasources + Migration dashboard

### Task 15: Datasources page

- [ ] **Step 1: Two sections** — persisted configs table + runtime keys list (same as Vaadin)
- [ ] **Step 2: Dialog** create/edit, test connection, driver upload (FormData)

### Task 16: Migration dashboard page

- [ ] **Step 1: KPI cards** from `GET /api/migration/summary`
- [ ] **Step 2: Backlog grid** with filter dropdown from `GET /api/migration/backlog?filter=`
- [ ] **Step 3: Open editor** column → `/console/templates/{id}?tab=migration`

---

## Milestone M6 — Remove Vaadin + docs + CI

### Task 17: Delete Vaadin

**Files:**
- Modify: `data-generator-service/pom.xml` — remove `vaadin-spring-boot-starter`, `vaadin-dev`, `vaadin-maven-plugin`
- Modify: `pom.xml` root — remove `${vaadin.version}` if unused
- Modify: `application.yaml` — remove `vaadin:` block
- Delete: `src/main/java/org/gensokyo/data/ui/**`, `AppShell.java`, `src/main/frontend/themes/**`, Vaadin generated frontend if not needed

- [ ] **Step 1: Remove dependencies and plugin**
- [ ] **Step 2: Delete Java UI package**
- [ ] **Step 3: Compile** `.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests package`
- [ ] **Step 4: Verify** `jar tf data-generator-service/target/*.jar | findstr static/console/index.html`

---

### Task 18: Static resource integration test

**Files:**
- Create: `data-generator-service/src/test/java/org/gensokyo/data/config/ConsoleStaticResourceIT.java`

```java
@SpringBootTest
class ConsoleStaticResourceIT {
    @Autowired ResourceLoader resources;

    @Test
    void indexHtmlBundled() {
        Resource r = resources.getResource("classpath:static/console/index.html");
        assertThat(r.exists()).isTrue();
    }
}
```

Run with `-am` package in CI.

---

### Task 19: Documentation

**Files:**
- Modify: `docs/operator-console-usage.md`
- Modify: `docs/superpowers/specs/2026-05-26-react-console-embedded-design.md` — Status → **Implemented**
- Modify: `AGENTS.md` — add console-web module + Node 22 for full package

- [ ] **Step 1: Replace Vaadin URLs with `/console/...`**
- [ ] **Step 2: Document dev workflow** (Vite + proxy) and production URL

---

### Task 20: Optional Playwright smoke

**Files:**
- Create: `data-generator-console-web/e2e/smoke.spec.ts` (or repo-root `e2e/`)

- [ ] **Step 1: Test** — open `/console/templates`, create navigation works when backend test profile up
- [ ] **Step 2: Wire CI** only if stable in headless (may skip in first merge)

- [ ] **Step 3: Final commit**

```bash
git commit -m "chore(console): remove vaadin and document react operator console"
```

---

## Plan self-review (vs spec)

| Spec requirement | Task |
|------------------|------|
| Module `data-generator-console-web` | Task 1–2 |
| Embed in service JAR | Task 3 |
| `/console/*` + SPA fallback | Task 4 |
| `/api/*` Facade | Tasks 5–9 |
| All 7 page areas | Tasks 11–16 |
| No auth | No task (default) |
| One-shot Vaadin removal | Task 17 |
| M1–M6 milestones | Tasks grouped by milestone |
| i18n zh/en | Task 10 |
| Jobs 2s poll | Task 12 |

**Placeholder scan:** None.

**Note:** JSON envelope uses existing `R<T>` (`success`, `message`, `data`) instead of spec’s `{ok, data}` — document in frontend `ApiResult` type.

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-26-react-console-embedded.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — fresh subagent per task (M1 Task 1, Task 2, …), review between tasks  
2. **Inline Execution** — implement in this session in milestone order with checkpoints after M1, M2, M3…

Which approach do you want?
