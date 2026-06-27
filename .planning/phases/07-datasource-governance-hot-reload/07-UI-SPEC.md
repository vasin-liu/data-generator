# Phase 7 UI Design Contract

**Phase:** 07 — Datasource Governance & Hot-Reload  
**Generated:** 2026-06-27 (chain auto-path)  
**Status:** Ready for planning

## Surfaces

| Surface | Route / Entry | Primary actions |
|---------|---------------|-----------------|
| Datasources list | `/console/datasources` | List JDBC/Kafka/ES; HEALTHY/DEGRADED badge; filter by kind |
| Datasource detail / edit | Modal or drawer from list | Test connection, Save, view last reload + failure reason |
| Template editor run | Template detail / editor | Run button must remain valid after datasource changes |
| Audit feed | `/console/audit` | Datasource category filter; deep-link from Datasources |

## States & Badges

### Connection health (D-26)

- **HEALTHY** — green badge; last successful reload timestamp visible on row or detail.
- **DEGRADED** — warning badge; detail shows failure reason and note that last known good config is in use for new runs.
- Badge appears on catalog/datasource list rows for managed connections.

### Connectivity test (D-18–D-21)

- **Test** button enabled for new and existing JDBC/Kafka/ES entries before Save.
- Success: inline success message with actionable summary (e.g., "Connected to cluster X").
- Failure: inline error with server message; Save blocked when governance requires passing test.
- **Bug fix:** New JDBC test must work without pre-existing row; common driver preset must persist after save/create.

### Governance feedback (D-13–D-16)

- Draft save: warnings for inline connections or policy violations (non-blocking in dev).
- Publish / Run: blocking error toast or modal when managed-only or secret-ref rules fail.

## Flows

### Save datasource with test gate

1. Operator fills form → clicks **Test connection**.
2. Spinner on test button; result inline below form.
3. Save disabled until test passes (when profile requires) OR operator in dev profile with warnings only.
4. On save success: list refreshes; HEALTHY badge unless reload marks DEGRADED.

### DEGRADED detail

1. Row shows DEGRADED badge.
2. Detail panel: failure reason from API, `lastReloadAt`, link text "Using last known good configuration for new runs".

### Audit deep-link (D-25)

1. From Datasources page: "View audit history" control per connection or global link.
2. Navigates to Audit with `category=DATASOURCE` (or equivalent filter) pre-applied.

### Template run regression (D-21)

- Run button on template detail must trigger run API and show job feedback; must not be disabled/invalid after datasource CRUD flows in same session.

## API expectations (console client)

- `GET /api/datasources` (or catalog list): include `healthStatus`, `lastReloadAt`, `degradedReason` fields.
- `POST .../test` unified for JDBC/Kafka/ES before save.
- Governance errors: HTTP 400 with `R.fail(message)` body consumed by toast.

## E2E coverage (`datasource-governance.spec.ts`, D-27–D-28)

- CRUD all three kinds with connectivity test gate.
- Save → reload → new run uses updated config; in-flight run unchanged (backend assertion via API helpers).
- DEGRADED badge visible after forced reload failure fixture.
- Governance block on publish in staging profile fixture.
- Audit entries visible after create/update/reload.
- playwright-cli snapshots for list and DEGRADED detail states.

## Out of scope (UI)

- Merged single Connections page (deferred).
- Unified template `connectionRef` field editor (deferred).
