# C Done + B-lite Done — Gap Checklist vs `master`

## Metadata

| Field | Value |
|-------|-------|
| Status | **Draft for review** |
| Baseline commit | `26f7ade` — `feat(console): add B-lite governance audit and publish gates` |
| Parent specs | `docs/superpowers/specs/2026-06-02-v2-capability-roadmap-design.md`, `docs/superpowers/specs/2026-06-01-c2-staging-closure-design.md` |
| Purpose | Decide what remains before declaring **C Done** / **B-lite Done** and starting **C2** |
| Out of scope | C2 distributed staging closure (explicit deferral) |

## Executive summary

Waves **0–4** and **B-lite core** landed on `master`: V1/migration product paths removed, MaterializationPolicy UI, Transform DAG editor + RunReport metrics, Workflow pause/resume UX, JsTransform editor, publish gate on schedules, audit API/page, RBAC filter + role matrix + unit tests, and **32** Playwright E2E tests.

**C Done** is **~85%**: all four official scenario families have **backend IT coverage** and **partial console E2E**, but the roadmap’s **“create from scenario”** wizard, **Scenario B full console loop**, **DAG staged preview in UI**, and **structured `branch` workflow editor** are still open.

**B-lite Done** is **~75%**: backend gates and audit UI exist, but **staging RBAC is docs-only** (no shipped `application-staging.yaml`), the **browser never sends role headers**, **Publish is not role-gated in UI**, and **filter/integration tests with `console-security.enabled=true`** are missing.

| Gate | Verdict on `26f7ade` | Block C2? |
|------|----------------------|-----------|
| **C Done** | **Not yet** — polish gaps below | Yes (per roadmap) |
| **B-lite Done** | **Not yet** — staging RBAC + UX gaps | Yes (per roadmap) |
| **C2 start** | Deferred until both above | — |

---

## Status legend

| Status | Meaning |
|--------|---------|
| **Done** | Meets acceptance criterion with evidence on `master` |
| **Partial** | Substantially implemented; criterion not fully met |
| **Missing** | Not implemented or not wired to product path |
| **N/A** | Explicitly out of scope for this gate |
| **Defer** | Post C Done / B-lite / C2 per roadmap |

---

## 1. C Done — official scenario families

**Roadmap gate:** Categories **A**, **B**, **WF**, **JS** — full console loop: **author → publish → run → RunReport** (`2026-06-02-v2-capability-roadmap-design.md` § Official scenario catalog).

### 1.1 Family-level summary

| Family | Scenario YAML | Backend IT | Console author E2E | Publish → run → report E2E | Status |
|--------|---------------|------------|--------------------|----------------------------|--------|
| **A** Synthetic + MatPol | `scenario-a-synthetic.yaml` | `V2ScenarioTemplateIT` | `materialization-policy.spec.ts` | Partial (save/policy; no full publish-run-report chain) | **Partial** |
| **B** Multi-source join | `scenario-b-lookup-join.yaml`, `scenario-dag-join.yaml` | `V2ScenarioTemplateIT`, `V2WorkflowScenarioIT` (dag-join) | `transform-dag.spec.ts` (synthetic DAG, not join scenario) | Partial (DAG metrics; not two-source join template) | **Partial** |
| **WF** Pause / branch / shared state | `scenario-wf-*.yaml` (4 files) | `V2WorkflowScenarioIT` | `workflow-pause.spec.ts` (pause only) | Pause/resume **Done**; branch **Missing** in UI/E2E | **Partial** |
| **JS** Transform | `scenario-js-transform.yaml` | `V2WorkflowScenarioIT` | `js-transform.spec.ts` | Partial (save + chain; publish optional in spec) | **Partial** |

### 1.2 C Done — criterion table

| ID | Criterion (from roadmap) | Status | Evidence on `master` | Gap / next step |
|----|--------------------------|--------|----------------------|-----------------|
| C-A1 | Scenario A configurable in console without YAML (MatPol) | **Done** | `SourcesStep.tsx`, `materialization-policy.spec.ts`, `scenario-a-synthetic.yaml` IT | Optional: E2E full publish → catalog run → report |
| C-A2 | “Create from scenario” for Synthetic (Sprint 2+) | **Missing** | Only empty scaffold: `TemplatesPage` → `/templates/new`, `fetchEditorScaffold()` | Add scenario picker + seed draft from `v2-scenarios` catalog API |
| C-B1 | Two-source join authored, published, run, per-transform diagnosis | **Partial** | Engine + `scenario-dag-join.yaml` IT; `TransformDagEditor`, `JobDetailPage` transformer table; E2E builds ad-hoc DAG not join scenario | E2E from multi-source `SourcesStep` through join DAG; document Scenario B path |
| C-B2 | “Create from scenario” for Multi-source join (Sprint 4) | **Missing** | Same as C-A2 | Same wizard; preset sources + join graph |
| C-B3 | Fan-out DAG scenario in CI | **Done** | `scenario-dag-fanout.yaml`, `V2WorkflowScenarioIT` | — |
| C-WF1 | Pause workflow visible and controllable from Job detail | **Done** | `JobDetailPage.tsx` PAUSED/resume/cancel; `workflow-pause.spec.ts`; backend `pauseReason` | — |
| C-WF2 | WorkflowPanel wizard UX (step types, params, compute binding) | **Partial** | `WorkflowPanel.tsx`, `WorkflowStepFields.tsx` for log/pause/invoke/shared_scope | **`branch` step JSON-only** (fallback textarea in `WorkflowStepFields.tsx`) |
| C-WF3 | Shared-state scenario + UI hints | **Partial** | `scenario-wf-shared-state.yaml` IT; shared_scope fields in editor | No dedicated E2E; hints/docs thin vs roadmap |
| C-WF4 | Branch workflow in console | **Missing** | `scenario-wf-branch.yaml` IT only | Structured branch editor + E2E |
| C-WF5 | “Create from scenario” for WF (Sprint 6) | **Missing** | — | Scenario wizard |
| C-JS1 | JS template created in console; scenario IT passes | **Done** | `TransformJsFields.tsx`, `js-transform.spec.ts`, `docs/js-transform-sandbox.md`, IT | — |
| C-JS2 | “Create from scenario” for JS (Sprint 7) | **Missing** | — | Scenario wizard |
| C-X1 | **C Done E2E:** all four families end-to-end in Playwright | **Partial** | 32 E2E tests; no single spec per official scenario catalog row | Add `scenario-catalog.spec.ts` or extend specs for A/B/WF/JS publish-run-report |
| C-X2 | Linear execution reliability (roadmap ~80% note) | **Defer** | C/D/E scenarios in `V2ScenarioTemplateIT` only | JSON streaming, sink retry/idempotency — post C Done polish |

### 1.3 Wave deliverables still open under C

| Wave | Deliverable | Status | Evidence | Next step |
|------|-------------|--------|----------|-----------|
| W2 | Staged preview **by DAG node** where API supports it | **Missing** (UI) | API: `ConsoleTemplateEditorActionsController` accepts `throughTransformIndex`; `TemplateV2StagedPreviewTests`; Review uses full-chain preview only | DAG node picker on Review/Transform tab; wire `throughTransformIndex` in `editor.ts` |
| W2 | `scenario-dag-join.yaml` console authoring parity | **Partial** | IT green; E2E uses hand-built DAG | Author join template in UI without YAML |
| W3 | `branch` in WorkflowPanel | **Missing** | JSON fallback | Branch step form (condition / thenSteps) |
| All | Scenario catalog **create from scenario** | **Missing** | Roadmap table § Official scenario catalog | One wizard entry point on Templates home |

---

## 2. B-lite Done — governance & operability

**Roadmap gate:** Publish gate + audit UI + **staging RBAC** + workflow job states (`2026-06-02-v2-capability-roadmap-design.md` § Phase B-lite scope).

### 2.1 B-lite — criterion table

| ID | Package | Criterion | Status | Evidence on `master` | Gap / next step |
|----|---------|-----------|--------|----------------------|-----------------|
| B1-1 | B1 | `requirePublishedForTaskRun` enforced for **scheduled** runs | **Done** | `TaskScheduleService.requirePublishedForTaskRun`; `TaskScheduleServiceTests.draftTemplateRejectedForSchedule` | — |
| B1-2 | B1 | DRAFT run **labeled** on Review | **Done** | `ReviewPanel.tsx` `review-run-draft-hint`, draft run button copy | — |
| B1-3 | B1 | `requirePublished=true` in prod for **non-editor** task runs | **Partial** | Default `DataGeneratorProperties.Governance.requirePublishedForTaskRun=true`; `TaskController.runById` requires publish | Console **`POST /templates/{id}/run`** and **`runExisting`** use `runByIdAllowDraft` — catalog run skips publish gate |
| B1-4 | B1 | OPERATOR runs **PUBLISHED** only (role matrix) | **Partial** | `ConsoleRole.OPERATOR` lacks `TEMPLATE_PUBLISH`; no runtime enforcement when security off | With security on: enforce publish on catalog run; keep explicit draft/run endpoints for editor |
| B2-1 | B2 | `GET /api/console/audit` | **Done** | `ConsoleAuditController`, `ConsoleAuditControllerTest` | — |
| B2-2 | B2 | Console audit page; publish events searchable | **Done** | `AuditPage.tsx`, nav route, `pages.spec.ts` audit smoke | Optional: filter E2E by `TEMPLATE_PUBLISH` |
| B2-3 | B2 | No secrets in audit detail | **Done** | `AuditDetailSanitizer.java` | — |
| B3-1 | B3 | RBAC enforced on mutating APIs | **Partial** | `ConsoleAuthorizationFilter`, permission map incl. `AUDIT_READ`, `/publish` → `TEMPLATE_PUBLISH` | **Default off** in dev/E2E (`application-e2e.yaml`); no HTTP IT with filter enabled |
| B3-2 | B3 | Staging sample config (`console-security.enabled=true`) | **Missing** | Documented snippet in `operator-console-usage.md` only | Add `application-staging.yaml` (or profile) in repo |
| B3-3 | B3 | 403 UX | **Partial** | `api/client.ts` 403 message | No E2E 403 cases (roadmap: optional) |
| B3-4 | B3 | Role ITs — VIEWER cannot save | **Missing** (integration) | `ConsoleRoleTests` unit matrix only | `@SpringBootTest` + MockMvc with security enabled |
| B3-5 | B3 | Console RBAC UX (role headers from browser) | **Missing** | No `X-Console-Role` in console-web | Dev role picker or ingress doc + integration test doubles |
| B3-6 | B3 | Publish hidden/disabled for non-ADMIN in UI | **Missing** | `ReviewPanel` always shows Publish | Gate button on `ConsoleRole` / runtime flags |
| B4-1 | B4 | PAUSED / Resume / Cancel in Job detail | **Done** | `JobDetailPage.tsx`, `workflow-pause.spec.ts` | — |

### 2.2 B-lite Done verdict

| Required for B-lite Done | Met? |
|--------------------------|------|
| Publish gate (schedules + prod policy) | **Mostly** — schedule gate done; catalog run bypass |
| Audit UI | **Yes** |
| Staging RBAC | **No** — config + browser + integration tests |
| Workflow job states | **Yes** |

---

## 3. Wave 0 — V1 amputation (prerequisite)

| ID | Criterion | Status | Evidence | Notes |
|----|-----------|--------|----------|-------|
| W0-1 | No migration page/tab/API | **Done** | No `MigrationPage`, no `/migration` route, no `Console*Migration*` controllers | — |
| W0-2 | V1 rejected in editor/API | **Done** | `TemplateEditorService.rejectV1Template`, `TemplateEditorServiceV1RejectionTests`, catalog filters V1 in `ConsoleTemplateController` | — |
| W0-3 | Migration docs archived | **Done** | `docs/archive/migration/**` | — |
| W0-4 | No migration/V1 in operator docs as product | **Done** | `operator-console-usage.md` V2-first | — |
| W0-5 | CI independent of V1/migration | **Done** | Gates on `v2-scenarios/**`, `verify-console.ps1` | — |
| W0-6 | Dead migration i18n removed | **Partial** | `en.json` / `zh-CN.json` still contain `migration.*` keys | Cosmetic cleanup |

**Wave 0 verdict:** **Done** (product paths removed; optional i18n cleanup).

---

## 4. Verification baseline (evidence)

| Layer | Command / artifact | Last known state |
|-------|-------------------|------------------|
| Baseline commit | `git rev-parse master` | `26f7ade` |
| Java unit (console slice) | `scripts/verify-console-unit.ps1` | Includes `ConsoleAuditControllerTest`, `ConsoleRoleTests`, `TaskScheduleServiceTests` |
| Frontend build | `npm run build` in `data-generator-console-web` | Passed in Wave 4 session |
| E2E | `scripts/e2e-podman.ps1` / `verify-console.ps1` | **32/32** Playwright (incl. `js-transform`, `transform-dag`, `workflow-pause`, audit page) |
| Scenario ITs | `V2ScenarioTemplateIT`, `V2WorkflowScenarioIT` | A/B/C/D/E + DAG + WF + JS YAMLs |
| Staged preview (backend) | `TemplateV2StagedPreviewTests` | Green; not exposed in console |

---

## 5. Explicit deferrals (do not block false “C Done” on these)

| Item | Roadmap timing | Notes |
|------|----------------|-------|
| **C2** Coordinator/Worker staging closure | After C Done + B-lite Done | `2026-06-01-c2-staging-closure-design.md` |
| Phase D template-level pipeline DAG | Far-term | API reservations only |
| Secret ref / full governance plane | Post B-lite | Plaintext password rejection exists |
| LDAP/OAuth/SSO | Not this cycle | — |
| AI productization | P1 after A′ | — |
| C/D/E file/chunk/stream as **C Done** families | N/A for C Done gate | IT coverage exists; roadmap lists as “Done / docs link” not in A/B/WF/JS gate |

---

## 6. Recommended closure packs (pick after review)

Prioritized **small** epics to close gates without starting C2.

### Pack 1 — **C Done polish** (~1–2 sprints)

1. Scenario catalog wizard (“create from scenario”) for A, B, WF, JS.
2. Structured **branch** workflow step editor + E2E against `scenario-wf-branch.yaml` patterns.
3. **DAG staged preview** UI (`throughTransformIndex` per node).
4. **`scenario-catalog.e2e.spec.ts`**: one happy path per family — author → publish → run → RunReport.

**Closes:** C-A2, C-B2, C-WF4/5, C-JS2, C-X1, W2 staged preview, Scenario B console parity.

### Pack 2 — **B-lite staging closure** (~0.5–1 sprint)

1. Ship `application-staging.yaml` with `console-security.enabled=true` + governance flags.
2. Console dev/staging **role header** injection (or documented proxy).
3. Hide/disable Publish for non-ADMIN; enforce published-only on **catalog Run**.
4. `ConsoleAuthorizationFilter` MockMvc / `@SpringBootTest` ITs (VIEWER save → 403).

**Closes:** B1-3/4, B3-2/4/5/6.

### Pack 3 — **Execution reliability** (parallel / post-gate)

Linear sink retry, streaming polish, partial-success semantics — does **not** block C Done per roadmap family gate but affects operator trust.

---

## 7. Gate decision matrix

| If you need… | Minimum packs |
|--------------|---------------|
| Declare **B-lite Done** | Pack 2 |
| Declare **C Done** | Pack 1 |
| Start **C2** staging design execution | Pack 1 + Pack 2 |
| Production operator trust (non-blocking) | Pack 3 |

---

## 8. Open questions for product

1. **Draft run policy:** Should catalog **Run** require `PUBLISHED` when `requirePublishedForTaskRun=true`, while Review keeps explicit “Run draft”?
2. **Scenario wizard scope:** Seed from bundled YAML only, or also DB-published exemplar templates?
3. **Branch editor depth:** Minimal condition + step list, or full nested workflow authoring?
4. **RBAC UX:** In-app role switcher for staging vs rely on ingress headers only?

---

## References

- `docs/superpowers/specs/2026-06-02-v2-capability-roadmap-design.md`
- `docs/superpowers/specs/2026-06-01-c2-staging-closure-design.md`
- `docs/operator-console-usage.md`
- `docs/js-transform-sandbox.md`
- `data-generator-service/src/main/resources/template/v2-scenarios/*.yaml`
- `data-generator-console-web/e2e/specs/*.spec.ts`
