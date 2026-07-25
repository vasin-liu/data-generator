# data-generator

## What This Is

**data-generator** is a Java/Maven synthetic-data platform for operators who define Template V2 pipelines (sources → transforms → sinks) and run them via a Spring Boot service with an embedded React operator console. The codebase is a mature brownfield monorepo (`3.0.0-SNAPSHOT`) with pluggable readers, writers, iterators, stages, Calcite SQL transforms, GraalJS/Velocity/SpEL scripting, and AI-assisted generation.

**v1.0 shipped** operator-uploadable multi-form UDFs, json/mask/lookup transform operators, a unified transform catalog API, and a quality-first automated test harness with P0 CI regression gates.

## Core Value

Operators can define, extend, and trust data-generation pipelines: register custom logic (UDFs), apply rich transforms, and verify behavior through an automated test harness before shipping.

## Current Milestone: v2.0 Reader/Writer & Datasource Platform

**Goal:** Close high-priority V2 source/sink gaps and establish a unified datasource abstraction with hot-reload governance, including first-class JDBC dialect support for Dameng, Kingbase, HighGo, PostgreSQL, and ClickHouse.

**Target features:**
- Reader/Writer gap closure: streaming CSV/JSON, JDBC upsert/merge, dialect-specific writers
- JDBC dialect expansion: Dameng (达梦), Kingbase (金仓), HighGo (翰高), PostgreSQL, ClickHouse
- Datasource module: unified JDBC/Kafka/ES abstractions in `data-generator-datasource`
- Datasource governance: hot-reload snapshots, managed vs inline connections, secret refs, connectivity test, audit trail
- Harness matrix rows for new RW and dialect paths

## Current State (v2.0 Phase 11 complete — ready for milestone archive)

- **Phase 11 complete:** DS-02 managed JDBC catalog sink E2E IT + Kingbase dialect evidence pack (RW-05/RW-06); audit flows #1/#8 → OK
- **v2.0 roadmap phases 6–11 (+07.1):** executed; progress 36/36 plans
- **Phase 07.1:** `DefaultRuntimeJdbcEndpointResolver` returns `snap:{instanceId}:{name}` on managed JDBC execute path
- **Test harness:** `.planning/test-matrix.yaml`, `scripts/verify-harness.ps1`, `harness-verify.yml`, embedded fixtures, Playwright smoke
- **UDFs:** Unified registry (java-plugin, script, sql); console upload/publish; JDBC persistence; template publish-time validation; sample UDFs in `samples/udf-samples/`
- **Transforms:** json/mask/lookup operators, `GET /api/console/transforms`, actionable run-report errors, `V2_JSON_EXTRACT`
- **CI gate:** expanded P0 matrix (Phase 10) must pass before merge (`AGENTS.md` merge criteria)

## Deferred Beyond v2.0

- Template-level orchestration (ORCH-01, ORCH-02)
- Exhaustive matrix coverage, distributed worker E2E (TEST-V2)

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

### Validated (v2.0 — through Phase 11)

- ✓ Datasource platform core + governance (DS-01..DS-05) — Phases 6–7; DS-03 execute-path snap routing closed in Phase 07.1
- ✓ RW streaming CSV/JSON + JDBC upsert (RW-01..RW-04) — Phase 8
- ✓ JDBC dialect expansion DM/KB/HG/PG/CK (RW-05, RW-06) — Phase 9
- ✓ Harness coverage + P0 CI gates for RW/DS paths (TEST-07, TEST-08) — Phase 10
- ✓ Closeout hardening: managed JDBC E2E IT + Kingbase dialect evidence pack (DS-02 proof depth, RW-05/RW-06 E2E depth) — Validated in Phase 11: v2.0 closeout hardening

### Active (v2.0)

- Milestone archive / remaining accepted audit tech_debt (Dameng live / Nyquist) — ready for `/gsd-complete-milestone`

### Out of Scope

- Template-level orchestration — deferred beyond v2.0
- Flow-control transforms (branch/retry/parallel DAG) — not in v1 scope
- Exhaustive 100% UI/control coverage — harness-first with phased targets
- Greenfield rewrite or Template V1 revival — V2 only

## Context

Brownfield codebase mapped 2026-06-17. v1.0 delivered 5 phases / 18 plans over 2026-06-17 → 2026-06-23 (~46 commits, +13k LOC in milestone range).

Known pressure points:

- Console RBAC disabled by default (`ConsoleSecurityProperties.enabled = false`)
- Internal Gensokyo Kafka/ES starters on Boot 3 APIs vs Boot 4 runtime
- `spring-ai` SNAPSHOT and Ollama-gated tests
- HTTP internal Nexus/SCM URLs

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
| Defer template orchestration to later version | Product decision during questioning | — Pending v2 |
| Defer Reader/Writer and datasource abstraction to v2 | Focus v1 on UDF + quality + transforms | ✓ v2.0 started |
| v2 dialect priority: DM, Kingbase, HighGo, PG, CK | Domestic + core analytical JDBC targets | ✓ Good — Phase 9 + Phase 11 Kingbase evidence pack |
| v2 RW: close streaming/upsert gaps before new adapters | Gap matrix over net-new connectors | ✓ Good — Phase 8 shipped |
| Test acceptance: harness + phased coverage (not exhaustive matrix in v1) | Pragmatic ramp vs big-bang UI coverage | ✓ Good — P0/P1/P2 tiers |
| P0 = 7 matrix rows; merge gate in CI | Protect core UDF/transform paths | ✓ Good — 7/7 green |

<details>
<summary>Pre-v1.0 planning snapshot (2026-06-17)</summary>

Initial GSD milestone scoped UDF, transform operators, and test harness as v1; deferred Reader/Writer, datasource refactor, and orchestration to v2. See `milestones/v1.0-REQUIREMENTS.md` for full v1 requirement outcomes.

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
*Last updated: 2026-07-25 after Phase 11 v2.0 closeout hardening*
