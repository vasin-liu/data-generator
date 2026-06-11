# React Operator Console (Embedded) — Design Spec

## Metadata

| Field | Value |
|-------|-------|
| Status | **Implemented** (M1–M6 on `feature-4.0`; Vaadin removed) — plan: `docs/superpowers/plans/2026-05-26-react-console-embedded.md` |
| Date | 2026-05-26 |
| Replaces | Vaadin-based operator console (`docs/superpowers/specs/2026-05-23-operator-console-design.md` — superseded at cutover) |
| Driver | Remove Vaadin; implement console as React SPA embedded in `data-generator-service`; unify UI API under `/api/*` |
| Branch | `feature-4.0` or follow-on `feature/console-react` |

## Decisions (locked)

| Topic | Choice |
|-------|--------|
| Scope | **B** — front-end rewrite **and** backend API adjustment for console |
| Page coverage | **C** — full replacement of all Vaadin views |
| Framework | **React** (Vite + React Router + TypeScript) |
| Module layout | **Option 2** — new Maven module `data-generator-console-web`, embed dist into service at package |
| Dev vs prod | **Dev separated** (Vite + proxy), **prod merged** into single service artifact |
| Auth | **None** — internal tool, direct access |
| UI base path | `/console/*` |
| API base path | `/api/*` |
| Cutover | **One-shot** — no long-term Vaadin coexistence |

## Problem statement

The operator console ships as **Vaadin 25** views inside `data-generator-service` (`org.gensokyo.data.ui.*`). This couples UI iteration to Java/Vaadin tooling, complicates theming and client-side UX, and adds `vaadin-maven-plugin` / Node prepare-frontend to every backend build.

Product requirements (template center, jobs, datasources, migration) are already met in Vaadin on `feature-4.0`, but the team wants:

1. A **mainstream SPA stack** (React) for UX velocity and hiring/tooling familiarity.
2. **Single deployable** — same Spring Boot process and port as today; static UI embedded in the service JAR / existing `tar.gz` assembly.
3. **Stable console API** under `/api/*` instead of ad hoc `/template`, `/task`, `/datasource` paths from the UI.

## Goals

1. **Full functional parity** with current Vaadin console (7 route areas + editor tabs).
2. **Embedded production delivery**: one `mvn package` produces backend + `static/console/**` inside the service JAR.
3. **Clear module boundary**: frontend sources live in `data-generator-console-web`; backend Facade in `data-generator-service`.
4. **Preserve domain logic**: reuse `TemplateEditorService`, `TaskExecutionService`, migration services, etc.; no duplicate business rules in React.

## Non-goals

- Login, RBAC, multi-tenant security
- Deprecating/removing legacy REST for external automation in the same release (mark deprecated; remove later)
- W3 V2 orchestration parity (policy **S1** unchanged — COMPATIBILITY_ONLY, no promote)
- Separate CDN/nginx-only frontend deployment (optional later; default is embedded)
- Visual V1 stage editor (V1 remains read-only + migration path until promoted)

## Target architecture

```text
┌─────────────────────────────────────────────────────────────┐
│  data-generator-service (Spring Boot 4, port e.g. 9876)   │
│  ┌─────────────────────┐   ┌──────────────────────────────┐ │
│  │ React SPA           │   │ REST /api/*                  │ │
│  │ GET /console/**     │   │ Console Facade controllers   │ │
│  │ → index.html fallback│   │ → existing *Service layer    │ │
│  └──────────▲──────────┘   └──────────────────────────────┘ │
│             │ static from classpath:/static/console/         │
└─────────────┼───────────────────────────────────────────────┘
              │
   data-generator-console-web (npm run build → dist/)
```

### Maven modules

| Module | Role | Output |
|--------|------|--------|
| `data-generator-console-web` | React + Vite + TS | `target/console-dist/` |
| `data-generator-service` | Boot app, `/api/*`, SPA config, assembly | JAR + `tar.gz` (unchanged layout) |

**Build dependency (build-time only):**

- Reactor builds `console-web` first.
- `data-generator-service` `package` copies `console-web/target/console-dist/**` → `target/classes/static/console/`.
- No runtime Maven dependency from service to console-web JAR.

Root `pom.xml` `<modules>` adds `data-generator-console-web`.

### URL routing

| Kind | Path | Behavior |
|------|------|----------|
| UI | `/console/**` | React Router (`basename=/console`); deep links → SPA fallback `index.html` |
| API | `/api/**` | JSON console API only (UI must not call legacy paths) |
| Redirect | `/` | 302 → `/console/` |
| Legacy REST | `/template`, `/task`, `/datasource`, … | Retained for scripts/CI; **deprecated** in docs |

**Spring configuration (`ConsoleWebConfig`):**

- Serve static assets from `classpath:/static/console/`.
- Non-file requests under `/console/**` → `index.html`.
- Do not apply SPA fallback to `/api/**`.

Vite `base: '/console/'` for correct asset URLs in production.

## Frontend module (`data-generator-console-web`)

### Suggested layout

```text
data-generator-console-web/
  package.json
  vite.config.ts
  tsconfig.json
  index.html
  src/
    main.tsx
    app/
      App.tsx
      routes.tsx
      layout/ConsoleLayout.tsx
      pages/
        HomePage.tsx
        TemplatesPage.tsx
        TemplateEditorPage.tsx
        DataSourcesPage.tsx
        JobsPage.tsx
        JobDetailPage.tsx
        MigrationDashboardPage.tsx
      features/          # template | job | datasource | migration
      api/               # fetch client, base /api
      i18n/              # zh-CN, en
      theme/             # light/dark via CSS variables + localStorage
```

### Stack

| Layer | Choice |
|-------|--------|
| Build | Vite 6 + TypeScript |
| Routing | React Router 7, `basename="/console"` |
| UI library | Ant Design (recommended for dense tables/forms) — confirm in implementation plan |
| Data | TanStack Query (polling for active jobs) |
| Forms | React Hook Form |
| YAML editor | CodeMirror 6 |
| i18n | react-i18next |

### Vaadin → React routes

| Vaadin | React |
|--------|-------|
| `/` | `/console/` |
| `/templates` | `/console/templates` |
| `/template/editor/new` | `/console/templates/new` |
| `/template/editor/{id}` | `/console/templates/:id` |
| `/datasources` | `/console/datasources` |
| `/jobs` | `/console/jobs` |
| `/jobs/{instanceId}` | `/console/jobs/:instanceId` |
| `/migration` | `/console/migration` |

Editor tabs: `?tab=general|sources|transform|sinks|execution|review|migration` (or nested routes).

### Development workflow

| Step | Command / URL |
|------|----------------|
| Backend | `.\mvnw-jdk25.ps1 -pl data-generator-service spring-boot:run` |
| Frontend | `cd data-generator-console-web && npm run dev` |
| Proxy | Vite: `/api` → `http://localhost:9876` |
| Dev UI | `http://localhost:5173/console/` |
| Integrated | `mvn -pl data-generator-service -am package` → `http://localhost:9876/console/` |

Env: `VITE_API_BASE=""` (relative `/api` in dev and prod).

## Backend: `/api/*` Facade

Package: `org.gensokyo.data.api.console` (name may be refined in implementation).

### Response contract

Success:

```json
{ "ok": true, "data": { } }
```

Error:

```json
{ "ok": false, "error": { "code": "VALIDATION_FAILED", "message": "..." } }
```

Use appropriate HTTP status codes. Dates: ISO-8601 strings.

### Endpoints (v1 — full console)

**Preferences / runtime**

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/console/preferences` | locale, darkMode |
| PUT | `/api/console/preferences` | optional server-side persistence |
| GET | `/api/console/runtime` | e.g. `v1ExecutionEnabled` (navbar banner) |

**Templates**

| Method | Path | Delegates to |
|--------|------|--------------|
| GET | `/api/templates` | list (+ archived filter) |
| POST | `/api/templates` | create |
| GET | `/api/templates/{id}` | editor payload |
| PUT | `/api/templates/{id}` | save draft |
| POST | `/api/templates/{id}/archive` | archive |
| POST | `/api/templates/{id}/restore` | restore |
| POST | `/api/templates/{id}/validate` | validate |
| POST | `/api/templates/{id}/preview` | preview |
| POST | `/api/templates/{id}/run` | start job → `instanceId` |
| PUT | `/api/templates/{id}/yaml` | apply YAML to draft |

**Jobs**

| Method | Path | Delegates to |
|--------|------|--------------|
| GET | `/api/jobs` | `TaskExecutionService.list` |
| GET | `/api/jobs/{instanceId}` | `getByInstanceId` |

**Datasources**

| Method | Path | Delegates to |
|--------|------|--------------|
| GET | `/api/datasources` | persisted + runtime keys |
| POST | `/api/datasources` | add/update |
| DELETE | `/api/datasources/{name}` | remove |
| POST | `/api/datasources/{name}/test` | test connection |
| POST | `/api/datasources/driver-upload` | multipart JAR |

**Migration**

| Method | Path | Delegates to |
|--------|------|--------------|
| GET | `/api/migration/summary` | summary KPIs |
| GET | `/api/migration/backlog` | backlog grid |
| GET | `/api/migration/templates/{id}/analyze` | analyze |
| POST | `/api/migration/templates/{id}/compare` | compare |
| POST | `/api/migration/templates/{id}/draft` | draft |
| POST | `/api/migration/templates/{id}/signoff` | sign-off |
| POST | `/api/migration/templates/{id}/promote` | promote (blocked for COMPATIBILITY_ONLY) |
| GET | `/api/migration/templates/{id}/inventory` | per-template inventory |

Legacy mapping reference: existing `/template/v2/editor/*`, `/template/migration/*`, `/task/executions*`, `/datasource/database/*`.

### Service module changes

1. Add Facade controllers + DTOs.
2. `@ControllerAdvice` for uniform errors.
3. Refactor thin legacy controllers to call shared services where duplicated.
4. **Remove after cutover:** `com.vaadin` dependencies, `vaadin-maven-plugin`, `AppShell`, `org.gensokyo.data.ui.*`, Vaadin `frontend/themes`, Vaadin i18n provider.

## Implementation milestones

| ID | Deliverable | Acceptance |
|----|-------------|------------|
| M1 | `data-generator-console-web` scaffold + Maven embed + SPA shell | `/console/` loads after `mvn package` |
| M2 | `/api/*` Facade + tests | Postman/IT covers main CRUD |
| M3 | Templates, Jobs, Job detail pages | Parity with Vaadin list/filter/poll |
| M4 | Template editor (6 tabs + YAML) | save, validate, preview, run |
| M5 | Datasources + Migration dashboard + editor migration tab | S1 promote disabled |
| M6 | Remove Vaadin; update `docs/operator-console-usage.md` | No `com.vaadin` on classpath |

Recommended: M5 demo-ready on feature branch; M6 as focused deletion PR.

## Testing

| Layer | Approach |
|-------|----------|
| Backend | `@WebMvcTest` / `@SpringBootTest` on Facade; keep existing service tests |
| Contract | Optional OpenAPI → `openapi-typescript` for frontend |
| Frontend | Vitest (forms); Playwright E2E (template save → job detail) |
| CI | Fail package if `npm run build` fails; assert `static/console/index.html` exists |
| Manual | zh/en, dark mode, YAML apply confirm, V1 read-only, migration compare |

## Risks and rollback

| Risk | Mitigation |
|------|------------|
| Slower CI (Node) | Cache `node_modules`; optional `-Dskip.console.frontend=true` for emergency backend-only builds |
| Breaking external REST consumers | Keep legacy paths; document deprecation |
| YAML corrupts draft | Confirm dialog on apply (same as today) |
| Cutover failure | Redeploy previous release artifact (tag before M6) |

Database: only additive schema (e.g. `archived` columns already present).

## Documentation updates at M6

- `docs/operator-console-usage.md` — React URLs, dev/prod commands, remove Vaadin notes
- Mark `2026-05-23-operator-console-design.md` as superseded
- AGENTS.md — mention `data-generator-console-web` module and Node requirement for full package

## Approval record

| Section | Approved |
|---------|----------|
| §1 Architecture & modules | 2026-05-26 |
| §2 Build & dev workflow | 2026-05-26 |
| §3 API, migration, testing | 2026-05-26 |

---

**Next step after spec approval:** invoke **writing-plans** skill for phased implementation tasks (M1–M6).
