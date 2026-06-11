# Quickstart

Get **data-generator** running locally and execute your first **Template V2** job in about 15 minutes.

**Languages:** [中文](../README.md) · [English](../README.en.md)

---

## What you will do

1. Build and start the Spring Boot service
2. Open the operator console
3. Create and publish a minimal V2 template
4. Run it and read the job report
5. (Optional) Trigger the same template via REST

---

## Prerequisites

| Requirement | Notes |
|-------------|-------|
| **JDK 25** | Set `JAVA_HOME` or use `mvnw-jdk25.ps1` on Windows |
| **Git clone** | This repository |
| **Network** | Internal Maven Nexus may require `.mvn/settings-jdk25.xml` |

Node.js is **not** required unless you develop the console UI with Vite hot reload.

---

## Step 1 — Build and start

From the repository root:

```powershell
# Verify toolchain
.\mvnw-jdk25.ps1 -v

# Package console frontend + service (required — service embeds console-dist)
.\mvnw-jdk25.ps1 -pl data-generator-console-web,data-generator-service -am -DskipTests package

# Start on port 9876
.\mvnw-jdk25.ps1 -pl data-generator-service spring-boot:run
```

On Linux/macOS with `JAVA_HOME` pointing at JDK 25:

```bash
./mvnw.cmd -s .mvn/settings-jdk25.xml -pl data-generator-console-web,data-generator-service -am -DskipTests package
./mvnw.cmd -s .mvn/settings-jdk25.xml -pl data-generator-service spring-boot:run
```

> **Why two modules?** `data-generator-service` copies `../data-generator-console-web/target/console-dist` into the JAR at build time. Packaging service alone without a prior console-web build fails.

Wait until the log shows the application listening on **9876**.

### Smoke-check endpoints

| Check | URL |
|-------|-----|
| Console home | http://localhost:9876/console/ |
| Runtime API | http://localhost:9876/api/console/runtime |
| Health (if exposed) | http://localhost:9876/actuator/health or project health endpoint |

---

## Step 2 — Open the console

1. Browse to **http://localhost:9876/console/**
2. Confirm the home page shows server capability flags (schedule / distributed — both off by default)
3. Use the sidebar: **Templates**, **Datasources**, **Jobs**, **Schedules**

The UI is available in **English** and **中文** (language switcher in the navbar).

For local UI development with hot reload, see [Operator console dev](../README.en.md#local-ui-dev-server) in the README.

---

## Step 3 — Create a template (console wizard)

### 3a. New template scaffold

1. Go to **Templates** → **New template**
2. **General** tab:
   - Name: `quickstart-synthetic`
   - Description: optional
3. **Sources** tab → add one source:
   - Source id: `seed`
   - Type: **Iterator** → **Number**
   - From `1`, To `5`, Step `1`
4. **Transform** tab:
   - Type: **SQL**
   - SQL:

   ```sql
   SELECT value AS id, value * 10 AS score FROM seed
   ```

5. **Sinks** tab:
   - Add writer type **Console**
6. **Execution** tab: leave defaults (suitable for this tiny demo)

### 3b. Validate, save, publish

1. Open the **Review** tab
2. Click **Validate** — fix any reported errors
3. Click **Save**
4. Click **Publish** — status must become `PUBLISHED`

> When `data.generator.governance.require-published-for-task-run=true` (default), only published templates can be started via `/task/run`. The console Review **Run** action can execute drafts during editing.

### 3c. Equivalent YAML

If you prefer YAML, the same template looks like this:

```yaml
name: quickstart-synthetic
sources:
  seed:
    type: iterator
    iterator:
      type: number
      from: 1
      to: 5
      step: 1
transform:
  type: sql
  sql: SELECT value AS id, value * 10 AS score FROM seed
sink:
  writers:
    - type: console
```

Paste into the editor **YAML** panel, then Validate → Save → Publish.

Reference samples ship under:

`data-generator-service/src/main/resources/template/v2-scenarios/scenario-a-synthetic.yaml`

---

## Step 4 — Run and inspect the job

### From the console

1. On **Review** or the template list, click **Run**
2. You are redirected to **Job detail** (`/console/jobs/{instanceId}`)
3. Wait for status **SUCCESS**
4. Expand **Run report** — you should see source/transform/sink metrics and console output counts

### From REST

List templates to find the numeric id:

```http
GET http://localhost:9876/task/list
```

Start a run (POST preferred):

```http
POST http://localhost:9876/task/run/{templateId}
```

Example with curl:

```bash
curl -X POST "http://localhost:9876/task/run/1"
```

Response includes `templateId` and `instanceId`. Open the job in the console:

`http://localhost:9876/console/jobs/{instanceId}`

---

## Step 5 — Optional next steps

### Register a JDBC datasource

Only needed when your template reads from or writes to a database.

1. **Datasources** → **Add**
2. Pick a **Common JDBC driver** preset (MySQL, PostgreSQL, etc.)
3. Fill host, database, username; reference a secret for the password in non-dev environments
4. In template **Sources** or **Sinks**, select the registered datasource id

Governance guide: [`template-v2-datasource-and-secret-governance.md`](template-v2-datasource-and-secret-governance.md)

### Enable cron schedules

1. Set in `application.yaml` or profile:

   ```yaml
   data:
     generator:
       schedule:
         enabled: true
   ```

2. Restart the service
3. **Schedules** → **New schedule** → choose a published template
4. Cron example (daily at 02:00): `0 0 2 * * *`

### Run bundled scenario templates

Scenario YAML files under `template/v2-scenarios/` are used in integration tests. Import via the console YAML editor or copy content into a new template.

| File | Demonstrates |
|------|----------------|
| `scenario-a-synthetic.yaml` | Number iterator + SQL + console |
| `scenario-b-lookup-join.yaml` | Query sources + SQL join |
| `scenario-c-csv-export.yaml` | CSV file sink |
| `scenario-d-chunked-jdbc.yaml` | Chunked JDBC read |
| `scenario-wf-pause-log.yaml` | L2 workflow steps |

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Maven cannot resolve artifacts | Missing internal settings | Use `-s .mvn/settings-jdk25.xml` or `mvnw-jdk25.ps1` |
| `Template must be PUBLISHED` | Governance flag | Publish the template, or use console draft Run |
| Port 9876 in use | Another process | Change `server.port` or stop the conflicting process |
| `console-dist does not exist` | Service packaged without frontend | `.\mvnw-jdk25.ps1 -pl data-generator-console-web,data-generator-service -am -DskipTests package` |
| Console 404 on `/console/` | Stale or missing embed | Same as above; or `verify-console-unit.ps1 -IncludeWebBuild` |
| AI-related test skips | No Ollama on localhost | Expected in CI/local without Ollama; unrelated to console quickstart |

---

## Where to go next

| Goal | Document |
|------|----------|
| Full console feature reference | [`operator-console-usage.md`](operator-console-usage.md) |
| Workflow & compute blocks | [`template-v2-workflow-authoring-guide.md`](template-v2-workflow-authoring-guide.md) |
| SQL / SpEL / JS transforms | [`template-v2-transformer-strategy.md`](template-v2-transformer-strategy.md) |
| Geospatial templates | [`geospatial-phase1-usage.md`](geospatial-phase1-usage.md) |
| Build & JDK notes | [`jdk25-upgrade.md`](jdk25-upgrade.md) |
| Contributor / module map | [`../AGENTS.md`](../AGENTS.md) |
