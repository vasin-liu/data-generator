# Milestones

## v2.3 — Geo Assets & Map Preview (Planning)

**Status:** Planning  
**Phases:** 21–23 (0 plans)  
**Timeline:** Started 2026-07-31  
**Goal:** Hosted GeoJSON assets in metadata DB, asset-id template binding, console map preview for assets and `geo_synthetic` — equal depth.

**Requirements:** GEO-05, GEO-07, GEO-08, GEO-09, GEO-10, GEO-11, GEO-12, GEO-13, GOV-01, DOC-01, TEST-11 (11 total)

**Key deliverables (planned):**

- GeoJSON upload + metadata DB persistence + list/get/delete with referential guard (GEO-05, GEO-08, GEO-09)
- Runtime `asset:{id}` resolution on execute path for geojson + geo_synthetic (GEO-10, GEO-11)
- Audit on upload/delete; RBAC respects existing enable flag (GOV-01)
- Console geo-assets map view + `geo_synthetic` config preview + template editor (GEO-07, GEO-12, GEO-13)
- Operator docs + optional P1 `geo-assets` harness row; P0 frozen at 15 (DOC-01, TEST-11)

**Roadmap:** [.planning/ROADMAP.md](ROADMAP.md) — **Next:** `/gsd-plan-phase 21`

**Deferred from v2.2:** GEO-05, GEO-07 (now in scope); GEO-06, DATA-01 remain deferred

---

## v2.2 — V2 Geo Synthetic Source (Shipped: 2026-07-31)

**Phases completed:** 3 phases, 9 plans, 26 tasks  
**Git range:** `v2.1` → `HEAD` (71 commits, 129 files, +6,569 / −57 lines)  
**Timeline:** 2026-07-30 → 2026-07-31 (2 days)  
**Closeout:** `tech_debt` accepted (5/5 requirements; Nyquist overall compliant)  
**Known deferred items at close:** GEO-05, GEO-06, GEO-07, DATA-01, P0 promotion of geo-synthetic (see `milestones/v2.2-MILESTONE-AUDIT.md`)

**Key accomplishments:**

- BBOX/CIRCLE geo generator modes with seeded, in-domain point synthesis (GEO-02 foundation)
- Template V2 `geo_synthetic` SourceVO + Factory + RowSource; path assets only (GEO-01, GEO-03)
- Four-mode `TemplateV2Runner` pipeline IT — boundary, line, bbox, circle (GEO-02 closeout)
- Docs distinguish `geo_synthetic` vs `geojson` with minimal YAML examples (GEO-04)
- P1 harness row `geo-synthetic` with three linked tests; P0 gate frozen at 15 (TEST-10)

### Archives

- Roadmap: [milestones/v2.2-ROADMAP.md](milestones/v2.2-ROADMAP.md)
- Requirements: [milestones/v2.2-REQUIREMENTS.md](milestones/v2.2-REQUIREMENTS.md)
- Audit: [milestones/v2.2-MILESTONE-AUDIT.md](milestones/v2.2-MILESTONE-AUDIT.md)
- Phases: `.planning/phases/18-geo-generator-modes/`, `19-v2-geo-synthetic-source/`, `20-pipeline-proof-docs-p1/` (retained in place)

### Known Gaps at Close

- None for v2.2 requirements (5/5 satisfied)
- Accepted tech debt: generator retry duplication, matrix inline linked_tests, design deferrals GEO-05..07 / DATA-01

---

## v2.1 Hardening & Weak-Spot Closure (Shipped: 2026-07-29)

**Phases completed:** 6 phases, 18 plans, 39 tasks
**Audit:** `tech_debt` (8/8 requirements; Nyquist overall compliant after validate-phase 12/13)
**Known deferred items at close:** RES-02, SEC-02, DIST-02, DIAL-03, orchestration, matrix-doc multi-line linked_tests drift (see `milestones/v2.1-MILESTONE-AUDIT.md`)

**Key accomplishments:**

- HTTP managed-catalog + PostgreSQL upsert via MockMvc `/task/run` (EXEC-01, EXEC-02)
- Dameng opt-in live path + `rowsUpserted` metric fix; Nyquist hygiene for 07/07.1/08 (DIAL-01, DIAL-02)
- Dual JDBC resolver ownership doc + inventory without merge (RES-01)
- Host dual-JVM coordinator→worker SUCCESS + P1 harness row (DIST-01)
- Header RBAC enable path documented/testable; default remains off (SEC-01)
- Four focused P1 matrix rows; P0 gate frozen at 15 (TEST-09)

### Archives

- Roadmap: [milestones/v2.1-ROADMAP.md](milestones/v2.1-ROADMAP.md)
- Requirements: [milestones/v2.1-REQUIREMENTS.md](milestones/v2.1-REQUIREMENTS.md)
- Audit: [milestones/v2.1-MILESTONE-AUDIT.md](milestones/v2.1-MILESTONE-AUDIT.md)
- Phases: [milestones/v2.1-phases/](milestones/v2.1-phases/)

---

## v2.0 — Reader/Writer & Datasource Platform

**Shipped:** 2026-07-25
**Closeout:** verified_closeout
**Phases:** 7 (6–11 + 07.1) | **Plans:** 36 | **Tasks:** 66
**Git range:** `v1.0` → `HEAD` (130 commits, 331 files, +33,397 / −482 lines)
**Timeline:** 2026-06-23 → 2026-07-25 (31 days)

### Summary

Brownfield release closing V2 source/sink gaps and establishing a unified datasource platform: managed JDBC/Kafka/ES catalog with snapshot hot-reload governance, streaming CSV/JSON I/O, dialect-correct JDBC upsert (including Dameng/Kingbase/HighGo/PostgreSQL/ClickHouse), and an expanded 15-row P0 harness merge gate.

### Key Accomplishments

1. **Datasource platform** — `data-generator-datasource` API + JDBC/Kafka/ES adapters; managed catalog resolution without template YAML changes (DS-01, DS-02)
2. **Governance & hot-reload** — Snapshot refresh, secret/connectivity policy, audit feed; Phase 07.1 wired `snap:` pools onto the JDBC execute path (DS-03..DS-05)
3. **RW streaming & upsert** — Chunked CSV/JSON sources/sinks; PG/MySQL upsert SQL; per-sink run-report metrics (RW-01..RW-04)
4. **JDBC dialect expansion** — DM/KB/HG/PG/CK writers, console presets, layered Testcontainers/unit evidence (RW-05, RW-06)
5. **Harness & CI** — Eight new P0 rows; 15/15 P0 green via `verify-harness.ps1`; docs/AGENTS merge criteria (TEST-07, TEST-08)
6. **Closeout hardening** — `ManagedJdbcCatalogSinkE2eIT` + Kingbase evidence pack; audit flows #1/#8 → OK

### Archives

- Roadmap: [milestones/v2.0-ROADMAP.md](milestones/v2.0-ROADMAP.md)
- Requirements: [milestones/v2.0-REQUIREMENTS.md](milestones/v2.0-REQUIREMENTS.md)
- Audit: [milestones/v2.0-MILESTONE-AUDIT.md](milestones/v2.0-MILESTONE-AUDIT.md)
- Phases: [milestones/v2.0-phases/](milestones/v2.0-phases/)

### Known Tech Debt at Close

- Dameng default CI = MERGE SQL unit only; live IT opt-in (`-Ddm.it=true`)
- Nyquist validation incomplete for phases 07 / 08 / 07.1 (hygiene, not DoD blockers)
- Dual JDBC resolvers (`JdbcCatalogResolver` vs `DefaultRuntimeJdbcEndpointResolver`) — consolidation deferred
- Managed-catalog E2E is in-process `TemplateV2Runner` (not HTTP `/task/run`)
- CodeGraph index missing under repo root

---

## v1.0 — UDF, Transform & Test Harness

**Shipped:** 2026-06-23
**Phases:** 5 | **Plans:** 18
**Git range:** `79c17b9` → `HEAD` (46 commits, 133 files, +13,276 / −63 lines)

### Summary

Quality-first brownfield release: automated test harness with feature matrix, unified multi-form UDF platform, Template V2 transform operators (json/mask/lookup), and P0 CI regression gate.

### Key Accomplishments

1. **Test harness foundation** — Feature matrix (`.planning/test-matrix.yaml`), `data-generator-test-fixtures`, `scripts/verify-harness.ps1`, `harness-verify.yml` CI, Playwright smoke
2. **UDF platform core** — Unified `UdfRegistry` for java-plugin, script, and SQL types with governance hooks
3. **UDF console & binding** — JDBC persistence, `/api/console/udfs`, React Udfs page, publish-time template validation, in-repo sample UDFs
4. **Transform operators** — json/mask/lookup VOs and runtime, transform catalog API, actionable run-report errors, `V2_JSON_EXTRACT`
5. **Coverage ramp** — P0/P1/P2 tiers, 7/7 P0 green, console API slice tests, merge regression gate documented in `AGENTS.md`

### Archives

- Roadmap: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)
- Requirements: [milestones/v1.0-REQUIREMENTS.md](milestones/v1.0-REQUIREMENTS.md)

### Known Gaps at Close

- No `v1.0-MILESTONE-AUDIT.md` (audit skipped; accepted as tech debt)
- Known deferred items at close: 3 (see `STATE.md` Deferred Items)
