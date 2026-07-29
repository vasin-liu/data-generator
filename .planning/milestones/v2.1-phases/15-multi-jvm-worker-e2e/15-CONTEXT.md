# Phase 15: Multi-JVM Worker E2E - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning
**Mode:** --auto (recommended defaults selected in one pass)

<domain>
## Phase Boundary

Prove **one multi-JVM happy path** for DIST-01: a **coordinator JVM** enqueues a V2 run, a **separate worker JVM** (`DataGeneratorWorkerApplication`) leases and executes it to **SUCCESS**, with **harness linkage** (P1 acceptable) and a **runnable local recipe**.

This phase closes the process-boundary gap left by existing single-JVM distributed ITs. It does **not** deliver full staging acceptance (AC-1..AC-7), chaos/lease-recovery drills, a second worker, RBAC-on paths, or P0 matrix expansion.

</domain>

<decisions>
## Implementation Decisions

### Multi-JVM topology & shared metadata DB
- **D-01:** Use the **existing C2 staging profile stack** — coordinator: `DataGeneratorApplication` with `distributed-staging,distributed-coordinator`; worker: `DataGeneratorWorkerApplication` with `distributed-staging,distributed-worker`. Both JVMs share a **file-backed H2 metadata DB** via `application-distributed-staging.yaml` (`jdbc:h2:file:./db/distributed-staging;AUTO_SERVER=TRUE`). Each verify run uses an **isolated temp `db/` directory** (not in-memory H2).
- **D-02:** **Primary runnable proof** = a new focused PowerShell script (recommended name: `scripts/verify-multi-jvm-worker.ps1`) that starts **two host OS processes**, triggers a run, polls terminal SUCCESS, and exits non-zero on failure. **Podman** (`scripts/e2e-distributed-podman.ps1`) remains an **optional operator drill** — it is **not** the DIST-01 done gate or harness-linked primary artifact.

### Harness linkage
- **D-03:** Add a **P1** matrix row (recommended id: `dist-multi-jvm-worker`) in `.planning/test-matrix.yaml` with `tier: P1`, linking to the verify script (and optional guard IT class name if research adds one). Regenerate matrix doc per existing harness flow. **Do not** promote this row to P0 or change the 15-row P0 gate.
- **D-04:** `scripts/verify-harness.ps1` must surface the new row in `target/test-matrix-summary.json`; row may stay `pending`/`partial` until implementation lands — P1 failures are **non-blocking** per `docs/test-harness.md`.

### Enqueue trigger & template shape
- **D-05:** Coordinator trigger = HTTP **`POST /task/run/{templateId}`** (`TaskController`) — same execute spine as Phase 12, not console-only enqueue helpers alone. Seed a **published** minimal V2 template before trigger (`data.generator.governance.require-published-for-task-run=true` in distributed-staging).
- **D-06:** Template shape = **minimal happy path**: iterator source → SQL transform → **console sink** (same family as `DistributedSplitRoleIntegrationTests`, but with **real** `TemplateV2Runner` execution in the multi-JVM path — no `CapturingTemplateV2Runner` stub).

### SUCCESS evidence & fast-path ITs
- **D-07:** Script assertions must observe **both** `distributed_job.status == SUCCESS` **and** linked `task_execution.status == SUCCESS` for the enqueued instance, within a bounded poll timeout (REST and/or shared-DB inspection — planner picks simplest reliable probe).
- **D-08:** Keep existing single-JVM distributed ITs (`DistributedSplitRoleIntegrationTests`, `DistributedJob*IntegrationTests`, etc.) **unchanged** as fast embedded feedback; multi-JVM proof is **additive**, not a replacement.

### Documentation packaging
- **D-09:** Extend `docs/staging-distributed-deployment.md` with a **DIST-01 local verify** subsection (prerequisites, script one-liner, expected SUCCESS signals, cleanup). Add an **AGENTS.md** Commands/docs pointer using the Phase 13/14 pattern (comment + path).

### Explicit non-goals (scope lock)
- **D-10:** **Out of scope for DIST-01 done criteria:** full staging AC-1..AC-7 checklist, chaos/heartbeat expiry drills (AC-4/5), second worker JVM, Playwright distributed specs as required gate, Podman-only proof path, RBAC-enabled runs (Phase 16), coordinator-local execution when worker disabled, new distributed framework/libs.

### Claude's Discretion
- Exact verify script filename if `verify-multi-jvm-worker.ps1` collides or a better name fits existing `verify-*.ps1` catalog
- Whether to add an optional env-gated `*IT` class (e.g. `MultiJvmWorkerE2eIT`) in addition to the script — script remains primary evidence
- Poll implementation details (REST job APIs vs direct JDBC against shared H2 file)
- Template seeding mechanism inside the script (SQL insert vs REST template publish APIs)
- Exact matrix row columns/notes text and `status` transition to `covered` when green

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — DIST-01 (active)
- `.planning/ROADMAP.md` — Phase 15 goal and success criteria
- `.planning/PROJECT.md` — multi-JVM worker E2E path in v2.1 scope
- `.planning/STATE.md` — Phase 15 entry; research flag on recipe choice
- `.planning/research/SUMMARY.md` — Phase 15 rationale; file H2 vs Podman flag
- `.planning/research/STACK.md` — two-process + shared file H2 pattern; P1 harness row guidance
- `.planning/research/ARCHITECTURE.md` — distributed enqueue/lease/execute flow
- `.planning/research/FEATURES.md` — reuse existing distributed stack; one happy path
- `.planning/research/PITFALLS.md` — multi-JVM flakiness; avoid over-scoping ACs

### Prior phase decisions
- `.planning/phases/12-http-execute-path-proof/12-CONTEXT.md` — HTTP `/task/run` spine proof (extend here across JVM boundary)

### Distributed runtime (source of truth)
- `data-generator-service/src/main/java/org/gensokyo/data/DataGeneratorWorkerApplication.java` — worker JVM entry
- `data-generator-service/src/main/java/org/gensokyo/data/task/DistributedJobService.java` — enqueue/lease orchestration
- `data-generator-service/src/main/java/org/gensokyo/data/task/DistributedJobLeaseRunner.java` — worker execution path
- `data-generator-service/src/main/java/org/gensokyo/data/task/DistributedJobWorker.java` — worker poller
- `data-generator-service/src/main/java/org/gensokyo/data/controller/TaskController.java` — `/task/run` enqueue trigger
- `data-generator-service/src/test/java/org/gensokyo/data/task/DistributedSplitRoleIntegrationTests.java` — single-JVM split-role reference template

### Config & existing recipes
- `data-generator-service/src/main/resources/application-distributed-staging.yaml` — shared file H2 + governance flags
- `data-generator-service/src/main/resources/application-distributed-coordinator.yaml` — coordinator role
- `data-generator-service/src/main/resources/application-distributed-worker.yaml` — worker role
- `data-generator-service/src/main/resources/service.env.example` — `DG_SERVICE_ROLE`, `DG_MAIN_CLASS` worker pattern
- `docs/staging-distributed-deployment.md` — C2 topology runbook (extend for DIST-01)
- `scripts/staging-distributed-smoke.ps1` — Maven pre-flight + optional REST smoke (reuse patterns, not primary gate)
- `scripts/e2e-distributed-podman.ps1` — optional Podman dual-JVM drill (supplement only)
- `scripts/lib/distributed-staging-rest.ps1` — REST enqueue/metrics helpers

### Harness
- `.planning/test-matrix.yaml` — add P1 row (do not edit P0 rows)
- `docs/test-harness.md` — P0 vs P1 semantics; verify-harness behavior
- `scripts/verify-harness.ps1` — matrix summary generation
- `AGENTS.md` — verify-script catalog + docs pointer home

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **C2 distributed stack is shipped:** `DistributedJobService`, `DistributedJobLeaseRunner`, coordinator enqueue via `TaskController`, worker poller in `DistributedJobWorker`, REST/console distributed metrics APIs
- **`DistributedSplitRoleIntegrationTests`** proves coordinator-does-not-run + worker executes — but **inside one JVM** with a capturing runner stub; Phase 15 must cross the **process boundary** with real execution
- **`application-distributed-staging.yaml`** already defines shared file H2 with `AUTO_SERVER=TRUE` for multi-JVM mounts
- **`staging-distributed-smoke.ps1`** and **`e2e-distributed-podman.ps1`** provide Maven pre-flight, REST enqueue smoke, and Podman dual-container patterns to adapt — avoid inventing a third topology
- **`scripts/lib/distributed-staging-rest.ps1`** — REST helpers for enqueue/metrics polling

### Established Patterns
- Embedded-first for ITs; multi-JVM requires **script + two processes** (research consensus)
- Harness changes: **P1 row + verify script**, P0 gate untouched (Phase 10/17 pattern)
- Docs packaging: single maintainer-facing doc section + AGENTS.md pointer (Phases 13–14)
- Windows builds via `mvnw-jdk25.ps1` + `.mvn/settings-jdk25.xml`

### Integration Points
- Coordinator enqueue: `TaskController.runById` → `DistributedJobService` → `distributed_job` + `task_execution` rows
- Worker claim: `DistributedJobWorker.pollAndRun` → `DistributedJobLeaseRunner.runLease` → `TemplateV2Runner.run` (same snap + runner stack as local path per Phase 7)
- Shared DB: both JVMs must see the same H2 file path; in-memory URLs **cannot** work across processes

</code_context>

<specifics>
## Specific Ideas

- Research flag resolved: **host two-JVM + file H2 script** wins over Podman-as-gate for DIST-01 (lower friction, embedded-first alignment); Podman stays documented optional path
- Poll timeout and worker `poll-delay-ms` should be tuned for deterministic local runs (planner may borrow values from `e2e-distributed-podman.ps1` worker Spring args)
- Success message in script output should echo instance/job ids for operator debugging

</specifics>

<deferred>
## Deferred Ideas

- **Full staging AC-1..AC-7 checklist** as phase DoD — belongs in staging ops sign-off, not DIST-01
- **Chaos / lease expiry / requeue drills** (AC-4/5) — optional future hardening
- **Second worker JVM** and load-split scenarios
- **Playwright distributed specs** (`distributed.spec.ts`, `distributed-job-detail.spec.ts`) as required DIST-01 gate
- **Podman-only** proof path as merge/harness requirement
- **P0 promotion** of multi-JVM row — defer to future milestone if flake-free and team agrees
- **RBAC-on distributed runs** — Phase 16

</deferred>

---
*Phase: 15-multi-jvm-worker-e2e*
*Discussed: 2026-07-29 (--auto)*
