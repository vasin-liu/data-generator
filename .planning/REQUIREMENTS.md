# Requirements: data-generator (UDF + Transform + Test Harness milestone)

**Defined:** 2026-06-17
**Core Value:** Operators can define, extend, and trust data-generation pipelines through uploadable UDFs, richer transforms, and an automated test harness.

## v1 Requirements

### Test Harness

- [ ] **TEST-01**: A documented feature matrix exists mapping platform capabilities (readers, writers, transforms, console flows) to test types and owners (`docs/` or `.planning/`)
- [ ] **TEST-02**: A reusable synthetic data simulation fixture library supports Template V2 runs in tests without production credentials
- [ ] **TEST-03**: A single CI-oriented entry script runs the harness slice (Maven modules + optional console build) with JDK 25 and project Maven settings
- [ ] **TEST-04**: Harness reports which matrix rows are covered vs pending (machine-readable summary for phased ramp)
- [ ] **TEST-05**: Embedded-first integration patterns are documented and exemplified for at least one reader, one writer, and one transform path
- [ ] **TEST-06**: Playwright console smoke path is wired to matrix rows for template edit and job trigger (extends `data-generator-console-web/e2e/`)

### UDF Platform

- [ ] **UDF-01**: A unified UDF registry model identifies UDFs by stable ID, type (java-plugin, script, sql), version, and lifecycle state
- [ ] **UDF-02**: Operators can register Java/PF4J UDF artifacts compatible with existing `data-generator-calcite` plugin loading
- [ ] **UDF-03**: Operators can register script UDFs (GraalJS and/or Velocity) with schema validation before publish
- [ ] **UDF-04**: Operators can register SQL/Calcite UDF definitions referenced by Template V2 SQL transforms
- [ ] **UDF-05**: Console/API supports upload, list, version history, and publish/deprecate for UDF artifacts (`data-generator-service/.../api/console/`)
- [ ] **UDF-06**: Template V2 validation rejects references to unknown or unpublished UDF IDs at publish time
- [ ] **UDF-07**: UDF governance hooks align with secret/template governance (no plaintext secrets in UDF payloads; audit on publish)
- [ ] **UDF-08**: Sample UDFs (one per type) ship in-repo with harness tests proving template reference works end-to-end

### Transform Enhancement

- [ ] **XFORM-01**: Catalog of built-in transform operators is documented and discoverable from console or API metadata
- [ ] **XFORM-02**: At least three new high-value built-in operators ship (e.g. JSON parse/map, lookup/join helper, masking/redaction — final set chosen in Phase 4 planning)
- [ ] **XFORM-03**: SQL transform expression surface supports additional scalar functions/types needed by new operators (Calcite layer in `data-generator-calcite/`)
- [ ] **XFORM-04**: Template V2 schema/version notes document new operator and UDF reference fields without breaking existing templates
- [ ] **XFORM-05**: Operator and UDF errors surface actionable messages in run reports and console job detail
- [ ] **XFORM-06**: Harness matrix includes rows for each new operator and UDF type with passing embedded tests

### Coverage Ramp

- [ ] **COV-01**: Phase-4 coverage target defines minimum matrix completion % and priority tiers (P0/P1/P2 rows)
- [ ] **COV-02**: P0 matrix rows (core V2 run, UDF publish, three transform types) reach automated green status in CI
- [ ] **COV-03**: Console API slice tests expand for UDF and transform metadata endpoints
- [ ] **COV-04**: Regression gate blocks merge when P0 matrix rows fail (documented in `AGENTS.md` / CI notes)

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Reader / Writer

- **RW-01**: Additional reader integrations (new JDBC dialects, object stores, messaging sources)
- **RW-02**: Additional writer integrations (warehouses, streams, bulk loaders)

### Datasource Abstraction

- **DS-01**: Refactor dynamic datasource maintenance for improved console CRUD and runtime hot-refresh
- **DS-02**: Pluggable datasource provider SPI with versioned config migration

### Template Orchestration

- **ORCH-01**: Multi-template workflow with dependencies, parameters, and failure policies
- **ORCH-02**: Visual orchestration UI and schedule integration beyond single-template runs

### Testing (stretch)

- **TEST-V2-01**: Exhaustive matrix coverage for every console control and adapter variant
- **TEST-V2-02**: Distributed worker E2E with multi-JVM harness

## Out of Scope

| Feature | Reason |
|---------|--------|
| New Reader/Writer adapters (v1) | User deferred to v2; focus on UDF + transforms + harness |
| Datasource abstraction overhaul (v1) | High blast radius; deferred per scoping session |
| Template-level orchestration (v1) | Explicitly deferred to later version |
| Flow-control transforms (branch/retry/parallel DAG) | User scoped v1 transforms to operators + SQL only |
| 100% UI control coverage in v1 | Harness-first with phased COV targets |
| Template V1 execution revival | Retired at runtime; V2 only |
| Greenfield rewrite | Brownfield extension milestone |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| TEST-01 | Phase 1 | Pending |
| TEST-02 | Phase 1 | Pending |
| TEST-03 | Phase 1 | Pending |
| TEST-04 | Phase 1 | Pending |
| TEST-05 | Phase 1 | Pending |
| TEST-06 | Phase 1 | Pending |
| UDF-01 | Phase 2 | Pending |
| UDF-02 | Phase 2 | Pending |
| UDF-03 | Phase 2 | Pending |
| UDF-04 | Phase 2 | Pending |
| UDF-07 | Phase 2 | Pending |
| UDF-05 | Phase 3 | Pending |
| UDF-06 | Phase 3 | Pending |
| UDF-08 | Phase 3 | Pending |
| XFORM-01 | Phase 4 | Pending |
| XFORM-02 | Phase 4 | Pending |
| XFORM-03 | Phase 4 | Pending |
| XFORM-04 | Phase 4 | Pending |
| XFORM-05 | Phase 4 | Pending |
| XFORM-06 | Phase 4 | Pending |
| COV-01 | Phase 5 | Pending |
| COV-02 | Phase 5 | Pending |
| COV-03 | Phase 5 | Pending |
| COV-04 | Phase 5 | Pending |

**Coverage:**

- v1 requirements: 24 total
- Mapped to phases: 24
- Unmapped: 0 ✓

---
*Requirements defined: 2026-06-17*
*Last updated: 2026-06-17 after roadmap creation*
