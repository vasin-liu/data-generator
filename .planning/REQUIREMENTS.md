# Requirements: data-generator v2.0

**Defined:** 2026-06-23
**Core Value:** Operators can define, extend, and trust data-generation pipelines: register custom logic (UDFs), apply rich transforms, and verify behavior through an automated test harness before shipping.

## v2.0 Requirements

### Reader/Writer — Gap Closure (RW)

- [ ] **RW-01**: CSV and JSON V2 sources support streaming/chunked reads so large files do not fully materialize in heap
- [ ] **RW-02**: CSV and JSON V2 sinks support streaming/chunked writes for large output datasets
- [ ] **RW-03**: JDBC sink supports upsert/merge semantics for PostgreSQL and MySQL with dialect-correct SQL generation
- [ ] **RW-04**: Run reports surface per-sink row counts and actionable errors for streaming and upsert paths

### Reader/Writer — Dialect Expansion (RW)

- [x] **RW-05**: First-class JDBC dialect writers for Dameng (达梦), Kingbase (金仓), HighGo (翰高), PostgreSQL, and ClickHouse — including insert and documented upsert/bulk behavior per engine
- [x] **RW-06**: Console datasource presets and validation recognize Dameng, Kingbase, HighGo, PostgreSQL, and ClickHouse connection shapes (URL templates, driver hints, connectivity test)

### Datasource Platform (DS)

- [x] **DS-01**: `data-generator-datasource` module hosts unified connection abstractions for JDBC, Kafka, and Elasticsearch (replacing ad-hoc service-only wiring)
- [x] **DS-02**: Managed connection catalog API resolves `dataSourceId` / `connectionRef` through the new abstraction layer without breaking existing console templates
- [ ] **DS-03**: Hot-reload applies datasource changes via snapshot-based refresh; in-flight runs continue on the connection snapshot taken at run start
- [ ] **DS-04**: Governance enforces managed vs inline connection policy, secret refs (no plaintext production secrets), and connectivity test before publish where configured
- [ ] **DS-05**: Datasource create/update/delete and hot-reload events emit audit records consumable by the console Audit page

### Test Harness (TEST)

- [ ] **TEST-07**: Feature matrix adds P0/P1 rows for streaming CSV/JSON, JDBC upsert, and each target dialect (DM, Kingbase, HighGo, PG, CK) with embedded-first tests
- [ ] **TEST-08**: `verify-harness.ps1` reports coverage status for new RW/DS matrix rows; P0 subset gates merge for dialect + streaming paths

## Future Requirements

Deferred beyond v2.0.

### Orchestration

- **ORCH-01**: Template-level workflow orchestration beyond current L2 compute blocks
- **ORCH-02**: Flow-control transforms (branch/retry/parallel DAG in transform layer)

### Coverage & Distributed

- **TEST-V2**: Exhaustive matrix coverage across all console flows
- **DIST-01**: Distributed worker multi-JVM E2E verification

### New Adapters

- **RW-07**: Net-new connector families (Redis, S3, HTTP API) — after gap closure and dialect targets ship

## Out of Scope

| Feature | Reason |
|---------|--------|
| Template-level orchestration (ORCH) | Explicitly deferred beyond v2.0 per product decision |
| Net-new non-JDBC connectors (Redis, S3, HTTP) | v2.0 focuses on gap closure + JDBC dialect targets first |
| Exhaustive 100% UI/control matrix | Harness-first with phased P0/P1 targets only |
| COPY/bulk loaders for every dialect | Ship per-dialect where high-value; document limits for remainder |
| Greenfield rewrite or V1 revival | V2-only product path |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| DS-01 | Phase 6 | Complete |
| DS-02 | Phase 6 | Complete |
| DS-03 | Phase 7 | Pending |
| DS-04 | Phase 7 | Pending |
| DS-05 | Phase 7 | Pending |
| RW-01 | Phase 8 | Pending |
| RW-02 | Phase 8 | Pending |
| RW-03 | Phase 8 | Pending |
| RW-04 | Phase 8 | Pending |
| RW-05 | Phase 9 | Complete |
| RW-06 | Phase 9 | Complete |
| TEST-07 | Phase 10 | Pending |
| TEST-08 | Phase 10 | Pending |

**Coverage:**

- v2.0 requirements: 13 total
- Mapped to phases: 13
- Unmapped: 0 ✓

---
*Requirements defined: 2026-06-23*
*Last updated: 2026-06-23 after v2.0 milestone scoping*
