# Operator console usage guide

Browser-based operator console for Template V2 authoring, JDBC datasource administration, task execution history, and V1→V2 migration. On `feature-4.0` the UI is a **React SPA** (`data-generator-console-web`) embedded in `data-generator-service` at `/console/*`. Console HTTP APIs live under `/api/*`; legacy REST paths (`/template/…`, `/task/…`, `/datasource/…`) remain for automation.

**UI stack:** React 19 + Vite + Ant Design + React Router. i18n: `data-generator-console-web/src/i18n/` (English + 中文). Theme/locale toggles are in the console navbar (client-side).

**Design:** `docs/superpowers/specs/2026-05-26-react-console-embedded-design.md`  
**Legacy Vaadin spec (superseded):** `docs/superpowers/specs/2026-05-23-operator-console-design.md`

## Start the service (production-style)

```powershell
# Build console assets + service JAR (Node 22+ required for npm in console-web module)
.\mvnw-jdk25.ps1 -pl data-generator-service -am -DskipTests package

# Run the service
.\mvnw-jdk25.ps1 -pl data-generator-service spring-boot:run
```

From the IDE: run `DataGeneratorApplication` after a Maven package so `classpath:static/console/index.html` exists.

Default HTTP port: **9876** (`data-generator-service/src/main/resources/application.yaml`).

| Surface | URL |
|---------|-----|
| React console | http://localhost:9876/console/ |
| Console API | http://localhost:9876/api/… |
| Legacy REST | http://localhost:9876/template/…, `/task/…`, `/datasource/… |
| H2 console (dev) | http://localhost:9876/h2 |

The layout shows **V1 execution: enabled/disabled** from `data.generator.v1-execution.enabled` (default `true`), via `GET /api/console/runtime`.

**H2 file DB (`jdbc:h2:file:../db/data-generator`):** On first start after upgrading, `db/schema.sql` runs `ALTER TABLE … ADD COLUMN IF NOT EXISTS` for `template.archived` / `archived_at`. If you still see “Column ARCHIVED not found”, stop the app and delete `db/data-generator.mv.db` (and `.trace.db` if present) to recreate from schema, or run the two `ALTER TABLE` lines in the H2 console.

## Local UI development (Vite)

Run the Spring Boot backend on port **9876**, then in `data-generator-console-web`:

```powershell
cd data-generator-console-web
npm install
npm run dev
```

Open http://localhost:5173/console/ — Vite proxies `/api` to `http://localhost:9876`. Production builds use `npm run build` (invoked automatically by Maven during `package`).

## UI routes

| Area | Route | Purpose |
|------|-------|---------|
| Home | `/console/` | Links to primary areas |
| Templates | `/console/templates` | Catalog grid, archive/restore |
| Template editor | `/console/templates/new` or `/console/templates/{id}` | V2 wizard + YAML + migration tab |
| Datasources | `/console/datasources` | `datasource_config` CRUD + test |
| Jobs | `/console/jobs` | `task_execution` history |
| Job detail | `/console/jobs/{instanceId}` | Status, error, metrics JSON |
| Migration | `/console/migration` | Inventory summary + backlog |

The React app calls `/api/*` facades; those delegate to the same Spring services as the legacy REST controllers.

---

## P1 — Template center

### List (`/console/templates`)

- Columns: id, name, archived flag.
- **New template** → `/console/templates/new`.
- **Edit** → `/console/templates/{id}`.
- **Run** — starts async execution (archived rows disabled); opens job detail.
- **Archive** / **Restore** — soft delete (`template.archived`, `archived_at`). Archived templates are hidden from task list by default.

### Editor (`/console/templates/...`)

**Model C:** structured forms (primary) + **YAML advanced** panel.

| Tab | Binds to |
|-----|----------|
| General | `name`, generator settings |
| Sources | `query` / `iterator` sources; JDBC names from runtime datasource registry |
| Transform | `sql` / `spel` transform |
| Sinks | `CONSOLE`, `JDBC`, `KAFKA`, `ELASTICSEARCH` |
| Execution | `executionPolicy`, chunking |
| Review | Validate, **Preview**, Save, **Run** |
| Migration | Analyze, compare, sign-off, promote (saved templates only) |

**V1 templates:** wizard fields read-only; V1 YAML shown read-only. Use **Migration** to analyze, compare, and promote.

### Console API (templates)

Under `/api/console/templates` and `/api/console/templates/{id}/editor/…` — see `ConsoleTemplateController` and `ConsoleTemplateEditorActionsController`. Legacy editor REST remains at `/template/v2/editor/…` for compatibility.

### P1 acceptance (manual)

1. Create a V2 template with a query source + JDBC sink referencing a datasource from **Datasources**.
2. **Review → Validate** — expect no blocking errors.
3. Save; confirm row in `/console/templates`.
4. Archive; confirm hidden from list (without “Include archived”).
5. Restore.

---

## P2 — Datasource center

### UI (`/console/datasources`)

- Persisted configs grid: name, JDBC URL, driver, enabled.
- Runtime keys — union of Spring `application.yaml` dynamic datasources and persisted rows.
- New / Edit dialog: name, URL, user, password, driver class, optional driver JAR upload.
- **Test connection** — validates without saving bad config.
- **Remove** — drops runtime key and disables persisted row.

Console API: `/api/datasources`. Legacy: `/datasource/database/…`.

### P2 acceptance (manual)

1. Add an H2 in-memory datasource (driver `org.h2.Driver`).
2. Restart the app; confirm the source still appears under runtime keys and in the editor Sources dropdown.
3. Use that name in a template `dataSourceId` / JDBC sink.

---

## P3 — Job center

### UI (`/console/jobs`)

- Grid: instance id, template name, kind (V1/V2), status, finished time.
- Optional filter by template id.
- Polling while any visible row is `QUEUED` or `RUNNING`.
- **Detail** → `/console/jobs/{instanceId}`.

Console API: `/api/console/jobs`. Legacy run: `POST /task/run/{templateId}`.

Statuses: `QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELLED`.

### P3 acceptance (manual)

1. Save a minimal V2 template with a console sink.
2. Run from the UI or `POST /task/run/{templateId}`.
3. Open **Jobs** — row reaches `SUCCESS`.
4. Open detail — confirm timestamps; metrics or error text when applicable.
5. Archive guard: start a long run, attempt archive in editor — expect rejection while `QUEUED`/`RUNNING`.

### Cron schedules (Phase B schedule hook)

REST API: `/api/console/schedules` (list, create, update, delete). Each row stores a Spring six-field `cronExpression` and `templateId` (published templates only at trigger time).

Enable the poller on the service node:

```yaml
data:
  generator:
    schedule:
      enabled: true
      poll-delay-ms: 60000
```

The poller calls the same path as `POST /task/run/{templateId}` (skips when the template already has a `QUEUED`/`RUNNING` execution). Distributed enqueue applies when `data.generator.distributed.enabled=true`.

### UI (`/console/schedules`)

- List, create, edit, and delete schedules (published templates only in the picker).
- Filter by template id; table shows next/last trigger times.
- Sidebar **Schedules** / **定时任务** (route `/console/schedules`).

---

## P4 — Migration

### Per-template tab (editor **Migration**)

Available when editing a saved template.

| Action | Console API | Notes |
|--------|-------------|-------|
| Analyze | `GET /api/migration/analyze/{id}` | Classification, path, blockers |
| Draft | `POST /api/migration/draft/{id}` | Preview; apply to editor |
| Compare | `POST /api/migration/compare/{id}` | Dual-run + report path |
| Sign-off | `POST /api/migration/inventory/{inventoryId}/signoff` | Uses `db-{templateId}` |
| Promote | `POST /api/migration/promote/{id}` | Disabled for `COMPATIBILITY_ONLY` / `BLOCKED` |

**W3 policy S1:** templates classified `COMPATIBILITY_ONLY` cannot promote from the UI. See `docs/migration/reports/builtin-orchestration-census.md` and `docs/migration/orchestration-retirement-boundary.md`.

### Global dashboard (`/console/migration`)

- Summary cards from `GET /api/migration/summary`.
- Backlog grid with filters — `GET /api/migration/backlog?filter=`.

Inventory file: `docs/migration/scenario-inventory.yaml`.

### P4 acceptance (manual)

1. Open a V1 `db-{id}` template (or refresh inventory).
2. **Migration** tab → Analyze → Compare.
3. Record sign-off when inventory row exists.
4. Promote when classification allows; confirm editor switches to V2 wizard.
5. On a COMPATIBILITY_ONLY row, confirm **Promote** stays disabled.

---

## Archived templates

- Archive is **soft delete** only (no physical row removal).
- Task list omits archived templates by default.
- Editor save/archive rejects archived rows until restored.
- Archive is blocked while `task_execution` has `QUEUED` or `RUNNING` for that template id.

## Retirement alignment

The console improves day-to-day ops; it does **not** replace M2 staging promote gates or P4 production cutover. See:

- `docs/migration/retirement-readiness.md`
- `docs/migration/staging-readiness-checklist.md`
- `docs/migration/wave-freeze-schedule.md`
- `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md`

## Known gaps (follow-on)

- Form source types: `json`, `csv`, `geojson`, etc. (P1.5).
- Spring Security / roles on console routes.
- W3 V2 orchestration spike (P5, deferred under S1).

Preview requires **IN_MEMORY** execution mode (CHUNKED/STREAMING templates fail with a clear error).

## Verify after changes

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am test
```

Confirm the console bundle is on the classpath:

```powershell
jar tf data-generator-service\target\data-generator-service-*.jar | findstr static/console/index.html
```
