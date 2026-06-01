# Phase C — Distributed Execution Implementation Plan

**Goal:** Deliver in-process partitioned parallelism (C1), then multi-node coordinator/worker (C2), per `docs/superpowers/specs/2026-05-29-v2-only-full-capability-design.md`.

**Entry:** Phase B merged to `master` (2026-06-01).

**Branch:** `feature-4.3`

## Epics → tasks

| Task | Epic | Deliverable |
|------|------|-------------|
| 1 | C1 | `ExecutionPolicyVO.partitionCount` + `partitionKey`; `EffectiveExecutionPolicy` resolution |
| 2 | C1 | `PartitionedComputeBlockRunner` with ForkJoinPool; hash/round-robin row split |
| 3 | C1 | `ComputeBlockRunner` delegates when `partitionCount > 1`; synchronized sink writes |
| 4 | C1 | Validator + `RunMetrics` partition counters |
| 5 | C1 | `PartitionedComputeBlockRunnerTests` — multi-partition correctness |
| 6 | C2 | `distributed_job` queue table, coordinator lease, worker heartbeat (future) |
| 7 | C2 | Worker process entrypoint + REST contract tests (future) |
| 8 | — | Console partition metrics (read-only, future) |

**C1 checkpoint:** Fixed dataset processed correctly across N partitions; metrics aggregate; single-partition path unchanged.

**C2 checkpoint (later):** Coordinator schedules job; worker executes and reports heartbeat; lease recovery on failure.

## File map (C1)

| Path | Action |
|------|--------|
| `data-generator-core/.../ExecutionPolicyVO.java` | Add `partitionCount`, `partitionKey` |
| `data-generator-calcite/.../EffectiveExecutionPolicy.java` | Resolve partition fields |
| `data-generator-calcite/.../RowPartitioner.java` | Create |
| `data-generator-calcite/.../RunMetricsSupport.java` | Create merge helper |
| `data-generator-calcite/.../PartitionedComputeBlockRunner.java` | Create |
| `data-generator-calcite/.../ComputeBlockRunner.java` | Delegate when partitioned |
| `data-generator-calcite/.../RunMetrics.java` | Partition counters |
| `data-generator-service/.../TemplateV2Validator.java` | Validate partitionCount |
| `data-generator-calcite/.../PartitionedComputeBlockRunnerTests.java` | Create |
