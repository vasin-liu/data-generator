# Milestone v2.0: Reader/Writer & Datasource Platform

**Status:** 🚧 IN PROGRESS (Phase 6 validated)
**Phases:** 6-10
**Total Plans:** TBD

## Overview

Brownfield milestone delivering V2 source/sink gap closure (streaming CSV/JSON, JDBC upsert), first-class JDBC dialect support for Dameng, Kingbase, HighGo, PostgreSQL, and ClickHouse, a populated `data-generator-datasource` abstraction layer, and snapshot-based hot-reload governance — capped by harness matrix expansion and P0 CI gates for new paths.

## Phases

- [x] **Phase 6: Datasource Platform Core** — Unified JDBC/Kafka/ES abstractions; managed catalog resolution (DS-01, DS-02)
- [ ] **Phase 7: Datasource Governance & Hot-Reload** — Snapshot refresh, policy enforcement, connectivity test, audit (DS-03, DS-04, DS-05)
- [x] **Phase 8: RW Streaming & Upsert** — Chunked CSV/JSON I/O; PG/MySQL upsert; run-report diagnostics (RW-01..RW-04)
- [ ] **Phase 9: JDBC Dialect Expansion** — DM, Kingbase, HighGo, PG, CK writers + console presets (RW-05, RW-06)
- [ ] **Phase 10: Harness Coverage & CI Gates** — Matrix rows and P0 gate for RW/DS paths (TEST-07, TEST-08)

## Phase Details

### Phase 6: Datasource Platform Core

**Goal**: Runtime resolves managed JDBC, Kafka, and Elasticsearch connections through a unified `data-generator-datasource` module instead of ad-hoc service wiring.

**Depends on**: v1.0 complete (harness + console APIs exist)

**Requirements**: DS-01, DS-02

**Success Criteria** (what must be TRUE):

1. Operator template referencing `dataSourceId` for JDBC, Kafka, or ES resolves through the new abstraction layer without changing template YAML shape
2. `data-generator-datasource` contains submodules or packages for JDBC, Kafka, and ES with clear extension points — not an empty aggregator POM
3. Existing console datasource CRUD and V2 run paths pass regression tests using the new resolution layer

**Plans**: 5 plans in 4 waves

**Wave 1** *(no dependencies)*

- `06-01` — Datasource API module & Catalog contracts (DS-01)

**Wave 2** *(blocked on Wave 1)*

- `06-02` — JDBC datasource adapter (DS-01)
- `06-03` — Kafka & Elasticsearch adapter relocation (DS-01)

**Wave 3** *(blocked on Wave 2)*

- `06-04` — Service Catalog implementation & bootstrap wiring (DS-02)

**Wave 4** *(blocked on Wave 3)*

- `06-05` — Calcite runtime integration & regression gate (DS-01, DS-02)

### Phase 7: Datasource Governance & Hot-Reload

**Goal**: Operators can safely update connections with snapshot-based hot-reload, policy enforcement, and audit visibility.

**Depends on**: Phase 6

**Requirements**: DS-03, DS-04, DS-05

**Success Criteria** (what must be TRUE):

1. Updating a managed datasource takes effect for new runs without breaking in-flight runs (snapshot at run start)
2. Template publish rejects plaintext production secrets when governance policy requires `passwordSecretRef` / `apiKeySecretRef`
3. Console connectivity test succeeds or fails with actionable message before operator saves a datasource
4. Datasource create/update/delete and reload events appear in the Audit page feed

**Plans**: 5 plans in 4 waves

**Wave 1** *(no dependencies)*

- `07-01` — Catalog API extensions & execution snapshot schema (DS-03, DS-04, DS-05)

**Wave 2** *(blocked on Wave 1)*

- `07-02` — Run-start snapshot, hot-reload & DEGRADED runtime (DS-03)

**Wave 3** *(blocked on Wave 2)*

- `07-03` — Connectivity test, governance gates & audit events (DS-04, DS-05)

**Wave 4** *(blocked on Wave 3)*

- `07-04` — Console datasource UX, bug fixes & audit deep-link (DS-03, DS-04, DS-05)
- `07-05` — Playwright E2E, playwright-cli & Phase 7 UAT scripts (DS-03, DS-04, DS-05)

**Verification**: `.\scripts\verify-phase7-uat-datasource-governance.ps1 -SkipPlaywright` (Maven IT slice); full UAT adds Podman Playwright + playwright-cli snapshots.

### Phase 8: RW Streaming & Upsert

**Goal**: Large CSV/JSON pipelines stream without full heap materialization; JDBC sinks support upsert on PostgreSQL and MySQL with clear run reports.

**Depends on**: Phase 6 (connection resolution stable)

**Requirements**: RW-01, RW-02, RW-03, RW-04

**Success Criteria** (what must be TRUE):

1. Operator can run a V2 template with a large CSV or JSON source in streaming/chunked mode without OOM on a documented fixture size
2. Operator can run a V2 template writing CSV or JSON sink output in streaming mode for large row counts
3. JDBC sink upsert on PostgreSQL and MySQL reloads idempotently (re-run updates existing keys instead of duplicating)
4. Job run report shows per-sink row counts and actionable errors for streaming and upsert failure paths

**Plans**: 12 plans in 5 waves

**Wave 1** *(no dependencies within wave for 08-01; 08-02 blocked on 08-01; 08-03 blocked on Wave 1 sources)*

- `08-01` — Chunked CSV row source & UTF-8 BOM parser (RW-01)
- `08-02` — Chunked JSON row source & NDJSON/array streaming parser (RW-01)
- `08-03` — Pipeline CSV/JSON eligibility & registry policy wiring (RW-01)

**Wave 2** *(blocked on Wave 1)*

- `08-04` — CSV/JSON streaming sinks per-chunk flush + pipeline finalize hook (RW-02)
- `08-05` — JDBC upsert SQL generation for PostgreSQL & MySQL (RW-03)

**Wave 3** *(blocked on Wave 2)*

- `08-06` — Run report sink metrics & actionable errors (RW-04)
- `08-07` — Publish validation, warnings & console form hints (RW-03, RW-04)

**Wave 4** *(blocked on Wave 3)*

- `08-08` — V2 scenario YAML fixtures & scenario IT harness (RW-01, RW-02, RW-03, RW-04)
- `08-09` — OOM proof IT & Testcontainers upsert idempotency (RW-01, RW-03)
- `08-10` — Calcite unit tests for streaming pipelines & SQL builder (RW-01, RW-02, RW-03)

**Wave 5** *(blocked on Wave 4)*

- `08-11` — Playwright E2E RW streaming & upsert scenarios (RW-01..RW-04)
- `08-12` — UAT verify script, operator docs & ROADMAP update (RW-01..RW-04)

**Verification**: `.\scripts\verify-phase8-uat-rw-streaming-upsert.ps1 -SkipPlaywright` (Maven IT slice); full UAT adds Podman Playwright (`npm run e2e:phase8-rw-streaming-upsert`).

### Phase 9: JDBC Dialect Expansion

**Goal**: Operators use Dameng, Kingbase, HighGo, PostgreSQL, and ClickHouse as first-class JDBC targets with dialect-correct writers and console presets.

**Depends on**: Phase 8 (writer pipeline patterns established)

**Requirements**: RW-05, RW-06

**Success Criteria** (what must be TRUE):

1. Operator can configure and test datasources for Dameng, Kingbase, HighGo, PostgreSQL, and ClickHouse from console presets with correct URL/driver hints
2. V2 JDBC sink generates dialect-appropriate INSERT (and documented upsert/bulk where supported) for each of the five engines
3. Embedded harness tests pass for at least one read/write scenario per target dialect without production credentials
4. Unsupported capabilities per dialect (e.g. ClickHouse upsert limits) are documented in operator-facing docs — not silent failures

**Plans**: 1/5 plans executed

- [x] 09-01-PLAN.md
- [ ] 09-02-PLAN.md
- [ ] 09-03-PLAN.md
- [ ] 09-04-PLAN.md
- [ ] 09-05-PLAN.md

**Wave 1** *(no dependencies within wave — parallel)*

- `09-01` — JDBC sink dialect SQL generation & publish validation (RW-05, D-01–D-08)
- `09-02` — Console driver presets & connectivity hygiene (RW-06, D-09–D-11)

**Wave 2** *(blocked on Wave 1 SQL builder)*

- `09-03` — Embedded dialect integration tests: PG/CK Testcontainers, KB/HG PG-proxy, DM MERGE unit, CK reject (RW-05, D-13–D-15)

**Wave 3** *(blocked on Waves 1–2)*

- `09-04` — Playwright preset E2E & UAT verify script (RW-05, RW-06, D-12, D-16)

**Wave 4** *(blocked on Waves 1–3)*

- `09-05` — Operator docs, AGENTS.md & ROADMAP registry (RW-05, RW-06, D-17, D-18)

**Verification**: `.\scripts\verify-phase9-uat-jdbc-dialect.ps1 -SkipPlaywright` (Maven dialect slice); full UAT adds Podman Playwright (`npm run e2e:phase9-jdbc-dialect`).

### Phase 10: Harness Coverage & CI Gates

**Goal**: New RW and datasource capabilities are tracked in the feature matrix with a P0 subset gating merge.

**Depends on**: Phases 6-9 (features to cover)

**Requirements**: TEST-07, TEST-08

**Success Criteria** (what must be TRUE):

1. `.planning/test-matrix.yaml` includes P0/P1 rows for streaming CSV/JSON, JDBC upsert, and each target dialect (DM, Kingbase, HighGo, PG, CK)
2. `scripts/verify-harness.ps1` emits `target/test-matrix-summary.json` showing covered vs pending for new rows
3. CI merge gate fails when any P0 row for streaming, upsert, or dialect paths regresses
4. `AGENTS.md` documents the expanded P0 row set and verification command

**Plans**: TBD via `/gsd-plan-phase 10`

## Requirement Coverage

| Requirement | Phase |
|-------------|-------|
| DS-01 | 6 |
| DS-02 | 6 |
| DS-03 | 7 |
| DS-04 | 7 |
| DS-05 | 7 |
| RW-01 | 8 |
| RW-02 | 8 |
| RW-03 | 8 |
| RW-04 | 8 |
| RW-05 | 9 |
| RW-06 | 9 |
| TEST-07 | 10 |
| TEST-08 | 10 |

**Coverage:** 13/13 requirements mapped ✓

## Out of Scope (v2.0)

- Template-level orchestration (ORCH)
- Net-new non-JDBC connectors (Redis, S3, HTTP)
- Exhaustive 100% matrix coverage
- Distributed worker multi-JVM E2E

---
*Roadmap created: 2026-06-23 for milestone v2.0*
