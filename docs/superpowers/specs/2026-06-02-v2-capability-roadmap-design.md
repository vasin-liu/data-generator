# V2 Capability Roadmap — Product Progress & Next Phase Design

## Metadata

| Field | Value |
|-------|-------|
| Status | **Approved** (2026-06-02) |
| Driver | Product priority: **C** V2 capability completion → **B** operable console → **A** C2 distributed last |
| V1 policy | **Hard cut** — no compatibility layer, no migration product path |
| Parent specs | `docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md`, `docs/template-v2-product-roadmap.md` |
| Supersedes narrative | V1 migration / dual-run / COMPATIBILITY_ONLY as active product work |

## Problem statement

The repository has delivered a strong **linear V2** execution path, a **React operator console**, and **partial Phase A′** assets (workflow runner, transform DAG, JsTransform validator, scenario ITs, minimal workflow/DAG console editors). Recent engineering focus shifted to console UX and E2E verification.

Product direction requires **V2-only** with full orchestration capabilities (Workflow, L1 DAG, materialization policies, JS transforms) as the primary value proposition. **V1 templates and migration must exit the product path.** Distributed multi-node execution (C2) remains important but **after** A′ productization and B-lite operability.

## Goal

Deliver a phased roadmap where:

1. **C (A′ productization)** — Business users can author, publish, run, and observe four official scenario families entirely in the console without YAML or V1.
2. **B (B-lite)** — Publish governance, audit visibility, and header-based RBAC are enabled for staging/production without blocking C waves.
3. **A (C2)** — Deferred until C Done + B-lite Done; no staging closure work in the current cycle.
4. **V1** — Removed from APIs, console, CI, and operator documentation.

## Non-goals (this cycle)

| Item | Rationale |
|------|-----------|
| V1 execution, migration UI/API, dual-run | User decision: hard cut |
| Phase D template-level pipeline DAG | Far-term per approved V2-only design |
| C2 Coordinator/Worker staging closure | Priority A last |
| LDAP/OAuth/SSO, multi-tenant IAM | Out of B-lite scope |
| Full enterprise approval workflows | B-lite stops at RBAC + audit |

---

## Strategic direction

Template V2 is the **only** template model. Product positioning shifts from "linear SQL pipeline runs" to:

> **Orchestrated data generation and shaping** — Workflow + Transform DAG + materialization policies + observable runs.

The console is a **V2 authoring and operations surface**, not a migration workbench.

### V1 hard-cut policy

| Area | Decision |
|------|----------|
| V1 execution | Permanently off; not documented as an operator feature |
| V1 templates in API/list/editor | Reject with explicit error; do not show read-only |
| Migration UI/API/docs | Remove or move to `docs/archive/` |
| CI | Drop V1/migration tests; gate on `template/v2-scenarios/**` + `verify-console.ps1` |
| Legacy flags | Dev-only if retained; no product mention |

---

## Current progress snapshot (2026-06-02)

| Layer | Progress | Notes |
|-------|----------|-------|
| Platform (JDK 25, Boot 4) | ~100% | Stable |
| Linear V2 execution | ~80% | CHUNKED/STREAMING, multi-source/sink, RunReport |
| A′ engine (WF/DAG/JS) | ~60% | Validator + runner + scenario ITs exist |
| A′ console authoring | ~40% | WorkflowPanel, TransformDagEditor exist; not product-grade |
| B governance backend | ~50% | Lifecycle, AuditService, ConsoleRole exist; RBAC default off |
| B console | ~25% | Publish on Review; no audit page; no RBAC UX |
| Console E2E/CI | ~90% | 28 Playwright tests, fixed verify pipeline |
| C2 distributed | ~40% | Code foundation; staging not accepted |
| V1/migration | To remove | Wave 0 |

---

## Recommended approach

**Scenario-driven A′ productization (Wave 0–4)** — build on existing engine and console skeletons; each wave ships engine hardening + console UX + scenario YAML + tests. **B-lite** interleaves from Wave 1 without blocking C.

Alternatives rejected:

- **Engine-first, console-later** — conflicts with B-second priority.
- **50/50 parallel C+B teams** — context-switch cost unless staffing ≥3 FTE.

---

## Phase A′ delivery waves

### Wave 0 — V1 amputation (~2 weeks)

**Deliverables:**

- Remove migration page/tab, `VITE_ENABLE_MIGRATION`, public migration APIs
- V1 template kind: API/editor returns error; excluded from catalog
- Delete or archive migration/V1 ITs; update `operator-console-usage.md`
- Move migration runbooks to `docs/archive/`

**Acceptance:** New operators cannot discover or use V1/migration paths.

### Wave 1 — MaterializationPolicy (~3 weeks)

**Deliverables:**

- Replace free-text materialization hint in `SourcesStep` with structured `MaterializationPolicyVO` fields
- Validator diagnostics surfaced on save/publish
- Extend `scenario-a-synthetic.yaml`; FieldHelp + i18n
- Playwright: source policy form → save → catalog

**Acceptance:** Synthetic data scenario configurable in console without YAML edits.

### Wave 2 — L1 Transform DAG (~4 weeks)

**Deliverables:**

- Harden `TransformDagEditor`: node types, edges, cycle hints, linear/DAG toggle
- Job detail: transform node metrics in RunReport
- Staged preview by DAG node where API supports it
- Extend `scenario-dag-join.yaml`; add fan-out scenario
- Playwright: `transform-dag.spec.ts`

**Acceptance:** Two-source join template authored, published, run, diagnosed per transform node.

### Wave 3 — L2 Workflow (~4 weeks)

**Deliverables:**

- WorkflowPanel wizard UX (step types, params, compute block binding)
- Jobs: `PAUSED` status, pause reason, resume/cancel
- Shared scope documentation + UI hints
- New shared-state scenario YAML
- Playwright: minimal LOG → ComputeBlock workflow

**Acceptance:** Pause workflow visible and controllable from Job detail.

### Wave 4 — JsTransform (~2 weeks)

**Deliverables:**

- JS transform step in ComputeBlock editor (script, limits display)
- Sandbox operations doc
- Keep `scenario-js-transform.yaml` as CI gate

**Acceptance:** JS template created in console; scenario IT passes.

### Official scenario catalog

| Scenario | Wave | Console "create from scenario" |
|----------|------|--------------------------------|
| A Synthetic | 1 | Sprint 2+ |
| B Multi-source join | 2 | Sprint 4 |
| C/D/E File/chunk/stream | Done | Link from docs |
| WF pause/branch | 3 | Sprint 6 |
| JS transform | 4 | Sprint 7 |

**C Done criteria:** Categories A, B, WF, JS — full console loop: author → publish → run → RunReport.

**Estimated calendar:** 16–18 weeks (1 FE + 1–2 BE).

---

## Phase B-lite scope

### Principles

- Do not block C waves (≤1 sprint per B item)
- RBAC via existing `X-Console-Actor` / `X-Console-Role` headers
- Audit before fine-grained permission expansion
- Default open in dev; staging/prod documented to enable `console-security.enabled=true`

### Role matrix (extend existing `ConsoleRole`)

| Role | Read | Edit | Run | Publish | Datasource admin | Audit read |
|------|------|------|-----|---------|------------------|------------|
| VIEWER | ✅ | | | | | ✅ |
| EDITOR | ✅ | ✅ | | | | ✅ |
| OPERATOR | ✅ | ✅ | ✅ | | | ✅ |
| DATASOURCE_ADMIN | ✅ | | | | ✅ | ✅ |
| ADMIN | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**Publish rule:** Only **ADMIN** may publish/archive; EDITOR saves DRAFT; OPERATOR runs **PUBLISHED** templates only.

### B work packages

| ID | Timing | Deliverables | Acceptance |
|----|--------|--------------|------------|
| B1 Publish | Wave 1 | `requirePublished=true` in prod; DRAFT blocked on schedule; draft run labeled on Review | DRAFT cannot schedule |
| B2 Audit UI | Wave 2 | `GET /api/console/audit`; console audit page; no secrets in detail | Publish events searchable |
| B3 RBAC | Wave 3 | Enforce on mutating APIs; staging sample config; 403 UX; role ITs | VIEWER cannot save |
| B4 Job lifecycle | Wave 3 | PAUSED/Resume/Cancel in Job detail tied to workflow | Pause controllable in UI |

**B-lite Done:** Publish gate + audit UI + staging RBAC + workflow job states.

---

## Testing strategy

### Fixed pipelines (already in place)

| Layer | Command | Trigger |
|-------|---------|---------|
| Java unit (console slice) | `scripts/verify-console-unit.ps1` | Console/service changes |
| Frontend build | `npm run build` in console-web | Console changes |
| UI/E2E | `scripts/verify-console.ps1` | Console changes |
| CI | `.github/workflows/console-verify.yml` | Path filters on push/PR |

### Per-wave test additions

| Wave | Backend | Frontend |
|------|---------|------------|
| 0 | Remove V1/migration tests; ensure v2-scenarios IT green | Remove migration E2E; update acceptance spec |
| 1 | MaterializationPolicy validator tests | Playwright source policy |
| 2 | DAG scenario IT + RunReport assertions | `transform-dag.spec.ts` |
| 3 | Workflow pause/resume IT | Job PAUSED E2E |
| 4 | JsTransform sandbox IT | JS step save E2E |
| B1–B3 | Role enforcement IT, audit API tests | Audit page smoke; 403 cases optional |

### Regression source of truth

- `data-generator-service/src/main/resources/template/v2-scenarios/*.yaml`
- `V2WorkflowScenarioIT` + existing linear scenario ITs
- Playwright specs under `data-generator-console-web/e2e/specs/`

After Wave 0: **no CI job may depend on V1 templates or migration APIs.**

---

## Milestone acceptance summary

| Milestone | Date (relative) | Gate |
|-----------|-----------------|------|
| M0 V1 cut | Week 2 | No migration/V1 product paths |
| M1 MatPol | Week 5 | Scenario A in console |
| M2 DAG | Week 9 | Scenario B + node RunReport |
| M3 Workflow | Week 13 | Pause/resume in console |
| M4 JS | Week 15 | Scenario JS in console |
| **C Done** | Week 16–18 | A/B/WF/JS official scenarios E2E |
| **B-lite Done** | Week 13–16 | Publish + audit + RBAC staging |
| **A (C2) start** | After C+B Done | C2 staging design execution |

---

## Explicit deferrals

| Item | When |
|------|------|
| C2 Coordinator/Worker staging | After C Done + B-lite Done |
| Phase D template pipeline | Far-term; keep API reservations only |
| Secret ref / full governance plane | Post B-lite; not blocking A′ |
| AI productization (prompt registry, cost) | P1 after A′ |
| LDAP/OAuth | Not scheduled this cycle |

---

## Timeline overview

```text
Weeks  0–2    3–5     6–9     10–13    14–15    16–18
       Wave0   Wave1   Wave2   Wave3    Wave4    C Done
       V1 cut  MatPol  L1 DAG  L2 WF    JS
B-lite         B1      B2      B3+B4
A (C2)                                              → start after C+B
```

---

## References

- `docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md`
- `docs/template-v2-product-roadmap.md`
- `docs/calcite-implementation-status.md`
- `docs/operator-console-usage.md`
- `docs/superpowers/specs/2026-06-01-c2-staging-closure-design.md` (deferred)
