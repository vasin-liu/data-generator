# data-generator

## What This Is

**data-generator** is a Java/Maven synthetic-data platform for operators who define Template V2 pipelines (sources → transforms → sinks) and run them via a Spring Boot service with an embedded React operator console. The codebase is a mature brownfield monorepo (`3.0.0-SNAPSHOT`) with pluggable readers, writers, iterators, stages, Calcite SQL transforms, GraalJS/Velocity/SpEL scripting, and AI-assisted generation.

**v1.0 shipped** operator-uploadable multi-form UDFs, json/mask/lookup transform operators, a unified transform catalog API, and a quality-first automated test harness with P0 CI regression gates.

**v2.0 shipped** a populated `data-generator-datasource` platform (JDBC/Kafka/ES) with snapshot hot-reload governance, streaming CSV/JSON I/O, JDBC upsert, first-class dialects (Dameng, Kingbase, HighGo, PostgreSQL, ClickHouse), and an expanded 15-row P0 harness merge gate.

**v2.1 shipped** HTTP execute-path proof for managed catalog/dialect, Dameng opt-in live path + Nyquist hygiene, dual JDBC resolver ownership docs, one multi-JVM worker E2E, RBAC enable-path (default-off), and focused P1 harness rows — P0 merge gate unchanged at 15.

## Core Value

Operators can define, extend, and trust data-generation pipelines: register custom logic (UDFs), apply rich transforms, and verify behavior through an automated test harness before shipping.

## Current State

**Shipped: v2.1 Hardening & Weak-Spot Closure** (2026-07-29)

- HTTP `/task/run` proves managed JDBC catalog + PostgreSQL upsert (MockMvc ITs; not in-process-only)
- Dameng live IT opt-in recipe + `rowsUpserted` metric fix; Nyquist VALIDATION hygiene for 07/07.1/08 and v2.1 phases 12–17
- Dual JDBC resolver ownership doc + inventory (no code merge)
- Host dual-JVM coordinator→worker SUCCESS (`verify-multi-jvm-worker.ps1`); P1 harness row
- Header RBAC enable path documented/testable; default remains off
- Four focused P1 matrix rows; `verify-harness.ps1` P0 gate still 15 rows

Archives: `.planning/milestones/v2.1-ROADMAP.md`, `v2.1-REQUIREMENTS.md`, `v2.1-MILESTONE-AUDIT.md`

## Next Milestone Goals

Define via `/gsd-new-milestone`. Likely candidates from deferred backlog: RES-02 resolver consolidation, SEC-02 default-on RBAC, DIST-02 full staging AC matrix, orchestration, net-new connectors.

## Requirements

### Validated (v1.0)

- ✓ Template V2 Calcite pipeline execution — existing
- ✓ Pluggable readers/writers/iterators/stages — existing
- ✓ Spring Boot REST + embedded React console — existing
- ✓ JDBC, Kafka, ES, file sinks — existing
- ✓ GraalJS, Velocity, SpEL scripting — existing
- ✓ PF4J plugin framework — existing
- ✓ AI-assisted generation — existing
- ✓ Operator console (templates, jobs, datasources, schedules) — existing
- ✓ V1 execution retired — existing
- ✓ Test harness foundation (TEST-01..06) — v1.0 Phase 1
- ✓ Unified multi-form UDF registry + governance (UDF-01..04, UDF-07) — v1.0 Phase 2
- ✓ Console UDF upload/publish + template validation + samples (UDF-05, UDF-06, UDF-08) — v1.0 Phase 3
- ✓ Transform operators + catalog + errors (XFORM-01..06) — v1.0 Phase 4
- ✓ P0 coverage ramp + CI gate (COV-01..04) — v1.0 Phase 5

### Validated (v2.0)

- ✓ Datasource platform core + governance (DS-01..DS-05) — Phases 6–7; DS-03 execute-path snap routing in Phase 07.1
- ✓ RW streaming CSV/JSON + JDBC upsert (RW-01..RW-04) — Phase 8
- ✓ JDBC dialect expansion DM/KB/HG/PG/CK (RW-05, RW-06) — Phase 9
- ✓ Harness coverage + P0 CI gates for RW/DS paths (TEST-07, TEST-08) — Phase 10
- ✓ Closeout hardening: managed JDBC E2E IT + Kingbase dialect evidence pack — Phase 11

### Validated (v2.1)

- ✓ HTTP execute-path proof for managed catalog / dialect journeys (EXEC-01, EXEC-02) — Phase 12
- ✓ Dameng live IT opt-in path + Nyquist hygiene (DIAL-01, DIAL-02) — Phase 13
- ✓ Dual JDBC resolver ownership docs (RES-01) — Phase 14
- ✓ Multi-JVM worker E2E path (DIST-01) — Phase 15
- ✓ RBAC testable enable path, default-off (SEC-01) — Phase 16
- ✓ Focused P1 harness rows; P0 unchanged (TEST-09) — Phase 17

### Active

(None — awaiting `/gsd-new-milestone`)

### Out of Scope

- Template-level orchestration — deferred beyond v2.1
- Flow-control transforms (branch/retry/parallel DAG) — not in current scope
- Exhaustive 100% UI/control coverage — harness-first with phased targets
- Greenfield rewrite or Template V1 revival — V2 only
- Net-new connectors (Redis, S3, HTTP) — deferred after hardening
- Default-on console RBAC (SEC-02) — keep opt-in; enable path shipped in v2.1
- Full JDBC resolver code consolidation (RES-02) — docs-only in v2.1
- Full distributed AC-1..AC-7 (DIST-02) — one happy path only in v2.1
- Dameng live as P0 gate (DIAL-03) — licensed driver / CI cost

## Context

Brownfield codebase mapped 2026-06-17.

- **v1.0:** 5 phases / 18 plans over 2026-06-17 → 2026-06-23 (~46 commits, +13k LOC in milestone range)
- **v2.0:** 7 phases / 36 plans over 2026-06-23 → 2026-07-25 (~130 commits, +33k / −0.5k LOC; 331 files)
- **v2.1:** 6 phases / 18 plans over 2026-07-25 → 2026-07-29

Known pressure points:

- Console RBAC disabled by default (enable path documented; SEC-02 deferred)
- Internal Gensokyo Kafka/ES starters on Boot 3 APIs vs Boot 4 runtime
- `spring-ai` SNAPSHOT and Ollama-gated tests
- HTTP internal Nexus/SCM URLs
- Accepted tech debt: RES-02, DIST-02, matrix-doc multi-line `linked_tests` drift

## Constraints

- **Tech stack**: Java 25, Maven, Spring Boot 4.x — build via `mvnw-jdk25.ps1` / `.mvn/settings-jdk25.xml`
- **Compatibility**: Extend Template V2 schema; avoid breaking console APIs without migration notes
- **Security**: UDF upload governed; align with `SecretResolver` / template governance
- **Testing**: Embedded-first (H2, embedded Kafka, WireMock); Playwright/Podman for console E2E
- **Documentation**: Public Java APIs follow copyright/Javadoc rules

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| v1 delivery: quality-first (test harness before feature breadth) | Reduce regression risk while adding UDF/transform surface | ✓ Good — P0 gate shipped |
| UDF v1: multi-form (Java PF4J + script + SQL) with unified registry | Uploadable custom logic across transform stacks | ✓ Good — registry + console shipped |
| Transform v1: operators + SQL enhancement (not flow-control DAG) | Scoped built-in operators/SQL only | ✓ Good — json/mask/lookup shipped |
| Defer template orchestration to later version | Product decision during questioning | — Pending next milestone |
| Defer Reader/Writer and datasource abstraction to v2 | Focus v1 on UDF + quality + transforms | ✓ Good — v2.0 shipped |
| v2 dialect priority: DM, Kingbase, HighGo, PG, CK | Domestic + core analytical JDBC targets | ✓ Good — Phase 9 + Phase 11 Kingbase pack |
| v2 RW: close streaming/upsert gaps before new adapters | Gap matrix over net-new connectors | ✓ Good — Phase 8 shipped |
| Snapshot hot-reload + execute-path `snap:` routing | In-flight runs keep run-start connection snapshot | ✓ Good — Phase 7 + 07.1 |
| Keep dual JDBC resolvers with ownership split | Catalog-side vs V2 execute-path authority | ✓ Good — v2.1 RES-01 docs; RES-02 still deferred |
| Dameng live IT opt-in (`-Ddm.it=true`) | Licensed driver / CI cost | ✓ Good — v2.1 DIAL-01 recipe + metric fix |
| P0 expanded to 15 rows; `verify-harness.ps1` merge gate | Protect streaming/upsert/dialect paths | ✓ Good — Phase 10; frozen through v2.1 |
| Test acceptance: harness + phased coverage | Pragmatic ramp vs big-bang UI coverage | ✓ Good — P0/P1/P2 tiers |
| v2.1: breadth hardening over new features | Close proof/reliability gaps after v2.0 | ✓ Good — shipped 2026-07-29 |
| v2.1 RBAC stays default-off with testable enable path | Avoid breaking local/dev defaults | ✓ Good — SEC-01; SEC-02 deferred |
| v2.1 P1 rows for proofs; no P0 promotion | Keep merge gate stable | ✓ Good — TEST-09 |

<details>
<summary>Pre-v1.0 planning snapshot (2026-06-17)</summary>

Initial GSD milestone scoped UDF, transform operators, and test harness as v1; deferred Reader/Writer, datasource refactor, and orchestration to v2. See `milestones/v1.0-REQUIREMENTS.md` for full v1 requirement outcomes.

</details>

<details>
<summary>v2.0 planning snapshot (2026-06-23 → 2026-07-25)</summary>

Milestone goal: close high-priority V2 source/sink gaps and establish unified datasource abstraction with hot-reload governance. See `milestones/v2.0-REQUIREMENTS.md` and `milestones/v2.0-MILESTONE-AUDIT.md` for outcomes and accepted tech debt.

</details>

<details>
<summary>v2.1 planning snapshot (2026-07-25 → 2026-07-29)</summary>

Hardening milestone: HTTP execute proof, Dameng/Nyquist hygiene, resolver ownership docs, multi-JVM worker, RBAC enable-path, P1 harness rows. See `milestones/v2.1-REQUIREMENTS.md` and `milestones/v2.1-MILESTONE-AUDIT.md`.

</details>

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-07-29 after v2.1 milestone*
