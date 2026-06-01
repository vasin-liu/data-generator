# Phase C2 — Staging Closure Design

## Metadata

| Field | Value |
|-------|-------|
| Status | **Approved** (2026-06-01) |
| Driver | Product priority **A**: multi-node distributed execution stable on staging |
| Branch baseline | `feature-4.3` (merge to `master` as first deliverable) |
| Parent spec | `docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md` (Phase C) |
| Supersedes plan gaps | `docs/superpowers/plans/2026-05-29-v2-phase-c-distributed.md` tasks 6–8 marked “future” |

## Problem statement

Phase C2 foundation exists on `feature-4.3`: `distributed_job` queue, lease/heartbeat REST, embedded `DistributedJobCoordinator`, standalone `DataGeneratorWorkerApplication`, Console read-only metrics, and integration tests for enqueue, cancel-before-run, lease recovery, and worker polling.

Staging cannot yet rely on **true split roles** (Coordinator enqueue-only + remote Workers execute) with production-grade lease behavior (long-run heartbeat, bounded retry). The phase plan document is stale relative to code.

## Goal

Deploy on staging with **one Coordinator JVM** (enqueue only) and **one or more Worker JVMs** (poll and execute), sharing one database, meeting explicit acceptance criteria below.

## Non-goals (this iteration)

| Item | Rationale |
|------|-----------|
| HTTP-only Worker without DB | Higher latency and auth scope; defer |
| Kubernetes Job / Operator adapter | Phase C optional; not required for staging |
| Phase D template pipeline / artifact store | Far-term |
| Splitting C1 partitions into multiple queue rows | C1 remains in-process inside one job |
| V1 execution retirement (`v1-execution.enabled=false`) | Separate ops track |

## Deployment topology

```text
┌─────────────────────┐     enqueue      ┌──────────────┐
│ Coordinator JVM     │ ───────────────► │ distributed_ │
│ DataGeneratorApp    │                  │ job (DB)     │
│ poll disabled       │                  └──────┬───────┘
└─────────────────────┘                         │
                                                  │ lease / heartbeat
                    ┌─────────────────────────────┼─────────────────────────────┐
                    ▼                             ▼                             ▼
            ┌───────────────┐             ┌───────────────┐             ┌───────────────┐
            │ Worker JVM 1  │             │ Worker JVM 2  │             │ Worker JVM N  │
            │ WorkerApp +   │             │ same profile  │             │ unique        │
            │ profile       │             │               │             │ worker-id     │
            └───────────────┘             └───────────────┘             └───────────────┘
```

### Configuration matrix

| Role | Main class | Profile / YAML | `data.generator.distributed` |
|------|------------|----------------|------------------------------|
| Coordinator | `DataGeneratorApplication` | `application-distributed-coordinator.yaml` (new) | `enabled=true`, `worker-enabled=false`, `coordinator-poll-enabled=false` |
| Worker | `DataGeneratorWorkerApplication` | `distributed-worker` (existing) | `enabled=true`, `worker-enabled=true`, `coordinator-poll-enabled=false`, unique `worker-id` |

Workers and Coordinator use the **same JDBC datasource** and schema (`distributed_job`, `task_execution`, `template`).

## Functional requirements

### FR-1 — Coordinator enqueue-only

When `data.generator.distributed.enabled=true` and `coordinator-poll-enabled=false`:

- `/task/run` (V2) inserts `task_execution` and `distributed_job` (status `QUEUED`).
- No local `TemplateV2Runner` invocation on the Coordinator JVM.
- `DistributedJobCoordinator` bean may be absent or never polls (conditional property already supported).

### FR-2 — Worker execution

When `worker-enabled=true`:

- `DistributedJobWorker` polls `leaseNext`, then `DistributedJobLeaseRunner.runLease`.
- Terminal states update both `distributed_job` and `task_execution` (existing behavior).
- Cancel-before-run and cancel-during-run remain supported via `cancel_requested` on `task_execution`.

### FR-3 — Execution-period heartbeat

Long-running jobs must not lose lease while status is `RUNNING`.

- New property: `data.generator.distributed.heartbeat-interval-ms` (default: `leaseSeconds * 1000 / 3`, minimum 5_000).
- `DistributedJobLeaseRunner` invokes `distributedJobService.heartbeat` on that interval during `templateV2Runner.run`.
- Heartbeat stops when run completes or throws.

### FR-4 — Bounded retry / requeue

- New properties:
  - `data.generator.distributed.max-attempts` (default `3`)
  - `data.generator.distributed.requeue-on-failure` (default `true`)
- On worker failure (`markFailed`): if `attempts < max-attempts` and `requeue-on-failure`, transition job to `QUEUED`, clear `worker_id`, keep last `error_message` truncated.
- If max attempts exceeded: remain `FAILED`; `task_execution` stays failed.
- Expired-lease re-acquisition (existing) counts as new `attempts` increment on `tryAcquireLease`.

### FR-5 — Observability (existing, verify on staging)

- `GET /api/console/distributed/metrics` — queue depth by status, active workers.
- `GET /api/jobs/{instanceId}` — `JobExecutionDetail` with `distributedJob` and `partitionMetrics` when present.

## Staging acceptance criteria

| ID | Criterion |
|----|-----------|
| AC-1 | Coordinator starts with coordinator profile; V2 run enqueues row; no template execution on Coordinator |
| AC-2 | Worker consumes queue; job ends `SUCCESS`; Console job detail shows distributed row |
| AC-3 | Cancel before worker run → `CANCELLED` on job and execution |
| AC-4 | Kill worker mid-lease; after lease expiry second worker completes or fails deterministically |
| AC-5 | Template with artificial delay (> `leaseSeconds`) still completes without lease steal |
| AC-6 | Simulated failure requeues until `max-attempts`, then `FAILED` |
| AC-7 | `feature-4.3` merged to `master`; staging YAML checked in under `docs/` |

## Testing strategy

| Layer | Scope |
|-------|--------|
| Unit | `DistributedJobService.requeueAfterFailure` (new), heartbeat scheduler in lease runner |
| Integration | Split-role Spring test: coordinator poll off, worker poll on; retry requeue IT; extended heartbeat IT with slow runner |
| Manual | `docs/staging-distributed-deployment.md` checklist |

Commands (Windows):

```bash
.\mvnw-jdk25.ps1 -pl "data-generator-service" -am test "-Dtest=DistributedJob*"
```

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Duplicate execution if lease logic regresses | Keep `tryAcquireLease` optimistic locking; IT for expired lease |
| Coordinator accidentally polls | `coordinator-poll-enabled=false` in coordinator profile; IT asserts no runner on coordinator context |
| DB clock skew | Use DB/application `Instant.now()` consistently; document NTP on staging |

## References

- `DistributedJobLeaseRunner`, `DistributedJobWorker`, `DistributedJobCoordinator`
- `application-distributed-worker.yaml`
- Integration tests: `DistributedJobCoordinatorIntegrationTests`, `DistributedJobWorkerIntegrationTests`, `DistributedJobCoordinatorCancelIntegrationTests`
