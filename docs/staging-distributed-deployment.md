# Staging — Distributed Coordinator + Worker Deployment

Deploy Phase C2 multi-node execution on staging with **one Coordinator JVM** (enqueue only) and **one or more Worker JVMs** (poll and execute), sharing a single database.

## Topology

```text
Coordinator (DataGeneratorApplication)  →  distributed_job (DB)  ←  Worker(s) (DataGeneratorWorkerApplication)
```

## JVM roles

| Role | Main class | Spring profile / config |
|------|------------|-------------------------|
| Coordinator | `org.gensokyo.data.DataGeneratorApplication` | `distributed-coordinator` → `application-distributed-coordinator.yaml` |
| Worker | `org.gensokyo.data.DataGeneratorWorkerApplication` | `distributed-worker` → `application-distributed-worker.yaml` |

### Coordinator

```bash
java -jar data-generator-service.jar \
  --spring.profiles.active=distributed-coordinator
```

Required properties (from profile):

- `data.generator.distributed.enabled=true`
- `data.generator.distributed.worker-enabled=false`
- `data.generator.distributed.coordinator-poll-enabled=false`

V2 `/task/run` enqueues `task_execution` + `distributed_job` without running templates locally.

### Worker

```bash
java -jar data-generator-service.jar \
  --spring.profiles.active=distributed-worker
```

Set a **unique** `data.generator.distributed.worker-id` per JVM (profile uses `${HOSTNAME:worker-local}`).

Optional tuning:

```yaml
data:
  generator:
    distributed:
      lease-seconds: 30
      heartbeat-interval-ms: 10000   # default: leaseSeconds * 1000 / 3
      max-attempts: 3
      requeue-on-failure: true
      poll-delay-ms: 2000
```

## Shared database

Coordinator and all workers must use the **same JDBC URL** and schema (`distributed_job`, `task_execution`, `template`). Workers lease rows via optimistic `tryAcquireLease`; do not run multiple workers against isolated databases.

## Console (read-only observability)

| Endpoint | Purpose |
|----------|---------|
| `GET /api/console/distributed/metrics` | Queue depth by status, active workers |
| `GET /api/jobs/{instanceId}` | Job detail with `distributedJob` and partition metrics |

Console is served from the Coordinator (or any node with console enabled); metrics read the shared DB.

## Staging acceptance checklist

| ID | Check |
|----|--------|
| AC-1 | Coordinator starts with `distributed-coordinator` profile; V2 run creates `QUEUED` row; template does **not** execute on Coordinator |
| AC-2 | Worker consumes queue; job ends `SUCCESS`; Console job detail shows distributed row |
| AC-3 | Cancel before worker run → `CANCELLED` on job and execution |
| AC-4 | Kill worker mid-lease; after lease expiry another worker completes or fails deterministically |
| AC-5 | Long-running template (> `leaseSeconds`) completes without lease steal (heartbeat extends lease) |
| AC-6 | Simulated failure requeues until `max-attempts`, then `FAILED` |
| AC-7 | `feature-4.3` merged to `master`; this runbook and profiles present under `docs/` and `classpath:` |

## Smoke procedure (manual)

1. Start Coordinator with coordinator profile against staging DB.
2. Start Worker with `distributed-worker` profile and unique `worker-id`.
3. Trigger a small V2 template via REST or Console; confirm Coordinator logs show enqueue only.
4. Confirm Worker logs show lease + success; query `distributed_job.status = SUCCESS`.
5. Open Console metrics and job detail for the instance id.
6. (Optional) Set `max-attempts=1` on a test worker and force template failure → `FAILED` without requeue.

## References

- Design: `docs/superpowers/specs/2026-06-01-c2-staging-closure-design.md`
- Implementation plan: `docs/superpowers/plans/2026-06-01-c2-staging-closure.md`
