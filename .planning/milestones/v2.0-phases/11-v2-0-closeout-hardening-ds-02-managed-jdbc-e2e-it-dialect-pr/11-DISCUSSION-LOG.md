# Phase 11: v2.0 closeout hardening - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-24
**Phase:** 11-v2-0-closeout-hardening-ds-02-managed-jdbc-e2e-it-dialect-pr
**Areas discussed:** Managed E2E proof surface, Managed E2E run path, Dialect depth target, Dialect journey shape, Harness/CI linkage
**Language:** 中文 (operator-facing); artifacts in English

---

## Area selection

| Option | Description | Selected |
|--------|-------------|----------|
| 1–5 individually | Discuss subset of gray areas | |
| all | Discuss all five gray areas | ✓ |

**User's choice:** all (五个灰区全部讨论)
**Notes:** Reply language Chinese for subsequent Q&A.

---

## Managed E2E proof surface

### Q1 — Proof layer

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | New dedicated `@SpringBootTest` | ✓ |
| 2 | Extend `V2ScenarioTemplateIT` | |
| 3 | Playwright-primary | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-01**

### Q2 — How to create managed DS

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | `DataSourceConfigService.save` | ✓ |
| 2 | Direct metadata DB + `registerToRuntime` | |
| 3 | HTTP `/api/datasources` | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-02**

### Q3 — Which side uses managed id

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | JDBC sink only | ✓ |
| 2 | Source + sink same managed DS | |
| 3 | Source only | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-03**

### Q4 — Connectivity-before-save gate

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Keep phase7-test gate off | ✓ |
| 2 | Call Catalog.test then save | |
| 3 | Gate on with H2 always-pass | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-04**

**Notes:** User chose continue → next area after area 1 locked.

---

## Managed E2E run path

### Q1 — Run entrypoint

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | In-process `TemplateV2Runner` | ✓ |
| 2 | Console `/api/templates/{id}/run` | |
| 3 | `/task/run/{id}` | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-05**

### Q2 — Row assertion

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | `COUNT(*)` on managed table | ✓ |
| 2 | RunReport / sink metrics only | |
| 3 | Both | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-06**

### Q3 — Anti-cheat assertions

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Template has managed `dataSourceId` only (no inline `dataSource`) | ✓ |
| 2 | Also assert resolve source `MANAGED` | |
| 3 | Both 1+2 | |
| 4 | Rows only | |

**User's choice:** 1 → **D-07**

### Q4 — Sink write mode

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Plain INSERT | ✓ |
| 2 | MySQL-mode upsert on H2 | |
| 3 | Claude decides | |

**User's choice:** 1 → **D-08**

---

## Dialect depth target

### Q1 — Primary non-PG engine

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Kingbase/HighGo (PG-proxy) | ✓ |
| 2 | Dameng opt-in live IT | |
| 3 | ClickHouse | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-09**

### Q2 — Playwright preset id

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | `kingbase8` | ✓ |
| 2 | `highgo` | |
| 3 | Both | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-10**

### Q3 — Connectivity proof

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Existing Maven + Playwright preset→save only | ✓ |
| 2 | Playwright also clicks Test Connection | |
| 3 | Catalog.test success path (needs live DB) | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-11**

### Q4 — Dameng this phase

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Leave Dameng code/IT unchanged | ✓ |
| 2 | Docs cross-ref only | |
| 3 | Change Dameng IT/gates | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-12**

---

## Dialect journey shape

### Q1 — How to stitch evidence

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Evidence pack + verification narrative | ✓ |
| 2 | New Maven orchestration IT | |
| 3 | Long Playwright E2E | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-13**

### Q2 — Verify script

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | New `verify-phase11-uat-closeout-hardening.ps1` | ✓ |
| 2 | Extend phase9 UAT script only | |
| 3 | No new script | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-14**

### Q3 — PG vs kingbase in Playwright

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Keep `postgresql16` and add `kingbase8` | ✓ |
| 2 | Replace with `kingbase8` only | |
| 3 | `test.each` multi-preset (PG+KB) | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-15**

### Q4 — New upsert IT?

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Reuse `ChunkedPipelineKingbaseDialectTests` | ✓ |
| 2 | Write new orchestration-style upsert IT | |
| 3 | Claude decides | |

**User's choice:** 1 → **D-16**

---

## Harness / CI linkage

### Q1 — CI / matrix hang

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Verification/UAT only — do not change P0 | ✓ |
| 2 | Add P1 matrix row | |
| 3 | Add/change P0 | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-17**

### Q2 — Docs

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | `AGENTS.md` + update audit flow disposition after execute | ✓ |
| 2 | Also `docs/test-harness.md` | |
| 3 | Audit only | |
| 4 | Claude decides | |

**User's choice:** 1 → **D-18**

### Q3 — npm script

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Reuse `e2e:phase9-jdbc-dialect` | ✓ |
| 2 | New `e2e:phase11-closeout` alias | |
| 3 | Claude decides | |

**User's choice:** 1 → **D-19**

### Q4 — Audit document closeout

| Option | Description | Selected |
|--------|-------------|----------|
| 1 | Surgical update of flows #1/#8 during Phase 11 closeout | ✓ |
| 2 | Pointers only; full re-audit before complete-milestone | |
| 3 | Force full `/gsd-audit-milestone` inside this phase | |

**User's choice:** 1 → **D-20**
**Notes:** Overall audit `status` may remain `tech_debt` for Dameng/Nyquist items outside this phase.

---

## Claude's Discretion

- IT class naming / fixture DDL
- Playwright parameterization style for `kingbase8`
- Exact Maven includes in phase11 verify script
- Verification narrative wording linking the three dialect evidence pieces

## Deferred Ideas

- Dual JDBC resolver consolidation
- P0 / harness matrix expansion for managed catalog
- Dameng live as Phase 11 deliverable
- ClickHouse as primary non-PG journey
- HTTP run path as canonical managed E2E
- Full milestone re-audit inside Phase 11 (optional later)
- Nyquist / VALIDATION.md hygiene for 7/8/07.1
