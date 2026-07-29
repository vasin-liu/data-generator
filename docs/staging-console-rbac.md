# Staging — Console Header RBAC Enable Path

Enable and verify **opt-in header RBAC** for operator console `/api/**` endpoints (SEC-01). RBAC is **disabled by default** for local development; turn it on only via named Spring profiles or explicit configuration.

For general console usage (templates, jobs, datasources), see [operator-console-usage.md](operator-console-usage.md). This document owns the **enable recipe**, profile contract, and verify one-liners.

## Purpose

When `data.generator.console-security.enabled=true`, every `/api/**` request must carry a valid role header. Missing or invalid headers receive **403 Forbidden**. Legacy routes (`/task/**`, `/healthz`) are unaffected.

Default local/dev behavior keeps RBAC off so operators and CI can run without injecting headers.

## Property keys

| Property | Default (Java) | Description |
|----------|----------------|-------------|
| `data.generator.console-security.enabled` | `false` | Master switch; must be `true` to enforce RBAC |
| `data.generator.console-security.role-header` | `X-Console-Role` | Header carrying `ConsoleRole` name |
| `data.generator.console-security.actor-header` | `X-Console-Actor` | Optional audit actor label (not required for authorization) |

Source: `ConsoleSecurityProperties` (`data-generator-service`).

## Required headers

Send the role header on every `/api/**` call when RBAC is enabled. Optionally include the actor header for audit trails.

```bash
# VIEWER — read-only catalog and jobs
curl -H "X-Console-Role: VIEWER" http://localhost:8080/api/templates/scenarios

# ADMIN with optional audit actor
curl -H "X-Console-Role: ADMIN" -H "X-Console-Actor: ops-alice" \
  http://localhost:8080/api/templates/scenarios
```

Valid role values (case-insensitive): `VIEWER`, `EDITOR`, `OPERATOR`, `DATASOURCE_ADMIN`, `ADMIN`.

## Role → permission summary

Compact matrix sourced from `ConsoleRole` / `ConsolePermission`:

| Role | Permissions |
|------|-------------|
| **VIEWER** | `TEMPLATE_READ`, `JOB_READ`, `AUDIT_READ` |
| **EDITOR** | VIEWER + `TEMPLATE_EDIT` |
| **OPERATOR** | EDITOR + `TEMPLATE_RUN` |
| **DATASOURCE_ADMIN** | `DATASOURCE_ADMIN`, `TEMPLATE_READ`, `AUDIT_READ` |
| **ADMIN** | All permissions (includes `TEMPLATE_PUBLISH`, `JOB_CANCEL`, `SECRET_ADMIN`) |

Notes:

- **Publish** requires `ADMIN` (`TEMPLATE_PUBLISH`).
- **Draft run** requires `OPERATOR` or higher (`TEMPLATE_RUN`).
- **Datasource CRUD** requires `DATASOURCE_ADMIN` or `ADMIN`.

## Profile contract (D-09)

RBAC must remain **off** on base/dev and default E2E/distributed profiles. Only opt-in overlays enable it.

| Profile file | `spring.profiles.active` | `console-security.enabled` |
|--------------|--------------------------|----------------------------|
| Base `application.yaml` | (default) | `false` (Java default; no yaml block) |
| `application-e2e.yaml` | `e2e` | **false** |
| `application-e2e-distributed.yaml` | `e2e-distributed` | **false** |
| `application-distributed-staging.yaml` | `distributed-staging` | **false** |
| `application-staging.yaml` | `staging` | **true** (port **8080**) |
| `application-e2e-rbac.yaml` | `e2e-rbac` | **true** |

Do not enable RBAC on `distributed-staging` for SEC-01 — Phase 15 multi-JVM verify scripts stay header-free.

## Enable staging on host

Activate the staging overlay:

```bash
java -jar data-generator-service.jar --spring.profiles.active=staging
```

Packaged tarball layout: profile file at `conf/application-staging.yaml`. Staging listens on port **8080** and enables RBAC plus publish governance.

## Verify one-liners

Primary SEC-01 proof is the **Maven IT slice** (not the P0 merge gate):

```powershell
.\scripts\verify-rbac-enable.ps1 -SkipPlaywright
```

This runs:

- `ConsoleSecurityDefaultOffIT` — default-off regression (Pitfall 4)
- `ConsoleAuthorizationIntegrationIT` — deny/allow HTTP paths when enabled
- `ConsoleAuthorizationFilterTest`, `ConsoleUdfAuthorizationFilterTest` — filter unit tests

Use `-SkipPlaywright` for backend-only verification. Optional Podman Playwright (`rbac.console.spec.ts`, `rbac.ui.spec.ts`) is documented in plan **16-03**; omit `-SkipPlaywright` once that leg is wired.

**Not a P0 merge gate:** `verify-rbac-enable.ps1` is supplementary operator/UAT evidence. Merge blocking remains `.\scripts\verify-harness.ps1` (P0 matrix rows). P1 RBAC harness row is deferred to Phase 17 (TEST-09).

## E2E environment distinction (D-07)

Three related but distinct paths:

| Flag / profile | Meaning |
|----------------|---------|
| Default `verify-console.ps1` / `e2e` profile | RBAC **off** (`application-e2e.yaml`, `enabled: false`) |
| `DG_E2E_RBAC=true` | Dedicated RBAC Podman profile (`spring.profiles.active=e2e-rbac`); runs `rbac.console.spec.ts` + `rbac.ui.spec.ts` via `scripts/e2e-podman.ps1` |
| `DG_E2E_GOVERNANCE_STAGING=true` | Injects **ADMIN** headers in default E2E helpers (`e2e/helpers/api.ts` `defaultConsoleRole()`); **not** the same as full RBAC profile enforcement |

Do not conflate governance-staging header injection with the dedicated `e2e-rbac` profile.

## Non-goals / deferred

- **P1 matrix row** — Phase 17 / TEST-09 (`test-matrix.yaml` wiring)
- **Default-on production RBAC** — SEC-02 (product decision)
- **OAuth2 / JWT / Spring Security IdP** — out of milestone scope
- **Multi-JVM worker + RBAC combined proof** — optional future hardening (D-10)

## Related docs

- [operator-console-usage.md](operator-console-usage.md) — console features and configuration quick reference
- [staging-distributed-deployment.md](staging-distributed-deployment.md) — distributed coordinator/worker (RBAC stays off on `distributed-staging`)
