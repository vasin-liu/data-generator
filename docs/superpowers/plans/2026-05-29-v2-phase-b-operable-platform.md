# Phase B — Operable Platform Implementation Plan

**Goal:** Deliver governance, RBAC, task lifecycle, secrets, and audit per `docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md` Phase B.

**Entry:** Phase A′ merged to `master` (2026-05-29).

**Branch:** `feature-4.2`

## Epics → tasks

| Task | Epic | Deliverable |
|------|------|-------------|
| 1 | B1 | `secret_entry` table, `SecretService`, `passwordSecretRef` on inline JDBC + datasource registry |
| 2 | B1 | `TemplateV2Validator` rejects plaintext passwords when governance policy enabled |
| 3 | B2 | `TemplatePO.status` DRAFT/PUBLISHED/ARCHIVED, publish API with validate gate |
| 4 | B2 | Run gate: production `/task/run` requires PUBLISHED; editor draft run allowed |
| 5 | B3 | `ConsoleSecurityProperties` + `ConsoleAuthorizationFilter` (header role, disabled by default) |
| 6 | B4 | `cancel_requested`, cancel/retry/resume REST, `PAUSED` execution status |
| 7 | B4 | Workflow `PauseStepVO.manual` + `WorkflowPauseCoordinator` |
| 8 | B5 | `audit_event` table + `AuditService`; lineage columns on `task_execution` |
| 9 | — | Console status badge + publish action; RBAC/publish ITs |

**Phase B checkpoint:** Secrets resolve at runtime; publish blocks invalid templates; RBAC tests pass; cancel/retry/resume APIs work; audit rows on template/datasource/run events.

## File map

| Path | Action |
|------|--------|
| `db/schema.sql` | `secret_entry`, `audit_event`, template `status`, execution columns |
| `data-generator-core/.../secret/SecretResolver.java` | Create |
| `data-generator-core/.../InlineDataSourceVO.java` | Add `passwordSecretRef` |
| `data-generator-service/.../secret/SecretService.java` | Create |
| `data-generator-service/.../config/DefaultRuntimeJdbcEndpointResolver.java` | Resolve secrets |
| `data-generator-service/.../template/TemplateLifecycleService.java` | Publish/archive status |
| `data-generator-service/.../security/ConsoleAuthorizationFilter.java` | RBAC |
| `data-generator-service/.../audit/AuditService.java` | Append-only audit |
| `data-generator-service/.../task/TaskExecutionService.java` | Cancel/PAUSED/lineage |
| `data-generator-calcite/.../WorkflowPauseCoordinator.java` | Manual pause |
| `data-generator-console-web/` | Status + publish UI |
