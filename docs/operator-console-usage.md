# Operator console usage guide

Browser-based operator console for Template V2 authoring, JDBC datasource administration, task execution history, and V1→V2 migration. Implemented on `feature-4.0` (Vaadin **25.0.3** + Spring Boot 4).

**Design:** `docs/superpowers/specs/2026-05-23-operator-console-design.md`  
**Implementation plan:** `docs/superpowers/plans/2026-05-23-operator-console.md`

## Start the service

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am spring-boot:run
```

Default HTTP port: **9876** (`data-generator-service/src/main/resources/application.yaml`).

| Surface | URL |
|---------|-----|
| Vaadin UI | http://localhost:9876/ |
| REST (unchanged paths) | http://localhost:9876/template/…, `/task/…`, `/datasource/… |
| H2 console (dev) | http://localhost:9876/h2 |

The drawer shows **V1 execution: enabled/disabled** from `pci.data.generator.v1-execution.enabled` (default `true`).

## UI routes

| Area | Route | Purpose |
|------|-------|---------|
| Home | `/` | Links to primary areas |
| Templates | `/templates` | Catalog grid, archive/restore |
| Template editor | `/template/editor/new` or `/template/editor/{id}` | V2 wizard + YAML advanced |
| Datasources | `/datasources` | `datasource_config` CRUD + test |
| Jobs | `/jobs` | `task_execution` history |
| Job detail | `/jobs/{instanceId}` | Status, error, metrics JSON |
| Migration | `/migration` | Inventory summary + backlog |

REST APIs remain available for automation and CI; the UI calls the same Spring services.

---

## P1 — Template center

### List (`/templates`)

- Columns: id, name, archived flag.
- **New template** → editor at `/template/editor/new`.
- **Edit** → `/template/editor/{id}`.
- **Archive** / **Restore** — soft delete (`template.archived`, `archived_at`). Archived templates are hidden from `GET /task/list` by default.

### Editor (`/template/editor/...`)

**Model C:** structured forms (primary) + **YAML advanced** toggle.

| Tab | Binds to |
|-----|----------|
| General | `name`, generator settings |
| Sources | `query` / `iterator` sources; JDBC names from runtime datasource registry |
| Transform | `sql` / `spel` transform |
| Sinks | `CONSOLE`, `JDBC`, `KAFKA`, `ELASTICSEARCH` |
| Execution | `executionPolicy`, chunking |
| Review | Validate, Save |

**V1 templates:** wizard fields are read-only; V1 YAML shown read-only. Use the **Migration** tab to analyze, compare, and promote.

**YAML advanced:** Apply parses YAML into the draft; Sync from form dumps the current draft. On Apply, form steps refresh from the parsed draft.

### Template editor REST

Base path: `/template/v2/editor`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/create` | Create empty V2 draft + persist |
| GET | `/{templateId}` | Load editor payload (kind, draft, V1 yaml if legacy) |
| PUT | `/{templateId}` | Save draft (normalize + validate) |
| POST | `/{templateId}/archive` | Archive (fails if a run is QUEUED/RUNNING) |
| POST | `/{templateId}/restore` | Restore archived row |

V2 control plane (preview/validate) remains under `/template/v2/validate`, `/template/v2/preview/{id}`, etc.

### P1 acceptance (manual)

1. Create a V2 template with a query source + JDBC sink referencing a datasource from **Datasources**.
2. **Review → Validate** — expect no blocking errors.
3. Save; confirm row in `/templates`.
4. Archive; confirm hidden from list (without “Include archived”).
5. Restore.

---

## P2 — Datasource center (D1 persistence)

### UI (`/datasources`)

- **Persisted configs** grid: name, JDBC URL, driver, enabled.
- **Runtime keys** — union of Spring `application.yaml` dynamic datasources and persisted rows loaded at startup.
- **New / Edit** dialog: name, URL, user, password, driver class, optional driver JAR upload.
- **Test connection** — validates without saving bad config.
- **Remove** — drops runtime key and sets `datasource_config.enabled=false`.

Rows survive restart via `DataSourceBootstrap` (`@Order(100)`).

### Datasource REST

| Method | Path | Description |
|--------|------|-------------|
| GET | `/datasource/database/list` | Runtime datasource names |
| GET | `/datasource/database/configs` | Persisted summaries (no passwords) |
| POST | `/datasource/database/addDatasource` | Multipart save + register runtime |
| POST | `/datasource/database/remove/{name}` | Remove runtime + disable row |
| POST | `/datasource/database/test` | JSON body connection test |
| POST | `/datasource/database/test/{name}` | Test persisted row |

### P2 acceptance (manual)

1. Add an H2 in-memory datasource (driver `org.h2.Driver`).
2. Restart the app; confirm the source still appears under **Runtime keys** and in the editor Sources dropdown.
3. Use that name in a template `dataSourceId` / JDBC sink.

---

## P3 — Job center

### UI (`/jobs`)

- Grid: instance id, template name, kind (V1/V2), status, finished time.
- Optional filter by template id.
- **Detail** link → `/jobs/{instanceId}` (refresh for latest status).

Statuses: `QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELLED`.

### Run templates

| Method | Path | Description |
|--------|------|-------------|
| POST | `/task/run/{templateId}` | Preferred for UI/automation |
| GET | `/task/runById/{templateId}` | Legacy alias |
| GET | `/task/runByName/{name}` | By name (fails if ambiguous) |

Each run allocates a snowflake `instanceId`, inserts `task_execution`, and submits async work. V2 runs store `metrics_json` from `RunMetrics` when present.

### Execution history REST

| Method | Path | Description |
|--------|------|-------------|
| GET | `/task/executions?templateId=` | List history (optional filter) |
| GET | `/task/executions/{instanceId}` | Single row |

### P3 acceptance (manual)

1. Save a minimal V2 template with a console sink.
2. `POST /task/run/{templateId}` or trigger via REST client.
3. Open **Jobs** — row reaches `SUCCESS` after refresh.
4. Open detail — confirm timestamps; metrics or error text when applicable.
5. Archive guard: start a long run, attempt archive in editor — expect rejection while `QUEUED`/`RUNNING`.

---

## P4 — Migration

### Per-template tab (editor **Migration**)

Available when editing a saved template (`/template/editor/{id}`).

| Action | REST equivalent | Notes |
|--------|-----------------|-------|
| Analyze | `GET /template/migration/analyze/{id}` | Classification, path, blockers |
| Draft | `POST /template/migration/draft/{id}` | Dialog preview; **Apply to editor** |
| Compare | `POST /template/migration/compare/{id}` | Dual-run + markdown report path |
| Sign-off | `POST /template/migration/inventory/{inventoryId}/signoff` | Uses `db-{templateId}` |
| Promote | `POST /template/migration/promote/{id}` | Disabled for `COMPATIBILITY_ONLY` / `BLOCKED` |

**W3 policy S1:** templates classified `COMPATIBILITY_ONLY` (e.g. PAUSE/LOG orchestration) cannot promote from the UI. See `docs/migration/reports/builtin-orchestration-census.md` and `docs/migration/orchestration-retirement-boundary.md`.

After promote, the editor reloads as V2.

### Global dashboard (`/migration`)

- Summary cards from `GET /template/migration/summary`.
- Backlog grid with filters (`ALL`, `READY`, `BLOCKED`, `COMPATIBILITY_ONLY`, `NEEDS_COMPARE`, `PENDING_SIGNOFF`) — `GET /template/migration/backlog?filter=`.

Inventory file: `docs/migration/scenario-inventory.yaml`.

### P4 acceptance (manual)

1. Open a V1 `db-{id}` template (or refresh inventory: `POST /template/migration/inventory/refresh`).
2. **Migration** tab → Analyze → Compare.
3. Record sign-off when inventory row exists.
4. Promote when classification allows; confirm editor switches to V2 wizard.
5. On a COMPATIBILITY_ONLY row, confirm **Promote** stays disabled.

---

## Archived templates

- Archive is **soft delete** only (no physical row removal).
- `GET /task/list?includeArchived=false` (default) omits archived templates.
- Editor save/archive rejects archived rows until restored.
- Archive is blocked while `task_execution` has `QUEUED` or `RUNNING` for that template id.

## Retirement alignment

The console improves day-to-day ops; it does **not** replace M2 staging promote gates or P4 production cutover. See:

- `docs/migration/retirement-readiness.md` — checklist (P2 operator UI marked complete)
- `docs/migration/staging-readiness-checklist.md` — M2 staging
- `docs/migration/wave-freeze-schedule.md` — family sign-off dates
- `docs/superpowers/specs/2026-05-21-v1-retirement-deferred-ops-design.md` — M1 vs M2 boundaries

## Known gaps (follow-on)

- **Review → Run** button navigating to job detail (use REST `POST /task/run/{id}` today).
- Job list auto-poll every 2s while any `RUNNING` (manual Refresh today).
- Form source types: `json`, `csv`, `geojson`, etc. (P1.5).
- Spring Security / roles on Vaadin routes.
- W3 V2 orchestration spike (P5, deferred under S1).

## Verify after changes

```powershell
.\mvnw-jdk25.ps1 -pl data-generator-service -am test
```
