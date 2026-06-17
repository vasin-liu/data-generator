# Roadmap: data-generator — UDF, Transform & Test Harness

## Overview

Quality-first brownfield milestone: establish an automated test harness and feature matrix, deliver a unified multi-form UDF platform (Java PF4J, script, SQL), enhance Template V2 transform operators and SQL surface, then ramp coverage to P0 CI gates. Reader/Writer expansion, datasource refactor, and template orchestration remain v2.

## Phases

- [ ] **Phase 1: Test Harness Foundation** — Feature matrix, simulation fixtures, CI entry, coverage reporting
- [ ] **Phase 2: UDF Platform Core** — Unified registry, multi-form registration, governance
- [ ] **Phase 3: UDF Console & Template Binding** — Upload/publish APIs, template validation, sample UDFs
- [ ] **Phase 4: Transform Operators & SQL** — New built-in operators, Calcite enhancements, error surfacing
- [ ] **Phase 5: Coverage Ramp & CI Gates** — P0 matrix green, API slices, merge regression gate

## Phase Details

### Phase 1: Test Harness Foundation

**Goal**: Operators and developers have a documented, runnable test harness with synthetic data fixtures and matrix-based coverage tracking.

**Depends on**: Nothing (first phase)

**Requirements**: TEST-01, TEST-02, TEST-03, TEST-04, TEST-05, TEST-06

**Success Criteria** (what must be TRUE):

1. Running the harness entry script on JDK 25 produces a pass/fail result and matrix coverage summary without manual credential setup
2. At least one embedded integration example exists per major adapter class (reader, writer, transform) using simulation fixtures
3. Playwright smoke covers template edit and job trigger paths tied to documented matrix rows

**Plans**: 3 plans

Plans:

**Wave 1**

- [ ] 01-01: Feature matrix schema + coarse row catalog at `.planning/test-matrix.yaml`, semi-automatic draft generator, and generated `docs/test-feature-matrix.md` (TEST-01)

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 01-02: `data-generator-test-fixtures` test-jar module with `FixtureTemplates.load`/`H2Seed.apply` and one embedded example per adapter (reader/writer/transform) using synthetic fixtures (TEST-02, TEST-05)

**Wave 3** *(blocked on Waves 1-2 completion)*

- [ ] 01-03: `scripts/verify-harness.ps1` embedded fast path, `target/test-matrix-summary.json` coverage summary, `harness-verify.yml` CI, and Playwright template-edit + job-trigger smoke tied to matrix rows (TEST-03, TEST-04, TEST-06)

Cross-cutting constraints:
- `.planning/test-matrix.yaml` is the single source of truth (created in 01-01, linked_tests populated in 01-03)
- Embedded-first, no production credentials; pending matrix rows never fail the harness in Phase 1

### Phase 2: UDF Platform Core

**Goal**: Template V2 can resolve published UDFs through a unified registry supporting Java plugins, scripts, and SQL definitions with governance.

**Depends on**: Phase 1 (harness rows for UDF types)

**Requirements**: UDF-01, UDF-02, UDF-03, UDF-04, UDF-07

**Success Criteria** (what must be TRUE):

1. Each UDF type (java-plugin, script, sql) can be registered programmatically and appears in the registry with version and state
2. PF4J Java UDFs load through existing Calcite plugin paths without breaking current samples
3. Governance rejects UDF payloads that violate secret/template policy at registration time

**Plans**: 3 plans

Plans:

- [ ] 02-01: Design and implement unified UDF registry model and persistence
- [ ] 02-02: Implement Java/PF4J and script UDF registration runtimes
- [ ] 02-03: Implement SQL/Calcite UDF registration and governance integration

### Phase 3: UDF Console & Template Binding

**Goal**: Operators upload, version, and publish UDFs via console/API; templates validate UDF references at publish time.

**Depends on**: Phase 2

**Requirements**: UDF-05, UDF-06, UDF-08

**Success Criteria** (what must be TRUE):

1. Console operator can upload a UDF artifact, publish it, and see it listed with version history
2. Publishing a template referencing an unknown UDF fails with a clear validation error
3. In-repo sample UDFs (one per type) pass harness end-to-end tests when referenced in a template run

**UI hint**: yes

**Plans**: 3 plans

Plans:

- [ ] 03-01: Console/API upload, list, publish, deprecate endpoints and persistence
- [ ] 03-02: Template V2 publish-time UDF reference validation
- [ ] 03-03: Sample UDFs, console UX polish, harness E2E for UDF flows

### Phase 4: Transform Operators & SQL

**Goal**: Template V2 gains new built-in transform operators and SQL enhancements with discoverable metadata and actionable errors.

**Depends on**: Phase 2 (UDF coexistence in transform layer)

**Requirements**: XFORM-01, XFORM-02, XFORM-03, XFORM-04, XFORM-05, XFORM-06

**Success Criteria** (what must be TRUE):

1. Operator can discover available transform operators (built-in + UDF) from API or console metadata
2. At least three new built-in operators work in a Template V2 run with harness-covered examples
3. Transform/UDF failures appear in job run reports with enough context to fix template YAML

**Plans**: 3 plans

Plans:

- [ ] 04-01: Operator catalog metadata API and documentation
- [ ] 04-02: Implement new built-in operators and Calcite SQL enhancements
- [ ] 04-03: Schema/docs update, error surfacing, harness rows for each operator

### Phase 5: Coverage Ramp & CI Gates

**Goal**: P0 feature-matrix rows are green in CI; merge gate enforces regression protection for core UDF/transform paths.

**Depends on**: Phases 1–4

**Requirements**: COV-01, COV-02, COV-03, COV-04

**Success Criteria** (what must be TRUE):

1. Documented P0/P1/P2 tiers exist with explicit matrix completion targets
2. All P0 matrix rows pass in the standard CI harness run
3. Console API slice tests cover UDF and transform metadata endpoints
4. Contributors have documented merge criteria when P0 rows fail

**Plans**: 2 plans

Plans:

- [ ] 05-01: Define P0/P1/P2 tiers and close P0 matrix gaps
- [ ] 05-02: Expand API slice tests and document CI regression gate in AGENTS.md

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Test Harness Foundation | 0/3 | Not started | - |
| 2. UDF Platform Core | 0/3 | Not started | - |
| 3. UDF Console & Template Binding | 0/3 | Not started | - |
| 4. Transform Operators & SQL | 0/3 | Not started | - |
| 5. Coverage Ramp & CI Gates | 0/2 | Not started | - |
