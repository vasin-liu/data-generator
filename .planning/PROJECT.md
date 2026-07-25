# data-generator

## What This Is

**data-generator** is a Java/Maven synthetic-data platform for operators who define Template V2 pipelines (sources → transforms → sinks) and run them via a Spring Boot service with an embedded React operator console. The codebase is a mature brownfield monorepo (`3.0.0-SNAPSHOT`) with pluggable readers, writers, iterators, stages, Calcite SQL transforms, GraalJS/Velocity/SpEL scripting, and AI-assisted generation.

**v1.0 shipped** operator-uploadable multi-form UDFs, json/mask/lookup transform operators, a unified transform catalog API, and a quality-first automated test harness with P0 CI regression gates.

**v2.0 shipped** a populated `data-generator-datasource` platform (JDBC/Kafka/ES) with snapshot hot-reload governance, streaming CSV/JSON I/O, JDBC upsert, first-class dialects (Dameng, Kingbase, HighGo, PostgreSQL, ClickHouse), and an expanded 15-row P0 harness merge gate.

## Core Value

Operators can define, extend, and trust data-generation pipelines: register custom logic (UDFs), apply rich transforms, and verify behavior through an automated test harness before shipping.

## Current State

**Shipped: v2.0 Reader/Writer & Datasource Platform** (2026-07-25)

- Unified connection catalog + adapters; managed `dataSourceId` resolution on V2 execute path with `snap:` run snapshots
- Streaming/chunked CSV/JSON sources and sinks; PG/MySQL upsert with run-report diagnostics
- Five-engine JDBC dialect writers and console presets; Kingbase evidence pack + managed JDBC E2E IT
- Harness: 15 P0 rows; `scripts/verify-harness.ps1` is the canonical merge gate

Archives: `.planning/milestones/v2.0-ROADMAP.md`, `v2.0-REQUIREMENTS.md`, `v2.0-MILESTONE-AUDIT.md`

## Current Milestone: v2.1 Hardening & Weak-Spot Closure

**Goal:** Close the highest-value proof and reliability gaps left after v2.0 — without opening a new major feature lane.

**Target features:**
- Execute-path evidence: managed-catalog (+ dialect) journeys through HTTP `/task/run`
- Dialect/driver hardening: Dameng live IT documented green path; Nyquist/validation hygiene backfill
- Resolver ownership docs + call-site inventory (no code merge)
- One multi-JVM distributed worker E2E path with harness linkage
- Console RBAC: keep default-off; testable enable path + staging/e2e docs
- Focused P1 harness expansion for new proof paths; P0 remains merge gate

**Out of scope this milestone:** template orchestration, net-new connectors (Redis/S3/HTTP), default-on RBAC, full JDBC resolver consolidation

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

### Active

(None — define next milestone requirements with `/gsd-new-milestone`)

### Out of Scope

- Template-level orchestration — deferred beyond v2.0
- Flow-control transforms (branch/retry/parallel DAG) — not in v1/v2 scope
- Exhaustive 100% UI/control coverage — harness-first with phased targets
- Greenfield rewrite or Template V1 revival — V2 only
- Net-new non-JDBC connectors in v2.0 — deferred after dialect/gap closure

## Context

Brownfield codebase mapped 2026-06-17.

- **v1.0:** 5 phases / 18 plans over 2026-06-17 → 2026-06-23 (~46 commits, +13k LOC in milestone range)
- **v2.0:** 7 phases / 36 plans over 2026-06-23 → 2026-07-25 (~130 commits, +33k / −0.5k LOC; 331 files)

Known pressure points:

- Console RBAC disabled by default (`ConsoleSecurityProperties.enabled = false`)
- Internal Gensokyo Kafka/ES starters on Boot 3 APIs vs Boot 4 runtime
- `spring-ai` SNAPSHOT and Ollama-gated tests
- HTTP internal Nexus/SCM URLs
- Accepted v2.0 tech debt: Dameng opt-in IT, Nyquist hygiene, dual JDBC resolvers

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
| Keep dual JDBC resolvers with ownership split | Catalog-side vs V2 execute-path authority | ⚠️ Revisit — consolidation deferred |
| Dameng live IT opt-in (`-Ddm.it=true`) | Licensed driver / CI cost | ⚠️ Revisit — accepted tech debt |
| P0 expanded to 15 rows; `verify-harness.ps1` merge gate | Protect streaming/upsert/dialect paths | ✓ Good — Phase 10 |
| Test acceptance: harness + phased coverage | Pragmatic ramp vs big-bang UI coverage | ✓ Good — P0/P1/P2 tiers |

<details>
<summary>Pre-v1.0 planning snapshot (2026-06-17)</summary>

Initial GSD milestone scoped UDF, transform operators, and test harness as v1; deferred Reader/Writer, datasource refactor, and orchestration to v2. See `milestones/v1.0-REQUIREMENTS.md` for full v1 requirement outcomes.

</details>

<details>
<summary>v2.0 planning snapshot (2026-06-23 → 2026-07-25)</summary>

Milestone goal: close high-priority V2 source/sink gaps and establish unified datasource abstraction with hot-reload governance. See `milestones/v2.0-REQUIREMENTS.md` and `milestones/v2.0-MILESTONE-AUDIT.md` for outcomes and accepted tech debt.

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
*Last updated: 2026-07-25 after v2.0 milestone*
