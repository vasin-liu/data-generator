---
phase: 08-rw-streaming-upsert
plan: 08
subsystem: test
tags: [scenarios, streaming, upsert, v2-scenario-it, rw-01, rw-02, rw-03, rw-04]

requires: [08-04, 08-05, 08-06, 08-07]
provides:
  - scenario-f-streaming-csv.yaml and scenario-f-streaming-ndjson.yaml (GF-F)
  - scenario-g-upsert-pg.yaml and scenario-g-upsert-mysql.yaml (GF-G)
  - streaming-orders.csv and streaming-events.ndjson fixtures (120 rows)
  - V2ScenarioTemplateIT extended scenarioResources, prepareScenarioFixtures, assertScenarioOutcome
  - materializeLargeStreamingCsvFixture hook for 08-09 OOM proof (≥10 MB)
  - Catalog GF-F/GF-G entries with RW-01..RW-03 trace
affects: [08-09, 08-11, 08-12]

tech-stack:
  added: []
  patterns:
    - "Extend scenario-e-streaming-jdbc / V2ScenarioTemplateIT harness (D-07)"
    - "H2 MODE=PostgreSQL|MySQL in upsert YAML; PG scenario @EnabledIf-style skip when ON CONFLICT unsupported (W-01)"
    - "Upsert idempotency: second run stable row count + rowsUpserted > 0 (D-15)"
    - "scenario-e-partial-sink asserts actionable errorSample with sink key, jdbc type, target (W-08, RW-04)"

key-files:
  created:
    - data-generator-service/src/main/resources/template/v2-scenarios/scenario-f-streaming-csv.yaml
    - data-generator-service/src/main/resources/template/v2-scenarios/scenario-f-streaming-ndjson.yaml
    - data-generator-service/src/main/resources/template/v2-scenarios/scenario-g-upsert-pg.yaml
    - data-generator-service/src/main/resources/template/v2-scenarios/scenario-g-upsert-mysql.yaml
    - data-generator-service/src/main/resources/template/v2-scenarios/fixtures/streaming-orders.csv
    - data-generator-service/src/main/resources/template/v2-scenarios/fixtures/streaming-events.ndjson
  modified:
    - data-generator-service/src/test/java/org/gensokyo/data/template/V2ScenarioTemplateIT.java
    - docs/template-v2-scenario-template-catalog.md

key-decisions:
  - "STREAMING mode for GF-F CSV/NDJSON (not CHUNKED) to match scenario-e pattern with peakRowsInMemory assertions"
  - "Console sink for GF-F fast IT; JDBC upsert scenarios use inline H2 with dialect-specific MODE URLs"
  - "PG upsert IT gated by h2SupportsPostgresUpsert() probe; MySQL upsert runs on H2 MODE=MySQL"
  - "Large CSV generated in prepareScenarioFixtures, not committed (repo size); moderate 120-row fixtures on classpath"

patterns-established:
  - "assertUpsertScenarioOutcome helper: first run insert, mutate source, second run upsert without row growth"
  - "materializeClasspathFixture shared helper for scenario-c and Phase 8 streaming fixtures"

requirements-completed: [RW-01, RW-02, RW-03, RW-04]

duration: 35min
completed: 2026-06-29
---

# Phase 08 Plan 08 Summary

**V2 scenario YAML fixtures and IT harness for CSV/NDJSON streaming and PG/MySQL JDBC upsert (GF-F / GF-G)**

## Performance

- **Duration:** 35 min
- **Tasks:** 2
- **Files modified:** 8 (6 created, 2 updated)

## Accomplishments

- Added four Phase 8 scenario templates following `scenario-e-streaming-jdbc.yaml` shape (D-07)
- `scenario-f-streaming-csv`: STREAMING CSV source → SQL → console sink, 120 rows, chunk size 40
- `scenario-f-streaming-ndjson`: STREAMING NDJSON source (`format: ndjson`) → SQL → console sink
- `scenario-g-upsert-pg` / `scenario-g-upsert-mysql`: CHUNKED query → SQL → JDBC upsert with `upsertKeys: [id]`
- Classpath fixtures `streaming-orders.csv` and `streaming-events.ndjson` (120 data rows each)
- `V2ScenarioTemplateIT` registers all four scenarios; extends partial-sink assertions for Phase 8 actionable errors (W-08)
- `materializeLargeStreamingCsvFixture()` generates ≥10 MB CSV temp file for plan 08-09 OOM IT (D-06)
- Catalog documents GF-F (RW-01/RW-02) and GF-G (RW-03) with evidence pointers

## Task Commits

1. **Scenario YAMLs, fixtures, IT harness, catalog** — feat(08-08)
2. **Plan summary** — docs(08-08)

## Files Created/Modified

- `scenario-f-streaming-csv.yaml` — GF-F CSV streaming reference
- `scenario-f-streaming-ndjson.yaml` — GF-F NDJSON streaming reference
- `scenario-g-upsert-pg.yaml` — GF-G PostgreSQL upsert (H2 MODE=PostgreSQL)
- `scenario-g-upsert-mysql.yaml` — GF-G MySQL upsert (H2 MODE=MySQL)
- `fixtures/streaming-orders.csv`, `fixtures/streaming-events.ndjson` — moderate IT fixtures
- `V2ScenarioTemplateIT.java` — scenario registration, fixture prep, streaming/upsert/partial-sink assertions
- `template-v2-scenario-template-catalog.md` — GF-F/GF-G catalog rows

## Decisions Made

- GF-F uses console sink (not file sink) for fast embedded IT without temp output cleanup
- Upsert scenarios seed 40 rows (`UPSERT_FIXTURE_ROWS`) aligned with `sourceChunkSize: 40`
- PG upsert scenario skipped at runtime when H2 probe fails `ON CONFLICT DO UPDATE` (W-01); MySQL path always exercised on H2

## Deviations from Plan

- `scenario-f-streaming-csv` uses STREAMING only (plan allowed CHUNKED or STREAMING); STREAMING chosen for parity with `scenario-e-streaming-jdbc` and `peakRowsInMemory` assertion
- Large `large-streaming-orders.csv` not committed; generated programmatically in IT (`materializeLargeStreamingCsvFixture`)
- PG upsert may be assumption-skipped on some H2 builds rather than Testcontainers (Testcontainers proof deferred to 08-09, D-25)

## Issues Encountered

- Maven `-pl data-generator-service -am test -Dtest=V2ScenarioTemplateIT` requires quoted `-D` props on PowerShell and `-Dsurefire.failIfNoSpecifiedTests=false` with reactor slice

## Self-Check: PASSED

- `.\mvnw-jdk25.ps1 -pl data-generator-service -am test "-Dtest=V2ScenarioTemplateIT" "-Dsurefire.failIfNoSpecifiedTests=false"`: exit 0 (~5 min)
- All 12 scenario resources load, validate, and execute (PG upsert skipped when H2 lacks ON CONFLICT)
- `scenario-f-streaming-csv`: executionMode STREAMING, rowsWritten 120, peakRowsInMemory > 0
- `scenario-f-streaming-ndjson`: executionMode STREAMING, rowsWritten 120
- `scenario-g-upsert-mysql`: idempotent second run, rowsUpserted > 0
- `scenario-e-partial-sink`: errorSample contains sink key, jdbc, `__missing_sink_target__`, rowsFailed > 0
- `scenario-e-streaming-jdbc` regression: unchanged assertions pass

## Next Phase Readiness

- Plan 08-09 can use `materializeLargeStreamingCsvFixture()` for `-Xmx256m` OOM IT and Testcontainers PG upsert proof
- Plan 08-11/08-12 E2E can scaffold from GF-F/GF-G scenario IDs in catalog

---
*Phase: 08-rw-streaming-upsert*
*Completed: 2026-06-29*
