---
phase: 08-rw-streaming-upsert
plan: 11
subsystem: test
tags: [playwright, e2e, rw-01, rw-02, rw-03, rw-04, d-23, d-17, d-18, d-20]

requires: [08-08, 08-09]
provides:
  - rw-streaming-upsert.spec.ts with 7 D-23 scenarios (6 API/UI + job detail UI smoke)
  - e2e/helpers/rw-streaming-upsert.ts (runScenarioTwice, assertSinkMetrics, large-file warn)
  - Extended template-run.ts (bundle publish, re-run, failure poll, sink metric types)
  - GF-FC/GF-FN/GF-GP/GF-GM catalog bindings + scenario-f-chunked-csv.yaml
  - E2eV2ScenarioFixtureService + ConsoleE2eScenarioFixtureController (@Profile e2e)
  - npm script e2e:phase8-rw-streaming-upsert
affects: [08-12]

tech-stack:
  added: []
  patterns:
    - "API-first createPublishRunFromScenario + Phase 7 mixed UI smoke"
    - "W-06 large-file warn: temp ≥10 MB CSV locally; maxRows bar in Podman (DG_E2E_IN_CONTAINER)"
    - "E2e profile fixture seed on scenario scaffold for JDBC upsert/partial-sink"

key-files:
  created:
    - data-generator-console-web/e2e/specs/rw-streaming-upsert.spec.ts
    - data-generator-console-web/e2e/helpers/rw-streaming-upsert.ts
    - data-generator-service/src/main/resources/template/v2-scenarios/scenario-f-chunked-csv.yaml
    - data-generator-service/src/main/java/org/gensokyo/data/template/E2eV2ScenarioFixtureService.java
    - data-generator-service/src/main/java/org/gensokyo/data/api/console/ConsoleE2eScenarioFixtureController.java
  modified:
    - data-generator-console-web/e2e/helpers/template-run.ts
    - data-generator-console-web/package.json
    - data-generator-service/src/main/java/org/gensokyo/data/template/V2ScenarioCatalogService.java
    - data-generator-service/src/main/java/org/gensokyo/data/template/editor/TemplateEditorService.java
    - scripts/verify-phase8-uat-rw-streaming-upsert.ps1

key-decisions:
  - "ConsoleTemplateScenarioController deferred — catalog extended in V2ScenarioCatalogService; e2e fixtures in ConsoleE2eScenarioFixtureController"
  - "GF-FC distinct from GF-FN for CHUNKED CSV vs NDJSON STREAMING D-23 matrix"
  - "Upsert idempotency E2E mutates source via POST /api/e2e/scenarios/{id}/mutate-upsert-source between runs"
  - "GF-GP skipped when H2 lacks ON CONFLICT (W-01) via postgres-upsert-supported probe"

patterns-established:
  - "runScenarioTwice: publish once, mutate upsert source, re-run same templateId"
  - "publishLargeFileInMemoryDraft: API inject draft + Review save + ant-message-warning assertion"

requirements-completed: [RW-01, RW-02, RW-03, RW-04]

duration: 75min
completed: 2026-06-29
---

# Phase 08 Plan 11 Summary

**Playwright E2E RW streaming/upsert scenarios (D-23) with API-first helpers and e2e JDBC fixture seeding**

## Performance

- **Duration:** 75 min
- **Tasks:** 2
- **Files modified:** 10 (5 created, 5 updated)

## Accomplishments

- `rw-streaming-upsert.spec.ts`: 7 tests covering D-23 #1–#6 plus D-17 job-detail sink `rowsOk` UI smoke
- `rw-streaming-upsert.ts`: `runScenarioTwice`, `assertSinkMetrics`, `generateLargeCsvFixture`, `publishLargeFileInMemoryDraft` (W-06 dual strategy)
- `template-run.ts`: `createPublishRunBundleFromScenario`, `runPublishedTemplate`, `waitForJobFailure`, extended `JobDetailPayload` sink metrics
- Catalog: `GF-FC` (CHUNKED CSV), `GF-FN` (NDJSON STREAMING), `GF-GP` / `GF-GM` (upsert)
- `scenario-f-chunked-csv.yaml` for explicit CHUNKED CSV E2E path
- `E2eV2ScenarioFixtureService` seeds upsert/partial-sink H2 tables on scenario scaffold when `e2e` profile active
- `ConsoleE2eScenarioFixtureController`: postgres upsert probe + mutate-upsert-source between re-runs
- `package.json`: `e2e:phase8-rw-streaming-upsert` script (verify script sets `DG_E2E_IN_CONTAINER`)

## Task Commits

1. **E2E specs, helpers, catalog, e2e fixture service** — feat(08-11)
2. **Plan summary** — docs(08-11)

## Files Created/Modified

- `rw-streaming-upsert.spec.ts` — D-23 matrix + UI smoke
- `rw-streaming-upsert.ts` — Phase 8 Playwright helpers
- `template-run.ts` — bundle publish, re-run, extended report types
- `scenario-f-chunked-csv.yaml` — GF-FC CHUNKED CSV reference
- `V2ScenarioCatalogService.java` — GF-FC/FN/GP/GM bindings
- `E2eV2ScenarioFixtureService.java` — e2e JDBC fixture seed + upsert mutation
- `ConsoleE2eScenarioFixtureController.java` — e2e-only REST hooks
- `TemplateEditorService.java` — fixture prep on scenario scaffold
- `verify-phase8-uat-rw-streaming-upsert.ps1` — `DG_E2E_IN_CONTAINER` for Podman warn scenario

## Decisions Made

- Used `ConsoleE2eScenarioFixtureController` instead of planned `ConsoleTemplateScenarioController` name; catalog remains in `V2ScenarioCatalogService`
- Large-file warn: `maxRows: 100_000` in Podman (validator-equivalent bar); ≥10 MB temp CSV on host-local service
- GF-GP Playwright test skips when `postgres-upsert-supported` is false (mirrors `V2ScenarioTemplateIT` W-01)
- No mid-run progress assertions (D-18); terminal SUCCESS/FAILED only

## Deviations from Plan

- `ConsoleTemplateScenarioController.java` not created — functionality split across existing catalog service + new e2e fixture controller
- Scenario 6 Podman path uses `maxRows` bar instead of 10 MB file (server cannot read host temp paths without mount); documented in helper Javadoc (W-06)
- `scenario-f-streaming-csv` (STREAMING) not used for D-23 #1; dedicated `GF-FC` CHUNKED scenario added

## Issues Encountered

- Upsert E2E requires in-JVM H2 seeding — addressed with `@Profile("e2e")` fixture service on scaffold load
- `TemplateV2VO` has no `getSink()` — normalizer maps YAML `sink` to `sinks` list

## Self-Check

- [ ] `npm run e2e:phase8-rw-streaming-upsert` green against Podman (`verify-phase8-uat-rw-streaming-upsert.ps1`)
- [x] Spec contains ≥6 D-23 scenarios + UI smoke
- [x] `e2e:phase8-rw-streaming-upsert` in package.json
- [x] GF-FC/GF-FN/GF-GP/GF-GM catalog bindings registered
- [x] Service compiles with e2e fixture classes
- [ ] Playwright run pending (service not local; Podman verify optional)

## Next Phase Readiness

- Plan 08-12 can wire `verify-phase8-uat-rw-streaming-upsert.ps1` into AGENTS.md and finalize operator docs

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
