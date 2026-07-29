# Phase 15: Multi-JVM Worker E2E - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-29
**Phase:** 15-Multi-JVM Worker E2E
**Mode:** --auto
**Areas discussed:** Multi-JVM topology & shared DB, Primary proof artifact, Harness linkage, Enqueue trigger & template, SUCCESS evidence & scope boundary

---

## Multi-JVM topology & shared metadata DB

| Option | Description | Selected |
|--------|-------------|----------|
| Host two JVMs + shared file H2 (`distributed-staging` profiles) | Coordinator + worker OS processes sharing `jdbc:h2:file:…;AUTO_SERVER=TRUE` temp dir | ✓ |
| Podman dual-container only | Require `e2e-distributed-podman.ps1` as DIST-01 gate | |
| Single-JVM split-role IT only | Extend `DistributedSplitRoleIntegrationTests` without second process | |

**Auto selection:** Host two JVMs + shared file H2 (recommended default)
**Notes:** Aligns with `application-distributed-staging.yaml`, embedded-first preference, and STACK research. Closes the real process-boundary gap single-JVM ITs cannot prove.

---

## Primary proof artifact

| Option | Description | Selected |
|--------|-------------|----------|
| New `scripts/verify-multi-jvm-worker.ps1` | Runnable Windows recipe: spawn processes, trigger, poll SUCCESS | ✓ |
| Podman script as primary gate | Link harness to `e2e-distributed-podman.ps1` only | |
| Maven IT spawning processes | `@SpringBootTest` with ProcessBuilder only, no operator script | |

**Auto selection:** New focused PowerShell verify script as primary; Podman optional supplement
**Notes:** Reuses existing profile stack; avoids Podman-only CI friction while keeping Podman drill for operators.

---

## Harness linkage

| Option | Description | Selected |
|--------|-------------|----------|
| New P1 row `dist-multi-jvm-worker` | Linked to verify script; non-blocking per test-harness.md | ✓ |
| P0 promotion | Add multi-JVM to 15-row merge gate | |
| No matrix row | Script-only with no harness linkage | |

**Auto selection:** P1 matrix row (DIST-01 allows P1)
**Notes:** P0 gate must remain unchanged; Phase 17 may expand P1 tracking further.

---

## Enqueue trigger & template shape

| Option | Description | Selected |
|--------|-------------|----------|
| `POST /task/run/{templateId}` + published minimal V2 template | Iterator → SQL → console sink; real `TemplateV2Runner` | ✓ |
| Console `/api/templates/.../run` only | Console API path without TaskController spine | |
| REST enqueue smoke helper only | `Invoke-DistributedEnqueueSmoke` without full SUCCESS poll | |

**Auto selection:** TaskController `/task/run` + published minimal template
**Notes:** Extends Phase 12 HTTP execute spine across JVMs; matches staging deployment doc.

---

## SUCCESS evidence & scope boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Assert `distributed_job` + `task_execution` SUCCESS; keep AC-1..AC-7 out of DoD | One happy path only | ✓ |
| Full staging AC-1..AC-7 as DoD | Complete C2 staging closure checklist | |
| Playwright distributed specs required | UI E2E as gate | |

**Auto selection:** Dual status SUCCESS assertion; explicit non-goals for AC checklist, chaos, second worker, Playwright gate, P0 promotion
**Notes:** Matches ROADMAP success criteria #4 and research PITFALLS anti-pattern (over-scoping multi-JVM).

---

## Claude's Discretion

- Exact script filename, optional env-gated IT, poll mechanism, template seeding approach, matrix row status transition

## Deferred Ideas

- Full AC-1..AC-7 staging checklist, chaos drills, second worker, Playwright-as-gate, Podman-only proof, P0 promotion, RBAC-on distributed runs (Phase 16)
