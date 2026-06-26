# Phase 7: Datasource Governance & Hot-Reload - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-26
**Phase:** 7-Datasource Governance & Hot-Reload
**Areas discussed:** Run-start snapshot, Hot-reload, Governance policy, Connectivity test, Audit, UI bugs, DEGRADED UX, Inline migration, Playwright matrix, Distributed edge cases

---

## Run-Start Snapshot

| Option | Description | Selected |
|--------|-------------|----------|
| Params only | Freeze URL/username/secretRef/cluster params | ✓ |
| Resolved handles | Freeze DataSource/KafkaTemplate/ES Client instances | |
| CatalogEntry record | Freeze full CatalogEntry JSON | |
| Worker RUNNING timing | Snapshot when worker starts executing | ✓ |
| All three kinds | JDBC + Kafka + ES | ✓ |
| DB + memory | Persist on task_execution + in-memory until terminal | ✓ |

**User's choice:** Params only; Worker RUNNING; all kinds; DB permanent + memory until terminal; inline params included; BOOTSTRAP/MANAGED tag recorded; Worker writes snapshot; version token for race safety.

**Notes:** User requested pros/cons for snapshot content and TTL options before confirming.

---

## Hot-Reload

| Option | Description | Selected |
|--------|-------------|----------|
| On save | Immediate Catalog/runtime refresh | ✓ |
| Never touch in-flight | Isolation via execution snapshot | ✓ |
| Same for Kafka/ES | Consistent with JDBC | ✓ |
| Keep DB, mark DEGRADED | On reload failure | ✓ |

**User's choice:** Save-triggered reload; in-flight untouched; JDBC/Kafka/ES parity; DEGRADED with last-known-good for new runs.

---

## Governance Policy

| Option | Description | Selected |
|--------|-------------|----------|
| Prod managed required | No inline blocks when governance on | ✓ |
| Profile-based | staging/prod on, dev off | ✓ |
| Profile split BOOTSTRAP | dev/staging allow BOOTSTRAP; prod MANAGED only | ✓ |
| Draft warn / publish block | Warnings on draft; block publish/run | ✓ |
| Grandfather | Existing published inline templates exempt until changed | ✓ |

**User's choice:** Full managed policy in staging/prod with grandfathering for unchanged published templates.

**Notes:** User asked for pros/cons on inline vs managed and profile granularity before selecting.

---

## Connectivity Test

| Option | Description | Selected |
|--------|-------------|----------|
| Catalog.test() unified API | Single entry for all kinds | ✓ |
| Before save and publish | Dual gate | ✓ |
| All kinds Phase 7 | JDBC + Kafka + ES | ✓ |
| Playwright strict | New spec + playwright-cli | ✓ |

**User's choice:** Unified Catalog test; mandatory before save and template publish; all three kinds; strict Playwright/playwright-cli regression.

---

## Audit

| Option | Description | Selected |
|--------|-------------|----------|
| Full event set | CREATE/UPDATE/DELETE/RELOAD/DEGRADED/CONNECTIVITY_FAIL/GOVERNANCE_BLOCK | ✓ |
| Summary only | No secrets in feed | ✓ |
| Every reload | Audit each hot-reload | ✓ |
| Datasource filter | Audit page filter + link from Datasources | ✓ |

---

## Supplementary Areas

### UI Bug Scope
**User's choice:** Fix Datasources bugs **and** template run button in Phase 7 Playwright batch.

### DEGRADED UX
**User's choice:** Badge + detail view with failure reason and last good config reference.

### Playwright Matrix
**User's choice:** New `datasource-governance.spec.ts` with full CRUD/governance coverage + playwright-cli.

### Distributed Edge Cases
**User's choice:** Catalog version/updatedAt pinned at RUNNING. Worker path: **read execution snapshot JSON only** (recommended over live Catalog fallback).

---

## Claude's Discretion

Snapshot JSON schema, DEGRADED last-good storage mechanism, Kafka/ES test probe implementation, grandfather “material change” detection.

## Deferred Ideas

- Unified `connectionRef` template field
- Merged Connections console page
- Phase 10 harness P0 rows for DS paths (Phase 7 uses Playwright UAT scripts instead)
