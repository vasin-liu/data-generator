---
phase: 07-datasource-governance-hot-reload
plan: 02
completed: 2026-06-27
---

# Phase 07 Plan 02 Summary

Run-start connection snapshots, snapshot-scoped catalog resolve, and save-triggered hot-reload with DEGRADED fallback (DS-03 Wave 2).

## Task 1: Connection snapshot at RUNNING

- Added `ConnectionSnapshotSupport` — walks Template V2 sources/sinks (JDBC, Kafka, ES + inline blocks), pins catalog version/source tags
- Extended `TaskExecutionService` with `captureConnectionSnapshot`, `persistConnectionSnapshot`, in-process cache, DB fallback for worker path, cache eviction on terminal status
- Wired snapshot capture in `TaskController.runV2Tracked` and `DistributedJobLeaseRunner.runLease` after `markRunning`
- Tests: `ConnectionSnapshotSupportTests`, `ConnectionSnapshotIT`

## Task 2: Snapshot-scoped catalog + hot-reload + DEGRADED

- Added `ExecutionSnapshotConnectionCatalog` (`@Primary`) — resolves from execution snapshot when `WorkflowRunContext.instanceId()` is set; isolated JDBC routing keys (`snap:{instanceId}:{name}`)
- Added `HotReloadCoordinator` — JDBC/Kafka/ES reload with HEALTHY/DEGRADED state overlay and last-known-good retention on failure
- Added `SnapshotConnectionMaterializer` for param-only handle materialization
- Implemented `ConnectionCatalogImpl.reload()` delegating to coordinator; health overlays merged in `listAll`
- Updated `DataSourceConfigService.save` and `MessagingClusterConfigService.saveKafka/saveElasticsearch` to call `catalog.reload()`
- Updated `CoreConfig` fallback bean; `ConnectionCatalogImplTests` constructor for coordinator dependency
- Tests: `HotReloadTests` (in-flight isolation, new-run reload, DEGRADED fallback)

## Verification

```text
.\mvnw-jdk25.ps1 -pl data-generator-service -am test \
  -Dtest=ConnectionSnapshotSupportTests,ConnectionSnapshotIT,HotReloadTests,ExecutionConnectionSnapshotTests \
  -Dsurefire.failIfNoSpecifiedTests=false -q
```

Result: **PASS** (9 tests)

## Deferred to 07-03

- `ConnectionCatalog.test()` implementation (connectivity gate)
- `DATASOURCE_RELOAD` audit emission
- Governance policy enforcement
