# Architecture Research

**Domain:** Brownfield Template V2 / Spring Boot data-generator — v2.1 hardening integration
**Researched:** 2026-07-25
**Confidence:** HIGH

> **Note:** CodeGraph is not initialized under this repo root (no `.codegraph/`). Symbol and call-path claims below come from direct source reads and prior v2.0 verification artifacts. Run `codegraph init -i` if future research should prefer MCP structural queries.

## Standard Architecture

### System Overview

v2.1 does **not** add a new domain module. It adds **proof, documentation, and reliability seams** on the v2.0 stack: console/legacy HTTP → task orchestration → Calcite V2 runner → managed JDBC catalog (with optional `snap:` routing) → sinks; optional distributed worker; opt-in console header RBAC; harness matrix.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Presentation                                                                 │
│  React console (/console)  │  Console REST /api/**  │  Legacy /task/**       │
│  ConsoleAuthorizationFilter (header RBAC; default OFF)                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Application / orchestration (data-generator-service)                         │
│  TemplateEditorRunSupport → TaskController                                   │
│  TaskExecutionService (queue, snapshot capture, status)                      │
│  DistributedJobService (enqueue / lease / heartbeat)  [optional]             │
│  DataSourceConfigService → ConnectionCatalog (+ ExecutionSnapshot overlay) │
├─────────────────────────────────────────────────────────────────────────────┤
│ Domain / execution (data-generator-calcite + adapters)                       │
│  WorkflowRunContext.bind(instanceId)                                         │
│  TemplateV2Runner → Chunked / Streaming / Workflow pipelines                 │
│  RuntimeJdbcEndpointResolver (DefaultRuntimeJdbcEndpointResolver)            │
│       └── ConnectionCatalog.resolve → routing key (logical | snap:…)         │
├─────────────────────────────────────────────────────────────────────────────┤
│ Infrastructure                                                               │
│  DynamicRoutingDataSource / Druid  │  H2 metadata  │  JDBC dialects          │
│  Harness: test-matrix.yaml → verify-harness.ps1 → harness-verify.yml (P0)  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| `TaskController` | HTTP/legacy run entry; V2 normalize/validate; queue; local async or distributed enqueue | `POST /task/run/{id}`, `runByIdAllowDraft` |
| `TemplateEditorRunSupport` / console template editors | Console `/api/templates/.../run` delegates to `TaskController` | Same execute path as legacy |
| `TaskExecutionService` | Persist execution row; capture connection snapshot JSON; mark RUNNING/terminal | Snapshot before `WorkflowRunContext.bind` |
| `TemplateV2Runner` | Policy resolve → pipeline execute | Calcite module; no HTTP awareness |
| `DataSourceConfigService` | Persist managed JDBC configs; sync catalog/routing | Operator CRUD + connectivity gate |
| `ConnectionCatalog` / `ExecutionSnapshotConnectionCatalog` | Resolve JDBC/Kafka/ES; `@Primary` snapshot overlay under active run | Returns `snap:{instanceId}:{name}` when bound |
| `DefaultRuntimeJdbcEndpointResolver` | **V2 execute-path** JDBC routing authority | Used by QuerySource / JdbcRowSink adapters |
| `JdbcCatalogResolver` | **Catalog-side** JDBC resolve helper (datasource module) | Parallel API; not injected on execute path |
| `DistributedJobService` + `DistributedJobLeaseRunner` | Queue + lease + heartbeat; worker/coordinator run leased row | Same snap + `TemplateV2Runner` as local path |
| `DataGeneratorWorkerApplication` | Second JVM entry (`distributed-worker` profile) | Claims leases via poller |
| `ConsoleSecurityProperties` + `ConsoleAuthorizationFilter` | Opt-in header RBAC on `/api/**` | `enabled=false` by default |
| Harness (`test-matrix.yaml`, `verify-harness.ps1`) | Capability rows → Maven/Playwright links; P0 merge gate | P1 tracked, non-blocking |

## Recommended Project Structure

No new top-level modules for v2.1. Touch points stay inside existing trees:

```
data-generator-service/
├── src/main/java/org/gensokyo/data/
│   ├── controller/TaskController.java          # HTTP execute entry (proof target)
│   ├── api/console/                            # Console run → TemplateEditorRunSupport
│   ├── task/                                   # TaskExecutionService, Distributed*
│   ├── config/DefaultRuntimeJdbcEndpointResolver.java
│   ├── config/ConsoleSecurityProperties.java
│   ├── security/ConsoleAuthorizationFilter.java
│   └── datasource/DataSourceConfigService.java
├── src/test/java/...                           # NEW/EXTENDED ITs for HTTP, RBAC, dist
data-generator-datasource/.../JdbcCatalogResolver.java   # docs/inventory only
data-generator-calcite/.../TemplateV2Runner.java         # unchanged entry; tests may drive
.planning/test-matrix.yaml                      # NEW P1 rows (not P0 inflation)
scripts/verify-harness.ps1                      # consume matrix; keep P0 semantics
docs/                                           # resolver ownership, Dameng green path, RBAC staging
```

### Structure Rationale

- **service:** Owns HTTP, orchestration, snap capture, distributed queue, console security — where v2.1 proofs attach.
- **datasource-jdbc:** Owns catalog-side resolver; v2.1 documents ownership, does not merge into service resolver.
- **calcite:** Runtime engine stays stable; proofs should go *through* service HTTP/worker paths when closing the Phase 11 gap.
- **.planning + scripts:** Harness remains the trust surface; P1 expands without raising merge bar.

## Architectural Patterns

### Pattern 1: Shared execute spine (local vs distributed)

**What:** Both in-process async (`TaskController.runV2Tracked`) and leased worker (`DistributedJobLeaseRunner`) follow: queue → mark RUNNING → `captureConnectionSnapshot` → `WorkflowRunContext.bind` → `templateV2Runner.run` → terminal status / report.

**When to use:** Any new proof that claims “operator run path” must hit this spine (HTTP enqueue at minimum), not only bare `TemplateV2Runner.run`.

**Trade-offs:** Async completion complicates ITs (poll status / sink COUNT). In-process `TemplateV2Runner` is faster but leaves the accepted Phase 11 HTTP gap open.

**Example (conceptual):**
```java
// TaskController.runV2 — local path
taskExecutionService.queueExecution(...);
if (distributedEnabled) {
    distributedJobService.enqueue(taskExecutionId, templateId, instanceId, null);
} else {
    executor.submit(() -> runV2Tracked(template, instanceId));
}
// runV2Tracked / DistributedJobLeaseRunner:
//   captureConnectionSnapshot → WorkflowRunContext.bind → templateV2Runner.run
```

### Pattern 2: Dual JDBC resolvers with ownership split (docs-only in v2.1)

**What:**
- `DefaultRuntimeJdbcEndpointResolver` — V2 execute-path authority; returns `connectionName()` after `ConnectionCatalog.resolve` (logical or `snap:{instanceId}:{name}`).
- `JdbcCatalogResolver` — catalog-module helper with parallel catalog-first / inline semantics; **not** the production execute injection.

**When to use:** Execute adapters call `RuntimeJdbcEndpointResolver`. Catalog/bootstrap/tests may use `JdbcCatalogResolver`. Consolidation is **out of scope**.

**Trade-offs:** Duplication risk vs blast radius of merging snap semantics into the datasource module. v2.1 closes risk with call-site inventory + docs, not a refactor.

### Pattern 3: Harness tiering (P0 gate vs P1 proof)

**What:** Matrix rows declare `tier`. `verify-harness.ps1` runs linked tests; CI blocks on `p0.pass` only. New hardening proofs land as **P1** (or opt-in/skipped-conditional) until stable.

**When to use:** HTTP catalog journey, distributed one-path, RBAC enable, Dameng live — track as P1; keep Dameng MERGE unit on existing P0 row.

**Trade-offs:** P1 can go red without blocking merge — intentional for flaky/multi-JVM/live paths.

### Pattern 4: Opt-in security filter

**What:** `ConsoleAuthorizationFilter.shouldNotFilter` returns true when `console-security.enabled=false`. When enabled, role from `X-Console-Role` maps to `ConsolePermission` on `/api/**`.

**When to use:** Staging/e2e profiles and `ConsoleAuthorizationIntegrationIT`. Never flip default for local/dev.

**Trade-offs:** Intranet header model is not IdP; default-off avoids breaking console e2e and developer loops.

## Data Flow

### Request Flow — operator run (v2.1 HTTP proof target)

```
Operator (console or client)
    ↓ POST /api/templates/{id}/run  OR  POST /task/run/{id}
ConsoleAuthorizationFilter (skip if RBAC off)
    ↓
TemplateEditorRunSupport → TaskController.runById / runByIdAllowDraft
    ↓
TaskExecutionService.queueExecution (+ lineage)
    ↓ (local)                    ↓ (distributed.enabled)
async runV2Tracked               DistributedJobService.enqueue
    ↓                            ↓ worker leaseNext + heartbeat
captureConnectionSnapshot
WorkflowRunContext.bind(instanceId)
    ↓
TemplateV2Runner.run
    ↓
DefaultRuntimeJdbcEndpointResolver → ConnectionCatalog
    → routing key (snap:… when bound) → DynamicRoutingDataSource → JDBC source/sink
    ↓
TaskExecutionService mark SUCCESS/FAILED + RunReportCollector
```

### State Management

```
TaskExecutionPO (instanceId, status, connectionSnapshotJson, report)
    ↕
activeSnapshots (in-memory cache on TaskExecutionService)
    ↕
ExecutionSnapshotConnectionCatalog (@Primary) overlays live catalog for snap: keys
```

Distributed:
```
DistributedJobPO (QUEUED → LEASED/RUNNING → SUCCESS/FAILED)
    ↕ lease / heartbeat / requeue via DistributedJobRepository
```

### Key Data Flows

1. **Managed JDBC save → catalog:** `DataSourceConfigService.save` → `ConnectionCatalog` + routing registration (Phase 11 IT already proves in-process run; v2.1 extends through HTTP enqueue).
2. **In-flight isolation:** Run-start snapshot + bound context → `snap:` routing keys retained across mid-flight catalog reload (`JdbcSnapshotExecutePathIT`).
3. **Distributed claim:** Coordinator enqueue → worker `leaseNext` → `DistributedJobLeaseRunner` (same snap + runner) → heartbeat until terminal.
4. **Harness evidence:** Matrix row → linked Maven/Playwright class → `target/test-matrix-summary.json` → P0 gate.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Single-node / local | Default: distributed off; in-process executor; RBAC off; H2 metadata |
| Staging dual-JVM | Enable `data.generator.distributed.*`; worker profile JVM; one happy-path E2E (v2.1) |
| Larger fleets | Existing lease/heartbeat already support multi-worker claim; **not** a v2.1 expand — avoid AC-1..AC-7 CI gate |

### Scaling Priorities (hardening lens)

1. **First bottleneck for trust:** Missing HTTP execute proof — operators do not run via in-process runner; close with MockMvc/`@SpringBootTest` journey.
2. **Second bottleneck:** Multi-JVM flakiness if over-scoped — ship **one** path as P1; keep full staging checklist in docs.

## Anti-Patterns

### Anti-Pattern 1: Prove catalog only via bare `TemplateV2Runner`

**What people do:** Reuse Phase 11 IT style (unbound runner, logical catalog name) and call the HTTP gap “closed.”

**Why it's wrong:** Leaves the accepted audit limit open; does not exercise queue, snapshot bind, or async completion.

**Do this instead:** HTTP `/task/run` or console run → wait for terminal → assert sink rows / report; expect `snap:` under bound context when asserting resolver keys.

### Anti-Pattern 2: Merge dual JDBC resolvers “while documenting”

**What people do:** Consolidate `JdbcCatalogResolver` into `DefaultRuntimeJdbcEndpointResolver` in the same milestone as ownership docs.

**Why it's wrong:** High blast radius on DS-03 snap semantics; contradicts explicit out-of-scope.

**Do this instead:** Call-site inventory + maintainer docs; defer merge to a dedicated refactor milestone.

### Anti-Pattern 3: Inflate P0 with live/multi-JVM proofs

**What people do:** Add Dameng live IT or Podman dual-JVM as P0 merge blockers.

**Why it's wrong:** Licensed drivers / container cost / flake break the 15-row gate that currently protects streaming/upsert/dialect unit paths.

**Do this instead:** Focused **P1** rows; keep Dameng MERGE unit on existing P0; live/dist as opt-in or skipped-conditional.

### Anti-Pattern 4: Default-on console RBAC

**What people do:** Flip `ConsoleSecurityProperties.enabled` to true globally.

**Why it's wrong:** Breaks local/dev and existing console e2e assumptions built around open `/api/**`.

**Do this instead:** Document staging/e2e enable; keep `ConsoleAuthorizationIntegrationIT` green when enabled.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Managed JDBC engines (H2/PG/MySQL/KB/HG/CK/DM) | Catalog + dialect SQL builder + Testcontainers/opt-in | DM live remains `-Ddm.it=true` / env gate |
| Kafka / ES | Catalog resolve via TemplateV2RuntimeServices | Out of v2.1 hardening focus |
| Podman (distributed e2e) | Existing `e2e-distributed-podman.ps1` / staging docs | Link one path to harness P1 |
| CI (GitHub Actions) | `harness-verify.yml` reads `p0.pass` | Do not change P0 semantics for flaky paths |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Console `/api` ↔ `TaskController` | Direct bean call via `TemplateEditorRunSupport` | Same spine as legacy `/task` |
| Service ↔ Calcite | `TemplateV2Runner` + `RuntimeJdbcEndpointResolver` bean | Snap behavior only when context bound |
| Service ↔ datasource-jdbc | Shared `ConnectionCatalog` API; dual resolvers coexist | Docs-only ownership in v2.1 |
| Coordinator ↔ Worker | DB-backed `DistributedJobPO` + optional REST lease API | Worker reuses `DistributedJobLeaseRunner` |
| Harness ↔ modules | Matrix `owner_module` + `linked_tests` | P1 for new proofs; P0 unchanged count/semantics |

## New vs Modified (v2.1)

| Area | New | Modified |
|------|-----|----------|
| HTTP managed-catalog / dialect journey | IT(s), possibly matrix P1 row, UAT notes | Thin glue only if polling helpers missing; **not** new run API |
| Dameng live IT + Nyquist | VALIDATION.md hygiene; green-path docs | `ChunkedPipelineDamengUpsertIT` / `DamengTestSupport` wiring (opt-in) |
| Resolver ownership | Docs + call-site inventory artifact | Javadoc cross-links if needed — **no class merge** |
| Multi-JVM worker E2E | One harness-linked evidence path (script/IT) | Config/docs; reuse `DistributedJobService` / worker app |
| Console RBAC | Staging/e2e enable docs; optional P1 matrix link | Keep default `enabled=false`; ensure existing IT green |
| Harness | Focused **P1** rows for new proofs | Do **not** expand P0 gate for live/dist-only paths |

## Suggested Build Order

Dependency-aware order (aligns with FEATURES.md P1→P2):

1. **HTTP execute-path proof** (managed catalog ± dialect) — closes highest-value trust gap; establishes spine for later matrix rows.  
   *Verify:* MockMvc/`@SpringBootTest` → `/task/run` or console run → terminal SUCCESS → sink `COUNT(*)` (or dialect-aware write).
2. **Dameng live IT green path + Nyquist hygiene** — can parallelize docs/VALIDATION with (1); live IT needs env.  
   *Verify:* `-Ddm.it=true` documented green; VALIDATION backfill for 07 / 07.1 / 08.
3. **Resolver ownership docs + call-site inventory** — low coupling; do early to prevent accidental merge PRs during IT work.  
   *Verify:* Inventory lists execute-path vs catalog-side callers; no resolver merge diff.
4. **Multi-JVM worker E2E one path** — after HTTP spine familiarity; reuse distributed scripts.  
   *Verify:* Coordinator enqueue → worker lease → SUCCESS; harness P1 link.
5. **RBAC testable enable path** — independent of JDBC; document staging; keep default off.  
   *Verify:* `ConsoleAuthorizationIntegrationIT` green with enable profile; docs for e2e/staging.
6. **Focused P1 harness expansion** — last: wire rows for proofs that already exist.  
   *Verify:* `verify-harness.ps1` still `p0.pass=true` (15 P0); new P1 rows reported in summary.

```
1. HTTP catalog/dialect proof     → verify: HTTP→SUCCESS→rows
2. Dameng green path + Nyquist    → verify: opt-in IT + VALIDATION files
3. Resolver docs/inventory        → verify: docs only, no merge
4. Distributed one-path E2E       → verify: dual-JVM SUCCESS + P1 link
5. RBAC enable path (default off) → verify: IT + staging docs
6. P1 harness rows                → verify: p0.pass unchanged; P1 tracked
```

## Sources

- `.planning/PROJECT.md` — v2.1 goals / out of scope
- `.planning/research/FEATURES.md` — feature landscape and priority matrix
- `.planning/milestones/v2.0-MILESTONE-AUDIT.md` — accepted HTTP limit, Dameng opt-in, dual resolvers, Nyquist partial
- `.planning/milestones/v2.0-REQUIREMENTS.md` — DIST-01 deferred; DS/RW/TEST shipped
- Code: `TaskController`, `TemplateEditorRunSupport`, `TaskExecutionService`, `TemplateV2Runner`, `DefaultRuntimeJdbcEndpointResolver`, `JdbcCatalogResolver`, `DataSourceConfigService`, `ExecutionSnapshotConnectionCatalog`, `DistributedJobService`, `DistributedJobLeaseRunner`, `DataGeneratorWorkerApplication`, `ConsoleSecurityProperties`, `ConsoleAuthorizationFilter`
- Harness: `.planning/test-matrix.yaml`, `scripts/verify-harness.ps1`, `AGENTS.md` merge criteria
- Phase artifacts: 07.1 / 11 RESEARCH & VERIFICATION (snap: ownership; ManagedJdbcCatalogSinkE2eIT limits)

---
*Architecture research for: data-generator v2.1 Hardening & Weak-Spot Closure*
*Researched: 2026-07-25*
