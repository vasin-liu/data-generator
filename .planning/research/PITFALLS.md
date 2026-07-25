# Pitfalls Research

**Domain:** Brownfield Template V2 hardening — HTTP execute-path proof, dialect opt-in ITs, dual JDBC resolver docs, multi-JVM distributed E2E, RBAC enable-path, P1 harness expansion  
**Researched:** 2026-07-25  
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: Calling in-process `TemplateV2Runner` and labeling it HTTP `/task/run`

**What goes wrong:**
A new IT reuses the Phase 11 `ManagedJdbcCatalogSinkE2eIT` pattern (save catalog → `templateV2Runner.run` → `COUNT(*)`) and claims the v2.1 “HTTP execute-path” requirement is closed. Audit/flow language drifts again: “E2E” means in-process runner, not the operator/API path.

**Why it happens:**
In-process runner is fast, deterministic, and already green. HTTP `/task/run` (and console `/api/templates/{id}/run`) adds queueing, `TaskExecutionService`, async completion, governance gates (`require-published-for-task-run`), and often `WorkflowRunContext` / `snap:` binding — easy to skip under schedule pressure.

**How to avoid:**
- Define success as: HTTP (or MockMvc) POST that exercises the **task run** stack, then assert sink rows and/or job SUCCESS.
- Keep Phase 11 IT as regression; do **not** rename or “upgrade” it in place to pretend HTTP coverage.
- Document which endpoint is proven (`/task/run` vs `/api/templates/.../run`) — they are not interchangeable for evidence.

**Warning signs:**
- New test class has no `MockMvc` / `TestRestTemplate` / Playwright call to a run URL.
- SUMMARY claims “HTTP proof” while linked_tests still point only at `ManagedJdbcCatalogSinkE2eIT`.
- Assertions never wait on `task_execution` / job status.

**Phase to address:**
Phase 12 (HTTP execute-path proof)

---

### Pitfall 2: Wrong `WorkflowRunContext` / `snap:` assumptions on the HTTP path

**What goes wrong:**
HTTP proof either (a) unbound-runs only and never hits `snap:{instanceId}:{name}`, or (b) force-binds context incorrectly and breaks managed-id resolution. Mid-flight DS reload isolation (DS-03) looks covered when it is not.

**Why it happens:**
Phase 11 deliberately avoided `WorkflowRunContext.bind` so resolve stays on logical `dataSourceId`. Real `/task/run` orchestration **does** bind run context. Copying Phase 11 constraints into an HTTP IT, or over-binding in a unit-style test, both produce false confidence.

**How to avoid:**
- Separate proofs: unbound managed-id path (catalog resolve) vs bound `snap:` path (reuse / extend `JdbcSnapshotExecutePathIT` patterns).
- For HTTP runs, assert the routing key / pool behavior that the production path actually uses.
- Do not “fix” resolvers while writing the HTTP IT — observe first.

**Warning signs:**
- IT comments say “no WorkflowRunContext” on a class meant to prove task run.
- Assertions only check `dataSourceId` string equality, never snapshot key or post-reload isolation.
- Failures only when governance/hot-reload profiles are enabled.

**Phase to address:**
Phase 12 (HTTP proof) + Phase 14 (resolver ownership docs — call-site inventory must list bound vs unbound callers)

---

### Pitfall 3: Promoting Dameng live IT into default CI

**What goes wrong:**
`-Ddm.it=true` / `DG_DM_IT=true` becomes required for `verify-harness.ps1` or a P0 matrix row. CI fails without a licensed DM driver/image; jobs get slower or skip-conditional flapping; merge gate turns red for infrastructure, not product regressions.

**Why it happens:**
v2.1 says “Dameng live IT documented green path.” Teams interpret “green” as “always runs in CI.” `ChunkedPipelineDamengUpsertIT` is still a placeholder that `Assumptions.abort`s even when the flag is on — temptation to wire a heavy container and make it P0.

**How to avoid:**
- Keep default CI = MERGE SQL unit (`JdbcSinkSqlBuilderTests`) as today.
- Ship a **documented opt-in recipe** (flags, image/host, expected PASS) and a UAT/script slice that is **not** the P0 merge gate.
- If the IT remains placeholder, do not claim “live IT green” — claim “documented enable path + unit MERGE.”

**Warning signs:**
- `dm.it` appears in harness-verify.yml or P0 `linked_tests` without `skipped-conditional`.
- New Dameng Testcontainers module pulled into every `-am test`.
- CI minutes spike only on dialect jobs.

**Phase to address:**
Phase 13 (Dameng path + Nyquist hygiene)

---

### Pitfall 4: Accidental RBAC default-on (or profile bleed)

**What goes wrong:**
`ConsoleSecurityProperties.enabled` default flips to `true`, or `application.yaml` / shared e2e profile enables console-security globally. Local `mvn test`, Podman e2e, and operator demos break with 401/403 until headers are set. “Hardening” becomes a breaking default change — explicitly out of scope for v2.1.

**Why it happens:**
Staging (`application-staging.yaml`) and `application-e2e-rbac.yaml` already enable RBAC. Copy-paste into base `application.yaml`, or changing the Java field default (`enabled = false`), looks like a one-line “security win.”

**How to avoid:**
- Keep Java default and base config **off**.
- Prove enablement only via dedicated profile/IT (`application-e2e-rbac.yaml`, `ConsoleAuthorizationIntegrationIT` pattern) + staging/e2e docs.
- Add an explicit assertion in the RBAC phase: default property remains `false` in base config and `ConsoleSecurityProperties`.

**Warning signs:**
- Diff touches `enabled = false` → `true` in `ConsoleSecurityProperties`.
- Non-RBAC Playwright specs start needing `X-Console-Role`.
- “Fix” PRs adding headers to every console client.

**Phase to address:**
Phase 16 (RBAC testable enable path)

---

### Pitfall 5: Flaky multi-JVM distributed worker E2E

**What goes wrong:**
Dual-JVM (coordinator + worker) tests flake on lease timing, shared H2 `AUTO_SERVER`, port collisions, leftover `distributed_job` rows, or heartbeat shorter than CI load. Intermittent red CI teaches people to skip the only distributed proof.

**Why it happens:**
Distributed path needs shared JDBC, unique `worker-id`, lease/heartbeat windows (`docs/staging-distributed-deployment.md`), and careful startup order. Single-JVM mocks of “lease acquired” do not catch race/expiry. Podman/Playwright dual-process is slower and order-sensitive.

**How to avoid:**
- Prefer one **narrow** happy-path (enqueue on coordinator → worker SUCCESS) before lease-steal/chaos cases.
- Reuse existing distributed staging profiles (`application-e2e-distributed.yaml`, `distributed-staging`) and documented AC-1/AC-2 style checks; gate chaos (AC-4/AC-5) as optional/P1.
- Stabilize with generous timeouts, unique DB paths per run, and explicit cleanup — not shorter leases.
- Link harness as P1/`skipped-conditional` until flake rate is known; do **not** expand P0 with a dual-JVM row on day one.

**Warning signs:**
- Failures only under load / only on Windows file H2.
- Test sleeps fixed 1–2s “hoping” lease expires.
- Worker and coordinator share the same `worker-id` or isolated DBs.

**Phase to address:**
Phase 15 (multi-JVM worker E2E)

---

### Pitfall 6: “Docs-only” resolver work becomes a code merge

**What goes wrong:**
While inventorying `JdbcCatalogResolver` vs `DefaultRuntimeJdbcEndpointResolver`, someone consolidates them “while we’re here.” Execute-path `snap:` behavior or catalog-side helpers regress; Phase 07.1 ownership Javadoc is undone.

**Why it happens:**
Class Javadoc already says consolidation is deferred; reading duplicate resolve/register logic feels like unfinished work. Hardening milestones attract cleanup.

**How to avoid:**
- v2.1 deliverable = ownership doc + call-site inventory only (PROJECT.md).
- Any behavioral change needs an explicit scope re-open, not a drive-by PR.
- Inventory must mark: catalog-side vs V2 execute-path vs bound `snap:` callers.

**Warning signs:**
- Diff deletes or wraps one resolver into the other.
- “Refactor” commits without new HTTP/Dameng/RBAC proofs.
- Tests move from service execute-path to datasource-module-only coverage.

**Phase to address:**
Phase 14 (resolver ownership docs)

---

### Pitfall 7: Scope creep into ORCH / net-new connectors

**What goes wrong:**
Hardening work expands into template orchestration (ORCH-01/02), Redis/S3/HTTP connectors, or “while fixing run path, add flow-control transforms.” Milestone duration balloons; P0 gate and proof depth stay open.

**Why it happens:**
HTTP `/task/run` and distributed worker sit next to workflow/orchestration code. Connector gaps look like “reliability.” Deferred STATE rows feel urgent once touched.

**How to avoid:**
- Treat ORCH and net-new connectors as **hard out of scope** (PROJECT.md).
- If a proof needs a missing connector, use existing JDBC/CSV/JSON fixtures — do not add adapters.
- Reject plans whose success criteria mention branch/retry/parallel DAG or new sink kinds.

**Warning signs:**
- New modules under `data-generator-writer-*` / `reader-*` for Redis/S3/HTTP.
- Roadmap items titled “orchestration hardening.”
- Requirements that cannot map to HTTP proof, Dameng, resolvers, multi-JVM, RBAC, or P1 rows.

**Phase to address:**
All v2.1 phases (12+) — enforce at plan review; Phase 17 (P1 harness) especially vulnerable to “add rows for everything”

---

### Pitfall 8: Promoting new proofs straight to P0 and breaking the merge gate

**What goes wrong:**
HTTP `/task/run`, Dameng live, and multi-JVM rows are added as **P0**. `verify-harness.ps1` / harness-verify.yml blocks merges on opt-in infra or flaky dual-JVM. Team disables the gate or marks everything skipped-conditional — worse than no new rows.

**Why it happens:**
v2.0 expanded P0 to 15 for streaming/upsert/dialects. Habit: “important ⇒ P0.” v2.1 explicitly wants **focused P1** for new proof paths; P0 remains the merge gate.

**How to avoid:**
- New hardening proofs → **P1** (or skipped-conditional) until stable and embedded-first.
- Do not raise P0 count unless a path is CI-default, fast, and non-flaky.
- Keep UAT scripts supplementary (RETROSPECTIVE lesson).

**Warning signs:**
- `test-matrix.yaml` diffs set `tier: P0` on Dameng/multi-JVM/HTTP rows without flake data.
- harness-verify fails for missing Docker/DM license.
- PRs that “temporarily” ignore `p0.pass`.

**Phase to address:**
Phase 17 (P1 harness expansion)

---

### Pitfall 9: Nyquist backfill as rewrite theater

**What goes wrong:**
Missing VALIDATION.md for phases 07.1/08 (and partial 07) turns into re-running whole feature phases, rewriting tests, or blocking v2.1 DoD on historical Nyquist. Real HTTP/Dameng/RBAC work stalls.

**Why it happens:**
Audit flagged Nyquist as tech_debt. “Hygiene” is vague; agents over-index on compliance frontmatter.

**How to avoid:**
- Backfill VALIDATION.md / `nyquist_compliant` with **existing** linked tests — documentation hygiene, not new product scope.
- Do not reopen Phase 8 streaming implementation for coverage math.
- Keep Nyquist non-blocking for v2.1 feature DoD unless explicitly required in phase CONTEXT.

**Warning signs:**
- Plans that only edit old phase folders without new Phase 12+ proofs.
- Large test refactors under “Nyquist.”
- DoD checklist requiring all historical phases COMPLIANT before HTTP IT lands.

**Phase to address:**
Phase 13 (Nyquist hygiene backfill alongside Dameng docs)

---

### Pitfall 10: Same-JVM IT re-run flake and stdout floods

**What goes wrong:**
Catalog/H2 ITs omit `DROP IF EXISTS` / unique mem DB names; re-runs in the same JVM fail. Streaming OOM/UAT ITs dump huge payloads to logs (Phase 8 debt), hiding real failures in multi-step verify scripts.

**Why it happens:**
Phase 11 review already warned on bare `CREATE TABLE` for named H2 mem DBs. Copying that IT for HTTP proof repeats the flake. Log-heavy ITs were accepted as debt.

**How to avoid:**
- Unique table/DB names per test class; idempotent DDL.
- Cap or redirect large payload dumps in any new UAT slice.
- Prefer job status + COUNT(*) over printing full row sets.

**Warning signs:**
- IT green once, red on `-Dtest=...` re-run.
- UAT logs multi-MB of JSON/CSV rows.
- Surefire “table already exists.”

**Phase to address:**
Phase 12 (HTTP IT craft) + Phase 13 (hygiene when touching Phase 8 artifacts)

---

### Pitfall 11: Treating CodeGraph init as a hardening deliverable

**What goes wrong:**
Milestone time spent on `codegraph init -i` and index maintenance instead of HTTP/Dameng/RBAC proofs. Or agents assume CodeGraph is available (audit noted missing index) and stall research.

**Why it happens:**
v2.0 audit listed missing CodeGraph under tooling debt. It helps future audits but does not close operator-facing proof gaps.

**How to avoid:**
- Optional tooling chore, not a v2.1 success criterion.
- Use grep/Read when `.codegraph/` is absent; do not block plans on indexing.

**Warning signs:**
- Phase plan whose only SC is “CodeGraph index exists.”
- Research loops retrying codegraph against unindexed root.

**Phase to address:**
Optional chore after Phase 17 — not a proof phase

---

## Technical Debt Patterns

Shortcuts that seem reasonable but create long-term problems.

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| In-process `TemplateV2Runner` as sole “E2E” | Fast green IT | HTTP/queue/governance gaps reopen at next audit | Regression only; never as v2.1 HTTP DoD |
| Dameng MERGE unit + opt-in placeholder IT | CI stays free of DM license | Live dialect path never proven | Until documented green path exists; keep out of P0 |
| Dual JDBC resolvers side-by-side | No risky merge | Drift between catalog-side and execute-path | v2.1 docs-only; merge only with dedicated milestone |
| RBAC default-off | Local/dev ergonomics | Deployments forget to enable | Default-off OK; must have testable enable + staging docs |
| P0-only matrix growth | Strong merge gate | Flaky/opt-in rows block all PRs | New hardening proofs as P1 first |
| Skip Nyquist VALIDATION.md | Ship features faster | Audit noise, false orphans | Never for **new** phases 12+; backfill old phases as hygiene only |
| Dual-JVM E2E with tight leases | “Realistic” timing | Chronic flake | Longer leases + narrow AC first |
| UAT script per phase | Operator-friendly | Script sprawl; unclear merge gate | OK if harness remains canonical |

## Integration Gotchas

Common mistakes when connecting proofs to this system.

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| `/task/run` vs console run API | Prove one, document the other | State the exact URL; prefer `/task/run` if that is the milestone wording |
| Managed `dataSourceId` | Inline `InlineDataSourceVO` in “managed” IT | Catalog save + managed id only on sink/source under test |
| `snap:` routing | Unbound runner assumed equal to task run | Bound context under real run orchestration |
| Dameng | Require live DM in default `mvn test` | Unit MERGE always; live IT behind `-Ddm.it` / `DG_DM_IT` |
| Kingbase/PG-proxy | Assume proxy ≡ licensed engine | Keep proxy for CI; document licensed limits |
| Distributed worker | Single JVM “simulates” two roles without shared DB | Coordinator + worker profiles, shared JDBC, unique `worker-id` |
| Console RBAC | Enable in base `application.yaml` | Profile/`@SpringBootTest` properties + `application-e2e-rbac.yaml` |
| Harness matrix | Link Playwright-only for backend execute path | Maven IT for HTTP/MockMvc path; Playwright supplementary |
| H2 metadata DB | File DB locks across dual JVM on Windows | Follow distributed-staging `AUTO_SERVER` / unique paths guidance |
| Governance flags | Run unpublished template via HTTP | Align with `require-published-for-task-run` in test profile |

## Performance Traps

Patterns that work at small scale but fail as usage grows.

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Full reactor test for every proof | 20–40+ min local loops | Narrow `-pl … -am -Dtest=` slices | Every PR if HTTP IT pulls full console frontend |
| Dameng container in default CI | Queue backlog, license failures | Opt-in flag; P1/UAT only | First DM-less CI agent |
| Dual-JVM + Playwright + large streaming fixture | Timeouts, OOM, log floods | Tiny template for distributed happy path | CI runners &lt; 4 GB or shared runners |
| Short lease + slow CI | False lease steals | leaseSeconds ≫ p95 run; heartbeat = lease/3 | Contended CI |
| Dumping all streaming rows to stdout | Unusable UAT logs | Count/hash asserts | Already painful at Phase 8 UAT sizes |

## Security Mistakes

Domain-specific security issues beyond general web security.

| Mistake | Risk | Prevention |
|---------|------|------------|
| Default-on RBAC without header clients | Breaks trusted-intranet installs; emergency disable culture | Keep default-off; document staging enable |
| RBAC tests that only check filter unit, never enable path | “Covered” but e2e profile unused | IT with `console-security.enabled=true` + role headers |
| Logging secrets in Dameng/connectivity failure asserts | Credential leakage in CI logs | Assert actionable messages **without** secrets (Kingbase pattern) |
| HTTP run IT bypasses plaintext/governance checks | Ships templates that publish would reject | Use published templates / governance-on profile for HTTP proof |
| Consolidating resolvers and dropping `SecretResolver` path | Secret resolution drift | Docs-only in v2.1; preserve both resolve paths |

## UX Pitfalls

Common user experience mistakes in this domain (operator console / run ops).

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Docs say “enable RBAC” without profile name/headers | Operators lock themselves out | Document `application-e2e-rbac` / staging keys + required headers |
| Distributed run with no worker started | Jobs stuck QUEUED; console looks “broken” | Runbook: coordinator then worker; metrics endpoint check |
| Dameng “supported” but only MERGE unit | Operators expect live upsert in CI/demo | Explicit “unit always / live opt-in” in dialect docs |
| Harness red on P1-only failure presented as merge block | Confusion / ignored gate | UI/docs: P0 blocks merge; P1 tracked in summary only |

## "Looks Done But Isn't" Checklist

Things that appear complete but are missing critical pieces.

- [ ] **HTTP execute-path:** Often missing actual `/task/run` (or documented console equivalent) — verify MockMvc/HTTP + job/execution SUCCESS + sink rows, not only `TemplateV2Runner`
- [ ] **Managed catalog proof:** Often missing managed `dataSourceId` (inline DS sneaks back) — verify catalog save + no inline DS on the proven sink
- [ ] **`snap:` path:** Often missing bound `WorkflowRunContext` — verify snapshot key or reload isolation on the run path used by HTTP
- [ ] **Dameng:** Often missing real wiring (placeholder `Assumptions.abort`) — verify either live PASS with flag **or** honest “docs + unit only” wording
- [ ] **Resolver docs:** Often missing call-site inventory — verify catalog-side vs execute-path vs `snap:` callers listed; no code merge
- [ ] **Multi-JVM:** Often missing second JVM / shared DB — verify coordinator enqueue + distinct worker process SUCCESS
- [ ] **RBAC:** Often missing default-still-off check — verify base `enabled=false` + separate enable-path IT/profile
- [ ] **P1 rows:** Often missing matrix linkage — verify `.planning/test-matrix.yaml` + harness summary, **not** silent P0 promotion
- [ ] **Nyquist:** Often missing VALIDATION.md for **new** phases 12+ — verify frontmatter on each new phase (backfill old phases without rewrites)
- [ ] **Accepted limits:** Often missing audit/SUMMARY notes — verify in-process vs HTTP, opt-in DM, dual-resolver deferral still explicit

## Recovery Strategies

When pitfalls occur despite prevention, how to recover.

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| False HTTP proof (in-process only) | MEDIUM | Keep IT; add real HTTP IT; fix audit wording; do not delete Phase 11 coverage |
| RBAC default flipped on | HIGH | Revert default; restore base yaml; add regression assert on `enabled=false`; fix broken e2e clients |
| Dameng forced into P0/CI | HIGH | Demote to P1/opt-in; restore MERGE unit as CI proof; document flag recipe |
| Flaky multi-JVM gate | MEDIUM | Remove from P0; lengthen lease; narrow to happy path; quarantine chaos cases |
| Accidental resolver merge regression | HIGH | Revert merge; restore ownership Javadoc; re-run `JdbcSnapshotExecutePathIT` + managed sink IT |
| ORCH/connector scope creep | MEDIUM | Move work to deferred STATE; cut plans; re-baseline milestone DoD |
| P0 gate disabled | HIGH | Restore `p0.pass` enforcement; move unstable rows to P1 |

## Pitfall-to-Phase Mapping

How roadmap phases should address these pitfalls. (v2.1 phases provisional, starting at 12.)

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| In-process labeled as HTTP | Phase 12 | Linked test hits `/task/run` (or named console run) + execution SUCCESS |
| Wrong `snap:` / bind assumptions | Phase 12 (+ 14 inventory) | Bound vs unbound cases documented; snapshot IT still green |
| Dameng in default CI | Phase 13 | Default `mvn test` skips live DM; opt-in recipe PASS documented |
| Nyquist rewrite theater | Phase 13 | VALIDATION backfill only; no Phase 8 behavior rewrite |
| Resolver code merge | Phase 14 | Docs + call-site inventory diff only; both resolvers remain |
| Flaky multi-JVM | Phase 15 | One stable happy-path E2E; not P0 until flake-free |
| RBAC default-on | Phase 16 | Property default false + enable-path IT/profile green |
| P0 promotion / gate breakage | Phase 17 | New rows `tier: P1`; `p0.pass` still merge gate |
| ORCH/connector creep | All (plan review) | No ORCH/Redis/S3/HTTP adapter plans in v2.1 |
| CodeGraph as deliverable | Optional chore | Not in phase DoD |
| Same-JVM H2 flake / log floods | Phase 12–13 | Idempotent DDL; no full-row stdout in new UAT |

## Sources

- `.planning/PROJECT.md` — v2.1 scope / out of scope (HTTP proof, Dameng, resolver docs-only, multi-JVM, RBAC default-off, P1)
- `.planning/milestones/v2.0-MILESTONE-AUDIT.md` — accepted limits (in-process E2E, Dameng opt-in, dual resolvers, Nyquist partial, CodeGraph missing)
- `.planning/RETROSPECTIVE.md` — harness-as-gate, accepted limits documentation, dual-resolver inserted phase lesson
- `ManagedJdbcCatalogSinkE2eIT` + Phase 11 CONTEXT/RESEARCH (D-05 in-process vs HTTP)
- `DefaultRuntimeJdbcEndpointResolver` / `JdbcCatalogResolver` ownership Javadoc
- `DamengTestSupport` / `ChunkedPipelineDamengUpsertIT` (`-Ddm.it`, placeholder abort)
- `ConsoleSecurityProperties` + `application-e2e-rbac.yaml` / staging security profiles
- `docs/staging-distributed-deployment.md` — coordinator/worker, lease/heartbeat, AC matrix
- `AGENTS.md` — P0 merge gate via `verify-harness.ps1`; P1 does not block
- `.planning/test-matrix.yaml` — tier semantics

---
*Pitfalls research for: v2.1 Hardening & Weak-Spot Closure (data-generator)*  
*Researched: 2026-07-25*
