# Phase C2 Staging Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close Phase C2 for staging with Coordinator enqueue-only plus Worker JVM execution, execution-period heartbeat, bounded failure requeue, split-role tests, and deployment docs.

**Architecture:** Keep shared-DB queue model on `feature-4.3` baseline. Add coordinator-only Spring profile, extend `DistributedExecutionProperties` and `DistributedJobService` for retry/heartbeat, enhance `DistributedJobLeaseRunner` with periodic heartbeat during `TemplateV2Runner.run`, verify with integration tests and staging checklist.

**Tech Stack:** Java 25, Spring Boot 4.x, H2/PostgreSQL (staging), Maven `mvnw-jdk25.ps1`, existing `distributed_job` table.

**Spec:** `docs/superpowers/specs/2026-06-01-c2-staging-closure-design.md`

---

## File map

| Path | Responsibility |
|------|----------------|
| `DistributedExecutionProperties.java` | `heartbeatIntervalMs`, `maxAttempts`, `requeueOnFailure` |
| `DistributedJobService.java` | `requeueAfterFailure`, adjust `markFailed` or call requeue from runner |
| `DistributedJobLeaseRunner.java` | Scheduled heartbeat during run; invoke requeue on failure path |
| `application-distributed-coordinator.yaml` | Coordinator enqueue-only defaults |
| `application-distributed-worker.yaml` | Document `max-attempts`, heartbeat |
| `docs/staging-distributed-deployment.md` | Staging runbook + AC checklist |
| `2026-05-29-v2-phase-c-distributed.md` | Mark tasks 6–8 done; link closure spec |
| `DistributedJobRequeueIntegrationTests.java` | Failure → requeue → success IT |
| `DistributedSplitRoleIntegrationTests.java` | Worker-only poll in split config |
| `SlowTemplateV2Runner` (test config) | Delayed runner for heartbeat IT |

---

### Task 1: Coordinator profile and docs (P0)

**Files:**
- Create: `data-generator-service/src/main/resources/application-distributed-coordinator.yaml`
- Create: `docs/staging-distributed-deployment.md`
- Modify: `docs/superpowers/plans/2026-05-29-v2-phase-c-distributed.md`

- [ ] **Step 1: Add coordinator profile YAML**

```yaml
# Profile: distributed-coordinator (use with DataGeneratorApplication)
data:
  generator:
    distributed:
      enabled: true
      worker-enabled: false
      coordinator-poll-enabled: false
```

- [ ] **Step 2: Write staging deployment doc** covering: JVM count, `spring.profiles.active=distributed-coordinator` vs `DataGeneratorWorkerApplication`, shared DB, unique `worker-id`, Console URLs, AC-1–AC-7 from spec.

- [ ] **Step 3: Update phase-c plan** — tasks 6–8 marked complete; add link to `2026-06-01-c2-staging-closure-design.md`.

- [ ] **Step 4: Commit**

```bash
git add data-generator-service/src/main/resources/application-distributed-coordinator.yaml docs/staging-distributed-deployment.md docs/superpowers/plans/2026-05-29-v2-phase-c-distributed.md
git commit -m "docs(service): add distributed coordinator profile and staging runbook"
```

---

### Task 2: Execution properties for heartbeat and retry (P1/P2 config)

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/config/DistributedExecutionProperties.java`

- [ ] **Step 1: Add fields with defaults**

```java
/** Heartbeat period during RUNNING (ms). When <= 0, derived from leaseSeconds. */
private long heartbeatIntervalMs = 0;

/** Max lease attempts before terminal FAILED. */
private int maxAttempts = 3;

/** When true, markFailed may return job to QUEUED if attempts remain. */
private boolean requeueOnFailure = true;

public long resolvedHeartbeatIntervalMs() {
    if (heartbeatIntervalMs > 0) {
        return heartbeatIntervalMs;
    }
    return Math.max(5_000L, (leaseSeconds * 1000L) / 3);
}
```

- [ ] **Step 2: Commit**

```bash
git add data-generator-service/src/main/java/org/gensokyo/data/config/DistributedExecutionProperties.java
git commit -m "feat(service): add distributed heartbeat and retry properties"
```

---

### Task 3: Requeue-after-failure in DistributedJobService (P2)

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/task/DistributedJobService.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/task/DistributedJobRequeueIntegrationTests.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void markFailedRequeuesWhenAttemptsRemain() {
    Long jobId = distributedJobService.enqueue(1L, 2L, 3L, null);
    distributedJobService.leaseNext("w1", 30);
    distributedJobService.markRunning(jobId, "w1");
    distributedJobService.markFailedWithRetryPolicy(jobId, "w1", "boom", 3, true);
    DistributedJobPO row = repository.findById(jobId).orElseThrow();
    assertEquals(DistributedJobStatus.QUEUED.name(), row.getStatus());
    assertNull(row.getWorkerId());
}
```

Adjust method name to match implementation.

- [ ] **Step 2: Run test — expect FAIL**

```bash
.\mvnw-jdk25.ps1 -pl "data-generator-service" -am test "-Dtest=DistributedJobRequeueIntegrationTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- [ ] **Step 3: Implement `markFailedWithRetryPolicy`**

After `requireOwned`, read `attempts`. If `requeueOnFailure && attempts < maxAttempts`, set status `QUEUED`, clear `workerId`, set `errorMessage`, clear `leaseUntil`/`finishedAt`, `saveAndFlush`. Else existing `markTerminal(FAILED)`.

Update `DistributedJobLeaseRunner` catch block to call `markFailedWithRetryPolicy` with properties instead of `markFailed`.

- [ ] **Step 4: Run test — expect PASS**

- [ ] **Step 5: Add test `markFailedTerminalWhenMaxAttemptsExceeded`**

- [ ] **Step 6: Commit**

```bash
git add data-generator-service/src/main/java/org/gensokyo/data/task/DistributedJobService.java data-generator-service/src/test/java/org/gensokyo/data/task/DistributedJobRequeueIntegrationTests.java data-generator-service/src/main/java/org/gensokyo/data/task/DistributedJobLeaseRunner.java
git commit -m "feat(service): requeue distributed jobs on failure within max attempts"
```

---

### Task 4: Execution-period heartbeat (P1)

**Files:**
- Modify: `data-generator-service/src/main/java/org/gensokyo/data/task/DistributedJobLeaseRunner.java`
- Create: `data-generator-service/src/test/java/org/gensokyo/data/task/DistributedJobHeartbeatIntegrationTests.java`

- [ ] **Step 1: Write failing test** with `@Primary` runner that sleeps 2s; `leaseSeconds=1`, `heartbeatIntervalMs=500`; assert `last_heartbeat_at` updated twice during run.

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Implement heartbeat loop in `executeTrackedRun`**

```java
ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
ScheduledFuture<?> heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
        () -> {
            try {
                distributedJobService.heartbeat(jobId, workerId, leaseSeconds);
            } catch (Exception ignored) {
                // lease lost — run will fail on next check
            }
        },
        distributedExecutionProperties.resolvedHeartbeatIntervalMs(),
        distributedExecutionProperties.resolvedHeartbeatIntervalMs(),
        TimeUnit.MILLISECONDS);
try {
    TemplateV2RunResult result = templateV2Runner.run(template);
    // ... existing success path
} finally {
    heartbeatTask.cancel(true);
    heartbeatExecutor.shutdownNow();
}
```

Inject `DistributedExecutionProperties` into `DistributedJobLeaseRunner`.

- [ ] **Step 4: Run test — expect PASS**

- [ ] **Step 5: Commit**

```bash
git add data-generator-service/src/main/java/org/gensokyo/data/task/DistributedJobLeaseRunner.java data-generator-service/src/test/java/org/gensokyo/data/task/DistributedJobHeartbeatIntegrationTests.java
git commit -m "feat(service): heartbeat during distributed job execution"
```

---

### Task 5: Split-role integration test (P3)

**Files:**
- Create: `data-generator-service/src/test/java/org/gensokyo/data/task/DistributedSplitRoleIntegrationTests.java`

- [ ] **Step 1: SpringBootTest properties**

```java
properties = {
    "spring.config.location=classpath:/application-phase7-test.yaml",
    "data.generator.distributed.enabled=true",
    "data.generator.distributed.worker-enabled=true",
    "data.generator.distributed.coordinator-poll-enabled=false"
}
```

- [ ] **Step 2: Test `coordinatorDoesNotPollWorkerExecutes`**

- Autowire `TaskController`, `DistributedJobWorker`, assert `DistributedJobCoordinator` bean **not** present OR document optional bean — use `@Autowired(required = false) DistributedJobCoordinator coordinator` assert null when `coordinator-poll-enabled=false`.

- `runById` → job QUEUED → `worker.pollAndRun()` → SUCCESS.

- [ ] **Step 3: Run tests**

```bash
.\mvnw-jdk25.ps1 -pl "data-generator-service" -am test "-Dtest=DistributedSplitRoleIntegrationTests,DistributedJob*IntegrationTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- [ ] **Step 4: Commit**

---

### Task 6: Merge to master and verify (P0)

- [ ] **Step 1: Full service module test**

```bash
.\mvnw-jdk25.ps1 -pl "data-generator-service" -am test
```

- [ ] **Step 2: Merge `feature-4.3` → `master` via team PR process**

- [ ] **Step 3: Staging smoke per `docs/staging-distributed-deployment.md`**

---

## Plan self-review (spec coverage)

| Spec requirement | Task |
|------------------|------|
| FR-1 Coordinator enqueue-only | Task 1 profile + Task 5 IT |
| FR-2 Worker execution | Task 5 (existing worker ITs) |
| FR-3 Heartbeat | Task 4 |
| FR-4 Retry/requeue | Task 3 |
| FR-5 Observability | Existing; Task 6 staging verify |
| AC-1–AC-7 | Task 1 doc + Task 6 |

No placeholders remain in task steps above.
