# Requirements: data-generator v2.1

**Defined:** 2026-07-25
**Core Value:** Operators can define, extend, and trust data-generation pipelines: register custom logic (UDFs), apply rich transforms, and verify behavior through an automated test harness before shipping.

## v2.1 Requirements

Hardening & weak-spot closure — proof, reliability, and documentation on shipped v2.0 surfaces. No new major feature lane.

### Execute-Path Evidence (EXEC)

- [x] **EXEC-01**: Operator (or IT via MockMvc/HTTP) can run a V2 template that uses a **managed** JDBC `dataSourceId` through `/task/run` or console `/api/templates/{id}/run`, and the run reaches SUCCESS with sink row evidence (not only in-process `TemplateV2Runner`)
- [x] **EXEC-02**: At least one CI-friendly dialect path (PostgreSQL and/or Kingbase-proxy patterns already in-repo) is proven on the same HTTP execute spine for managed catalog → dialect-aware write/upsert evidence

### Dialect & Validation Hygiene (DIAL)

- [x] **DIAL-01**: Dameng live IT has a documented opt-in green path (`-Ddm.it=true` / `DG_DM_IT=true`, host/image, expected PASS); default CI remains MERGE-unit based — live IT is not a P0 merge requirement
- [x] **DIAL-02**: Nyquist/VALIDATION hygiene backfilled for lagging phases 07, 07.1, and 08 (docs/`nyquist_compliant` status accurate)

### Resolver Ownership (RES)

- [x] **RES-01**: Maintainers have an ownership document and call-site inventory for `JdbcCatalogResolver` (catalog-side) vs `DefaultRuntimeJdbcEndpointResolver` (V2 execute-path); **no** code merge of the two resolvers in this milestone

### Distributed Reliability (DIST)

- [ ] **DIST-01**: One multi-JVM happy path is proven: coordinator enqueues → worker JVM leases/executes → SUCCESS, with a harness-linked row (P1 acceptable) and a runnable script/recipe

### Console Security (SEC)

- [ ] **SEC-01**: Operators can enable header RBAC via documented staging/e2e path and verify authorization behavior in IT/E2E; `data.generator.console-security.enabled` remains **false** by default

### Harness Gates (TEST)

- [ ] **TEST-09**: Feature matrix adds focused **P1** rows for new v2.1 proof paths (HTTP managed-catalog execute, multi-JVM worker, RBAC enable); P0 set and `verify-harness.ps1` merge-gate semantics remain unchanged

## Future Requirements

Deferred beyond v2.1.

### Orchestration

- **ORCH-01**: Template-level workflow orchestration beyond current L2 compute blocks
- **ORCH-02**: Flow-control transforms (branch/retry/parallel DAG)

### Coverage & Connectors

- **TEST-V2**: Exhaustive matrix coverage across all console flows
- **RW-07**: Net-new connector families (Redis, S3, HTTP API)
- **RES-02**: Full JDBC resolver code consolidation into a single authority
- **SEC-02**: Default-on console RBAC for production profiles
- **DIST-02**: Full staging distributed acceptance matrix (AC-1..AC-7)
- **DIAL-03**: Dameng live IT as P0 merge-gate requirement

## Out of Scope

| Feature | Reason |
|---------|--------|
| Template-level orchestration (ORCH) | Hardening milestone — no new feature lane |
| Net-new connectors (Redis, S3, HTTP) | Deferred after hardening |
| Default-on console RBAC | Keep local/dev defaults; document enable path only |
| Full JDBC resolver merge | Docs + inventory only in v2.1 |
| Dameng live as P0 gate | Licensed driver / CI cost; keep opt-in |
| Full distributed AC-1..AC-7 as DoD | One happy path only |
| Exhaustive 100% UI matrix | Focused P1 only |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| EXEC-01 | 12 | Complete |
| EXEC-02 | 12 | Complete |
| DIAL-01 | 13 | Complete |
| DIAL-02 | 13 | Complete |
| RES-01 | 14 | Complete |
| DIST-01 | 15 | Pending |
| SEC-01 | 16 | Pending |
| TEST-09 | 17 | Pending |

**Coverage:**

- v2.1 requirements: 8 total
- Mapped to phases: 8 (100%)
- Unmapped: 0

---
*Requirements defined: 2026-07-25*
*Last updated: 2026-07-25 — roadmap phases 12–17 mapped*
